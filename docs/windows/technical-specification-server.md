---
title: "BridgeOne Windows 서버 기술 가이드"
description: "Windows 서버 프로그램의 기술적 아키텍처, 핵심 기능 명세 및 시스템 설계"
tags: ["windows-server", "architecture", "specification", "system-design"]
version: "v0.9"
owner: "Chatterbones"
updated: "2025-10-22"
note: "본 문서에 존재하는 모든 상수값 및 설정값은 초기 값으로, 확정된 고정값이 아님"
---

# BridgeOne Windows 서버 기술 가이드

> **상세 아키텍처**: 전체 시스템 아키텍처는 [`technical-specification.md` §3]를 참조하세요.
> **용어 참조**: Windows 플랫폼 관련 모든 용어는 `technical-specification.md`의 §6.4 Windows 플랫폼 용어집을 참조하세요.

## 1. 서버 소개

**개요**: BridgeOne Windows 서버는 ESP32-S3 동글을 통해 수신되는 고급 입력 명령을 Windows 환경에서 처리하는 핵심 구성요소입니다.

**핵심 특징**:
- **고급 기능 제공**: 멀티 커서 등 단순 HID를 넘어서는 확장 기능 지원
- **보안 환경 적응**: EDR/방화벽 등으로 차단될 수 있는 환경을 고려한 설계
- **저지연 처리**: 전체 입력 지연 목표 50ms 달성에 기여

## 2. 기술 스택 및 아키텍처

### 2.1 USB 인터페이스 계약

**Windows 서버가 인식하는 USB 인터페이스**:

Windows 서버는 ESP32-S3 동글의 다음 USB 인터페이스를 고정된 순서로 인식합니다:
```
Interface 0: HID Boot Keyboard (0x03/0x01/0x01)
Interface 1: HID Boot Mouse    (0x03/0x01/0x02)
Interface 2: CDC-ACM Comm      (0x02/0x02/0x00)
Interface 3: CDC-ACM Data      (0x0A/0x00/0x00)
```

**Windows 서버의 인터페이스 식별 방식**:
- USB 열거 시 인터페이스 번호로 각 기능 식별
- HID 인터페이스는 기본 드라이버 사용 (별도 핸들 불필요)
- Vendor CDC는 인터페이스 2/3을 통해 직접 통신

**프로토콜 계약**:
- **Vendor CDC 메시지**: 0xFF 헤더 + command + length + JSON 페이로드 + CRC16 체크섬 (Little-Endian)
- **최대 페이로드 크기**: 448 bytes
- **바이트 순서**: Little-Endian (length, checksum 필드)

### 2.2 기술 스택 선택 원칙

**플랫폼 기반 기술**:
- **.NET 8+ (Long Term Support)**: 크로스 플랫폼 호환성 및 성능 최적화
- **Windows API 통합**: 시스템 레벨 커서 제어 및 입력 처리
- **Vendor CDC 통신**: ESP32-S3 동글과의 양방향 데이터 교환
- **Windows Service 아키텍처**: 백그라운드 실행 및 시스템 통합

**핵심 기술 요구사항**:
- 실시간 USB 통신 처리
- 시스템 레벨 입력 시뮬레이션
- 멀티스레드 비동기 처리
- 서비스 라이프사이클 관리

### 2.2 시스템 아키텍처 설계

**계층별 구성요소**:

**연결 관리 계층**:
- ESP32-S3 동글과의 핸드셰이크 및 연결 상태 관리
- Keep-alive 신호 처리 및 연결 품질 모니터링
- 연결 실패 감지 및 자동 복구 메커니즘

**입력 처리 계층**:
- 고급 입력 명령 해석 및 실행
- 멀티 커서 상태 관리 및 전환 처리
- 매크로 실행 요청 중계 (Orbit 프로그램 연동)

**상태 관리 계층**:
- Essential ↔ Standard 모드 전환 관리
- 시스템 상태 동기화 및 일관성 보장
- 설정 변경 사항 실시간 적용

**시스템 통합 계층**:
- Windows API 호출 및 시스템 이벤트 처리
- 보안 정책 적용 및 권한 관리
- 성능 모니터링 및 리소스 최적화

### 2.3 통신 방식

#### 2.3.1 기본 경로: HID Boot Mouse

**정의 및 특징**:
- **범용성**: 모든 운영체제에서 기본 지원
- **표준화된 데이터**: X/Y 좌표 이동, 버튼 클릭, 휠 스크롤만 전송 가능
- **높은 호환성**: BIOS나 BitLocker 화면에서도 동작

**Windows 서버 관점**:
- HID 마우스는 Windows 기본 드라이버로 자동 처리
- 서버 프로그램이 직접 HID 데이터를 수신하지 않음
- Essential/Standard 모드에서 모두 동작 보장

#### 2.3.2 기본 경로: HID Boot Keyboard

**정의 및 특징**:
- **범용성**: 모든 운영체제에서 기본 지원
- **표준화된 데이터**: 키 코드, 모디파이어 키(Shift, Ctrl, Alt, Win) 전송 가능
- **높은 호환성**: BIOS나 BitLocker 화면에서도 동작
- **제한된 키 범위**: Boot Protocol에서는 기본 키만 지원 (A-Z, 0-9, 특수키 등)

**Windows 서버 관점**:
- HID 키보드는 Windows 기본 드라이버로 자동 처리
- 서버 프로그램이 직접 HID 데이터를 수신하지 않음
- Essential/Standard 모드에서 모두 동작 보장

#### 2.3.3 확장 경로: Vendor CDC

**정의 및 특징**:
- **Vendor CDC**: 제조사가 자유롭게 데이터 형식 정의 가능
- **CDC(Communications Device Class)**: 가상 시리얼 포트로 자유로운 데이터 교환
- **전용 통신 채널**: Windows 서버와 ESP32-S3 동글 간 1:1 양방향 통신

**Windows 서버의 활용**:
- Vendor CDC 인터페이스를 통해 고급 기능 명령 수신
- 멀티 커서 전환 요청 처리
- 매크로 실행 요청을 Orbit 프로그램으로 중계
- 응답 메시지를 Vendor CDC로 다시 전송

**Vendor CDC vs Named Pipe 프로토콜 비교** (Phase 2 통신 안정화):

| 항목 | Vendor CDC (ESP32-S3 ↔ Windows) | Named Pipe (Windows ↔ Orbit) |
|------|--------------------------------|------------------------------|
| **프레임 구조** | 바이너리 프레임 (헤더 + command + length + payload + checksum) | JSON 텍스트 스트림 |
| **헤더** | 0xFF (1 byte) | 없음 |
| **길이 필드** | `length` 필드 (Little-Endian, 2 bytes) | 없음 (개행 문자로 구분) |
| **페이로드 최대 크기** | 448 bytes | 제한 없음 (실용적으로 ~4KB) |
| **메시지 구분자** | `length` 필드 기반 정확한 프레임 추출 | 개행 문자 `\n` |
| **체크섬** | CRC16 (Little-Endian, 2 bytes) | 없음 |
| **인코딩** | UTF-8 (JSON 페이로드) | UTF-8 (JSON) |
| **전송 방향** | 양방향 | 양방향 |
| **오류 검증** | CRC16 + length 필드 | JSON 파싱만 |
| **재전송 메커니즘** | CRC 오류 시 자동 재전송 (최대 3회) | 없음 (애플리케이션 레벨 처리) |
| **연결 유형** | USB CDC 가상 시리얼 포트 | 윈도우 Named Pipe |

**Vendor CDC 오류 처리** (Phase 2 통신 안정화):
```csharp
public async Task<bool> ProcessVendorCdcFrameAsync(byte[] frameData)
{
    try
    {
        // 1. 프레임 구조 검증
        if (frameData.Length < 6)  // 최소 프레임 크기: header(1) + command(1) + length(2) + checksum(2)
        {
            Logger.Error($"Incomplete Vendor CDC frame: {frameData.Length} bytes");
            await SendErrorResponseAsync("CRC_MISMATCH", "Incomplete frame");
            return false;
        }
        
        // 2. 헤더 검증
        if (frameData[0] != 0xFF)
        {
            Logger.Error($"Invalid Vendor CDC header: 0x{frameData[0]:X2}");
            await SendErrorResponseAsync("CRC_MISMATCH", "Invalid header");
            return false;
        }
        
        // 3. length 필드 추출 및 검증 (Little-Endian)
        ushort payloadLength = BitConverter.ToUInt16(frameData, 2);
        if (payloadLength > 448)
        {
            Logger.Error($"Payload length exceeds limit: {payloadLength} > 448");
            await SendErrorResponseAsync("CRC_MISMATCH", "Payload too large");
            return false;
        }
        
        // 4. 전체 프레임 크기 검증
        int expectedFrameSize = 1 + 1 + 2 + payloadLength + 2;
        if (frameData.Length < expectedFrameSize)
        {
            Logger.Error($"Frame size mismatch: received {frameData.Length}, expected {expectedFrameSize}");
            await SendErrorResponseAsync("CRC_MISMATCH", "Frame size mismatch");
            return false;
        }
        
        // 5. CRC16 검증
        ushort receivedChecksum = BitConverter.ToUInt16(frameData, 4 + payloadLength);
        ushort calculatedChecksum = CalculateCrc16(frameData, 4, payloadLength);
        
        if (receivedChecksum != calculatedChecksum)
        {
            Logger.Error($"CRC mismatch: received 0x{receivedChecksum:X4}, calculated 0x{calculatedChecksum:X4}");
            await SendErrorResponseAsync("CRC_MISMATCH", "CRC16 checksum verification failed");
            return false;
        }
        
        // 6. JSON 페이로드 추출 및 파싱
        string jsonPayload = Encoding.UTF8.GetString(frameData, 4, payloadLength);
        JObject json;
        
        try
        {
            json = JObject.Parse(jsonPayload);
        }
        catch (JsonReaderException ex)
        {
            Logger.Error($"JSON parse error: {ex.Message}");
            await SendErrorResponseAsync("JSON_PARSE_ERROR", $"Failed to parse JSON payload: {ex.Message}");
            return false;
        }
        
        // 7. 필수 필드 검증
        string command = json["command"]?.ToString();
        if (string.IsNullOrEmpty(command))
        {
            Logger.Error("Missing required field: 'command'");
            await SendErrorResponseAsync("JSON_PARSE_ERROR", "Missing required field: 'command'");
            return false;
        }
        
        // 8. 명령 처리
        await HandleVendorCdcCommandAsync(json);
        return true;
    }
    catch (Exception ex)
    {
        Logger.Error($"Unexpected error processing Vendor CDC frame: {ex.Message}");
        await SendErrorResponseAsync("EXCEPTION", ex.Message);
        return false;
    }
}

private async Task SendErrorResponseAsync(string errorCode, string errorMessage)
{
    var errorResponse = new
    {
        command = "ERROR_RESPONSE",
        error_code = errorCode,
        error_message = errorMessage,
        timestamp = DateTime.UtcNow.ToString("O")
    };
    
    string jsonString = JsonConvert.SerializeObject(errorResponse);
    await SendVendorCdcFrameAsync(jsonString);
    
    Logger.Info($"Error response sent: {errorCode} - {errorMessage}");
}

private ushort CalculateCrc16(byte[] data, int offset, int length)
{
    ushort crc = 0;
    for (int i = 0; i < length; i++)
    {
        crc ^= (ushort)(data[offset + i] << 8);
        for (int j = 0; j < 8; j++)
        {
            if ((crc & 0x8000) != 0)
                crc = (ushort)((crc << 1) ^ 0x1021);
            else
                crc <<= 1;
        }
    }
    return crc;
}
```

**Named Pipe 메시지 처리**:
```csharp
public async Task ProcessNamedPipeMessageAsync(string messageText)
{
    // Named Pipe는 '\n'으로 메시지 구분
    string[] messages = messageText.Split('\n', StringSplitOptions.RemoveEmptyEntries);
    
    foreach (string message in messages)
    {
        try
        {
            JObject json = JObject.Parse(message);
            await HandleNamedPipeCommandAsync(json);
        }
        catch (JsonReaderException ex)
        {
            Logger.Error($"Named Pipe JSON parse error: {ex.Message}");
            // Named Pipe는 오류 응답을 전송하지 않음 (Orbit이 처리)
        }
    }
}
```

## 3. 핵심 기능 기술 구현

### 3.1 연결 관리 기술 구현

**연결 신호 프로토콜 설계**:
- **서버 주도적 연결**: Windows 서버가 0.5초 주기로 연결 요청 신호 전송
- **3단계 연결 확립**: 신호 전송 → 클라이언트 응답 → 핸드셰이크 완료
- **상태 동기화**: Authentication과 State Sync를 통한 안전한 연결 확립

**핸드셰이크 프로토콜 명세**:
- **Phase 1 - Authentication (1초)**: 보안 인증 및 기기 검증
- **Phase 2 - State Sync (1초)**: 상태 정보 동기화 및 기능 협상
- **연결 완료**: Keep-alive 시스템 활성화 및 정상 통신 시작

**Keep-alive 정책**:
- **Ping-Pong 방식**: 서버 → Vendor CDC로 ping 전송, pong 응답 수신
- **실패 감지**: 연속 3회 pong 미수신 시 연결 불안정 판정
- **자동 복구**: 지수 백오프 알고리즘을 통한 점진적 재연결 시도

**메시지 형식 설계**:
- **JSON 기반 구조화**: 메시지 타입, ID, 타임스탬프, 페이로드 구조
- **확장성**: 새로운 기능 추가 시 하위 호환성 보장
- **압축 및 최적화**: 대용량 데이터 전송 시 효율성 확보

### 3.2 핸드셰이크 프로토콜

**Phase 1: Authentication (1초)**:
- **Challenge-Response 방식**: 서버에서 랜덤 challenge 생성 및 Vendor CDC로 전송
- **응답 검증**: 1초 내에 Vendor CDC로 올바른 응답 수신 필요
- **기기 검증**: 연결된 기기의 정당성 확인
- **보안 인증**: 암호화된 인증 프로토콜을 통한 안전한 연결 확립

**Phase 2: State Sync (1초)**:
- **상태 정보 교환**: 현재 서버 상태, 지원 기능, Keep-alive 주기 정보를 Vendor CDC로 전송
- **기능 협상**: 양쪽에서 지원하는 기능들의 호환성 확인
- **연결 확립**: ACK 수신 시 연결 상태를 활성화하고 Keep-alive 시작
- **동기화 완료**: 모든 상태 정보가 양쪽에서 일치하는지 확인

### 3.3 Keep-alive 정책

**Ping-Pong 방식 설계**:
- **주기적 신호 전송**: 서버에서 0.5초 주기로 Vendor CDC를 통해 ping 신호 전송
- **응답 대기**: Vendor CDC를 통해 1초 내에 pong 응답 수신 대기
- **실패 카운팅**: 연속으로 pong 미수신 시 실패 카운트 증가
- **임계값 기반 판정**: 연속 3회 실패 시 연결 불안정으로 판정
- **자동 복구**: 연결 문제 감지 시 지수 백오프 알고리즘으로 재연결 시도
- **오류 처리**: 통신 오류 발생 시 로깅 및 상태 복구 메커니즘

### 3.4 메시지 형식

**JSON 기반 구조화 메시지 설계**:
- **표준 메시지 구조**: MessageType, MessageId, Timestamp, Payload 4개 필드로 구성
- **메시지 타입 분류**: 각 기능별로 고유한 메시지 타입 정의
- **고유 식별자**: 각 메시지마다 고유한 ID 부여로 중복 처리 방지
- **타임스탬프**: 메시지 생성 시점 기록으로 순서 보장 및 디버깅 지원
- **페이로드**: 실제 데이터를 담는 유연한 구조

**주요 메시지 타입들**:
- **MACRO_START_REQUEST**: 매크로 실행 요청 (매크로 ID, 파라미터 포함)
- **MULTI_CURSOR_SWITCH**: 멀티 커서 전환 요청 (대상 커서 ID, 애니메이션 타입 포함)
- **KEYBOARD_KEY_DOWN/UP**: 단일 키 입력 요청 (키 코드, 모디파이어 키 포함)
- **SHORTCUT_EXECUTE**: 복합 키 조합 실행 요청 (단축키 ID, 키 시퀀스 포함)
- **STICKY_HOLD_TOGGLE**: Sticky Hold 상태 변경 요청 (키 코드, 래치 상태 포함)

### 3.5 키보드 입력 처리 기술 구현

**키보드 입력 시스템 설계 원리**:
- **다층 입력 처리**: 단일 키, 복합 키 조합, 매크로 실행의 계층적 처리
- **상태 기반 관리**: 모디파이어 키 상태 및 Sticky Hold 상태의 독립적 추적
- **시퀀스 보장**: 복합 키 조합의 정확한 Down/Up 순서 보장
- **보안 고려**: Windows 보안 정책 및 시스템 키 제약 사항 준수

**알고리즘 설계 요구사항**:
- **키 상태 추적**: 각 키별 Press/Release 상태의 실시간 관리
- **모디파이어 관리**: Ctrl, Alt, Shift, Win 키의 조합 상태 추적
- **Sticky Hold 처리**: 500ms 임계값 기반 키 유지 상태 관리
- **디바운스 제어**: 150ms 내 중복 입력 방지 메커니즘

**입력 시뮬레이션 설계**:
- **Windows API 통합**: SendInput API를 통한 하드웨어 수준 입력 시뮬레이션
- **가상 키 매핑**: 수신된 키 식별자와 Windows Virtual-Key 코드 간 매핑 테이블
- **시퀀스 실행**: 복합 키 조합의 정확한 타이밍 제어
- **오류 복구**: 입력 실패 시 키 상태 정리 및 복구 메커니즘

**성능 최적화 설계**:
- **입력 지연**: 키보드 입력 처리 지연시간 10ms 이내 달성
- **상태 동기화**: 100ms 주기로 키 상태 일관성 검증
- **메모리 효율**: 키 상태 정보의 최소 메모리 사용량 유지

### 3.6 멀티 커서 기술 구현

**멀티 커서 시스템 설계 원리**:
- **듀얼 커서 패러다임**: 실제 커서 1개 + 가상 커서 1개의 혁신적 인터페이스
- **텔레포트 메커니즘**: 커서 전환 요청 시 실제 커서의 순간 위치 이동
- **가상 커서 이미지**: 텔레포트 대상 위치를 시각적으로 표시하는 워프 포인트

**알고리즘 설계 요구사항**:
- **위치 상태 관리**: 각 커서별 좌표 정보의 독립적 유지
- **활성 커서 추적**: 현재 실제 커서와 연결된 가상 커서 ID 관리
- **동기화 메커니즘**: 실제 커서와 가상 커서 간의 상태 일관성 보장
- **성능 최적화**: 커서 전환 지연시간 50ms 이내 달성

#### 3.6.1 가상 커서 렌더링 시스템 구현 명세

##### 3.6.1.1 전체화면 투명 오버레이 윈도우 구현

**WPF Window 기본 설정**:
```csharp
// VirtualCursorOverlayWindow.xaml
<Window x:Class="BridgeOne.Server.VirtualCursorOverlayWindow"
        xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
        WindowStyle="None"
        AllowsTransparency="True"
        Background="Transparent"
        Topmost="True"
        ShowInTaskbar="False"
        ResizeMode="NoResize"
        WindowState="Maximized"
        Loaded="Window_Loaded">
```

**Win32 확장 스타일 적용**:
- **WS_EX_TRANSPARENT (0x00000020)**: 마우스 이벤트가 윈도우를 관통하여 하위 윈도우로 전달
- **WS_EX_LAYERED (0x00080000)**: 투명도 및 알파 블렌딩 지원
- **WS_EX_TOOLWINDOW (0x00000080)**: Alt+Tab 목록에서 제외
- **WS_EX_NOACTIVATE (0x08000000)**: 포커스를 받지 않음

**SetWindowLong 적용 코드 구조**:
```csharp
private void Window_Loaded(object sender, RoutedEventArgs e)
{
    var hwnd = new WindowInteropHelper(this).Handle;
    int extendedStyle = GetWindowLong(hwnd, GWL_EXSTYLE);
    SetWindowLong(hwnd, GWL_EXSTYLE, 
        extendedStyle | WS_EX_TRANSPARENT | WS_EX_LAYERED | WS_EX_TOOLWINDOW | WS_EX_NOACTIVATE);
}

[DllImport("user32.dll")]
private static extern int GetWindowLong(IntPtr hWnd, int nIndex);

[DllImport("user32.dll")]
private static extern int SetWindowLong(IntPtr hWnd, int nIndex, int dwNewLong);

private const int GWL_EXSTYLE = -20;
private const int WS_EX_TRANSPARENT = 0x00000020;
private const int WS_EX_LAYERED = 0x00080000;
private const int WS_EX_TOOLWINDOW = 0x00000080;
private const int WS_EX_NOACTIVATE = 0x08000000;
```

**멀티 모니터 환경 대응**:
- **SystemParameters.VirtualScreenWidth/Height**: 모든 모니터를 포함한 가상 화면 크기 획득
- **Screen.AllScreens (System.Windows.Forms)**: 각 모니터의 해상도 및 위치 정보 수집
- **좌표 계산**: 가상 커서 위치를 가상 화면 좌표계로 변환
- **경계 검증**: 가상 커서가 실제 모니터 영역 밖으로 벗어나는 경우 클램핑 처리

**윈도우 크기 및 위치 설정**:
```csharp
// 모든 모니터를 포함하는 가상 화면 전체를 커버
this.Left = SystemParameters.VirtualScreenLeft;
this.Top = SystemParameters.VirtualScreenTop;
this.Width = SystemParameters.VirtualScreenWidth;
this.Height = SystemParameters.VirtualScreenHeight;
```

##### 3.6.1.2 커서 이미지 렌더링 구현

**XAML Canvas 구조**:
```xml
<Canvas x:Name="CursorCanvas">
    <Image x:Name="VirtualCursorImage"
           Width="32" Height="32"
           RenderTransformOrigin="0,0">
        <Image.RenderTransform>
            <TransformGroup>
                <ScaleTransform x:Name="CursorScaleTransform" ScaleX="1.0" ScaleY="1.0"/>
                <TranslateTransform x:Name="CursorTranslateTransform" X="0" Y="0"/>
            </TransformGroup>
        </Image.RenderTransform>
        <Image.Effect>
            <DropShadowEffect x:Name="CursorGlowEffect" 
                              ShadowDepth="0" 
                              BlurRadius="0" 
                              Color="White" 
                              Opacity="0"/>
        </Image.Effect>
    </Image>
</Canvas>
```

**.cur/.ani 파일 로딩 및 표시**:
```csharp
// 커서 이미지 소스 설정
public void LoadCursorImage(string cursorFilePath)
{
    try
    {
        // .cur 파일의 경우 Icon으로 로드 후 BitmapSource로 변환
        using (var icon = new Icon(cursorFilePath))
        {
            VirtualCursorImage.Source = Imaging.CreateBitmapSourceFromHIcon(
                icon.Handle,
                Int32Rect.Empty,
                BitmapSizeOptions.FromEmptyOptions());
        }
    }
    catch (Exception ex)
    {
        // 폴백: 기본 화살표 커서 이미지 사용
        LoadDefaultCursorImage();
        Logger.Error($"Failed to load cursor image: {ex.Message}");
    }
}
```

**.ani (애니메이션 커서) 처리**:
- **DispatcherTimer 활용**: 프레임 간격에 맞춰 커서 이미지 변경
- **ANIHEADER 구조체 파싱**: .ani 파일에서 프레임 수, 지연시간 정보 추출
- **프레임 시퀀스 관리**: 각 프레임의 Icon 데이터를 배열로 저장 및 순환 재생

**Canvas 좌표계 기반 위치 계산**:
```csharp
// 가상 커서 위치 업데이트
public void UpdateCursorPosition(Point screenPosition)
{
    // 화면 좌표를 Canvas 좌표로 변환 (가상 화면 기준)
    double canvasX = screenPosition.X - SystemParameters.VirtualScreenLeft;
    double canvasY = screenPosition.Y - SystemParameters.VirtualScreenTop;
    
    // 핫스팟 오프셋 적용 (커서 이미지의 클릭 지점)
    canvasX -= _cursorHotspotX;
    canvasY -= _cursorHotspotY;
    
    // TranslateTransform으로 위치 이동
    CursorTranslateTransform.X = canvasX;
    CursorTranslateTransform.Y = canvasY;
}
```

**60fps 업데이트 메커니즘**:
```csharp
private DispatcherTimer _renderTimer;

private void InitializeRenderTimer()
{
    _renderTimer = new DispatcherTimer(DispatcherPriority.Render);
    _renderTimer.Interval = TimeSpan.FromMilliseconds(16); // ~60fps
    _renderTimer.Tick += (s, e) => UpdateVirtualCursor();
    _renderTimer.Start();
}

private void UpdateVirtualCursor()
{
    if (_isVirtualCursorVisible && _pendingCursorPosition.HasValue)
    {
        UpdateCursorPosition(_pendingCursorPosition.Value);
        _pendingCursorPosition = null;
    }
}
```

**GPU 하드웨어 가속 활용**:
```csharp
// App.xaml.cs 또는 Window 생성자에서 설정
RenderOptions.ProcessRenderMode = RenderMode.Default; // 하드웨어 가속 우선
this.UseLayoutRounding = true; // 픽셀 정렬 최적화

// 개별 Image 컨트롤에 캐싱 힌트 설정
RenderOptions.SetBitmapScalingMode(VirtualCursorImage, BitmapScalingMode.HighQuality);
RenderOptions.SetCachingHint(VirtualCursorImage, CachingHint.Cache);
```

##### 3.6.1.3 성능 최적화 및 메모리 관리

**프레임 스킵 방지**:
- **Dispatcher 우선순위**: `DispatcherPriority.Render` 사용으로 렌더링 우선 처리
- **프레임 드롭 감지**: 실제 렌더링 간격 측정 및 로깅
- **적응형 업데이트**: 시스템 부하 시 업데이트 주기 동적 조절 (16ms → 20ms)

**메모리 사용량 목표**:
- **오버레이 윈도우**: 20MB 이내 (비트맵 캐시 포함)
- **커서 이미지 캐시**: 최대 10개 커서 이미지 동시 캐싱 (각 ~100KB)
- **메모리 누수 방지**: Weak Reference 패턴으로 이미지 리소스 관리

#### 3.6.2 커서 팩 자동 감지 알고리즘 상세 명세

##### 3.6.2.1 레지스트리 스캔 단계

**레지스트리 경로 및 키 정의**:
```csharp
private const string CURSOR_REGISTRY_PATH = @"Control Panel\Cursors";
private readonly string[] CURSOR_TYPES = new string[]
{
    "Arrow",        // 기본 화살표
    "Help",         // 도움말
    "AppStarting",  // 백그라운드 작업
    "Wait",         // 대기 (모래시계/회전)
    "Crosshair",    // 십자선
    "IBeam",        // 텍스트 입력
    "NWPen",        // 필기
    "No",           // 금지
    "SizeNS",       // 수직 크기 조절
    "SizeWE",       // 수평 크기 조절
    "SizeNWSE",     // 대각선 크기 조절 ↖↘
    "SizeNESW",     // 대각선 크기 조절 ↗↙
    "SizeAll",      // 전체 이동
    "UpArrow",      // 대체 선택
    "Hand"          // 링크 선택
};
```

**레지스트리 읽기 구현**:
```csharp
public Dictionary<string, string> ScanCursorRegistry()
{
    var cursorPaths = new Dictionary<string, string>();
    
    using (var key = Registry.CurrentUser.OpenSubKey(CURSOR_REGISTRY_PATH))
    {
        if (key == null)
        {
            Logger.Warn("Cursor registry key not found");
            return cursorPaths;
        }
        
        foreach (var cursorType in CURSOR_TYPES)
        {
            var path = key.GetValue(cursorType) as string;
            if (!string.IsNullOrEmpty(path) && File.Exists(path))
            {
                cursorPaths[cursorType] = path;
                Logger.Debug($"Found cursor: {cursorType} -> {path}");
            }
        }
    }
    
    return cursorPaths;
}
```

**파일 존재 여부 검증**:
- **즉시 검증**: `File.Exists()` 호출로 파일 실존 확인
- **권한 검증**: 파일 읽기 권한 확인 (FileIOPermission)
- **손상 검증**: 파일 헤더 읽기로 유효한 .cur/.ani 파일인지 확인

##### 3.6.2.2 공통 폴더 추론 알고리즘

**경로 문자열 분석**:
```csharp
public string InferCommonCursorFolder(Dictionary<string, string> cursorPaths)
{
    if (cursorPaths.Count == 0) return null;
    
    // 각 커서 파일의 디렉터리 경로 추출
    var directories = cursorPaths.Values
        .Select(path => Path.GetDirectoryName(path))
        .Where(dir => !string.IsNullOrEmpty(dir))
        .ToList();
    
    if (directories.Count == 0) return null;
    
    // 디렉터리 빈도 계산
    var directoryGroups = directories
        .GroupBy(dir => dir, StringComparer.OrdinalIgnoreCase)
        .Select(g => new { Directory = g.Key, Count = g.Count() })
        .OrderByDescending(x => x.Count)
        .ToList();
    
    // 80% 이상 동일 경로 기준
    var mostCommon = directoryGroups.First();
    var threshold = directories.Count * 0.8;
    
    if (mostCommon.Count >= threshold)
    {
        Logger.Info($"Detected common cursor folder: {mostCommon.Directory} ({mostCommon.Count}/{directories.Count})");
        return mostCommon.Directory;
    }
    
    Logger.Warn($"No dominant cursor folder found (best: {mostCommon.Count}/{directories.Count})");
    return null;
}
```

**메타데이터 파일 탐색**:
```csharp
public CursorPackMetadata ParseMetadataFiles(string cursorFolder)
{
    var metadata = new CursorPackMetadata();
    
    // install.inf 파일 탐색
    var installInfPath = Path.Combine(cursorFolder, "install.inf");
    if (File.Exists(installInfPath))
    {
        metadata.PackName = ParseInstallInf(installInfPath);
    }
    
    // theme.ini 파일 탐색
    var themeIniPath = Path.Combine(cursorFolder, "theme.ini");
    if (File.Exists(themeIniPath))
    {
        var themeData = ParseThemeIni(themeIniPath);
        metadata.PackName = themeData.Name ?? metadata.PackName;
        metadata.Author = themeData.Author;
        metadata.Version = themeData.Version;
    }
    
    // README, LICENSE 파일 탐색
    var readmeFiles = Directory.GetFiles(cursorFolder, "README*", SearchOption.TopDirectoryOnly);
    if (readmeFiles.Length > 0)
    {
        metadata.ReadmePath = readmeFiles[0];
    }
    
    return metadata;
}

private string ParseInstallInf(string installInfPath)
{
    // INF 파일에서 [Strings] 섹션의 CursorSchemeName 추출
    var lines = File.ReadAllLines(installInfPath);
    var inStringsSection = false;
    
    foreach (var line in lines)
    {
        if (line.Trim() == "[Strings]")
        {
            inStringsSection = true;
            continue;
        }
        
        if (inStringsSection && line.Contains("="))
        {
            var parts = line.Split('=');
            if (parts[0].Trim().Equals("CursorSchemeName", StringComparison.OrdinalIgnoreCase))
            {
                return parts[1].Trim(' ', '"');
            }
        }
    }
    
    return null;
}
```

##### 3.6.2.3 품질 등급 산정 시스템

**완성도 체크 알고리즘**:
```csharp
public CursorPackQuality AssessCursorPackQuality(Dictionary<string, string> cursorPaths)
{
    // 13개 표준 커서 타입 대비 실제 보유 타입 수
    var availableCount = cursorPaths.Count(kvp => CURSOR_TYPES.Contains(kvp.Key));
    var completionRate = (double)availableCount / CURSOR_TYPES.Length;
    
    // 품질 등급 판정
    CursorPackQuality quality;
    if (completionRate >= 0.90) quality = CursorPackQuality.Excellent;
    else if (completionRate >= 0.70) quality = CursorPackQuality.Good;
    else if (completionRate >= 0.40) quality = CursorPackQuality.Basic;
    else quality = CursorPackQuality.Incomplete;
    
    Logger.Info($"Cursor pack quality: {quality} ({availableCount}/{CURSOR_TYPES.Length}, {completionRate:P0})");
    
    return quality;
}

public enum CursorPackQuality
{
    Excellent,   // 90%+ (12-13개)
    Good,        // 70-89% (9-11개)
    Basic,       // 40-69% (5-8개)
    Incomplete   // <40% (0-4개)
}
```

**품질 등급별 처리 전략**:
- **Excellent**: 모든 커서 타입의 가상 커서 이미지 완벽 지원
- **Good**: 핵심 커서 타입(Arrow, IBeam, Wait, Hand) 우선 지원, 나머지는 대체 이미지 사용
- **Basic**: Arrow, IBeam, Wait만 지원, 나머지는 기본 화살표로 대체
- **Incomplete**: 경고 메시지 표시 및 시스템 기본 커서로 폴백

#### 3.6.3 커서 상태 감지 및 동기화 메커니즘

##### 3.6.3.1 실시간 커서 상태 감지 구현

**GetCursorInfo API 활용**:
```csharp
[StructLayout(LayoutKind.Sequential)]
private struct CURSORINFO
{
    public int cbSize;
    public int flags;
    public IntPtr hCursor;
    public POINT ptScreenPos;
}

[StructLayout(LayoutKind.Sequential)]
private struct POINT
{
    public int X;
    public int Y;
}

[DllImport("user32.dll")]
private static extern bool GetCursorInfo(ref CURSORINFO pci);

private const int CURSOR_SHOWING = 0x00000001;
```

**100ms 주기 상태 확인 시스템**:
```csharp
private DispatcherTimer _cursorStateTimer;
private IntPtr _lastCursorHandle = IntPtr.Zero;

private void InitializeCursorStateMonitoring()
{
    _cursorStateTimer = new DispatcherTimer();
    _cursorStateTimer.Interval = TimeSpan.FromMilliseconds(100);
    _cursorStateTimer.Tick += OnCursorStateTimerTick;
    _cursorStateTimer.Start();
}

private void OnCursorStateTimerTick(object sender, EventArgs e)
{
    var cursorInfo = new CURSORINFO { cbSize = Marshal.SizeOf(typeof(CURSORINFO)) };
    
    if (GetCursorInfo(ref cursorInfo))
    {
        // 커서 핸들이 변경되었는지 확인
        if (cursorInfo.hCursor != _lastCursorHandle)
        {
            _lastCursorHandle = cursorInfo.hCursor;
            OnCursorStateChanged(cursorInfo.hCursor);
        }
    }
}
```

**상태 변경 시 가상 커서 즉시 업데이트**:
```csharp
private void OnCursorStateChanged(IntPtr cursorHandle)
{
    try
    {
        // 커서 타입 식별
        var cursorType = IdentifyCursorType(cursorHandle);
        
        // 대응하는 커서 이미지 파일 경로 조회
        if (_detectedCursorPack.TryGetValue(cursorType, out var cursorPath))
        {
            // 가상 커서 이미지 업데이트
            UpdateVirtualCursorImage(cursorPath);
            Logger.Debug($"Virtual cursor updated to: {cursorType}");
        }
        else
        {
            // 폴백: 기본 커서 이미지 사용
            LoadDefaultCursorImage();
            Logger.Warn($"Cursor type not found in pack: {cursorType}");
        }
    }
    catch (Exception ex)
    {
        Logger.Error($"Failed to update virtual cursor state: {ex.Message}");
    }
}
```

##### 3.6.3.2 커서 타입별 매핑 테이블 구현

**시스템 커서 ID 매핑**:
```csharp
private static readonly Dictionary<IntPtr, string> SYSTEM_CURSOR_MAP = new Dictionary<IntPtr, string>
{
    { LoadCursor(IntPtr.Zero, IDC_ARROW), "Arrow" },
    { LoadCursor(IntPtr.Zero, IDC_IBEAM), "IBeam" },
    { LoadCursor(IntPtr.Zero, IDC_WAIT), "Wait" },
    { LoadCursor(IntPtr.Zero, IDC_CROSS), "Crosshair" },
    { LoadCursor(IntPtr.Zero, IDC_UPARROW), "UpArrow" },
    { LoadCursor(IntPtr.Zero, IDC_SIZENWSE), "SizeNWSE" },
    { LoadCursor(IntPtr.Zero, IDC_SIZENESW), "SizeNESW" },
    { LoadCursor(IntPtr.Zero, IDC_SIZEWE), "SizeWE" },
    { LoadCursor(IntPtr.Zero, IDC_SIZENS), "SizeNS" },
    { LoadCursor(IntPtr.Zero, IDC_SIZEALL), "SizeAll" },
    { LoadCursor(IntPtr.Zero, IDC_NO), "No" },
    { LoadCursor(IntPtr.Zero, IDC_HAND), "Hand" },
    { LoadCursor(IntPtr.Zero, IDC_APPSTARTING), "AppStarting" },
    { LoadCursor(IntPtr.Zero, IDC_HELP), "Help" }
};

[DllImport("user32.dll", CharSet = CharSet.Auto)]
private static extern IntPtr LoadCursor(IntPtr hInstance, int lpCursorName);

// 표준 커서 ID 상수
private const int IDC_ARROW = 32512;
private const int IDC_IBEAM = 32513;
private const int IDC_WAIT = 32514;
private const int IDC_CROSS = 32515;
private const int IDC_UPARROW = 32516;
private const int IDC_SIZENWSE = 32642;
private const int IDC_SIZENESW = 32643;
private const int IDC_SIZEWE = 32644;
private const int IDC_SIZENS = 32645;
private const int IDC_SIZEALL = 32646;
private const int IDC_NO = 32648;
private const int IDC_HAND = 32649;
private const int IDC_APPSTARTING = 32650;
private const int IDC_HELP = 32651;
```

**커서 타입 식별 알고리즘**:
```csharp
private string IdentifyCursorType(IntPtr cursorHandle)
{
    // 시스템 기본 커서 매칭 시도
    if (SYSTEM_CURSOR_MAP.TryGetValue(cursorHandle, out var cursorType))
    {
        return cursorType;
    }
    
    // 커스텀 커서의 경우 GetIconInfo로 추가 정보 획득
    if (GetIconInfo(cursorHandle, out var iconInfo))
    {
        // 핫스팟 위치 기반 추론 (예: 중앙이면 Wait, 좌상단이면 Arrow 등)
        var hotspotX = iconInfo.xHotspot;
        var hotspotY = iconInfo.yHotspot;
        
        // 비트맵 크기 획득
        if (GetBitmapDimensions(iconInfo.hbmColor, out var width, out var height))
        {
            // 휴리스틱 기반 타입 추론
            if (hotspotX == width / 2 && hotspotY == height / 2)
                return "Wait"; // 중앙 핫스팟 → 대기 커서
            else if (hotspotX < 5 && hotspotY < 5)
                return "Arrow"; // 좌상단 핫스팟 → 화살표 커서
            else
                return "Unknown";
        }
        
        // 리소스 해제
        DeleteObject(iconInfo.hbmColor);
        DeleteObject(iconInfo.hbmMask);
    }
    
    // 기본값: Arrow
    return "Arrow";
}

[StructLayout(LayoutKind.Sequential)]
private struct ICONINFO
{
    public bool fIcon;
    public int xHotspot;
    public int yHotspot;
    public IntPtr hbmMask;
    public IntPtr hbmColor;
}

[DllImport("user32.dll")]
private static extern bool GetIconInfo(IntPtr hIcon, out ICONINFO piconinfo);

[DllImport("gdi32.dll")]
private static extern bool DeleteObject(IntPtr hObject);
```

#### 3.6.4 텔레포트 메커니즘 구현 명세

##### 3.6.4.1 좌표 계산 및 검증

**멀티 커서 전환 메시지 파싱**:
```csharp
// JSON 메시지 수신 예시:
// {"command": "multi_cursor_switch", "touchpad_id": "pad2", "cursor_position": {"x": 1920, "y": 540}}

public class MultiCursorSwitchMessage
{
    [JsonPropertyName("command")]
    public string Command { get; set; }
    
    [JsonPropertyName("touchpad_id")]
    public string TouchpadId { get; set; }
    
    [JsonPropertyName("cursor_position")]
    public CursorPosition Position { get; set; }
    
    [JsonPropertyName("timestamp")]
    public string Timestamp { get; set; }
}

public class CursorPosition
{
    [JsonPropertyName("x")]
    public int X { get; set; }
    
    [JsonPropertyName("y")]
    public int Y { get; set; }
}
```

**멀티 모니터 환경 절대 좌표 계산**:
```csharp
public Point CalculateAbsoluteCursorPosition(CursorPosition relativePos)
{
    // 가상 화면 좌표계로 변환 (모든 모니터를 포함하는 가상 공간)
    var virtualScreenLeft = SystemParameters.VirtualScreenLeft;
    var virtualScreenTop = SystemParameters.VirtualScreenTop;
    
    // 상대 좌표를 절대 좌표로 변환
    var absoluteX = virtualScreenLeft + relativePos.X;
    var absoluteY = virtualScreenTop + relativePos.Y;
    
    return new Point(absoluteX, absoluteY);
}
```

**화면 경계 검증 및 클램핑**:
```csharp
public Point ValidateAndClampCursorPosition(Point targetPosition)
{
    var screens = Screen.AllScreens;
    
    // 목표 위치가 어느 모니터에 속하는지 확인
    foreach (var screen in screens)
    {
        var bounds = screen.Bounds;
        if (bounds.Contains((int)targetPosition.X, (int)targetPosition.Y))
        {
            // 유효한 위치
            Logger.Debug($"Target position is valid (Monitor: {screen.DeviceName})");
            return targetPosition;
        }
    }
    
    // 유효하지 않은 위치 → 가장 가까운 모니터 경계로 클램핑
    var primaryScreen = Screen.PrimaryScreen.Bounds;
    var clampedX = Math.Max(primaryScreen.Left, Math.Min(primaryScreen.Right - 1, (int)targetPosition.X));
    var clampedY = Math.Max(primaryScreen.Top, Math.Min(primaryScreen.Bottom - 1, (int)targetPosition.Y));
    
    Logger.Warn($"Position out of bounds, clamped to ({clampedX}, {clampedY})");
    return new Point(clampedX, clampedY);
}
```

##### 3.6.4.2 커서 이동 실행 및 성능 측정

**SetCursorPos API 호출**:
```csharp
[DllImport("user32.dll")]
private static extern bool SetCursorPos(int X, int Y);

public async Task<TeleportResult> ExecuteTeleportAsync(CursorPosition targetPosition)
{
    var stopwatch = Stopwatch.StartNew();
    
    try
    {
        // 좌표 계산 및 검증
        var absolutePos = CalculateAbsoluteCursorPosition(targetPosition);
        var validatedPos = ValidateAndClampCursorPosition(absolutePos);
        
        // 커서 이동 실행
        var success = SetCursorPos((int)validatedPos.X, (int)validatedPos.Y);
        
        stopwatch.Stop();
        var latency = stopwatch.ElapsedMilliseconds;
        
        if (success)
        {
            Logger.Info($"Teleport succeeded in {latency}ms to ({validatedPos.X}, {validatedPos.Y})");
            
            // 성능 목표 검증
            if (latency > 50)
            {
                Logger.Warn($"Teleport latency exceeded target: {latency}ms > 50ms");
            }
            
            return new TeleportResult
            {
                Success = true,
                ActualPosition = validatedPos,
                Latency = latency
            };
        }
        else
        {
            Logger.Error("SetCursorPos failed");
            return new TeleportResult { Success = false, ErrorCode = "SET_CURSOR_POS_FAILED" };
        }
    }
    catch (Exception ex)
    {
        stopwatch.Stop();
        Logger.Error($"Teleport exception: {ex.Message}");
        return new TeleportResult { Success = false, ErrorCode = "EXCEPTION", ErrorMessage = ex.Message };
    }
}

public class TeleportResult
{
    public bool Success { get; set; }
    public Point ActualPosition { get; set; }
    public long Latency { get; set; }
    public string ErrorCode { get; set; }
    public string ErrorMessage { get; set; }
}
```

**응답 메시지 생성 및 전송**:
```csharp
public async Task SendTeleportResponseAsync(TeleportResult result)
{
    var response = new
    {
        command = "multi_cursor_switch_ack",
        success = result.Success,
        error_code = result.ErrorCode,
        actual_position = result.Success ? new { x = (int)result.ActualPosition.X, y = (int)result.ActualPosition.Y } : null,
        latency_ms = result.Latency,
        timestamp = DateTime.UtcNow.ToString("o")
    };
    
    var json = JsonSerializer.Serialize(response);
    await _vendorCdcInterface.SendJsonMessageAsync(json);
}
```

#### 3.6.5 전환 애니메이션 시스템 구현

##### 3.6.5.1 3단계 전환 애니메이션 상세 명세

**Storyboard 기반 애니메이션 정의**:
```xml
<Storyboard x:Key="CursorTeleportAnimation">
    <!-- Phase 1: 페이드 아웃 (150ms) -->
    <DoubleAnimation Storyboard.TargetName="VirtualCursorImage"
                     Storyboard.TargetProperty="Opacity"
                     From="1.0" To="0.3"
                     Duration="0:0:0.15"
                     EasingFunction="{StaticResource QuadraticEaseOut}"/>
    
    <!-- Phase 2: 글로우 증가 (150ms) -->
    <DoubleAnimation Storyboard.TargetName="CursorGlowEffect"
                     Storyboard.TargetProperty="BlurRadius"
                     From="0" To="20"
                     BeginTime="0:0:0.15"
                     Duration="0:0:0.15"
                     EasingFunction="{StaticResource QuadraticEaseInOut}"/>
    
    <DoubleAnimation Storyboard.TargetName="CursorGlowEffect"
                     Storyboard.TargetProperty="Opacity"
                     From="0" To="0.8"
                     BeginTime="0:0:0.15"
                     Duration="0:0:0.15"
                     EasingFunction="{StaticResource QuadraticEaseInOut}"/>
    
    <!-- Phase 3: 페이드 인 (150ms) -->
    <DoubleAnimation Storyboard.TargetName="VirtualCursorImage"
                     Storyboard.TargetProperty="Opacity"
                     From="0.3" To="1.0"
                     BeginTime="0:0:0.30"
                     Duration="0:0:0.15"
                     EasingFunction="{StaticResource QuadraticEaseIn}"/>
    
    <DoubleAnimation Storyboard.TargetName="CursorGlowEffect"
                     Storyboard.TargetProperty="BlurRadius"
                     From="20" To="0"
                     BeginTime="0:0:0.30"
                     Duration="0:0:0.15"
                     EasingFunction="{StaticResource QuadraticEaseIn}"/>
    
    <DoubleAnimation Storyboard.TargetName="CursorGlowEffect"
                     Storyboard.TargetProperty="Opacity"
                     From="0.8" To="0"
                     BeginTime="0:0:0.30"
                     Duration="0:0:0.15"
                     EasingFunction="{StaticResource QuadraticEaseIn}"/>
</Storyboard>
```

**애니메이션 트리거 코드**:
```csharp
public async Task PlayTeleportAnimationAsync()
{
    var storyboard = (Storyboard)this.Resources["CursorTeleportAnimation"];
    
    var tcs = new TaskCompletionSource<bool>();
    storyboard.Completed += (s, e) => tcs.SetResult(true);
    
    storyboard.Begin();
    
    await tcs.Task; // 애니메이션 완료 대기 (450ms)
    
    Logger.Debug("Teleport animation completed");
}
```

##### 3.6.5.2 모드 활성화/비활성화 애니메이션

**멀티 커서 모드 활성화 애니메이션 (200ms)**:
```xml
<Storyboard x:Key="MultiCursorActivationAnimation">
    <!-- 페이드 인 -->
    <DoubleAnimation Storyboard.TargetName="VirtualCursorImage"
                     Storyboard.TargetProperty="Opacity"
                     From="0" To="1.0"
                     Duration="0:0:0.2"
                     EasingFunction="{StaticResource QuadraticEaseOut}"/>
    
    <!-- 스케일 애니메이션 -->
    <DoubleAnimation Storyboard.TargetName="CursorScaleTransform"
                     Storyboard.TargetProperty="ScaleX"
                     From="0.8" To="1.0"
                     Duration="0:0:0.2"
                     EasingFunction="{StaticResource BackEaseOut}"/>
    
    <DoubleAnimation Storyboard.TargetName="CursorScaleTransform"
                     Storyboard.TargetProperty="ScaleY"
                     From="0.8" To="1.0"
                     Duration="0:0:0.2"
                     EasingFunction="{StaticResource BackEaseOut}"/>
</Storyboard>
```

**멀티 커서 모드 비활성화 애니메이션 (150ms)**:
```xml
<Storyboard x:Key="MultiCursorDeactivationAnimation">
    <!-- 페이드 아웃 -->
    <DoubleAnimation Storyboard.TargetName="VirtualCursorImage"
                     Storyboard.TargetProperty="Opacity"
                     From="1.0" To="0"
                     Duration="0:0:0.15"
                     EasingFunction="{StaticResource QuadraticEaseIn}"/>
    
    <!-- 스케일 축소 -->
    <DoubleAnimation Storyboard.TargetName="CursorScaleTransform"
                     Storyboard.TargetProperty="ScaleX"
                     From="1.0" To="0.8"
                     Duration="0:0:0.15"
                     EasingFunction="{StaticResource QuadraticEaseIn}"/>
    
    <DoubleAnimation Storyboard.TargetName="CursorScaleTransform"
                     Storyboard.TargetProperty="ScaleY"
                     From="1.0" To="0.8"
                     Duration="0:0:0.15"
                     EasingFunction="{StaticResource QuadraticEaseIn}"/>
</Storyboard>
```

**애니메이션 제어 코드**:
```csharp
public async Task ActivateMultiCursorModeAsync()
{
    // 가상 커서 초기 위치 설정 (현재 실제 커서 위치)
    var currentPos = GetCurrentCursorPosition();
    UpdateCursorPosition(currentPos);
    
    // 활성화 애니메이션 재생
    var storyboard = (Storyboard)this.Resources["MultiCursorActivationAnimation"];
    var tcs = new TaskCompletionSource<bool>();
    storyboard.Completed += (s, e) => tcs.SetResult(true);
    storyboard.Begin();
    
    VirtualCursorImage.Visibility = Visibility.Visible;
    await tcs.Task;
    
    Logger.Info("Multi-cursor mode activated");
}

public async Task DeactivateMultiCursorModeAsync()
{
    // 비활성화 애니메이션 재생
    var storyboard = (Storyboard)this.Resources["MultiCursorDeactivationAnimation"];
    var tcs = new TaskCompletionSource<bool>();
    storyboard.Completed += (s, e) =>
    {
        VirtualCursorImage.Visibility = Visibility.Collapsed;
        tcs.SetResult(true);
    };
    storyboard.Begin();
    
    await tcs.Task;
    
    Logger.Info("Multi-cursor mode deactivated");
}
```

#### 3.6.6 상태 관리 및 동기화 시스템

##### 3.6.6.1 커서 위치 상태 관리

**상태 데이터 구조**:
```csharp
public class MultiCursorState
{
    // 모드 상태
    public bool IsMultiCursorEnabled { get; set; }
    
    // 커서 A (실제 커서가 있는 위치)
    public CursorInfo CursorA { get; set; }
    
    // 커서 B (가상 커서 이미지 위치)
    public CursorInfo CursorB { get; set; }
    
    // 현재 활성 커서 (A 또는 B)
    public string ActiveCursorId { get; set; } // "pad1" or "pad2"
    
    // 마지막 업데이트 시간
    public DateTime LastUpdated { get; set; }
}

public class CursorInfo
{
    public Point Position { get; set; }
    public string CursorType { get; set; } // "Arrow", "IBeam", etc.
    public string CursorImagePath { get; set; }
    public Point Hotspot { get; set; }
}
```

**상태 업데이트 메서드**:
```csharp
private MultiCursorState _multiCursorState = new MultiCursorState();
private readonly object _stateLock = new object();

public void UpdateMultiCursorState(string touchpadId, Point newPosition)
{
    lock (_stateLock)
    {
        if (touchpadId == "pad1")
        {
            // Pad1 활성화 → 실제 커서가 CursorA 위치, 가상 커서가 CursorB 위치
            _multiCursorState.ActiveCursorId = "pad1";
            _multiCursorState.CursorA.Position = newPosition;
            
            // 실제 커서 텔레포트
            SetCursorPos((int)newPosition.X, (int)newPosition.Y);
            
            // 가상 커서는 CursorB 위치 유지
        }
        else if (touchpadId == "pad2")
        {
            // Pad2 활성화 → 실제 커서가 CursorB 위치, 가상 커서가 CursorA 위치
            _multiCursorState.ActiveCursorId = "pad2";
            _multiCursorState.CursorB.Position = newPosition;
            
            // 실제 커서 텔레포트
            SetCursorPos((int)newPosition.X, (int)newPosition.Y);
            
            // 가상 커서는 CursorA 위치 유지
        }
        
        _multiCursorState.LastUpdated = DateTime.UtcNow;
    }
}
```

##### 3.6.6.2 멀티 커서 전환 메시지 처리

**멀티 커서 전환 요청 수신 및 처리**:
```csharp
public async Task HandleMultiCursorSwitchMessageAsync(MultiCursorSwitchMessage message)
{
    try
    {
        // 1. 가상 커서 위치 업데이트
        Point virtualCursorPos;
        
        if (message.TouchpadId == "pad1")
        {
            // Pad1 활성화 → CursorB를 가상 커서로 표시
            virtualCursorPos = _multiCursorState.CursorB.Position;
        }
        else
        {
            // Pad2 활성화 → CursorA를 가상 커서로 표시
            virtualCursorPos = _multiCursorState.CursorA.Position;
        }
        
        UpdateVirtualCursorPosition(virtualCursorPos);
        
        // 2. 전환 애니메이션 재생
        await PlayTeleportAnimationAsync();
        
        // 3. 실제 커서 텔레포트
        var teleportResult = await ExecuteTeleportAsync(message.Position);
        
        // 4. 상태 업데이트
        UpdateMultiCursorState(message.TouchpadId, new Point(message.Position.X, message.Position.Y));
        
        // 5. 응답 전송
        await SendTeleportResponseAsync(teleportResult);
    }
    catch (Exception ex)
    {
        Logger.Error($"Failed to handle multi-cursor switch: {ex.Message}");
        await SendTeleportResponseAsync(new TeleportResult 
        { 
            Success = false, 
            ErrorCode = "EXCEPTION", 
            ErrorMessage = ex.Message 
        });
    }
}
```

#### 3.6.7 성능 지표 및 품질 보증

**성능 목표 정의** (Phase 2 통신 안정화):
```csharp
private const int TARGET_TELEPORT_LATENCY_MS = 50;  // Android 앱 500ms 타임아웃의 1/10
private const int TARGET_RENDER_FPS = 60;
private const int TARGET_STATE_SYNC_INTERVAL_MS = 100;
private const int MAX_MEMORY_USAGE_MB = 20;
```

**멀티 커서 처리 시간 목표 설정 근거** (Phase 2 통신 안정화):
- **목표 처리 시간**: 50ms 이내
- **설정 근거**:
  - Android 앱의 멀티 커서 ACK 타임아웃: 500ms
  - 타임아웃 시간 = 처리 시간 × 10 (안전 마진)
  - 따라서 처리 시간 목표 = 500ms ÷ 10 = 50ms
- **처리 단계별 예상 시간**:
  - Vendor CDC 프레임 수신 및 파싱: 5ms
  - JSON 파싱 및 검증: 3ms
  - 가상 커서 위치 업데이트: 2ms
  - 텔레포트 애니메이션 시작: 5ms
  - 실제 커서 텔레포트 (Win32 API): 10ms
  - 상태 업데이트 및 응답 생성: 5ms
  - Vendor CDC 프레임 전송: 5ms
  - **총합**: 35ms (목표 50ms 이내)
- **성능 모니터링**: 50ms 초과 시 경고 로그 기록
- **최악의 경우 대응**: 시스템 부하 상황에서도 100ms 이내 처리 보장

**성능 모니터링 구현**:
```csharp
public class MultiCursorPerformanceMonitor
{
    private readonly List<long> _teleportLatencies = new List<long>();
    private readonly List<double> _renderFrameTimes = new List<double>();
    
    public void RecordTeleportLatency(long latencyMs)
    {
        _teleportLatencies.Add(latencyMs);
        
        if (_teleportLatencies.Count > 100)
            _teleportLatencies.RemoveAt(0);
        
        // 목표 초과 시 경고
        if (latencyMs > TARGET_TELEPORT_LATENCY_MS)
        {
            Logger.Warn($"Teleport latency exceeded target: {latencyMs}ms > {TARGET_TELEPORT_LATENCY_MS}ms");
        }
    }
    
    public void RecordRenderFrameTime(double frameTimeMs)
    {
        _renderFrameTimes.Add(frameTimeMs);
        
        if (_renderFrameTimes.Count > 180) // 3초치 프레임 (60fps 기준)
            _renderFrameTimes.RemoveAt(0);
        
        var targetFrameTime = 1000.0 / TARGET_RENDER_FPS;
        if (frameTimeMs > targetFrameTime)
        {
            Logger.Warn($"Render frame time exceeded target: {frameTimeMs:F2}ms > {targetFrameTime:F2}ms");
        }
    }
    
    public PerformanceMetrics GetMetrics()
    {
        return new PerformanceMetrics
        {
            AvgTeleportLatency = _teleportLatencies.Count > 0 ? _teleportLatencies.Average() : 0,
            MaxTeleportLatency = _teleportLatencies.Count > 0 ? _teleportLatencies.Max() : 0,
            AvgRenderFps = _renderFrameTimes.Count > 0 ? 1000.0 / _renderFrameTimes.Average() : 0,
            MinRenderFps = _renderFrameTimes.Count > 0 ? 1000.0 / _renderFrameTimes.Max() : 0,
            CurrentMemoryUsageMB = GC.GetTotalMemory(false) / (1024.0 * 1024.0)
        };
    }
}

public class PerformanceMetrics
{
    public double AvgTeleportLatency { get; set; }
    public long MaxTeleportLatency { get; set; }
    public double AvgRenderFps { get; set; }
    public double MinRenderFps { get; set; }
    public double CurrentMemoryUsageMB { get; set; }
}
```

### 3.7 성능 모니터링 기술 구현

**실시간 성능 지표 수집**:
- **지연시간 모니터링**: Android 기기와의 통신 지연시간 실시간 측정
- **CPU 사용률 추적**: 서버 프로그램의 CPU 사용량 모니터링
- **메모리 사용량 감시**: 메모리 사용량 및 메모리 누수 감지
- **연결 품질 평가**: Keep-alive 상태, 패킷 손실률 등 연결 안정성 지표

**성능 데이터 처리**:
- **시계열 데이터 관리**: 성능 지표의 시간별 변화 추적
- **임계값 기반 알림**: 성능 저하 감지 시 자동 알림 시스템
- **통계 분석**: 평균, 최대, 최소값 등 성능 통계 산출
- **트렌드 분석**: 장기간 성능 변화 패턴 분석

### 3.8 설정 관리 기술 구현

**설정 저장소 설계**:
- **계층적 설정 구조**: 연결, 보안, 성능, 시작 설정의 논리적 그룹화
- **동적 설정 변경**: 실행 중 설정 변경 시 즉시 적용 메커니즘
- **설정 검증**: 입력값 유효성 검사 및 충돌 방지
- **백업 및 복원**: 설정 백업 및 기본값 복원 기능

**설정 동기화**:
- **실시간 적용**: 설정 변경 시 관련 모듈에 즉시 반영
- **일관성 보장**: 설정 간 의존성 관리 및 충돌 해결
- **영구 저장**: 설정 변경사항의 안전한 영구 저장

### 3.9 스플래시 시스템 기술 구현

**애니메이션 시퀀스 설계**:
- **6단계 애니메이션**: 검은 배경 → 브릿지 등장 → 별 회전 → 정지 → 확장 → 텍스트 등장
- **총 지속 시간**: 2.5초 (각 단계별 정확한 타이밍 제어)
- **60fps 타겟팅**: 부드러운 애니메이션을 위한 프레임 레이트 관리

**리소스 관리**:
- **비동기 로딩**: 애니메이션과 병렬로 시스템 초기화 진행
- **메모리 최적화**: 애니메이션 리소스의 효율적 로딩 및 해제
- **접근성 고려**: 시스템 모션 감소 설정 감지 시 단순 페이드인 사용

**WPF/XAML 기반 설계 요구사항**:

**XAML 구조 설계**:
- **Window 설정**: `WindowStyle="None"`, `AllowsTransparency="True"`, `WindowStartupLocation="CenterScreen"`
- **Canvas 컨테이너**: 절대 위치 기반 레이아웃으로 정확한 애니메이션 제어
- **리소스 정의**: 색상, 브러시, 스타일을 ResourceDictionary로 중앙 관리
- **Path 요소**: 브릿지와 별의 기하학적 형태를 Path.Data로 정의

**Storyboard 기반 애니메이션 시스템**:
- **타임라인 제어**: `BeginTime` 속성으로 순차적 애니메이션 실행
- **Duration 정밀성**: 각 애니메이션의 정확한 지속시간 설정
- **이징 함수**: `QuadraticEase`, `CubicEase` 등을 활용한 자연스러운 움직임
- **EventTrigger**: 윈도우 Loaded 이벤트에서 Storyboard 자동 시작

**속성 기반 애니메이션 설계**:
- **StrokeDashOffset**: 브릿지 Wipe-in을 위한 대시 패턴 애니메이션
- **RotateTransform**: 별 회전을 위한 각도 속성 애니메이션
- **ScaleTransform**: 별 크기 확장을 위한 스케일 속성 애니메이션
- **Opacity**: 텍스트 페이드 인을 위한 투명도 애니메이션

**윈도우 생명주기 통합**:
- **시작 조건**: 프로그램 시작 시 스플래시 윈도우 최우선 표시
- **완료 처리**: 애니메이션 완료 이벤트에서 메인 윈도우 표시 및 스플래시 윈도우 닫기
- **예외 처리**: 사용자 클릭이나 키 입력 시 스킵 기능
- **리소스 관리**: 윈도우 닫기 시 모든 애니메이션 리소스 정리

**성능 및 품질 목표**:

**렌더링 성능**:
- **프레임률**: 60fps 안정적 유지 (16.67ms 프레임 시간)
- **DirectX 가속**: WPF의 하드웨어 가속 렌더링 파이프라인 활용
- **벡터 최적화**: Path 기반 벡터 그래픽으로 확장성과 효율성 확보
- **메모리 효율**: Storyboard 완료 후 즉시 리소스 해제

**호환성 목표**:
- **Windows 버전**: Windows 10 1903+ (90% 사용자 커버리지)
- **.NET Framework**: .NET 8.0 이상 또는 .NET Framework 4.8
- **해상도 지원**: 96 DPI부터 고해상도 디스플레이까지 DPI 인식
- **멀티 모니터**: 다양한 모니터 구성에서 일관된 중앙 배치

**품질 검증 기준**:
- **시각적 정확성**: 디자인 스펙과의 픽셀 단위 일치
- **타이밍 정확성**: 각 단계별 시간 오차 ±30ms 이내 (Windows 타이머 정밀도 고려)
- **부드러움**: GPU 가속을 통한 끊김 없는 애니메이션
- **일관성**: 다양한 하드웨어 환경에서 동일한 결과

### 3.10 Orbit 프로그램 연동 및 매크로 실행 시스템

> **개요**: Windows 서버는 ESP32-S3로부터 수신한 매크로 실행 요청을 외부 매크로 실행 엔진인 Orbit 프로그램으로 중계하고, 실행 결과를 다시 ESP32-S3로 전달하는 중계 역할을 수행합니다.

#### 3.10.1 Named Pipe 기반 IPC 아키텍처

**Named Pipe 서버 설계**:
- **Pipe 이름**: `\\.\pipe\BridgeOne_Orbit` (고정)
- **통신 모드**: 비동기 양방향 (Asynchronous Duplex)
- **메시지 모드**: Byte Stream (JSON 메시지 단위)
- **버퍼 크기**: 입력 4096 bytes, 출력 4096 bytes
- **보안 설정**: ACL 기반 접근 제어 (동일 사용자 계정만 접근 가능)

**연결 관리 설계**:
- **서버 시작**: BridgeOne Windows 서버 시작 시 Named Pipe 서버 자동 생성
- **클라이언트 대기**: 비동기 `WaitForConnectionAsync()` 호출로 Orbit 연결 대기
- **동시 연결**: 단일 Orbit 인스턴스만 연결 허용 (다중 연결 시 새 연결 거부)
- **연결 상태 추적**: `isOrbitConnected` 플래그로 연결 상태 관리

**Keep-alive 메커니즘**:
- **Ping 간격**: 10초 주기로 Ping 메시지 전송
- **Pong 타임아웃**: 5초 내 Pong 응답 없으면 연결 끊김으로 간주
- **자동 재연결**: 연결 끊김 감지 시 3초 후 자동 재연결 대기 (최대 3회 시도)

#### 3.10.2 메시지 프로토콜 및 JSON 구조

**Windows → Orbit (매크로 실행 명령)**:

```json
{
  "message_id": "uuid-v4",
  "command": "MACRO_EXECUTE",
  "timestamp": "2025-10-21T12:34:56.789Z",
  "payload": {
    "macro_id": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

**필드 설명**:
- `message_id`: Windows 서버에서 생성하는 UUID v4 형식의 메시지 고유 식별자 (응답 매칭용)
- `command`: 명령 유형 (고정값: `"MACRO_EXECUTE"`)
- `timestamp`: ISO 8601 형식의 요청 시각
- `payload.macro_id`: Vendor CDC로 수신한 UUID v4 형식의 매크로 고유 식별자

**Orbit → Windows (매크로 실행 결과 응답)**:

```json
{
  "message_id": "uuid-v4",
  "command": "MACRO_RESULT",
  "timestamp": "2025-10-21T12:35:01.234Z",
  "payload": {
    "success": true,
    "error_message": null
  }
}
```

**필드 설명**:
- `message_id`: 원본 요청의 `message_id`와 동일 (요청-응답 매칭용)
- `command`: 응답 유형 (고정값: `"MACRO_RESULT"`)
- `timestamp`: ISO 8601 형식의 응답 시각
- `payload.success`: 매크로 실행 성공 여부 (Boolean)
- `payload.error_message`: 실패 시 오류 메시지 (성공 시 `null`, 실패 시 문자열)

**오류 응답 예시**:

```json
{
  "message_id": "uuid-v4",
  "command": "MACRO_RESULT",
  "timestamp": "2025-10-21T12:35:01.234Z",
  "payload": {
    "success": false,
    "error_message": "MACRO_NOT_FOUND"
  }
}
```

**Keep-alive Ping/Pong 메시지**:

```json
// Windows → Orbit (Ping)
{
  "message_id": "uuid-v4",
  "command": "PING",
  "timestamp": "2025-10-21T12:34:56.789Z"
}

// Orbit → Windows (Pong)
{
  "message_id": "uuid-v4",
  "command": "PONG",
  "timestamp": "2025-10-21T12:34:56.890Z"
}
```

#### 3.10.3 매크로 실행 메시지 라우팅 시스템

**요청 경로 (Vendor CDC → Named Pipe)**:

1. **Vendor CDC 수신**: ESP32-S3로부터 `MACRO_START_REQUEST` JSON 수신
   ```json
   {
     "command": "MACRO_START_REQUEST",
     "macro_id": "550e8400-e29b-41d4-a716-446655440000",
     "timestamp": "2025-10-21T12:34:56.789Z"
   }
   ```

2. **요청 큐 등록**:
   - UUID v4 형식의 `message_id` 생성 (예: `7a3b2f8c-9d1e-4f5a-8b7c-6d5e4f3a2b1c`)
   - 요청 매칭 큐에 등록: `pendingRequests[message_id] = {macro_id, timestamp, timeout_timer}`
   - 30초 타임아웃 타이머 시작

3. **Orbit 연결 상태 확인**:
   - `isOrbitConnected == false`: 즉시 `ORBIT_NOT_CONNECTED` 오류 응답 생성 후 ESP32-S3로 전송
   - `isOrbitConnected == true`: 다음 단계 진행

4. **Named Pipe 전송**:
   - JSON 메시지 생성 (§3.10.2 구조 준수)
   - Named Pipe를 통해 Orbit으로 전송
   - 전송 실패 시 `PIPE_WRITE_ERROR` 오류 처리

**응답 경로 (Named Pipe → Vendor CDC)**:

1. **Named Pipe 수신**: Orbit으로부터 `MACRO_RESULT` JSON 수신

2. **응답 매칭**:
   - `message_id`로 `pendingRequests` 큐에서 원본 요청 검색
   - 매칭 성공: 타임아웃 타이머 취소 및 큐에서 제거
   - 매칭 실패: 로그 기록 후 무시 (이미 타임아웃된 요청)

3. **Vendor CDC 프레임 재구성**:
   - JSON 메시지 생성:
   ```json
   {
     "command": "MACRO_RESULT",
     "macro_id": "550e8400-e29b-41d4-a716-446655440000",
     "timestamp": "2025-10-21T12:35:01.234Z",
     "payload": {
       "success": true,
       "error_message": null
     }
   }
   ```
   - Vendor CDC 프레임으로 재구성 (0xFF 헤더, CRC16 체크섬 추가)

4. **ESP32-S3 전송**: Vendor CDC 인터페이스를 통해 ESP32-S3로 전송

#### 3.10.4 오류 처리 및 타임아웃 메커니즘

**Orbit 미연결 오류**:
- **감지 시점**: `MACRO_START_REQUEST` 수신 시 `isOrbitConnected == false` 확인
- **응답 시간**: 즉시 (< 10ms)
- **응답 메시지**:
  ```json
  {
    "command": "MACRO_RESULT",
    "macro_id": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": "2025-10-21T12:34:56.800Z",
    "payload": {
      "success": false,
      "error_message": "ORBIT_NOT_CONNECTED"
    }
  }
  ```
- **Windows 서버 처리**: 오류 응답을 Vendor CDC로 즉시 전송

**매크로 실행 타임아웃**:
- **타임아웃 시간**: 30,000ms (30초)
- **타이머 시작**: `MACRO_START_REQUEST` 수신 및 Named Pipe 전송 직후
- **타임아웃 응답**:
  ```json
  {
    "command": "MACRO_RESULT",
    "macro_id": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": "2025-10-21T12:35:26.789Z",
    "payload": {
      "success": false,
      "error_message": "MACRO_TIMEOUT"
    }
  }
  ```
- **큐 정리**: 타임아웃 발생 시 `pendingRequests`에서 해당 요청 제거
- **Windows 서버 처리**: 타임아웃 응답을 Vendor CDC로 전송

**JSON 파싱 오류**:
- **감지 시점**: Named Pipe에서 수신한 메시지의 JSON 파싱 실패
- **로그 기록**: 원본 메시지 및 파싱 오류 상세 정보 로그 기록
- **응답 처리**: 해당 메시지 무시 (잘못된 형식이므로 응답 불가)
- **재연결 판단**: 연속 3회 파싱 오류 발생 시 Orbit 연결 끊김으로 간주

**Named Pipe 연결 끊김**:
- **감지 시점**: Named Pipe Write/Read 실패 시 (예외 발생)
- **즉시 처리**:
  - `isOrbitConnected = false`로 설정
  - `pendingRequests`의 모든 요청에 대해 `PIPE_DISCONNECTED` 오류 응답 전송
  - 요청 큐 전체 초기화
- **자동 재연결**:
  - 3초 후 Named Pipe 서버 재생성 및 연결 대기
  - 최대 3회 재시도, 실패 시 수동 Orbit 재시작 필요
- **상태 알림**: GUI 트레이 아이콘에 Orbit 연결 끊김 상태 표시

#### 3.10.5 C# .NET Named Pipe 서버 설계 명세

**`NamedPipeServerStream` 사용 권장 구조**:
```csharp
// 네임스페이스: System.IO.Pipes
// 서버 생성 시 설정:
// - pipeName: "BridgeOne_Orbit"
// - direction: PipeDirection.InOut
// - maxNumberOfServerInstances: 1
// - transmissionMode: PipeTransmissionMode.Byte
// - options: PipeOptions.Asynchronous
```

**비동기 연결 대기**:
```csharp
// await pipeServer.WaitForConnectionAsync(cancellationToken);
// Orbit 연결 시 isOrbitConnected = true 설정
// 연결 완료 후 Read/Write 비동기 작업 시작
```

**JSON 메시지 송수신**:
```csharp
// 송신: JSON 직렬화 → UTF-8 인코딩 → StreamWriter로 전송 → '\n' 구분자 추가
// 수신: StreamReader로 '\n'까지 읽기 → UTF-8 디코딩 → JSON 역직렬화
```

**보안 설정 (ACL)**:
```csharp
// PipeSecurity 설정으로 현재 사용자 계정만 접근 허용
// 관리자 권한 없이 동작 가능하도록 설계
```

#### 3.10.6 Orbit 프로그램 클라이언트 요구사항

> **Note**: Orbit 프로그램은 BridgeOne과 독립적인 외부 프로그램입니다. 상세 구현은 Orbit 프로그램 전용 문서를 참조하세요.

**Named Pipe 클라이언트 최소 요구사항**:
- **연결**: `\\.\pipe\BridgeOne_Orbit` 파이프에 연결
- **메시지 형식**: JSON 텍스트 스트림 (개행 문자 `\n`로 구분)
- **인코딩**: UTF-8
- **Keep-alive**: 10초 주기 PONG 응답 필수

**필수 구현 메서드**:
- **매크로 실행**: `MACRO_EXECUTE` 명령 수신 → 매크로 실행 → `MACRO_RESULT` 응답
- **Keep-alive 응답**: `PING` 수신 → `PONG` 응답 (5초 이내)

#### 3.10.7 Windows 서버의 매크로 중계 처리

> **전체 시스템 플로우**: 전체 플랫폼에 걸친 매크로 실행 시퀀스는 [`technical-specification.md` §4.4.1.1]을 참조하세요.

**Windows 서버 역할 요약**:

**매크로 요청 수신 (Vendor CDC)**:
```
Vendor CDC 수신 → JSON 파싱
  ↓ message_id 생성: "7a3b2f8c-..."
  ↓ pendingRequests 큐 등록 (30초 타이머 시작)
  ↓ isOrbitConnected 확인
  ↓ Named Pipe 전송 → Orbit
```

**매크로 결과 중계 (Named Pipe → Vendor CDC)**:
```
Named Pipe 수신 → JSON 파싱
  ↓ message_id로 pendingRequests 큐 검색
  ↓ 원본 macro_id 매칭
  ↓ 타이머 취소 및 큐에서 제거
  ↓ Vendor CDC 프레임 재구성
  ↓ ESP32-S3로 전송
```

**핵심 처리 로직**:
- message_id 생성 및 요청-응답 매칭 관리
- Orbit 연결 상태 실시간 확인
- 30초 타임아웃 타이머 관리
- 양방향 메시지 형식 변환 (Vendor CDC ↔ Named Pipe)

#### 3.10.8 오류 시나리오별 처리 전략

| 오류 시나리오 | 감지 위치 | 응답 코드 | 응답 시간 | Windows 서버 처리 |
|------------|---------|---------|---------|----------------|
| Orbit 미연결 | Windows 서버 | `ORBIT_NOT_CONNECTED` | 즉시 | 즉시 ESP32-S3로 오류 응답 전송 |
| 매크로 타임아웃 | Windows 서버 | `MACRO_TIMEOUT` | 30초 | 타임아웃 타이머 만료 시 오류 응답 전송 및 큐 정리 |
| 존재하지 않는 매크로 | Orbit | `MACRO_NOT_FOUND` | 1초 이내 | Named Pipe로 수신 후 ESP32-S3로 중계 |
| 매크로 실행 실패 | Orbit | `EXECUTION_ERROR: {상세 메시지}` | 가변 | Named Pipe로 수신 후 ESP32-S3로 중계 |
| JSON 파싱 오류 | Windows 서버 | `INVALID_FORMAT` | 즉시 | 로그 기록 후 무시 (응답 불가) |
| Named Pipe 끊김 | Windows 서버 | `PIPE_DISCONNECTED` | 3초 | 모든 대기 요청에 오류 응답 전송 후 재연결 시도 |

#### 3.10.9 성능 및 신뢰성 요구사항

**응답 시간 목표**:
- **정상 플로우**: Vendor CDC 수신 → Orbit 실행 완료 → Vendor CDC 응답 전송까지 3초 이내 (매크로 실행 시간 제외)
- **오류 응답**: Orbit 미연결 감지 → Vendor CDC 오류 응답 전송까지 10ms 이내

**동시 처리 능력**:
- **순차 처리**: 동시에 여러 매크로 요청이 들어올 경우 순차적으로 처리 (큐 기반)
- **큐 최대 크기**: 10개 요청까지 대기 가능, 초과 시 `QUEUE_FULL` 오류 반환

**메모리 관리**:
- **요청 큐 크기**: `pendingRequests` Dictionary 최대 10개 항목 유지
- **타임아웃 정리**: 타임아웃 발생 시 해당 항목 즉시 제거로 메모리 누수 방지

**로그 기록**:
- **디버그 로그**: 모든 Named Pipe 송수신 메시지, 요청 큐 상태 변경
- **오류 로그**: JSON 파싱 실패, Named Pipe 연결 끊김, 타임아웃 발생
- **통계 로그**: 매크로 실행 횟수, 평균 응답 시간, 오류 발생 빈도

## 4. 주요 기술 플로우

### 4.1 초기 설치 및 설정 기술 플로우

**MSI 패키지 설치 과정**:
- **권한 요청**: 관리자 권한 획득 및 UAC 처리
- **파일 배포**: 실행 파일, 설정 파일, 리소스 파일의 시스템 배치
- **레지스트리 등록**: 시스템 서비스 등록 및 자동 시작 설정
- **방화벽 규칙**: Windows Defender 방화벽 예외 규칙 자동 생성

**초기 구성 마법사**:
- **기본 설정 적용**: 최적화된 기본값으로 자동 구성
- **포트 할당**: 사용 가능한 포트 자동 탐지 및 할당
- **보안 설정**: 기본 보안 정책 적용 및 인증서 설치
- **서비스 시작**: Windows 서비스 등록 및 자동 시작 설정

### 4.2 연결 관리 기술 플로우

**연결 확립 과정**:
- **포트 스캔**: 사용 가능한 USB 포트 및 ESP32-S3 동글 감지
- **연결 시도**: 감지된 동글과의 초기 통신 시도
- **핸드셰이크**: 보안 인증 및 상태 동기화 수행
- **Keep-alive 시작**: 연결 유지를 위한 주기적 신호 시작

**연결 상태 모니터링**:
- **품질 감시**: 지연시간, 패킷 손실률 등 연결 품질 지표 추적
- **이상 감지**: 연결 불안정 징후 조기 발견
- **자동 복구**: 연결 문제 발생 시 자동 재연결 시도
- **상태 알림**: GUI 및 트레이 아이콘을 통한 상태 표시

### 4.3 오류 처리 및 복구 기술 플로우

**자동 복구 시스템**:
- **지수 백오프**: 재연결 시도 간격의 점진적 증가
- **대체 포트 시도**: 기본 포트 실패 시 다른 포트로 자동 전환
- **방화벽 규칙 재생성**: 방화벽 차단 시 규칙 자동 재생성
- **서비스 재시작**: 심각한 오류 시 서비스 자동 재시작

**비정상 종료 복구**:
- **안전한 정리**: 진행 중인 작업의 안전한 중단
- **입력 해제**: 모든 눌린 키/마우스 버튼의 강제 해제
- **커서 안전 위치**: 커서를 화면 중앙 등 안전한 위치로 이동
- **가상 커서 정리**: 모든 가상 커서 오버레이 제거

### 4.4 멀티 커서 관리 기술 플로우

**모드 전환 과정**:
- **모드 활성화**: 싱글 → 멀티 커서 모드 전환 시 가상 커서 생성
- **커서 동기화**: 실제 커서와 가상 커서의 초기 위치 동기화
- **상태 반영**: 현재 커서 상태의 가상 커서 이미지 반영
- **애니메이션**: 200ms fade-in 애니메이션으로 부드러운 등장

**텔레포트 실행**:
- **전환 감지**: Vendor CDC를 통한 커서 전환 요청 수신
- **위치 계산**: 목표 가상 커서 위치 계산 및 검증
- **커서 이동**: 실제 커서의 순간 위치 이동 (50ms 이내)
- **시각적 피드백**: 3단계 애니메이션을 통한 전환 표시

**가상 커서 관리**:
- **실시간 업데이트**: 16ms 주기로 가상 커서 위치 및 상태 업데이트
- **상태 동기화**: 100ms 주기로 실제 커서 상태 확인 및 반영
- **렌더링 최적화**: 변경된 영역만 선택적으로 다시 그리기

## 5. UI 및 시스템 통합

### 5.1 Windows API 통합

**시스템 레벨 통합 요구사항**:
- **커서 제어**: 시스템 커서 위치 조회, 설정, 상태 감지
- **입력 시뮬레이션**: 마우스/키보드 입력의 하드웨어 수준 시뮬레이션
- **키보드 입력 처리**: SendInput API를 통한 키 이벤트 생성 및 전송
- **키 상태 관리**: GetKeyState, GetAsyncKeyState API를 통한 실시간 키 상태 추적
- **시스템 이벤트**: Windows 메시지 루프 및 이벤트 처리
- **권한 관리**: 관리자 권한 요청 및 보안 정책 준수

**플랫폼 호환성 설계**:
- **Windows 버전**: Windows 10/11 호환성 보장
- **아키텍처 지원**: x86, x64, ARM64 아키텍처 대응
- **DPI 인식**: 고해상도 디스플레이에서의 정확한 좌표 처리
- **다중 모니터**: 멀티 모니터 환경에서의 좌표 변환

### 5.2 WPF UI 기술 구현

**UI 프레임워크 설계**:
- **Fluent Design 적용**: Windows 11 네이티브 디자인 언어 구현
- **MVVM 패턴**: Model-View-ViewModel 아키텍처 적용
- **데이터 바인딩**: 실시간 데이터 업데이트를 위한 양방향 바인딩
- **커맨드 패턴**: 사용자 액션 처리를 위한 커맨드 시스템

**테마 시스템 구현**:
- **동적 테마 전환**: 시스템 테마 변경 감지 및 자동 적용
- **Mica 배경 효과**: Windows 11 반투명 배경 효과 구현
- **액센트 색상**: 시스템 액센트 색상 자동 감지 및 적용

**애니메이션 엔진**:
- **Storyboard 기반**: WPF Storyboard를 활용한 부드러운 애니메이션
- **성능 최적화**: GPU 가속을 활용한 효율적 렌더링
- **접근성 고려**: 시스템 모션 감소 설정 감지 및 대응

### 5.3 접근성 기술 구현

**스크린 리더 지원**:
- **자동화 속성**: UI 요소별 접근성 속성 정의
- **키보드 네비게이션**: Tab 순서 및 포커스 관리
- **음성 피드백**: 상태 변경 시 적절한 음성 안내

**시각적 접근성**:
- **색상 대비**: WCAG 2.1 AA 기준 준수 (최소 4.5:1)
- **색상 독립성**: 색상에만 의존하지 않는 정보 전달
- **크기 조절**: 사용자 정의 UI 크기 조절 지원

**마우스 접근성**:
- **충분한 클릭 영역**: 최소 44x44px 터치 영역 보장
- **명확한 호버 효과**: 마우스 상호작용 가능 영역 시각적 표시
- **드래그 앤 드롭**: 접근성을 고려한 드래그 앤 드롭 구현

### 5.4 색상 시스템 기술 구현

**색상 시스템 아키텍처 설계**:
- **색상 리소스 관리자**: 중앙화된 색상 리소스 관리 시스템
- **테마별 색상 분리**: 다크/라이트 테마별 독립적인 색상 리소스 관리
- **동적 색상 로딩**: 런타임에 테마에 따른 색상 리소스 동적 로딩
- **색상 캐싱 시스템**: 자주 사용되는 색상의 메모리 캐싱으로 성능 최적화

**동적 테마 감지 및 적용 메커니즘**:
- **시스템 테마 이벤트 감지**: Windows 시스템 테마 변경 이벤트 실시간 감지
- **테마 전환 처리**: 테마 변경 시 즉시 색상 리소스 업데이트 및 UI 재렌더링
- **시스템 액센트 색상 동기화**: Windows 11 시스템 액센트 색상과의 실시간 동기화
- **Mica 배경 효과 연동**: 시스템 테마 변경에 따른 Mica 배경 효과 자동 조정

**색상 시스템 성능 최적화**:
- **색상 리소스 메모리 풀링**: 색상 브러시 객체의 재사용을 통한 메모리 효율성
- **GPU 가속 색상 렌더링**: 하드웨어 가속을 활용한 색상 렌더링 최적화
- **색상 계산 캐싱**: 복잡한 색상 계산 결과의 캐싱으로 CPU 사용량 최적화
- **비동기 색상 로딩**: 대용량 색상 리소스의 비동기 로딩으로 UI 응답성 보장

**시스템 통합 및 호환성**:
- **Windows API 색상 시스템 통합**: Windows 시스템 색상 API와의 완전한 통합
- **접근성 색상 대비 검증**: WCAG 2.1 AA 기준 준수를 위한 실시간 대비율 검증
- **DPI 인식 색상 처리**: 고해상도 디스플레이에서의 정확한 색상 렌더링

**색상 시스템 보안 및 안정성**:
- **색상 리소스 검증**: 로드된 색상 리소스의 유효성 검증 및 오류 처리
- **메모리 누수 방지**: 색상 리소스의 안전한 생성 및 해제 메커니즘
- **예외 처리**: 색상 시스템 오류 시 안전한 폴백 색상 적용
- **성능 모니터링**: 색상 시스템 성능 지표 실시간 모니터링

## 6. 성능 최적화 및 리소스 관리

### 6.1 저지연 처리 설계

**성능 목표**:
- **전체 입력 지연**: 50ms 이내 기여
- **커서 전환 지연**: 50ms 이내
- **애니메이션 프레임율**: 60fps 유지
- **CPU 사용률**: 최대 15% 제한

**최적화 전략**:
- **고우선순위 스레드**: 입력 처리를 위한 전용 고우선순위 스레드 풀
- **메모리 풀링**: GC 압박 최소화를 위한 객체 재사용
- **비동기 처리**: 블로킹 작업의 비동기 처리로 응답성 확보
- **캐싱 시스템**: 자주 사용되는 리소스의 메모리 캐싱

### 6.2 리소스 관리 설계

**메모리 관리**:
- **약한 참조**: 대용량 리소스의 약한 참조 활용
- **버퍼 풀링**: 작은 크기 버퍼의 재사용 풀
- **주기적 정리**: 30초 주기의 메모리 정리 작업
- **임계값 기반 GC**: 메모리 사용량 임계값 초과 시에만 강제 GC

**USB 통신 최적화**:
- **프레임 재사용**: Keep-alive 프레임의 사전 구성 및 재사용
- **단일 트랜잭션**: USB 인터럽트 전송으로 저지연 보장
- **오류 복구**: 통신 오류 시 빠른 복구 메커니즘
- **연결 풀링**: USB 연결의 효율적 관리

## 7. 보안 및 배포

### 7.1 시스템 요구사항

**운영체제 및 런타임**:
- **Windows 11** (권장) / **Windows 10** (최소)
- **.NET 6+ Runtime** 설치 필요
- **관리자 권한** (Windows API 호출용)

**하드웨어 요구사항**:
- **RAM**: 최소 100MB, 권장 256MB
- **저장공간**: 50MB (로그 파일 포함)
- **USB 포트**: ESP32-S3 동글 연결용

### 7.2 보안 환경 설정

**EDR/Anti-virus 대응**:
- **코드 서명**: 신뢰할 수 있는 인증서를 통한 코드 서명
- **화이트리스트 등록**: Windows Defender 예외 요청
- **방화벽 규칙**: 자동 방화벽 예외 규칙 생성
- **권한 최소화**: 필요한 최소 권한만 요청

### 7.3 데이터 보호

**암호화 및 개인정보 보호**:
- **설정 암호화**: 사용자 설정의 DPAPI 기반 암호화
- **통신 보안**: 핸드셰이크 과정의 보안 인증
- **로그 보호**: 민감한 정보의 로그 제외
- **임시 파일**: 임시 파일의 안전한 생성 및 삭제

## 8. 상수 및 설정 정의

### 8.1 통신 관련 상수

```
KEEP_ALIVE_INTERVAL_MS = 500          // Keep-alive 신호 주기
KEEP_ALIVE_MISSED_THRESHOLD = 3       // 연결 불안정 판정 임계값
CONNECTION_TIMEOUT_MS = 5000          // 연결 타임아웃
HANDSHAKE_TIMEOUT_MS = 10000          // 핸드셰이크 타임아웃
FRAME_SIZE_BYTES = 8                  // 프레임 크기 (고정)
```

### 8.2 성능 관련 상수

```
E2E_LATENCY_TARGET_MS = 50            // 엔드투엔드 지연시간 목표
MAX_CPU_USAGE_PERCENT = 15            // 최대 CPU 사용률
MAX_MEMORY_USAGE_MB = 256             // 최대 메모리 사용량
RENDER_FPS_TARGET = 60                // 렌더링 목표 FPS
```

### 8.3 키보드 입력 관련 상수

```
KEY_STICKY_HOLD_THRESHOLD_MS = 500    // Sticky Hold 활성화 임계시간
REINPUT_DEBOUNCE_MS = 150             // 중복 입력 방지 디바운스 시간
KEYBOARD_INPUT_LATENCY_TARGET_MS = 10 // 키보드 입력 처리 목표 지연시간
KEY_STATE_SYNC_INTERVAL_MS = 100      // 키 상태 동기화 주기
MODIFIER_KEY_TIMEOUT_MS = 5000        // 모디파이어 키 자동 해제 시간
```

### 8.4 멀티 커서 관련 상수

```
CURSOR_TELEPORT_ANIMATION_MS = 150    // 커서 텔레포트 애니메이션 시간
VIRTUAL_CURSOR_UPDATE_INTERVAL_MS = 16 // 가상 커서 업데이트 주기 (60fps)
CURSOR_STATE_CHECK_INTERVAL_MS = 100  // 커서 상태 확인 주기
```

### 8.5 스플래시 애니메이션 상수

```
SPLASH_TOTAL_DURATION_MS = 2500       // 전체 스플래시 시간
SPLASH_PHASE1_MS = 300                // Phase 1 지속시간
SPLASH_PHASE2_MS = 700                // Phase 2 지속시간
SPLASH_PHASE3_MS = 500                // Phase 3 지속시간
SPLASH_PHASE4_MS = 300                // Phase 4 지속시간
SPLASH_PHASE5_MS = 100                // Phase 5 지속시간
SPLASH_PHASE6_MS = 600                // Phase 6 지속시간
```

### 8.6 색상 시스템 상수

```
// 색상 대비 관련 상수
MIN_COLOR_CONTRAST_RATIO = 4.5        // 최소 색상 대비율 (WCAG 2.1 AA)
MIN_LARGE_TEXT_CONTRAST_RATIO = 3.0   // 대형 텍스트 최소 대비율
MIN_UI_COMPONENT_CONTRAST_RATIO = 3.0 // UI 컴포넌트 최소 대비율

// 투명도 관련 상수
SHADOW_OPACITY = 0.3                  // 그림자 효과 투명도 (30%)
DISABLED_OPACITY = 0.6                // Disabled 상태 투명도 (60%)
HOVER_OPACITY = 0.8                   // 호버 효과 투명도 (80%)

// 색상 시스템 성능 상수
COLOR_CACHE_SIZE = 256                // 색상 캐시 최대 크기
COLOR_LOAD_TIMEOUT_MS = 1000          // 색상 리소스 로딩 타임아웃
COLOR_VALIDATION_INTERVAL_MS = 5000   // 색상 리소스 검증 주기

// 테마 전환 관련 상수
THEME_TRANSITION_DURATION_MS = 300    // 테마 전환 애니메이션 시간
THEME_DETECTION_INTERVAL_MS = 1000    // 시스템 테마 감지 주기
ACCENT_COLOR_SYNC_INTERVAL_MS = 500   // 액센트 색상 동기화 주기
```

## 9. 참조 및 연관 문서

### 9.1 서버 프로그램 관련 문서 체계

본 문서를 포함한 Windows 서버 프로그램 관련 3개 문서는 다음과 같은 관계를 가집니다:

- **`design-guide-server.md` (디자인 가이드)**: 사용자에게 보이는 GUI의 전체적인 디자인 원칙, 사용자 경험(UX) 흐름, 각 화면의 레이아웃과 동작 방식을 정의
- **`styleframe-server.md` (스타일프레임)**: 디자인 가이드의 시각적 구현을 위한 구체적인 색상 팔레트, 타이포그래피, 아이콘, 컴포넌트 스타일 등 시각적 요소 상세화
- **`technical-guide-server.md` (본 문서, 기술 가이드)**: 눈에 보이지 않는 핵심 로직의 기술적 아키텍처 - 통신 연결, 멀티 커서 시스템, 성능 최적화 등 서버의 핵심 기능에 대한 설계 명세

### 9.2 핵심 참조 문서

**프로젝트 전체 명세**:
- `docs/PRD.md`: 전체 프로젝트 요구사항 및 목표
- `docs/technical-specification.md`: 프로토콜 및 상태 전이 명세 (SSOT)
- `docs/Board/usb-hid-bridge-architecture.md`: 동글과의 연계 구조

**Android 앱과의 연동**:
- `docs/Android/design-guide-app.md`: Android 앱과의 플로우 일관성
- `docs/Android/component-design-guide-app.md`: 고급 기능 요구사항
- `docs/Android/component-touchpad.md`: 멀티 커서 및 터치패드 명세

**구현 세부사항**:
- `.cursor/rules/` 디렉터리: C# .NET 구현 세부사항 (별도 규칙 파일들)

### 9.3 교차 참조 정책

**용어 정의**: 모든 문서에서 `technical-specification.md` §6.3 Windows 플랫폼 용어집 준수
**상수/임계값**: 본 문서의 §8 상수 정의를 단일 출처로 활용
**플로우/전환**: Android 앱 가이드와 서버 가이드 간 일관성 유지

### 9.4 문서 작성 원칙

본 문서는 `documentation-guide.md`의 원칙을 준수합니다:
- **"무엇을(What)"과 "왜(Why)"** 중심의 설계 명세
- 구체적인 구현 코드는 `.cursor/rules/` 디렉터리의 규칙 파일에서 제공
- 설계 의도와 요구사항을 명확히 정의하여 구현 가이드 생성을 지원

## 10. GUI 구현 기술 스택

### 10.1 라이브러리 선정 배경

**선정 라이브러리**: WPF UI (https://github.com/lepoco/wpfui)

**선정 이유**:
- **Fluent Design System 완벽 지원**: Windows 11 네이티브 디자인 언어 구현
- **SystemThemeWatcher 기능**: 시스템 테마 변경 실시간 감지 및 자동 적용
- **Mica 배경 효과**: Windows 11 고유의 반투명 배경 효과 지원
- **풍부한 컴포넌트**: NavigationView, Card, Button 등 styleframe 요구사항 만족
- **접근성 지원**: WCAG 2.1 AA 기준 준수 및 스크린 리더 호환
- **활발한 유지보수**: 최신 Windows 기능 업데이트 지속적 반영
- **NuGet 패키지**: 간편한 설치 및 의존성 관리

### 10.2 WPF UI 라이브러리 활용 전략

#### 10.2.1 아키텍처 설계

**MVVM 패턴 기반 구조**:
```
┌─────────────────────────────────────────────────────────┐
│                MainWindow (FluentWindow)               │
├─────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌───────────────────────────────┐  │
│  │NavigationView   │  │Main Content Area              │  │
│  │(Side Panel)    │  │┌─────────────────────────────┐│  │
│  │                 │  ││Page1 (홈 화면)             ││  │
│  │- Home           │  ││Page2 (멀티 커서)          ││  │
│  │- Multi Cursor   │  ││Page3 (시스템 설정)         ││  │
│  │- System Settings│  ││Page4 (오류 처리)           ││  │
│  │- Error Handling │  │└─────────────────────────────┘│  │
│  └─────────────────┘  └───────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

**의존성 주입 구조**:
- **IPageService**: 페이지 생성 및 관리 담당
- **INavigationService**: 페이지 전환 및 상태 관리
- **IThemeService**: 테마 변경 감지 및 적용
- **IAnimationService**: Storyboard 기반 애니메이션 관리

#### 10.2.2 Fluent Design System 구현

**SystemThemeWatcher 연동**:
```csharp
// MainWindow.xaml.cs - 시스템 테마 감지 및 자동 적용
public partial class MainWindow : FluentWindow
{
    public MainWindow()
    {
        InitializeComponent();

        Loaded += (sender, args) =>
        {
            Wpf.Ui.Appearance.SystemThemeWatcher.Watch(
                this,                                    // Window instance
                Wpf.Ui.Controls.WindowBackdropType.Mica, // Mica effect
                true                                     // Auto accent sync
            );
        };
    }
}
```

**동적 테마 전환**:
- **시스템 테마 변경 감지**: Windows 설정 변경 시 실시간 반영
- **Mica 배경 효과**: Windows 11 반투명 배경 자동 적용
- **Accent 색상 동기화**: 시스템 액센트 색상과 자동 동기화
- **부드러운 전환**: 300ms 애니메이션으로 자연스러운 테마 전환

#### 10.2.3 NavigationView 기반 화면 전환

**선언적 네비게이션 구성**:
```xml
<!-- MainWindow.xaml - 사이드바 네비게이션 -->
<ui:NavigationView x:Name="RootNavigationView"
                   PaneDisplayMode="Left"
                   IsBackButtonVisible="Collapsed">
    <ui:NavigationView.MenuItems>
        <ui:NavigationViewItem Content="홈"
                               Icon="{ui:SymbolIcon Home24}"
                               TargetPageType="{x:Type pages:HomePage}" />
        <ui:NavigationViewItem Content="멀티 커서"
                               Icon="{ui:SymbolIcon Cursor24}"
                               TargetPageType="{x:Type pages:MultiCursorPage}" />
        <ui:NavigationViewItem Content="시스템 설정"
                               Icon="{ui:SymbolIcon Settings24}"
                               TargetPageType="{x:Type pages:SystemSettingsPage}" />
    </ui:NavigationView.MenuItems>
</ui:NavigationView>
```

**페이지 전환 애니메이션**:
- **슬라이드 전환**: 좌우 슬라이드 애니메이션 (300ms)
- **페이드 효과**: 콘텐츠 영역 페이드 인/아웃 (150ms)
- **상태 기반 전환**: 연결 상태별 다른 애니메이션 적용

#### 10.2.4 접근성 지원 구현

**WCAG 2.1 AA 기준 준수**:
- **색상 대비율**: 최소 4.5:1 (일반 텍스트), 3:1 (대형 텍스트)
- **키보드 네비게이션**: Tab 순서 및 포커스 표시
- **스크린 리더 지원**: 모든 UI 요소에 적절한 접근성 속성
- **터치 영역**: 최소 44x44px 클릭 영역 보장

**자동화 속성 적용**:
```csharp
// 각 컨트롤에 접근성 속성 자동 설정
public void SetAccessibilityProperties(FrameworkElement element)
{
    // 버튼의 경우
    if (element is Button button)
    {
        AutomationProperties.SetName(button, "연결 상태 확인");
        AutomationProperties.SetHelpText(button, "현재 Android 기기 연결 상태를 확인합니다");
        AutomationProperties.SetAutomationId(button, "connection_check_button");
    }
}
```

#### 10.2.5 애니메이션 시스템 설계

**Storyboard 기반 애니메이션 관리**:
- **스플래시 애니메이션**: 6단계 시퀀스 (총 2.5초)
- **페이지 전환**: 슬라이드 + 페이드 조합 애니메이션
- **상태 변경**: 연결 상태별 색상 애니메이션
- **마이크로 인터랙션**: 호버, 클릭 효과

**성능 최적화**:
- **GPU 가속**: 모든 애니메이션 하드웨어 가속 적용
- **프레임 레이트**: 60fps 안정적 유지
- **메모리 관리**: 애니메이션 완료 후 리소스 자동 정리

#### 10.2.6 반응형 디자인 구현

**AdaptiveTrigger 활용**:
```xml
<!-- App.xaml - 반응형 트리거 정의 -->
<ui:ThemesDictionary Theme="Dark">
    <ui:ThemesDictionary.AdaptiveTriggers>
        <AdaptiveTrigger MinWindowWidth="1200">
            <!-- Large 모드: 3열 레이아웃 -->
        </AdaptiveTrigger>
        <AdaptiveTrigger MinWindowWidth="900">
            <!-- Medium 모드: 2열 레이아웃 -->
        </AdaptiveTrigger>
        <AdaptiveTrigger MinWindowWidth="0">
            <!-- Compact 모드: 1열 레이아웃 -->
        </AdaptiveTrigger>
    </ui:ThemesDictionary.AdaptiveTriggers>
</ui:ThemesDictionary>
```

**동적 레이아웃 전환**:
- **Compact 모드** (<900px): 네비게이션 자동 축소
- **Medium 모드** (900-1200px): 표준 2열 레이아웃
- **Large 모드** (>1200px): 확장 3열 레이아웃

### 10.3 품질 보증 기준

**시각적 품질**:
- Fluent Design System 가이드라인 100% 준수
- Windows 11/10 환경에서 일관된 시각적 경험
- 60fps 애니메이션 부드러운 재생

**접근성 품질**:
- WCAG 2.1 AA 기준 100% 만족
- 키보드만으로 모든 기능 접근 가능
- 스크린 리더 완벽 지원

**성능 품질**:
- 초기 로딩 시간 2초 이내
- 메모리 사용량 100MB 이내
- CPU 사용률 15% 미만

### 10.4 확장성 고려사항

**플러그인 아키텍처**:
- 새로운 화면 추가 시 최소 코드 변경
- 커스텀 애니메이션 쉽게 추가 가능
- 테마 확장 용이한 구조

**유지보수성**:
- 코드 중복 최소화
- 설정 변경 시 실시간 반영
- 자동화된 테스트 케이스 준비
