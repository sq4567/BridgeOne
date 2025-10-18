using System.IO.Ports;
using System.Management;
using System.Text;
using System.Text.Json;
using Serilog;

namespace BridgeOne;

/// <summary>
/// ESP32-S3 Vendor CDC(Communications Device Class) 장치와 통신하는 관리자
/// System.IO.Ports.SerialPort를 사용하여 JSON 프레임 기반 명령 송수신 처리
/// </summary>
public class VendorCdcManager : IDisposable
{
    #region Constants

    /// <summary>
    /// ESP32-S3 USB Vendor ID (Espressif Systems)
    /// 참조: https://github.com/espressif/arduino-esp32
    /// </summary>
    private const ushort ESP32_VENDOR_ID = 0x303A;

    /// <summary>
    /// 프레임 시작 바이트 (0xFF)
    /// Android 앱에서 정의한 BridgeOne 프로토콜 헤더
    /// </summary>
    private const byte FRAME_HEADER = 0xFF;

    /// <summary>
    /// 기본 Baud Rate (115200)
    /// ESP32-S3 CDC 통신의 표준 속도
    /// 참조: https://docs.espressif.com/projects/esp-idf/en/latest/esp32s3/api-reference/peripherals/usb_device.html
    /// </summary>
    private const int DEFAULT_BAUD_RATE = 115200;

    /// <summary>
    /// 최대 프레임 크기 (4KB)
    /// </summary>
    private const int MAX_FRAME_SIZE = 4096;

    #endregion

    #region Fields

    /// <summary>
    /// Serial Port 인스턴스
    /// </summary>
    private SerialPort? _serialPort;

    /// <summary>
    /// 비동기 수신 루프를 위한 CancellationTokenSource
    /// </summary>
    private CancellationTokenSource? _cancellationTokenSource;

    /// <summary>
    /// 비동기 수신 태스크
    /// </summary>
    private Task? _receiveTask;

    /// <summary>
    /// Dispose 여부
    /// </summary>
    private bool _disposed = false;

    #endregion

    #region Events

    /// <summary>
    /// JSON 응답 수신 이벤트
    /// </summary>
    public event EventHandler<JsonResponseEventArgs>? JsonResponseReceived;

    /// <summary>
    /// 연결 상태 변경 이벤트
    /// </summary>
    public event EventHandler<bool>? ConnectionStateChanged;

    #endregion

    #region Properties

    /// <summary>
    /// CDC 연결 상태
    /// </summary>
    public bool IsConnected => _serialPort?.IsOpen ?? false;

    /// <summary>
    /// 현재 사용 중인 COM 포트 이름
    /// </summary>
    public string? PortName => _serialPort?.PortName;

    #endregion

    #region Initialization

    /// <summary>
    /// VendorCdcManager 생성자
    /// </summary>
    public VendorCdcManager()
    {
        Log.Debug("VendorCdcManager 인스턴스 생성");
    }

    #endregion

    #region Public Methods

    /// <summary>
    /// ESP32-S3 CDC 장치를 자동으로 감지하고 연결
    /// ManagementObjectSearcher를 사용하여 USB 장치 열거
    /// </summary>
    /// <returns>연결 성공 여부</returns>
    [System.Runtime.Versioning.SupportedOSPlatform("windows")]
    public bool Connect()
    {
        try
        {
            Log.Information("ESP32-S3 CDC 장치를 검색 중...");

            // COM 포트 자동 감지
            string? comPort = FindEsp32ComPort();
            if (comPort == null)
            {
                Log.Warning("ESP32-S3 CDC 장치를 찾을 수 없습니다");
                return false;
            }

            Log.Information("ESP32-S3 CDC 장치 발견: {PortName}", comPort);

            // SerialPort 초기화
            _serialPort = new SerialPort(comPort)
            {
                BaudRate = DEFAULT_BAUD_RATE,
                DataBits = 8,
                Parity = Parity.None,
                StopBits = StopBits.One,
                Handshake = Handshake.None,
                ReadTimeout = 1000,
                WriteTimeout = 1000,
                DtrEnable = true,  // CDC 연결을 위해 DTR 활성화
                RtsEnable = true   // CDC 연결을 위해 RTS 활성화
            };

            // 포트 열기
            _serialPort.Open();
            Log.Information("COM 포트 열기 성공: {PortName} (BaudRate: {BaudRate})", 
                comPort, DEFAULT_BAUD_RATE);

            // 비동기 수신 루프 시작
            _cancellationTokenSource = new CancellationTokenSource();
            _receiveTask = Task.Run(() => ReceiveLoopAsync(_cancellationTokenSource.Token));

            // 연결 상태 이벤트 발생
            ConnectionStateChanged?.Invoke(this, true);

            return true;
        }
        catch (Exception ex)
        {
            Log.Error(ex, "CDC 연결 실패");
            return false;
        }
    }

    /// <summary>
    /// CDC 연결 종료
    /// </summary>
    public void Disconnect()
    {
        try
        {
            // 비동기 수신 루프 중지
            _cancellationTokenSource?.Cancel();
            _receiveTask?.Wait(2000); // 최대 2초 대기

            // Serial Port 닫기
            if (_serialPort?.IsOpen == true)
            {
                _serialPort.Close();
                Log.Information("COM 포트 닫기 완료: {PortName}", _serialPort.PortName);
            }

            // 연결 상태 이벤트 발생
            ConnectionStateChanged?.Invoke(this, false);
        }
        catch (Exception ex)
        {
            Log.Error(ex, "CDC 연결 종료 중 오류 발생");
        }
    }

    /// <summary>
    /// JSON 명령 전송
    /// System.Text.Json을 사용하여 직렬화하고, 프레임으로 조립하여 전송
    /// 프레임 구조: [0xFF][LengthH][LengthL][JSON Payload][CRC16H][CRC16L]
    /// </summary>
    /// <param name="command">명령 이름 (예: "PING", "GET_STATUS")</param>
    /// <param name="data">명령 데이터 (JSON으로 직렬화될 객체)</param>
    /// <returns>전송 성공 여부</returns>
    public bool SendJsonCommand(string command, object? data = null)
    {
        if (!IsConnected)
        {
            Log.Warning("CDC 연결되지 않음. 명령 전송 불가: {Command}", command);
            return false;
        }

        try
        {
            // JSON 객체 생성
            var jsonObject = new
            {
                command = command,
                data = data,
                timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
            };

            // JSON 직렬화 (System.Text.Json 사용)
            // 참조: Newtonsoft.Json과 유사한 API이지만 .NET 8.0에 내장
            string jsonString = JsonSerializer.Serialize(jsonObject, new JsonSerializerOptions
            {
                PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
                WriteIndented = false
            });

            byte[] jsonBytes = Encoding.UTF8.GetBytes(jsonString);
            
            if (jsonBytes.Length > MAX_FRAME_SIZE - 5) // 헤더(1) + 길이(2) + CRC(2) = 5바이트
            {
                Log.Error("JSON 페이로드 크기 초과: {Size} bytes (최대: {MaxSize})", 
                    jsonBytes.Length, MAX_FRAME_SIZE - 5);
                return false;
            }

            // 프레임 조립
            // [0xFF][LengthH][LengthL][JSON Payload][CRC16H][CRC16L]
            ushort payloadLength = (ushort)jsonBytes.Length;
            ushort crc16 = CalculateCrc16(jsonBytes);

            using var memoryStream = new MemoryStream();
            using var writer = new BinaryWriter(memoryStream);

            writer.Write(FRAME_HEADER);                        // 0xFF
            writer.Write((byte)(payloadLength >> 8));          // Length High
            writer.Write((byte)(payloadLength & 0xFF));        // Length Low
            writer.Write(jsonBytes);                           // JSON Payload
            writer.Write((byte)(crc16 >> 8));                  // CRC16 High
            writer.Write((byte)(crc16 & 0xFF));                // CRC16 Low

            byte[] frame = memoryStream.ToArray();

            // SerialPort.Write()로 전송
            _serialPort!.Write(frame, 0, frame.Length);

            Log.Debug("JSON 명령 전송 완료: {Command}, 프레임 크기: {FrameSize} bytes", 
                command, frame.Length);
            Log.Verbose("전송 JSON: {Json}", jsonString);

            return true;
        }
        catch (Exception ex)
        {
            Log.Error(ex, "JSON 명령 전송 실패: {Command}", command);
            return false;
        }
    }

    #endregion

    #region Private Methods

    /// <summary>
    /// ESP32-S3 COM 포트 자동 감지
    /// ManagementObjectSearcher로 USB 장치 열거하여 VID=0x303A인 장치의 COM 포트 찾기
    /// </summary>
    /// <returns>COM 포트 이름 (예: "COM3") 또는 null</returns>
    [System.Runtime.Versioning.SupportedOSPlatform("windows")]
    private string? FindEsp32ComPort()
    {
        try
        {
            // WMI를 사용하여 USB 장치 열거
            // 참조: https://docs.microsoft.com/en-us/windows/win32/cimwin32prov/win32-pnpentity
            using var searcher = new ManagementObjectSearcher(
                "SELECT * FROM Win32_PnPEntity WHERE Caption LIKE '%(COM%'");

            foreach (ManagementObject device in searcher.Get())
            {
                string? caption = device["Caption"]?.ToString();
                string? deviceId = device["DeviceID"]?.ToString();

                if (string.IsNullOrEmpty(caption) || string.IsNullOrEmpty(deviceId))
                    continue;

                // ESP32-S3 VID 확인 (VID_303A)
                if (deviceId.Contains($"VID_{ESP32_VENDOR_ID:X4}", StringComparison.OrdinalIgnoreCase))
                {
                    // Caption에서 COM 포트 번호 추출 (예: "USB Serial Device (COM3)")
                    var match = System.Text.RegularExpressions.Regex.Match(caption, @"\(COM(\d+)\)");
                    if (match.Success)
                    {
                        string comPort = $"COM{match.Groups[1].Value}";
                        Log.Debug("ESP32-S3 장치 발견: {Caption}, DeviceID: {DeviceId}, ComPort: {ComPort}", 
                            caption, deviceId, comPort);
                        return comPort;
                    }
                }
            }

            return null;
        }
        catch (Exception ex)
        {
            Log.Error(ex, "COM 포트 검색 중 오류 발생");
            return null;
        }
    }

    /// <summary>
    /// 비동기 수신 루프
    /// 0xFF 헤더를 대기하고, 프레임을 수신하여 CRC16 검증 후 JSON 파싱
    /// </summary>
    /// <param name="cancellationToken">취소 토큰</param>
    private async Task ReceiveLoopAsync(CancellationToken cancellationToken)
    {
        Log.Debug("비동기 수신 루프 시작");

        byte[] buffer = new byte[MAX_FRAME_SIZE];

        try
        {
            while (!cancellationToken.IsCancellationRequested && IsConnected)
            {
                try
                {
                    // 0xFF 헤더 대기
                    int headerByte = _serialPort!.ReadByte();
                    if (headerByte != FRAME_HEADER)
                        continue;

                    // 길이 읽기 (2바이트, Big-Endian)
                    int lengthHigh = _serialPort.ReadByte();
                    int lengthLow = _serialPort.ReadByte();
                    if (lengthHigh == -1 || lengthLow == -1)
                        continue;

                    ushort payloadLength = (ushort)((lengthHigh << 8) | lengthLow);
                    
                    if (payloadLength > MAX_FRAME_SIZE - 5)
                    {
                        Log.Warning("수신 프레임 크기 초과: {Length} bytes", payloadLength);
                        continue;
                    }

                    // JSON Payload 읽기
                    int bytesRead = 0;
                    while (bytesRead < payloadLength)
                    {
                        int read = await _serialPort.BaseStream.ReadAsync(
                            buffer.AsMemory(bytesRead, payloadLength - bytesRead), 
                            cancellationToken);
                        
                        if (read == 0)
                            break;
                        
                        bytesRead += read;
                    }

                    if (bytesRead != payloadLength)
                    {
                        Log.Warning("Payload 읽기 불완전: {BytesRead}/{Expected}", bytesRead, payloadLength);
                        continue;
                    }

                    // CRC16 읽기 (2바이트, Big-Endian)
                    int crcHigh = _serialPort.ReadByte();
                    int crcLow = _serialPort.ReadByte();
                    if (crcHigh == -1 || crcLow == -1)
                        continue;

                    ushort receivedCrc = (ushort)((crcHigh << 8) | crcLow);

                    // CRC16 검증
                    ushort calculatedCrc = CalculateCrc16(buffer.AsSpan(0, payloadLength));
                    if (receivedCrc != calculatedCrc)
                    {
                        Log.Warning("CRC16 검증 실패: 수신={Received:X4}, 계산={Calculated:X4}", 
                            receivedCrc, calculatedCrc);
                        continue;
                    }

                    // JSON 파싱 (System.Text.Json 사용)
                    string jsonString = Encoding.UTF8.GetString(buffer, 0, payloadLength);
                    Log.Verbose("수신 JSON: {Json}", jsonString);

                    using var jsonDocument = JsonDocument.Parse(jsonString);
                    var root = jsonDocument.RootElement;

                    // 이벤트 발생
                    JsonResponseReceived?.Invoke(this, new JsonResponseEventArgs(root));

                    Log.Debug("JSON 응답 수신 완료: {Length} bytes", payloadLength);
                }
                catch (TimeoutException)
                {
                    // 타임아웃은 정상 동작 (데이터가 없을 때)
                    await Task.Delay(10, cancellationToken);
                }
                catch (OperationCanceledException)
                {
                    // 취소 요청 시 정상 종료
                    break;
                }
                catch (Exception ex)
                {
                    Log.Error(ex, "프레임 수신 중 오류 발생");
                    await Task.Delay(100, cancellationToken);
                }
            }
        }
        catch (Exception ex)
        {
            Log.Error(ex, "수신 루프에서 치명적 오류 발생");
        }

        Log.Debug("비동기 수신 루프 종료");
    }

    /// <summary>
    /// CRC-16/CCITT-FALSE 계산
    /// 다항식: 0x1021, 초기값: 0xFFFF, 최종 XOR: 0x0000
    /// BridgeOne 프로토콜 표준 CRC 알고리즘
    /// </summary>
    /// <param name="data">데이터</param>
    /// <returns>CRC-16 값</returns>
    private static ushort CalculateCrc16(ReadOnlySpan<byte> data)
    {
        ushort crc = 0xFFFF;
        const ushort polynomial = 0x1021;

        foreach (byte b in data)
        {
            crc ^= (ushort)(b << 8);
            for (int i = 0; i < 8; i++)
            {
                if ((crc & 0x8000) != 0)
                    crc = (ushort)((crc << 1) ^ polynomial);
                else
                    crc <<= 1;
            }
        }

        return crc;
    }

    #endregion

    #region IDisposable

    /// <summary>
    /// 리소스 해제
    /// </summary>
    public void Dispose()
    {
        if (_disposed)
            return;

        Disconnect();

        _serialPort?.Dispose();
        _cancellationTokenSource?.Dispose();

        _disposed = true;
        GC.SuppressFinalize(this);
    }

    #endregion
}

/// <summary>
/// JSON 응답 이벤트 인자
/// </summary>
public class JsonResponseEventArgs : EventArgs
{
    /// <summary>
    /// JSON 응답 데이터 (JsonElement)
    /// </summary>
    public JsonElement JsonData { get; }

    /// <summary>
    /// 생성자
    /// </summary>
    /// <param name="jsonData">JSON 데이터</param>
    public JsonResponseEventArgs(JsonElement jsonData)
    {
        JsonData = jsonData;
    }
}

