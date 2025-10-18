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
            Log.Information("Version: 1.0.0 (Phase 1.2.3.3)");
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
            
            // Raw Input 핸들러 초기화
            using var rawInputHandler = new RawInputHandler();
            
            // Raw Input 이벤트 핸들러 등록
            rawInputHandler.MouseInputReceived += (sender, e) =>
            {
                // 마우스 이동만 로깅 (버튼 이벤트는 별도)
                if (e.DeltaX != 0 || e.DeltaY != 0)
                {
                    Log.Debug("Raw Input 마우스 이동: ({DeltaX}, {DeltaY})", e.DeltaX, e.DeltaY);
                }
                
                // 버튼 이벤트 로깅
                if (e.ButtonFlags != 0)
                {
                    Log.Information("Raw Input 마우스 버튼: Flags=0x{Flags:X4}", e.ButtonFlags);
                }
                
                // 휠 이벤트 로깅
                if ((e.ButtonFlags & 0x0400) != 0) // RI_MOUSE_WHEEL
                {
                    Log.Information("Raw Input 마우스 휠: Delta={Delta}", e.WheelDelta);
                }
            };
            
            rawInputHandler.KeyboardInputReceived += (sender, e) =>
            {
                Log.Information("Raw Input 키보드: VKey=0x{VKey:X2}, ScanCode=0x{ScanCode:X2}, {State}",
                    e.VirtualKey, e.ScanCode, e.IsKeyDown ? "Down" : "Up");
            };
            
            // Raw Input 핸들러 시작
            if (!rawInputHandler.Start())
            {
                Log.Error("Raw Input 핸들러 시작 실패");
                return;
            }
            
            Log.Information("Raw Input 핸들러 시작 완료");
            
            // 장치 감지 및 입력 수신 대기 (30초간 테스트)
            Log.Information("========================================");
            Log.Information("ESP32-S3 장치를 감지 중입니다.");
            Log.Information("테스트: ESP32-S3를 연결하고 HID 마우스/키보드 입력을 보내보세요.");
            Log.Information("30초 후 자동 종료됩니다...");
            Log.Information("========================================");
            await Task.Delay(30000);
            
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
