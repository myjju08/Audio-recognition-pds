package com.earbrief.app.engine.context

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KoreanQuestionDetector @Inject constructor() : QuestionDetector {
    private val questionEndings = listOf(
        "세요?", "까요?", "나요?", "죠?", "어요?", "아요?",
        "할까요?", "될까요?", "있어요?", "없어요?",
        "어때요?", "어떠세요?", "괜찮으세요?",
        "맞아요?", "아닌가요?", "그런가요?",
        "해볼까요?", "진행할까요?", "시작할까요?",
        "입니까?", "습니까?", "ㅂ니까?",
        "인가요?", "건가요?", "은가요?",
        "인데요?", "는데요?",
        "어?", "지?", "냐?", "니?", "나?", "래?", "ㄹ까?"
    )

    private val questionPatterns = listOf(
        Regex("(시간|일정|스케줄).*(되|있|괜찮)"),
        Regex("(언제|어디|얼마|몇|누구|무엇|왜|어떻게)"),
        Regex("(생각|의견|제안).*(있|해주|말씀)"),
        Regex(".*\\?\\s*$")
    )

    override fun isQuestion(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        return questionEndings.any { trimmed.endsWith(it) } ||
            questionPatterns.any { it.containsMatchIn(trimmed) }
    }
}
