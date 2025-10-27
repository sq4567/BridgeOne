using System.Text;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using System.Windows.Shapes;
using Wpf.Ui.Controls;

namespace BridgeOne
{
    /// <summary>
    /// MainWindow.xaml에 대한 상호 작용 논리입니다.
    /// WPF-UI의 FluentWindow를 사용하여 Fluent Design System을 적용한 메인 윈도우입니다.
    /// Mica 배경 효과를 적용하고 다크 테마로 고정됩니다.
    /// </summary>
    public partial class MainWindow : FluentWindow
    {
        public MainWindow()
        {
            InitializeComponent();
            
            // 창이 로드된 후 Mica 배경 효과 설정
            Loaded += (sender, args) =>
            {
                Wpf.Ui.Appearance.SystemThemeWatcher.Watch(
                    this,
                    Wpf.Ui.Controls.WindowBackdropType.Mica
                );
            };
        }
    }
}