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
    private readonly HandshakeService _handshakeService;
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
    [NotifyCanExecuteChangedFor(nameof(RunHandshakeTestCommand))]
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

    public ConnectionViewModel(CdcConnectionService connectionService, VendorCdcProtocol protocol, HandshakeService handshakeService)
    {
        _connectionService = connectionService;
        _protocol = protocol;
        _handshakeService = handshakeService;

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
    private async Task RunHandshakeTestAsync()
    {
        AppendDebugLog("========== 핸드셰이크 테스트 시작 ==========");

        var sw = Stopwatch.StartNew();

        try
        {
            var result = await _handshakeService.PerformHandshakeAsync();
            sw.Stop();

            if (result.Success)
            {
                AppendDebugLog($"[핸드셰이크] 성공 ({sw.ElapsedMilliseconds}ms)");
                AppendDebugLog($"  디바이스: {result.DeviceName ?? "(없음)"}");
                AppendDebugLog($"  펌웨어: {result.FirmwareVersion ?? "(없음)"}");
                AppendDebugLog($"  모드: {result.Mode}");
                AppendDebugLog($"  수락된 기능: [{string.Join(", ", result.AcceptedFeatures)}]");
            }
            else
            {
                AppendDebugLog($"[핸드셰이크] 실패 ({sw.ElapsedMilliseconds}ms)");
                AppendDebugLog($"  사유: {result.FailReason}");
                AppendDebugLog($"  메시지: {result.ErrorMessage}");
            }
        }
        catch (OperationCanceledException)
        {
            sw.Stop();
            AppendDebugLog($"[핸드셰이크] 취소됨 ({sw.ElapsedMilliseconds}ms)");
        }
        catch (Exception ex)
        {
            sw.Stop();
            AppendDebugLog($"[핸드셰이크] 예외 발생: {ex.Message}");
        }

        AppendDebugLog("========== 핸드셰이크 테스트 완료 ==========");
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
