using System.Diagnostics;
using System.IO;
using System.IO.Ports;
using System.Management;
using BridgeOne.Models;

namespace BridgeOne.Services;

/// <summary>
/// ESP32-S3 BridgeOne 동글의 CDC COM 포트를 자동 감지하고 SerialPort로 연결하는 서비스.
/// USB 핫플러그(연결/해제)를 실시간 모니터링합니다.
/// </summary>
public sealed class CdcConnectionService : IDisposable
{
    // ==================== 상수 ====================

    /// <summary>Espressif VID</summary>
    private const ushort TargetVid = 0x303A;

    /// <summary>BridgeOne PID</summary>
    private const ushort TargetPid = 0x4001;

    /// <summary>CDC는 baud rate 무관하지만, 형식상 설정</summary>
    private const int BaudRate = 115200;

    // ==================== 연결 상태 ====================

    public enum ConnectionState
    {
        Disconnected,
        Connecting,
        Connected,
        Error
    }

    private ConnectionState _state = ConnectionState.Disconnected;
    public ConnectionState State
    {
        get => _state;
        private set
        {
            if (_state == value) return;
            var old = _state;
            _state = value;
            StateChanged?.Invoke(this, new ConnectionStateEventArgs(old, value));
        }
    }

    /// <summary>현재 연결된 장치 정보 (미연결 시 null)</summary>
    public DeviceInfo? ConnectedDevice { get; private set; }

    /// <summary>내부 SerialPort (Protocol 계층에서 BaseStream 접근용)</summary>
    public SerialPort? Port { get; private set; }

    // ==================== 이벤트 ====================

    public event EventHandler<ConnectionStateEventArgs>? StateChanged;
    public event EventHandler<string>? ErrorOccurred;

    public sealed class ConnectionStateEventArgs : EventArgs
    {
        public ConnectionState OldState { get; }
        public ConnectionState NewState { get; }
        public ConnectionStateEventArgs(ConnectionState oldState, ConnectionState newState)
        {
            OldState = oldState;
            NewState = newState;
        }
    }

    // ==================== WMI 핫플러그 모니터링 ====================

    private ManagementEventWatcher? _insertWatcher;
    private ManagementEventWatcher? _removeWatcher;
    private readonly SynchronizationContext? _syncContext;
    private bool _disposed;

    public CdcConnectionService()
    {
        _syncContext = SynchronizationContext.Current;
    }

    // ==================== 공개 API ====================

    /// <summary>
    /// 서비스를 시작합니다. 이미 연결된 장치를 검색하고, 핫플러그 모니터링을 시작합니다.
    /// </summary>
    public void Start()
    {
        StartHotplugWatchers();
        TryScanAndConnect();
    }

    /// <summary>
    /// 서비스를 중지합니다. 연결을 해제하고 핫플러그 모니터링을 중단합니다.
    /// </summary>
    public void Stop()
    {
        StopHotplugWatchers();
        Disconnect();
    }

    /// <summary>
    /// 현재 연결된 장치를 해제합니다.
    /// </summary>
    public void Disconnect()
    {
        ClosePort();
        ConnectedDevice = null;
        State = ConnectionState.Disconnected;
    }

    /// <summary>
    /// 수동으로 장치를 검색하여 연결을 시도합니다.
    /// </summary>
    public bool TryScanAndConnect()
    {
        if (State == ConnectionState.Connected) return true;

        State = ConnectionState.Connecting;

        var device = FindBridgeOneDevice();
        if (device == null)
        {
            State = ConnectionState.Disconnected;
            return false;
        }

        return TryOpenPort(device);
    }

    // ==================== WMI 장치 검색 ====================

    /// <summary>
    /// WMI를 사용하여 VID=0x303A, PID=0x4001인 CDC COM 포트를 찾습니다.
    /// </summary>
    private DeviceInfo? FindBridgeOneDevice()
    {
        try
        {
            // Win32_PnPEntity에서 VID/PID로 필터링하여 COM 포트를 찾음
            var vidPidFilter = $"VID_{TargetVid:X4}&PID_{TargetPid:X4}";
            var query = new SelectQuery(
                "Win32_PnPEntity",
                $"PNPDeviceID LIKE '%{vidPidFilter}%' AND Name LIKE '%(COM%'"
            );

            using var searcher = new ManagementObjectSearcher(query);
            using var results = searcher.Get();

            foreach (ManagementObject obj in results)
            {
                var name = obj["Name"]?.ToString() ?? string.Empty;
                var pnpDeviceId = obj["PNPDeviceID"]?.ToString() ?? string.Empty;
                var description = obj["Description"]?.ToString() ?? string.Empty;

                // 이름에서 COM 포트 번호 추출: "BridgeOne CDC (COM3)" → "COM3"
                var comPort = ExtractComPort(name);
                if (string.IsNullOrEmpty(comPort)) continue;

                // PnP Device ID에서 시리얼 번호 추출
                var serialNumber = ExtractSerialNumber(pnpDeviceId);

                return new DeviceInfo
                {
                    Vid = TargetVid,
                    Pid = TargetPid,
                    ComPort = comPort,
                    Description = description,
                    SerialNumber = serialNumber,
                    PnpDeviceId = pnpDeviceId
                };
            }
        }
        catch (ManagementException ex)
        {
            RaiseError($"WMI 장치 검색 실패: {ex.Message}");
        }
        catch (Exception ex)
        {
            RaiseError($"장치 검색 중 예외: {ex.Message}");
        }

        return null;
    }

    /// <summary>
    /// "USB Serial Device (COM3)" 같은 문자열에서 "COM3"을 추출합니다.
    /// </summary>
    private static string? ExtractComPort(string deviceName)
    {
        var start = deviceName.LastIndexOf("(COM", StringComparison.OrdinalIgnoreCase);
        if (start < 0) return null;

        var end = deviceName.IndexOf(')', start);
        if (end < 0) return null;

        // "(COM3)" → "COM3"
        return deviceName[(start + 1)..end];
    }

    /// <summary>
    /// PnP Device ID에서 시리얼 번호를 추출합니다.
    /// 예: "USB\VID_303A&PID_4001\00000001" → "00000001"
    /// </summary>
    private static string ExtractSerialNumber(string pnpDeviceId)
    {
        var lastBackslash = pnpDeviceId.LastIndexOf('\\');
        return lastBackslash >= 0 ? pnpDeviceId[(lastBackslash + 1)..] : string.Empty;
    }

    // ==================== SerialPort 관리 ====================

    /// <summary>
    /// 지정된 장치의 COM 포트를 열어 연결합니다.
    /// </summary>
    private bool TryOpenPort(DeviceInfo device)
    {
        try
        {
            ClosePort();

            var port = new SerialPort(device.ComPort)
            {
                BaudRate = BaudRate,
                DataBits = 8,
                Parity = Parity.None,
                StopBits = StopBits.One,
                ReadTimeout = 1000,
                WriteTimeout = 1000,
                // CDC는 하드웨어 흐름 제어 불필요
                Handshake = Handshake.None,
                DtrEnable = true,
                RtsEnable = true
            };

            port.Open();

            Port = port;
            ConnectedDevice = device;
            State = ConnectionState.Connected;

            Debug.WriteLine($"[CdcConnectionService] 연결됨: {device}");
            return true;
        }
        catch (UnauthorizedAccessException)
        {
            RaiseError($"COM 포트 접근 거부: {device.ComPort} (다른 프로그램이 사용 중일 수 있음)");
        }
        catch (IOException ex)
        {
            RaiseError($"COM 포트 열기 실패: {device.ComPort} - {ex.Message}");
        }
        catch (Exception ex)
        {
            RaiseError($"COM 포트 연결 중 예외: {ex.Message}");
        }

        State = ConnectionState.Error;
        return false;
    }

    /// <summary>
    /// 현재 열려 있는 SerialPort를 안전하게 닫습니다.
    /// </summary>
    private void ClosePort()
    {
        if (Port == null) return;

        try
        {
            if (Port.IsOpen)
            {
                Port.DiscardInBuffer();
                Port.DiscardOutBuffer();
                Port.Close();
            }
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"[CdcConnectionService] 포트 닫기 중 예외: {ex.Message}");
        }
        finally
        {
            Port.Dispose();
            Port = null;
        }
    }

    // ==================== USB 핫플러그 감지 ====================

    /// <summary>
    /// WMI 이벤트를 사용하여 USB 장치 연결/해제를 모니터링합니다.
    /// </summary>
    private void StartHotplugWatchers()
    {
        StopHotplugWatchers();

        try
        {
            // 장치 연결 감지
            var insertQuery = new WqlEventQuery(
                "__InstanceCreationEvent",
                TimeSpan.FromSeconds(1),
                "TargetInstance ISA 'Win32_PnPEntity'"
            );
            _insertWatcher = new ManagementEventWatcher(insertQuery);
            _insertWatcher.EventArrived += OnDeviceInserted;
            _insertWatcher.Start();

            // 장치 해제 감지
            var removeQuery = new WqlEventQuery(
                "__InstanceDeletionEvent",
                TimeSpan.FromSeconds(1),
                "TargetInstance ISA 'Win32_PnPEntity'"
            );
            _removeWatcher = new ManagementEventWatcher(removeQuery);
            _removeWatcher.EventArrived += OnDeviceRemoved;
            _removeWatcher.Start();

            Debug.WriteLine("[CdcConnectionService] 핫플러그 모니터링 시작");
        }
        catch (ManagementException ex)
        {
            RaiseError($"핫플러그 모니터링 시작 실패: {ex.Message}");
        }
    }

    private void StopHotplugWatchers()
    {
        if (_insertWatcher != null)
        {
            _insertWatcher.EventArrived -= OnDeviceInserted;
            _insertWatcher.Stop();
            _insertWatcher.Dispose();
            _insertWatcher = null;
        }

        if (_removeWatcher != null)
        {
            _removeWatcher.EventArrived -= OnDeviceRemoved;
            _removeWatcher.Stop();
            _removeWatcher.Dispose();
            _removeWatcher = null;
        }
    }

    /// <summary>
    /// USB 장치가 연결되었을 때 호출됩니다.
    /// BridgeOne 장치인지 확인 후 자동 연결을 시도합니다.
    /// </summary>
    private void OnDeviceInserted(object sender, EventArrivedEventArgs e)
    {
        var targetInstance = (ManagementBaseObject)e.NewEvent["TargetInstance"];
        var pnpDeviceId = targetInstance["PNPDeviceID"]?.ToString() ?? string.Empty;

        // BridgeOne 장치인지 확인
        var vidPidFilter = $"VID_{TargetVid:X4}&PID_{TargetPid:X4}";
        if (!pnpDeviceId.Contains(vidPidFilter, StringComparison.OrdinalIgnoreCase))
            return;

        Debug.WriteLine($"[CdcConnectionService] BridgeOne 장치 연결 감지: {pnpDeviceId}");

        // CDC COM 포트가 준비될 때까지 대기 (usbser.sys 드라이버 로드 시간)
        // 500ms 후 1차 시도, 실패 시 500ms 후 재시도
        Task.Delay(500).ContinueWith(_ =>
        {
            PostToSyncContext(() =>
            {
                if (!TryScanAndConnect())
                    Task.Delay(500).ContinueWith(__ => PostToSyncContext(() => TryScanAndConnect()));
            });
        });
    }

    /// <summary>
    /// USB 장치가 해제되었을 때 호출됩니다.
    /// 현재 연결된 BridgeOne 장치가 분리된 경우 연결을 정리합니다.
    /// </summary>
    private void OnDeviceRemoved(object sender, EventArrivedEventArgs e)
    {
        var targetInstance = (ManagementBaseObject)e.NewEvent["TargetInstance"];
        var pnpDeviceId = targetInstance["PNPDeviceID"]?.ToString() ?? string.Empty;

        // 현재 연결된 장치가 분리되었는지 확인
        if (ConnectedDevice == null) return;

        var vidPidFilter = $"VID_{TargetVid:X4}&PID_{TargetPid:X4}";
        if (!pnpDeviceId.Contains(vidPidFilter, StringComparison.OrdinalIgnoreCase))
            return;

        Debug.WriteLine($"[CdcConnectionService] BridgeOne 장치 분리 감지: {pnpDeviceId}");

        PostToSyncContext(() => Disconnect());
    }

    // ==================== 유틸리티 ====================

    /// <summary>
    /// UI 스레드에서 액션을 실행합니다 (WPF Dispatcher 호환).
    /// </summary>
    private void PostToSyncContext(Action action)
    {
        if (_syncContext != null)
            _syncContext.Post(_ => action(), null);
        else
            action();
    }

    private void RaiseError(string message)
    {
        Debug.WriteLine($"[CdcConnectionService] 오류: {message}");
        PostToSyncContext(() => ErrorOccurred?.Invoke(this, message));
    }

    // ==================== IDisposable ====================

    public void Dispose()
    {
        if (_disposed) return;
        _disposed = true;

        Stop();
    }
}
