package com.earbrief.app.engine.context

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("KoreanQuestionDetector")
class KoreanQuestionDetectorTest {
    private val detector = KoreanQuestionDetector()

    @Nested
    @DisplayName("positive detection")
    inner class PositiveDetection {

        @Test
        fun `detects korean question utterances`() {
            val positives = listOf(
                "시간 있으세요?",
                "괜찮을까요?",
                "이건 뭐죠?",
                "왜 그렇게 됐나요?",
                "다음 주에 가능한가요?",
                "언제 만날까요?",
                "어디에서 진행할까요?",
                "얼마나 걸리나요?",
                "몇 시가 좋을까요?",
                "누구랑 같이 가나요?",
                "무엇을 준비하면 될까요?",
                "어떻게 하면 좋을까요?",
                "의견 있으세요?",
                "시간 괜찮으세요?",
                "일정 되나요?",
                "시작할까요?"
            )

            positives.forEach { text ->
                assertThat(detector.isQuestion(text)).isTrue()
            }
        }
    }

    @Nested
    @DisplayName("negative detection")
    inner class NegativeDetection {

        @Test
        fun `does not detect statements and requests as question`() {
            val negatives = listOf(
                "회의 일정 잡아주세요",
                "네, 알겠습니다",
                "보고서 제출했습니다",
                "오늘 작업 마무리했습니다",
                "자료 공유 부탁드립니다",
                "지금 출발하겠습니다",
                "곧 연락드리겠습니다",
                "회의실 예약 완료",
                "이번 주 금요일에 진행합니다"
            )

            negatives.forEach { text ->
                assertThat(detector.isQuestion(text)).isFalse()
            }
        }
    }
}
