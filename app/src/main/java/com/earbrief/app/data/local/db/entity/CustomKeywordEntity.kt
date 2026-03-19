package com.earbrief.app.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_keywords")
data class CustomKeywordEntity(
    @PrimaryKey val id: String,
    val keyword: String,
    val pattern: String?,
    @ColumnInfo(name = "action_type") val actionType: String,
    @ColumnInfo(name = "response_template") val responseTemplate: String?,
    @ColumnInfo(name = "is_active") val isActive: Boolean,
    @ColumnInfo(name = "trigger_count") val triggerCount: Int,
    @ColumnInfo(name = "last_triggered_ms") val lastTriggeredMs: Long?,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long
)
