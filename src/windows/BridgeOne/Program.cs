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
            Log.Information("Version: 1.0.0 (Phase 1.2.3.2)");
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
            
            // 장치 감지 대기 (10초간 테스트)
            Log.Information("ESP32-S3 장치를 감지 중입니다. 10초 후 종료됩니다...");
            Log.Information("테스트를 위해 ESP32-S3를 USB에 연결하거나 분리해보세요.");
            await Task.Delay(10000);
            
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
