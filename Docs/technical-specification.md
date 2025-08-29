---
title: "BridgeOne 기술 명세서"
description: "프로토콜·상태모델·알고리즘·성능·오류와 플랫폼 간 계약을 단일 출처로 규정하는 SSOT(Single Source of Truth) 문서"
tags: ["spec", "contract", "protocol", "state-machine", "algorithms"]
version: "v0.1"
owner: "Chatterbones"
updated: "2025-09-19"
note: "본 문서에 존재하는 모든 상수값 및 설정값은 초기 값으로, 확정된 고정값이 아님"
---

# BridgeOne 기술 명세서

## 용어집/정의

- Selected/Unselected: 선택 상태. 시각 강조/선택 표시. 입력 가능과 혼동 금지.
- Enabled/Disabled: 입력 가능 상태. 포인터/키 입력 허용 여부.
- Essential/Standard: 운용 상태. Windows 서버와 연결되지 않은 상태는 Essential(필수 기능), 연결된 상태는 Standard(모든 기능)입니다.
- TransportState: NoTransport | UsbOpening | UsbReady.
- RFC2119: MUST/SHOULD/MAY 규범 용어.
- 상태 용어 사용 원칙(금칙어 포함): "활성/비활성" 금지. Selected/Unselected, Enabled/Disabled로 표기[[memory:5809234]].

## 1. 목적/범위/용어

### 1.1 문서 역할 정의

본 문서는 **BridgeOne 프로젝트의 SSOT(Single Source of Truth) 규범 문서**입니다:
- **목적**: 프로토콜·상태·알고리즘·성능·오류 정책을 중앙에서 규정해 문서 간 드리프트를 방지
- **성격**: 기술 명세 및 플랫폼 간 계약서 ("**무엇을**" 해야 하는가)

### 1.2 범위 및 용어

- **범위**: 앱↔동글↔PC 입력 경로 전반. UI 시각 규칙은 `Docs/design-guide-app.md`를 참조하되, 상호작용 알고리즘은 본 문서가 우선
- **용어**: `Selected/Unselected`(선택), `Enabled/Disabled`(입력 가능), `Essential/Standard`(운용 상태), `TransportState` 등

## 2. 시스템 개요

### 2.1 하드웨어 연결 구조

```mermaid
graph TD
    subgraph "물리적 연결 구조"
        Android_App["Android 앱<br/>(Samsung Galaxy s10e)"] -- "USB-C" --> OTG_Y["OTG Y 케이블<br/>(Charge-while-OTG)"]
        OTG_Y -- "Device 포트" --> USB_Serial["USB-Serial 어댑터<br/>(CP2102, 3.3V TTL)"]
        OTG_Y -- "Power 포트" --> Charger["충전기<br/>(5V 2A+)"]
        USB_Serial -- "UART<br/>(1Mbps, 8N1)<br/>TX/RX/GND" --> ESP32_S3["ESP32-S3<br/>(Geekble nano)"]
        ESP32_S3 -- "USB Device<br/>(복합 장치)" --> PC["PC<br/>(Windows 10/11)"]
    end
    
    subgraph "전력 공급"
        Charger -- "5V 2A+" --> Android_App
    end
```

**하드웨어 구성 요소:**
- **Android 앱**: Samsung Galaxy s10e (2280×1080, 5.8인치, Android 12)
- **OTG Y 케이블**: 동시 충전 및 USB OTG 지원
- **USB-Serial 어댑터**: CP2102 칩셋, 3.3V TTL 레벨
- **ESP32-S3**: Geekble nano 보드, USB 복합 장치 구성
- **PC**: Windows 11, USB Host 포트

### 2.2 데이터 흐름 경로

```mermaid
graph TD
    subgraph "Essential 모드 데이터 흐름"
        Essential_Page["Essential 전용<br/>페이지"] -- "제한된 입력<br/>(마우스+키보드)" --> Android_App
        Android_App -- "8B 델타 프레임<br/>(HID Boot Protocol)" --> ESP32_S3
        ESP32_S3 -- "HID Boot Mouse<br/>Interface" --> HID_Mouse_Driver["HID Boot Mouse Driver"]
        ESP32_S3 -- "HID Boot Keyboard<br/>Interface" --> HID_Keyboard_Driver["HID Boot Keyboard Driver"]
        ESP32_S3 -. "Vendor-specific Interface<br/>(Disabled in Essential)" .-> Windows_Server["Windows Server<br/>(미연결)"]
        HID_Mouse_Driver --> PC_BIOS["PC (BIOS/로그인)"]
        HID_Keyboard_Driver --> PC_BIOS
    end

    subgraph "Standard 모드 데이터 흐름"
        Standard_Page["Standard 페이지<br/>(모든 기능)"] -- "전체 입력<br/>(마우스+키보드+고급)" --> Android_App
        Android_App -- "8B 델타 프레임<br/>(HID Report Protocol)" --> ESP32_S3
        Android_App -- "커스텀 명령<br/>(0xFF 헤더 + JSON)" --> ESP32_S3
        ESP32_S3 -- "HID Report Mouse<br/>Interface" --> HID_Mouse_Driver
        ESP32_S3 -- "HID Report Keyboard<br/>Interface" --> HID_Keyboard_Driver
        ESP32_S3 -- "Vendor-specific<br/>(멀티 커서, 매크로)" --> Windows_Service["Windows 서비스<br/>(고급 기능)"]
        Windows_Service -- "응답 데이터" --> ESP32_S3
        ESP32_S3 -- "응답 중계" --> Android_App
        HID_Mouse_Driver --> PC_OS["PC (Windows OS)"]
        HID_Keyboard_Driver --> PC_OS
    end
```

**데이터 흐름 특징:**
- **Essential 모드**: Windows 서버 미연결 상태로 HID Boot Protocol만 사용하여 BIOS/로그인 단계에서 동작
- **Standard 모드**: Windows 서버 연결 상태로 HID Report Protocol과 Vendor-specific를 통한 고급 기능 제공
- **기본 경로**: 8바이트 프레임을 통한 마우스/키보드 입력 (모든 모드에서 동작)
- **확장 경로**: 커스텀 명령을 통한 멀티 커서, 매크로 등 고급 기능 (Standard 모드에서만)

**프로토콜 구분:**
- **HID Boot Protocol**: Essential 모드에서 사용, BIOS/UEFI 호환성 보장
- **HID Report Protocol**: Standard 모드에서 사용, 확장 기능 지원
- **Vendor-specific**: Windows 서비스와의 양방향 통신, 고급 기능 처리

## 3. 상수/임계값 (Standardized Constants)

### 3.1 개요

> (작성 예정)

## 4. 플랫폼별 주요 로직

> (작성 예정)
