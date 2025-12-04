# BridgeOne Communication Protocol Reference

BridgeOne 시스템의 통신 프로토콜 상세 명세입니다. UART 프레임 구조와 USB HID 프로토콜을 포함합니다.

---

## 통신 흐름 개요

```
Android 앱 → [UART 델타 프레임] → ESP32-S3 → [USB HID] → PC
   │                                  │
   └─ 8바이트 복합 프레임 ─────────────┴─ Boot Protocol (BIOS 호환)
```

---

## UART 델타 프레임 (Android → ESP32-S3)

### 프레임 구조

```c
// 8바이트 고정 크기 복합 프레임 (Little-Endian)
struct BridgeFrame {
    uint8_t  seq;       // [0] 순번 (유실 감지용, 0~255 순환)
    uint8_t  buttons;   // [1] 마우스 버튼: bit0: L, bit1: R, bit2: M
    int8_t   deltaX;    // [2] 상대 X 이동 (-127~127)
    int8_t   deltaY;    // [3] 상대 Y 이동 (-127~127)
    int8_t   wheel;     // [4] 휠 스크롤
    uint8_t  modifiers; // [5] 수정자 키: Ctrl, Shift, Alt, Win
    uint8_t  keyCode1;  // [6] 주 키코드 (HID Usage)
    uint8_t  keyCode2;  // [7] 보조 키코드 (HID Usage)
};
```

### 필드 상세

#### seq (순번)
- **크기**: 1 byte (uint8_t)
- **범위**: 0~255 (순환)
- **용도**: 패킷 유실 감지
- **동작**: 매 프레임마다 1씩 증가, 255 다음 0으로 순환

#### buttons (마우스 버튼)
- **크기**: 1 byte (uint8_t)
- **비트 구조**:
  ```
  bit 0: 왼쪽 버튼 (0x01)
  bit 1: 오른쪽 버튼 (0x02)
  bit 2: 가운데 버튼 (0x04)
  bit 3-7: 예약됨
  ```

#### deltaX, deltaY (상대 이동)
- **크기**: 각 1 byte (int8_t)
- **범위**: -127 ~ +127
- **단위**: 픽셀 (상대 이동량)
- **부호**: 양수 = 오른쪽/아래, 음수 = 왼쪽/위

#### wheel (휠 스크롤)
- **크기**: 1 byte (int8_t)
- **범위**: -127 ~ +127
- **부호**: 양수 = 위로 스크롤, 음수 = 아래로 스크롤

#### modifiers (수정자 키)
- **크기**: 1 byte (uint8_t)
- **비트 구조** (HID 표준):
  ```
  bit 0: Left Ctrl (0x01)
  bit 1: Left Shift (0x02)
  bit 2: Left Alt (0x04)
  bit 3: Left Win/GUI (0x08)
  bit 4: Right Ctrl (0x10)
  bit 5: Right Shift (0x20)
  bit 6: Right Alt (0x40)
  bit 7: Right Win/GUI (0x80)
  ```

#### keyCode1, keyCode2 (HID 키코드)
- **크기**: 각 1 byte (uint8_t)
- **범위**: 0x00 ~ 0xFF (HID Usage Table)
- **값 0x00**: 키 없음
- **동시 입력**: 최대 2개 키 동시 전송

### 전송 특성

| 항목 | 값 |
|------|-----|
| 프레임 크기 | 8 bytes (고정) |
| 전송 주기 | 4-8ms (125-250 Hz) |
| UART 속도 | 1 Mbps |
| 데이터 비트 | 8 |
| 패리티 | None |
| 정지 비트 | 1 |
| 바이트 순서 | Little-Endian |

### 예제 프레임

```c
// 마우스 왼쪽 클릭 + 오른쪽으로 10px 이동
{0x01, 0x01, 0x0A, 0x00, 0x00, 0x00, 0x00, 0x00}
// seq=1, buttons=L, deltaX=10, deltaY=0, wheel=0, mod=0, key1=0, key2=0

// Ctrl+C 키 입력
{0x02, 0x00, 0x00, 0x00, 0x00, 0x01, 0x06, 0x00}
// seq=2, buttons=0, deltaX=0, deltaY=0, wheel=0, mod=LCtrl, key1=C(0x06), key2=0

// 마우스 휠 위로 3칸
{0x03, 0x00, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00}
// seq=3, buttons=0, deltaX=0, deltaY=0, wheel=3, mod=0, key1=0, key2=0
```

---

## USB HID 프로토콜 (ESP32-S3 → PC)

### Boot Protocol 사용 이유

- **BIOS/UEFI 호환**: 부팅 시 키보드/마우스 동작 가능
- **BitLocker 지원**: 암호화 해제 화면에서 입력 가능
- **드라이버 불필요**: OS 기본 드라이버로 동작
- **최대 호환성**: 모든 PC 환경에서 동작 보장

### 마우스 리포트 (Boot Protocol)

```c
// Boot Protocol - 마우스 (3 bytes)
struct HidMouseReport {
    uint8_t buttons;   // [0] 마우스 버튼 상태
    int8_t  x;         // [1] X 이동 (-127~127)
    int8_t  y;         // [2] Y 이동 (-127~127)
};
```

#### 버튼 비트맵
```
bit 0: 왼쪽 버튼
bit 1: 오른쪽 버튼
bit 2: 가운데 버튼
```

### 키보드 리포트 (Boot Protocol)

```c
// Boot Protocol - 키보드 (8 bytes)
struct HidKeyboardReport {
    uint8_t modifiers;      // [0] 수정자 키 (Ctrl, Shift, Alt, Win)
    uint8_t reserved;       // [1] 예약됨 (항상 0x00)
    uint8_t keycodes[6];    // [2-7] 동시에 눌린 키 (최대 6개)
};
```

#### 수정자 키 비트맵
```
bit 0: Left Ctrl
bit 1: Left Shift
bit 2: Left Alt
bit 3: Left GUI (Win)
bit 4: Right Ctrl
bit 5: Right Shift
bit 6: Right Alt
bit 7: Right GUI (Win)
```

---

## HID 키코드 매핑 (자주 사용)

### 알파벳
| 키 | HID 코드 | 키 | HID 코드 |
|----|---------|----|---------|
| A | 0x04 | N | 0x11 |
| B | 0x05 | O | 0x12 |
| C | 0x06 | P | 0x13 |
| D | 0x07 | Q | 0x14 |
| E | 0x08 | R | 0x15 |
| F | 0x09 | S | 0x16 |
| G | 0x0A | T | 0x17 |
| H | 0x0B | U | 0x18 |
| I | 0x0C | V | 0x19 |
| J | 0x0D | W | 0x1A |
| K | 0x0E | X | 0x1B |
| L | 0x0F | Y | 0x1C |
| M | 0x10 | Z | 0x1D |

### 숫자
| 키 | HID 코드 |
|----|---------|
| 1 | 0x1E |
| 2 | 0x1F |
| 3 | 0x20 |
| 4 | 0x21 |
| 5 | 0x22 |
| 6 | 0x23 |
| 7 | 0x24 |
| 8 | 0x25 |
| 9 | 0x26 |
| 0 | 0x27 |

### 기능 키
| 키 | HID 코드 | 키 | HID 코드 |
|----|---------|----|---------|
| Enter | 0x28 | Tab | 0x2B |
| Escape | 0x29 | Space | 0x2C |
| Backspace | 0x2A | Delete | 0x4C |
| F1 | 0x3A | F7 | 0x40 |
| F2 | 0x3B | F8 | 0x41 |
| F3 | 0x3C | F9 | 0x42 |
| F4 | 0x3D | F10 | 0x43 |
| F5 | 0x3E | F11 | 0x44 |
| F6 | 0x3F | F12 | 0x45 |

### 방향 키
| 키 | HID 코드 |
|----|---------|
| Right Arrow | 0x4F |
| Left Arrow | 0x50 |
| Down Arrow | 0x51 |
| Up Arrow | 0x52 |

---

## 프로토콜 변환 로직 (ESP32-S3)

### UART → HID 변환 흐름

```c
void process_bridge_frame(BridgeFrame* frame) {
    // 1. 마우스 리포트 생성
    if (frame->deltaX != 0 || frame->deltaY != 0 || frame->buttons != 0) {
        HidMouseReport mouse = {
            .buttons = frame->buttons,
            .x = frame->deltaX,
            .y = frame->deltaY
        };
        tud_hid_mouse_report(REPORT_ID_MOUSE, mouse.buttons, mouse.x, mouse.y, frame->wheel, 0);
    }

    // 2. 키보드 리포트 생성
    if (frame->keyCode1 != 0 || frame->keyCode2 != 0 || frame->modifiers != 0) {
        uint8_t keycodes[6] = {frame->keyCode1, frame->keyCode2, 0, 0, 0, 0};
        tud_hid_keyboard_report(REPORT_ID_KEYBOARD, frame->modifiers, keycodes);
    }
}
```

### 키 릴리스 처리

```c
// 키가 떼어진 경우 빈 리포트 전송
if (prev_keyCode1 != 0 && frame->keyCode1 == 0) {
    uint8_t empty[6] = {0};
    tud_hid_keyboard_report(REPORT_ID_KEYBOARD, 0, empty);
}
```

---

## 참고 자료

- [USB HID Usage Tables](https://usb.org/document-library/hid-usage-tables-15)
- [USB Device Class Definition for HID](https://www.usb.org/document-library/device-class-definition-hid-111)
- [TinyUSB HID Documentation](https://docs.tinyusb.org/en/latest/reference/class/hid.html)
