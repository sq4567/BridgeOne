using System.Diagnostics;
using System.Text;
using System.Windows;
using System.Windows.Media;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using BridgeOne.Protocol;
using BridgeOne.Services;
using static BridgeOne.Services.CdcConnectionService;

namespace BridgeOne.ViewModels;

/// <summary>
/// ESP32-S3 BridgeOne 동글의 연결 상태를 관리하는 ViewModel.
/// CdcConnectionService의 상태 변경과 VendorCdcProtocol의 디버그 이벤트를 구독하여
/// UI에 바인딩 가능한 속성으로 노출합니다.
/// </summary>
public sealed partial class ConnectionViewModel : ObservableObject, IDisposable
{
    private readonly CdcConnectionService _connectionService;
    private readonly VendorCdcProtocol _protocol;
    private readonly StringBuilder _debugLogBuilder = new();
    private const int MaxDebugLogLines = 200;
    private bool _disposed;

    // ==================== Observable Properties ====================

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(StatusBrush))]
    [NotifyPropertyChangedFor(nameof(StatusText))]
    [NotifyPropertyChangedFor(nameof(IsConnected))]
    [NotifyPropertyChangedFor(nameof(IsConnecting))]
    [NotifyPropertyChangedFor(nameof(IsDeviceInfoVisible))]
    [NotifyPropertyChangedFor(nameof(ToggleButtonText))]
    [NotifyCanExecuteChangedFor(nameof(ToggleConnectionCommand))]
    [NotifyCanExecuteChangedFor(nameof(SendPingCommand))]
    [NotifyCanExecuteChangedFor(nameof(RunCrcVerificationCommand))]
    [NotifyCanExecuteChangedFor(nameof(RunTransmissionDebugCommand))]
    [NotifyCanExecuteChangedFor(nameof(RunCrcErrorTestCommand))]
    private ConnectionState _connectionState = ConnectionState.Disconnected;

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(StatusText))]
    [NotifyPropertyChangedFor(nameof(IsDeviceInfoVisible))]
    private string _comPort = string.Empty;

    [ObservableProperty]
    private string _deviceDescription = string.Empty;

    [ObservableProperty]
    private string _debugLog = string.Empty;

    // ==================== Computed Properties ====================

    /// <summary>연결 상태에 따른 색상 브러시</summary>
    public SolidColorBrush StatusBrush => ConnectionState switch
    {
        ConnectionState.Disconnected => new SolidColorBrush(Color.FromRgb(0x6B, 0x72, 0x80)), // Gray-500
        ConnectionState.Connecting   => new SolidColorBrush(Color.FromRgb(0xF5, 0x9E, 0x0B)), // Amber-500
        ConnectionState.Connected    => new SolidColorBrush(Color.FromRgb(0x10, 0xB9, 0x81)), // Green-500
        ConnectionState.Error        => new SolidColorBrush(Color.FromRgb(0xEF, 0x44, 0x44)), // Red-500
        _                            => new SolidColorBrush(Color.FromRgb(0x6B, 0x72, 0x80))
    };

    /// <summary>연결 상태 텍스트</summary>
    public string StatusText => ConnectionState switch
    {
        ConnectionState.Disconnected => "미연결",
        ConnectionState.Connecting   => "연결 중...",
        ConnectionState.Connected    => $"연결됨 ({ComPort})",
        ConnectionState.Error        => "연결 오류",
        _                            => "알 수 없음"
    };

    public bool IsConnected => ConnectionState == ConnectionState.Connected;
    public bool IsConnecting => ConnectionState == ConnectionState.Connecting;
    public bool IsDeviceInfoVisible => ConnectionState == ConnectionState.Connected && !string.IsNullOrEmpty(ComPort);
    public string ToggleButtonText => IsConnected ? "연결 해제" : "연결";

    // ==================== Constructor ====================

    public ConnectionViewModel(CdcConnectionService connectionService, VendorCdcProtocol protocol)
    {
        _connectionService = connectionService;
        _protocol = protocol;

        // CdcConnectionService 이벤트 (UI 스레드에서 발생)
        _connectionService.StateChanged += OnConnectionStateChanged;
        _connectionService.ErrorOccurred += OnErrorOccurred;

        // VendorCdcProtocol 이벤트 (백그라운드 스레드에서 발생 → Dispatcher 필요)
        _protocol.DebugTextReceived += OnDebugTextReceived;
        _protocol.FrameReceived += OnFrameReceived;
        _protocol.FrameDiscarded += OnFrameDiscarded;

        // 초기 상태 동기화
        SyncFromCurrentState();
    }

    // ==================== Commands ====================

    [RelayCommand(CanExecute = nameof(CanToggleConnection))]
    private void ToggleConnection()
    {
        if (IsConnected)
            _connectionService.Disconnect();
        else
            _connectionService.TryScanAndConnect();
    }

    private bool CanToggleConnection => !IsConnecting;

    [RelayCommand]
    private void CopyDebugLog()
    {
        if (!string.IsNullOrEmpty(DebugLog))
            Clipboard.SetText(DebugLog);
    }

    [RelayCommand]
    private void ClearDebugLog()
    {
        _debugLogBuilder.Clear();
        DebugLog = string.Empty;
    }

    [RelayCommand(CanExecute = nameof(IsConnected))]
    private async Task SendPingAsync()
    {
        try
        {
            var sw = Stopwatch.StartNew();
            AppendDebugLog("[PING 테스트] PING 전송 중...");
            await _protocol.SendFrameAsync((byte)VendorCdcCommand.Ping);
            sw.Stop();
            AppendDebugLog($"[PING 테스트] 전송 완료 ({sw.ElapsedMilliseconds}ms)");
        }
        catch (Exception ex)
        {
            AppendDebugLog($"[PING 테스트] 전송 실패: {ex.Message}");
        }
    }

    [RelayCommand(CanExecute = nameof(IsConnected))]
    private async Task RunTransmissionDebugAsync()
    {
        AppendDebugLog("========== 대용량 전송 버그 진단 ==========");

        // ── 테스트 케이스 정의 ──────────────────────────────────────────
        var cases = new (string label, byte[] payload)[]
        {
            // [크기 문제 확인] 0xFF 없는 페이로드 — 사이즈별 스캔
            ("Size  64B 0xAA",  MakeFilledPayload(  64, 0xAA)),
            ("Size 128B 0xAA",  MakeFilledPayload( 128, 0xAA)),
            ("Size 256B 0xAA",  MakeFilledPayload( 256, 0xAA)),
            ("Size 448B 0xAA",  MakeFilledPayload( 448, 0xAA)),

            // [0xFF 내용 확인] 0xFF 포함, 64바이트 이하
            ("FF at [0]  64B",  MakePayloadWithFf( 64, ffIndex: 0)),
            ("FF at [32] 64B",  MakePayloadWithFf( 64, ffIndex: 32)),
            ("FF at [63] 64B",  MakePayloadWithFf( 64, ffIndex: 63)),

            // [크기 + 0xFF 복합] 256B / 448B 순차 (0xFF 포함)
            ("Seq 256B (0xFF@255)", MakeSequentialPayload(256)),
            ("Seq 448B (0xFF@255)", MakeSequentialPayload(448)),
        };

        foreach (var (label, payload) in cases)
        {
            var sentCrc = Crc16.Calculate(payload);
            AppendDebugLog($"[TX] {label,-26} size={payload.Length,3}B sentCRC=0x{sentCrc:X4}");

            // 응답을 수신하기 위한 TCS (최대 2초 대기)
            var tcs = new TaskCompletionSource<VendorCdcFrame>(
                TaskCreationOptions.RunContinuationsAsynchronously);

            void OnFrame(object? _, VendorCdcFrame f)
            {
                if (f.Command == (byte)VendorCdcCommand.StateSyncAck ||
                    f.Command == (byte)VendorCdcCommand.Error)
                    tcs.TrySetResult(f);
            }
            void OnDiscarded(object? _, VendorCdcFrame f) =>
                tcs.TrySetResult(f);  // CRC 오류 프레임도 캡처

            _protocol.FrameReceived  += OnFrame;
            _protocol.FrameDiscarded += OnDiscarded;

            try
            {
                await _protocol.SendFrameAsync((byte)VendorCdcCommand.StateSync, payload);

                var completed = await Task.WhenAny(tcs.Task, Task.Delay(2000));
                if (completed != tcs.Task)
                {
                    AppendDebugLog($"[RX] {label,-26} ⏱ 타임아웃 (응답 없음)");
                    continue;
                }

                var resp = tcs.Task.Result;
                if (resp.Command == (byte)VendorCdcCommand.Error)
                {
                    AppendDebugLog($"[RX] {label,-26} ❌ ESP32 CRC오류 → ERROR 응답");
                }
                else
                {
                    var echoCrc = Crc16.Calculate(resp.Payload);
                    bool match  = echoCrc == sentCrc;
                    AppendDebugLog(
                        $"[RX] {label,-26} {(match ? "✓ PASS" : "✗ FAIL")} " +
                        $"echoCRC=0x{echoCrc:X4} payloadLen={resp.Payload.Length}B");
                }
            }
            finally
            {
                _protocol.FrameReceived  -= OnFrame;
                _protocol.FrameDiscarded -= OnDiscarded;
            }

            await Task.Delay(300);
        }

        AppendDebugLog("========== 진단 완료 ==========");
    }

    // ── 페이로드 생성 헬퍼 ─────────────────────────────────────────────

    private static byte[] MakeFilledPayload(int size, byte fill)
    {
        var buf = new byte[size];
        Array.Fill(buf, fill);
        return buf;
    }

    private static byte[] MakeSequentialPayload(int size)
    {
        var buf = new byte[size];
        for (int i = 0; i < size; i++) buf[i] = (byte)(i & 0xFF);
        return buf;
    }

    private static byte[] MakePayloadWithFf(int size, int ffIndex)
    {
        var buf = new byte[size];
        Array.Fill(buf, (byte)0xAA);
        buf[ffIndex] = 0xFF;
        return buf;
    }

    [RelayCommand(CanExecute = nameof(IsConnected))]
    private async Task RunCrcErrorTestAsync()
    {
        AppendDebugLog("========== CRC 오류 처리 검증 시작 ==========");

        // ── 테스트 1: 빈 페이로드 + 틀린 CRC ──
        {
            AppendDebugLog("[TX] PING(빈 페이로드) + 잘못된 CRC 0xDEAD 전송");

            var tcs = new TaskCompletionSource<(string type, VendorCdcFrame frame)>(
                TaskCreationOptions.RunContinuationsAsynchronously);

            void OnFrame(object? _, VendorCdcFrame f) => tcs.TrySetResult(("frame", f));
            void OnDiscarded(object? _, VendorCdcFrame f) => tcs.TrySetResult(("discarded", f));

            _protocol.FrameReceived += OnFrame;
            _protocol.FrameDiscarded += OnDiscarded;

            try
            {
                // 0xFF + PING(0x10) + length=0x0000 + CRC=0xDEAD (정상은 0x0000)
                var corrupted = new byte[] { 0xFF, 0x10, 0x00, 0x00, 0xAD, 0xDE };
                await _protocol.SendRawBytesAsync(corrupted);

                var completed = await Task.WhenAny(tcs.Task, Task.Delay(2000));
                if (completed != tcs.Task)
                {
                    AppendDebugLog("[RX] ⏱ 타임아웃 — ESP32 응답 없음");
                }
                else
                {
                    var (type, resp) = tcs.Task.Result;
                    if (type == "discarded")
                    {
                        AppendDebugLog($"[RX] ✓ FrameDiscarded 이벤트 발생 (cmd=0x{resp.Command:X2}) — Windows CRC 오류 감지 PASS");
                    }
                    else if (resp.Command == (byte)VendorCdcCommand.Error)
                    {
                        var detail = resp.Payload.Length >= 2
                            ? $"original_cmd=0x{resp.Payload[0]:X2}, error_code=0x{resp.Payload[1]:X2}"
                            : $"payload={resp.Payload.Length}B";
                        AppendDebugLog($"[RX] ✓ ESP32 ERROR 응답 수신 ({detail}) — ESP32 CRC 오류 감지 PASS");
                    }
                    else
                    {
                        AppendDebugLog($"[RX] ✗ 예상치 못한 응답: cmd=0x{resp.Command:X2}");
                    }
                }
            }
            finally
            {
                _protocol.FrameReceived -= OnFrame;
                _protocol.FrameDiscarded -= OnDiscarded;
            }
        }

        await Task.Delay(500);

        // ── 테스트 2: "PING" 페이로드 + 틀린 CRC ──
        {
            AppendDebugLog("[TX] StateSync(\"PING\" 4B) + 잘못된 CRC 0xBEEF 전송");

            var tcs = new TaskCompletionSource<(string type, VendorCdcFrame frame)>(
                TaskCreationOptions.RunContinuationsAsynchronously);

            void OnFrame(object? _, VendorCdcFrame f) => tcs.TrySetResult(("frame", f));
            void OnDiscarded(object? _, VendorCdcFrame f) => tcs.TrySetResult(("discarded", f));

            _protocol.FrameReceived += OnFrame;
            _protocol.FrameDiscarded += OnDiscarded;

            try
            {
                // 0xFF + StateSync(0x03) + length=4 LE + "PING" + CRC=0xBEEF (정상은 0xE0E7)
                var corrupted = new byte[] {
                    0xFF, 0x03, 0x04, 0x00,
                    0x50, 0x49, 0x4E, 0x47,  // "PING"
                    0xEF, 0xBE                // 0xBEEF LE
                };
                await _protocol.SendRawBytesAsync(corrupted);

                var completed = await Task.WhenAny(tcs.Task, Task.Delay(2000));
                if (completed != tcs.Task)
                {
                    AppendDebugLog("[RX] ⏱ 타임아웃 — ESP32 응답 없음");
                }
                else
                {
                    var (type, resp) = tcs.Task.Result;
                    if (type == "discarded")
                    {
                        AppendDebugLog($"[RX] ✓ FrameDiscarded 이벤트 발생 (cmd=0x{resp.Command:X2}) — Windows CRC 오류 감지 PASS");
                    }
                    else if (resp.Command == (byte)VendorCdcCommand.Error)
                    {
                        var detail = resp.Payload.Length >= 2
                            ? $"original_cmd=0x{resp.Payload[0]:X2}, error_code=0x{resp.Payload[1]:X2}"
                            : $"payload={resp.Payload.Length}B";
                        AppendDebugLog($"[RX] ✓ ESP32 ERROR 응답 수신 ({detail}) — ESP32 CRC 오류 감지 PASS");
                    }
                    else
                    {
                        AppendDebugLog($"[RX] ✗ 예상치 못한 응답: cmd=0x{resp.Command:X2}");
                    }
                }
            }
            finally
            {
                _protocol.FrameReceived -= OnFrame;
                _protocol.FrameDiscarded -= OnDiscarded;
            }
        }

        AppendDebugLog("========== CRC 오류 처리 검증 완료 ==========");
    }

    [RelayCommand(CanExecute = nameof(IsConnected))]
    private async Task RunCrcVerificationAsync()
    {
        AppendDebugLog("========== CRC16 교차 검증 시작 ==========");

        // 테스트 벡터 1: 빈 페이로드
        var empty = Array.Empty<byte>();
        var crc1 = Crc16.Calculate(empty);
        AppendDebugLog($"[CRC 벡터1] Empty payload → CRC=0x{crc1:X4}");

        // 테스트 벡터 2: "PING" (4바이트)
        var ping = Encoding.UTF8.GetBytes("PING");
        var crc2 = Crc16.Calculate(ping);
        AppendDebugLog($"[CRC 벡터2] \"PING\" → CRC=0x{crc2:X4}");

        // 테스트 벡터 3: 448바이트 순차 데이터 (0x00~0xFF 반복)
        var maxPayload = new byte[448];
        for (int i = 0; i < 448; i++) maxPayload[i] = (byte)(i & 0xFF);
        var crc3 = Crc16.Calculate(maxPayload);
        AppendDebugLog($"[CRC 벡터3] 448B sequential → CRC=0x{crc3:X4}");

        AppendDebugLog("--- ESP32-S3에 프레임 전송하여 CRC 일치 검증 ---");

        try
        {
            // 벡터1: PING 명령 (빈 페이로드) → ESP32가 CRC 오류 없이 수신하면 일치
            await _protocol.SendFrameAsync((byte)VendorCdcCommand.Ping);
            AppendDebugLog("[CRC 벡터1] PING(빈 페이로드) 전송 완료 → PONG 응답 대기");

            await Task.Delay(500);

            // 벡터2: StateSync 명령 + "PING" 페이로드 → ESP32가 CRC 오류 없이 수신하면 일치
            await _protocol.SendFrameAsync((byte)VendorCdcCommand.StateSync, ping);
            AppendDebugLog("[CRC 벡터2] StateSync(\"PING\" 페이로드) 전송 완료");

            await Task.Delay(500);

            // 벡터3: StateSync 명령 + 448바이트 페이로드 → ESP32가 CRC 오류 없이 수신하면 일치
            await _protocol.SendFrameAsync((byte)VendorCdcCommand.StateSync, maxPayload);
            AppendDebugLog("[CRC 벡터3] StateSync(448B 페이로드) 전송 완료");

            await Task.Delay(500);
            AppendDebugLog("========== CRC16 교차 검증 완료 ==========");
            AppendDebugLog("→ ESP32-S3 로그에 [CRC 오류] 없으면 PASS");
            AppendDebugLog("→ ESP32-S3 시작 시 출력한 CRC 값과 위 값 비교");
        }
        catch (Exception ex)
        {
            AppendDebugLog($"[CRC 검증] 전송 실패: {ex.Message}");
        }
    }

    // ==================== Event Handlers ====================

    /// <summary>연결 상태 변경 (UI 스레드)</summary>
    private void OnConnectionStateChanged(object? sender, ConnectionStateEventArgs e)
    {
        ConnectionState = e.NewState;
        UpdateDeviceInfo();
    }

    /// <summary>오류 발생 (UI 스레드)</summary>
    private void OnErrorOccurred(object? sender, string message)
    {
        AppendDebugLog($"[오류] {message}");
    }

    /// <summary>디버그 텍스트 수신 (백그라운드 스레드 → Dispatcher 마샬링)</summary>
    private void OnDebugTextReceived(object? sender, string text)
    {
        Application.Current.Dispatcher.BeginInvoke(() => AppendDebugLog(text));
    }

    /// <summary>프레임 수신 (백그라운드 스레드 → Dispatcher 마샬링)</summary>
    private void OnFrameReceived(object? sender, VendorCdcFrame frame)
    {
        var cmdName = frame.Command switch
        {
            (byte)VendorCdcCommand.Pong => "PONG",
            (byte)VendorCdcCommand.AuthResponse => "AUTH_RESPONSE",
            (byte)VendorCdcCommand.StateSyncAck => "STATE_SYNC_ACK",
            (byte)VendorCdcCommand.ModeNotify => "MODE_NOTIFY",
            (byte)VendorCdcCommand.Error => "ERROR",
            _ => $"0x{frame.Command:X2}"
        };

        var payloadHex = frame.Payload.Length > 0
            ? $" [{BitConverter.ToString(frame.Payload).Replace("-", " ")}]"
            : "";

        Application.Current.Dispatcher.BeginInvoke(() =>
            AppendDebugLog($"[수신] {cmdName} (payload={frame.Payload.Length}B){payloadHex}"));
    }

    /// <summary>CRC 오류 프레임 폐기 (백그라운드 스레드 → Dispatcher 마샬링)</summary>
    private void OnFrameDiscarded(object? sender, VendorCdcFrame frame)
    {
        Application.Current.Dispatcher.BeginInvoke(() =>
            AppendDebugLog($"[CRC 오류] cmd=0x{frame.Command:X2}, payload={frame.Payload.Length}B"));
    }

    // ==================== Private Helpers ====================

    private void SyncFromCurrentState()
    {
        ConnectionState = _connectionService.State;
        UpdateDeviceInfo();
    }

    private void UpdateDeviceInfo()
    {
        var device = _connectionService.ConnectedDevice;
        ComPort = device?.ComPort ?? string.Empty;
        DeviceDescription = device?.Description ?? string.Empty;
    }

    private void AppendDebugLog(string text)
    {
        var trimmed = text.TrimEnd('\r', '\n');
        if (string.IsNullOrEmpty(trimmed)) return;

        _debugLogBuilder.AppendLine(trimmed);

        // 최대 줄 수 제한
        var content = _debugLogBuilder.ToString();
        var lines = content.Split('\n');
        if (lines.Length > MaxDebugLogLines)
        {
            _debugLogBuilder.Clear();
            var kept = string.Join('\n', lines[^MaxDebugLogLines..]);
            _debugLogBuilder.Append(kept);
            content = kept;
        }

        DebugLog = content;
    }

    // ==================== IDisposable ====================

    public void Dispose()
    {
        if (_disposed) return;
        _disposed = true;

        _connectionService.StateChanged -= OnConnectionStateChanged;
        _connectionService.ErrorOccurred -= OnErrorOccurred;
        _protocol.DebugTextReceived -= OnDebugTextReceived;
        _protocol.FrameReceived -= OnFrameReceived;
        _protocol.FrameDiscarded -= OnFrameDiscarded;
    }
}
