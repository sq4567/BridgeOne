using System.Diagnostics;
using System.Text;
using System.Text.Json;
using BridgeOne.Protocol;

namespace BridgeOne.Services;

/// <summary>
/// Keep-alive 서비스.
/// 핸드셰이크 완료 후 주기적으로 PING을 전송하고 PONG 응답을 모니터링합니다.
///
/// - 0.5초(500ms) 주기 PING 전송
/// - PONG 응답으로 RTT(왕복 지연) 측정
/// - 연속 3회 실패 시 ConnectionLost 이벤트 발생
/// - 지수 백오프 자동 재연결 (1초 → 2초 → 4초 → 8초 → 16초 → 30초)
/// - 최근 10개 RTT의 이동 평균으로 연결 품질 판정
/// </summary>
public sealed class KeepAliveService : IDisposable
{
    // ==================== 상수 ====================

    /// <summary>PING 전송 주기 (ms)</summary>
    private const int PingIntervalMs = 500;

    /// <summary>PONG 응답 타임아웃 (ms)</summary>
    private const int PongTimeoutMs = 1000;

    /// <summary>연속 실패 허용 횟수</summary>
    private const int MaxConsecutiveFailures = 3;

    /// <summary>RTT 이동 평균 윈도우 크기</summary>
    private const int RttWindowSize = 10;

    /// <summary>재연결 지수 백오프 최대 대기 시간 (초)</summary>
    private const int MaxReconnectDelaySec = 30;

    /// <summary>연결 품질 기준: 양호 (RTT < 20ms)</summary>
    private const double QualityGoodThresholdMs = 20;

    /// <summary>연결 품질 기준: 보통 (RTT < 100ms)</summary>
    private const double QualityFairThresholdMs = 100;

    // ==================== 의존성 ====================

    private readonly VendorCdcProtocol _protocol;
    private readonly CdcConnectionService _connectionService;
    private readonly HandshakeService _handshakeService;

    // ==================== 상태 ====================

    private CancellationTokenSource? _keepAliveCts;
    private Task? _keepAliveTask;
    private CancellationTokenSource? _reconnectCts;
    private Task? _reconnectTask;

    private int _consecutiveFailures;
    private readonly Queue<double> _rttWindow = new();
    private readonly object _rttLock = new();
    private bool _disposed;
    private bool _isRunning;

    // ==================== 이벤트 ====================

    /// <summary>연결 끊김 감지 시 발생 (연속 3회 PONG 미수신)</summary>
    public event EventHandler? ConnectionLost;

    /// <summary>자동 재연결 성공 시 발생</summary>
    public event EventHandler? Reconnected;

    /// <summary>RTT 업데이트 시 발생</summary>
    public event EventHandler<RttUpdatedEventArgs>? RttUpdated;

    /// <summary>연결 품질 변경 시 발생</summary>
    public event EventHandler<ConnectionQuality>? QualityChanged;

    /// <summary>재연결 시도 시 발생 (시도 횟수, 다음 대기 시간)</summary>
    public event EventHandler<ReconnectAttemptEventArgs>? ReconnectAttempt;

    /// <summary>상태 변경 로그 발생</summary>
    public event EventHandler<string>? StatusLog;

    // ==================== 공개 속성 ====================

    /// <summary>현재 평균 RTT (ms). 측정값 없으면 -1.</summary>
    public double AverageRttMs
    {
        get
        {
            lock (_rttLock)
            {
                if (_rttWindow.Count == 0) return -1;
                return _rttWindow.Average();
            }
        }
    }

    /// <summary>마지막 RTT (ms). 측정값 없으면 -1.</summary>
    public double LastRttMs { get; private set; } = -1;

    /// <summary>현재 연결 품질</summary>
    public ConnectionQuality Quality { get; private set; } = ConnectionQuality.Unknown;

    /// <summary>Keep-alive가 동작 중인지 여부</summary>
    public bool IsRunning => _isRunning;

    /// <summary>현재 연속 실패 횟수</summary>
    public int ConsecutiveFailures => _consecutiveFailures;

    // ==================== 생성자 ====================

    public KeepAliveService(
        VendorCdcProtocol protocol,
        CdcConnectionService connectionService,
        HandshakeService handshakeService)
    {
        _protocol = protocol;
        _connectionService = connectionService;
        _handshakeService = handshakeService;
    }

    // ==================== 공개 API ====================

    /// <summary>
    /// Keep-alive 루프를 시작합니다.
    /// 핸드셰이크 완료 후 호출해야 합니다.
    /// </summary>
    public void Start()
    {
        if (_isRunning) return;

        StopReconnect();
        _consecutiveFailures = 0;
        ClearRttWindow();

        _keepAliveCts = new CancellationTokenSource();
        _keepAliveTask = Task.Run(() => KeepAliveLoopAsync(_keepAliveCts.Token));
        _isRunning = true;

        RaiseStatusLog("Keep-alive 시작 (500ms 주기)");
        Debug.WriteLine("[KeepAliveService] Keep-alive 루프 시작");
    }

    /// <summary>
    /// Keep-alive 루프를 중지합니다.
    /// </summary>
    public void Stop()
    {
        if (!_isRunning) return;

        _keepAliveCts?.Cancel();
        _keepAliveCts?.Dispose();
        _keepAliveCts = null;
        _keepAliveTask = null;
        _isRunning = false;

        StopReconnect();

        RaiseStatusLog("Keep-alive 중지");
        Debug.WriteLine("[KeepAliveService] Keep-alive 루프 중지");
    }

    // ==================== Keep-alive 루프 ====================

    private async Task KeepAliveLoopAsync(CancellationToken ct)
    {
        using var timer = new PeriodicTimer(TimeSpan.FromMilliseconds(PingIntervalMs));

        try
        {
            while (await timer.WaitForNextTickAsync(ct))
            {
                await SendPingAndWaitPongAsync(ct);
            }
        }
        catch (OperationCanceledException)
        {
            // 정상 종료
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"[KeepAliveService] Keep-alive 루프 예외: {ex.Message}");
            RaiseStatusLog($"Keep-alive 루프 오류: {ex.Message}");
        }
    }

    private async Task SendPingAndWaitPongAsync(CancellationToken ct)
    {
        var timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

        // PING 전송
        try
        {
            var pingJson = JsonSerializer.Serialize(new
            {
                command = "PING",
                timestamp
            });

            await _protocol.SendFrameAsync(
                (byte)VendorCdcCommand.Ping,
                pingJson,
                ct);
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            Debug.WriteLine($"[KeepAliveService] PING 전송 실패: {ex.Message}");
            OnPongFailed();
            return;
        }

        // PONG 응답 대기 (1초 타임아웃)
        using var timeoutCts = CancellationTokenSource.CreateLinkedTokenSource(ct);
        timeoutCts.CancelAfter(PongTimeoutMs);

        try
        {
            while (await _protocol.FrameReader.WaitToReadAsync(timeoutCts.Token))
            {
                while (_protocol.FrameReader.TryRead(out var frame))
                {
                    if (frame.Command != (byte)VendorCdcCommand.Pong)
                        continue;

                    // PONG 수신 → RTT 계산
                    var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
                    var echoTimestamp = ExtractTimestamp(frame.Payload);

                    if (echoTimestamp.HasValue)
                    {
                        var rtt = now - echoTimestamp.Value;
                        OnPongReceived(rtt);
                    }
                    else
                    {
                        // timestamp 파싱 실패해도 PONG 수신은 성공으로 처리
                        var rtt = now - timestamp;
                        OnPongReceived(rtt);
                    }

                    return; // PONG 수신 완료
                }
            }
        }
        catch (OperationCanceledException) when (timeoutCts.IsCancellationRequested && !ct.IsCancellationRequested)
        {
            // PONG 타임아웃
            Debug.WriteLine("[KeepAliveService] PONG 타임아웃 (1초)");
            OnPongFailed();
        }
    }

    // ==================== PONG 결과 처리 ====================

    private void OnPongReceived(long rttMs)
    {
        _consecutiveFailures = 0;
        LastRttMs = rttMs;

        // RTT 이동 평균 업데이트
        lock (_rttLock)
        {
            _rttWindow.Enqueue(rttMs);
            while (_rttWindow.Count > RttWindowSize)
                _rttWindow.Dequeue();
        }

        var avgRtt = AverageRttMs;
        var newQuality = ClassifyQuality(avgRtt);

        RttUpdated?.Invoke(this, new RttUpdatedEventArgs(rttMs, avgRtt));

        if (newQuality != Quality)
        {
            Quality = newQuality;
            QualityChanged?.Invoke(this, newQuality);
        }

        Debug.WriteLine($"[KeepAliveService] PONG 수신: RTT={rttMs}ms, 평균={avgRtt:F1}ms, 품질={Quality}");
    }

    private void OnPongFailed()
    {
        _consecutiveFailures++;

        if (_consecutiveFailures == 1)
            RaiseStatusLog("PONG 미수신 (1회)");
        else if (_consecutiveFailures == 2)
        {
            RaiseStatusLog("PONG 미수신 (2회) - 연결 불안정");
            Quality = ConnectionQuality.Unstable;
            QualityChanged?.Invoke(this, ConnectionQuality.Unstable);
        }

        Debug.WriteLine($"[KeepAliveService] PONG 실패: 연속 {_consecutiveFailures}회");

        if (_consecutiveFailures >= MaxConsecutiveFailures)
        {
            RaiseStatusLog($"연속 {MaxConsecutiveFailures}회 PONG 미수신 → 연결 끊김 판정");
            Debug.WriteLine("[KeepAliveService] 연결 끊김 판정 → ConnectionLost");

            // Keep-alive 중지 → 재연결 시작
            _keepAliveCts?.Cancel();
            _isRunning = false;
            Quality = ConnectionQuality.Disconnected;
            QualityChanged?.Invoke(this, ConnectionQuality.Disconnected);
            ConnectionLost?.Invoke(this, EventArgs.Empty);

            // 자동 재연결 시작
            StartReconnect();
        }
    }

    // ==================== 자동 재연결 ====================

    private void StartReconnect()
    {
        StopReconnect();
        _reconnectCts = new CancellationTokenSource();
        _reconnectTask = Task.Run(() => ReconnectLoopAsync(_reconnectCts.Token));
    }

    private void StopReconnect()
    {
        _reconnectCts?.Cancel();
        _reconnectCts?.Dispose();
        _reconnectCts = null;
        _reconnectTask = null;
    }

    private async Task ReconnectLoopAsync(CancellationToken ct)
    {
        int attempt = 0;

        while (!ct.IsCancellationRequested)
        {
            attempt++;
            var delaySec = Math.Min((int)Math.Pow(2, attempt - 1), MaxReconnectDelaySec);

            RaiseStatusLog($"재연결 대기 중... ({delaySec}초 후 시도 #{attempt})");
            ReconnectAttempt?.Invoke(this, new ReconnectAttemptEventArgs(attempt, delaySec));

            try
            {
                await Task.Delay(TimeSpan.FromSeconds(delaySec), ct);
            }
            catch (OperationCanceledException)
            {
                return;
            }

            RaiseStatusLog($"재연결 시도 #{attempt}...");

            // 1. CDC 포트 재연결
            _connectionService.Disconnect();
            if (!_connectionService.TryScanAndConnect())
            {
                RaiseStatusLog($"재연결 #{attempt} 실패: 장치 미발견");
                continue;
            }

            // 2. Protocol 수신 루프가 시작될 시간을 약간 대기
            try
            {
                await Task.Delay(100, ct);
            }
            catch (OperationCanceledException)
            {
                return;
            }

            // 3. 핸드셰이크 재시도
            try
            {
                var result = await _handshakeService.PerformHandshakeAsync(ct);

                if (result.Success)
                {
                    RaiseStatusLog($"재연결 성공! (시도 #{attempt})");
                    Debug.WriteLine($"[KeepAliveService] 재연결 성공: 시도 #{attempt}");

                    Reconnected?.Invoke(this, EventArgs.Empty);

                    // Keep-alive 재시작
                    _consecutiveFailures = 0;
                    ClearRttWindow();
                    _keepAliveCts = new CancellationTokenSource();
                    _keepAliveTask = Task.Run(() => KeepAliveLoopAsync(_keepAliveCts.Token));
                    _isRunning = true;

                    return; // 재연결 성공 → 루프 종료
                }

                RaiseStatusLog($"재연결 #{attempt} 실패: 핸드셰이크 실패 ({result.FailReason})");
            }
            catch (OperationCanceledException)
            {
                return;
            }
            catch (Exception ex)
            {
                RaiseStatusLog($"재연결 #{attempt} 실패: {ex.Message}");
            }
        }
    }

    // ==================== 유틸리티 ====================

    private static long? ExtractTimestamp(byte[] payload)
    {
        if (payload.Length == 0) return null;

        try
        {
            var json = Encoding.UTF8.GetString(payload);
            using var doc = JsonDocument.Parse(json);
            if (doc.RootElement.TryGetProperty("timestamp", out var ts))
                return ts.GetInt64();
        }
        catch
        {
            // 파싱 실패 시 null 반환
        }

        return null;
    }

    private static ConnectionQuality ClassifyQuality(double avgRttMs)
    {
        if (avgRttMs < 0) return ConnectionQuality.Unknown;
        if (avgRttMs <= QualityGoodThresholdMs) return ConnectionQuality.Good;
        if (avgRttMs <= QualityFairThresholdMs) return ConnectionQuality.Fair;
        return ConnectionQuality.Unstable;
    }

    private void ClearRttWindow()
    {
        lock (_rttLock)
        {
            _rttWindow.Clear();
        }

        LastRttMs = -1;
        Quality = ConnectionQuality.Unknown;
    }

    private void RaiseStatusLog(string message)
    {
        StatusLog?.Invoke(this, message);
    }

    // ==================== IDisposable ====================

    public void Dispose()
    {
        if (_disposed) return;
        _disposed = true;

        Stop();
    }
}

// ==================== 이벤트 인자 및 열거형 ====================

/// <summary>연결 품질 등급</summary>
public enum ConnectionQuality
{
    /// <summary>측정값 없음</summary>
    Unknown,

    /// <summary>양호 (평균 RTT ≤ 20ms)</summary>
    Good,

    /// <summary>보통 (평균 RTT ≤ 100ms)</summary>
    Fair,

    /// <summary>불안정 (평균 RTT > 100ms 또는 PONG 실패 다수)</summary>
    Unstable,

    /// <summary>연결 끊김</summary>
    Disconnected
}

/// <summary>RTT 업데이트 이벤트 인자</summary>
public sealed class RttUpdatedEventArgs : EventArgs
{
    /// <summary>이번 RTT (ms)</summary>
    public double CurrentRttMs { get; }

    /// <summary>이동 평균 RTT (ms)</summary>
    public double AverageRttMs { get; }

    public RttUpdatedEventArgs(double currentRttMs, double averageRttMs)
    {
        CurrentRttMs = currentRttMs;
        AverageRttMs = averageRttMs;
    }
}

/// <summary>재연결 시도 이벤트 인자</summary>
public sealed class ReconnectAttemptEventArgs : EventArgs
{
    /// <summary>시도 횟수</summary>
    public int Attempt { get; }

    /// <summary>다음 재시도까지 대기 시간 (초)</summary>
    public int DelaySeconds { get; }

    public ReconnectAttemptEventArgs(int attempt, int delaySeconds)
    {
        Attempt = attempt;
        DelaySeconds = delaySeconds;
    }
}
