package com.earbrief.app.engine.context

interface QuestionDetector {
    fun isQuestion(text: String): Boolean
}
