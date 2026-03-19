package com.earbrief.app.data.repository

import com.earbrief.app.data.local.db.dao.TriggerEventDao
import com.earbrief.app.data.local.db.entity.TriggerEventEntity
import com.earbrief.app.domain.model.Entity
import com.earbrief.app.domain.model.EntityType
import com.earbrief.app.domain.model.TriggerEvent
import com.earbrief.app.domain.model.TriggerType
import com.earbrief.app.domain.model.TriggerUrgency
import com.earbrief.app.domain.repository.TriggerEventRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

@Singleton
class RoomTriggerEventRepository @Inject constructor(
    private val triggerEventDao: TriggerEventDao,
    private val json: Json,
) : TriggerEventRepository {

    override suspend fun insert(event: TriggerEvent) {
        triggerEventDao.insert(event.toEntity())
    }

    override fun observeBySession(sessionId: String): Flow<List<TriggerEvent>> {
        return triggerEventDao.observeBySession(sessionId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getBySession(sessionId: String): List<TriggerEvent> {
        return triggerEventDao.getBySession(sessionId).map { it.toDomain() }
    }

    override suspend fun deleteOlderThan(timestampMs: Long) {
        triggerEventDao.deleteOlderThan(timestampMs)
    }

    private fun TriggerEvent.toEntity(): TriggerEventEntity {
        return TriggerEventEntity(
            id = id,
            sessionId = sessionId,
            triggerType = triggerType.name,
            sourceUtteranceId = sourceUtteranceId,
            sourceText = sourceText,
            detectedEntities = encodeEntities(detectedEntities),
            whisperText = whisperText,
            confidence = confidence,
            priority = priority,
            urgency = urgency.name,
            metadata = encodeMetadata(metadata),
            createdAtMs = createdAtMs,
        )
    }

    private fun TriggerEventEntity.toDomain(): TriggerEvent {
        return TriggerEvent(
            id = id,
            sessionId = sessionId,
            triggerType = TriggerType.valueOf(triggerType),
            sourceUtteranceId = sourceUtteranceId,
            sourceText = sourceText,
            detectedEntities = decodeEntities(detectedEntities),
            whisperText = whisperText,
            confidence = confidence,
            priority = priority,
            urgency = TriggerUrgency.valueOf(urgency),
            metadata = decodeMetadata(metadata),
            createdAtMs = createdAtMs,
        )
    }

    private fun encodeEntities(entities: List<Entity>): String {
        val jsonArray = buildJsonArray {
            entities.forEach { entity ->
                add(
                    buildJsonObject {
                        put("type", entity.type.name)
                        put("value", entity.value)
                        put("normalizedValue", entity.normalizedValue)
                        put("confidence", entity.confidence)
                        put("start", entity.position.first)
                        put("endInclusive", entity.position.last)
                        put("sourceUtteranceId", entity.sourceUtteranceId)
                    }
                )
            }
        }
        return jsonArray.toString()
    }

    private fun decodeEntities(encoded: String): List<Entity> {
        return runCatching {
            val array = json.parseToJsonElement(encoded).jsonArray
            array.mapNotNull { element ->
                val obj = element.jsonObject
                val type = obj["type"]?.jsonPrimitive?.contentOrNull?.let(EntityType::valueOf)
                    ?: return@mapNotNull null
                val value = obj["value"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val normalizedValue = obj["normalizedValue"]?.jsonPrimitive?.contentOrNull
                    ?: return@mapNotNull null
                val confidence = obj["confidence"]?.jsonPrimitive?.floatOrNull ?: return@mapNotNull null
                val start = obj["start"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
                val endInclusive = obj["endInclusive"]?.jsonPrimitive?.intOrNull
                    ?: return@mapNotNull null
                val sourceUtteranceId = obj["sourceUtteranceId"]?.jsonPrimitive?.contentOrNull
                    ?: return@mapNotNull null

                Entity(
                    type = type,
                    value = value,
                    normalizedValue = normalizedValue,
                    confidence = confidence,
                    position = IntRange(start, endInclusive),
                    sourceUtteranceId = sourceUtteranceId,
                )
            }
        }.getOrElse { emptyList() }
    }

    private fun encodeMetadata(metadata: Map<String, String>): String {
        val jsonObject = buildJsonObject {
            metadata.forEach { (key, value) ->
                put(key, value)
            }
        }
        return jsonObject.toString()
    }

    private fun decodeMetadata(encoded: String): Map<String, String> {
        return runCatching {
            val jsonObject = json.parseToJsonElement(encoded).jsonObject
            jsonObject.mapValues { (_, valueElement) ->
                valueElement.jsonPrimitive.content
            }
        }.getOrElse { emptyMap() }
    }
}
