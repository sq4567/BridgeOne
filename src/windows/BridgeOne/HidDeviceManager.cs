using HidLibrary;
using Serilog;

namespace BridgeOne;

/// <summary>
/// HID 장치 관리자
/// ESP32-S3 HID 장치의 자동 감지 및 연결/분리 이벤트 처리를 담당
/// </summary>
/// <remarks>
/// HidLibrary를 사용하여 시스템에 연결된 HID 장치를 스캔하고,
/// ESP32-S3 보드(Espressif VID: 0x303A)를 필터링하여 감지합니다.
/// 1초 간격의 폴링 방식으로 장치 연결/분리 이벤트를 감지합니다.
/// </remarks>
public class HidDeviceManager : IDisposable
{
    #region Constants
    
    /// <summary>
    /// Espressif Systems의 USB Vendor ID (VID)
    /// </summary>
    /// <remarks>
    /// ESP32-S3는 Espressif 제품이므로 VID는 0x303A입니다.
    /// 참조: https://github.com/espressif/usb-pids
    /// </remarks>
    private const int ESPRESSIF_VID = 0x303A;
    
    /// <summary>
    /// 장치 감지 폴링 간격 (밀리초)
    /// </summary>
    private const int POLLING_INTERVAL_MS = 1000;
    
    #endregion

    #region Fields
    
    /// <summary>
    /// 장치 감지 폴링을 위한 타이머
    /// </summary>
    private readonly System.Threading.Timer? _pollingTimer;
    
    /// <summary>
    /// 이전 스캔에서 감지된 장치 경로 목록
    /// </summary>
    private readonly HashSet<string> _previousDevicePaths = new();
    
    /// <summary>
    /// 현재 연결된 ESP32-S3 HID 장치 목록
    /// </summary>
    private readonly List<HidDevice> _connectedDevices = new();
    
    /// <summary>
    /// 개체가 이미 해제되었는지 여부
    /// </summary>
    private bool _disposed = false;
    
    #endregion

    #region Events
    
    /// <summary>
    /// ESP32-S3 HID 장치가 연결되었을 때 발생하는 이벤트
    /// </summary>
    public event EventHandler<HidDevice>? DeviceConnected;
    
    /// <summary>
    /// ESP32-S3 HID 장치가 분리되었을 때 발생하는 이벤트
    /// </summary>
    public event EventHandler<string>? DeviceDisconnected;
    
    #endregion

    #region Constructor & Destructor
    
    /// <summary>
    /// HidDeviceManager의 새 인스턴스를 초기화합니다.
    /// </summary>
    public HidDeviceManager()
    {
        Log.Debug("HidDeviceManager 초기화");
        
        // 초기 장치 스캔
        ScanDevices();
        
        // 폴링 타이머 시작 (1초 간격)
        _pollingTimer = new System.Threading.Timer(
            callback: _ => ScanDevices(),
            state: null,
            dueTime: POLLING_INTERVAL_MS,
            period: POLLING_INTERVAL_MS);
        
        Log.Information("HID 장치 감지 시작 (폴링 간격: {Interval}ms)", POLLING_INTERVAL_MS);
    }
    
    /// <summary>
    /// HidDeviceManager 리소스를 해제합니다.
    /// </summary>
    ~HidDeviceManager()
    {
        Dispose(false);
    }
    
    #endregion

    #region Public Methods
    
    /// <summary>
    /// 장치 감지를 중지합니다.
    /// </summary>
    public void Stop()
    {
        Log.Information("HID 장치 감지 중지");
        _pollingTimer?.Dispose();
    }
    
    /// <summary>
    /// 현재 연결된 ESP32-S3 HID 장치 목록을 반환합니다.
    /// </summary>
    /// <returns>연결된 장치 목록</returns>
    public IReadOnlyList<HidDevice> GetConnectedDevices()
    {
        return _connectedDevices.AsReadOnly();
    }
    
    #endregion

    #region Private Methods
    
    /// <summary>
    /// 시스템에 연결된 모든 HID 장치를 스캔하고 ESP32-S3 장치를 필터링합니다.
    /// </summary>
    /// <remarks>
    /// HidLibrary의 HidDevices.Enumerate()를 사용하여 모든 HID 장치를 열거하고,
    /// Espressif VID(0x303A)를 가진 장치만 필터링합니다.
    /// 이전 스캔 결과와 비교하여 연결/분리 이벤트를 감지합니다.
    /// </remarks>
    private void ScanDevices()
    {
        try
        {
            // 모든 HID 장치 열거
            var allDevices = HidDevices.Enumerate();
            
            // ESP32-S3 장치 필터링 (Espressif VID)
            var esp32Devices = allDevices
                .Where(device => device.Attributes.VendorId == ESPRESSIF_VID)
                .ToList();
            
            // 현재 스캔에서 발견된 장치 경로 집합
            var currentDevicePaths = new HashSet<string>(
                esp32Devices.Select(d => d.DevicePath));
            
            // 신규 연결된 장치 감지
            foreach (var device in esp32Devices)
            {
                if (!_previousDevicePaths.Contains(device.DevicePath))
                {
                    OnDeviceConnected(device);
                }
            }
            
            // 분리된 장치 감지
            foreach (var previousPath in _previousDevicePaths)
            {
                if (!currentDevicePaths.Contains(previousPath))
                {
                    OnDeviceDisconnected(previousPath);
                }
            }
            
            // 이전 스캔 결과 업데이트
            _previousDevicePaths.Clear();
            foreach (var path in currentDevicePaths)
            {
                _previousDevicePaths.Add(path);
            }
        }
        catch (Exception ex)
        {
            Log.Error(ex, "HID 장치 스캔 중 오류 발생");
        }
    }
    
    /// <summary>
    /// 장치 연결 이벤트를 처리합니다.
    /// </summary>
    /// <param name="device">연결된 HID 장치</param>
    private void OnDeviceConnected(HidDevice device)
    {
        // 장치 정보 로깅
        Log.Information("========================================");
        Log.Information("ESP32-S3 HID 장치 감지됨");
        Log.Information("----------------------------------------");
        Log.Information("VID: 0x{VID:X4}", device.Attributes.VendorId);
        Log.Information("PID: 0x{PID:X4}", device.Attributes.ProductId);
        Log.Information("제품명: {ProductName}", GetProductName(device));
        Log.Information("제조사: {Manufacturer}", GetManufacturer(device));
        Log.Information("장치 경로: {DevicePath}", device.DevicePath);
        Log.Information("========================================");
        
        // 연결된 장치 목록에 추가
        _connectedDevices.Add(device);
        
        // 이벤트 발생
        DeviceConnected?.Invoke(this, device);
    }
    
    /// <summary>
    /// 장치 분리 이벤트를 처리합니다.
    /// </summary>
    /// <param name="devicePath">분리된 장치 경로</param>
    private void OnDeviceDisconnected(string devicePath)
    {
        Log.Warning("========================================");
        Log.Warning("ESP32-S3 HID 장치 분리됨");
        Log.Warning("장치 경로: {DevicePath}", devicePath);
        Log.Warning("========================================");
        
        // 연결된 장치 목록에서 제거
        _connectedDevices.RemoveAll(d => d.DevicePath == devicePath);
        
        // 이벤트 발생
        DeviceDisconnected?.Invoke(this, devicePath);
    }
    
    /// <summary>
    /// HID 장치의 제품명을 안전하게 가져옵니다.
    /// </summary>
    /// <param name="device">HID 장치</param>
    /// <returns>제품명 또는 "Unknown"</returns>
    private string GetProductName(HidDevice device)
    {
        try
        {
            // HidLibrary의 OpenDevice() 메서드를 사용
            device.OpenDevice();
            if (device.IsOpen)
            {
                byte[] data;
                var result = device.ReadProduct(out data);
                device.CloseDevice();
                
                if (result && data != null)
                {
                    // UTF-16 문자열로 변환 (HID 디스크립터는 UTF-16 사용)
                    return System.Text.Encoding.Unicode.GetString(data).TrimEnd('\0');
                }
            }
            return "Unknown (Cannot open device)";
        }
        catch (Exception ex)
        {
            Log.Debug(ex, "제품명 읽기 실패");
            return "Unknown";
        }
    }
    
    /// <summary>
    /// HID 장치의 제조사명을 안전하게 가져옵니다.
    /// </summary>
    /// <param name="device">HID 장치</param>
    /// <returns>제조사명 또는 "Unknown"</returns>
    private string GetManufacturer(HidDevice device)
    {
        try
        {
            // HidLibrary의 OpenDevice() 메서드를 사용
            device.OpenDevice();
            if (device.IsOpen)
            {
                byte[] data;
                var result = device.ReadManufacturer(out data);
                device.CloseDevice();
                
                if (result && data != null)
                {
                    // UTF-16 문자열로 변환 (HID 디스크립터는 UTF-16 사용)
                    return System.Text.Encoding.Unicode.GetString(data).TrimEnd('\0');
                }
            }
            return "Unknown (Cannot open device)";
        }
        catch (Exception ex)
        {
            Log.Debug(ex, "제조사명 읽기 실패");
            return "Unknown";
        }
    }
    
    #endregion

    #region IDisposable Implementation
    
    /// <summary>
    /// 관리되는 및 관리되지 않는 리소스를 해제합니다.
    /// </summary>
    /// <param name="disposing">관리되는 리소스를 해제할지 여부</param>
    protected virtual void Dispose(bool disposing)
    {
        if (!_disposed)
        {
            if (disposing)
            {
                // 관리되는 리소스 해제
                _pollingTimer?.Dispose();
                
                // 연결된 모든 장치 정리
                foreach (var device in _connectedDevices)
                {
                    device?.Dispose();
                }
                _connectedDevices.Clear();
                
                Log.Debug("HidDeviceManager 리소스 해제 완료");
            }
            
            _disposed = true;
        }
    }
    
    /// <summary>
    /// HidDeviceManager 리소스를 해제합니다.
    /// </summary>
    public void Dispose()
    {
        Dispose(true);
        GC.SuppressFinalize(this);
    }
    
    #endregion
}

