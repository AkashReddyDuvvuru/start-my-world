package com.stealthstream.di

import com.stealthstream.logging.AuditLogger
import com.stealthstream.logging.AuditLoggerImpl
import com.stealthstream.logging.MetricsCollector
import com.stealthstream.logging.MetricsCollectorImpl
import com.stealthstream.security.SecurityAuditor
import com.stealthstream.security.SecurityAuditorImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for security and logging.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityLoggingModule {

    @Binds
    @Singleton
    abstract fun bindSecurityAuditor(
        impl: SecurityAuditorImpl
    ): SecurityAuditor

    @Binds
    @Singleton
    abstract fun bindAuditLogger(
        impl: AuditLoggerImpl
    ): AuditLogger

    companion object {
        @Provides
        @Singleton
        fun provideMetricsCollector(): MetricsCollector {
            return MetricsCollectorImpl()
        }
    }
}
