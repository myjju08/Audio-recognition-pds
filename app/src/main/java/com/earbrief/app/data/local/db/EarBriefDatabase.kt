package com.earbrief.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.earbrief.app.data.local.db.dao.CustomKeywordDao
import com.earbrief.app.data.local.db.dao.SessionDao
import com.earbrief.app.data.local.db.dao.TriggerEventDao
import com.earbrief.app.data.local.db.entity.CustomKeywordEntity
import com.earbrief.app.data.local.db.entity.SessionEntity
import com.earbrief.app.data.local.db.entity.TriggerEventEntity

@Database(
    entities = [
        TriggerEventEntity::class,
        CustomKeywordEntity::class,
        SessionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class EarBriefDatabase : RoomDatabase() {
    abstract fun triggerEventDao(): TriggerEventDao
    abstract fun customKeywordDao(): CustomKeywordDao
    abstract fun sessionDao(): SessionDao
}
