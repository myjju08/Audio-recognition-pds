package com.earbrief.app.di

import com.earbrief.app.service.DefaultSessionManager
import com.earbrief.app.service.SessionManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {
    @Binds
    @Singleton
    abstract fun bindSessionManager(impl: DefaultSessionManager): SessionManager
}
