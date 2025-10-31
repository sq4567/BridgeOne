/**
 * @file usb_descriptors.c
 * @brief BridgeOne USB Composite 디바이스 디스크립터 구현
 * 
 * 역할:
 * - TinyUSB 디스크립터 콜백 함수 구현 (Device, Config, HID Report, String)
 * - HID Boot Keyboard/Mouse 리포트 디스크립터 정의
 * - String Descriptor (제조사, 제품명, 시리얼 번호)
 * 
 * 참고:
 * - 이 파일의 디스크립터 정의는 esp32s3-code-implementation-guide.md §1.3 규칙을 준수합니다
 * - 인터페이스 순서는 절대 변경 불가 (Keyboard→Mouse→CDC 순서 고정)
 * - HID Report Descriptor는 Boot Protocol 표준을 따름
 */

#include <string.h>
#include "tusb.h"
#include "usb_descriptors.h"

// ==================== 1. Device Descriptor ====================
/**
 * USB Device Descriptor 정의
 * 
 * 디바이스의 기본 정보(VID/PID, USB 버전, 클래스)를 호스트에 제공
 */
tusb_desc_device_t const desc_device = {
    .bLength            = sizeof(tusb_desc_device_t),
    .bDescriptorType    = TUSB_DESC_DEVICE,
    .bcdUSB             = 0x0200,          // USB 2.0 (BCD: 2.0.0)
    
    // 복합 디바이스는 클래스를 0x00으로 설정하고 각 인터페이스에서 클래스 정의
    .bDeviceClass       = 0x00,
    .bDeviceSubClass    = 0x00,
    .bDeviceProtocol    = 0x00,
    
    .bMaxPacketSize0    = 64,              // Control EP 최대 패킷 크기
    
    // Vendor/Product ID (0x303A: Espressif VID, 0x4001: BridgeOne PID)
    .idVendor           = USB_VID,
    .idProduct          = USB_PID,
    .bcdDevice          = 0x0100,          // 버전 1.0.0
    
    // String Descriptor 인덱스 (1-based)
    .iManufacturer      = 0x01,
    .iProduct           = 0x02,
    .iSerialNumber      = 0x03,
    
    .bNumConfigurations = 0x01             // 단일 Configuration 지원
};

/**
 * Device Descriptor 콜백 함수
 * 
 * TinyUSB에서 호스트가 Device Descriptor를 요청할 때 호출
 * 
 * 반환값: Device Descriptor 배열 포인터
 */
uint8_t const* tud_descriptor_device_cb(void) {
    return (uint8_t const*)&desc_device;
}

// ==================== 2. HID Report Descriptors ====================
/**
 * HID Boot Keyboard Report Descriptor (8바이트 고정)
 * 
 * 구조:
 * - [0] Modifier keys (Ctrl, Shift, Alt, GUI) - 1 byte
 * - [1] Reserved (0x00) - 1 byte
 * - [2-7] Key codes (6-key rollover) - 6 bytes
 * 
 * TUD_HID_REPORT_DESC_KEYBOARD 매크로: Boot Protocol 표준 준수
 */
uint8_t const desc_hid_keyboard_report[] = {
    TUD_HID_REPORT_DESC_KEYBOARD()
};

/**
 * HID Boot Mouse Report Descriptor (4바이트 고정)
 * 
 * 구조:
 * - [0] Buttons (Left, Right, Middle) - 1 byte
 * - [1] Delta X (-127~127) - 1 byte signed
 * - [2] Delta Y (-127~127) - 1 byte signed
 * - [3] Wheel (-127~127) - 1 byte signed
 * 
 * TUD_HID_REPORT_DESC_MOUSE 매크로: Boot Protocol 표준 준수
 */
uint8_t const desc_hid_mouse_report[] = {
    TUD_HID_REPORT_DESC_MOUSE()
};

/**
 * HID Report Descriptor 콜백 함수
 * 
 * 호스트가 HID Report Descriptor를 요청할 때 호출
 * 
 * 매개변수:
 * - instance: Configuration Descriptor에 정의된 HID 인터페이스 번호
 * 
 * 반환값: 해당 인터페이스의 HID Report Descriptor 배열 포인터
 */
uint8_t const* tud_hid_descriptor_report_cb(uint8_t instance) {
    if (instance == ITF_NUM_HID_KEYBOARD) {
        return desc_hid_keyboard_report;
    } else if (instance == ITF_NUM_HID_MOUSE) {
        return desc_hid_mouse_report;
    }
    return NULL;
}

/**
 * HID Get Report 콜백 함수
 * 
 * 호스트가 현재 HID 리포트 상태를 요청할 때 호출됨
 * 
 * 참고: 이는 hid_handler.c에서 구현됩니다.
 */
// 함수 구현은 hid_handler.c로 이동됨

/**
 * HID Set Report 콜백 함수
 * 
 * 호스트가 HID 디바이스의 상태를 설정할 때 호출됨 (예: LED)
 * 
 * 참고: 이는 hid_handler.c에서 구현됩니다.
 */
// 함수 구현은 hid_handler.c로 이동됨

// ==================== 3. Configuration Descriptor ====================
/**
 * Configuration Descriptor 배열
 * 
 * 디바이스가 지원하는 인터페이스(HID Keyboard, HID Mouse, CDC)의 구성을 정의
 * 
 * 구성 순서 (절대 변경 금지):
 * 1. Configuration 정보 (9 bytes)
 * 2. Interface 0: HID Boot Keyboard (9 bytes)
 * 3. Interface 1: HID Boot Mouse (9 bytes)
 * 4. Interface 2/3: CDC-ACM (66 bytes)
 */
uint8_t const desc_configuration[] = {
    // ========== Configuration Descriptor ==========
    // 총 4개 인터페이스, 500mA 전력 소모 요청
    TUD_CONFIG_DESCRIPTOR(
        1,                              // bConfigurationValue
        ITF_NUM_TOTAL,                  // bNumInterfaces (4개)
        0,                              // iConfiguration (String ID, 0=없음)
        CONFIG_TOTAL_LEN,               // wTotalLength (전체 크기)
        TUSB_DESC_CONFIG_ATT_REMOTE_WAKEUP, // bmAttributes (Remote Wakeup 지원)
        500                             // bMaxPower (500mA = 250 units × 2mA)
    ),
    
    // ========== Interface 0: HID Boot Keyboard ==========
    // HID Descriptor 매크로: (Interface#, String ID, Protocol, ReportDesc Size, Endpoint, MaxPacketSize, PollInterval)
    TUD_HID_DESCRIPTOR(
        ITF_NUM_HID_KEYBOARD,           // bInterfaceNumber
        0,                              // iInterface (String ID, 0=없음)
        HID_ITF_PROTOCOL_KEYBOARD,      // bInterfaceProtocol (Boot Keyboard)
        sizeof(desc_hid_keyboard_report), // wDescriptorLength (Report Descriptor 크기)
        EPNUM_HID_KB,                   // bEndpointAddress (IN Endpoint)
        CFG_TUD_HID_EP_BUFSIZE,         // wMaxPacketSize (64 bytes)
        1                               // bInterval (1ms 폴링)
    ),
    
    // ========== Interface 1: HID Boot Mouse ==========
    TUD_HID_DESCRIPTOR(
        ITF_NUM_HID_MOUSE,              // bInterfaceNumber
        0,                              // iInterface (String ID, 0=없음)
        HID_ITF_PROTOCOL_MOUSE,         // bInterfaceProtocol (Boot Mouse)
        sizeof(desc_hid_mouse_report),  // wDescriptorLength (Report Descriptor 크기)
        EPNUM_HID_MOUSE,                // bEndpointAddress (IN Endpoint)
        CFG_TUD_HID_EP_BUFSIZE,         // wMaxPacketSize (64 bytes)
        1                               // bInterval (1ms 폴링)
    ),
    
    // ========== Interface 2/3: CDC-ACM (Communication + Data) ==========
    // CDC Descriptor 매크로: (Comm Interface#, String ID, NotifEP, MaxPacketSize, DataOUT EP, DataIN EP, MaxPacketSize)
    TUD_CDC_DESCRIPTOR(
        ITF_NUM_CDC_COMM,               // bInterfaceNumber (Comm Interface)
        4,                              // iInterface (String ID: "BridgeOne CDC")
        EPNUM_CDC_NOTIF,                // bEndpointAddress (Notification IN)
        8,                              // bMaxPacketSize (Notification EP는 작음)
        EPNUM_CDC_OUT,                  // bEndpointAddress (Data OUT)
        EPNUM_CDC_IN,                   // bEndpointAddress (Data IN)
        CFG_TUD_CDC_EP_BUFSIZE          // wMaxPacketSize (64 bytes)
    )
};

/**
 * Configuration Descriptor 콜백 함수
 * 
 * 호스트가 Configuration Descriptor를 요청할 때 호출
 * 
 * 매개변수:
 * - index: Configuration 인덱스 (이 프로젝트는 단일 Configuration만 지원)
 * 
 * 반환값: Configuration Descriptor 배열 포인터
 */
uint8_t const* tud_descriptor_configuration_cb(uint8_t index) {
    (void)index;  // 단일 Configuration만 지원하므로 index는 무시
    return desc_configuration;
}

// ==================== 4. String Descriptors ====================
/**
 * String Descriptor 배열
 * 
 * 인덱스:
 * 0: Language ID (US English: 0x0409)
 * 1: Manufacturer ("Chatterbones")
 * 2: Product ("BridgeOne USB Bridge")
 * 3: Serial Number ("00000001")
 * 4: CDC Interface ("BridgeOne CDC")
 */
char const* string_desc_arr[] = {
    (const char[]){ 0x09, 0x04 },           // 0: Language ID (US English)
    "Chatterbones",                          // 1: Manufacturer
    "BridgeOne USB Bridge",                  // 2: Product
    "00000001",                              // 3: Serial Number
    "BridgeOne Vendor CDC",                  // 4: CDC Interface Description
};

/**
 * String Descriptor 콜백 함수
 * 
 * 호스트가 String Descriptor를 요청할 때 호출
 * ASCII 문자열을 UTF-16LE 형식으로 변환하여 반환
 * 
 * 매개변수:
 * - index: String Descriptor 인덱스
 * - langid: Language ID (이 프로젝트는 US English만 지원)
 * 
 * 반환값: UTF-16LE 형식의 String Descriptor 배열 포인터
 */
uint16_t const* tud_descriptor_string_cb(uint8_t index, uint16_t langid) {
    (void)langid;  // 단일 언어만 지원하므로 langid는 무시
    
    static uint16_t _desc_str[32];
    uint8_t chr_count;
    
    if (index == 0) {
        // Language ID Descriptor
        memcpy(&_desc_str[1], string_desc_arr[0], 2);
        chr_count = 1;
    } else {
        // String Descriptor 인덱스 범위 확인
        if (index >= sizeof(string_desc_arr) / sizeof(string_desc_arr[0])) {
            return NULL;
        }
        
        const char* str = string_desc_arr[index];
        chr_count = strlen(str);
        if (chr_count > 31) chr_count = 31;  // 최대 31자 제한
        
        // ASCII → UTF-16LE 변환
        // UTF-16LE: 각 ASCII 문자가 2바이트 (LowByte, HighByte)로 확장
        for (uint8_t i = 0; i < chr_count; i++) {
            _desc_str[1 + i] = str[i];
        }
    }
    
    // String Descriptor 헤더 구성
    // 바이트 0: 크기 (2 + chr_count × 2)
    // 바이트 1: 타입 (0x03 = String Descriptor)
    // 구성: (Type << 8) | Length
    _desc_str[0] = (TUSB_DESC_STRING << 8) | (2 * chr_count + 2);
    
    return _desc_str;
}
