# Phase 2.1.4.2: UART 및 HID 태스크 생성 - 우선순위 조정 변경사항

**작성일**: 2025-11-01
**Phase**: Phase 2.1.4.2
**상태**: ✅ 완료
**빌드 결과**: ✅ idf.py build 성공 (바이너리: 0x385c0 bytes, 93% 여유)

---

## 📋 변경사항 개요

### 기존 계획 vs 실제 구현

#### 우선순위 설정

| 항목 | 기존 계획 | 변경 전 (Phase 2.1.2.3) | 변경 후 (Phase 2.1.4.2) | 변경 사유 |
|-----|---------|-------------------|-------------------|---------|
| UART Priority | 6 | 6 | 6 | 변경 없음 (유지) |
| HID Priority | 5 (계획) | 7 (구현됨) | 5 (조정) | 데이터 흐름과 우선순위 순서 일치 |
| USB Priority | 4 (계획) | 5 (구현됨) | 4 (조정) | HID Priority 조정에 따른 계층 조정 |

#### 우선순위 계층 변화

```
Phase 2.1.2.3 구현 후 (문제 상황):
  Priority 7: HID task (Core 0)
  Priority 6: UART task (Core 0)
  Priority 5: USB task (Core 1)
  
  문제: UART < HID (데이터 흐름과 반대)

Phase 2.1.4.2 최종 (수정됨):
  Priority 6: UART task (Core 0) ← 데이터 입수
  Priority 5: HID task (Core 0)  ← 프레임 처리
  Priority 4: USB task (Core 1)  ← USB 전송
  
  ✅ 데이터 흐름(UART → HID → USB)과 우선순위 순서 완벽 일치
```

---

## 🔧 변경 내용 상세

### 1. 코드 변경사항

#### 파일: `src/board/BridgeOne/main/BridgeOne.c`

**변경 1**: HID 태스크 우선순위 조정 (라인 134)
```c
// 변경 전
xTaskCreatePinnedToCore(
    hid_task,
    "HID",
    3072,
    NULL,
    7,  // ← Priority 7
    NULL,
    0
);

// 변경 후
xTaskCreatePinnedToCore(
    hid_task,
    "HID",
    3072,
    NULL,
    5,  // ← Priority 5 (조정됨)
    NULL,
    0
);
```

**변경 2**: USB 태스크 우선순위 조정 (라인 154)
```c
// 변경 전
xTaskCreatePinnedToCore(
    usb_task,
    "USB",
    4096,
    NULL,
    5,  // ← Priority 5
    NULL,
    1
);

// 변경 후
xTaskCreatePinnedToCore(
    usb_task,
    "USB",
    4096,
    NULL,
    4,  // ← Priority 4 (조정됨)
    NULL,
    1
);
```

**변경 3**: HID 태스크 주석 업데이트 (라인 126)
```c
// 변경 전: "우선순위 7: UART 태스크(6)보다는 낮지만 USB 태스크(5)보다 높음"
// 변경 후: "우선순위 5: UART 태스크(6)보다는 낮고, USB 태스크(4)보다는 높음 (데이터 흐름 순서)"
```

**변경 4**: USB 태스크 주석 업데이트 (라인 146)
```c
// 변경 전: "우선순위 5: 일반 우선순위 (높지 않음)"
// 변경 후: "우선순위 4: 낮은 우선순위 (데이터 처리 후 최종 전송)"
```

**변경 5**: HID 로깅 메시지 (라인 143)
```c
// 변경 전: ESP_LOGI(TAG, "HID task created (Core 0, Priority 7)");
// 변경 후: ESP_LOGI(TAG, "HID task created (Core 0, Priority 5)");
```

**변경 6**: USB 로깅 메시지 (라인 163)
```c
// 변경 전: ESP_LOGI(TAG, "USB task created (Core 1, Priority 5)");
// 변경 후: ESP_LOGI(TAG, "USB task created (Core 1, Priority 4)");
```

### 2. 문서 변경사항

#### 파일: `docs/development-plans/phase-2-communication-stabilization.md`

**변경**: Phase 2.1.4.2 검증 완료 표시
- 검증 항목 4개 모두 ✅ 체크
- "우선순위 최종 결정 및 확정 (옵션 2 적용: UART 6 > HID 5 > USB 4)" 기록

**추가**: Phase 2.1.4.3 영향 분석
- USB Priority 검증 항목 수정 (5 → 4)
- 우선순위 계층 업데이트

**추가**: Phase 2.1.5 검증 항목
- 부팅 로그에서 우선순위 확인 항목 추가
- 우선순위 순서 일치 확인 항목 추가

**추가**: Phase 2.1.4 변경사항 분석 섹션
- 기존 계획 vs 실제 구현 비교표
- 후속 Phase 영향도 분석표
- 변경 정당화

---

## 📊 영향도 분석

### 1. 기능 영향

| 항목 | 영향 | 근거 |
|-----|------|------|
| UART 수신 | ❌ 없음 | Priority 6 유지 (변경 없음) |
| HID 프레임 처리 | ✅ 개선 | Priority 조정으로 우선순위 순서 명확화 |
| USB 전송 | ❌ 없음 | 큐 기반 동작 (Priority 5→4 조정해도 무관) |
| 버그 발생 위험 | ❌ 없음 | 큐 기반 IPC → 우선순위 의존성 낮음 |

**결론**: 기능 영향 없음, 유지보수성 개선

### 2. 후속 Phase 영향

#### Phase 2.1.4.3 (USB 태스크 생성 및 TWDT 설정)
- **변경 필요**: ✅ USB Priority 검증 항목 수정 (5 → 4)
- **영향도**: 중 (검증 항목 수정)
- **상태**: ✅ 수정 완료

#### Phase 2.1.5 (펌웨어 플래싱 및 HID 인식 검증)
- **변경 필요**: ✅ 부팅 로그 검증 항목 추가
- **영향도**: 저 (검증 항목 추가만)
- **상태**: ✅ 검증 항목 추가

#### Phase 2.1.6+ (Windows HID 인식 및 기본 검증)
- **변경 필요**: ❌ 없음 (우선순위 영향 없음)
- **영향도**: 없음
- **상태**: 그대로 진행

### 3. 코드 유지보수성 개선

| 항목 | 개선 사항 |
|-----|---------|
| 코드 가독성 | 우선순위와 데이터 흐름 순서 일치 |
| 주석 명확성 | "데이터 흐름 순서" 명시 |
| 로깅 정확성 | 정확한 Priority 값 로깅 |
| 향후 확장성 | 신규 태스크 추가 시 우선순위 할당 규칙 명확 |

---

## ✅ 빌드 검증

```
Project build complete. To flash, run:
  idf.py flash

Build Statistics:
  - Binary size: 0x385c0 bytes
  - Free space: 0x2c7a40 bytes (93% free)
  - Largest app partition: 0x300000 bytes
```

✅ **빌드 성공**: 모든 변경사항 적용 후 정상 컴파일

---

## 📝 의사결정 근거

### 왜 옵션 2 (우선순위 조정)을 선택했는가?

1. **기술적 타당성**
   - FreeRTOS Priority 숫자가 높을수록 우선순위가 높음
   - 데이터 흐름(UART → HID → USB)과 우선순위 순서를 일치시키는 것이 설계 원칙
   - 코드 리뷰/유지보수 시 직관성 향상

2. **기능 안정성**
   - 큐 기반 IPC (Inter-Process Communication) 사용
   - 우선순위보다 큐 상태가 실행을 결정
   - 실제 버그나 성능 저하 위험 없음

3. **향후 확장성**
   - 신규 태스크 추가 시 우선순위 할당 규칙 명확
   - 데이터 흐름 기반 우선순위 할당 가능
   - 코드 일관성 유지

4. **비용-효과 분석**
   - 변경 비용: 낮음 (코드 6줄, 주석 수정)
   - 개선 효과: 높음 (유지보수성, 확장성)
   - 위험도: 매우 낮음 (기능 영향 없음)

### 왜 옵션 1 (현재 상태 유지)을 선택하지 않았는가?

- 코드 리뷰 시 불일치 지적 가능성 높음
- 향후 신규 태스크 추가 시 혼동 가능성
- 유지보수 비용 증가 위험

---

## 🔍 QA 체크리스트

- [x] 변경된 코드 빌드 성공
- [x] 우선순위 값 정확성 확인 (UART 6 > HID 5 > USB 4)
- [x] 주석 및 로깅 메시지 업데이트 완료
- [x] Phase 2.1.4.3 검증 항목 수정
- [x] Phase 2.1.5 검증 항목 추가
- [x] 변경사항 문서화 완료

---

## 📌 결론

✅ **Phase 2.1.4.2 우선순위 조정 완료**
- HID Priority: 7 → 5
- USB Priority: 5 → 4
- 데이터 흐름과 우선순위 순서 완벽 일치
- 빌드 성공, 기능 영향 없음
- 향후 유지보수성/확장성 개선
