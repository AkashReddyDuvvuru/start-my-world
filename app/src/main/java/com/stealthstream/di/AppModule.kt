package com.stealthstream.di

import android.content.Context
import com.stealthstream.data.crypto.CryptoRepositoryImpl
import com.stealthstream.domain.repository.AuditRepository
import com.stealthstream.domain.repository.CameraRepository
import com.stealthstream.domain.repository.CryptoRepository
import com.stealthstream.domain.repository.NetworkRepository
import com.stealthstream.domain.usecase.PermissionUseCase
import com.stealthstream.domain.usecase.PermissionUseCaseImpl
import com.stealthstream.domain.usecase.StreamingUseCase
import com.stealthstream.domain.usecase.StreamingUseCaseImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for repositories.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideCryptoRepository(): CryptoRepository {
        return CryptoRepositoryImpl()
    }

    // CameraRepository and NetworkRepository will be provided
    // in a separate module once implementation is complete
}

/**
 * Dependency injection module for use cases.
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun providePermissionUseCase(
        @ApplicationContext context: Context
    ): PermissionUseCase {
        return PermissionUseCaseImpl(context)
    }

    @Provides
    @Singleton
    fun provideStreamingUseCase(
        cameraRepository: CameraRepository,
        cryptoRepository: CryptoRepository,
        networkRepository: NetworkRepository,
        auditRepository: AuditRepository
    ): StreamingUseCase {
        return StreamingUseCaseImpl(
            cameraRepository,
            cryptoRepository,
            networkRepository,
            auditRepository
        )
    }
}
