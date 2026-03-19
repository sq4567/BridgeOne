using System.Windows;
using Microsoft.Extensions.DependencyInjection;
using BridgeOne.Protocol;
using BridgeOne.Services;
using BridgeOne.ViewModels;

namespace BridgeOne
{
    /// <summary>
    /// DI 컨테이너를 구성하고 앱 생명주기를 관리합니다.
    /// StartupUri 대신 OnStartup에서 직접 MainWindow를 생성하여
    /// DI를 통한 DataContext 주입을 지원합니다.
    /// </summary>
    public partial class App : Application
    {
        private ServiceProvider? _serviceProvider;

        protected override void OnStartup(StartupEventArgs e)
        {
            base.OnStartup(e);

            // DI 컨테이너 구성
            var services = new ServiceCollection();

            // 서비스 계층 (등록 순서 = 해제 역순)
            services.AddSingleton<CdcConnectionService>();
            services.AddSingleton<VendorCdcProtocol>();

            // ViewModel 계층
            services.AddSingleton<ConnectionViewModel>();
            services.AddSingleton<MainViewModel>();

            _serviceProvider = services.BuildServiceProvider();

            // MainWindow 생성 및 DataContext 주입
            var mainWindow = new MainWindow
            {
                DataContext = _serviceProvider.GetRequiredService<MainViewModel>()
            };
            mainWindow.Show();

            // 창이 표시된 후 연결 서비스 시작 (핫플러그 모니터링 + 초기 스캔)
            var connectionService = _serviceProvider.GetRequiredService<CdcConnectionService>();
            connectionService.Start();
        }

        protected override void OnExit(ExitEventArgs e)
        {
            if (_serviceProvider != null)
            {
                // 연결 서비스 명시적 중지 (핫플러그 워처 정리)
                _serviceProvider.GetService<CdcConnectionService>()?.Stop();

                // ServiceProvider.Dispose()가 IDisposable 싱글톤을 역순으로 해제
                // ConnectionViewModel → VendorCdcProtocol → CdcConnectionService
                _serviceProvider.Dispose();
            }

            base.OnExit(e);
        }
    }
}
