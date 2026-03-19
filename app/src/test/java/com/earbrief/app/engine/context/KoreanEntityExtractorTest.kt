package com.earbrief.app.engine.context

import com.earbrief.app.domain.model.EntityType
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("KoreanEntityExtractor")
class KoreanEntityExtractorTest {
    private val referenceTimeMs = ZonedDateTime
        .of(2026, 3, 18, 9, 0, 0, 0, ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    private val isoPattern = Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}")

    @Nested
    @DisplayName("integration extraction")
    inner class IntegrationExtraction {
        private val extractor = KoreanEntityExtractor(
            dateTimeParser = KoreanDateTimeParser(),
            questionDetector = KoreanQuestionDetector()
        )

        @Test
        fun `extracts datetime and question entities across korean utterances`() {
            val cases = listOf(
                Case("내일 오후 3시에 미팅 가능하세요?", true, true, "내일 오후 3시"),
                Case("다음 주 화요일 오전 10시 괜찮나요?", true, true, "다음 주 화요일 오전 10시"),
                Case("모레 점심에 만날까요?", true, true, "모레 점심"),
                Case("3월 15일 오후 2시", true, false, "3월 15일 오후 2시"),
                Case("오늘 회의록 정리했어요", false, false, null),
                Case("이번 주 금요일 저녁에 시간 되세요?", true, true, "이번 주 금요일 저녁"),
                Case("30분 후에 전화할게요", true, false, "30분 후"),
                Case("2시 반에 보자", true, false, "2시 반"),
                Case("글피 아침에 출장입니다", true, false, "글피 아침"),
                Case("다다음 주 월요일 오전 9시 가능하세요?", true, true, "다다음 주 월요일 오전 9시"),
                Case("2026년 4월 2일 오후 1시 회의", true, false, "2026년 4월 2일 오후 1시"),
                Case("5월 1일에 마감입니다", true, false, "5월 1일"),
                Case("다음 주 일요일 저녁은 어때요?", true, true, "다음 주 일요일 저녁"),
                Case("2시간 후 다시 확인해 주세요", true, false, "2시간 후"),
                Case("오후 4시 30분에 출발합니다", true, false, "오후 4시 30분"),
                Case("오전 11시 괜찮나요?", true, true, "오전 11시"),
                Case("내일 점심에 가능할까요?", true, true, "내일 점심"),
                Case("이번 주 수요일 일정 확정했습니다", true, false, "이번 주 수요일"),
                Case("10시 15분에 브리핑 진행", true, false, "10시 15분"),
                Case("저녁에 통화 가능하세요?", true, true, "저녁"),
                Case("그냥 이야기해요", false, false, null)
            )

            cases.forEach { case ->
                val entities = extractor.extract(case.text, referenceTimeMs)
                val dateTimes = entities.filter { it.type == EntityType.DATETIME }
                val questions = entities.filter { it.type == EntityType.QUESTION }

                assertThat(dateTimes.isNotEmpty()).isEqualTo(case.hasDateTime)
                assertThat(questions.isNotEmpty()).isEqualTo(case.hasQuestion)

                if (case.hasDateTime) {
                    val firstDateTime = dateTimes.first()
                    assertThat(isoPattern.matches(firstDateTime.normalizedValue)).isTrue()
                    assertThat(firstDateTime.position.first).isAtLeast(0)
                    assertThat(firstDateTime.position.last).isAtLeast(firstDateTime.position.first)
                    assertThat(firstDateTime.sourceUtteranceId).isEmpty()
                    case.expectedValueSnippet?.let { snippet ->
                        assertThat(firstDateTime.value).contains(snippet)
                    }
                }

                if (case.hasQuestion) {
                    val question = questions.first()
                    assertThat(question.normalizedValue).isEqualTo("QUESTION")
                    assertThat(question.confidence).isEqualTo(0.9f)
                }
            }
        }
    }

    @Nested
    @DisplayName("entity mapping")
    inner class MappingTest {

        @Test
        fun `maps parser results into datetime entity fields`() {
            val parser = mockk<KoreanDateTimeParser>()
            val detector = mockk<QuestionDetector>()
            val extractor = KoreanEntityExtractor(parser, detector)
            val parseResult = DateTimeParseResult(
                value = "내일 오후 3시",
                normalizedValue = "2026-03-19T15:00",
                range = 0..7,
                confidence = 0.93f
            )

            every { parser.parse(any(), any()) } returns listOf(parseResult)
            every { detector.isQuestion(any()) } returns false

            val entities = extractor.extract("내일 오후 3시에 미팅", referenceTimeMs)
            assertThat(entities).hasSize(1)

            val entity = entities.first()
            assertThat(entity.type).isEqualTo(EntityType.DATETIME)
            assertThat(entity.value).isEqualTo("내일 오후 3시")
            assertThat(entity.normalizedValue).isEqualTo("2026-03-19T15:00")
            assertThat(entity.confidence).isEqualTo(0.93f)
            assertThat(entity.position).isEqualTo(0..7)
        }
    }

    private data class Case(
        val text: String,
        val hasDateTime: Boolean,
        val hasQuestion: Boolean,
        val expectedValueSnippet: String?
    )
}
