---
title: "BridgeOne Phase 3.2: Windows 서버 CDC 연결 계층 구현"
description: "BridgeOne 프로젝트 Phase 3.2 - Windows 서버의 ESP32-S3 CDC 자동 감지, 연결 및 Vendor CDC 프레임 송수신"
tags: ["windows", "cdc", "serial-port", "wpf", "mvvm", "vendor-cdc"]
version: "v1.0"
owner: "Chatterbones"
updated: "2026-03-19"
---

# BridgeOne Phase 3.2: Windows 서버 CDC 연결 계층 구현

**개발 기간**: 4-6일

**목표**: Windows 서버가 ESP32-S3 동글의 CDC COM 포트를 자동 감지하고 Vendor CDC 프레임을 송수신할 수 있도록 합니다.

**핵심 성과물**:
- ESP32-S3 CDC COM 포트 자동 감지 및 연결
- Vendor CDC 프레임 프로토콜 구현 (C# 측)
- CRC16-CCITT 구현 (ESP32-S3 측과 동일 알고리즘)
- USB 핫플러그 감지 (장치 연결/해제 이벤트)
- MVVM 아키텍처 기반 연결 상태 UI

**선행 조건**: Phase 3.1 (ESP32-S3 Vendor CDC 핸들러) 완료

---

## 📋 Phase 3.1 완료 사항 요약

- ESP32-S3 Vendor CDC 프레임 파서 및 CRC16 검증 구현 완료
- CDC RX 멀티플렉싱 (디버그 텍스트 ↔ Vendor CDC 바이너리 공존)
- 명령 코드 디스패처 및 FreeRTOS vendor_cdc_task 구동
- PC에서 수동으로 0xFF 바이너리 프레임 전송 시 ESP32-S3 파싱 성공 확인

---

## Phase 3.2.1: USB CDC Serial Port 자동 감지 및 연결 ✅ 완료

**목표**: Windows에서 ESP32-S3 동글의 CDC 가상 COM 포트를 자동 감지하고 SerialPort로 연결

**구현 완료 내역**:
1. NuGet 패키지 추가:
   - `System.IO.Ports` v9.0.3: SerialPort 클래스
   - `System.Management` v9.0.3: WMI 쿼리 (USB 장치 정보 조회)
2. `CdcConnectionService` 클래스 구현:
   - WMI `Win32_PnPEntity`를 통한 USB 장치 열거
   - VID=0x303A / PID=0x4001 필터로 ESP32-S3 CDC 장치 검색
   - 장치 이름에서 COM 포트 번호 추출 (예: "USB Serial Device (COM3)" → "COM3")
   - SerialPort 열기 (115200, 8N1, DTR/RTS 활성화)
   - C# 이벤트 기반 연결 상태 알림 (`StateChanged`, `ErrorOccurred`)
   - `ConnectionState` enum: Disconnected / Connecting / Connected / Error
   - `Port` 프로퍼티를 public으로 노출 (Phase 3.2.2에서 `BaseStream` 접근용)
3. USB 핫플러그 감지:
   - `WqlEventQuery`로 `__InstanceCreationEvent` / `__InstanceDeletionEvent` 모니터링
   - 장치 연결 시 1.5초 대기 후 자동 COM 포트 감지 및 연결 시도
   - 장치 제거 시 SerialPort 정리 및 연결 해제
   - `SynchronizationContext`를 통한 UI 스레드 안전성 보장
4. `DeviceInfo` 모델 클래스:
   - Vid, Pid, ComPort, Description, SerialNumber, PnpDeviceId 필드

**생성된 파일**:
- `src/windows/BridgeOne/Services/CdcConnectionService.cs`
- `src/windows/BridgeOne/Models/DeviceInfo.cs`

**수정된 파일**:
- `src/windows/BridgeOne/BridgeOne.csproj`: NuGet 패키지 추가

**⚠️ 후속 Phase 영향 사항**:
- `CdcConnectionService.Port` (public `SerialPort?`)로 Phase 3.2.2에서 `BaseStream` 직접 접근 가능
- `ConnectionState`는 `CdcConnectionService.ConnectionState`로 참조 (내부 enum)
- 상태 변경 구독은 `StateChanged` 이벤트 사용 (IObservable 아님)

**검증**:
- [x] `CdcConnectionService` 클래스 구현됨
- [x] VID=0x303A / PID=0x4001 장치의 CDC COM 포트 자동 감지
- [x] SerialPort 열기/닫기 정상 동작
- [x] USB 핫플러그: 장치 연결 시 자동 감지
- [x] USB 핫플러그: 장치 제거 시 자동 cleanup
- [x] COM 포트 미발견 시 적절한 오류 처리
- [x] `dotnet build` 성공

---

## Phase 3.2.2: Vendor CDC 프레임 프로토콜 구현 (Windows 측)

**목표**: Windows 서버에서 Vendor CDC 바이너리 프레임을 조립/파싱하고, 디버그 텍스트와 분리하는 프로토콜 계층 구현

**개발 기간**: 1.5-2일

**세부 목표**:
1. `Crc16` 정적 클래스 구현:
   - CRC16-CCITT (다항식 0x1021, 초기값 0x0000)
   - ESP32-S3 측과 **정확히 동일한 알고리즘** (payload만 CRC 대상)
   - `ushort Calculate(byte[] data, int offset, int length)` 메서드
2. `VendorCdcFrame` 레코드/클래스 구현:
   - Command (byte), Payload (byte[]), IsValid (bool)
   - `ToBytes()`: 프레임 직렬화 (0xFF + command + length_LE + payload + crc16_LE)
   - `static Parse(byte[] data)`: 바이트 배열에서 프레임 역직렬화
3. `VendorCdcProtocol` 클래스 구현:
   - `CdcConnectionService`를 DI로 주입받아 `Port.BaseStream`으로 비동기 I/O
   - 비동기 수신 루프: `SerialPort.BaseStream.ReadAsync()`
   - 수신 버퍼에서 0xFF 헤더 탐색 + 길이 기반 프레임 추출
   - **디버그 텍스트 분리**: 0xFF가 아닌 바이트는 디버그 로그 채널로 전달
   - 파싱된 프레임을 `Channel<VendorCdcFrame>`으로 전달 (producer-consumer)
   - CRC 오류 시 프레임 폐기 (기술 명세 §2.3.3 오류 복구 전략: 타임아웃 기반 자동 재시도)
4. 프레임 전송:
   - `SendFrameAsync(byte command, string jsonPayload)` 메서드
   - JSON → UTF-8 인코딩 → 프레임 조립 → CRC 부착 → SerialPort 전송

**신규 파일**:
- `src/windows/BridgeOne/Protocol/Crc16.cs`
- `src/windows/BridgeOne/Protocol/VendorCdcFrame.cs`
- `src/windows/BridgeOne/Protocol/VendorCdcProtocol.cs`

**참조 문서 및 섹션**:
- `docs/windows/technical-specification-server.md` §2.3.3 Vendor CDC 프레임 구조
- `docs/windows/technical-specification-server.md` CRC16 C# 참조 코드 (246~261행)
- `docs/windows/technical-specification-server.md` Vendor CDC 오류 처리 (144~262행)

**⚠️ CRC16 교차 검증 필수**:
- ESP32-S3 (C)와 Windows (C#)에서 동일 페이로드에 대해 동일 CRC 값 산출 확인
- 테스트 벡터: 빈 페이로드 "", "PING", 448바이트 최대 페이로드
- Byte order 주의: length와 CRC16 모두 Little-Endian

**검증**:
- [ ] CRC16-CCITT 구현됨 (다항식 0x1021, 초기값 0x0000)
- [ ] ESP32-S3와 동일 테스트 벡터로 CRC 결과 일치 확인
- [ ] `VendorCdcFrame.ToBytes()` 정상 동작 (프레임 직렬화)
- [ ] 수신 루프에서 0xFF 프레임과 디버그 텍스트 정상 분리
- [ ] 프레임 전송 (`SendFrameAsync`) 후 ESP32-S3에서 수신 확인
- [ ] ESP32-S3에서 보낸 응답 프레임을 서버가 올바르게 수신 및 CRC 검증
- [ ] CRC 오류 시 프레임 폐기 동작 확인 (타임아웃 기반 재시도)
- [ ] `dotnet build` 성공

---

## Phase 3.2.3: MVVM 아키텍처 및 연결 상태 UI

**목표**: DI 컨테이너 설정, ViewModel 구현, MainWindow에 연결 상태 UI 추가

**개발 기간**: 1-2일

**세부 목표**:
1. DI(Dependency Injection) 컨테이너 설정:
   - `App.xaml.cs`에서 `ServiceCollection` 구성
   - `CdcConnectionService`, `VendorCdcProtocol` 싱글톤 등록
   - ViewModel 등록
2. `MainViewModel` 구현:
   - 앱 전체 상태 관리
   - 하위 ViewModel 생성 및 관리
3. `ConnectionViewModel` 구현 (CommunityToolkit.Mvvm 활용):
   - `[ObservableProperty]` 연결 상태: `CdcConnectionService.ConnectionState` enum 사용
   - `[ObservableProperty]` COM 포트 정보
   - `[ObservableProperty]` 디바이스 정보 (펌웨어 버전 등)
   - `[RelayCommand]` 수동 연결/해제
   - `CdcConnectionService.StateChanged` 이벤트 구독 (C# 이벤트 기반)
4. MainWindow UI 업데이트:
   - 상태바에 연결 정보 표시 (COM 포트, 연결 상태 아이콘)
   - "Hello World" 텍스트 → 연결 상태 대시보드로 교체
   - 연결 상태에 따른 색상 변경:
     - Disconnected: 회색
     - Connecting: 노란색 (깜빡임)
     - Connected: 녹색
     - Error: 빨간색
5. 디버그 로그 뷰 (선택):
   - ESP32-S3에서 수신된 디버그 텍스트 로그를 별도 패널에 표시
   - 스크롤 가능한 텍스트 영역

**신규 파일**:
- `src/windows/BridgeOne/ViewModels/MainViewModel.cs`
- `src/windows/BridgeOne/ViewModels/ConnectionViewModel.cs`

**수정 파일**:
- `src/windows/BridgeOne/App.xaml.cs`: DI 컨테이너 설정
- `src/windows/BridgeOne/MainWindow.xaml`: 연결 상태 UI 추가
- `src/windows/BridgeOne/MainWindow.xaml.cs`: DataContext 설정

**참조 문서 및 섹션**:
- `docs/windows/design-guide-server.md` - Windows 서버 디자인 가이드
- `docs/windows/styleframe-server.md` - Windows 서버 스타일프레임
- CommunityToolkit.Mvvm 문서: `ObservableObject`, `RelayCommand`, Source Generators

**검증**:
- [ ] DI 컨테이너에 모든 서비스 등록됨
- [ ] `ConnectionViewModel`이 연결 상태 변경을 실시간 반영
- [ ] MainWindow에 연결 상태 표시됨 (4가지 상태 색상)
- [ ] COM 포트 정보가 UI에 표시됨
- [ ] 수동 연결/해제 버튼 동작
- [ ] USB 케이블 분리 시 UI가 Disconnected로 전환
- [ ] `dotnet build` 성공 및 앱 실행 가능

---

## Phase 3.2 E2E 검증

Phase 3.2 완료 후 수행할 통합 테스트:

1. **양방향 프레임 전송**: Windows 서버에서 0xFF 프레임 전송 → ESP32-S3 수신 확인 → 응답 프레임 → Windows 수신 확인
2. **CRC 교차 검증**: 동일 payload에 대해 양쪽 CRC16 값 일치 확인
3. **디버그 로그 분리**: ESP32-S3의 ESP_LOG 출력이 Windows에서 텍스트로 표시되고, Vendor CDC 프레임과 간섭하지 않음
4. **핫플러그**: USB 케이블 연결 → 자동 감지 → 분리 → 정상 해제 → 재연결 → 자동 복구
5. **UI 상태**: 각 단계에서 ConnectionViewModel 상태가 올바르게 변경됨

---

## Phase 3.2 핵심 성과

**Phase 3.2 완료 시 달성되는 상태**:
- ✅ Windows 서버가 ESP32-S3 CDC COM 포트를 자동 감지하고 연결
- ✅ Vendor CDC 바이너리 프레임 양방향 송수신 가능
- ✅ CRC16 무결성 검증이 양쪽에서 동일하게 동작
- ✅ 디버그 로그와 Vendor CDC 프레임이 간섭 없이 공존
- ✅ MVVM 패턴 기반 Windows 서버 아키텍처 구축
- ✅ Phase 3.3 (핸드셰이크 프로토콜)의 선행 조건 충족
