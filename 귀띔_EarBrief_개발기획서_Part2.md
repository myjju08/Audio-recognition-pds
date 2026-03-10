# 🎧 귀띔(EarBrief) 개발 기획서 v1.0 — Part 2

> **문서 버전**: v1.0  
> **작성일**: 2026-03-06  
> **이전 문서**: [Part 1 - 시스템 아키텍처 & 모듈 설계](earbrief_dev_spec_part1.md)

---

## 목차 (Part 2)

5. [API 명세 및 외부 서비스 연동](#5-api-명세-및-외부-서비스-연동)
6. [보안 및 개인정보보호 아키텍처](#6-보안-및-개인정보보호-아키텍처)
7. [테스트 전략 및 품질 보증](#7-테스트-전략-및-품질-보증)
8. [CI/CD 및 배포 전략](#8-cicd-및-배포-전략)
9. [Phase별 상세 구현 계획](#9-phase별-상세-구현-계획)
10. [모니터링 및 운영 전략](#10-모니터링-및-운영-전략)

---

## 5. API 명세 및 외부 서비스 연동

### 5.1 외부 서비스 의존성 맵

| 서비스 | 용도 | Phase | 과금 모델 | 예상 월 비용 (1000 사용자) |
|--------|------|-------|-----------|--------------------------|
| **Deepgram** | 실시간 STT | MVP~ | $0.0043/min | ~$6,450 |
| **AssemblyAI** | 화자 분리 | 베타~ | $0.00025/sec | ~$2,250 |
| **Claude Haiku** | 귓속말 생성 | 베타~ | $0.25/1M input tok | ~$750 |
| **ElevenLabs** | TTS 귓속말 음성 | MVP~ | $0.18/1K chars | ~$2,700 |
| **Google Calendar** | 빈 시간 조회 | MVP~ | 무료 (OAuth) | $0 |
| **OpenAI Embedding** | 장기메모리 벡터화 | 베타~ | $0.02/1M tok | ~$60 |
| **Pinecone** | 벡터 DB | 베타~ | Serverless $0.08/1M reads | ~$240 |
| **Firebase** | Auth, Analytics, Crashlytics | MVP~ | 무료~Spark | $0~$25 |

### 5.2 Deepgram Streaming STT API 연동 상세

#### WebSocket 연결 관리

```kotlin
class DeepgramSttEngine(
    private val apiKey: String,
    private val config: DeepgramConfig
) : SttEngine {

    private var webSocket: WebSocket? = null
    private val reconnectDelay = 1000L  // 재연결 대기 (ms)
    private val maxRetries = 5

    // 연결 URL 생성
    private fun buildUrl(): String {
        val params = listOf(
            "model=nova-2", "language=ko",
            "smart_format=true", "interim_results=true",
            "utterance_end_ms=1500", "vad_events=true",
            "encoding=linear16", "sample_rate=16000", "channels=1"
        ).joinToString("&")
        return "wss://api.deepgram.com/v1/listen?$params"
    }

    // 오디오 프레임 전송
    fun sendAudioFrame(pcmData: ByteArray) {
        webSocket?.send(pcmData.toByteString())
    }

    // 응답 처리
    override fun onMessage(text: String) {
        val result = Json.decodeFromString<DeepgramResult>(text)
        if (result.isFinal) {
            // ContextEngine으로 최종 전사 결과 전달
            contextEngine.processUtterance(
                Utterance(
                    text = result.transcript,
                    confidence = result.confidence,
                    words = result.words
                )
            )
        }
    }
}
```

#### 에러 핸들링 & Fallback

```
[WebSocket 연결]
    │
    ├── 성공 → 정상 스트리밍
    │
    ├── 401 Unauthorized → API 키 갱신 시도
    │
    ├── 연결 끊김 → 지수 백오프 재연결 (1s → 2s → 4s → 8s → 16s)
    │   └── 5회 실패 → 온디바이스 Whisper 전환 (Phase 3)
    │                  → 사용자 알림: "오프라인 모드로 전환됩니다"
    │
    └── 네트워크 없음 → 즉시 온디바이스 전환
```

### 5.3 Claude Haiku API 연동 (귓속말 생성)

#### 프롬프트 템플릿

```python
WHISPER_GENERATION_PROMPT = """
당신은 '귀띔'이라는 AI 비서입니다. 사용자의 귀에 소근소근 귓속말로 도움을 줍니다.

[규칙]
1. 3~5문장 이내로 매우 간결하게
2. 친근하지만 정중한 반말 톤 ("~해요", "~이에요")
3. 핵심 정보만 전달, 불필요한 인사/설명 생략
4. 숫자/날짜는 구체적으로 언급
5. 긴급할 때는 "주의:" 접두어 사용

[트리거 유형]: {trigger_type}
[현재 맥락]:
- 대화 요약: {conversation_summary}
- 감지된 엔티티: {entities}
- 트리거 원인: {trigger_reason}

[추가 정보]:
{additional_context}

귓속말을 생성하세요:
"""
```

#### API 호출 설정

```kotlin
data class ClaudeRequest(
    val model: String = "claude-3-5-haiku-20241022",
    val maxTokens: Int = 150,        // 짧은 응답 강제
    val temperature: Float = 0.3f,   // 일관된 응답
    val stream: Boolean = true,      // 스트리밍 (지연 최소화)
    val system: String = SYSTEM_PROMPT,
    val messages: List<Message>
)

// 평균 지연: 200~400ms (스트리밍 첫 토큰)
// 평균 입력 토큰: ~300 / 출력 토큰: ~80
```

### 5.4 ElevenLabs TTS API 연동

```kotlin
data class ElevenLabsRequest(
    val text: String,
    val modelId: String = "eleven_multilingual_v2",
    val voiceSettings: VoiceSettings = VoiceSettings(
        stability = 0.75f,
        similarityBoost = 0.80f,
        style = 0.15f,
        useSpeakerBoost = false
    ),
    val outputFormat: String = "mp3_22050_32"  // 저용량 (귓속말용)
)

// 오디오 캐싱: 동일 텍스트 재요청 시 로컬 캐시 사용
// 캐시 TTL: 24시간 / 최대 50MB
```

### 5.5 Google Calendar API 연동

```kotlin
class CalendarRepository(
    private val calendarApi: GoogleCalendarApi
) {
    /**
     * 특정 날짜/시간 범위의 빈 시간 슬롯 조회
     * @param date 조회할 날짜
     * @param startHour 시작 시간 (기본 9시)
     * @param endHour 종료 시간 (기본 18시)
     * @return 30분 단위 빈 슬롯 리스트
     */
    suspend fun getAvailableSlots(
        date: LocalDate,
        startHour: Int = 9,
        endHour: Int = 18
    ): List<TimeSlot> {
        val events = calendarApi.events().list("primary")
            .setTimeMin(DateTime(date.atTime(startHour, 0)))
            .setTimeMax(DateTime(date.atTime(endHour, 0)))
            .setSingleEvents(true)
            .setOrderBy("startTime")
            .execute()

        return calculateFreeSlots(events.items, startHour, endHour)
    }
}
```

---

## 6. 보안 및 개인정보보호 아키텍처

### 6.1 핵심 보안 원칙

> **"녹음하지 않는다. 분석하고 즉시 삭제한다."**

| 원칙 | 구현 방법 |
|------|-----------|
| **No Recording** | PCM 오디오 데이터는 메모리에서만 처리, 디스크 저장 안 함 |
| **Immediate Deletion** | STT 변환 완료 즉시 원본 오디오 프레임 메모리 해제 |
| **10분 TTL** | 단기 메모리 텍스트도 10분 후 자동 삭제 |
| **암호화 전송** | 모든 클라우드 API 호출 TLS 1.3 필수 |
| **암호화 저장** | 장기 메모리 AES-256 암호화. 키는 Android Keystore 보관 |
| **사용자 제어권** | 언제든 전체 데이터 삭제 가능. "데이터 폭파" 버튼 제공 |

### 6.2 데이터 흐름별 보안 조치

```
[마이크 입력] ──── 메모리 only, 디스크 저장 금지
      │
      ▼
[VAD/STT 처리] ── 처리 완료 즉시 오디오 데이터 zeroing
      │
      ▼
[텍스트 데이터] ── 10분 TTL, 만료 시 secure delete
      │
      ├──▶ [클라우드 전송] ── TLS 1.3 + API 키 암호화 저장 (Keystore)
      │                        서버에서 30초 내 삭제 (Deepgram DPA 준수)
      │
      └──▶ [장기 메모리 저장] ── AES-256 암호화 → Pinecone 저장
                                  사용자별 격리 (namespace)
                                  플랜별 만료일 자동 삭제
```

### 6.3 권한 & 동의 UX

1. **onboarding 시**: 명확한 동의 화면
   - "귀띔은 주변 소리를 실시간으로 분석하지만, 녹음하거나 저장하지 않습니다"
   - 개인정보 처리방침 전문 링크
   - 동의 체크박스 (마이크, 캘린더, 연락처 각각)

2. **실행 중**: Foreground Notification 상시 표시
   - "🎧 귀띔이 듣고 있어요" + 일시정지 버튼
   - 상태바 아이콘으로 마이크 사용 중 표시

3. **데이터 관리**: 설정 > 개인정보
   - "모든 데이터 삭제" 버튼 (즉시 실행)
   - 장기 메모리 개별 항목 열람/삭제
   - 데이터 내보내기 (GDPR 대응)

### 6.4 법적 준수 사항

| 법률 | 조치 |
|------|------|
| **개인정보보호법 (한국)** | 녹음이 아닌 실시간 분석+즉시삭제 아키텍처. 개인정보 처리방침 공개 |
| **통신비밀보호법** | 제3자 동의 관련 법무법인 검토. 앱 실행 시 고지 의무 이행 |
| **GDPR** | 데이터 이동권, 삭제권 보장. DPA 체결 (클라우드 서비스별) |
| **Google Play 정책** | Foreground Service 마이크 타입 선언. 권한 사용 목적 명시 |

---

## 7. 테스트 전략 및 품질 보증

### 7.1 테스트 레벨별 전략

| 레벨 | 커버리지 목표 | 도구 | 대상 |
|------|-------------|------|------|
| **Unit Test** | 80%+ | JUnit5 + MockK | 모든 Engine, UseCase, Repository |
| **Integration Test** | 핵심 흐름 100% | Espresso + Hilt Testing | API 연동, DB CRUD |
| **E2E Test** | 주요 시나리오 4종 | UI Automator | 전체 파이프라인 |
| **Performance Test** | 지연시간 SLA | Android Profiler + 커스텀 | 파이프라인 벤치마크 |
| **STT 정확도 Test** | 한국어 벤치마크 | 커스텀 테스트셋 | 음성 인식 품질 |

### 7.2 핵심 테스트 시나리오

#### TC-001: 일정 갭 트리거 E2E

```
사전조건: 캘린더에 화요일 오후 2~5시 빈 시간
입력: "다음 주 화요일 오후에 시간 되세요?" (테스트 오디오)
기대:
  1. STT 전사 정확도 ≥ 90%
  2. DATETIME 엔티티 추출 ("다음 주 화요일 오후")
  3. QUESTION 감지 (true)
  4. Calendar API 호출 → 빈 시간 반환
  5. 침묵 구간 감지 (0.8초+)
  6. TTS 귓속말 출력 (15초 이내)
  7. 전체 지연시간 < 2.0초 (MVP)
```

#### TC-002: 위험 감지 트리거

```
입력: "위약금은 계약금의 30%이고 3개월 해지 불가 조항이 있습니다"
기대:
  1. 위험 키워드 감지: "위약금", "해지 불가"
  2. 긴급 모드 활성화
  3. 진동(100ms) + 경고음(200ms) + 귓속말
  4. 귓속말 내용에 "30%", "3개월" 포함
```

#### TC-003: 배터리 영향 테스트

```
조건: 2시간 연속 청취 (혼합 환경: 대화 40% + 침묵 60%)
기대:
  배터리 추가 소모 < 20% (MVP) / < 10% (정식)
  메모리 사용량 < 200MB
  CPU 평균 사용률 < 15%
```

#### TC-004: 과개입 방지 테스트

```
조건: 10분간 트리거 조건 10회 발생
기대:
  실제 개입 ≤ 3회 (버짓 준수)
  사용자 STOPPED 반응 시 즉시 침묵 + 쿨다운
```

### 7.3 STT 정확도 벤치마크 셋

| 환경 | 테스트 문장 수 | 목표 정확도 |
|------|-------------|------------|
| 조용한 실내 | 200문장 | ≥ 95% |
| 카페 (50dB 소음) | 200문장 | ≥ 85% |
| 야외 (바람/교통) | 100문장 | ≥ 75% |
| 다자간 대화 (3인) | 100문장 | ≥ 80% |
| 전문용어 (의료/법률) | 100문장 | ≥ 85% |

---

## 8. CI/CD 및 배포 전략

### 8.1 CI Pipeline (GitHub Actions)

```yaml
# .github/workflows/android-ci.yml
name: EarBrief CI

on: [push, pull_request]

jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Kotlin Lint (ktlint)
        run: ./gradlew ktlintCheck
      - name: Android Lint
        run: ./gradlew lintDebug

  unit-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Unit Tests
        run: ./gradlew testDebugUnitTest
      - name: Coverage Report
        run: ./gradlew koverReport
      - name: Check Coverage ≥ 80%
        run: ./scripts/check_coverage.sh 80

  integration-test:
    runs-on: macos-latest  # Emulator 필요
    steps:
      - uses: actions/checkout@v4
      - name: Start Emulator
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 34
          script: ./gradlew connectedDebugAndroidTest

  build:
    needs: [lint, unit-test]
    runs-on: ubuntu-latest
    steps:
      - name: Build APK
        run: ./gradlew assembleRelease
      - name: Upload to Firebase App Distribution
        uses: wzieba/Firebase-Distribution-Github-Action@v1
```

### 8.2 배포 전략

| 단계 | 배포 채널 | 대상 | 자동화 |
|------|----------|------|--------|
| **Internal** | Firebase App Distribution | 개발팀 (5명) | push to `dev` 자동 |
| **Closed Beta** | Google Play 비공개 테스트 | 얼리어답터 100명 | PR merge to `beta` |
| **Open Beta** | Google Play 오픈 테스트 | 1,000명 | 수동 승인 후 배포 |
| **Production** | Google Play + Galaxy Store | 전체 | 수동 승인 (관리자 2인) |

### 8.3 Feature Flag 시스템

```kotlin
object FeatureFlags {
    // Phase별 기능 제어
    val SPEAKER_DIARIZATION = RemoteConfig.getBoolean("ff_diarization")
    val ML_TRIGGER = RemoteConfig.getBoolean("ff_ml_trigger")
    val ONDEVICE_STT = RemoteConfig.getBoolean("ff_ondevice_stt")
    val ONDEVICE_LLM = RemoteConfig.getBoolean("ff_ondevice_llm")

    // 트리거별 On/Off
    val TRIGGER_SCHEDULE_GAP = RemoteConfig.getBoolean("ff_trigger_schedule")
    val TRIGGER_RISK_DETECT = RemoteConfig.getBoolean("ff_trigger_risk")
    // ... 7종 각각 제어 가능
}
```

---

## 9. Phase별 상세 구현 계획

### 9.1 Phase 1: MVP (0~2개월, 8주)

#### Sprint 1-2 (Week 1~2): 프로젝트 설정 & 오디오 캡처

| Task | 상세 | 담당 | 예상 공수 |
|------|------|------|-----------|
| 프로젝트 초기화 | Kotlin + Compose + Hilt 프로젝트 생성 | Android | 2d |
| Foreground Service | AudioCaptureService 기본 구현 | Android | 3d |
| 오디오 캡처 | AudioRecord 16kHz PCM 스트림 구현 | Android | 2d |
| VAD 연동 | Silero VAD ONNX 로드 + 프레임 처리 | Android/ML | 3d |
| 권한 처리 | 마이크/알림 권한 요청 플로우 | Android | 1d |
| 테스트 | VAD 정확도 검증, 배터리 초기 측정 | QA | 2d |

#### Sprint 3-4 (Week 3~4): STT 연동

| Task | 상세 | 담당 | 예상 공수 |
|------|------|------|-----------|
| Deepgram WebSocket | 연결/재연결/에러 핸들링 구현 | Android | 3d |
| STT 스트림 | PCM → WebSocket → 전사결과 수신 파이프라인 | Android | 3d |
| 전사 결과 파싱 | interim/final 결과 처리 | Android | 2d |
| 기본 UI | 청취 On/Off 토글 + 전사 텍스트 표시 | Android | 2d |
| 테스트 | STT 정확도 벤치마크 (5개 환경) | QA | 2d |

#### Sprint 5-6 (Week 5~6): 트리거 3종 & 캘린더

| Task | 상세 | 담당 | 예상 공수 |
|------|------|------|-----------|
| 엔티티 추출 | DATETIME, QUESTION 패턴 매칭 | AI/NLP | 3d |
| T-001 일정 갭 | 트리거 로직 + Calendar API 연동 | Android | 4d |
| T-002 침묵 감지 | 침묵 지속시간 기반 트리거 | Android | 2d |
| T-006 키워드 즉시 | 사용자 키워드 등록/매칭 | Android | 2d |
| 맥락 저장 | SQLite 10분 롤링 윈도우 | Android | 2d |
| 테스트 | 트리거 정탐률/오탐률 측정 | QA | 2d |

#### Sprint 7-8 (Week 7~8): TTS & 통합

| Task | 상세 | 담당 | 예상 공수 |
|------|------|------|-----------|
| ElevenLabs TTS | API 연동 + 오디오 재생 | Android | 3d |
| 타이밍 엔진 | 침묵 구간 감지 + 출력 스케줄링 | Android | 3d |
| 버즈 오디오 출력 | Bluetooth A2DP/SCO 라우팅 | Android | 2d |
| 트리거 로그 UI | 로그 리스트 화면 구현 | Android | 2d |
| E2E 통합 테스트 | 전체 파이프라인 시나리오 4종 | QA | 3d |
| 버그 수정 | 통합 테스트 발견 이슈 해결 | All | 2d |

> **MVP 완성 기준**: 마이크→STT→트리거3종→귓속말 출력 전체 파이프라인 정상 작동

---

### 9.2 Phase 2: 베타 (2~4개월, 8주)

#### Sprint 9-10: 화자 분리 & 맥락 엔진 고도화

- AssemblyAI 화자 분리 연동 (SELF vs 상대방)
- 온보딩 음성 등록 + Voice Embedding
- EntityExtractor 고도화 (PERSON, LOCATION, MONEY, PROJECT)
- Dragonfly 단기 메모리 도입
- 벡터 DB (Pinecone) 장기 메모리 구현

#### Sprint 11-12: 트리거 7종 완성

- T-003 위험 감지 (키워드 사전 + 긴급모드)
- T-004 모르는 용어 (지식 프로필 DB)
- T-005 기억 보조 (엔티티 재등장 감지)
- T-007 인물 식별 (연락처 매칭)
- 트리거 충돌 해소 로직

#### Sprint 13-14: 적응형 개입 & 버즈 SDK

- 적응형 개입 조절 시스템 (10분 3회 버짓)
- 사용자 반응 학습 (ACTED/IGNORED/STOPPED)
- Galaxy Buds SDK 완전 연동 (착용감지, 터치제스처)
- 제스처 UX: 탭1회(반복), 탭2회(상세), 길게탭(정지)

#### Sprint 15-16: Claude Haiku & 베타 준비

- Claude Haiku 연동 (고품질 귓속말 생성)
- 프롬프트 최적화 & A/B 테스트
- 한국어/영어 혼용 대화 지원
- 클로즈드 베타 배포 (100명)
- 피드백 수집 시스템 구현

---

### 9.3 Phase 3: 정식 출시 (4~8개월, 16주)

#### 주요 작업

| 기간 | 작업 | 상세 |
|------|------|------|
| Week 17-20 | 온디바이스 강화 | Whisper 온디바이스 탑재, 오프라인 모드 |
| Week 21-24 | ML 트리거 | 수집 데이터 기반 TriggerClassifier 학습/배포 |
| Week 25-28 | B2B 엔터프라이즈 | 관리자 대시보드, SLA, 보안 강화 |
| Week 29-30 | 다국어 확장 | 영어, 일본어 지원 |
| Week 31-32 | 스토어 출시 | Galaxy Store + Google Play 등록 |

---

## 10. 모니터링 및 운영 전략

### 10.1 앱 모니터링 스택

| 도구 | 용도 | 수집 데이터 |
|------|------|-------------|
| **Firebase Crashlytics** | 크래시 리포트 | 스택트레이스, 디바이스 정보 |
| **Firebase Analytics** | 사용자 행동 | 세션시간, 트리거 사용률, 기능별 사용률 |
| **Firebase Performance** | 성능 | 파이프라인 지연시간, API 응답시간, 프레임 드롭 |
| **Custom Metrics** | 서비스 품질 | STT 정확도, 트리거 정탐률, 배터리 소모 |

### 10.2 핵심 모니터링 메트릭

```
[실시간 대시보드 - Grafana]

┌────────────────────────────────────────────────┐
│ 파이프라인 지연시간 (P50/P95/P99)                │
│  ├── Audio→STT: ___ms                          │
│  ├── STT→Trigger: ___ms                        │
│  ├── Trigger→LLM: ___ms                        │
│  ├── LLM→TTS: ___ms                            │
│  └── Total E2E: ___ms                          │
├────────────────────────────────────────────────┤
│ 트리거 통계                                     │
│  ├── 시간당 트리거 발생 수: ___                   │
│  ├── 트리거별 분포: [차트]                       │
│  ├── 정탐률(Precision): ___%                    │
│  └── 사용자 만족도(유용성): ___/5                │
├────────────────────────────────────────────────┤
│ 시스템 건강                                     │
│  ├── DAU / MAU: ___                            │
│  ├── 크래시율: ___%                              │
│  ├── API 에러율: ___%                           │
│  └── 평균 세션 시간: ___분                       │
└────────────────────────────────────────────────┘
```

### 10.3 알림 규칙

| 조건 | 심각도 | 알림 채널 | 대응 |
|------|--------|----------|------|
| 크래시율 > 1% | 🔴 Critical | Slack + PagerDuty | 즉시 핫픽스 |
| E2E 지연 P95 > 3초 | 🟠 Warning | Slack | 24시간 내 조사 |
| STT 에러율 > 5% | 🟠 Warning | Slack | Deepgram 상태 확인 |
| 일 API 비용 > 예산 150% | 🟡 Info | Email | 주간 리뷰에서 논의 |

### 10.4 운영 비용 최적화 전략

| 전략 | 설명 | 예상 절감 |
|------|------|-----------|
| **VAD 게이트** | 침묵 시 STT API 호출 안 함 | STT 비용 60% 절감 |
| **TTS 캐싱** | 자주 사용되는 응답 로컬 캐시 | TTS 비용 20% 절감 |
| **버짓 제한** | 10분 3회 → API 호출 자연 제한 | LLM 비용 40% 절감 |
| **온디바이스 전환** | Phase 3에서 로컬 STT/LLM | 장기적으로 80% 절감 |
| **배치 임베딩** | 세션 종료 시 1회 벡터화 | Embedding 비용 90% 절감 |

---

## 부록: KPI 추적 체크리스트

### 서비스 품질 KPI

| 지표 | MVP | 베타 | 정식 |
|------|-----|------|------|
| E2E 지연시간 | < 2.0s | < 1.5s | < 1.0s |
| STT 정확도 (한국어) | > 85% | > 90% | > 95% |
| 트리거 정탐률 | > 70% | > 80% | > 90% |
| 오탐률 | < 20% | < 10% | < 5% |
| 배터리 추가소모 | < 20%/2h | < 15%/2h | < 10%/2h |

### 비즈니스 KPI

| 지표 | 클로즈드 베타 | 오픈 베타 | 출시 6개월 |
|------|-------------|----------|-----------|
| 누적 사용자 | 100명 | 1,000명 | 10,000명 |
| 일평균 사용시간 | > 30분 | > 45분 | > 60분 |
| 귓속말 유용성 | > 3.5/5 | > 4.0/5 | > 4.3/5 |
| Pro 전환율 | - | > 5% | > 15% |
| 월간 구독 유지율 | - | > 60% | > 75% |

---

> **© 2026 EarBrief Inc. All rights reserved.**  
> 본 문서는 귀띔(EarBrief) 서비스기획서 v1.0을 기반으로 작성된 개발 기획서입니다.
