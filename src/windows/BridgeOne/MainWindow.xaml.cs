using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using Wpf.Ui.Controls;

namespace BridgeOne
{
    /// <summary>
    /// MainWindow.xaml에 대한 상호 작용 논리입니다.
    /// WPF-UI의 FluentWindow를 사용하여 Fluent Design System을 적용한 메인 윈도우입니다.
    /// Mica 배경 효과를 적용하고 다크 테마로 고정되며, 커스텀 타이틀 바를 제공합니다.
    /// DataContext는 App.xaml.cs의 DI 컨테이너에서 MainViewModel로 주입됩니다.
    /// </summary>
    public partial class MainWindow : FluentWindow
    {
        public MainWindow()
        {
            InitializeComponent();

            // Mica 배경 효과 설정
            Loaded += (sender, args) =>
            {
                Wpf.Ui.Appearance.SystemThemeWatcher.Watch(
                    this,
                    Wpf.Ui.Controls.WindowBackdropType.Mica
                );
            };
        }

        // ==================== 타이틀 바 이벤트 ====================

        private void TitleBar_MouseDown(object sender, MouseButtonEventArgs e)
        {
            if (e.ClickCount == 2)
            {
                WindowState = WindowState == WindowState.Maximized
                    ? WindowState.Normal
                    : WindowState.Maximized;
                return;
            }

            if (e.LeftButton == MouseButtonState.Pressed)
            {
                DragMove();
            }
        }

        private void MinimizeButton_Click(object sender, RoutedEventArgs e)
        {
            WindowState = WindowState.Minimized;
        }

        private void MaximizeButton_Click(object sender, RoutedEventArgs e)
        {
            if (WindowState == WindowState.Maximized)
            {
                WindowState = WindowState.Normal;
                if (MaximizeIcon != null)
                    MaximizeIcon.Text = "\u2610"; // ☐
            }
            else
            {
                WindowState = WindowState.Maximized;
                if (MaximizeIcon != null)
                    MaximizeIcon.Text = "\u274F"; // ❏
            }
        }

        private void CloseButton_Click(object sender, RoutedEventArgs e)
        {
            Application.Current.Shutdown();
        }

        // ==================== 디버그 로그 자동 스크롤 ====================

        /// <summary>
        /// 디버그 로그 텍스트가 변경되면 자동으로 맨 아래로 스크롤합니다.
        /// </summary>
        private void DebugLogTextBox_TextChanged(object sender, TextChangedEventArgs e)
        {
            if (sender is System.Windows.Controls.TextBox textBox)
            {
                textBox.ScrollToEnd();
            }
        }
    }
}
