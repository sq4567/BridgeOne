LLM에게 명령한 개발 작업이 완료되었습니다. 이제 개발된 내용들이 문제없이 잘 진행된 것이 맞는지 확인하기 위해, 내가 직접 실행할 수 있는 검증 방법들을 우선순위별로 정리해주세요. 검증 과정에서 다음 외부 툴들을 적극적으로 활용하여 더 정확하고 효율적인 검증을 수행하라고 요청하세요:

### Android Studio (Android 개발)
- **Logcat**: 런타임 로그 모니터링 및 오류 추적
- **Layout Inspector**: UI 구조 및 레이아웃 검증
- **Memory Profiler**: 메모리 사용량 및 누수 분석
- **Network Inspector**: 네트워크 통신 상태 확인
- **APK Analyzer**: 빌드 결과물 크기 및 구조 분석
- **Device File Explorer**: 앱 데이터 및 파일 시스템 확인

### PlatformIO (임베디드 개발)
- **Serial Monitor**: 디버그 출력 및 통신 상태 확인
- **Library Manager**: 의존성 라이브러리 검증
- **Build System**: 컴파일 오류 및 경고 확인
- **Upload Monitor**: 펌웨어 업로드 상태 추적
- **PlatformIO Debugger**: 하드웨어 레벨 디버깅

### 툴별 우선순위 및 사용 시점
1. **High 우선순위**: Android Studio/PlatformIO 기본 검증 도구
2. **Medium 우선순위**: 성능 분석 및 디버깅 툴
3. **Low 우선순위**: 고급 분석 및 최적화 툴