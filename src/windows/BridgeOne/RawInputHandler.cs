using System.Runtime.InteropServices;
using Serilog;

namespace BridgeOne;

/// <summary>
/// Windows Raw Input API를 사용하여 HID 마우스/키보드 입력을 수신하는 핸들러
/// </summary>
/// <remarks>
/// Raw Input API를 통해 ESP32-S3 HID 장치의 마우스/키보드 입력을 저수준에서 직접 수신합니다.
/// RIDEV_INPUTSINK 플래그를 사용하여 백그라운드에서도 입력을 수신할 수 있습니다.
/// 
/// 참조:
/// - RegisterRawInputDevices: https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-registerrawinputdevices
/// - GetRawInputData: https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-getrawinputdata
/// </remarks>
public class RawInputHandler : IDisposable
{
    #region Win32 API Declarations (P/Invoke)
    
    // --- Constants ---
    
    /// <summary>
    /// Generic Page (HID Usage Page 0x01)
    /// </summary>
    private const ushort HID_USAGE_PAGE_GENERIC = 0x01;
    
    /// <summary>
    /// Mouse (HID Usage 0x02)
    /// </summary>
    private const ushort HID_USAGE_GENERIC_MOUSE = 0x02;
    
    /// <summary>
    /// Keyboard (HID Usage 0x06)
    /// </summary>
    private const ushort HID_USAGE_GENERIC_KEYBOARD = 0x06;
    
    /// <summary>
    /// RIDEV_INPUTSINK: 윈도우가 포커스를 잃어도 입력을 수신
    /// </summary>
    private const int RIDEV_INPUTSINK = 0x00000100;
    
    /// <summary>
    /// WM_INPUT 메시지
    /// </summary>
    private const int WM_INPUT = 0x00FF;
    
    /// <summary>
    /// RID_INPUT: GetRawInputData에서 입력 데이터 가져오기
    /// </summary>
    private const int RID_INPUT = 0x10000003;
    
    /// <summary>
    /// RIM_TYPEMOUSE: 마우스 입력
    /// </summary>
    private const int RIM_TYPEMOUSE = 0;
    
    /// <summary>
    /// RIM_TYPEKEYBOARD: 키보드 입력
    /// </summary>
    private const int RIM_TYPEKEYBOARD = 1;
    
    /// <summary>
    /// RI_MOUSE_LEFT_BUTTON_DOWN: 마우스 왼쪽 버튼 다운
    /// </summary>
    private const ushort RI_MOUSE_LEFT_BUTTON_DOWN = 0x0001;
    
    /// <summary>
    /// RI_MOUSE_LEFT_BUTTON_UP: 마우스 왼쪽 버튼 업
    /// </summary>
    private const ushort RI_MOUSE_LEFT_BUTTON_UP = 0x0002;
    
    /// <summary>
    /// RI_MOUSE_RIGHT_BUTTON_DOWN: 마우스 오른쪽 버튼 다운
    /// </summary>
    private const ushort RI_MOUSE_RIGHT_BUTTON_DOWN = 0x0004;
    
    /// <summary>
    /// RI_MOUSE_RIGHT_BUTTON_UP: 마우스 오른쪽 버튼 업
    /// </summary>
    private const ushort RI_MOUSE_RIGHT_BUTTON_UP = 0x0008;
    
    /// <summary>
    /// RI_MOUSE_WHEEL: 마우스 휠 움직임
    /// </summary>
    private const ushort RI_MOUSE_WHEEL = 0x0400;
    
    // --- Structures ---
    
    /// <summary>
    /// RAWINPUTDEVICE 구조체: Raw Input 장치 등록 정보
    /// </summary>
    [StructLayout(LayoutKind.Sequential)]
    private struct RAWINPUTDEVICE
    {
        public ushort UsagePage;
        public ushort Usage;
        public int Flags;
        public IntPtr Target;
    }
    
    /// <summary>
    /// RAWINPUTHEADER 구조체: Raw Input 헤더
    /// </summary>
    [StructLayout(LayoutKind.Sequential)]
    private struct RAWINPUTHEADER
    {
        public int Type;
        public int Size;
        public IntPtr Device;
        public IntPtr wParam;
    }
    
    /// <summary>
    /// RAWMOUSE 구조체: 마우스 입력 데이터
    /// </summary>
    [StructLayout(LayoutKind.Sequential)]
    private struct RAWMOUSE
    {
        public ushort Flags;
        public ushort ButtonFlags;
        public ushort ButtonData;
        public uint RawButtons;
        public int LastX;
        public int LastY;
        public uint ExtraInformation;
    }
    
    /// <summary>
    /// RAWKEYBOARD 구조체: 키보드 입력 데이터
    /// </summary>
    [StructLayout(LayoutKind.Sequential)]
    private struct RAWKEYBOARD
    {
        public ushort MakeCode;
        public ushort Flags;
        public ushort Reserved;
        public ushort VKey;
        public uint Message;
        public uint ExtraInformation;
    }
    
    /// <summary>
    /// RAWINPUT 구조체: Raw Input 데이터 (Union 타입)
    /// </summary>
    [StructLayout(LayoutKind.Explicit)]
    private struct RAWINPUT
    {
        [FieldOffset(0)]
        public RAWINPUTHEADER Header;
        
        [FieldOffset(24)]
        public RAWMOUSE Mouse;
        
        [FieldOffset(24)]
        public RAWKEYBOARD Keyboard;
    }
    
    // --- P/Invoke Methods ---
    
    /// <summary>
    /// Raw Input 장치 등록
    /// </summary>
    [DllImport("user32.dll", SetLastError = true)]
    private static extern bool RegisterRawInputDevices(
        [MarshalAs(UnmanagedType.LPArray, SizeParamIndex = 1)] RAWINPUTDEVICE[] pRawInputDevices,
        int uiNumDevices,
        int cbSize);
    
    /// <summary>
    /// Raw Input 데이터 가져오기
    /// </summary>
    [DllImport("user32.dll", SetLastError = true)]
    private static extern int GetRawInputData(
        IntPtr hRawInput,
        int uiCommand,
        out RAWINPUT pData,
        ref int pcbSize,
        int cbSizeHeader);
    
    /// <summary>
    /// 메시지 루프에서 메시지 가져오기
    /// </summary>
    [DllImport("user32.dll")]
    private static extern bool GetMessage(out MSG lpMsg, IntPtr hWnd, uint wMsgFilterMin, uint wMsgFilterMax);
    
    /// <summary>
    /// 메시지 변환
    /// </summary>
    [DllImport("user32.dll")]
    private static extern bool TranslateMessage([In] ref MSG lpMsg);
    
    /// <summary>
    /// 메시지 디스패치
    /// </summary>
    [DllImport("user32.dll")]
    private static extern IntPtr DispatchMessage([In] ref MSG lpmsg);
    
    /// <summary>
    /// 숨겨진 메시지 전용 윈도우 생성
    /// </summary>
    [DllImport("user32.dll", SetLastError = true)]
    private static extern IntPtr CreateWindowEx(
        int dwExStyle,
        string lpClassName,
        string lpWindowName,
        int dwStyle,
        int x,
        int y,
        int nWidth,
        int nHeight,
        IntPtr hWndParent,
        IntPtr hMenu,
        IntPtr hInstance,
        IntPtr lpParam);
    
    /// <summary>
    /// 윈도우 메시지 처리 콜백 설정
    /// </summary>
    [DllImport("user32.dll", SetLastError = true)]
    private static extern IntPtr DefWindowProc(IntPtr hWnd, uint uMsg, IntPtr wParam, IntPtr lParam);
    
    /// <summary>
    /// 윈도우 파괴
    /// </summary>
    [DllImport("user32.dll", SetLastError = true)]
    private static extern bool DestroyWindow(IntPtr hWnd);
    
    /// <summary>
    /// 메시지 루프 종료
    /// </summary>
    [DllImport("user32.dll")]
    private static extern void PostQuitMessage(int nExitCode);
    
    /// <summary>
    /// MSG 구조체: Windows 메시지
    /// </summary>
    [StructLayout(LayoutKind.Sequential)]
    private struct MSG
    {
        public IntPtr hwnd;
        public uint message;
        public IntPtr wParam;
        public IntPtr lParam;
        public uint time;
        public POINT pt;
    }
    
    /// <summary>
    /// POINT 구조체: 2D 좌표
    /// </summary>
    [StructLayout(LayoutKind.Sequential)]
    private struct POINT
    {
        public int X;
        public int Y;
    }
    
    /// <summary>
    /// 윈도우 메시지 처리 콜백 델리게이트
    /// </summary>
    private delegate IntPtr WndProc(IntPtr hWnd, uint msg, IntPtr wParam, IntPtr lParam);
    
    #endregion
    
    #region Fields
    
    /// <summary>
    /// 메시지 루프 실행 중 여부
    /// </summary>
    private bool _isRunning = false;
    
    /// <summary>
    /// 메시지 루프 스레드
    /// </summary>
    private Thread? _messageLoopThread;
    
    /// <summary>
    /// 메시지 전용 윈도우 핸들
    /// </summary>
    private IntPtr _windowHandle = IntPtr.Zero;
    
    /// <summary>
    /// 리소스 해제 여부
    /// </summary>
    private bool _disposed = false;
    
    #endregion
    
    #region Events
    
    /// <summary>
    /// 마우스 입력 이벤트 인자
    /// </summary>
    public class MouseInputEventArgs : EventArgs
    {
        public int DeltaX { get; set; }
        public int DeltaY { get; set; }
        public ushort ButtonFlags { get; set; }
        public short WheelDelta { get; set; }
    }
    
    /// <summary>
    /// 키보드 입력 이벤트 인자
    /// </summary>
    public class KeyboardInputEventArgs : EventArgs
    {
        public ushort VirtualKey { get; set; }
        public ushort ScanCode { get; set; }
        public bool IsKeyDown { get; set; }
    }
    
    /// <summary>
    /// 마우스 입력 수신 이벤트
    /// </summary>
    public event EventHandler<MouseInputEventArgs>? MouseInputReceived;
    
    /// <summary>
    /// 키보드 입력 수신 이벤트
    /// </summary>
    public event EventHandler<KeyboardInputEventArgs>? KeyboardInputReceived;
    
    #endregion
    
    #region Public Methods
    
    /// <summary>
    /// Raw Input 핸들러를 시작합니다.
    /// </summary>
    /// <returns>시작 성공 여부</returns>
    public bool Start()
    {
        if (_isRunning)
        {
            Log.Warning("RawInputHandler가 이미 실행 중입니다.");
            return false;
        }
        
        Log.Information("RawInputHandler 시작 중...");
        
        // 메시지 루프를 별도 스레드에서 실행
        _isRunning = true;
        _messageLoopThread = new Thread(MessageLoopThreadProc)
        {
            Name = "RawInputMessageLoop",
            IsBackground = true
        };
        _messageLoopThread.Start();
        
        // 초기화 완료 대기 (최대 3초)
        for (int i = 0; i < 30; i++)
        {
            if (_windowHandle != IntPtr.Zero)
            {
                Log.Information("RawInputHandler 시작 완료");
                return true;
            }
            Thread.Sleep(100);
        }
        
        Log.Error("RawInputHandler 시작 실패: 타임아웃");
        return false;
    }
    
    /// <summary>
    /// Raw Input 핸들러를 중지합니다.
    /// </summary>
    public void Stop()
    {
        if (!_isRunning)
        {
            return;
        }
        
        Log.Information("RawInputHandler 중지 중...");
        
        _isRunning = false;
        
        // 메시지 루프 종료
        if (_windowHandle != IntPtr.Zero)
        {
            PostQuitMessage(0);
        }
        
        // 스레드 종료 대기 (최대 3초)
        _messageLoopThread?.Join(3000);
        
        Log.Information("RawInputHandler 중지 완료");
    }
    
    #endregion
    
    #region Private Methods
    
    /// <summary>
    /// 메시지 루프 스레드 프로시저
    /// </summary>
    private void MessageLoopThreadProc()
    {
        try
        {
            Log.Debug("메시지 루프 스레드 시작");
            
            // 숨겨진 메시지 전용 윈도우 생성
            // 참고: Raw Input을 위해서는 윈도우 핸들이 필요합니다
            _windowHandle = CreateMessageWindow();
            if (_windowHandle == IntPtr.Zero)
            {
                Log.Error("메시지 윈도우 생성 실패");
                return;
            }
            
            Log.Debug("메시지 윈도우 생성 완료: 0x{Handle:X}", _windowHandle);
            
            // Raw Input 장치 등록
            if (!RegisterRawInput())
            {
                Log.Error("Raw Input 등록 실패");
                return;
            }
            
            Log.Information("Raw Input 등록 완료 (마우스, 키보드)");
            
            // 메시지 루프 실행
            MSG msg;
            while (_isRunning && GetMessage(out msg, IntPtr.Zero, 0, 0))
            {
                // WM_INPUT 메시지 처리
                if (msg.message == WM_INPUT)
                {
                    ProcessRawInput(msg.lParam);
                }
                
                TranslateMessage(ref msg);
                DispatchMessage(ref msg);
            }
            
            Log.Debug("메시지 루프 종료");
        }
        catch (Exception ex)
        {
            Log.Error(ex, "메시지 루프에서 예외 발생");
        }
        finally
        {
            // 윈도우 정리
            if (_windowHandle != IntPtr.Zero)
            {
                DestroyWindow(_windowHandle);
                _windowHandle = IntPtr.Zero;
            }
        }
    }
    
    /// <summary>
    /// 메시지 전용 윈도우를 생성합니다.
    /// </summary>
    /// <returns>윈도우 핸들</returns>
    private IntPtr CreateMessageWindow()
    {
        // 간단한 메시지 전용 윈도우 생성
        // "Message" 클래스는 시스템에서 제공하는 메시지 전용 윈도우 클래스입니다
        const string MESSAGE_ONLY_WINDOW_CLASS = "Message";
        
        return CreateWindowEx(
            dwExStyle: 0,
            lpClassName: MESSAGE_ONLY_WINDOW_CLASS,
            lpWindowName: "RawInputMessageWindow",
            dwStyle: 0,
            x: 0,
            y: 0,
            nWidth: 0,
            nHeight: 0,
            hWndParent: new IntPtr(-3), // HWND_MESSAGE: 메시지 전용 윈도우
            hMenu: IntPtr.Zero,
            hInstance: IntPtr.Zero,
            lpParam: IntPtr.Zero);
    }
    
    /// <summary>
    /// Raw Input 장치를 등록합니다.
    /// </summary>
    /// <returns>등록 성공 여부</returns>
    private bool RegisterRawInput()
    {
        // 마우스와 키보드를 등록
        RAWINPUTDEVICE[] devices = new RAWINPUTDEVICE[2];
        
        // 마우스 (UsagePage=0x01, Usage=0x02)
        devices[0].UsagePage = HID_USAGE_PAGE_GENERIC;
        devices[0].Usage = HID_USAGE_GENERIC_MOUSE;
        devices[0].Flags = RIDEV_INPUTSINK; // 백그라운드에서도 입력 수신
        devices[0].Target = _windowHandle;
        
        // 키보드 (UsagePage=0x01, Usage=0x06)
        devices[1].UsagePage = HID_USAGE_PAGE_GENERIC;
        devices[1].Usage = HID_USAGE_GENERIC_KEYBOARD;
        devices[1].Flags = RIDEV_INPUTSINK; // 백그라운드에서도 입력 수신
        devices[1].Target = _windowHandle;
        
        bool result = RegisterRawInputDevices(
            devices,
            devices.Length,
            Marshal.SizeOf(typeof(RAWINPUTDEVICE)));
        
        if (!result)
        {
            int error = Marshal.GetLastWin32Error();
            Log.Error("RegisterRawInputDevices 실패: Win32 Error {Error}", error);
            return false;
        }
        
        return true;
    }
    
    /// <summary>
    /// WM_INPUT 메시지를 처리하여 Raw Input 데이터를 추출합니다.
    /// </summary>
    /// <param name="lParam">WM_INPUT 메시지의 lParam (RAWINPUT 핸들)</param>
    private void ProcessRawInput(IntPtr lParam)
    {
        try
        {
            // GetRawInputData로 입력 데이터 크기 가져오기
            int size = Marshal.SizeOf(typeof(RAWINPUT));
            
            // 입력 데이터 읽기
            RAWINPUT rawInput;
            int result = GetRawInputData(
                lParam,
                RID_INPUT,
                out rawInput,
                ref size,
                Marshal.SizeOf(typeof(RAWINPUTHEADER)));
            
            if (result == -1)
            {
                int error = Marshal.GetLastWin32Error();
                Log.Error("GetRawInputData 실패: Win32 Error {Error}", error);
                return;
            }
            
            // 입력 타입에 따라 처리
            if (rawInput.Header.Type == RIM_TYPEMOUSE)
            {
                ProcessMouseInput(rawInput.Mouse);
            }
            else if (rawInput.Header.Type == RIM_TYPEKEYBOARD)
            {
                ProcessKeyboardInput(rawInput.Keyboard);
            }
        }
        catch (Exception ex)
        {
            Log.Error(ex, "Raw Input 처리 중 예외 발생");
        }
    }
    
    /// <summary>
    /// 마우스 입력을 처리합니다.
    /// </summary>
    /// <param name="mouse">마우스 입력 데이터</param>
    private void ProcessMouseInput(RAWMOUSE mouse)
    {
        // 마우스 이동량
        int deltaX = mouse.LastX;
        int deltaY = mouse.LastY;
        
        // 버튼 플래그
        ushort buttonFlags = mouse.ButtonFlags;
        
        // 휠 델타 (상위 16비트에 저장)
        short wheelDelta = (short)mouse.ButtonData;
        
        // 로깅 (디버그용)
        if (deltaX != 0 || deltaY != 0)
        {
            Log.Debug("마우스 이동: DeltaX={DeltaX}, DeltaY={DeltaY}", deltaX, deltaY);
        }
        
        if (buttonFlags != 0)
        {
            string buttonAction = GetMouseButtonAction(buttonFlags);
            Log.Debug("마우스 버튼: {Action}", buttonAction);
        }
        
        if ((buttonFlags & RI_MOUSE_WHEEL) != 0)
        {
            Log.Debug("마우스 휠: Delta={Delta}", wheelDelta);
        }
        
        // 이벤트 발생
        MouseInputReceived?.Invoke(this, new MouseInputEventArgs
        {
            DeltaX = deltaX,
            DeltaY = deltaY,
            ButtonFlags = buttonFlags,
            WheelDelta = wheelDelta
        });
    }
    
    /// <summary>
    /// 키보드 입력을 처리합니다.
    /// </summary>
    /// <param name="keyboard">키보드 입력 데이터</param>
    private void ProcessKeyboardInput(RAWKEYBOARD keyboard)
    {
        // 가상 키 코드
        ushort vkey = keyboard.VKey;
        
        // 스캔 코드
        ushort scanCode = keyboard.MakeCode;
        
        // 키 다운/업 여부 (Flags의 0번 비트: 0=Down, 1=Up)
        bool isKeyDown = (keyboard.Flags & 0x01) == 0;
        
        // 로깅
        Log.Debug("키보드 입력: VKey=0x{VKey:X2}, ScanCode=0x{ScanCode:X2}, {State}",
            vkey, scanCode, isKeyDown ? "Down" : "Up");
        
        // 이벤트 발생
        KeyboardInputReceived?.Invoke(this, new KeyboardInputEventArgs
        {
            VirtualKey = vkey,
            ScanCode = scanCode,
            IsKeyDown = isKeyDown
        });
    }
    
    /// <summary>
    /// 마우스 버튼 플래그를 문자열로 변환합니다 (디버깅용).
    /// </summary>
    /// <param name="buttonFlags">버튼 플래그</param>
    /// <returns>버튼 액션 문자열</returns>
    private string GetMouseButtonAction(ushort buttonFlags)
    {
        List<string> actions = new();
        
        if ((buttonFlags & RI_MOUSE_LEFT_BUTTON_DOWN) != 0)
            actions.Add("LeftDown");
        if ((buttonFlags & RI_MOUSE_LEFT_BUTTON_UP) != 0)
            actions.Add("LeftUp");
        if ((buttonFlags & RI_MOUSE_RIGHT_BUTTON_DOWN) != 0)
            actions.Add("RightDown");
        if ((buttonFlags & RI_MOUSE_RIGHT_BUTTON_UP) != 0)
            actions.Add("RightUp");
        
        return actions.Count > 0 ? string.Join(", ", actions) : "Unknown";
    }
    
    #endregion
    
    #region IDisposable Implementation
    
    /// <summary>
    /// 리소스를 해제합니다.
    /// </summary>
    /// <param name="disposing">관리되는 리소스 해제 여부</param>
    protected virtual void Dispose(bool disposing)
    {
        if (!_disposed)
        {
            if (disposing)
            {
                // 관리되는 리소스 해제
                Stop();
            }
            
            _disposed = true;
        }
    }
    
    /// <summary>
    /// 리소스를 해제합니다.
    /// </summary>
    public void Dispose()
    {
        Dispose(true);
        GC.SuppressFinalize(this);
    }
    
    /// <summary>
    /// 소멸자
    /// </summary>
    ~RawInputHandler()
    {
        Dispose(false);
    }
    
    #endregion
}

