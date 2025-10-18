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
            Log.Information("Version: 1.0.0 (Phase 1.2.3.1)");
            Log.Information("==============================================");

            // 시스템 정보 로깅
            Log.Debug("OS: {OS}", Environment.OSVersion);
            Log.Debug(".NET Version: {DotNetVersion}", Environment.Version);
            Log.Debug("작업 디렉토리: {WorkingDirectory}", Environment.CurrentDirectory);

            // 테스트 로그 메시지
            Log.Information("로깅 시스템 초기화 완료");
            Log.Debug("Debug 레벨 로그 테스트");
            Log.Warning("Warning 레벨 로그 테스트");

            // 비동기 작업 시뮬레이션
            await Task.Delay(1000);
            Log.Information("비동기 작업 테스트 완료");

            // 실제 서버 로직은 이후 단계에서 구현 예정
            Log.Information("서버 초기화 완료. 종료 대기 중...");
            await Task.Delay(2000);

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
