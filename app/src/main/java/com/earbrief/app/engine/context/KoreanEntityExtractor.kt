package com.earbrief.app.engine.context

import com.earbrief.app.domain.model.Entity
import com.earbrief.app.domain.model.EntityType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KoreanEntityExtractor @Inject constructor(
    private val dateTimeParser: KoreanDateTimeParser,
    private val questionDetector: QuestionDetector
) : EntityExtractor {
    override fun extract(text: String, referenceTimeMs: Long): List<Entity> {
        val entities = mutableListOf<Entity>()

        entities.addAll(
            dateTimeParser.parse(text, referenceTimeMs).map { result ->
                Entity(
                    type = EntityType.DATETIME,
                    value = result.value,
                    normalizedValue = result.normalizedValue,
                    confidence = result.confidence,
                    position = result.range,
                    sourceUtteranceId = ""
                )
            }
        )

        if (questionDetector.isQuestion(text)) {
            entities.add(
                Entity(
                    type = EntityType.QUESTION,
                    value = text,
                    normalizedValue = "QUESTION",
                    confidence = 0.9f,
                    position = 0..text.length,
                    sourceUtteranceId = ""
                )
            )
        }

        return entities
    }
}
