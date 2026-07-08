package com.stealthstream.di

import com.stealthstream.data.audit.AuditRepositoryImpl
import com.stealthstream.data.camera.CameraRepositoryImpl
import com.stealthstream.data.network.NetworkRepositoryImpl
import com.stealthstream.domain.repository.AuditRepository
import com.stealthstream.domain.repository.CameraRepository
import com.stealthstream.domain.repository.NetworkRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for data layer repositories.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindCameraRepository(
        impl: CameraRepositoryImpl
    ): CameraRepository

    @Binds
    @Singleton
    abstract fun bindNetworkRepository(
        impl: NetworkRepositoryImpl
    ): NetworkRepository

    @Binds
    @Singleton
    abstract fun bindAuditRepository(
        impl: AuditRepositoryImpl
    ): AuditRepository
}
