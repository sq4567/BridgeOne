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
    /// Mica 배경 효과를 적용하고 다크 테마로 고정되며, 커스텀 타이틀 바를 제공합니다.
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

        /// <summary>
        /// 타이틀 바 드래그 이벤트 핸들러입니다.
        /// 타이틀 바를 마우스로 드래그하여 창을 이동할 수 있습니다.
        /// </summary>
        private void TitleBar_MouseDown(object sender, MouseButtonEventArgs e)
        {
            // 더블 클릭 시 창 최대/복원
            if (e.ClickCount == 2)
            {
                WindowState = WindowState == WindowState.Maximized 
                    ? WindowState.Normal 
                    : WindowState.Maximized;
                return;
            }

            // 싱글 클릭 드래그로 창 이동
            if (e.LeftButton == MouseButtonState.Pressed)
            {
                DragMove();
            }
        }

        /// <summary>
        /// 최소화 버튼 클릭 이벤트 핸들러입니다.
        /// 창을 최소화 상태로 변경합니다.
        /// </summary>
        private void MinimizeButton_Click(object sender, RoutedEventArgs e)
        {
            WindowState = WindowState.Minimized;
        }

        /// <summary>
        /// 최대화 버튼 클릭 이벤트 핸들러입니다.
        /// 창을 최대화/복원 상태로 토글합니다.
        /// 최대화 아이콘도 상태에 따라 업데이트됩니다.
        /// </summary>
        private void MaximizeButton_Click(object sender, RoutedEventArgs e)
        {
            // 창 상태 토글 (최대화 ↔ 복원)
            if (WindowState == WindowState.Maximized)
            {
                WindowState = WindowState.Normal;
                // 복원 상태 아이콘
                if (MaximizeIcon != null)
                {
                    MaximizeIcon.Text = "☐";
                }
            }
            else
            {
                WindowState = WindowState.Maximized;
                // 최대화 상태 아이콘
                if (MaximizeIcon != null)
                {
                    MaximizeIcon.Text = "❏";
                }
            }
        }

        /// <summary>
        /// 종료 버튼 클릭 이벤트 핸들러입니다.
        /// 애플리케이션을 종료합니다.
        /// </summary>
        private void CloseButton_Click(object sender, RoutedEventArgs e)
        {
            Application.Current.Shutdown();
        }
    }
}