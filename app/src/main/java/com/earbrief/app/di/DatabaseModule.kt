package com.earbrief.app.di

import android.content.Context
import androidx.room.Room
import com.earbrief.app.data.local.db.EarBriefDatabase
import com.earbrief.app.data.local.db.dao.CustomKeywordDao
import com.earbrief.app.data.local.db.dao.SessionDao
import com.earbrief.app.data.local.db.dao.TriggerEventDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): EarBriefDatabase {
        return Room.databaseBuilder(
            context,
            EarBriefDatabase::class.java,
            "earbrief.db",
        ).build()
    }

    @Provides
    fun provideTriggerEventDao(db: EarBriefDatabase): TriggerEventDao = db.triggerEventDao()

    @Provides
    fun provideCustomKeywordDao(db: EarBriefDatabase): CustomKeywordDao = db.customKeywordDao()

    @Provides
    fun provideSessionDao(db: EarBriefDatabase): SessionDao = db.sessionDao()
}
