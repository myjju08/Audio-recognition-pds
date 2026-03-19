package com.earbrief.app.di

import com.earbrief.app.data.calendar.ContentResolverCalendarRepository
import com.earbrief.app.domain.repository.CalendarRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CalendarModule {

    @Binds
    @Singleton
    abstract fun bindCalendarRepository(impl: ContentResolverCalendarRepository): CalendarRepository
}
