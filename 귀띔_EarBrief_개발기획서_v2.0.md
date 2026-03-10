# 🎧 귀띔(EarBrief) 개발 기획서 v2.1

> **문서 버전**: v2.1 (통합 상세판 - 최신 기술 반영)
> **작성일**: 2026-03-09
> **기반 문서**: 귀띔_EarBrief_서비스기획서_v1.0
> **목적**: 서비스기획서를 기반으로 한 상세 개발 명세, 구현 가이드, 운영 전략 통합 문서

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [시스템 아키텍처 상세](#2-시스템-아키텍처-상세)
3. [모듈별 상세 설계](#3-모듈별-상세-설계)
4. [데이터베이스 스키마 설계](#4-데이터베이스-스키마-설계)
5. [API 명세 및 외부 서비스 연동](#5-api-명세-및-외부-서비스-연동)
6. [UI/UX 화면 설계](#6-uiux-화면-설계)
7. [보안 및 개인정보보호 아키텍처](#7-보안-및-개인정보보호-아키텍처)
8. [테스트 전략 및 품질 보증](#8-테스트-전략-및-품질-보증)
9. [CI/CD 및 배포 전략](#9-cicd-및-배포-전략)
10. [Phase별 상세 구현 계획](#10-phase별-상세-구현-계획)
11. [모니터링 및 운영 전략](#11-모니터링-및-운영-전략)
12. [비용 분석 및 수익 모델](#12-비용-분석-및-수익-모델)
13. [리스크 관리 및 대응 전략](#13-리스크-관리-및-대응-전략)
14. [팀 구성 및 역할 정의](#14-팀-구성-및-역할-정의)
15. [부록: KPI 추적 체크리스트](#15-부록-kpi-추적-체크리스트)
16. [접근성(Accessibility) 전략](#16-접근성accessibility-전략) ★ NEW
17. [국제화(i18n) 아키텍처](#17-국제화i18n-아키텍처) ★ NEW
18. [앱 크기 최적화 전략](#18-앱-크기-최적화-전략) ★ NEW
19. [위젯 & Quick Settings Tile](#19-위젯--quick-settings-tile) ★ NEW
20. [API Rate Limiting & 쿼터 관리](#20-api-rate-limiting--쿼터-관리) ★ NEW
21. [데이터 마이그레이션 전략](#21-데이터-마이그레이션-전략) ★ NEW
22. [에러 처리 통합 전략](#22-에러-처리-통합-전략) ★ NEW

---

## 1. 프로젝트 개요

### 1.1 프로젝트 정보

| 항목 | 내용 |
|------|------|
| **프로젝트명** | 귀띔 (EarBrief) |
| **서비스 한줄 정의** | 갤럭시 버즈 기반 상시 청취 → 맥락 분석 → 선제적 귓속말 AI 비서 |
| **프로젝트 유형** | Android Native Application (Kotlin) |
| **대상 플랫폼** | Android 12+ (API Level 31+) |
| **대상 디바이스** | Samsung Galaxy 스마트폰 + Galaxy Buds 시리즈 |
| **개발 언어** | Kotlin (Android), Python (Backend/ML), TypeScript (관리 대시보드) |
| **아키텍처 패턴** | Clean Architecture + MVVM (Android) / Microservice (Backend) |
| **예상 개발 기간** | 12개월 (MVP 2개월 → 베타 4개월 → 정식출시 8개월) |
| **목표 사용자** | 30~40대 비즈니스 프로페셔널 (영업, 컨설팅, 변호사, 의사 등) |

### 1.2 서비스 핵심 차별점

| 구분 | 기존 AI 비서 (Siri, Bixby) | 귀띔 (EarBrief) |
|------|---------------------------|----------------|
| **작동 방식** | 사용자 호출("헤이 빅스비") 후 작동 | 상시 인지, 선제적 개입 (호출 불필요) |
| **맥락 인식** | 매번 상황 재설명 필요 | 대화 흐름 자동 누적 및 동기화 |
| **개입 타이밍** | 명령 입력 시점 | 침묵 구간 또는 필요 시점 자동 포착 |
| **출력 방식** | 화면 + 스피커 (주변에 노출) | 이어폰 귓속말 (사적, 비밀스러운 전달) |
| **데이터 보안** | 클라우드 전송 중심 | 온디바이스 처리 우선 (클라우드 최소화) |
| **사용자 부담** | 설명 비용 높음 | 설명 비용 제로화 지향 |

### 1.3 핵심 파이프라인

```
┌────────┐    ┌─────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ 마이크  │───▶│ VAD │───▶│ STT     │───▶│ 화자분리  │───▶│ 맥락누적  │
│ (16kHz)│    │침묵제거│    │(스트리밍)│    │(나/상대방)│    │  엔진    │
└────────┘    └─────┘    └──────────┘    └──────────┘    └─────┬────┘
                                                               │
┌────────┐    ┌─────────┐    ┌──────────┐    ┌──────────┐     │
│ 버즈   │◀───│ TTS    │◀───│ 타이밍   │◀───│ LLM응답  │◀────┤
│ 출력   │    │ 귓속말  │    │ 엔진    │    │ 생성     │     │
└────────┘    └─────────┘    └──────────┘    └──────────┘     │
                                                               │
                                              ┌──────────┐    │
                                              │ 트리거   │◀───┘
                                              │ 감지엔진 │
                                              │ (7종)    │
                                              └──────────┘
```

**파이프라인 지연시간 목표:**

| 구간 | MVP | 베타 | 정식 출시 |
|------|-----|------|----------|
| 마이크 → STT 전사 완료 | < 800ms | < 500ms | < 300ms |
| STT → 트리거 감지 | < 200ms | < 150ms | < 100ms |
| 트리거 → LLM 귓속말 생성 | < 600ms | < 500ms | < 400ms |
| LLM → TTS 음성 생성 | < 400ms | < 350ms | < 200ms |
| **전체 E2E** | **< 2.0s** | **< 1.5s** | **< 1.0s** |

### 1.4 개발 원칙

| 원칙 | 설명 | 구체적 구현 |
|------|------|------------|
| **Privacy-First** | 음성 데이터 녹음 금지, 실시간 분석 후 즉시 삭제 | PCM 데이터 메모리 only, 디스크 저장 절대 금지. STT 후 zeroing |
| **Battery-Aware** | 2시간 기준 배터리 추가소모 10% 이내 | VAD 필터링(침묵 시 CPU 0%), 적응형 샘플링, WakeLock 최소화 |
| **Latency-Critical** | E2E 1.0초 이내 (정식 출시 기준) | 스트리밍 STT, 스트리밍 LLM, TTS 프리패치, 파이프라인 병렬화 |
| **Adaptive UX** | 과개입 방지, 사용자 맞춤 조절 | 10분 3회 버짓, 사용자 반응 기반 임계값 자동 학습 |
| **Offline-Capable** | Phase 3에서 완전 오프라인 지원 | Whisper 온디바이스 STT + Gemma/Llama 온디바이스 LLM |
| **Graceful Degradation** | 네트워크/API 장애 시에도 기본 기능 유지 | 클라우드 → 온디바이스 자동 전환, 규칙 기반 트리거는 항상 작동 |

---

## 2. 시스템 아키텍처 상세

### 2.1 전체 시스템 구성도

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         ANDROID APPLICATION                              │
│                                                                          │
│  ┌─────────────┐  ┌──────────┐  ┌──────────────┐  ┌─────────────────┐  │
│  │ Audio       │─▶│ VAD      │─▶│ STT Engine   │─▶│ Speaker         │  │
│  │ Capture     │  │ (Silero) │  │ (Deepgram /  │  │ Diarization     │  │
│  │ Service     │  │ 1MB ONNX │  │  Whisper)    │  │ (AssemblyAI)    │  │
│  │ [Foreground]│  │          │  │              │  │                 │  │
│  └─────────────┘  └──────────┘  └──────────────┘  └────────┬────────┘  │
│                                                              │          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                    CONTEXT ACCUMULATION ENGINE                    │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌────────────────────┐    │   │
│  │  │ Entity       │  │ Short-term   │  │ Long-term Memory   │    │   │
│  │  │ Extractor    │  │ Memory       │  │ (Vector DB)        │    │   │
│  │  │ (NER/패턴)    │  │ (10분 롤링)  │  │ (일별 압축 요약)    │    │   │
│  │  └──────────────┘  └──────────────┘  └────────────────────┘    │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                    │                                     │
│  ┌─────────────────────────────────▼───────────────────────────────┐    │
│  │                    TRIGGER DETECTION ENGINE                      │    │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ │    │
│  │  │일정 갭  │ │침묵 감지│ │위험 감지│ │모르는용어│ │키워드   │ │    │
│  │  │ T-001   │ │ T-002   │ │ T-003   │ │ T-004   │ │ T-006   │ │    │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘ │    │
│  │  ┌─────────┐ ┌─────────┐ ┌────────────────────────────────┐   │    │
│  │  │기억 보조│ │인물 식별│ │ Adaptive Intervention Control  │   │    │
│  │  │ T-005   │ │ T-007   │ │ (10분 3회 버짓 + 학습)         │   │    │
│  │  └─────────┘ └─────────┘ └────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                    │                                     │
│  ┌─────────────────────────────────▼───────────────────────────────┐    │
│  │                    WHISPER OUTPUT ENGINE                         │    │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐ │    │
│  │  │ LLM Response │  │ Timing       │  │ TTS Engine           │ │    │
│  │  │ Generator    │  │ Engine       │  │ (ElevenLabs/OpenAI)  │ │    │
│  │  │ (Claude      │  │ (침묵 800ms  │  │ → Galaxy Buds 출력   │ │    │
│  │  │  Haiku)      │  │  대기)       │  │                      │ │    │
│  │  └──────────────┘  └──────────────┘  └──────────────────────┘ │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                    GALAXY BUDS INTEGRATION                      │    │
│  │  BudsManager | GestureHandler | WearDetector | AudioRouter      │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
├──────────────────────────────────────────────────────────────────────────┤
│                         LOCAL STORAGE                                    │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐       │
│  │ Room/SQLite│  │ Dragonfly  │  │ DataStore  │  │ ONNX Models│       │
│  │ 트리거로그  │  │ 단기메모리 │  │ 사용자설정 │  │ VAD/Whisper│       │
│  │ 지식DB     │  │ (TTL 10분) │  │ 프로필     │  │ TFLite     │       │
│  └────────────┘  └────────────┘  └────────────┘  └────────────┘       │
├──────────────────────────────────────────────────────────────────────────┤
│                         CLOUD SERVICES                                   │
│  ┌──────────┐ ┌───────────┐ ┌────────┐ ┌────────┐ ┌──────────┐        │
│  │ Deepgram │ │AssemblyAI │ │ Claude │ │Eleven  │ │ Pinecone │        │
│  │ STT      │ │Diarize    │ │ Haiku  │ │Labs TTS│ │ VectorDB │        │
│  │ nova-3   │ │Speaker    │ │ 4.5    │ │v2      │ │ 1536dim  │        │
│  └──────────┘ └───────────┘ └────────┘ └────────┘ └──────────┘        │
│  ┌──────────┐ ┌───────────┐ ┌────────────────────────────────────┐    │
│  │ Google   │ │ OpenAI    │ │ Firebase (Auth/Analytics/Crash/    │    │
│  │ Calendar │ │ Embedding │ │  Performance/RemoteConfig/AppDist) │    │
│  └──────────┘ └───────────┘ └────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Android 앱 레이어 구조 (Clean Architecture)

```
app/
├── presentation/                    # UI Layer (MVVM + Jetpack Compose)
│   ├── ui/
│   │   ├── main/                    # 메인 화면 (청취 On/Off 대시보드)
│   │   │   ├── MainScreen.kt
│   │   │   ├── ListeningStatusCard.kt
│   │   │   ├── RecentWhispersCard.kt
│   │   │   └── QuickStatsCard.kt
│   │   ├── settings/                # 설정 화면
│   │   │   ├── SettingsScreen.kt
│   │   │   ├── TriggerSettingsScreen.kt
│   │   │   ├── PrivacySettingsScreen.kt
│   │   │   ├── AudioSettingsScreen.kt
│   │   │   └── SubscriptionScreen.kt
│   │   ├── triggerlog/              # 트리거 이벤트 로그
│   │   │   ├── TriggerLogScreen.kt
│   │   │   ├── TriggerDetailSheet.kt
│   │   │   └── TriggerFilterChips.kt
│   │   ├── profile/                 # 사용자 프로필 & 지식 관리
│   │   │   ├── ProfileScreen.kt
│   │   │   ├── KnowledgeListScreen.kt
│   │   │   ├── KeywordManageScreen.kt
│   │   │   └── VoiceEnrollScreen.kt
│   │   ├── onboarding/              # 초기 설정 & 권한 요청
│   │   │   ├── OnboardingScreen.kt
│   │   │   ├── PermissionRequestScreen.kt
│   │   │   ├── VoiceRegistrationScreen.kt
│   │   │   ├── BudsConnectionScreen.kt
│   │   │   └── CalendarSyncScreen.kt
│   │   └── common/                  # 공통 UI 컴포넌트
│   │       ├── EarBriefTopBar.kt
│   │       ├── StatusIndicator.kt
│   │       ├── WhisperBubble.kt
│   │       └── AnimatedWaveform.kt
│   ├── viewmodel/
│   │   ├── MainViewModel.kt
│   │   ├── SettingsViewModel.kt
│   │   ├── TriggerLogViewModel.kt
│   │   ├── ProfileViewModel.kt
│   │   └── OnboardingViewModel.kt
│   ├── navigation/
│   │   ├── EarBriefNavGraph.kt
│   │   └── Screen.kt
│   └── theme/
│       ├── Theme.kt
│       ├── Color.kt
│       ├── Typography.kt
│       └── Shape.kt
│
├── domain/                          # Business Logic Layer
│   ├── model/
│   │   ├── Utterance.kt             # 발화 단위 모델
│   │   ├── ConversationContext.kt   # 대화 맥락 모델
│   │   ├── TriggerEvent.kt          # 트리거 이벤트 모델
│   │   ├── WhisperResponse.kt       # 귓속말 응답 모델
│   │   ├── UserProfile.kt           # 사용자 프로필 모델
│   │   ├── Entity.kt                # NER 엔티티 모델
│   │   ├── TimeSlot.kt              # 캘린더 빈 시간 모델
│   │   ├── SpeakerLabel.kt          # 화자 라벨 enum
│   │   ├── TriggerType.kt           # 트리거 유형 enum
│   │   ├── InterventionBudget.kt    # 개입 예산 모델
│   │   └── SubscriptionPlan.kt      # 구독 플랜 모델
│   ├── usecase/
│   │   ├── listening/
│   │   │   ├── StartListeningUseCase.kt
│   │   │   ├── StopListeningUseCase.kt
│   │   │   └── PauseListeningUseCase.kt
│   │   ├── processing/
│   │   │   ├── ProcessUtteranceUseCase.kt
│   │   │   ├── ExtractEntitiesUseCase.kt
│   │   │   └── AccumulateContextUseCase.kt
│   │   ├── trigger/
│   │   │   ├── DetectTriggerUseCase.kt
│   │   │   ├── GenerateWhisperUseCase.kt
│   │   │   └── ManageInterventionBudgetUseCase.kt
│   │   ├── memory/
│   │   │   ├── SaveToLongTermMemoryUseCase.kt
│   │   │   ├── SearchMemoryUseCase.kt
│   │   │   └── DeleteAllDataUseCase.kt
│   │   └── settings/
│   │       ├── ManageKeywordsUseCase.kt
│   │       ├── UpdateKnowledgeProfileUseCase.kt
│   │       └── EnrollVoiceUseCase.kt
│   └── repository/
│       ├── AudioRepository.kt
│       ├── SttRepository.kt
│       ├── ContextRepository.kt
│       ├── TriggerRepository.kt
│       ├── CalendarRepository.kt
│       ├── WhisperRepository.kt
│       ├── MemoryRepository.kt
│       ├── UserProfileRepository.kt
│       └── SubscriptionRepository.kt
│
├── data/                            # Data Layer
│   ├── repository/                  # Repository 구현체 (위 인터페이스 각각)
│   ├── local/
│   │   ├── db/
│   │   │   ├── EarBriefDatabase.kt        # Room DB 정의
│   │   │   ├── dao/
│   │   │   │   ├── TriggerEventDao.kt
│   │   │   │   ├── UserKnowledgeDao.kt
│   │   │   │   ├── CustomKeywordDao.kt
│   │   │   │   ├── SessionDao.kt
│   │   │   │   └── InterventionFeedbackDao.kt
│   │   │   └── entity/
│   │   │       ├── TriggerEventEntity.kt
│   │   │       ├── UserKnowledgeEntity.kt
│   │   │       ├── CustomKeywordEntity.kt
│   │   │       ├── SessionEntity.kt
│   │   │       └── InterventionFeedbackEntity.kt
│   │   ├── datastore/
│   │   │   ├── UserPreferencesDataStore.kt
│   │   │   └── OnboardingDataStore.kt
│   │   └── memory/
│   │       ├── ShortTermMemoryStore.kt    # In-memory 10분 롤링
│   │       └── TtsCacheStore.kt           # TTS 오디오 캐시
│   ├── remote/
│   │   ├── deepgram/
│   │   │   ├── DeepgramWebSocketClient.kt
│   │   │   ├── DeepgramConfig.kt
│   │   │   └── model/DeepgramResult.kt
│   │   ├── assemblyai/
│   │   │   ├── AssemblyAiClient.kt
│   │   │   └── model/DiarizationResult.kt
│   │   ├── claude/
│   │   │   ├── ClaudeApiClient.kt
│   │   │   ├── ClaudeStreamHandler.kt
│   │   │   └── model/ClaudeResponse.kt
│   │   ├── elevenlabs/
│   │   │   ├── ElevenLabsApiClient.kt
│   │   │   └── model/TtsResponse.kt
│   │   ├── calendar/
│   │   │   ├── GoogleCalendarClient.kt
│   │   │   └── model/CalendarEvent.kt
│   │   ├── pinecone/
│   │   │   ├── PineconeClient.kt
│   │   │   └── model/VectorRecord.kt
│   │   └── openai/
│   │       ├── EmbeddingApiClient.kt
│   │       └── model/EmbeddingResult.kt
│   └── mapper/
│       ├── UtteranceMapper.kt
│       ├── TriggerEventMapper.kt
│       └── EntityMapper.kt
│
├── service/                         # Android Services
│   ├── AudioCaptureService.kt       # Foreground Service (마이크 상시 청취)
│   ├── PipelineOrchestrator.kt      # 전체 파이프라인 생명주기 관리
│   └── BudsConnectionService.kt     # Galaxy Buds 연결 상태 관리
│
├── engine/                          # Core AI/ML Engines
│   ├── vad/
│   │   ├── VadEngine.kt             # 인터페이스
│   │   ├── SileroVadEngine.kt       # ONNX 구현체
│   │   └── VadConfig.kt
│   ├── stt/
│   │   ├── SttEngine.kt             # 인터페이스
│   │   ├── DeepgramSttEngine.kt     # 클라우드 STT
│   │   ├── WhisperSttEngine.kt      # 온디바이스 STT (Phase 3)
│   │   ├── SttStreamHandler.kt      # WebSocket 스트림 관리
│   │   └── SttFallbackManager.kt    # 클라우드↔온디바이스 전환 관리
│   ├── diarization/
│   │   ├── DiarizationEngine.kt
│   │   ├── SpeakerProfile.kt
│   │   └── VoiceEmbeddingManager.kt # 음성 등록/매칭
│   ├── context/
│   │   ├── ContextAccumulationEngine.kt
│   │   ├── EntityExtractor.kt       # NER + 패턴 매칭
│   │   ├── DateTimeParser.kt        # 한국어 날짜/시간 파싱
│   │   ├── ShortTermMemory.kt       # 10분 롤링 윈도우
│   │   ├── LongTermMemory.kt        # 벡터 DB 연동
│   │   └── TopicTracker.kt          # 대화 주제 추적
│   ├── trigger/
│   │   ├── TriggerDetectionEngine.kt  # 트리거 오케스트레이터
│   │   ├── TriggerPriorityQueue.kt    # 우선순위 큐 + 충돌 해소
│   │   ├── rules/                     # 규칙 기반 트리거 7종
│   │   │   ├── BaseTriggerRule.kt     # 추상 기본 클래스
│   │   │   ├── ScheduleGapTrigger.kt
│   │   │   ├── SilenceDetectTrigger.kt
│   │   │   ├── RiskDetectTrigger.kt
│   │   │   ├── UnknownTermTrigger.kt
│   │   │   ├── MemoryAssistTrigger.kt
│   │   │   ├── KeywordInstantTrigger.kt
│   │   │   └── PersonIdentifyTrigger.kt
│   │   └── ml/                        # ML 기반 트리거 (Phase 3)
│   │       ├── TriggerClassifier.kt
│   │       └── TriggerModel.tflite
│   ├── response/
│   │   ├── WhisperResponseGenerator.kt
│   │   ├── PromptTemplateManager.kt   # 트리거별 프롬프트 관리
│   │   └── ResponseValidator.kt       # 응답 품질/길이 검증
│   ├── timing/
│   │   ├── TimingEngine.kt            # 출력 시점 결정
│   │   ├── SilenceWindowDetector.kt   # 침묵 구간 감지
│   │   └── UrgencyClassifier.kt      # 긴급도 분류
│   ├── tts/
│   │   ├── TtsEngine.kt              # 인터페이스
│   │   ├── ElevenLabsTtsEngine.kt    # 클라우드 TTS
│   │   ├── WhisperToneConfig.kt      # 귓속말 톤 설정
│   │   └── AudioCacheManager.kt      # TTS 캐시 관리
│   └── adaptive/
│       ├── AdaptiveInterventionController.kt
│       ├── InterventionBudgetManager.kt  # 10분 3회 버짓
│       ├── UserFeedbackTracker.kt        # 반응 추적
│       └── ThresholdLearner.kt           # 임계값 자동 학습
│
├── buds/                            # Galaxy Buds SDK 연동
│   ├── BudsManager.kt               # 연결/상태 관리
│   ├── GestureHandler.kt            # 터치 제스처 처리
│   │   # 탭1회: 반복 | 탭2회: 상세 | 길게탭: 정지
│   ├── WearDetector.kt              # In-ear 착용 감지
│   └── AudioRouter.kt               # A2DP/SCO 오디오 라우팅
│
├── notification/                    # 알림 관리
│   ├── ForegroundNotificationManager.kt  # 상시 알림
│   ├── WhisperNotificationManager.kt     # 귓속말 히스토리 알림
│   └── NotificationChannels.kt
│
├── analytics/                       # 분석 & 메트릭
│   ├── AnalyticsTracker.kt          # Firebase Analytics 이벤트
│   ├── PerformanceTracker.kt        # 파이프라인 지연시간 측정
│   └── MetricsCollector.kt          # 커스텀 품질 메트릭
│
└── di/                              # Dependency Injection (Hilt)
    ├── AppModule.kt
    ├── EngineModule.kt
    ├── NetworkModule.kt
    ├── DatabaseModule.kt
    ├── AnalyticsModule.kt
    └── BudsModule.kt
```

### 2.3 기술 스택 상세

| 카테고리 | 기술 | 버전/사양 | 용도 | 비고 |
|----------|------|-----------|------|------|
| **Language** | Kotlin | 2.1+ | Android 앱 전체 | K2 컴파일러 + Coroutines + Flow 적극 활용 |
| **Min SDK** | Android 12 | API 31 | Foreground Service 마이크 타입 필수 | targetSdk 35 (Android 15) |
| **Build** | Gradle (KTS) | 8.x | 빌드 시스템 | Version Catalog 사용 |
| **DI** | Hilt | 2.50+ | 의존성 주입 | @AndroidEntryPoint 전체 |
| **Async** | Coroutines + Flow | 1.9+ | 비동기 스트림 처리 | SharedFlow/StateFlow |
| **Networking** | Ktor Client | 3.0+ | HTTP/WebSocket 통신 (KMP 지원) | 향후 iOS 공유 가능 |
| **Networking** | OkHttp (Engine) | 5.0+ | Ktor HTTP 엔진 + WebSocket | 인터셉터로 API 키 주입 |
| **Serialization** | kotlinx.serialization | 1.7+ | JSON 파싱 | Kotlin-first, Gson 대비 경량 |
| **Local DB** | Room | 2.7+ | 트리거로그, 지식DB (KMP 지원) | TypeConverter 커스텀 |
| **Settings** | DataStore | 1.1+ | 사용자 설정 저장 | Proto DataStore |
| **ML Runtime** | ONNX Runtime Mobile | 1.19+ | Silero VAD, 온디바이스 Whisper | GPU delegate 지원 |
| **ML Runtime** | LiteRT (구 TFLite) | 최신 | 트리거 분류 ML 모델 (Phase 3) | NNAPI/GPU delegate |
| **UI** | Jetpack Compose | BOM 2026.01+ | 선언적 UI | Material3 1.4+, MotionScheme |
| **Navigation** | Navigation Compose | 2.9+ | 화면 전환 | Type-safe args |
| **Audio** | Android AudioRecord | API 31+ | PCM 16bit 16kHz 캡처 | VOICE_RECOGNITION source |
| **Buds SDK** | Samsung Accessory SDK | 최신 | Galaxy Buds 연동 | Privileged API 신청 필요 |
| **Auth** | Firebase Auth | 최신 | 사용자 인증 | Google Sign-In |
| **Analytics** | Firebase Analytics | 최신 | 사용자 행동 분석 | 커스텀 이벤트 |
| **Crash** | Firebase Crashlytics | 최신 | 크래시 리포트 | non-fatal 포함 |
| **Performance** | Firebase Performance | 최신 | 성능 모니터링 | 커스텀 trace |
| **RemoteConfig** | Firebase Remote Config | 최신 | Feature Flag | 실시간 토글 |
| **Distribution** | Firebase App Distribution | 최신 | 내부/베타 배포 | CI/CD 연동 |
| **Image** | Coil | 2.5+ | 프로필 이미지 로딩 | Compose 네이티브 |
| **Testing** | JUnit5 + MockK | 5.10+ / 1.13+ | 단위 테스트 | Turbine (Flow 테스트) |
| **Testing** | Espresso + Hilt Testing | AndroidX | 통합 테스트 | @HiltAndroidTest |
| **Testing** | UI Automator | AndroidX | E2E 테스트 | 크로스 앱 |

### 2.4 데이터 흐름 시퀀스 다이어그램

```
사용자 대화 발생 시 전체 데이터 흐름:

┌──────┐ ┌─────┐ ┌─────┐ ┌──────┐ ┌───────┐ ┌──────┐ ┌───────┐ ┌─────┐ ┌─────┐ ┌──────┐
│ Mic  │ │ VAD │ │ STT │ │Diari-│ │Context│ │Trigger│ │  LLM  │ │Timer│ │ TTS │ │ Buds │
│      │ │     │ │     │ │zation│ │Engine │ │Engine │ │       │ │     │ │     │ │      │
└──┬───┘ └──┬──┘ └──┬──┘ └──┬───┘ └───┬───┘ └───┬───┘ └───┬───┘ └──┬──┘ └──┬──┘ └──┬───┘
   │        │       │       │         │         │         │        │       │       │
   │─PCM──▶│       │       │         │         │         │        │       │       │
   │        │─prob─▶│       │         │         │         │        │       │       │
   │        │ ≥0.5  │       │         │         │         │        │       │       │
   │        │       │─text──────────▶│         │         │        │       │       │
   │        │       │       │─speaker─▶│         │         │        │       │       │
   │        │       │       │         │─entity──▶│         │        │       │       │
   │        │       │       │         │─context─▶│         │        │       │       │
   │        │       │       │         │         │─trigger──▶│        │       │       │
   │        │       │       │         │         │(if match)│        │       │       │
   │        │       │       │         │         │         │─text──▶│       │       │
   │        │       │       │         │         │         │(whisper)│       │       │
   │        │  silence     │         │         │         │        │─wait──▶       │
   │        │◀──0.8s──────────────────────────────────────────────│ 800ms │       │
   │        │       │       │         │         │         │        │       │─mp3──▶│
   │        │       │       │         │         │         │        │       │       │─play
```

---

## 3. 모듈별 상세 설계

### 3.1 모듈 M-001: 실시간 오디오 캡처 (AudioCaptureService)

| 항목 | 내용 |
|------|------|
| **모듈 ID** | M-001 |
| **대응 기능** | F-001 (실시간 앰비언트 청취) |
| **서비스 타입** | Android Foreground Service (`MICROPHONE`) |
| **오디오 포맷** | PCM 16-bit, 16kHz, Mono |
| **버퍼 사이즈** | 512 samples (32ms per frame) |
| **오디오 소스** | `MediaRecorder.AudioSource.VOICE_RECOGNITION` |
| **채널** | `AudioFormat.CHANNEL_IN_MONO` |

#### 3.1.1 처리 흐름

```
[서비스 시작 요청] ─── startForegroundService(intent) 호출
    │
    ▼
┌─ Foreground Notification 생성 ─────────────────────────┐
│  채널ID: "earbrief_listening"                           │
│  제목: "🎧 귀띔이 듣고 있어요"                            │
│  본문: "대화를 분석 중입니다. 탭하여 일시정지"                │
│  액션: [일시정지] [종료]                                  │
│  우선순위: FOREGROUND_SERVICE_IMMEDIATE                  │
└──────────────────────┬──────────────────────────────────┘
                       │
    ▼
┌─ BudsManager.checkConnection() ────────────────────────┐
│  Galaxy Buds 연결 상태 확인                               │
│  ├── 연결됨: BluetoothScoAudioRecorder 사용              │
│  │          (버즈 마이크 → 더 가까운 음성 캡처)             │
│  └── 미연결: 내장 마이크 AudioRecord 사용                  │
│              → 사용자에게 버즈 연결 권유 알림               │
└──────────────────────┬──────────────────────────────────┘
                       │
    ▼
┌─ AudioRecord 초기화 ──────────────────────────────────┐
│  val minBufSize = AudioRecord.getMinBufferSize(        │
│      16000, CHANNEL_IN_MONO, ENCODING_PCM_16BIT        │
│  )                                                     │
│  val audioRecord = AudioRecord(                        │
│      VOICE_RECOGNITION, 16000,                         │
│      CHANNEL_IN_MONO, ENCODING_PCM_16BIT,              │
│      max(minBufSize, 512 * 2)                          │
│  )                                                     │
└──────────────────────┬──────────────────────────────────┘
                       │
    ▼
[오디오 스트림 루프 - Coroutine (Dispatchers.IO)]
    │
    ├──▶ 32ms 단위 PCM 프레임 읽기 (512 samples)
    │       │
    │       ▼
    │    VadEngine.processFrame(frame)
    │       │
    │       ├── speech (prob ≥ 0.5)
    │       │     ├── SttEngine.sendAudioFrame(frame) ──▶ 클라우드 전송
    │       │     └── frame 메모리 즉시 해제 (zeroing)
    │       │
    │       └── silence (prob < 0.3)
    │             ├── 프레임 드롭 (API 호출 안 함 → 배터리/비용 절약)
    │             ├── SilenceWindowDetector에 침묵 시간 누적 통보
    │             └── frame 메모리 즉시 해제
    │
    ├──▶ WearDetector 이벤트 수신 (Flow)
    │       ├── IN_EAR_DETECTED → 자동 재개
    │       ├── OUT_OF_EAR_DETECTED → 자동 일시정지
    │       └── CONNECTION_LOST → 내장 마이크 전환
    │
    ├──▶ BatteryMonitor 이벤트 수신
    │       ├── battery ≤ 20% → 저전력 모드 (VAD 주기 64ms, 트리거 1종만)
    │       ├── battery ≤ 10% → 서비스 자동 종료 + 사용자 알림
    │       └── charging → 정상 모드 복귀
    │
    └──▶ UserCommand 수신
            ├── PAUSE → 오디오 루프 일시중지 (녹음 중단)
            ├── RESUME → 오디오 루프 재개
            └── STOP → 서비스 종료 + 리소스 정리
```

#### 3.1.2 권한 요구사항

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.READ_CALENDAR" />
<uses-permission android:name="android.permission.WRITE_CALENDAR" />
<uses-permission android:name="android.permission.READ_CONTACTS" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<service
    android:name=".service.AudioCaptureService"
    android:foregroundServiceType="microphone"
    android:exported="false" />
```

#### 3.1.3 권한 요청 플로우 (온보딩)

```
[앱 최초 실행]
    ▼
Step 1: 서비스 소개 → "귀띔은 소리를 녹음하지 않고, 실시간으로 분석만 합니다"
    ▼
Step 2: 마이크 권한 → RECORD_AUDIO (필수 - 거부 시 앱 사용 불가)
    ▼
Step 3: 알림 권한 → POST_NOTIFICATIONS (필수 - Foreground Service)
    ▼
Step 4: 캘린더 권한 → READ_CALENDAR (선택 - 일정 갭 트리거)
    ▼
Step 5: 연락처 권한 → READ_CONTACTS (선택 - 인물 식별 트리거)
    ▼
Step 6: Bluetooth 권한 → BLUETOOTH_CONNECT (선택 - 버즈 연동)
    ▼
Step 7: 배터리 최적화 예외 등록 → Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
    ▼
Step 8: 음성 등록 → 3~5문장 녹음 → Voice Embedding (선택 - Phase 2)
    ▼
Step 9: Google 캘린더 OAuth 연동 (선택)
    ▼
[온보딩 완료]
```

#### 3.1.4 배터리 최적화 전략 상세

| 전략 | 구현 방법 | 예상 절감 | 적용 Phase |
|------|-----------|-----------|-----------|
| VAD 침묵 필터링 | speech prob < 0.3 → API 호출 안 함 | CPU 60% 감소 | MVP |
| 적응형 VAD 주기 | 5초 연속 침묵 시 VAD 주기 32ms → 64ms | 추가 10% 절감 | MVP |
| WakeLock 최소화 | PARTIAL_WAKE_LOCK만, 화면 OFF 시 최소 연산 | 화면OFF 50% 절감 | MVP |
| 배터리 모니터링 | 20% 이하 저전력, 10% 이하 자동 종료 | 긴급 대비 | MVP |
| WebSocket 하트비트 | keep-alive 15초 → 30초 (배터리 모드) | 네트워크 20% 절감 | 베타 |
| 오디오 프레임 풀링 | ByteArray 풀링 재사용 → GC 압박 감소 | 메모리 30% 절감 | MVP |
| Doze 모드 대응 | 배터리 최적화 예외 + WorkManager 하이브리드 | 백그라운드 생존율 95%+ | MVP |

---

### 3.2 모듈 M-002: VAD (Voice Activity Detection)

| 항목 | 내용 |
|------|------|
| **모듈 ID** | M-002 |
| **모델** | Silero VAD v5 (ONNX Runtime) |
| **모델 크기** | ~1MB (ONNX 양자화) |
| **런타임** | ONNX Runtime Mobile (CPU, GPU delegate 지원) |
| **입력** | PCM 16-bit 16kHz, 512 samples/frame (32ms) |
| **출력** | Float (0.0 ~ 1.0, speech probability) |
| **임계값** | speech_threshold: 0.5 / silence_threshold: 0.3 |
| **처리 시간** | < 1ms per frame (CPU, Snapdragon 8 Gen 1 기준) |
| **메모리 사용** | ~ 5MB (모델 로드 + 추론 버퍼) |

#### 3.2.1 핵심 인터페이스 & 구현

```kotlin
interface VadEngine {
    /** VAD 모델 초기화 (assets에서 ONNX 로드) */
    suspend fun initialize(context: Context)

    /** 단일 오디오 프레임의 speech probability 반환 */
    fun processFrame(audioFrame: ShortArray): Float

    /** 현재 상태 반환 */
    fun getCurrentState(): VadState

    /** 연속 침묵 시간 (ms) — 타이밍 엔진에서 사용 */
    fun getSilenceDurationMs(): Long

    /** 연속 발화 시간 (ms) — 디버깅용 */
    fun getSpeechDurationMs(): Long

    /** 리소스 해제 */
    fun release()
}

enum class VadState {
    SPEECH,      // 발화 중 (prob ≥ 0.5 3연속)
    SILENCE,     // 침묵 (prob < 0.3 10연속)
    TRANSITION   // 전환 구간 (debounce 중)
}

data class VadConfig(
    val speechThreshold: Float = 0.5f,
    val silenceThreshold: Float = 0.3f,
    val speechMinFrames: Int = 3,         // 96ms (3 × 32ms)
    val silenceMinFrames: Int = 10,       // 320ms (10 × 32ms)
    val adaptiveSilenceMs: Long = 5000,   // 5초 연속 침묵 시 주기 확대
    val adaptiveFrameMs: Int = 64         // 적응형 프레임 크기
)
```

#### 3.2.2 상태 전환 다이어그램

```
                    ┌──────────────────────┐
                    │      SILENCE         │
                    │ (초기 상태)           │
                    └──────────┬───────────┘
                               │
                    prob ≥ 0.5 (1회)
                               │
                    ┌──────────▼───────────┐
                    │    TRANSITION         │
                    │  (speech 진입 대기)    │
                    │  카운터 시작           │
                    └──────────┬───────────┘
                               │
                   3연속 프레임 ≥ 0.5 (96ms 경과)
                               │
                    ┌──────────▼───────────┐
                    │      SPEECH          │◀─── prob ≥ 0.3 (유지)
                    │  → STT 전송 시작     │
                    └──────────┬───────────┘
                               │
                    prob < 0.3 (1회)
                               │
                    ┌──────────▼───────────┐
                    │    TRANSITION         │
                    │  (silence 진입 대기)   │
                    │  카운터 시작           │
                    └──────────┬───────────┘
                               │
                  10연속 프레임 < 0.3 (320ms 경과)
                               │
                    ┌──────────▼───────────┐
                    │      SILENCE         │
                    │  → STT 전송 중단     │
                    │  → 침묵 시간 카운팅   │
                    └──────────────────────┘
```

#### 3.2.3 노이즈 대응

| 환경 | 설명 | 대응 전략 |
|------|------|-----------|
| 키보드 타이핑 | 규칙적 클릭음 | speech MinFrames = 5 (160ms)로 상향 |
| 에어컨/선풍기 | 지속적 백색 소음 | 첫 5초 캘리브레이션 → 기준선 자동 설정 |
| 기침/재채기 | 짧은 임펄스 | debounce 320ms로 필터링 |
| 배경 음악 | 지속적 주파수 | spectral gating 전처리 (Phase 2) |
| 다수 동시 대화 | 파티/회의 | diarization과 조합 → 가장 가까운 음성만 |

---

### 3.3 모듈 M-003: STT 엔진 (Speech-to-Text)

| 항목 | 내용 |
|------|------|
| **모듈 ID** | M-003 |
| **대응 기능** | F-002 (실시간 STT) |
| **이중 구성** | 클라우드 (Deepgram nova-3) + 온디바이스 (Whisper v3 turbo) |
| **기본 모드** | 클라우드 STT (MVP~베타), 하이브리드 (정식 출시) |
| **스트리밍 방식** | WebSocket 기반 실시간 단어 단위 전사 |
| **목표 지연** | 200~300ms (단어 출력까지) |

#### 3.3.1 Deepgram 클라우드 STT 상세

**WebSocket 연결 설정:**

```kotlin
object DeepgramConfig {
    const val WS_BASE_URL = "wss://api.deepgram.com/v1/listen"

    val PARAMS = mapOf(
        "model" to "nova-3",           // 최신 고정밀 모델 (2025.02 출시)
        "language" to "ko",            // 한국어
        "smart_format" to "true",      // 자동 문장부호, 숫자 포맷
        "interim_results" to "true",   // 중간 결과 (UI 표시용)
        "utterance_end_ms" to "1500",  // 발화 종료 판정 (1.5초 침묵)
        "vad_events" to "true",        // VAD 이벤트 수신
        "encoding" to "linear16",      // PCM 16-bit
        "sample_rate" to "16000",      // 16kHz
        "channels" to "1",             // Mono
        "punctuate" to "true",         // 자동 구두점
        "diarize" to "false",          // 화자 분리는 AssemblyAI 사용
        "numerals" to "true",          // 숫자를 숫자 형태로 (삼십 → 30)
        "keyterm" to "true"            // ★ Keyterm Prompting 활성화
    )

    // Keyterm Prompting: 사용자 직업/분야 기반 전문용어 사전
    // nova-3의 핵심 신기능: 모델 재학습 없이 즉시 용어 적응
    val MEDICAL_KEYTERMS = listOf(
        "SGLT-2", "GLP-1", "HbA1c", "메트포르민", "아날필락시스",
        "단클론알 항체", "면역관문억제", "PD-L1", "NGS"
    )

    val LEGAL_KEYTERMS = listOf(
        "위약벌", "다툴주의", "선관주의", "무과실책임", "선의의무",
        "손해배상예정앱", "기판력", "항소심", "상고심"
    )

    const val HEARTBEAT_INTERVAL_MS = 15_000L  // 15초 keep-alive
    const val MAX_RECONNECT_ATTEMPTS = 5
    val RECONNECT_DELAYS = listOf(1000L, 2000L, 4000L, 8000L, 16000L)
}
```

**WebSocket 응답 모델:**

```kotlin
@Serializable
data class DeepgramResult(
    val type: String,                    // "Results"
    val channel: Channel,
    val metadata: Metadata,
    @SerialName("is_final") val isFinal: Boolean,
    @SerialName("speech_final") val speechFinal: Boolean
) {
    @Serializable
    data class Channel(val alternatives: List<Alternative>)

    @Serializable
    data class Alternative(
        val transcript: String,
        val confidence: Float,
        val words: List<Word>
    )

    @Serializable
    data class Word(
        val word: String,
        val start: Float,              // 시작 시간 (초)
        val end: Float,                // 종료 시간 (초)
        val confidence: Float,
        @SerialName("punctuated_word") val punctuatedWord: String
    )
}
```

**에러 핸들링 & Fallback 전략:**

```
[WebSocket 연결 시도]
    │
    ├── 200 Connected → 정상 스트리밍 시작
    │     │
    │     ├── 정상 메시지 수신 → ContextEngine으로 전달
    │     │
    │     ├── 1006 Abnormal Close → 자동 재연결 (지수 백오프)
    │     │     1s → 2s → 4s → 8s → 16s
    │     │     5회 실패 시:
    │     │     ├── Phase 3: 온디바이스 Whisper 전환
    │     │     └── Phase 1-2: 서비스 일시정지 + 사용자 알림
    │     │
    │     └── 1008 Policy Violation → API 키 만료 → Firebase Auth 재인증
    │
    ├── 401 Unauthorized → API 키 갱신 → RemoteConfig에서 백업 키 로드
    │
    ├── 429 Rate Limited → 30초 대기 후 재시도 + 프레임 드롭 비율 증가
    │
    └── Network Unreachable → 즉시 온디바이스 Whisper 전환 (Phase 3)
                                규칙 기반 트리거만 동작 (Phase 1-2)
```

#### 3.3.2 온디바이스 Whisper STT (Phase 3)

| 항목 | Whisper tiny | Whisper base |
|------|-------------|-------------|
| **파라미터** | 39M | 74M |
| **모델 크기 (ONNX INT8)** | ~40MB | ~80MB |
| **처리 방식** | 5초 청크 단위 배치 | 5초 청크 단위 배치 |
| **예상 지연** | ~800ms | ~1.5s |
| **한국어 WER** | ~20% (정확도 80%) | ~13% (정확도 87%) |
| **메모리 사용** | ~150MB | ~250MB |
| **CPU 사용** | ~35% (Snapdragon 8 Gen 2) | ~55% |
| **GPU Delegate** | 지원 (NNAPI) | 지원 (NNAPI) |
| **권장 사양** | RAM 6GB+ | RAM 8GB+ |

**온/오프라인 자동 전환 로직:**

```kotlin
class SttFallbackManager(
    private val cloudEngine: DeepgramSttEngine,
    private val localEngine: WhisperSttEngine,  // Phase 3
    private val networkMonitor: NetworkMonitor
) {
    private var currentMode: SttMode = SttMode.CLOUD

    // 자동 전환 판단
    fun evaluateMode(): SttMode {
        return when {
            !networkMonitor.isOnline() -> SttMode.ON_DEVICE
            networkMonitor.latency > 2000 -> SttMode.ON_DEVICE
            cloudEngine.consecutiveErrors >= 3 -> SttMode.ON_DEVICE
            currentMode == SttMode.ON_DEVICE
                && networkMonitor.isStable(30_000) -> SttMode.CLOUD
            else -> currentMode
        }
    }
}

enum class SttMode { CLOUD, ON_DEVICE, HYBRID }
```

---

### 3.4 모듈 M-004: 화자 분리 (Speaker Diarization)

| 항목 | 내용 |
|------|------|
| **모듈 ID** | M-004 |
| **대응 기능** | F-002 (화자 분리) |
| **Phase** | Phase 2 (베타)부터 적용 |
| **API** | AssemblyAI Real-time Transcription + Speaker Labels |
| **최대 화자 수** | 4명 |
| **라벨** | `SELF` / `SPEAKER_A` / `SPEAKER_B` / `SPEAKER_C` |
| **Voice Embedding** | 코사인 유사도 기반 (≥ 0.85 → SELF) |

#### 3.4.1 사용자 본인 식별 상세

```
[온보딩 - 음성 등록]
    │
    ▼
사용자에게 3~5문장 녹음 요청:
  1. "안녕하세요, 저는 귀띔 사용자입니다"
  2. "오늘 날씨가 정말 좋네요"
  3. "다음 주 회의 일정을 확인해주세요"
  4. "이 프로젝트의 예산은 얼마인가요?" (선택)
  5. "감사합니다, 좋은 하루 되세요" (선택)
    │
    ▼
각 문장에서 Voice Embedding 추출 (256-dim 벡터)
    │
    ▼
5개 임베딩의 평균 벡터 = 사용자 기준 임베딩
    │
    ▼
Android Keystore로 암호화 → EncryptedSharedPreferences 저장
    │
    ▼
[실시간 매칭]
AssemblyAI speaker label 수신
    │
    ├── speaker_0 Voice Embedding 추출
    │     cosine_similarity(speaker_0, user_embedding)
    │     ├── ≥ 0.85 → label = SELF
    │     └── < 0.85 → label = SPEAKER_A
    │
    ├── speaker_1 → 동일 비교 → SELF 또는 SPEAKER_B
    └── speaker_2 → 동일 비교 → SELF 또는 SPEAKER_C
```

#### 3.4.2 핵심 데이터 모델

```kotlin
data class Utterance(
    val id: String = UUID.randomUUID().toString(),
    val speaker: SpeakerLabel,
    val text: String,
    val startTimeMs: Long,           // 세션 시작 기준 offset (ms)
    val endTimeMs: Long,
    val confidence: Float,           // STT 신뢰도 (0.0 ~ 1.0)
    val language: String = "ko",
    val entities: List<Entity> = emptyList(),
    val isQuestion: Boolean = false,  // 질문형 문장 여부
    val sentiment: Sentiment = Sentiment.NEUTRAL  // 감정 분석 (Phase 3)
)

enum class SpeakerLabel { SELF, SPEAKER_A, SPEAKER_B, SPEAKER_C, UNKNOWN }

data class Entity(
    val type: EntityType,
    val value: String,               // 원본 텍스트 ("다음 주 화요일")
    val normalizedValue: String,     // 정규화 ("2026-03-10")
    val confidence: Float,
    val position: IntRange,          // 텍스트 내 위치
    val sourceUtteranceId: String
)

enum class EntityType {
    DATETIME,       // "다음 주 화요일 오후 3시" → ISO 8601
    PERSON,         // "김부장님" → 연락처 매칭 시도
    LOCATION,       // "강남역 근처" → 지역명
    MONEY,          // "위약금 30%" → 금액/비율
    PERCENTAGE,     // "30%" → 수치
    PROJECT,        // "알파 프로젝트" → 프로젝트명
    ORGANIZATION,   // "ABC컴퍼니" → 회사/조직명
    TODO,           // "내일까지 보고서 제출" → 할 일
    QUESTION,       // 질문형 문장 감지
    KEYWORD,        // 사용자 등록 키워드
    UNKNOWN_TERM,   // 지식 프로필 미등록 전문용어
    PRODUCT,        // 제품명
    PHONE_NUMBER,   // 전화번호
    EMAIL           // 이메일 주소
}
```

---

### 3.5 모듈 M-005: 맥락 누적 엔진 (Context Accumulation Engine)

| 항목 | 내용 |
|------|------|
| **모듈 ID** | M-005 |
| **대응 기능** | F-003 (맥락 누적) |
| **단기 메모리** | 최근 10분 전체 대화 (로컬 Dragonfly + SQLite) |
| **장기 메모리** | 일별 압축 요약 + 벡터 임베딩 (Pinecone) |
| **엔티티 추출** | 규칙 기반 NER + DateTimeParser + 패턴 매칭 |
| **업데이트 주기** | 실시간 (Utterance 수신 즉시) |

#### 3.5.1 엔티티 추출 상세 (EntityExtractor)

**한국어 날짜/시간 파싱 (DateTimeParser):**

```kotlin
class DateTimeParser {
    // 지원 패턴 (정규식 + 규칙)
    private val patterns = listOf(
        // 상대적 날짜
        "오늘" to { LocalDate.now() },
        "내일" to { LocalDate.now().plusDays(1) },
        "모레" to { LocalDate.now().plusDays(2) },
        "글피" to { LocalDate.now().plusDays(3) },
        "다음 주 (월|화|수|목|금|토|일)요일" to { matchDayOfWeek(it, 1) },
        "이번 주 (월|화|수|목|금|토|일)요일" to { matchDayOfWeek(it, 0) },
        "다다음 주 (월|화|수|목|금|토|일)요일" to { matchDayOfWeek(it, 2) },

        // 절대적 날짜
        "(\\d{1,2})월 (\\d{1,2})일" to { parseMonthDay(it) },
        "(\\d{4})년 (\\d{1,2})월 (\\d{1,2})일" to { parseFullDate(it) },

        // 시간
        "오전 (\\d{1,2})시" to { parseAmTime(it) },
        "오후 (\\d{1,2})시" to { parsePmTime(it) },
        "(\\d{1,2})시 (\\d{1,2})분" to { parseHourMin(it) },
        "점심" to { LocalTime.of(12, 0) },
        "저녁" to { LocalTime.of(18, 0) },

        // 상대적 시간
        "(\\d+)시간 후" to { LocalTime.now().plusHours(it.toLong()) },
        "(\\d+)분 후" to { LocalTime.now().plusMinutes(it.toLong()) }
    )
}
```

**질문형 문장 감지:**

```kotlin
object QuestionDetector {
    private val questionEndings = listOf(
        "세요?", "까요?", "나요?", "죠?", "어요?", "아요?",
        "할까요?", "될까요?", "있어요?", "없어요?",
        "어때요?", "어떠세요?", "괜찮으세요?",
        "맞아요?", "아닌가요?", "그런가요?",
        "해볼까요?", "진행할까요?", "시작할까요?"
    )

    private val questionPatterns = listOf(
        Regex("(시간|일정|스케줄).*(되|있|괜찮)"),
        Regex("(언제|어디|얼마|몇|누구|무엇|왜|어떻게)"),
        Regex("(생각|의견|제안).*(있|해주|말씀)")
    )

    fun isQuestion(text: String): Boolean {
        return questionEndings.any { text.trimEnd().endsWith(it) }
            || questionPatterns.any { it.containsMatchIn(text) }
    }
}
```

**위험 키워드 사전:**

```kotlin
object RiskKeywordDictionary {
    val CONTRACT_RISKS = mapOf(
        "위약금" to RiskLevel.HIGH,
        "해지 불가" to RiskLevel.HIGH,
        "해지불가" to RiskLevel.HIGH,
        "자동 갱신" to RiskLevel.MEDIUM,
        "자동갱신" to RiskLevel.MEDIUM,
        "배타적 계약" to RiskLevel.HIGH,
        "독점 계약" to RiskLevel.HIGH,
        "손해배상" to RiskLevel.HIGH,
        "연대보증" to RiskLevel.CRITICAL,
        "무한책임" to RiskLevel.CRITICAL,
        "경업금지" to RiskLevel.MEDIUM,
        "기밀유지" to RiskLevel.LOW
    )

    val FINANCIAL_RISKS = mapOf(
        "원금 손실" to RiskLevel.HIGH,
        "보장 안 됨" to RiskLevel.MEDIUM,
        "수수료 별도" to RiskLevel.MEDIUM,
        "추가 비용" to RiskLevel.MEDIUM,
        "숨은 비용" to RiskLevel.HIGH,
        "연체" to RiskLevel.HIGH,
        "이자율" to RiskLevel.MEDIUM
    )

    val NEGOTIATION_PRESSURES = mapOf(
        "최종 제안" to RiskLevel.MEDIUM,
        "마지막 기회" to RiskLevel.HIGH,
        "지금 아니면" to RiskLevel.HIGH,
        "다른 곳에서도" to RiskLevel.MEDIUM,
        "특별 할인" to RiskLevel.LOW,
        "오늘까지만" to RiskLevel.MEDIUM
    )

    enum class RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }
}
```

#### 3.5.2 단기 메모리 상세 (ShortTermMemory)

```kotlin
class ShortTermMemory(
    private val config: ShortTermMemoryConfig = ShortTermMemoryConfig()
) {
    // 10분 롤링 윈도우
    private val utteranceQueue = ArrayDeque<Utterance>()
    private val activeEntities = mutableMapOf<EntityType, MutableList<Entity>>()
    private val conversationTopics = mutableSetOf<String>()
    private val speakerSet = mutableSetOf<SpeakerLabel>()
    private var interventionBudget = config.maxInterventionsPerWindow // 3

    // 새 발화 추가
    fun addUtterance(utterance: Utterance) {
        utteranceQueue.addLast(utterance)
        speakerSet.add(utterance.speaker)

        // 엔티티 인덱싱
        utterance.entities.forEach { entity ->
            activeEntities.getOrPut(entity.type) { mutableListOf() }.add(entity)
        }

        // 만료 utterance 정리
        evictExpired()
    }

    // 10분 경과한 utterance 제거 (엔티티만 보존)
    private fun evictExpired() {
        val cutoff = System.currentTimeMillis() - config.windowDurationMs // 600,000ms
        while (utteranceQueue.isNotEmpty()) {
            val oldest = utteranceQueue.first()
            if (oldest.endTimeMs < cutoff) {
                utteranceQueue.removeFirst()
                // 엔티티는 activeEntities에 이미 있으므로 보존
                // 원문 텍스트만 삭제 → 프라이버시 보호
            } else break
        }
    }

    // 트리거 엔진용: 최근 N개 발화 조회
    fun getRecentUtterances(count: Int = 10): List<Utterance>

    // 트리거 엔진용: 특정 엔티티 타입의 활성 엔티티 조회
    fun getActiveEntities(type: EntityType): List<Entity>

    // 트리거 엔진용: 마지막 발화자 확인
    fun getLastSpeaker(): SpeakerLabel?

    // 트리거 엔진용: 마지막 SELF 발화 이후 침묵 시간
    fun getTimeSinceLastSelfUtterance(): Long

    // 적응형 개입: 남은 개입 횟수
    fun getRemainingBudget(): Int = interventionBudget
    fun consumeBudget() { interventionBudget-- }
    fun resetBudget() { interventionBudget = config.maxInterventionsPerWindow }
}

data class ShortTermMemoryConfig(
    val windowDurationMs: Long = 600_000,  // 10분
    val maxInterventionsPerWindow: Int = 3, // 10분당 3회
    val maxUtterances: Int = 500            // 최대 보관 발화 수
)
```

#### 3.5.3 장기 메모리 상세 (LongTermMemory)

```kotlin
class LongTermMemory(
    private val pineconeClient: PineconeClient,
    private val embeddingClient: EmbeddingApiClient,
    private val encryptionManager: EncryptionManager
) {
    /**
     * 세션 종료 시 장기 메모리 저장
     * 1. LLM으로 세션 요약 생성
     * 2. 요약 텍스트를 벡터 임베딩
     * 3. AES-256 암호화 후 Pinecone 저장
     */
    suspend fun saveSessionSummary(
        sessionId: String,
        utterances: List<Utterance>,
        entities: List<Entity>,
        userId: String,
        planTier: String
    ) {
        // 1. LLM으로 압축 요약 (Claude Haiku)
        val summary = claudeClient.summarize(utterances, maxTokens = 200)

        // 2. 벡터 임베딩 (OpenAI text-embedding-3-small)
        val embedding = embeddingClient.embed(summary)  // 1536-dim

        // 3. 만료일 계산
        val expiresAt = when (planTier) {
            "free" -> Instant.now().plus(7, ChronoUnit.DAYS)
            "pro" -> Instant.now().plus(30, ChronoUnit.DAYS)
            "enterprise" -> Instant.MAX  // 무제한
            else -> Instant.now().plus(7, ChronoUnit.DAYS)
        }

        // 4. 암호화 + 저장
        val encrypted = encryptionManager.encrypt(summary)
        pineconeClient.upsert(
            namespace = userId,  // 사용자별 격리
            id = "mem_${LocalDate.now()}_$sessionId",
            values = embedding,
            metadata = mapOf(
                "date" to LocalDate.now().toString(),
                "summary_encrypted" to encrypted,
                "entities" to entities.map { it.value }.joinToString(","),
                "trigger_count" to entities.count().toString(),
                "expires_at" to expiresAt.toString()
            )
        )
    }

    /**
     * 기억 보조 트리거용: 유사 과거 맥락 검색
     */
    suspend fun searchRelevantMemories(
        query: String,
        userId: String,
        topK: Int = 5
    ): List<MemoryRecord> {
        val queryEmbedding = embeddingClient.embed(query)
        val results = pineconeClient.query(
            namespace = userId,
            vector = queryEmbedding,
            topK = topK,
            filter = mapOf("expires_at" to mapOf("\$gt" to Instant.now().toString()))
        )
        return results.map { it.toMemoryRecord(encryptionManager) }
    }
}
```

---

### 3.6 모듈 M-006: 트리거 감지 엔진 (Trigger Detection Engine)

| 항목 | 내용 |
|------|------|
| **모듈 ID** | M-006 |
| **대응 기능** | F-004 |
| **트리거 총 7종** | 일정갭/침묵감지/위험감지/모르는용어/기억보조/키워드즉시/인물식별 |
| **MVP (Phase 1)** | 규칙 기반 3종: T-001, T-002, T-006 |
| **베타 (Phase 2)** | 규칙 기반 7종 전체 |
| **정식 (Phase 3)** | ML 기반 트리거 모델 추가 (하이브리드) |

#### 3.6.1 트리거별 상세 감지 로직

**T-001: 일정 갭 트리거 (Schedule Gap) ★★★★★**

```
감지 조건 (AND):
  ① 상대방(SPEAKER_A/B/C) 발화에서 DATETIME 엔티티 추출됨
  ② 해당 발화가 질문형 (QuestionDetector.isQuestion() == true)
  ③ 현재 interventionBudget > 0

처리 흐름:
  DATETIME 감지
    → DateTimeParser로 정규화 ("다음 주 화요일 오후" → 2026-03-10)
    → Google Calendar API FreeBusy 조회
    → 해당 날짜/시간대 빈 슬롯 리스트 생성
    → Claude Haiku로 귓속말 생성
      입력: "화요일 오후 빈 시간: 14:00~15:00, 16:00~17:00"
      출력: "화요일 오후 2시부터 3시, 4시부터 5시가 비어있어요. 2시를 제안해보세요."
    → TimingEngine 큐에 삽입 (priority: 5, urgency: NORMAL)

예외 처리:
  - 캘린더 권한 미부여 → 스킵, 트리거 발화 안 함
  - Calendar API 실패 → "일정을 확인하지 못했어요. 직접 확인해보세요" 폴백
  - 날짜 파싱 실패 → 스킵
```

**T-002: 침묵 감지 트리거 (Silence Detect) ★★★★☆**

```
감지 조건 (AND):
  ① 상대방 마지막 발화가 질문형이거나 응답 기대 패턴
  ② 사용자(SELF) 무응답 시간 ≥ silenceThresholdMs (기본 2,500ms)
  ③ 직전 상대방 발화에 의미 있는 내용 있음 (문장 길이 ≥ 5자)
  ④ 현재 interventionBudget > 0

처리 흐름:
  침묵 2.5초 감지
    → ShortTermMemory에서 직전 대화 맥락 5개 조회
    → Claude Haiku로 답변 힌트 생성
      입력: 대화 맥락 + "사용자가 답변에 어려움을 겪고 있습니다"
      출력: "지난번 유사 프로젝트는 5천만원이었어요. 범위를 고려하면 7천만원 선이 적당해요."
    → TimingEngine에 즉시 출력 (priority: 4, urgency: NORMAL_IMMEDIATE)

동적 임계값:
  - 사용자가 평소 대답이 빠른 편 → 2.0초
  - 사용자가 평소 생각하고 대답하는 편 → 3.5초
  - AdaptiveInterventionController가 자동 조절
```

**T-003: 위험 감지 트리거 (Risk Detect) ★★★★☆**

```
감지 조건 (OR):
  ① 상대방 발화에서 RiskKeywordDictionary 매칭 (HIGH/CRITICAL)
  ② 금액 엔티티 + 위험 키워드 동시 감지
  ③ 여러 위험 키워드 같은 문장에서 동시 등장 (복합 위험)

긴급도 분류:
  CRITICAL (연대보증, 무한책임)
    → 즉시 출력: 진동(200ms) + 경고음(300ms) + 귓속말
    → 발화 중이라도 개입
  HIGH (위약금, 해지불가)
    → 짧은 진동(100ms) + 다음 침묵에서 귓속말
  MEDIUM (자동갱신, 수수료별도)
    → 침묵 대기 → 일반 귓속말

출력 예시:
  "주의: 위약금 30%와 3개월 해지 불가 조항이 감지됐어요. 협상 여지를 확인해보세요."
```

**T-004: 모르는 용어 트리거 (Unknown Term) ★★★☆☆**

```
감지 조건 (AND):
  ① 대화에서 전문용어/약어/외래어 등장
  ② user_knowledge DB에서 known = false (사용자가 아직 모르는 용어)
  ③ 이번 세션에서 해당 용어 최초 등장 (중복 제거)
  ④ 용어 빈도: 일반 대화에서 사용 빈도 하위 5% (전문성 판단)

처리 흐름:
  미지 용어 감지
    → Claude Haiku로 쉬운 설명 생성 (1~2문장)
    → 침묵 구간에 귓속말
    → user_knowledge DB에 자동 등록 (known = true)
    → 이후 재등장 시 트리거하지 않음

용어 판별 방법:
  1. 사전 탑재 전문용어 사전 (의료/법률/IT/금융 각 10,000+ 단어)
  2. 사용자 직업/전문분야 기반 필터링 (IT 개발자 → IT 용어 스킵)
  3. n-gram 빈도 분석 (일상 코퍼스 대비 낮은 빈도 → 전문용어 의심)
```

**T-005: 기억 보조 트리거 (Memory Assist) ★★★☆☆**

```
감지 조건 (AND):
  ① 현재 대화에서 이전에 언급된 엔티티 재등장
  ② 재등장 간격 ≥ 5분
  ③ 재등장 엔티티가 PERSON, PROJECT, MONEY, LOCATION 중 하나
  ④ 이전 맥락에 유의미한 추가 정보가 있음

처리 흐름:
  엔티티 재등장 감지 ("김부장")
    → ShortTermMemory에서 이전 맥락 검색
    → 이전에 "김부장"이 언급된 utterance들 수집
    → Claude Haiku로 요약 귓속말 생성
    → "아까 김부장님이 이 건에 대해 3천만원 선이라고 하셨어요."
```

**T-006: 키워드 즉시 트리거 (Keyword Instant) ★★★★☆**

```
감지 조건:
  ① 사용자가 사전 등록한 키워드 매칭 (custom_keywords DB)
  ② 키워드 + 패턴 조합 매칭 (정규식 지원)

기본 제공 키워드 (사용자 선택 활성화):
  "떨어졌다" + [물건명] → 쇼핑 리스트 추가
  "예약" + [장소명] → 예약 정보 제공
  "비밀번호" → 즉시 청취 일시정지 (보안)
  "비밀" → 즉시 청취 일시정지 (보안)
  "오프더레코드" → 30초간 청취 일시정지

사용자 커스텀 예시:
  키워드: "견적" → 액션: 최근 견적 내역 읽어주기
  키워드: "할인율" → 액션: 사전 등록된 최대 할인율 알려주기
```

**T-007: 인물 식별 트리거 (Person Identify) ★★★☆☆**

```
감지 조건 (AND):
  ① 대화에서 PERSON 엔티티 추출 ("김철수", "이부장")
  ② Android Contacts에서 매칭 성공
  ③ 해당 인물이 이번 세션에서 최초 언급

출력:
  "김철수: ABC컴퍼니 이사. 마지막 통화 3월 1일. 메모: 알파프로젝트 담당."

연락처 매칭 전략:
  1. 정확 매칭: displayName == "김철수"
  2. 퍼지 매칭: 성+직함 ("김부장") → 성이 "김"인 연락처 중 직함 매칭
  3. 별명 매칭: nickname 필드 검색
  4. 최근 통화 가중치: 최근 30일 내 통화 이력 있는 연락처 우선
```

#### 3.6.2 트리거 우선순위 큐 & 충돌 해소

```kotlin
class TriggerPriorityQueue {
    private val queue = PriorityQueue<TriggerCandidate>(
        compareByDescending<TriggerCandidate> { it.priority }
            .thenBy { it.detectedAtMs }
    )

    /**
     * 충돌 해소 규칙:
     * 1. 우선순위 높은 트리거만 실행 (나머지 폐기)
     * 2. 동일 우선순위: 먼저 감지된 것 우선
     * 3. 10분 3회 버짓 초과 시 전부 폐기
     * 4. CRITICAL 긴급 트리거는 버짓 무시 (위험 감지)
     * 5. 동일 유형 트리거 30초 내 중복 방지
     */
    fun resolve(budget: Int): TriggerCandidate? {
        if (queue.isEmpty()) return null
        val candidate = queue.peek()

        // CRITICAL은 버짓 무시
        if (candidate.urgency == Urgency.CRITICAL) return queue.poll()

        // 버짓 체크
        if (budget <= 0) {
            queue.clear()
            return null
        }

        return queue.poll()
    }
}
```

---

### 3.7 모듈 M-007: 귓속말 출력 엔진 (Whisper Output Engine)

| 항목 | 내용 |
|------|------|
| **모듈 ID** | M-007 |
| **대응 기능** | F-005 |
| **TTS 엔진** | ElevenLabs eleven_multilingual_v2 (기본) / OpenAI TTS (백업) |
| **음성 길이** | 최대 15초 이내 (3~5문장 원칙) |
| **볼륨** | 시스템 미디어 볼륨의 40~60% |
| **톤** | 차분하고 친근한 귓속말 톤 (stability: 0.75, style: 0.15) |
| **출력 타이밍** | 침묵 ≥ 800ms 구간에서만 (긴급 제외) |

#### 3.7.1 타이밍 엔진 상세

```
[트리거 발화 → 타이밍 큐 삽입]
    │
    ▼
긴급도 분류 (UrgencyClassifier)
    │
    ├── CRITICAL (위험감지 CRITICAL)
    │   └── 즉시 출력
    │       ├── 진동 200ms (Vibrator API)
    │       ├── 경고음 300ms (custom alert.wav)
    │       └── 귓속말 TTS 재생
    │       ※ 발화 중이라도 개입 (볼륨 80%)
    │
    ├── HIGH (위험감지 HIGH)
    │   └── 짧은 진동 100ms → 침묵 대기 → 출력
    │       ├── 침묵 대기 최대 15초
    │       └── 15초 타임아웃 시 강제 출력 (볼륨 50%)
    │
    ├── NORMAL (일정갭, 침묵감지, 키워드 등)
    │   └── 침묵 ≥ 800ms 대기
    │       ├── 침묵 감지 → 귓속말 출력
    │       ├── 30초 타임아웃 → 귓속말 폐기 (유효기간 초과)
    │       └── 새 트리거 도착 → 큐에서 우선순위 재정렬
    │
    └── LOW (모르는용어, 기억보조, 인물식별)
        └── 침묵 ≥ 1,500ms 대기
            ├── 충분한 침묵 후 출력 (볼륨 40%)
            └── 45초 타임아웃 → 폐기

[출력 중 사용자 발화 시작]
    → 즉시 페이드아웃 (300ms) → 나머지 큐에 재삽입
```

#### 3.7.2 TTS 설정 & 캐싱

```kotlin
data class WhisperTtsConfig(
    val voiceId: String = "korean_whisper_01",    // 커스텀 한국어 음성
    val modelId: String = "eleven_multilingual_v2",
    val stability: Float = 0.75f,                  // 안정적인 톤
    val similarityBoost: Float = 0.80f,
    val style: Float = 0.15f,                      // 낮은 표현력 (차분)
    val speakingRate: Float = 1.1f,                // 약간 빠르게
    val outputFormat: String = "mp3_22050_32",     // 저용량 포맷
    val volumeMultiplier: Float = 0.5f             // 시스템 볼륨의 50%
)

// 캐시 전략
object TtsCacheConfig {
    const val CACHE_DIR = "tts_cache"
    const val MAX_CACHE_SIZE_MB = 50
    const val CACHE_TTL_HOURS = 24
    const val MAX_ENTRIES = 200

    // 동일 텍스트 해시 → 캐시 히트 시 API 호출 스킵
    fun cacheKey(text: String): String = text.trim().lowercase().md5()
}
```

---

### 3.8 모듈 M-008: 적응형 개입 조절 (Adaptive Intervention Control)

| 항목 | 내용 |
|------|------|
| **모듈 ID** | M-008 |
| **대응 기능** | F-006 |
| **핵심 로직** | 10분 기준 최대 3회 개입 버짓 |
| **학습 방식** | 사용자 반응 패턴 기반 임계값 자동 조절 |
| **데이터 저장** | intervention_feedback 테이블 |

#### 3.8.1 사용자 반응 분류 & 학습 상세

```kotlin
enum class UserReaction {
    ACTED,           // 귓속말 후 관련 행동 (캘린더 오픈 등) → 긍정 +2
    REPLAYED,        // 탭 1회로 다시 들음 → 긍정 +1
    DETAIL_REQUEST,  // 탭 2회로 상세 요청 → 매우 긍정 +3
    IGNORED,         // 반응 없음 (30초 내) → 부정 -1
    STOPPED,         // "그만해" 또는 길게 탭 → 매우 부정 -3
}

class ThresholdLearner(
    private val feedbackDao: InterventionFeedbackDao
) {
    // 기본 임계값 (트리거 타입별)
    private val baseThresholds = mapOf(
        TriggerType.SCHEDULE_GAP to 0.7f,
        TriggerType.SILENCE_DETECT to 0.6f,
        TriggerType.RISK_DETECT to 0.3f,    // 위험은 낮은 임계값 (적극 개입)
        TriggerType.UNKNOWN_TERM to 0.7f,
        TriggerType.MEMORY_ASSIST to 0.8f,
        TriggerType.KEYWORD_INSTANT to 0.5f,
        TriggerType.PERSON_IDENTIFY to 0.8f
    )

    /**
     * 반응 기반 임계값 조절
     * 임계값이 높을수록 → 트리거 발동 조건이 까다로워짐 (더 조용)
     * 임계값이 낮을수록 → 트리거 발동 조건이 쉬워짐 (더 적극적)
     */
    fun adjustThreshold(triggerType: TriggerType, reaction: UserReaction) {
        val current = getCurrentThreshold(triggerType)
        val adjusted = when (reaction) {
            UserReaction.ACTED -> current                    // 유지
            UserReaction.REPLAYED -> current                 // 유지
            UserReaction.DETAIL_REQUEST -> (current - 0.05f).coerceAtLeast(0.1f)  // 적극적
            UserReaction.IGNORED -> (current + 0.1f).coerceAtMost(0.95f)         // 조용
            UserReaction.STOPPED -> {
                // 30% 상향 + 5분 쿨다운
                setCooldown(triggerType, 5 * 60 * 1000L)
                (current + 0.3f).coerceAtMost(0.95f)
            }
        }
        saveThreshold(triggerType, adjusted)
        logFeedback(triggerType, reaction, current, adjusted)
    }
}
```

#### 3.8.2 버즈 제스처 UX

| 제스처 | 감지 방법 | 동작 | UserReaction |
|--------|-----------|------|-------------|
| **탭 1회** | Galaxy Buds SDK touchpad event | 방금 귓속말 반복 재생 | REPLAYED |
| **탭 2회** | 200ms 이내 연속 2회 터치 | 현재 맥락 상세 설명 요청 | DETAIL_REQUEST |
| **길게 탭 (1.5초)** | 터치 지속시간 ≥ 1500ms | 청취 일시정지 (5분) | STOPPED |
| **귀에서 꺼냄** | In-ear sensor OFF | 자동 일시정지 | - |
| **귀에 꽂음** | In-ear sensor ON | 자동 재개 | - |
| **"그만해" 음성** | STT에서 "그만" 키워드 감지 | 즉시 침묵 + 5분 쿨다운 | STOPPED |

---

## 4. 데이터베이스 스키마 설계

### 4.1 로컬 DB (Room / SQLite)

```sql
-- ================================================================
-- 트리거 이벤트 로그 (핵심 테이블)
-- ================================================================
CREATE TABLE trigger_events (
    id              TEXT PRIMARY KEY,           -- UUID
    trigger_type    TEXT NOT NULL,              -- SCHEDULE_GAP, SILENCE, RISK, ...
    timestamp       INTEGER NOT NULL,           -- Unix timestamp (ms)
    session_id      TEXT NOT NULL,              -- FK → sessions.id
    utterance_id    TEXT,                        -- 트리거 발생시킨 발화 ID
    speaker         TEXT,                        -- 트리거 발생시킨 화자
    detected_entities TEXT,                     -- JSON: 감지된 엔티티 리스트
    context_summary TEXT,                       -- 트리거 시점 맥락 요약 (3문장)
    whisper_text    TEXT,                        -- 생성된 귓속말 텍스트
    whisper_audio_path TEXT,                    -- TTS 캐시 파일 로컬 경로
    whisper_duration_ms INTEGER,                -- 귓속말 재생 시간 (ms)
    was_delivered   INTEGER DEFAULT 0,          -- 실제 출력 여부 (0/1)
    delivery_delay_ms INTEGER,                  -- 트리거→출력 지연시간
    user_reaction   TEXT,                        -- ACTED/REPLAYED/DETAIL/IGNORED/STOPPED
    reaction_timestamp INTEGER,                 -- 반응 시점
    priority        INTEGER DEFAULT 3,          -- 1(최고) ~ 5(최저)
    urgency         TEXT DEFAULT 'NORMAL',      -- CRITICAL/HIGH/NORMAL/LOW
    confidence      REAL DEFAULT 0.0,           -- 트리거 신뢰도 (0.0~1.0)
    pipeline_latency_ms INTEGER,                -- 전체 파이프라인 지연시간
    created_at      INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000)
);

CREATE INDEX idx_trigger_session ON trigger_events(session_id);
CREATE INDEX idx_trigger_type ON trigger_events(trigger_type);
CREATE INDEX idx_trigger_timestamp ON trigger_events(timestamp DESC);

-- ================================================================
-- 사용자 지식 프로필 DB
-- ================================================================
CREATE TABLE user_knowledge (
    id              TEXT PRIMARY KEY,
    term            TEXT NOT NULL UNIQUE,        -- 용어 (소문자 정규화)
    display_term    TEXT NOT NULL,               -- 표시용 원본 ("SGLT-2 억제제")
    category        TEXT,                        -- 카테고리 (의료/법률/IT/금융/일반)
    subcategory     TEXT,                        -- 세부 카테고리
    known           INTEGER DEFAULT 0,           -- 사용자가 아는 용어 (0/1)
    explanation     TEXT,                        -- 용어 설명 (LLM 생성, 캐시)
    source          TEXT,                        -- 설명 출처 (claude/manual)
    encounter_count INTEGER DEFAULT 1,           -- 등장 횟수
    first_seen_at   INTEGER NOT NULL,            -- 최초 등장 시점
    last_seen_at    INTEGER NOT NULL,            -- 최근 등장 시점
    created_at      INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000)
);

CREATE INDEX idx_knowledge_term ON user_knowledge(term);
CREATE INDEX idx_knowledge_known ON user_knowledge(known);
CREATE INDEX idx_knowledge_category ON user_knowledge(category);

-- ================================================================
-- 사용자 등록 키워드 (커스텀 트리거)
-- ================================================================
CREATE TABLE custom_keywords (
    id              TEXT PRIMARY KEY,
    keyword         TEXT NOT NULL,               -- 키워드 ("떨어졌다")
    pattern         TEXT,                        -- 정규식 패턴 (옵션)
    context_pattern TEXT,                        -- 앞뒤 문맥 패턴 (옵션)
    action_type     TEXT NOT NULL,               -- SHOPPING_LIST/BOOKMARK/PAUSE/NOTIFY/CUSTOM
    action_data     TEXT,                        -- 액션 파라미터 (JSON)
    response_template TEXT,                      -- 귓속말 응답 템플릿
    is_active       INTEGER DEFAULT 1,           -- 활성화 여부
    trigger_count   INTEGER DEFAULT 0,           -- 트리거된 횟수
    last_triggered  INTEGER,                     -- 마지막 트리거 시점
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER
);

-- ================================================================
-- 세션 로그
-- ================================================================
CREATE TABLE sessions (
    id              TEXT PRIMARY KEY,
    start_time      INTEGER NOT NULL,
    end_time        INTEGER,
    duration_ms     INTEGER,                     -- 총 세션 시간
    total_utterances INTEGER DEFAULT 0,
    self_utterances INTEGER DEFAULT 0,           -- 사용자 발화 수
    other_utterances INTEGER DEFAULT 0,          -- 상대방 발화 수
    total_triggers  INTEGER DEFAULT 0,
    delivered_triggers INTEGER DEFAULT 0,        -- 실제 전달된 트리거 수
    total_whispers  INTEGER DEFAULT 0,
    avg_latency_ms  INTEGER,                     -- 평균 파이프라인 지연시간
    p95_latency_ms  INTEGER,                     -- P95 지연시간
    stt_accuracy    REAL,                        -- STT 정확도 (수동 리뷰 시)
    trigger_precision REAL,                      -- 트리거 정탐률
    battery_start   INTEGER,                     -- 시작 시 배터리 (%)
    battery_end     INTEGER,                     -- 종료 시 배터리 (%)
    battery_consumed INTEGER,                    -- 소모된 배터리 (%)
    audio_source    TEXT,                         -- BUDS_MIC / PHONE_MIC
    stt_mode        TEXT,                         -- CLOUD / ON_DEVICE / HYBRID
    speakers_count  INTEGER DEFAULT 1,           -- 감지된 화자 수
    summary         TEXT,                         -- 세션 요약 (LLM 생성)
    created_at      INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000)
);

-- ================================================================
-- 개입 빈도 학습 데이터
-- ================================================================
CREATE TABLE intervention_feedback (
    id              TEXT PRIMARY KEY,
    trigger_type    TEXT NOT NULL,
    trigger_event_id TEXT,                       -- FK → trigger_events.id
    reaction        TEXT NOT NULL,               -- UserReaction enum
    threshold_before REAL NOT NULL,              -- 조절 전 임계값
    threshold_after  REAL NOT NULL,              -- 조절 후 임계값
    context_type    TEXT,                        -- 대화 상황 유형 (meeting/call/casual)
    timestamp       INTEGER NOT NULL,
    session_id      TEXT                         -- FK → sessions.id
);

CREATE INDEX idx_feedback_type ON intervention_feedback(trigger_type);
CREATE INDEX idx_feedback_timestamp ON intervention_feedback(timestamp DESC);

-- ================================================================
-- 사용자 프로필
-- ================================================================
CREATE TABLE user_profile (
    id              TEXT PRIMARY KEY,
    display_name    TEXT,
    occupation      TEXT,                        -- 직업
    expertise       TEXT,                        -- 전문 분야 (JSON array)
    language_primary TEXT DEFAULT 'ko',          -- 주 사용 언어
    language_secondary TEXT,                     -- 부 사용 언어
    voice_embedding BLOB,                        -- 암호화된 Voice Embedding
    plan_tier       TEXT DEFAULT 'free',         -- free/pro/enterprise
    plan_expires_at INTEGER,
    onboarding_completed INTEGER DEFAULT 0,
    total_sessions  INTEGER DEFAULT 0,
    total_listening_minutes INTEGER DEFAULT 0,
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER
);
```

### 4.2 인메모리 단기 메모리 (Dragonfly / Redis 호환)

```
Key 구조 & TTL:

stm:{sessionId}:utterances          → List<Utterance JSON>       TTL: 600s
stm:{sessionId}:entities:{type}     → List<Entity JSON>          TTL: 600s
stm:{sessionId}:topics              → SortedSet<topic, score>    TTL: 600s
stm:{sessionId}:speakers            → Set<SpeakerLabel>          TTL: 600s
stm:{sessionId}:trigger_budget      → Integer (남은 개입 횟수)    TTL: 600s
stm:{sessionId}:last_trigger        → Timestamp                  TTL: 600s
stm:{sessionId}:cooldown:{type}     → Timestamp (쿨다운 만료)     TTL: 300s
stm:{sessionId}:term_seen           → Set<String> (이미 설명한 용어) TTL: 3600s
stm:{sessionId}:person_seen         → Set<String> (이미 식별한 인물) TTL: 3600s

MVP에서는 Dragonfly 대신 Kotlin in-memory 구현:
  ConcurrentHashMap + ScheduledExecutorService로 TTL 관리
```

### 4.3 벡터 DB (Pinecone Serverless) - 장기 메모리

```json
{
  "index_config": {
    "name": "earbrief-memory",
    "dimension": 1536,
    "metric": "cosine",
    "spec": {
      "serverless": {
        "cloud": "aws",
        "region": "us-east-1"
      }
    }
  },
  "record_example": {
    "id": "mem_20260309_session_abc123",
    "values": [0.012, -0.034, 0.056, "... 1536 dimensions ..."],
    "metadata": {
      "user_id": "user_xxx",
      "date": "2026-03-09",
      "summary_encrypted": "AES256:base64encoded...",
      "entities": ["김부장", "알파프로젝트", "7000만원", "ABC컴퍼니"],
      "entity_types": ["PERSON", "PROJECT", "MONEY", "ORGANIZATION"],
      "trigger_types_used": ["SCHEDULE_GAP", "RISK_DETECT"],
      "trigger_count": 4,
      "session_duration_min": 45,
      "speakers_count": 2,
      "plan_tier": "pro",
      "language": "ko",
      "expires_at": "2026-04-08T00:00:00Z"
    }
  }
}
```

---

## 5. API 명세 및 외부 서비스 연동

### 5.1 외부 서비스 의존성 맵

| 서비스 | 용도 | Phase | 과금 모델 | 예상 월비용 (1K 사용자) | Fallback |
|--------|------|-------|-----------|-----------------------|----------|
| **Deepgram** | 실시간 STT | MVP~ | $0.0077/min (nova-3 mono) | ~$11,550 | Whisper v3 turbo 온디바이스 |
| **AssemblyAI** | 화자 분리 | 베타~ | $0.00025/sec | ~$2,250 | 화자분리 없이 전체 텍스트 |
| **Claude Haiku 4.5** | 귳속말 생성 | 베타~ | $1.00/$5.00 (input/output 1M tok) | ~$1,800 | 규칙 기반 템플릿 응답 |
| **ElevenLabs** | TTS 음성 | MVP~ | 크레딧 기반 ($0.30/1K chars) | ~$4,500 | Android 내장 TTS |
| **Google Calendar** | 빈 시간 조회 | MVP~ | 무료 (OAuth) | $0 | 캘린더 없이 "확인해보세요" |
| **OpenAI Embedding** | 벡터 임베딩 | 베타~ | $0.02/1M tok | ~$60 | 장기 메모리 비활성화 |
| **Pinecone** | 벡터 DB | 베타~ | $0.08/1M reads | ~$240 | 로컬 SQLite FTS |
| **Firebase** | Auth/Analytics 등 | MVP~ | Spark(무료)~Blaze | $0~$25 | - (필수) |

**총 예상 월 운영비: ~$20,425 / 1,000명 활성 사용자 = $20.4/user/month**
→ Pro 구독료 ₩29,900 (≈$22)으로 상향 조정 또는 초기 구독료 유지 + 온디바이스 전환 가속화
→ VAD 필터링(60%) + TTS 캐싱(30%) + 버짓 제한(40%) 적용 시 실질 ~$10,000
→ 온디바이스 전환(Phase 3)으로 장기 80% 절감 목표

### 5.2 Deepgram STT API 연동 상세

```kotlin
class DeepgramSttEngine(
    private val apiKey: String,
    private val config: DeepgramConfig,
    private val contextEngine: ContextAccumulationEngine
) : SttEngine {

    private var webSocket: WebSocket? = null
    private var reconnectAttempt = 0
    private val _transcriptFlow = MutableSharedFlow<TranscriptResult>()
    val transcriptFlow: SharedFlow<TranscriptResult> = _transcriptFlow

    fun connect() {
        val url = "${DeepgramConfig.WS_BASE_URL}?${config.PARAMS.toQueryString()}"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Token $apiKey")
            .build()

        webSocket = OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
            .newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    reconnectAttempt = 0
                    startHeartbeat(ws)
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    val result = Json.decodeFromString<DeepgramResult>(text)
                    if (result.isFinal && result.transcript.isNotBlank()) {
                        scope.launch {
                            _transcriptFlow.emit(
                                TranscriptResult(
                                    text = result.transcript,
                                    confidence = result.confidence,
                                    words = result.words,
                                    isFinal = true
                                )
                            )
                        }
                    }
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    handleReconnect()
                }
            })
    }

    fun sendAudioFrame(pcmData: ByteArray) {
        webSocket?.send(pcmData.toByteString())
    }

    private fun handleReconnect() {
        if (reconnectAttempt >= DeepgramConfig.MAX_RECONNECT_ATTEMPTS) {
            // Fallback to on-device (Phase 3) or pause
            fallbackManager.switchToOnDevice()
            return
        }
        val delay = DeepgramConfig.RECONNECT_DELAYS[reconnectAttempt]
        reconnectAttempt++
        scope.launch {
            delay(delay)
            connect()
        }
    }
}
```

### 5.3 Claude Haiku API 연동 (귓속말 생성)

```kotlin
class WhisperResponseGenerator(
    private val claudeClient: ClaudeApiClient,
    private val promptManager: PromptTemplateManager
) {
    suspend fun generate(
        triggerType: TriggerType,
        context: ConversationContext,
        additionalInfo: Map<String, String> = emptyMap()
    ): WhisperResponse {
        val prompt = promptManager.buildPrompt(
            triggerType = triggerType,
            conversationSummary = context.getRecentSummary(5),
            entities = context.getActiveEntities(),
            triggerReason = context.triggerReason,
            additionalContext = additionalInfo
        )

        val response = claudeClient.streamComplete(
            ClaudeRequest(
                model = "claude-haiku-4-5",
                maxTokens = 150,       // 짧은 응답 강제
                temperature = 0.3f,    // 일관성 우선
                stream = true,
                system = WHISPER_SYSTEM_PROMPT,
                messages = listOf(Message(role = "user", content = prompt))
            )
        )

        return WhisperResponse(
            text = response.content,
            triggerType = triggerType,
            generatedAtMs = System.currentTimeMillis(),
            estimatedDurationMs = estimateTtsDuration(response.content)
        )
    }
}

// 시스템 프롬프트
const val WHISPER_SYSTEM_PROMPT = """
당신은 '귀띔'이라는 AI 비서입니다. 사용자의 이어폰으로 귓속말 형태의 짧은 도움을 줍니다.

[절대 규칙]
1. 반드시 3~5문장 이내. 15초 안에 읽을 수 있는 분량만.
2. 친근하지만 정중한 "~해요" 체 사용.
3. 핵심 정보만 전달. 인사/서론 생략.
4. 숫자/날짜/금액은 구체적으로 언급.
5. 긴급 경고 시 "주의:" 접두어 필수.
6. 사용자가 대화 중이므로 자연스럽게 활용할 수 있는 형태로.
7. 추측이나 불확실한 정보는 절대 포함하지 않기.
"""
```

### 5.4 ElevenLabs TTS API 연동

```kotlin
class ElevenLabsTtsEngine(
    private val apiKey: String,
    private val cacheManager: AudioCacheManager
) : TtsEngine {

    override suspend fun synthesize(text: String): TtsResult {
        // 1. 캐시 체크
        val cacheKey = TtsCacheConfig.cacheKey(text)
        cacheManager.get(cacheKey)?.let { cached ->
            return TtsResult(audioPath = cached, fromCache = true)
        }

        // 2. API 호출
        val response = httpClient.post("https://api.elevenlabs.io/v1/text-to-speech/${WhisperTtsConfig.voiceId}") {
            header("xi-api-key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(ElevenLabsRequest(
                text = text,
                modelId = "eleven_multilingual_v2",
                voiceSettings = VoiceSettings(
                    stability = 0.75f,
                    similarityBoost = 0.80f,
                    style = 0.15f,
                    useSpeakerBoost = false
                ),
                outputFormat = "mp3_22050_32"
            ))
        }

        // 3. 캐시 저장 + 반환
        val audioPath = cacheManager.save(cacheKey, response.bodyAsChannel())
        return TtsResult(audioPath = audioPath, fromCache = false)
    }
}
```

### 5.5 Google Calendar API 연동

```kotlin
class GoogleCalendarClient(
    private val credential: GoogleAccountCredential
) {
    private val calendarService = Calendar.Builder(
        AndroidHttp.newCompatibleTransport(),
        GsonFactory.getDefaultInstance(),
        credential
    ).setApplicationName("EarBrief").build()

    suspend fun getAvailableSlots(
        date: LocalDate,
        startHour: Int = 9,
        endHour: Int = 18,
        slotDurationMin: Int = 30
    ): List<TimeSlot> = withContext(Dispatchers.IO) {
        val timeMin = DateTime(date.atTime(startHour, 0).toInstant(ZoneOffset.of("+09:00")).toEpochMilli())
        val timeMax = DateTime(date.atTime(endHour, 0).toInstant(ZoneOffset.of("+09:00")).toEpochMilli())

        // FreeBusy API로 바쁜 시간 조회
        val request = FreeBusyRequest().apply {
            this.timeMin = timeMin
            this.timeMax = timeMax
            items = listOf(FreeBusyRequestItem().apply { id = "primary" })
        }

        val busySlots = calendarService.freebusy().query(request).execute()
            .calendars["primary"]?.busy ?: emptyList()

        // 빈 시간 계산
        calculateFreeSlots(
            busySlots = busySlots.map {
                TimeRange(it.start.value, it.end.value)
            },
            dayStart = timeMin.value,
            dayEnd = timeMax.value,
            slotDurationMin = slotDurationMin
        )
    }

    suspend fun createEvent(
        title: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        description: String = ""
    ): String = withContext(Dispatchers.IO) {
        val event = Event().apply {
            summary = title
            this.description = description
            start = EventDateTime().setDateTime(
                DateTime(startTime.toInstant(ZoneOffset.of("+09:00")).toEpochMilli())
            )
            end = EventDateTime().setDateTime(
                DateTime(endTime.toInstant(ZoneOffset.of("+09:00")).toEpochMilli())
            )
        }
        calendarService.events().insert("primary", event).execute().id
    }
}
```

---

## 6. UI/UX 화면 설계

### 6.1 화면 구성 (Information Architecture)

```
[앱 시작]
    │
    ├── (최초) 온보딩 플로우
    │     ├── 서비스 소개 (3 스크린 스와이프)
    │     ├── 권한 요청 (마이크 → 알림 → 캘린더 → 연락처 → BT)
    │     ├── 배터리 최적화 예외 등록
    │     ├── 버즈 연결 (선택)
    │     ├── 음성 등록 (선택, Phase 2)
    │     ├── 캘린더 연동 (선택)
    │     ├── 직업/전문분야 설정 (선택)
    │     └── 완료 → 메인
    │
    └── 메인 화면 (홈)
          │
          ├── 🎧 청취 상태 카드
          │     ├── On/Off 토글 (큰 원형 버튼)
          │     ├── 실시간 파형 애니메이션
          │     ├── 현재 상태: "듣고 있어요" / "일시정지" / "꺼져 있어요"
          │     └── 연결된 디바이스: "Galaxy Buds3 Pro"
          │
          ├── 📊 오늘의 요약 카드
          │     ├── 청취 시간: 1h 23m
          │     ├── 귓속말 횟수: 5회
          │     ├── 트리거 분포 (도넛 차트)
          │     └── 유용성 평균: 4.2/5
          │
          ├── 💬 최근 귓속말 카드 (리스트)
          │     ├── 각 항목: 시간, 트리거 유형 아이콘, 귓속말 텍스트
          │     ├── 탭 → 상세 바텀시트
          │     └── 더보기 → 트리거 로그 화면
          │
          └── BottomNav
                ├── 🏠 홈
                ├── 📋 로그 (트리거 이벤트 전체)
                ├── 🧠 지식 (용어 사전/키워드 관리)
                ├── 👤 프로필
                └── ⚙️ 설정
```

### 6.2 핵심 화면별 상세

#### 메인 화면 (홈)

```
┌─────────────────────────────┐
│  🎧 귀띔                    │  ← 상단바
│                             │
│  ┌───────────────────────┐  │
│  │    ╭──────────────╮   │  │
│  │    │              │   │  │
│  │    │   🎧 듣는 중  │   │  │  ← 큰 원형 토글 (On: 파란 그라데이션)
│  │    │   ~~~~~~~~   │   │  │     (실시간 파형 애니메이션)
│  │    │              │   │  │
│  │    ╰──────────────╯   │  │
│  │                       │  │
│  │  Galaxy Buds3 Pro 연결됨 │  │  ← 연결 상태
│  │  배터리: 72%    1h 23m  │  │  ← 배터리 + 청취 시간
│  └───────────────────────┘  │
│                             │
│  오늘의 귓속말 ──────── 더보기│
│  ┌───────────────────────┐  │
│  │ 📅 14:32  일정 갭      │  │
│  │ "화요일 2~5시 비어있어요"│  │  ← 최근 귓속말 카드
│  │ 👍 유용했어요          │  │
│  ├───────────────────────┤  │
│  │ ⚠️ 15:10  위험 감지     │  │
│  │ "주의: 위약금 30% 감지" │  │
│  │ 👍 유용했어요          │  │
│  └───────────────────────┘  │
│                             │
│  🏠    📋    🧠    👤   ⚙️  │  ← BottomNav
└─────────────────────────────┘
```

#### 트리거 로그 화면

```
┌─────────────────────────────┐
│  ← 트리거 로그              │
│                             │
│  ┌─────────────────────┐   │
│  │ 필터: [전체▾] [오늘▾] │   │  ← 필터 칩
│  │  📅일정  💬침묵  ⚠️위험│   │
│  │  📖용어  🧠기억  🔑키워드│  │
│  └─────────────────────┘   │
│                             │
│  3월 9일 (일) ──────────── │
│  ┌─────────────────────┐   │
│  │ 15:10 ⚠️ 위험 감지     │   │
│  │ 감지: "위약금 30%"     │   │
│  │ 귓속말: "주의: 위약금..."│   │
│  │ 반응: 👍 유용했어요     │   │
│  │ 지연: 1.2초            │   │  ← 파이프라인 성능
│  ├─────────────────────┤   │
│  │ 14:32 📅 일정 갭       │   │
│  │ 감지: "화요일 오후"    │   │
│  │ 귓속말: "2~5시 비어..."│   │
│  │ 반응: 👍 유용했어요     │   │
│  └─────────────────────┘   │
│                             │
│  🏠    📋    🧠    👤   ⚙️  │
└─────────────────────────────┘
```

### 6.3 디자인 시스템

| 요소 | 스펙 |
|------|------|
| **Primary Color** | #4A90D9 (신뢰감 있는 블루) |
| **Accent Color** | #FF8C42 (따뜻한 오렌지, 경고/알림용) |
| **Background** | #0D1117 (다크 모드 기본) / #F8F9FA (라이트 모드) |
| **Font** | Pretendard (한국어) + Inter (영문/숫자) |
| **Corner Radius** | 16dp (카드), 24dp (버튼), 50% (원형 토글) |
| **Elevation** | 카드: 2dp, FAB: 6dp, 바텀시트: 16dp |
| **Animation** | 파형: Lottie, 전환: shared element transition |

---

## 7. 보안 및 개인정보보호 아키텍처

### 7.1 핵심 보안 원칙

> **"녹음하지 않는다. 분석하고 즉시 삭제한다."**

| 원칙 | 구현 방법 | 검증 방법 |
|------|-----------|-----------|
| **No Recording** | PCM 오디오 메모리 only, File I/O 전면 차단 | ProGuard로 FileOutputStream 사용 정적 분석 |
| **Immediate Deletion** | STT 전송 후 ByteArray.fill(0) 즉시 수행 | Unit test로 zeroing 검증 |
| **10분 TTL** | 단기 메모리 자동 만료 + 명시적 삭제 | Integration test + 로그 검증 |
| **암호화 전송** | TLS 1.3 필수, Certificate Pinning | OkHttp CertificatePinner 설정 |
| **암호화 저장** | AES-256-GCM, 키는 Android Keystore | EncryptedSharedPreferences 사용 |
| **사용자 제어권** | "모든 데이터 삭제" 즉시 실행 (30초 내 완료) | E2E 테스트로 완전 삭제 검증 |
| **최소 권한** | 각 API에 필요한 최소 scope만 요청 | 코드 리뷰 체크리스트 |

### 7.2 데이터 흐름별 보안 조치

```
[마이크 입력] ────── PCM ByteArray, 메모리 only
      │                 디스크 I/O 전면 차단 (lint rule 추가)
      │                 처리 후 ByteArray.fill(0) 즉시 수행
      ▼
[VAD 처리] ──────── ONNX 추론, 입출력 모두 메모리 내
      │                 모델 파일만 assets에 존재 (읽기 전용)
      ▼
[클라우드 STT] ──── TLS 1.3 + Certificate Pinning
      │                 WebSocket 전송 데이터: PCM 바이너리만
      │                 Deepgram DPA: 서버에서 30초 내 삭제
      │                 API 키: Android Keystore 암호화 저장
      ▼
[텍스트 데이터] ──── 10분 TTL (ShortTermMemory)
      │                 만료 시 String.toCharArray().fill('\u0000') 후 GC
      │                 장기 저장 시: AES-256-GCM 암호화
      │
      ├──▶ [장기 메모리] ── AES-256-GCM 암호화 → Pinecone 저장
      │                       사용자별 namespace 격리
      │                       키: Android Keystore + 사용자 PIN 파생
      │                       플랜별 만료일 자동 삭제 (cron job)
      │
      └──▶ [트리거 로그] ── 로컬 Room DB (SQLCipher 암호화)
                              귓속말 텍스트만 저장 (원본 대화 저장 안 함)
                              30일 자동 삭제 (Free), 90일 (Pro)
```

### 7.3 권한 & 동의 UX 상세

**온보딩 동의 화면:**

```
┌─────────────────────────────┐
│                             │
│  🎧 귀띔은 이렇게 작동해요  │
│                             │
│  ✅ 주변 소리를 실시간으로   │
│     분석하지만,             │
│  ❌ 녹음하거나 저장하지      │
│     않습니다.               │
│                             │
│  📋 상세 내용 보기 →        │  ← 개인정보 처리방침 링크
│                             │
│  ☑ 마이크 접근 동의 (필수)   │
│  ☑ 알림 표시 동의 (필수)     │
│  ☐ 캘린더 접근 동의 (선택)   │
│  ☐ 연락처 접근 동의 (선택)   │
│  ☐ 블루투스 접근 동의 (선택) │
│                             │
│  [동의하고 시작하기]         │
│                             │
│  📄 전체 개인정보 처리방침   │  ← 전문 링크
│  📄 이용약관               │
└─────────────────────────────┘
```

**설정 > 개인정보 화면:**

```
┌─────────────────────────────┐
│  ← 개인정보 관리            │
│                             │
│  📊 데이터 현황             │
│  ├── 트리거 로그: 142건     │
│  ├── 학습된 용어: 89개      │
│  ├── 장기 메모리: 23건      │
│  └── 총 저장 용량: 12.3MB   │
│                             │
│  🔧 관리                    │
│  ├── 트리거 로그 열람/삭제 → │
│  ├── 장기 메모리 열람/삭제 → │
│  ├── 학습 데이터 초기화    → │
│  ├── 음성 등록 삭제        → │
│  └── 데이터 내보내기 (JSON)→ │
│                             │
│  🗑️ 위험 구역              │
│  ┌───────────────────────┐  │
│  │  💣 모든 데이터 삭제   │  │  ← 빨간 버튼
│  │  되돌릴 수 없습니다    │  │
│  └───────────────────────┘  │
│                             │
└─────────────────────────────┘
```

### 7.4 법적 준수 사항

| 법률/정책 | 위험 요소 | 대응 조치 | 담당 |
|-----------|-----------|-----------|------|
| **개인정보보호법** | 음성 데이터 수집 | 녹음 아닌 실시간 분석+즉시삭제. 개인정보 처리방침 공개 | 법무 |
| **통신비밀보호법** | 제3자 대화 캡처 | 법무법인 검토. "녹음이 아닌 실시간 텍스트 변환" 법적 해석 확인 | 법무 |
| **GDPR (EU 진출 시)** | 데이터 이동권/삭제권 | JSON 내보내기 + 완전 삭제 기능. DPA 체결 | 법무 |
| **Google Play 정책** | 마이크 백그라운드 | Foreground Service `microphone` 타입. 알림 상시 표시. 사용 목적 명시 | Android |
| **Samsung Galaxy Store** | 추가 심사 | Samsung Accessory SDK 가이드라인 준수. Privileged API 신청 | Android |
| **아동보호법 (COPPA)** | 13세 미만 사용자 | 앱 설명에 "13세 이상" 명시. 연령 확인 절차 | 기획 |

---

## 8. 테스트 전략 및 품질 보증

### 8.1 테스트 레벨별 전략

| 레벨 | 커버리지 목표 | 도구 | 대상 | 실행 시점 |
|------|-------------|------|------|-----------|
| **Unit Test** | 80%+ | JUnit5 + MockK + Turbine | Engine, UseCase, Repository | 매 PR |
| **Integration Test** | 핵심 흐름 100% | Espresso + Hilt Testing | API 연동, DB CRUD, 파이프라인 | 매 PR |
| **E2E Test** | 시나리오 8종 | UI Automator + 테스트 오디오 | 전체 파이프라인 | Nightly |
| **Performance Test** | SLA 준수 | Android Profiler + 커스텀 Trace | 지연시간, 배터리, 메모리 | Weekly |
| **STT 벤치마크** | 정확도 SLA | 커스텀 테스트셋 700문장 | 5개 환경별 음성 인식 | Phase 전환 시 |
| **Security Audit** | OWASP Top 10 | MobSF + 수동 검토 | 데이터 보안, API 키 관리 | Monthly |
| **Accessibility** | WCAG 2.1 AA | Accessibility Scanner | UI 전체 | Phase 전환 시 |

### 8.2 핵심 테스트 시나리오 (8종)

| ID | 시나리오 | 검증 포인트 |
|----|---------|------------|
| TC-001 | 일정 갭 E2E | STT정확도 ≥ 90%, DATETIME 추출, Calendar 조회, 귓속말 출력, E2E < 2.0s |
| TC-002 | 위험 감지 | 키워드 감지, 긴급 모드, 진동+경고음+귓속말, 내용에 수치 포함 |
| TC-003 | 침묵 감지 | 2.5초 침묵 감지, 맥락 기반 힌트 생성, 자연스러운 귓속말 |
| TC-004 | 배터리 영향 | 2시간 청취, 배터리 < 20%, 메모리 < 200MB, CPU < 15% |
| TC-005 | 과개입 방지 | 10분 10회 조건 발생 → 실제 개입 ≤ 3회, STOPPED 시 즉시 침묵 |
| TC-006 | 네트워크 장애 | WiFi OFF → 온디바이스 전환(P3) / 규칙 트리거만(P1-2), 복구 시 자동 전환 |
| TC-007 | 버즈 탈착 | 귀에서 꺼냄 → 자동 정지, 꽂음 → 자동 재개, 제스처 동작 |
| TC-008 | 데이터 삭제 | "모든 데이터 삭제" → 30초 내 Room+Pinecone+Cache 전체 삭제 검증 |

### 8.3 STT 정확도 벤치마크 셋

| 환경 | 문장 수 | MVP 목표 | 베타 목표 | 정식 목표 | 테스트 방법 |
|------|--------|---------|---------|---------|-----------|
| 조용한 실내 (< 30dB) | 200문장 | ≥ 85% | ≥ 92% | ≥ 97% | 스튜디오 녹음 재생 |
| 카페 (50dB) | 200문장 | ≥ 75% | ≥ 85% | ≥ 92% | 실제 카페 앰비언트 합성 |
| 야외 바람/교통 (65dB) | 100문장 | ≥ 65% | ≥ 75% | ≥ 85% | 야외 환경 녹음 합성 |
| 다자간 대화 (3인) | 100문장 | ≥ 70% | ≥ 80% | ≥ 88% | 3인 대화 녹음 |
| 전문용어 (의료/법률) | 100문장 | ≥ 75% | ≥ 85% | ≥ 92% | 전문 용어 포함 문장 |

---

## 9. CI/CD 및 배포 전략

### 9.1 CI Pipeline (GitHub Actions)

```yaml
name: EarBrief CI
on: [push, pull_request]

jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: ktlint Check
        run: ./gradlew ktlintCheck
      - name: Android Lint
        run: ./gradlew lintDebug

  unit-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Unit Tests
        run: ./gradlew testDebugUnitTest
      - name: Coverage Report (Kover)
        run: ./gradlew koverReport
      - name: Enforce Coverage ≥ 80%
        run: ./scripts/check_coverage.sh 80

  integration-test:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v4
      - name: Android Emulator Tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 34
          script: ./gradlew connectedDebugAndroidTest

  security-scan:
    runs-on: ubuntu-latest
    steps:
      - name: Dependency Vulnerability Scan
        run: ./gradlew dependencyCheckAnalyze
      - name: API Key Leak Check
        run: ./scripts/check_api_keys.sh

  build:
    needs: [lint, unit-test, security-scan]
    runs-on: ubuntu-latest
    steps:
      - name: Build Release APK
        run: ./gradlew assembleRelease
      - name: Upload to Firebase App Distribution
        uses: wzieba/Firebase-Distribution-Github-Action@v1
        with:
          appId: ${{ secrets.FIREBASE_APP_ID }}
          serviceCredentialsFileContent: ${{ secrets.FIREBASE_CREDENTIALS }}
          groups: internal-testers
```

### 9.2 배포 전략

| 단계 | 배포 채널 | 대상 | 트리거 | 자동화 |
|------|----------|------|--------|--------|
| **Internal** | Firebase App Distribution | 개발팀 5명 | push to `dev` | 자동 |
| **Closed Beta** | Google Play 비공개 테스트 | 얼리어답터 100명 | merge to `beta` | 반자동 (승인 후) |
| **Open Beta** | Google Play 오픈 테스트 | 1,000명 | 수동 승인 | 수동 |
| **Production** | Google Play + Galaxy Store | 전체 | 관리자 2인 승인 | 수동 |
| **Rollout** | Staged rollout | 1% → 5% → 25% → 100% | 크래시율 모니터링 | 반자동 |

### 9.3 Feature Flag 시스템

```kotlin
object FeatureFlags {
    // Phase별 기능 제어 (Firebase Remote Config)
    val SPEAKER_DIARIZATION = RemoteConfig.getBoolean("ff_diarization")      // Phase 2
    val ML_TRIGGER = RemoteConfig.getBoolean("ff_ml_trigger")                // Phase 3
    val ONDEVICE_STT = RemoteConfig.getBoolean("ff_ondevice_stt")            // Phase 3
    val ONDEVICE_LLM = RemoteConfig.getBoolean("ff_ondevice_llm")            // Phase 3
    val LONG_TERM_MEMORY = RemoteConfig.getBoolean("ff_long_term_memory")    // Phase 2

    // 트리거별 On/Off (7종 각각 독립 제어)
    val TRIGGER_SCHEDULE_GAP = RemoteConfig.getBoolean("ff_t001_schedule")
    val TRIGGER_SILENCE = RemoteConfig.getBoolean("ff_t002_silence")
    val TRIGGER_RISK = RemoteConfig.getBoolean("ff_t003_risk")
    val TRIGGER_UNKNOWN_TERM = RemoteConfig.getBoolean("ff_t004_term")
    val TRIGGER_MEMORY = RemoteConfig.getBoolean("ff_t005_memory")
    val TRIGGER_KEYWORD = RemoteConfig.getBoolean("ff_t006_keyword")
    val TRIGGER_PERSON = RemoteConfig.getBoolean("ff_t007_person")

    // 실험 (A/B 테스트)
    val WHISPER_VOICE_VARIANT = RemoteConfig.getString("ab_voice")           // A/B TTS 음성
    val SILENCE_THRESHOLD_MS = RemoteConfig.getLong("ab_silence_ms")          // A/B 침묵 임계값
}
```

---

## 10. Phase별 상세 구현 계획

### 10.1 Phase 1: MVP (0~2개월, 8주)

**목표**: 마이크 → STT → 트리거 3종 → 귓속말 출력 전체 파이프라인 작동

#### Sprint 1-2 (Week 1~2): 프로젝트 설정 & 오디오 캡처

| Task ID | Task | 상세 | 담당 | 공수 | 의존성 |
|---------|------|------|------|------|--------|
| S1-01 | 프로젝트 초기화 | Kotlin + Compose + Hilt + Room 프로젝트 생성 | Android | 2d | - |
| S1-02 | CI 파이프라인 | GitHub Actions 기본 설정 (lint + test + build) | DevOps | 1d | S1-01 |
| S1-03 | Foreground Service | AudioCaptureService 기본 구현 + 알림 | Android | 3d | S1-01 |
| S1-04 | 오디오 캡처 | AudioRecord 16kHz PCM 스트림 구현 | Android | 2d | S1-03 |
| S1-05 | VAD 연동 | Silero VAD ONNX 로드 + 프레임 처리 | Android/ML | 3d | S1-04 |
| S1-06 | 권한 처리 | 마이크/알림 권한 요청 플로우 | Android | 1d | S1-03 |
| S1-07 | 단위 테스트 | VAD 정확도 검증 + AudioCapture 테스트 | QA | 2d | S1-05 |
| S1-08 | 배터리 측정 | 초기 배터리 소모 벤치마크 | QA | 1d | S1-05 |

#### Sprint 3-4 (Week 3~4): STT 연동

| Task ID | Task | 상세 | 담당 | 공수 |
|---------|------|------|------|------|
| S2-01 | Deepgram WebSocket | 연결/재연결/에러핸들링/지수백오프 | Android | 3d |
| S2-02 | STT 스트림 파이프라인 | VAD → PCM → WebSocket → 전사결과 수신 | Android | 3d |
| S2-03 | 전사 결과 파싱 | interim/final 결과 처리 + TranscriptResult 모델 | Android | 2d |
| S2-04 | 기본 UI | 메인 화면: 청취 On/Off 토글 + 전사 텍스트 실시간 표시 | Android | 2d |
| S2-05 | STT 벤치마크 | 5개 환경 정확도 측정 (700문장) | QA | 2d |

#### Sprint 5-6 (Week 5~6): 트리거 3종 & 캘린더

| Task ID | Task | 상세 | 담당 | 공수 |
|---------|------|------|------|------|
| S3-01 | EntityExtractor 기본 | DATETIME 파싱 + QUESTION 감지 | AI/NLP | 3d |
| S3-02 | ShortTermMemory | in-memory 10분 롤링 윈도우 + 엔티티 관리 | Android | 2d |
| S3-03 | Google Calendar 연동 | OAuth + FreeBusy API + 빈 시간 계산 | Android | 3d |
| S3-04 | T-001 일정 갭 트리거 | DATETIME + QUESTION → Calendar → 귓속말 생성 | Android | 3d |
| S3-05 | T-002 침묵 감지 트리거 | 2.5초 무응답 감지 → 힌트 생성 (템플릿 기반) | Android | 2d |
| S3-06 | T-006 키워드 트리거 | custom_keywords CRUD + 실시간 매칭 | Android | 2d |
| S3-07 | 트리거 로그 DB | Room 테이블 + DAO 구현 | Android | 1d |
| S3-08 | 트리거 테스트 | 정탐률/오탐률 측정 | QA | 2d |

#### Sprint 7-8 (Week 7~8): TTS & 통합

| Task ID | Task | 상세 | 담당 | 공수 |
|---------|------|------|------|------|
| S4-01 | ElevenLabs TTS 연동 | API 연동 + MP3 캐싱 + 재생 | Android | 3d |
| S4-02 | 타이밍 엔진 | SilenceWindowDetector + 출력 스케줄링 | Android | 3d |
| S4-03 | 버즈 오디오 출력 | Bluetooth A2DP 라우팅 + 볼륨 조절 | Android | 2d |
| S4-04 | 트리거 로그 UI | 로그 리스트 + 필터 + 상세 바텀시트 | Android | 2d |
| S4-05 | 온보딩 플로우 | 권한요청 + 서비스소개 + 배터리최적화 | Android | 2d |
| S4-06 | E2E 통합 테스트 | TC-001 ~ TC-005 시나리오 수행 | QA | 3d |
| S4-07 | 버그 수정 & 최적화 | 통합 테스트 이슈 해결 + 성능 튜닝 | All | 3d |

> **MVP 완성 기준 (Week 8 체크리스트):**
>
> - [ ] 마이크 → VAD → STT 파이프라인 정상 작동
> - [ ] 트리거 3종 (일정갭/침묵/키워드) 정상 감지
> - [ ] 귓속말 TTS 출력 (갤럭시 버즈 또는 이어폰)
> - [ ] 전체 E2E 지연시간 < 2.0초
> - [ ] 배터리 2시간 추가소모 < 20%
> - [ ] 크래시 없이 30분 연속 동작
> - [ ] 기본 UI (홈/로그) 동작

---

### 10.2 Phase 2: 베타 (2~4개월, 8주)

**목표**: 트리거 7종 완성, 화자 분리, 맥락 엔진 강화, 100명 클로즈드 베타

#### Sprint 9-10 (Week 9~12): 화자 분리 & 맥락 엔진 고도화

| Task | 상세 | 담당 | 공수 |
|------|------|------|------|
| AssemblyAI 화자 분리 연동 | 실시간 전사 + speaker label | Android | 4d |
| 음성 등록 온보딩 | VoiceEnrollScreen + 임베딩 생성/저장 | Android | 3d |
| Voice Embedding 매칭 | 코사인 유사도 기반 SELF 판별 | ML | 3d |
| EntityExtractor 고도화 | PERSON, LOCATION, MONEY, PROJECT, ORGANIZATION | AI/NLP | 5d |
| 벡터 DB 연동 | Pinecone 클라이언트 + 장기 메모리 저장/검색 | Backend | 4d |
| 세션 요약 생성 | Claude로 세션 종료 시 요약 자동 생성 | AI | 2d |

#### Sprint 11-12 (Week 13~16): 트리거 7종 완성

| Task | 상세 | 담당 | 공수 |
|------|------|------|------|
| T-003 위험 감지 | RiskKeywordDictionary + 긴급 모드 + 진동/경고음 | Android | 4d |
| T-004 모르는 용어 | 전문용어 사전 + 지식 프로필 DB + LLM 설명 | AI/Android | 4d |
| T-005 기억 보조 | 엔티티 재등장 감지 + 이전 맥락 검색 | Android | 3d |
| T-007 인물 식별 | Contacts 매칭 + 정보 카드 구성 | Android | 3d |
| 트리거 우선순위 큐 | TriggerPriorityQueue + 충돌 해소 로직 | Android | 2d |
| Claude Haiku 연동 | 고품질 귓속말 생성 (템플릿 → LLM 전환) | AI | 3d |

#### Sprint 13-14 (Week 17~20): 적응형 개입 & 버즈 SDK

| Task | 상세 | 담당 | 공수 |
|------|------|------|------|
| 적응형 개입 시스템 | InterventionBudget + ThresholdLearner | Android | 4d |
| 사용자 반응 추적 | UserFeedbackTracker + DB 저장 | Android | 2d |
| Galaxy Buds SDK 연동 | 착용 감지 + 터치 제스처 (탭1/탭2/길게탭) | Android | 5d |
| 설정 화면 | 트리거 On/Off, 키워드 관리, 프로필 편집 | Android | 3d |
| 프롬프트 최적화 | A/B 테스트 + 귓속말 품질 튜닝 | AI | 3d |

#### Sprint 15-16 (Week 21~24): 베타 준비 & 배포

| Task | 상세 | 담당 | 공수 |
|------|------|------|------|
| 한/영 혼용 지원 | Deepgram multi-language + 한영 전환 감지 | Android | 3d |
| 프로필/지식 화면 | KnowledgeListScreen, KeywordManageScreen | Android | 3d |
| Firebase Analytics | 핵심 이벤트 42종 정의 + 구현 | Android | 2d |
| Performance 모니터링 | 커스텀 Trace 13개 + 대시보드 | DevOps | 2d |
| 클로즈드 베타 배포 | Google Play 비공개 테스트 100명 | PM | 2d |
| 피드백 수집 시스템 | 인앱 피드백 + Typeform 연동 | Android | 2d |
| 전체 E2E 테스트 | TC-001 ~ TC-008 전체 수행 | QA | 3d |
| 버그 수정 | 베타 피드백 기반 이슈 해결 | All | 5d |

---

### 10.3 Phase 3: 정식 출시 (4~8개월, 16주)

| 기간 | 작업 | 상세 | 담당 | 공수 |
|------|------|------|------|------|
| Week 25-28 | 온디바이스 STT | Whisper tiny/base ONNX 탑재, 오프라인 모드 | ML/Android | 15d |
| Week 25-28 | SttFallbackManager | 온/오프라인 자동 전환 로직 | Android | 4d |
| Week 29-32 | ML 트리거 모델 | 수집 데이터 기반 TriggerClassifier 학습 (TFLite) | ML | 15d |
| Week 29-32 | 온디바이스 LLM | Gemma 2B / Llama 3.2 3B 탑재 (고사양 한정) | ML | 10d |
| Week 33-36 | B2B 엔터프라이즈 | 관리자 대시보드 (TypeScript), 팀 관리, SLA | Full-stack | 15d |
| Week 37-38 | 구독 시스템 | Google Play Billing + 플랜 관리 | Android | 5d |
| Week 37-38 | 다국어 확장 | 영어, 일본어 STT+TTS+프롬프트 | AI/Android | 8d |
| Week 39-40 | Galaxy Store 등록 | 심사 준비, Samsung SDK 적합성 테스트 | Android | 5d |
| Week 39-40 | Google Play 정식 출시 | ASO 최적화, 스크린샷, 설명문, Staged rollout | PM | 3d |

---

## 11. 모니터링 및 운영 전략

### 11.1 모니터링 스택

| 도구 | 용도 | 핵심 메트릭 |
|------|------|------------|
| **Firebase Crashlytics** | 크래시 리포트 | 크래시율, 영향 사용자 수, 스택트레이스 |
| **Firebase Analytics** | 사용자 행동 | DAU, 세션 시간, 트리거 사용률, 퍼널 |
| **Firebase Performance** | 앱 성능 | 파이프라인 지연시간, API 응답시간, 렌더링 |
| **Custom Metrics** | 서비스 품질 | STT 정확도, 트리거 정탐률, 유용성 점수, 배터리 |
| **Grafana + Prometheus** | 서버 모니터링 (Phase 3 B2B) | API 가용성, 벡터DB 응답시간, 비용 |

### 11.2 핵심 이벤트 정의 (Firebase Analytics)

```kotlin
object AnalyticsEvents {
    // 세션
    const val SESSION_START = "session_start"
    const val SESSION_END = "session_end"

    // 파이프라인
    const val STT_RESULT = "stt_result"                    // confidence 포함
    const val TRIGGER_DETECTED = "trigger_detected"        // type, priority
    const val WHISPER_GENERATED = "whisper_generated"      // type, length
    const val WHISPER_DELIVERED = "whisper_delivered"       // latency_ms
    const val WHISPER_SKIPPED = "whisper_skipped"           // reason

    // 사용자 반응
    const val USER_REACTION = "user_reaction"              // type, reaction
    const val GESTURE_TAP = "gesture_tap"                  // count
    const val GESTURE_LONG_TAP = "gesture_long_tap"
    const val VOICE_STOP_COMMAND = "voice_stop"

    // 시스템
    const val BUDS_CONNECTED = "buds_connected"
    const val BUDS_DISCONNECTED = "buds_disconnected"
    const val STT_FALLBACK = "stt_fallback"                // reason
    const val BATTERY_LOW_MODE = "battery_low_mode"
    const val DATA_DELETED = "data_deleted"                // scope
}
```

### 11.3 알림 규칙

| 조건 | 심각도 | 알림 채널 | 대응 SLA |
|------|--------|----------|---------|
| 크래시율 > 1% (24h) | 🔴 Critical | Slack + PagerDuty | 4시간 내 핫픽스 |
| 크래시율 > 0.5% (24h) | 🟠 High | Slack | 24시간 내 조사 |
| E2E 지연 P95 > 3초 (1h) | 🟠 High | Slack | 24시간 내 조사 |
| STT API 에러율 > 5% (1h) | 🟠 High | Slack | Deepgram 상태 확인 |
| TTS API 에러율 > 5% (1h) | 🟡 Medium | Slack | 내장 TTS 폴백 확인 |
| 일 API 비용 > 예산 150% | 🟡 Medium | Email | 주간 리뷰 |
| DAU 전일 대비 30% 하락 | 🟡 Medium | Email | 주간 리뷰 |
| 새 버전 크래시율 > 이전 버전 | 🔴 Critical | Slack | 롤백 검토 |

### 11.4 운영 비용 최적화 전략

| 전략 | 상세 | 예상 절감 | Phase |
|------|------|-----------|-------|
| **VAD 게이트** | 침묵 시 STT API 호출 안 함 (60% 시간 필터) | STT 비용 60% | MVP |
| **TTS 캐싱** | 동일 텍스트 로컬 캐시 (24h, 50MB) | TTS 비용 20% | MVP |
| **버짓 제한** | 10분 3회 → LLM/TTS 호출 자연 제한 | LLM+TTS 40% | MVP |
| **배치 임베딩** | 세션 종료 시 1회 벡터화 (실시간 X) | Embedding 90% | 베타 |
| **응답 템플릿** | 자주 쓰는 패턴 → LLM 호출 없이 템플릿 | LLM 30% | 베타 |
| **온디바이스 전환** | Whisper + Gemma/Llama 로컬 처리 | 장기 80% | 정식 |

---

## 12. 비용 분석 및 수익 모델

### 12.1 월 운영비 시뮬레이션

**가정**: 활성 사용자 1인 평균 일 60분 청취, 월 20일 사용

| 항목 | 100명 (MVP) | 1,000명 (베타) | 10,000명 (정식) |
|------|------------|--------------|----------------|
| Deepgram STT | $645 | $6,450 | $51,600 (온디바이스 50%) |
| AssemblyAI | - | $2,250 | $18,000 |
| Claude Haiku | - | $750 | $6,000 |
| ElevenLabs TTS | $270 | $2,700 | $16,200 (캐시 40%) |
| Pinecone | - | $240 | $2,400 |
| OpenAI Embedding | - | $60 | $600 |
| Firebase | $0 | $25 | $200 |
| 서버/인프라 | $50 | $200 | $2,000 |
| **총 월 비용** | **$965** | **$12,675** | **$97,000** |
| **인당 월 비용** | **$9.65** | **$12.68** | **$9.70** |

### 12.2 수익 모델

| 플랜 | 월 가격 | 대상 | 핵심 기능 | 예상 비중 |
|------|--------|------|-----------|----------|
| **Free** | ₩0 | 일반 | 월 60분 + 트리거 2종 (일정갭, 키워드) | 70% |
| **Pro** | ₩19,900 | 비즈니스 전문직 | 무제한 + 7종 트리거 + 맥락 30일 + 캘린더 | 25% |
| **Enterprise** | 협의 | 기업/의료/법률 | 온디바이스 100% + 대시보드 + SLA + 커스텀 | 5% |

### 12.3 손익분기점 분석

```
1,000명 기준:
  Free 700명: 수익 $0, 비용 $4,437 (제한된 기능으로 비용 50% 절감)
  Pro 250명: 수익 $3,725 (₩19,900 × 250), 비용 $7,922
  Enterprise 50명: 수익 TBD
  
  총 수익: $3,725 + α
  총 비용: $12,359
  손익분기: Pro 약 850명 필요 (전체 약 3,400명)
  
  온디바이스 전환 후 (Phase 3):
  인당 비용 $3~4로 감소 → Pro 350명으로 손익분기
```

---

## 13. 리스크 관리 및 대응 전략

| 리스크 | 발생확률 | 영향도 | 대응 전략 | 담당 |
|--------|---------|--------|-----------|------|
| **개인정보 거부감** | ★★★★★ | ★★★★★ | 온디바이스 처리 강조, 투명한 알림, 데이터 삭제 권한 보장, 법무법인 검토 | 기획/법무 |
| **Android 배터리** | ★★★★☆ | ★★★★☆ | 배터리 최적화 예외 안내, VAD 게이트, 저전력 모드, Samsung 파트너십 | Android |
| **STT 소음 오류** | ★★★☆☆ | ★★★☆☆ | 노이즈 전처리, 신뢰도 임계값, 오탐 피드백, 사용자 신고 기능 | AI/음성 |
| **과개입 문제** | ★★★★☆ | ★★★★☆ | 10분 3회 버짓, 사용자 피드백 학습, 쿨다운, 점진적 조율 | AI/UX |
| **API 비용 초과** | ★★★☆☆ | ★★★☆☆ | VAD 게이트, TTS 캐싱, 온디바이스 비율 확대, 버짓 제한 | 개발/기획 |
| **Galaxy SDK 변경** | ★★☆☆☆ | ★★★★☆ | 표준 Android API 추상화 레이어, 타 버즈 호환성 연구 | Android |
| **법적 이슈** | ★★★☆☆ | ★★★★★ | '실시간 분석 후 즉시 삭제' 아키텍처, 법무법인 사전 검토, 사용자 동의 강화 | 법무 |
| **경쟁사 진입** | ★★★☆☆ | ★★★☆☆ | 빠른 시장 진입, 특허 출원 (트리거 엔진), 사용자 경험 데이터 축적 | 기획 |

---

## 14. 팀 구성 및 역할 정의

### 14.1 최소 팀 구성 (MVP, 5명)

| 역할 | 인원 | 핵심 담당 |
|------|------|-----------|
| **Android Lead / Tech Lead** | 1명 | 아키텍처 설계, Clean Architecture, Foreground Service, 파이프라인 |
| **Android Developer** | 1명 | UI(Compose), 설정, 온보딩, 캘린더/연락처 연동 |
| **AI/ML Engineer** | 1명 | VAD, STT 연동, 엔티티 추출, 트리거 로직, LLM 프롬프트 |
| **PM / 기획** | 1명 | 제품 기획, UX 설계, 사용자 리서치, 베타 운영 |
| **QA Engineer** | 1명(파트타임) | 테스트 자동화, STT 벤치마크, 배터리 테스트 |

### 14.2 확장 팀 (Phase 2~3, 8~10명)

| 추가 역할 | 인원 | 시점 |
|-----------|------|------|
| **Backend Developer** | 1명 | Phase 2 (벡터 DB, 사용자 관리) |
| **ML Engineer (온디바이스)** | 1명 | Phase 3 (Whisper/Gemma 최적화) |
| **Full-stack Developer** | 1명 | Phase 3 (B2B 대시보드) |
| **DevOps** | 1명(파트타임) | Phase 2 (CI/CD, 모니터링) |
| **법무/컴플라이언스** | 외부 | 상시 (개인정보, 통신비밀보호법) |

---

## 15. 부록: KPI 추적 체크리스트

### 15.1 서비스 품질 KPI

| 지표 | MVP (M+2) | 베타 (M+4) | 정식 (M+8) | 측정 방법 |
|------|-----------|-----------|-----------|-----------|
| E2E 지연시간 | < 2.0s | < 1.5s | < 1.0s | Firebase Performance Trace |
| STT 정확도 (한국어) | > 85% | > 90% | > 95% | 커스텀 벤치마크 셋 |
| 트리거 정탐률 | > 70% | > 80% | > 90% | 수동 리뷰 + 사용자 피드백 |
| 트리거 오탐률 | < 20% | < 10% | < 5% | IGNORED+STOPPED 비율 계산 |
| 배터리 추가소모 | < 20%/2h | < 15%/2h | < 10%/2h | Battery Historian 분석 |
| 크래시율 | < 2% | < 1% | < 0.5% | Firebase Crashlytics |
| ANR 발생률 | < 1% | < 0.5% | < 0.1% | Firebase Performance |

### 15.2 비즈니스 KPI

| 지표 | 클로즈드 베타 | 오픈 베타 | 출시 6개월 | 측정 방법 |
|------|-------------|----------|-----------|-----------|
| 누적 사용자 | 100명 | 1,000명 | 10,000명 | Firebase Analytics |
| DAU | 50명 | 400명 | 5,000명 | Firebase Analytics |
| 일평균 사용시간 | > 30분 | > 45분 | > 60분 | 세션 시간 집계 |
| 귓속말 유용성 | > 3.5/5 | > 4.0/5 | > 4.3/5 | 인앱 평가 |
| D7 리텐션 | > 40% | > 50% | > 60% | Firebase Analytics |
| D30 리텐션 | > 20% | > 30% | > 40% | Firebase Analytics |
| Pro 전환율 | - | > 5% | > 15% | Play Billing 데이터 |
| 월간 구독 유지율 | - | > 60% | > 75% | Play Billing 데이터 |
| NPS | - | > 30 | > 50 | 분기별 설문 |

### 15.3 마일스톤 체크리스트

| 시기 | 단계 | 완성 목표 | 체크 |
|------|------|-----------|------|
| M+2 | MVP 완성 | 청취→STT→트리거3종→귓속말 파이프라인 | ☐ |
| M+3 | 내부 테스트 | 팀원 실사용, 지연시간 최적화, 버그 수정 | ☐ |
| M+4 | 클로즈드 베타 | 100명 테스트, 트리거 7종, 화자분리, 피드백 수집 | ☐ |
| M+6 | 오픈 베타 | 1,000명 테스트, 적응형 개입, 버즈 SDK 완전 연동 | ☐ |
| M+8 | 정식 출시 | Galaxy/Google 스토어 등록, 구독 모델 런칭, B2B 파일럿 | ☐ |
| M+12 | 고도화 | 온디바이스 ML, 다국어, 글로벌 진입 | ☐ |

---

---

## 16. 접근성(Accessibility) 전략

> **v2.1 신규 추가 섹션** — WCAG 2.1 AA 기준 준수

### 16.1 핵심 접근성 구현

| 영역 | 구현 방법 | 대상 사용자 |
|------|-----------|------------|
| **TalkBack 지원** | 모든 Compose UI에 `contentDescription` 명시, 커스텀 `semantics` | 시각 장애 |
| **글자 크기 대응** | `sp` 단위 사용 + 시스템 글꼴 크기 설정 존중 (200%까지 대응) | 저시력 |
| **색맹 대응** | Primary/Accent 외 아이콘+텍스트 라벨 병행 (색상만으로 정보 전달 금지) | 색각 이상 |
| **터치 영역** | 최소 48dp × 48dp (Compose `Modifier.minimumInteractiveComponentSize`) | 운동 장애 |
| **진동 대안** | 위험 감지 시 진동 + 시각적 경고 카드 동시 표시 | 진동 미인지 |
| **자막 표시** | 귓속말 음성을 화면에 텍스트로도 동시 표시 (설정에서 On/Off) | 청각 장애 |
| **고대비 모드** | Material3 Dynamic Color + 고대비 테마 제공 | 저시력 |

### 16.2 접근성 테스트 방법

```
1. TalkBack 활성화 → 전체 화면 탐색 시나리오 수행
2. Accessibility Scanner 앱으로 자동 점검
3. 시스템 글꼴 크기 최대 설정 → 레이아웃 깨짐 검증
4. 색상 시뮬레이션 (개발자 옵션) → 적녹색맹 모드에서 UX 검증
5. Switch Access 테스트 → 물리 스위치로 전체 기능 접근 가능 여부
```

---

## 17. 국제화(i18n) 아키텍처

> **v2.1 신규 추가 섹션** — Phase 3 다국어 확장 대비 기반 설계

### 17.1 기반 설계 원칙

| 원칙 | 구현 방법 | 비고 |
|------|-----------|------|
| **문자열 외부화** | 모든 사용자 대면 텍스트를 `strings.xml`로 분리 | MVP부터 적용 |
| **날짜/시간 포맷** | `DateTimeFormatter.ofLocalizedDate()` 사용 | 로케일 자동 대응 |
| **숫자/화폐 포맷** | `NumberFormat.getCurrencyInstance(locale)` | ₩ / $ / ¥ 자동 |
| **복수형 처리** | `plurals.xml` 사용 | 언어별 복수형 규칙 상이 |
| **RTL 레이아웃** | `start/end` 사용 (left/right 금지), 아랍어 등 대비 | Phase 3 |
| **LLM 프롬프트** | 언어별 System Prompt 분리 관리 (PromptTemplateManager) | 언어별 톤/뉘앙스 |
| **TTS 음성** | 언어별 ElevenLabs 음성 ID 매핑 테이블 | voice_id 분리 |
| **Keyterm 사전** | 언어별 전문용어 사전 분리 (Deepgram Keyterm Prompting) | 언어별 사전 |

### 17.2 지원 언어 로드맵

| Phase | 지원 언어 | STT | TTS | 프롬프트 | 전문용어 사전 |
|-------|-----------|-----|-----|---------|-------------|
| MVP~베타 | 한국어 🇰🇷 | ✅ | ✅ | ✅ | ✅ |
| Phase 3 초기 | + 영어 🇺🇸 | ✅ | ✅ | ✅ | ✅ |
| Phase 3 후기 | + 일본어 🇯🇵 | ✅ | ✅ | ✅ | 기본 |
| Phase 4 (글로벌) | + 중국어/스페인어/독일어 | ✅ | ✅ | 순차 | 순차 |

---

## 18. 앱 크기 최적화 전략

> **v2.1 신규 추가 섹션**

### 18.1 앱 크기 예산

| 구성 요소 | 예상 크기 | 최적화 전략 |
|-----------|-----------|------------|
| **기본 APK (코드+리소스)** | ~8MB | ProGuard/R8 최적화, 미사용 리소스 제거 |
| **Silero VAD ONNX 모델** | ~1MB | assets 포함 (필수, on-install) |
| **전문용어 사전 (기본)** | ~3MB | 분야별 분리, 사용자 선택 다운로드 |
| **Lottie 애니메이션** | ~0.5MB | 벡터 최적화 |
| **합계 (On-install)** | **~12.5MB** | Google Play 150MB 제한 대비 여유 |
| **Whisper 모델 (Phase 3)** | ~40~80MB | Dynamic Feature Module로 분리 |
| **온디바이스 LLM (Phase 3)** | ~1.5GB | On-demand 다운로드, 별도 저장 관리 |
| **TTS 캐시 (런타임)** | ~50MB (최대) | LRU 캐시, 24시간 TTL |

### 18.2 Dynamic Feature Module 활용 (Phase 3)

```kotlin
// On-demand 모델 다운로드 (Play Feature Delivery)
class ModelDownloadManager(private val splitInstallManager: SplitInstallManager) {

    suspend fun downloadWhisperModel(): Result<Unit> {
        val request = SplitInstallRequest.newBuilder()
            .addModule("whisper_stt_module")
            .build()

        return suspendCancellableCoroutine { continuation ->
            splitInstallManager.startInstall(request)
                .addOnSuccessListener { continuation.resume(Result.success(Unit)) }
                .addOnFailureListener { continuation.resume(Result.failure(it)) }
        }
    }

    fun isWhisperInstalled(): Boolean {
        return splitInstallManager.installedModules.contains("whisper_stt_module")
    }
}
```

---

## 19. 위젯 & Quick Settings Tile

> **v2.1 신규 추가 섹션** — 빠른 접근을 위한 시스템 통합

### 19.1 홈 화면 위젯 (Glance)

```
┌──────────────────────┐
│  🎧 귀띔              │
│  ────────────────────│
│  ● 듣는 중 (1h 23m)   │
│  최근: 📅 일정 갭      │
│  [일시정지]  [종료]    │
└──────────────────────┘
크기: 3×1 (최소), 4×2 (확장)
프레임워크: Jetpack Glance (Compose 기반 위젯)
업데이트 주기: 1분 (리스너 기반, 폴링 아님)
```

### 19.2 Quick Settings Tile

```kotlin
class EarBriefQSTile : TileService() {
    // 알림창 빠른 설정 타일 (Android 7+)
    // 탭: 청취 On/Off 토글
    // 롱탭: 앱 메인 화면 열기
    // 아이콘: 🎧 (청취 중) / 🎧⏸ (일시정지)

    override fun onClick() {
        val isListening = AudioCaptureService.isRunning
        if (isListening) {
            // 일시정지
            AudioCaptureService.pause()
            qsTile.state = Tile.STATE_INACTIVE
        } else {
            // 재개
            AudioCaptureService.resume()
            qsTile.state = Tile.STATE_ACTIVE
        }
        qsTile.updateTile()
    }
}
```

---

## 20. API Rate Limiting & 쿼터 관리

> **v2.1 신규 추가 섹션**

### 20.1 플랜별 API 쿼터

| 항목 | Free | Pro | Enterprise |
|------|------|-----|-----------|
| **일 청취 시간** | 60분 | 무제한 | 무제한 |
| **귓속말/일** | 15회 | 무제한 | 무제한 |
| **트리거 종류** | 2종 (일정갭, 키워드) | 7종 전체 | 7종 + 커스텀 |
| **장기 메모리** | 7일 보관 | 30일 보관 | 무제한 |
| **음성 등록** | ❌ | ✅ | ✅ + 팀원 등록 |
| **Keyterm Prompting** | 기본 세트만 | ✅ 커스텀 | ✅ + 산업별 |
| **데이터 내보내기** | ❌ | ✅ JSON | ✅ JSON + CSV |

### 20.2 Rate Limiter 구현

```kotlin
class ApiRateLimiter(
    private val subscriptionRepo: SubscriptionRepository,
    private val usageTracker: UsageTracker
) {
    // 쿼터 체크 → 초과 시 Graceful Degradation
    suspend fun checkQuota(apiType: ApiType): QuotaResult {
        val plan = subscriptionRepo.getCurrentPlan()
        val todayUsage = usageTracker.getTodayUsage(apiType)

        return when (apiType) {
            ApiType.STT -> {
                val limitMinutes = if (plan == PlanTier.FREE) 60 else Int.MAX_VALUE
                if (todayUsage.minutes >= limitMinutes) {
                    QuotaResult.Exceeded("오늘 청취 시간을 모두 사용했어요. Pro로 업그레이드하면 무제한이에요.")
                } else {
                    QuotaResult.Allowed(remaining = limitMinutes - todayUsage.minutes)
                }
            }
            ApiType.WHISPER_GEN -> {
                val limitCount = if (plan == PlanTier.FREE) 15 else Int.MAX_VALUE
                if (todayUsage.count >= limitCount) {
                    QuotaResult.Exceeded("오늘 귓속말 횟수를 모두 사용했어요.")
                } else {
                    QuotaResult.Allowed(remaining = limitCount - todayUsage.count)
                }
            }
            // ... 기타 API별 쿼터
        }
    }
}
```

---

## 21. 데이터 마이그레이션 전략

> **v2.1 신규 추가 섹션** — Room DB 스키마 변경 시 안전한 업데이트 보장

### 21.1 마이그레이션 원칙

| 원칙 | 설명 |
|------|------|
| **무손실 업데이트** | 사용자 데이터는 절대 삭제되지 않음 (새 컬럼 추가만, 삭제 금지) |
| **자동 실행** | 앱 업데이트 후 최초 실행 시 Room AutoMigration 또는 수동 마이그레이션 자동 적용 |
| **롤백 대비** | destructive fallback 사용 금지. 실패 시 원본 DB 유지 |
| **테스트 필수** | 각 마이그레이션 버전 별 Unit Test 필수 (MigrationTestHelper) |

### 21.2 Room 마이그레이션 구현

```kotlin
@Database(
    entities = [
        TriggerEventEntity::class,
        UserKnowledgeEntity::class,
        CustomKeywordEntity::class,
        SessionEntity::class,
        InterventionFeedbackEntity::class,
        UserProfileEntity::class
    ],
    version = 2,  // 스키마 버전 관리
    autoMigrations = [
        AutoMigration(from = 1, to = 2)  // 자동 마이그레이션 우선
    ]
)
abstract class EarBriefDatabase : RoomDatabase() {
    companion object {
        // 자동 마이그레이션 불가 시 수동 정의
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 예: sentiment 컬럼 추가
                db.execSQL("ALTER TABLE trigger_events ADD COLUMN sentiment TEXT DEFAULT 'NEUTRAL'")
                // 예: 새 인덱스 추가
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_trigger_confidence ON trigger_events(confidence)")
            }
        }
    }
}
```

### 21.3 벡터 DB 마이그레이션

```
Pinecone 벡터 DB 마이그레이션은 API 키 변경/리전 이동 시에만 필요.
마이그레이션 방법:
  1. 새 인덱스 생성 (new-earbrief-memory)
  2. 기존 인덱스에서 벡터 + 메타데이터 추출
  3. 새 인덱스로 배치 upsert
  4. Firebase Remote Config로 인덱스 이름 전환 (앱 업데이트 불필요)
  5. 1주일 후 기존 인덱스 삭제

임베딩 모델 변경 시 (OpenAI → Gemini 등):
  → 차원(dimension) 변경 불가피 → 전체 재임베딩 필요
  → 점진적 전환: 신규 세션은 새 모델, 기존 데이터는 조회 시 재임베딩
```

---

## 22. 에러 처리 통합 전략

> **v2.1 신규 추가 섹션**

### 22.1 에러 처리 아키텍처

```kotlin
// 전역 에러 핸들러 (CoroutineExceptionHandler)
val globalErrorHandler = CoroutineExceptionHandler { _, throwable ->
    when (throwable) {
        is ApiQuotaExceededException -> {
            // 쿼터 초과 → 사용자 알림 + Graceful Degradation
            notifyUserQuotaExceeded(throwable.apiType)
        }
        is NetworkException -> {
            // 네트워크 오류 → 온디바이스 모드 전환
            sttFallbackManager.switchToOnDevice()
            analyticsTracker.logNonFatal("network_error", throwable)
        }
        is SttConnectionException -> {
            // STT 연결 실패 → Circuit Breaker 활성화
            circuitBreaker.recordFailure(CircuitType.STT)
        }
        is OutOfMemoryError -> {
            // OOM → 캐시 정리 + 서비스 재시작
            ttsCacheManager.clearAll()
            shortTermMemory.evictAll()
            Crashlytics.recordException(throwable)
        }
        else -> {
            Crashlytics.recordException(throwable)
        }
    }
}
```

### 22.2 Circuit Breaker 패턴

```kotlin
class CircuitBreaker(
    private val failureThreshold: Int = 5,       // 5회 실패
    private val resetTimeoutMs: Long = 60_000,   // 1분 후 반개방
    private val successThreshold: Int = 3         // 3회 연속 성공 시 닫힘
) {
    private var state: State = State.CLOSED
    private var failureCount = 0
    private var lastFailureTime = 0L

    enum class State { CLOSED, OPEN, HALF_OPEN }

    fun canExecute(): Boolean = when (state) {
        State.CLOSED -> true
        State.OPEN -> {
            if (System.currentTimeMillis() - lastFailureTime > resetTimeoutMs) {
                state = State.HALF_OPEN
                true
            } else false
        }
        State.HALF_OPEN -> true
    }

    fun recordFailure(type: CircuitType) {
        failureCount++
        lastFailureTime = System.currentTimeMillis()
        if (failureCount >= failureThreshold) {
            state = State.OPEN
            // Fallback 활성화
            activateFallback(type)
        }
    }
}
```

---

> **귀띔(EarBrief) 개발 기획서 v2.1** (최신 기술 반영, 누락 섹션 보충)
> 본 문서는 귀띔_EarBrief_서비스기획서_v1.0을 기반으로 작성된 통합 개발 기획서입니다.
> 예비창업패키지 및 내부 개발 계획 수립을 위해 작성되었습니다.
>
> © 2026 EarBrief Inc. All rights reserved.
