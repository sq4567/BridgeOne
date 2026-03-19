using CommunityToolkit.Mvvm.ComponentModel;

namespace BridgeOne.ViewModels;

/// <summary>
/// 앱 전체 상태를 관리하는 최상위 ViewModel.
/// 하위 ViewModel을 생성 및 관리하며, MainWindow의 DataContext로 사용됩니다.
/// </summary>
public sealed class MainViewModel : ObservableObject
{
    /// <summary>ESP32-S3 연결 상태 ViewModel</summary>
    public ConnectionViewModel Connection { get; }

    public MainViewModel(ConnectionViewModel connectionViewModel)
    {
        Connection = connectionViewModel;
    }
}
