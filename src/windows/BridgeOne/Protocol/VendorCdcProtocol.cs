using System.Buffers.Binary;
using System.Diagnostics;
using System.IO;
using System.Text;
using System.Threading.Channels;
using BridgeOne.Services;

namespace BridgeOne.Protocol;

/// <summary>
/// Vendor CDC 프레임 프로토콜 계층.
/// CdcConnectionService의 SerialPort.BaseStream 위에서 비동기 수신 루프를 돌며
/// 0xFF 바이너리 프레임과 디버그 텍스트를 분리합니다.
/// </summary>
public sealed class VendorCdcProtocol : IDisposable
{
    private const int ReadBufferSize = 1024;

    private readonly CdcConnectionService _connection;
    private readonly Channel<VendorCdcFrame> _frameChannel;

    private CancellationTokenSource? _receiveCts;
    private Task? _receiveTask;
    private readonly SemaphoreSlim _sendLock = new(1, 1);

    // 수신 버퍼: 부분 수신된 데이터를 누적
    private byte[] _accumBuffer = new byte[VendorCdcFrame.MaxFrameSize * 2];
    private int _accumLength;

    private bool _disposed;

    // ==================== 이벤트 ====================

    /// <summary>유효한 프레임 수신 시 발생 (Channel과 병행 사용 가능)</summary>
    public event EventHandler<VendorCdcFrame>? FrameReceived;

    /// <summary>디버그 텍스트 수신 시 발생 (0xFF가 아닌 바이트열)</summary>
    public event EventHandler<string>? DebugTextReceived;

    /// <summary>CRC 오류로 프레임 폐기 시 발생</summary>
    public event EventHandler<VendorCdcFrame>? FrameDiscarded;

    // ==================== 공개 API ====================

    /// <summary>수신된 프레임을 읽는 Channel Reader</summary>
    public ChannelReader<VendorCdcFrame> FrameReader => _frameChannel.Reader;

    public VendorCdcProtocol(CdcConnectionService connection)
    {
        _connection = connection;

        _frameChannel = Channel.CreateBounded<VendorCdcFrame>(
            new BoundedChannelOptions(64)
            {
                FullMode = BoundedChannelFullMode.DropOldest,
                SingleReader = false,
                SingleWriter = true
            });

        // 연결 상태 변경 시 수신 루프 자동 시작/중지
        _connection.StateChanged += OnConnectionStateChanged;
    }

    /// <summary>
    /// 바이너리 페이로드로 프레임을 전송합니다.
    /// </summary>
    public async Task SendFrameAsync(byte command, byte[] payload,
        CancellationToken cancellationToken = default)
    {
        var stream = _connection.Port?.BaseStream
            ?? throw new InvalidOperationException("SerialPort가 연결되어 있지 않습니다.");

        if (payload.Length > VendorCdcFrame.MaxPayloadSize)
            throw new ArgumentException(
                $"페이로드 크기 초과: {payload.Length} > {VendorCdcFrame.MaxPayloadSize}");

        var frame = new VendorCdcFrame(command, payload);
        var bytes = frame.ToBytes();

        await _sendLock.WaitAsync(cancellationToken);
        try
        {
            await stream.WriteAsync(bytes, cancellationToken);
            await stream.FlushAsync(cancellationToken);
        }
        finally
        {
            _sendLock.Release();
        }

        Debug.WriteLine(
            $"[VendorCdcProtocol] TX: cmd=0x{command:X2}, payload={payload.Length}B");
    }

    /// <summary>
    /// JSON 문자열 페이로드로 프레임을 전송합니다.
    /// </summary>
    public Task SendFrameAsync(byte command, string jsonPayload,
        CancellationToken cancellationToken = default)
    {
        var payload = Encoding.UTF8.GetBytes(jsonPayload);
        return SendFrameAsync(command, payload, cancellationToken);
    }

    /// <summary>
    /// 페이로드 없이 프레임을 전송합니다 (예: PING).
    /// </summary>
    public Task SendFrameAsync(byte command,
        CancellationToken cancellationToken = default)
        => SendFrameAsync(command, Array.Empty<byte>(), cancellationToken);

    // ==================== 수신 루프 ====================

    private void StartReceiveLoop()
    {
        if (_receiveTask is { IsCompleted: false })
            return;

        _accumLength = 0;
        _receiveCts = new CancellationTokenSource();
        _receiveTask = Task.Run(() => ReceiveLoopAsync(_receiveCts.Token));

        Debug.WriteLine("[VendorCdcProtocol] 수신 루프 시작");
    }

    private void StopReceiveLoop()
    {
        _receiveCts?.Cancel();
        _receiveCts?.Dispose();
        _receiveCts = null;
        _receiveTask = null;
        _accumLength = 0;

        Debug.WriteLine("[VendorCdcProtocol] 수신 루프 중지");
    }

    private async Task ReceiveLoopAsync(CancellationToken ct)
    {
        var readBuffer = new byte[ReadBufferSize];

        try
        {
            while (!ct.IsCancellationRequested)
            {
                var stream = _connection.Port?.BaseStream;
                if (stream == null || !_connection.Port!.IsOpen)
                    break;

                int bytesRead;
                try
                {
                    bytesRead = await stream.ReadAsync(readBuffer, 0, readBuffer.Length, ct);
                }
                catch (OperationCanceledException)
                {
                    break;
                }
                catch (Exception ex) when (ex is IOException or ObjectDisposedException)
                {
                    Debug.WriteLine(
                        $"[VendorCdcProtocol] 수신 스트림 오류 (연결 해제 가능): {ex.Message}");
                    break;
                }

                if (bytesRead == 0)
                    continue;

                // 누적 버퍼에 추가
                EnsureAccumCapacity(bytesRead);
                Array.Copy(readBuffer, 0, _accumBuffer, _accumLength, bytesRead);
                _accumLength += bytesRead;

                // 누적 버퍼에서 프레임과 디버그 텍스트 추출
                ProcessAccumulatedData();
            }
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            Debug.WriteLine($"[VendorCdcProtocol] 수신 루프 예외: {ex.Message}");
        }
    }

    // ==================== 버퍼 처리 ====================

    /// <summary>
    /// 누적 버퍼를 스캔하여 0xFF 프레임과 디버그 텍스트를 분리합니다.
    /// </summary>
    private void ProcessAccumulatedData()
    {
        int pos = 0;

        while (pos < _accumLength)
        {
            // 0xFF가 아닌 바이트 → 디버그 텍스트
            if (_accumBuffer[pos] != VendorCdcFrame.Header)
            {
                int textStart = pos;
                while (pos < _accumLength && _accumBuffer[pos] != VendorCdcFrame.Header)
                    pos++;

                var text = Encoding.UTF8.GetString(_accumBuffer, textStart, pos - textStart);
                if (!string.IsNullOrEmpty(text))
                    DebugTextReceived?.Invoke(this, text);

                continue;
            }

            // 0xFF 발견 → 프레임 파싱 시도
            int remaining = _accumLength - pos;

            // 최소 오버헤드 크기(6바이트)를 받을 때까지 대기
            if (remaining < VendorCdcFrame.OverheadSize)
                break;

            // length 필드 읽기
            ushort payloadLen = BinaryPrimitives.ReadUInt16LittleEndian(
                _accumBuffer.AsSpan(pos + 2));

            // 비정상적인 length → 이 0xFF는 프레임 헤더가 아님, 디버그 텍스트로 처리
            if (payloadLen > VendorCdcFrame.MaxPayloadSize)
            {
                DebugTextReceived?.Invoke(this, $"[0xFF:non-frame]");
                pos++;
                continue;
            }

            int expectedFrameSize = VendorCdcFrame.OverheadSize + payloadLen;

            // 프레임 전체를 아직 다 받지 못함 → 대기
            if (remaining < expectedFrameSize)
                break;

            // 프레임 파싱
            var frame = VendorCdcFrame.Parse(_accumBuffer, pos);
            if (frame != null)
            {
                if (frame.IsValid)
                {
                    _frameChannel.Writer.TryWrite(frame);
                    FrameReceived?.Invoke(this, frame);

                    Debug.WriteLine(
                        $"[VendorCdcProtocol] RX: cmd=0x{frame.Command:X2}, " +
                        $"payload={frame.Payload.Length}B");
                }
                else
                {
                    FrameDiscarded?.Invoke(this, frame);
                    Debug.WriteLine(
                        $"[VendorCdcProtocol] CRC 오류 → 프레임 폐기: cmd=0x{frame.Command:X2}");
                }

                pos += expectedFrameSize;
            }
            else
            {
                // 파싱 실패 → 이 0xFF 건너뜀
                pos++;
            }
        }

        // 남은 데이터를 버퍼 앞쪽으로 이동
        if (pos > 0 && pos < _accumLength)
        {
            Array.Copy(_accumBuffer, pos, _accumBuffer, 0, _accumLength - pos);
            _accumLength -= pos;
        }
        else if (pos >= _accumLength)
        {
            _accumLength = 0;
        }
    }

    private void EnsureAccumCapacity(int additionalBytes)
    {
        int required = _accumLength + additionalBytes;
        if (required <= _accumBuffer.Length)
            return;

        int newSize = Math.Max(_accumBuffer.Length * 2, required);
        var newBuffer = new byte[newSize];
        Array.Copy(_accumBuffer, newBuffer, _accumLength);
        _accumBuffer = newBuffer;
    }

    // ==================== 연결 상태 연동 ====================

    private void OnConnectionStateChanged(object? sender,
        CdcConnectionService.ConnectionStateEventArgs e)
    {
        if (e.NewState == CdcConnectionService.ConnectionState.Connected)
            StartReceiveLoop();
        else if (e.OldState == CdcConnectionService.ConnectionState.Connected)
            StopReceiveLoop();
    }

    // ==================== IDisposable ====================

    public void Dispose()
    {
        if (_disposed) return;
        _disposed = true;

        _connection.StateChanged -= OnConnectionStateChanged;
        StopReceiveLoop();
        _frameChannel.Writer.TryComplete();
        _sendLock.Dispose();
    }
}
