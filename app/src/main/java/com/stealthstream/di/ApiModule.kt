package com.stealthstream.di

import com.stealthstream.api.EncryptionPipeline
import com.stealthstream.api.EncryptionPipelineImpl
import com.stealthstream.api.StreamingApi
import com.stealthstream.api.StreamingApiImpl
import com.stealthstream.domain.auth.AuthenticationProvider
import com.stealthstream.domain.auth.AuthenticationProviderImpl
import com.stealthstream.domain.auth.AuthorizationProvider
import com.stealthstream.domain.auth.AuthorizationProviderImpl
import com.stealthstream.domain.coordinator.StreamingCoordinator
import com.stealthstream.domain.coordinator.StreamingCoordinatorImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for API and service layer.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ApiModule {

    @Binds
    @Singleton
    abstract fun bindStreamingApi(
        impl: StreamingApiImpl
    ): StreamingApi

    @Binds
    @Singleton
    abstract fun bindEncryptionPipeline(
        impl: EncryptionPipelineImpl
    ): EncryptionPipeline

    @Binds
    @Singleton
    abstract fun bindStreamingCoordinator(
        impl: StreamingCoordinatorImpl
    ): StreamingCoordinator

    @Binds
    @Singleton
    abstract fun bindAuthenticationProvider(
        impl: AuthenticationProviderImpl
    ): AuthenticationProvider

    @Binds
    @Singleton
    abstract fun bindAuthorizationProvider(
        impl: AuthorizationProviderImpl
    ): AuthorizationProvider
}
