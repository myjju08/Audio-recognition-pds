package com.earbrief.app.di

import com.earbrief.app.data.repository.RoomCustomKeywordRepository
import com.earbrief.app.data.repository.RoomSessionRepository
import com.earbrief.app.data.repository.RoomTriggerEventRepository
import com.earbrief.app.domain.repository.CustomKeywordRepository
import com.earbrief.app.domain.repository.SessionRepository
import com.earbrief.app.domain.repository.TriggerEventRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTriggerEventRepository(
        impl: RoomTriggerEventRepository
    ): TriggerEventRepository

    @Binds
    @Singleton
    abstract fun bindCustomKeywordRepository(
        impl: RoomCustomKeywordRepository
    ): CustomKeywordRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(
        impl: RoomSessionRepository
    ): SessionRepository
}
