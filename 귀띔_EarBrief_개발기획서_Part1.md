# 🎧 귀띔(EarBrief) 개발 기획서 v1.0 — Part 1

> **문서 버전**: v1.0  
> **작성일**: 2026-03-06  
> **기반 문서**: 귀띔_EarBrief_서비스기획서_v1.0  
> **목적**: 서비스기획서를 기반으로 한 상세 개발 명세 및 구현 가이드

---

## 목차 (Part 1)

1. [프로젝트 개요](#1-프로젝트-개요)
2. [시스템 아키텍처 상세](#2-시스템-아키텍처-상세)
3. [모듈별 상세 설계](#3-모듈별-상세-설계)
4. [데이터베이스 스키마 설계](#4-데이터베이스-스키마-설계)

---

## 1. 프로젝트 개요

### 1.1 프로젝트 정보

| 항목 | 내용 |
|------|------|
| **프로젝트명** | 귀띔 (EarBrief) |
| **프로젝트 유형** | Android Native Application (Kotlin) |
| **대상 플랫폼** | Android 12+ (API Level 31+) |
| **대상 디바이스** | Samsung Galaxy 스마트폰 + Galaxy Buds 시리즈 |
| **개발 언어** | Kotlin (Android), Python (Backend/ML), TypeScript (관리 대시보드) |
| **아키텍처 패턴** | Clean Architecture + MVVM (Android) / Microservice (Backend) |
| **예상 개발 기간** | 12개월 (MVP 2개월 → 베타 4개월 → 정식출시 8개월) |

### 1.2 핵심 파이프라인 요약

```
마이크 입력 → VAD(침묵제거) → STT(스트리밍) → 화자분리 → 맥락누적엔진
    → 트리거감지 → LLM응답생성 → 타이밍엔진 → TTS → 갤럭시버즈 출력
```

### 1.3 개발 원칙

| 원칙 | 설명 |
|------|------|
| **Privacy-First** | 음성 데이터는 녹음되지 않으며 실시간 분석 후 즉시 삭제. 온디바이스 처리 우선 |
| **Battery-Aware** | VAD 기반 침묵 구간 필터링으로 불필요한 연산 최소화. 2시간 기준 배터리 추가소모 10% 이내 |
| **Latency-Critical** | 전체 파이프라인 end-to-end 지연시간 1.0초 이내 (정식 출시 기준) |
| **Adaptive UX** | 10분당 최대 3회 개입 버짓. 사용자 반응 학습 기반 자동 조절 |
| **Offline-Capable** | Phase 3에서 온디바이스 STT/LLM으로 오프라인 모드 지원 |

---

## 2. 시스템 아키텍처 상세

### 2.1 전체 시스템 구성도

```
┌─────────────────────────────────────────────────────────────────┐
│                      ANDROID APPLICATION                        │
│                                                                 │
│  ┌──────────┐   ┌──────────┐   ┌──────────────┐   ┌─────────┐ │
│  │ Audio    │──▶│  VAD     │──▶│  STT Engine  │──▶│ Context │ │
│  │ Capture  │   │ (Silero) │   │ (On/Cloud)   │   │ Engine  │ │
│  │ Service  │   │          │   │              │   │         │ │
│  └──────────┘   └──────────┘   └──────┬───────┘   └────┬────┘ │
│                                       │                 │      │
│                              ┌────────▼─────────┐      │      │
│                              │ Speaker          │      │      │
│                              │ Diarization      │──────┘      │
│                              └──────────────────┘             │
│                                                                │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐      │
│  │ Trigger      │◀──│ Context      │   │ Adaptive     │      │
│  │ Detection    │   │ Accumulation │   │ Intervention │      │
│  │ Engine       │──▶│ Engine       │──▶│ Control      │      │
│  └──────┬───────┘   └──────────────┘   └──────┬───────┘      │
│         │                                      │               │
│         ▼                                      ▼               │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐      │
│  │ LLM Response │──▶│ Timing       │──▶│ TTS Output   │      │
│  │ Generator    │   │ Engine       │   │ (Buds)       │      │
│  └──────────────┘   └──────────────┘   └──────────────┘      │
│                                                                │
├─────────────────────────────────────────────────────────────────┤
│                      LOCAL STORAGE                              │
│  ┌──────────┐   ┌──────────┐   ┌──────────────┐              │
│  │ SQLite   │   │ Dragonfly│   │ SharedPrefs  │              │
│  │ (메타DB) │   │ (단기메모리)│   │ (사용자설정) │              │
│  └──────────┘   └──────────┘   └──────────────┘              │
├─────────────────────────────────────────────────────────────────┤
│                      CLOUD SERVICES                             │
│  ┌──────────┐ ┌───────────┐ ┌────────┐ ┌──────┐ ┌──────────┐ │
│  │ Deepgram │ │AssemblyAI │ │ Claude │ │Eleven│ │ Pinecone │ │
│  │ STT API  │ │Diarize API│ │ Haiku  │ │ Labs │ │ VectorDB │ │
│  └──────────┘ └───────────┘ └────────┘ └──────┘ └──────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 Android 앱 레이어 구조 (Clean Architecture)

```
app/
├── presentation/          # UI Layer (MVVM)
│   ├── ui/
│   │   ├── main/          # 메인 화면 (청취 On/Off)
│   │   ├── settings/      # 설정 화면
│   │   ├── triggerlog/    # 트리거 로그 화면
│   │   ├── profile/       # 사용자 프로필/지식 관리
│   │   └── onboarding/    # 초기 설정 & 권한 요청
│   ├── viewmodel/         # ViewModel 클래스
│   └── adapter/           # RecyclerView Adapter
│
├── domain/                # Business Logic Layer
│   ├── model/             # Domain 엔티티
│   │   ├── Utterance.kt
│   │   ├── ConversationContext.kt
│   │   ├── TriggerEvent.kt
│   │   ├── WhisperResponse.kt
│   │   └── UserProfile.kt
│   ├── usecase/           # Use Case
│   │   ├── StartListeningUseCase.kt
│   │   ├── StopListeningUseCase.kt
│   │   ├── ProcessUtteranceUseCase.kt
│   │   ├── DetectTriggerUseCase.kt
│   │   ├── GenerateWhisperUseCase.kt
│   │   └── ManageInterventionBudgetUseCase.kt
│   └── repository/        # Repository Interface
│
├── data/                  # Data Layer
│   ├── repository/        # Repository 구현체
│   ├── local/             # Room DB, DataStore, In-memory
│   ├── remote/            # Deepgram, AssemblyAI, Claude, ElevenLabs, Calendar, Pinecone
│   └── mapper/            # Data ↔ Domain 매퍼
│
├── service/               # Android Services
│   ├── AudioCaptureService.kt
│   ├── PipelineOrchestrator.kt
│   └── BudsConnectionService.kt
│
├── engine/                # Core Engines
│   ├── vad/               # Silero VAD
│   ├── stt/               # Deepgram + Whisper
│   ├── diarization/       # 화자 분리
│   ├── context/           # 맥락 누적 엔진
│   ├── trigger/           # 트리거 감지 (rules/ + ml/)
│   ├── response/          # LLM 응답 생성
│   ├── timing/            # 타이밍 엔진
│   ├── tts/               # TTS 출력
│   └── adaptive/          # 적응형 개입 조절
│
├── buds/                  # Galaxy Buds SDK 연동
│   ├── BudsManager.kt
│   ├── GestureHandler.kt
│   ├── WearDetector.kt
│   └── AudioRouter.kt
│
└── di/                    # Dependency Injection (Hilt)
```

### 2.3 기술 스택 상세

| 카테고리 | 기술 | 버전 | 용도 |
|----------|------|------|------|
| **Language** | Kotlin | 1.9+ | Android 앱 전체 |
| **Min SDK** | Android 12 | API 31 | Foreground Service 타입 필수 |
| **DI** | Hilt | 2.50+ | 의존성 주입 |
| **Async** | Coroutines + Flow | 1.8+ | 비동기 스트림 처리 |
| **Networking** | OkHttp + Retrofit | 4.12+ / 2.9+ | REST API / WebSocket |
| **Local DB** | Room | 2.6+ | 메타데이터, 로그 |
| **ML Runtime** | ONNX Runtime | 1.16+ | Silero VAD, Whisper |
| **ML Runtime** | TensorFlow Lite | 2.14+ | 트리거 ML 모델 |
| **UI** | Jetpack Compose | 1.6+ | 선언적 UI |
| **Audio** | AudioRecord API | - | PCM 16bit 16kHz |
| **Buds** | Samsung Accessory SDK | 최신 | Galaxy Buds 연동 |

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

#### 처리 흐름

```
[서비스 시작]
    ▼
Foreground Notification 생성 ("귀띔이 주변 소리를 듣고 있어요")
    ▼
AudioRecord 초기화 (MIC, 16kHz, MONO, PCM_16BIT)
    ▼
Galaxy Buds 마이크 우선 체크 → SCO 오디오 라우팅
    ▼
[오디오 스트림 루프]
    ├── 32ms 단위 PCM 프레임 읽기
    │   ├── VAD: speech → STT Engine 전달
    │   └── VAD: silence → 프레임 드롭 (배터리 절약)
    └── WearDetector 이벤트 수신
        ├── 버즈 탈착 → 자동 일시정지
        └── 버즈 착용 → 자동 재개
```

#### 권한 요구사항

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.READ_CALENDAR" />
<uses-permission android:name="android.permission.READ_CONTACTS" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```

#### 배터리 최적화 전략

| 전략 | 구현 방법 | 예상 절감 |
|------|-----------|-----------|
| VAD 필터링 | Silero VAD로 침묵 구간 필터링 | CPU 60% 감소 |
| 적응형 샘플링 | 조용할 때 VAD 주기 확대 (32→64ms) | 추가 10% 절감 |
| WakeLock 최소화 | PARTIAL_WAKE_LOCK만 사용 | 화면OFF 50% 절감 |
| 배터리 모니터링 | 20% 이하 시 저전력 모드 전환 | 긴급 대비 |

---

### 3.2 모듈 M-002: VAD (Voice Activity Detection)

| 항목 | 내용 |
|------|------|
| **모듈 ID** | M-002 |
| **모델** | Silero VAD v5 (ONNX, ~1MB) |
| **런타임** | ONNX Runtime Mobile |
| **입력** | PCM 16-bit 16kHz, 512 samples/frame |
| **출력** | Float (0.0~1.0 speech probability) |
| **임계값** | speech ≥ 0.5 / silence < 0.3 |
| **처리 시간** | < 1ms per frame (CPU) |

#### 핵심 인터페이스

```kotlin
interface VadEngine {
    suspend fun initialize(context: Context)
    fun processFrame(audioFrame: ShortArray): Float
    fun getCurrentState(): VadState  // SPEECH, SILENCE, TRANSITION
    fun getSilenceDurationMs(): Long
    fun release()
}
```

#### 상태 전환

- **Speech 진입**: 3연속 프레임(96ms) speech prob ≥ 0.5
- **Silence 진입**: 10연속 프레임(320ms) speech prob < 0.3
- **Debounce**: 짧은 기침/소음 오작동 방지

---

### 3.3 모듈 M-003: STT 엔진 (Speech-to-Text)

| 항목 | 내용 |
|------|------|
| **모듈 ID** | M-003 |
| **이중 구성** | 온디바이스 (Whisper) + 클라우드 (Deepgram) |
| **기본 모드** | 클라우드 STT (MVP~베타) |
| **스트리밍** | WebSocket 기반 실시간 단어 단위 전사 |
| **목표 지연** | 300~500ms |

#### Deepgram WebSocket 설정

```kotlin
val DEEPGRAM_PARAMS = mapOf(
    "model" to "nova-2", "language" to "ko",
    "smart_format" to "true", "interim_results" to "true",
    "utterance_end_ms" to "1500", "vad_events" to "true",
    "encoding" to "linear16", "sample_rate" to "16000"
)
```

#### 온디바이스 Whisper (Phase 3)

| 모델 | 크기 | 지연 | 한국어 정확도 |
|------|------|------|--------------|
| tiny | ~40MB | 800ms | ~80% |
| base | ~80MB | 1.5s | ~87% |

---

### 3.4 모듈 M-004: 화자 분리 (Speaker Diarization)

| 항목 | 내용 |
|------|------|
| **모듈 ID** | M-004 |
| **Phase** | Phase 2 (베타)부터 |
| **API** | AssemblyAI Real-time + Speaker Labels |
| **최대 화자** | 4명 |
| **라벨** | SELF / SPEAKER_A / SPEAKER_B / SPEAKER_C |

#### 사용자 본인 식별

1. **온보딩**: 음성 샘플 3~5문장 → Voice Embedding 생성 → 로컬 암호화 저장
2. **실시간**: 각 speaker embedding과 코사인 유사도 비교 (≥ 0.85 → SELF)

#### 데이터 모델

```kotlin
data class Utterance(
    val id: String, val speaker: SpeakerLabel,
    val text: String, val startTimeMs: Long, val endTimeMs: Long,
    val confidence: Float, val language: String = "ko",
    val entities: List<Entity> = emptyList()
)
```

---

### 3.5 모듈 M-005: 맥락 누적 엔진

| 항목 | 내용 |
|------|------|
| **모듈 ID** | M-005 |
| **단기 메모리** | 최근 10분 전체 대화 (로컬 SQLite/Dragonfly) |
| **장기 메모리** | 일별 압축 요약 + 벡터 임베딩 (Pinecone) |
| **엔티티** | DATETIME, PERSON, LOCATION, MONEY, PROJECT, TODO, QUESTION, KEYWORD, UNKNOWN_TERM |

#### 단기 메모리 (Rolling Window 10분)

- `Queue<Utterance>` — 10분 경과 시 엔티티만 보존, 원문 삭제
- 동시 관리: activeEntities, conversationTopics, currentSpeakers

#### 장기 메모리 (Pinecone)

| 항목 | 내용 |
|------|------|
| 임베딩 | OpenAI text-embedding-3-small (1536dim) |
| 보존 | Free: 7일 / Pro: 30일 / Enterprise: 무제한 |
| 암호화 | AES-256, 사용자별 키 격리 |

---

### 3.6 모듈 M-006: 트리거 감지 엔진

| 트리거 | 감지 조건 | 우선순위 |
|--------|-----------|----------|
| **T-001 일정 갭** | DATETIME엔티티 + 질문형 문장 → Calendar API 조회 | ★★★★★ |
| **T-002 침묵 감지** | 상대방 발화 후 SELF 무응답 ≥ 2.5초 + 질문형 패턴 | ★★★★☆ |
| **T-003 위험 감지** | 위험 키워드("위약금","해지불가","자동갱신" 등) | ★★★★☆ |
| **T-004 모르는 용어** | 지식 프로필 미등록 전문용어 (세션 내 최초만) | ★★★☆☆ |
| **T-005 기억 보조** | 이전 엔티티 재등장 (간격 ≥ 5분) | ★★★☆☆ |
| **T-006 키워드 즉시** | 사용자 등록 키워드 매칭 | ★★★★☆ |
| **T-007 인물 식별** | 인물명 → 연락처 매칭 | ★★★☆☆ |

#### 충돌 해소 규칙
- 동시 트리거 시 우선순위 높은 것만 실행
- 동일 우선순위면 먼저 감지된 것 우선
- 10분 3회 버짓 내에서만 개입

---

### 3.7 모듈 M-007: 귓속말 출력 엔진

| 항목 | 내용 |
|------|------|
| **TTS** | ElevenLabs 한국어 (eleven_multilingual_v2) |
| **음성 길이** | 최대 15초 (3~5문장) |
| **볼륨** | 일반 볼륨의 40~60% (귓속말 톤) |
| **타이밍** | 0.8초+ 침묵 구간에서만 출력 (긴급 제외) |

#### 타이밍 엔진

- **URGENT** (위험감지): 즉시 진동(100ms) + 경고음(200ms) + 귓속말
- **NORMAL**: 침묵 ≥ 800ms 대기 → 출력 / 30초 타임아웃 → 폐기

---

### 3.8 모듈 M-008: 적응형 개입 조절

| 사용자 반응 | 조치 |
|------------|------|
| **ACTED** (행동) | 임계값 유지 |
| **REPLAYED** (재청취) | 임계값 유지 |
| **DETAIL_REQUEST** (상세요청) | 임계값 소폭 하향 (더 적극적) |
| **IGNORED** (무시) | 임계값 10% 상향 (더 조용) |
| **STOPPED** (중단) | 임계값 30% 상향 + 5분 쿨다운 |

---

## 4. 데이터베이스 스키마 설계

### 4.1 로컬 DB (Room / SQLite)

```sql
-- 트리거 이벤트 로그
CREATE TABLE trigger_events (
    id              TEXT PRIMARY KEY,
    trigger_type    TEXT NOT NULL,
    timestamp       INTEGER NOT NULL,
    utterance_id    TEXT,
    context_summary TEXT,
    whisper_text    TEXT,
    whisper_audio   TEXT,
    was_delivered   INTEGER DEFAULT 0,
    user_reaction   TEXT,
    priority        INTEGER DEFAULT 3,
    session_id      TEXT NOT NULL
);

-- 사용자 지식 DB
CREATE TABLE user_knowledge (
    id              TEXT PRIMARY KEY,
    term            TEXT NOT NULL UNIQUE,
    category        TEXT,
    known           INTEGER DEFAULT 0,
    explanation     TEXT,
    first_seen_at   INTEGER,
    last_seen_at    INTEGER
);

-- 사용자 등록 키워드
CREATE TABLE custom_keywords (
    id              TEXT PRIMARY KEY,
    keyword         TEXT NOT NULL,
    pattern         TEXT,
    action_type     TEXT NOT NULL,
    action_data     TEXT,
    is_active       INTEGER DEFAULT 1,
    created_at      INTEGER NOT NULL
);

-- 세션 로그
CREATE TABLE sessions (
    id              TEXT PRIMARY KEY,
    start_time      INTEGER NOT NULL,
    end_time        INTEGER,
    total_utterances INTEGER DEFAULT 0,
    total_triggers  INTEGER DEFAULT 0,
    total_whispers  INTEGER DEFAULT 0,
    avg_latency_ms  INTEGER,
    battery_start   INTEGER,
    battery_end     INTEGER
);

-- 개입 빈도 학습 데이터
CREATE TABLE intervention_feedback (
    id              TEXT PRIMARY KEY,
    trigger_type    TEXT NOT NULL,
    reaction        TEXT NOT NULL,
    threshold_before REAL,
    threshold_after  REAL,
    timestamp       INTEGER NOT NULL
);
```

### 4.2 인메모리 단기 메모리 (Dragonfly)

```
stm:{sessionId}:utterances     → List<Utterance>  (TTL: 600s)
stm:{sessionId}:entities       → Hash<EntityType, List<Entity>>
stm:{sessionId}:topics         → Set<String>
stm:{sessionId}:speakers       → Set<SpeakerLabel>
stm:{sessionId}:trigger_budget → Integer (남은 개입 횟수)
stm:{sessionId}:last_trigger   → Timestamp
```

### 4.3 벡터 DB (Pinecone) - 장기 메모리

```json
{
  "index": "earbrief-memory",
  "dimension": 1536,
  "metric": "cosine",
  "record": {
    "id": "mem_20260306_session_abc123",
    "values": [0.012, -0.034, "...1536dim..."],
    "metadata": {
      "user_id": "user_xxx",
      "date": "2026-03-06",
      "summary": "김부장과 알파프로젝트 예산 협의. 7천만원 제안.",
      "entities": ["김부장", "알파프로젝트", "7000만원"],
      "plan_tier": "pro"
    }
  }
}
```

---

> **[Part 2](earbrief_dev_spec_part2.md)에서 계속:**
> - 5. API 명세 및 외부 서비스 연동
> - 6. 보안 및 개인정보보호 아키텍처
> - 7. 테스트 전략 및 품질 보증
> - 8. CI/CD 및 배포 전략
> - 9. Phase별 상세 구현 계획 (스프린트 분배)
> - 10. 모니터링 및 운영 전략
