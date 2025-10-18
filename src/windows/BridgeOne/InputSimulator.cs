using System.Runtime.InteropServices;
using Serilog;

namespace BridgeOne;

/// <summary>
/// Windows SendInput API를 사용하여 마우스/키보드 입력을 시뮬레이션하는 클래스
/// </summary>
/// <remarks>
/// HID 입력을 실제 Windows 입력 이벤트로 변환하여 시스템 전역에 전달합니다.
/// SendInput API를 사용하여 하드웨어 입력처럼 동작하도록 구현되었습니다.
/// 
/// 참조:
/// - SendInput: https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-sendinput
/// - INPUT structure: https://learn.microsoft.com/en-us/windows/win32/api/winuser/ns-winuser-input
/// - MOUSEINPUT structure: https://learn.microsoft.com/en-us/windows/win32/api/winuser/ns-winuser-mouseinput
/// - KEYBDINPUT structure: https://learn.microsoft.com/en-us/windows/win32/api/winuser/ns-winuser-keybdinput
/// </remarks>
public class InputSimulator
{
    #region Win32 API Declarations (P/Invoke)
    
    // --- Constants ---
    
    /// <summary>
    /// INPUT_MOUSE: 마우스 입력 타입
    /// </summary>
    private const int INPUT_MOUSE = 0;
    
    /// <summary>
    /// INPUT_KEYBOARD: 키보드 입력 타입
    /// </summary>
    private const int INPUT_KEYBOARD = 1;
    
    /// <summary>
    /// MOUSEEVENTF_MOVE: 마우스 이동 (상대 좌표)
    /// </summary>
    private const uint MOUSEEVENTF_MOVE = 0x0001;
    
    /// <summary>
    /// MOUSEEVENTF_LEFTDOWN: 왼쪽 버튼 다운
    /// </summary>
    private const uint MOUSEEVENTF_LEFTDOWN = 0x0002;
    
    /// <summary>
    /// MOUSEEVENTF_LEFTUP: 왼쪽 버튼 업
    /// </summary>
    private const uint MOUSEEVENTF_LEFTUP = 0x0004;
    
    /// <summary>
    /// MOUSEEVENTF_RIGHTDOWN: 오른쪽 버튼 다운
    /// </summary>
    private const uint MOUSEEVENTF_RIGHTDOWN = 0x0008;
    
    /// <summary>
    /// MOUSEEVENTF_RIGHTUP: 오른쪽 버튼 업
    /// </summary>
    private const uint MOUSEEVENTF_RIGHTUP = 0x0010;
    
    /// <summary>
    /// MOUSEEVENTF_MIDDLEDOWN: 중앙 버튼 다운
    /// </summary>
    private const uint MOUSEEVENTF_MIDDLEDOWN = 0x0020;
    
    /// <summary>
    /// MOUSEEVENTF_MIDDLEUP: 중앙 버튼 업
    /// </summary>
    private const uint MOUSEEVENTF_MIDDLEUP = 0x0040;
    
    /// <summary>
    /// MOUSEEVENTF_WHEEL: 수직 휠 회전
    /// </summary>
    private const uint MOUSEEVENTF_WHEEL = 0x0800;
    
    /// <summary>
    /// MOUSEEVENTF_HWHEEL: 수평 휠 회전
    /// </summary>
    private const uint MOUSEEVENTF_HWHEEL = 0x1000;
    
    /// <summary>
    /// KEYEVENTF_EXTENDEDKEY: 확장 키 플래그
    /// </summary>
    private const uint KEYEVENTF_EXTENDEDKEY = 0x0001;
    
    /// <summary>
    /// KEYEVENTF_KEYUP: 키 업 플래그
    /// </summary>
    private const uint KEYEVENTF_KEYUP = 0x0002;
    
    /// <summary>
    /// WHEEL_DELTA: 휠 한 클릭의 기본 델타 값 (120)
    /// </summary>
    private const int WHEEL_DELTA = 120;
    
    // --- Structures ---
    
    /// <summary>
    /// INPUT 구조체: 입력 이벤트
    /// </summary>
    [StructLayout(LayoutKind.Sequential)]
    private struct INPUT
    {
        public int Type;
        public InputUnion Data;
    }
    
    /// <summary>
    /// InputUnion: INPUT 구조체의 Union 부분
    /// </summary>
    [StructLayout(LayoutKind.Explicit)]
    private struct InputUnion
    {
        [FieldOffset(0)]
        public MOUSEINPUT Mouse;
        
        [FieldOffset(0)]
        public KEYBDINPUT Keyboard;
        
        [FieldOffset(0)]
        public HARDWAREINPUT Hardware;
    }
    
    /// <summary>
    /// MOUSEINPUT 구조체: 마우스 입력 데이터
    /// </summary>
    [StructLayout(LayoutKind.Sequential)]
    private struct MOUSEINPUT
    {
        public int dx;
        public int dy;
        public uint mouseData;
        public uint dwFlags;
        public uint time;
        public IntPtr dwExtraInfo;
    }
    
    /// <summary>
    /// KEYBDINPUT 구조체: 키보드 입력 데이터
    /// </summary>
    [StructLayout(LayoutKind.Sequential)]
    private struct KEYBDINPUT
    {
        public ushort wVk;
        public ushort wScan;
        public uint dwFlags;
        public uint time;
        public IntPtr dwExtraInfo;
    }
    
    /// <summary>
    /// HARDWAREINPUT 구조체: 하드웨어 입력 데이터
    /// </summary>
    [StructLayout(LayoutKind.Sequential)]
    private struct HARDWAREINPUT
    {
        public uint uMsg;
        public ushort wParamL;
        public ushort wParamH;
    }
    
    // --- P/Invoke Methods ---
    
    /// <summary>
    /// 입력 이벤트를 시스템에 전송
    /// </summary>
    /// <param name="nInputs">입력 이벤트 개수</param>
    /// <param name="pInputs">입력 이벤트 배열</param>
    /// <param name="cbSize">INPUT 구조체 크기</param>
    /// <returns>성공한 이벤트 개수</returns>
    [DllImport("user32.dll", SetLastError = true)]
    private static extern uint SendInput(
        uint nInputs,
        [MarshalAs(UnmanagedType.LPArray), In] INPUT[] pInputs,
        int cbSize);
    
    #endregion
    
    #region Fields
    
    /// <summary>
    /// 입력 지연시간 측정을 위한 타이머
    /// </summary>
    private System.Diagnostics.Stopwatch _latencyStopwatch = new();
    
    #endregion
    
    #region Public Methods - Mouse
    
    /// <summary>
    /// 마우스 커서를 상대 좌표로 이동시킵니다.
    /// </summary>
    /// <param name="deltaX">X축 이동량 (픽셀)</param>
    /// <param name="deltaY">Y축 이동량 (픽셀)</param>
    /// <returns>시뮬레이션 성공 여부</returns>
    public bool SimulateMouseMove(int deltaX, int deltaY)
    {
        _latencyStopwatch.Restart();
        
        INPUT[] inputs = new INPUT[1];
        
        // 마우스 이동 입력 생성
        inputs[0].Type = INPUT_MOUSE;
        inputs[0].Data.Mouse.dx = deltaX;
        inputs[0].Data.Mouse.dy = deltaY;
        inputs[0].Data.Mouse.mouseData = 0;
        inputs[0].Data.Mouse.dwFlags = MOUSEEVENTF_MOVE;
        inputs[0].Data.Mouse.time = 0;
        inputs[0].Data.Mouse.dwExtraInfo = IntPtr.Zero;
        
        // 입력 전송
        uint result = SendInput(1, inputs, Marshal.SizeOf(typeof(INPUT)));
        
        _latencyStopwatch.Stop();
        
        if (result == 0)
        {
            int error = Marshal.GetLastWin32Error();
            Log.Error("SimulateMouseMove 실패: Win32 Error {Error}", error);
            return false;
        }
        
        Log.Debug("마우스 이동 시뮬레이션: ({DeltaX}, {DeltaY}), 지연시간: {Latency}ms",
            deltaX, deltaY, _latencyStopwatch.Elapsed.TotalMilliseconds);
        
        return true;
    }
    
    /// <summary>
    /// 마우스 버튼 클릭을 시뮬레이션합니다.
    /// </summary>
    /// <param name="button">마우스 버튼 종류</param>
    /// <param name="isDown">버튼 다운(true) 또는 업(false)</param>
    /// <returns>시뮬레이션 성공 여부</returns>
    public bool SimulateMouseClick(MouseButton button, bool isDown)
    {
        _latencyStopwatch.Restart();
        
        INPUT[] inputs = new INPUT[1];
        
        // 마우스 버튼 입력 생성
        inputs[0].Type = INPUT_MOUSE;
        inputs[0].Data.Mouse.dx = 0;
        inputs[0].Data.Mouse.dy = 0;
        inputs[0].Data.Mouse.mouseData = 0;
        inputs[0].Data.Mouse.dwFlags = GetMouseButtonFlag(button, isDown);
        inputs[0].Data.Mouse.time = 0;
        inputs[0].Data.Mouse.dwExtraInfo = IntPtr.Zero;
        
        // 입력 전송
        uint result = SendInput(1, inputs, Marshal.SizeOf(typeof(INPUT)));
        
        _latencyStopwatch.Stop();
        
        if (result == 0)
        {
            int error = Marshal.GetLastWin32Error();
            Log.Error("SimulateMouseClick 실패: Win32 Error {Error}", error);
            return false;
        }
        
        Log.Information("마우스 버튼 시뮬레이션: {Button} {State}, 지연시간: {Latency}ms",
            button, isDown ? "Down" : "Up", _latencyStopwatch.Elapsed.TotalMilliseconds);
        
        return true;
    }
    
    /// <summary>
    /// 마우스 휠 스크롤을 시뮬레이션합니다.
    /// </summary>
    /// <param name="wheelDelta">휠 델타 값 (양수: 위로, 음수: 아래로)</param>
    /// <param name="isHorizontal">수평 스크롤 여부</param>
    /// <returns>시뮬레이션 성공 여부</returns>
    public bool SimulateMouseWheel(short wheelDelta, bool isHorizontal = false)
    {
        _latencyStopwatch.Restart();
        
        INPUT[] inputs = new INPUT[1];
        
        // 마우스 휠 입력 생성
        inputs[0].Type = INPUT_MOUSE;
        inputs[0].Data.Mouse.dx = 0;
        inputs[0].Data.Mouse.dy = 0;
        inputs[0].Data.Mouse.mouseData = (uint)wheelDelta;
        inputs[0].Data.Mouse.dwFlags = isHorizontal ? MOUSEEVENTF_HWHEEL : MOUSEEVENTF_WHEEL;
        inputs[0].Data.Mouse.time = 0;
        inputs[0].Data.Mouse.dwExtraInfo = IntPtr.Zero;
        
        // 입력 전송
        uint result = SendInput(1, inputs, Marshal.SizeOf(typeof(INPUT)));
        
        _latencyStopwatch.Stop();
        
        if (result == 0)
        {
            int error = Marshal.GetLastWin32Error();
            Log.Error("SimulateMouseWheel 실패: Win32 Error {Error}", error);
            return false;
        }
        
        Log.Information("마우스 휠 시뮬레이션: Delta={Delta}, {Direction}, 지연시간: {Latency}ms",
            wheelDelta, isHorizontal ? "Horizontal" : "Vertical", _latencyStopwatch.Elapsed.TotalMilliseconds);
        
        return true;
    }
    
    #endregion
    
    #region Public Methods - Keyboard
    
    /// <summary>
    /// 키보드 키 입력을 시뮬레이션합니다.
    /// </summary>
    /// <param name="vKey">가상 키 코드</param>
    /// <param name="isDown">키 다운(true) 또는 업(false)</param>
    /// <returns>시뮬레이션 성공 여부</returns>
    public bool SimulateKeyPress(ushort vKey, bool isDown)
    {
        _latencyStopwatch.Restart();
        
        INPUT[] inputs = new INPUT[1];
        
        // 키보드 입력 생성
        inputs[0].Type = INPUT_KEYBOARD;
        inputs[0].Data.Keyboard.wVk = vKey;
        inputs[0].Data.Keyboard.wScan = 0;
        inputs[0].Data.Keyboard.dwFlags = isDown ? 0 : KEYEVENTF_KEYUP;
        inputs[0].Data.Keyboard.time = 0;
        inputs[0].Data.Keyboard.dwExtraInfo = IntPtr.Zero;
        
        // 확장 키 여부 확인 (화살표 키, Home, End 등)
        if (IsExtendedKey(vKey))
        {
            inputs[0].Data.Keyboard.dwFlags |= KEYEVENTF_EXTENDEDKEY;
        }
        
        // 입력 전송
        uint result = SendInput(1, inputs, Marshal.SizeOf(typeof(INPUT)));
        
        _latencyStopwatch.Stop();
        
        if (result == 0)
        {
            int error = Marshal.GetLastWin32Error();
            Log.Error("SimulateKeyPress 실패: Win32 Error {Error}", error);
            return false;
        }
        
        Log.Information("키보드 입력 시뮬레이션: VKey=0x{VKey:X2}, {State}, 지연시간: {Latency}ms",
            vKey, isDown ? "Down" : "Up", _latencyStopwatch.Elapsed.TotalMilliseconds);
        
        return true;
    }
    
    #endregion
    
    #region Private Helper Methods
    
    /// <summary>
    /// 마우스 버튼과 상태에 따른 플래그를 반환합니다.
    /// </summary>
    /// <param name="button">마우스 버튼</param>
    /// <param name="isDown">버튼 다운 여부</param>
    /// <returns>MOUSEEVENTF 플래그</returns>
    private uint GetMouseButtonFlag(MouseButton button, bool isDown)
    {
        return button switch
        {
            MouseButton.Left => isDown ? MOUSEEVENTF_LEFTDOWN : MOUSEEVENTF_LEFTUP,
            MouseButton.Right => isDown ? MOUSEEVENTF_RIGHTDOWN : MOUSEEVENTF_RIGHTUP,
            MouseButton.Middle => isDown ? MOUSEEVENTF_MIDDLEDOWN : MOUSEEVENTF_MIDDLEUP,
            _ => 0
        };
    }
    
    /// <summary>
    /// 확장 키 여부를 판별합니다.
    /// </summary>
    /// <param name="vKey">가상 키 코드</param>
    /// <returns>확장 키 여부</returns>
    private bool IsExtendedKey(ushort vKey)
    {
        // 확장 키 목록 (화살표 키, Home, End, Insert, Delete, Page Up, Page Down 등)
        return vKey switch
        {
            0x21 => true, // VK_PRIOR (Page Up)
            0x22 => true, // VK_NEXT (Page Down)
            0x23 => true, // VK_END
            0x24 => true, // VK_HOME
            0x25 => true, // VK_LEFT (Left Arrow)
            0x26 => true, // VK_UP (Up Arrow)
            0x27 => true, // VK_RIGHT (Right Arrow)
            0x28 => true, // VK_DOWN (Down Arrow)
            0x2D => true, // VK_INSERT
            0x2E => true, // VK_DELETE
            0x5B => true, // VK_LWIN (Left Windows Key)
            0x5C => true, // VK_RWIN (Right Windows Key)
            0x5D => true, // VK_APPS (Context Menu Key)
            _ => false
        };
    }
    
    #endregion
    
    #region Nested Types
    
    /// <summary>
    /// 마우스 버튼 종류
    /// </summary>
    public enum MouseButton
    {
        /// <summary>왼쪽 버튼</summary>
        Left,
        
        /// <summary>오른쪽 버튼</summary>
        Right,
        
        /// <summary>중앙 버튼</summary>
        Middle
    }
    
    #endregion
}

