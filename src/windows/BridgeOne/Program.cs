using Serilog;
using Serilog.Events;

namespace BridgeOne;

/// <summary>
/// BridgeOne Windows Server Application
/// ESP32-S3 HID 장치와 통신하여 Android 터치 입력을 Windows 입력으로 변환하는 서버 애플리케이션
/// </summary>
internal class Program
{
    /// <summary>
    /// 애플리케이션의 비동기 진입점
    /// </summary>
    /// <param name="args">커맨드 라인 인자</param>
    /// <returns>비동기 작업</returns>
    private static async Task Main(string[] args)
    {
        // Serilog 로거 초기화
        // - 콘솔과 파일에 동시 출력
        // - Debug 레벨 이상의 모든 로그 기록
        // - 일별 로그 파일 롤링
        Log.Logger = new LoggerConfiguration()
            .MinimumLevel.Debug()
            .WriteTo.Console(
                outputTemplate: "[{Timestamp:HH:mm:ss} {Level:u3}] {Message:lj}{NewLine}{Exception}")
            .WriteTo.File(
                path: "logs/bridgeone-.log",
                rollingInterval: RollingInterval.Day,
                rollOnFileSizeLimit: true,
                fileSizeLimitBytes: 10 * 1024 * 1024, // 10MB
                retainedFileCountLimit: 7, // 최근 7일치 로그 보관
                outputTemplate: "[{Timestamp:yyyy-MM-dd HH:mm:ss.fff} {Level:u3}] {Message:lj}{NewLine}{Exception}")
            .CreateLogger();

        try
        {
            Log.Information("==============================================");
            Log.Information("BridgeOne Windows Server 시작");
            Log.Information("Version: 1.0.0 (Phase 1.2.3.5)");
            Log.Information("==============================================");

            // 시스템 정보 로깅
            Log.Debug("OS: {OS}", Environment.OSVersion);
            Log.Debug(".NET Version: {DotNetVersion}", Environment.Version);
            Log.Debug("작업 디렉토리: {WorkingDirectory}", Environment.CurrentDirectory);

            // 테스트 로그 메시지
            Log.Information("로깅 시스템 초기화 완료");

            // HID 장치 관리자 초기화
            using var hidDeviceManager = new HidDeviceManager();
            
            // 장치 연결 이벤트 핸들러 등록
            hidDeviceManager.DeviceConnected += (sender, device) =>
            {
                Log.Information("이벤트: ESP32-S3 HID 장치 연결됨");
                Log.Debug("이벤트 핸들러에서 장치 경로: {Path}", device.DevicePath);
            };
            
            // 장치 분리 이벤트 핸들러 등록
            hidDeviceManager.DeviceDisconnected += (sender, devicePath) =>
            {
                Log.Warning("이벤트: ESP32-S3 HID 장치 분리됨");
                Log.Debug("이벤트 핸들러에서 장치 경로: {Path}", devicePath);
            };
            
            Log.Information("HID 장치 관리자 초기화 완료");
            
            // Input Simulator 초기화
            var inputSimulator = new InputSimulator();
            Log.Information("Input Simulator 초기화 완료");
            
            // Raw Input 핸들러 초기화
            using var rawInputHandler = new RawInputHandler();
            
            // Raw Input 이벤트 핸들러 등록 → InputSimulator로 연결
            rawInputHandler.MouseInputReceived += (sender, e) =>
            {
                // 마우스 이동
                if (e.DeltaX != 0 || e.DeltaY != 0)
                {
                    inputSimulator.SimulateMouseMove(e.DeltaX, e.DeltaY);
                }
                
                // 마우스 버튼 클릭
                if ((e.ButtonFlags & 0x0001) != 0) // RI_MOUSE_LEFT_BUTTON_DOWN
                {
                    inputSimulator.SimulateMouseClick(InputSimulator.MouseButton.Left, true);
                }
                if ((e.ButtonFlags & 0x0002) != 0) // RI_MOUSE_LEFT_BUTTON_UP
                {
                    inputSimulator.SimulateMouseClick(InputSimulator.MouseButton.Left, false);
                }
                if ((e.ButtonFlags & 0x0004) != 0) // RI_MOUSE_RIGHT_BUTTON_DOWN
                {
                    inputSimulator.SimulateMouseClick(InputSimulator.MouseButton.Right, true);
                }
                if ((e.ButtonFlags & 0x0008) != 0) // RI_MOUSE_RIGHT_BUTTON_UP
                {
                    inputSimulator.SimulateMouseClick(InputSimulator.MouseButton.Right, false);
                }
                if ((e.ButtonFlags & 0x0020) != 0) // RI_MOUSE_MIDDLE_BUTTON_DOWN
                {
                    inputSimulator.SimulateMouseClick(InputSimulator.MouseButton.Middle, true);
                }
                if ((e.ButtonFlags & 0x0040) != 0) // RI_MOUSE_MIDDLE_BUTTON_UP
                {
                    inputSimulator.SimulateMouseClick(InputSimulator.MouseButton.Middle, false);
                }
                
                // 휠 이벤트
                if ((e.ButtonFlags & 0x0400) != 0) // RI_MOUSE_WHEEL
                {
                    inputSimulator.SimulateMouseWheel(e.WheelDelta);
                }
            };
            
            rawInputHandler.KeyboardInputReceived += (sender, e) =>
            {
                // 키보드 입력
                inputSimulator.SimulateKeyPress(e.VirtualKey, e.IsKeyDown);
            };
            
            // Raw Input 핸들러 시작
            if (!rawInputHandler.Start())
            {
                Log.Error("Raw Input 핸들러 시작 실패");
                return;
            }
            
            Log.Information("Raw Input 핸들러 시작 완료");
            
            // ========================================
            // Phase 1.2.3.5: Vendor CDC 통신 초기화
            // ========================================
            Log.Information("========================================");
            Log.Information("Phase 1.2.3.5: Vendor CDC 통신 초기화");
            Log.Information("========================================");
            
            // VendorCdcManager 초기화
            using var vendorCdcManager = new VendorCdcManager();
            
            // JSON 응답 수신 이벤트 핸들러 등록
            vendorCdcManager.JsonResponseReceived += (sender, e) =>
            {
                try
                {
                    // 명령 타입 확인
                    if (e.JsonData.TryGetProperty("command", out var commandElement))
                    {
                        string command = commandElement.GetString() ?? "UNKNOWN";
                        Log.Information("CDC 응답 수신: {Command}", command);
                        
                        // 데이터 출력 (있는 경우)
                        if (e.JsonData.TryGetProperty("data", out var dataElement))
                        {
                            Log.Debug("응답 데이터: {Data}", dataElement.ToString());
                        }
                    }
                }
                catch (Exception ex)
                {
                    Log.Error(ex, "JSON 응답 처리 중 오류 발생");
                }
            };
            
            // 연결 상태 변경 이벤트 핸들러 등록
            vendorCdcManager.ConnectionStateChanged += (sender, isConnected) =>
            {
                if (isConnected)
                    Log.Information("CDC 연결됨: {PortName}", vendorCdcManager.PortName);
                else
                    Log.Warning("CDC 연결 해제됨");
            };
            
            // ESP32-S3 CDC 장치 연결 시도
            if (vendorCdcManager.Connect())
            {
                Log.Information("========================================");
                Log.Information("CDC 연결 성공!");
                Log.Information("PING 명령 전송 테스트를 시작합니다...");
                Log.Information("========================================");
                
                // PING 명령 전송 테스트 (5회)
                for (int i = 1; i <= 5; i++)
                {
                    Log.Information("PING 명령 전송 #{Count}...", i);
                    
                    if (vendorCdcManager.SendJsonCommand("PING", new { sequence = i }))
                    {
                        Log.Debug("PING #{Count} 전송 성공", i);
                    }
                    else
                    {
                        Log.Error("PING #{Count} 전송 실패", i);
                    }
                    
                    await Task.Delay(2000); // 2초 대기
                }
                
                Log.Information("========================================");
                Log.Information("PING 테스트 완료");
                Log.Information("========================================");
            }
            else
            {
                Log.Warning("========================================");
                Log.Warning("CDC 연결 실패!");
                Log.Warning("ESP32-S3 장치가 연결되어 있는지 확인하세요.");
                Log.Warning("========================================");
            }
            
            // 장치 감지 및 입력 수신 대기 (추가 20초)
            Log.Information("========================================");
            Log.Information("ESP32-S3 장치를 감지 중입니다.");
            Log.Information("테스트: ESP32-S3를 연결하고 HID 마우스/키보드 입력을 보내보세요.");
            Log.Information("20초 후 자동 종료됩니다...");
            Log.Information("========================================");
            await Task.Delay(20000);
            
            // 현재 연결된 장치 목록 출력
            var connectedDevices = hidDeviceManager.GetConnectedDevices();
            Log.Information("========================================");
            Log.Information("현재 연결된 ESP32-S3 장치: {Count}개", connectedDevices.Count);
            foreach (var device in connectedDevices)
            {
                Log.Information("  - VID: 0x{VID:X4}, PID: 0x{PID:X4}, 경로: {Path}", 
                    device.Attributes.VendorId, 
                    device.Attributes.ProductId, 
                    device.DevicePath);
            }
            Log.Information("========================================");
            
            // Raw Input 핸들러 중지
            rawInputHandler.Stop();
            
            // HID 장치 관리자 중지
            hidDeviceManager.Stop();

            Log.Information("==============================================");
            Log.Information("BridgeOne Windows Server 정상 종료");
            Log.Information("==============================================");
        }
        catch (Exception ex)
        {
            // 예상치 못한 예외 처리
            Log.Fatal(ex, "애플리케이션에서 치명적인 오류 발생");
            Environment.ExitCode = 1;
        }
        finally
        {
            // 로그 버퍼 플러시 및 리소스 정리
            await Log.CloseAndFlushAsync();
        }
    }
}
