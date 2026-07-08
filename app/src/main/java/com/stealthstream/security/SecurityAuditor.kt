package com.stealthstream.security

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.stealthstream.util.SecureLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Security auditor for runtime checks.
 */
interface SecurityAuditor {
    /**
     * Run security audit.
     */
    suspend fun audit(): SecurityAuditResult
}

/**
 * Security audit result.
 */
data class SecurityAuditResult(
    val passedChecks: Int,
    val failedChecks: Int,
    val issues: List<SecurityIssue>
)

/**
 * Individual security issue.
 */
data class SecurityIssue(
    val severity: SecuritySeverity,
    val category: String,
    val message: String
)

enum class SecuritySeverity {
    CRITICAL, HIGH, MEDIUM, LOW
}

/**
 * Implementation of security auditor.
 */
@Singleton
class SecurityAuditorImpl @Inject constructor(
    private val context: Context
) : SecurityAuditor {

    companion object {
        private const val TAG = "SecurityAuditor"
    }

    override suspend fun audit(): SecurityAuditResult {
        val issues = mutableListOf<SecurityIssue>()
        var passedChecks = 0
        var failedChecks = 0

        // Check 1: Required permissions
        if (hasRequiredPermissions()) {
            passedChecks++
        } else {
            failedChecks++
            issues.add(
                SecurityIssue(
                    severity = SecuritySeverity.CRITICAL,
                    category = "PERMISSIONS",
                    message = "Missing required permissions"
                )
            )
        }

        // Check 2: Debuggable flag
        if (!isDebugBuild()) {
            passedChecks++
        } else {
            failedChecks++
            issues.add(
                SecurityIssue(
                    severity = SecuritySeverity.HIGH,
                    category = "BUILD",
                    message = "App is debuggable in production"
                )
            )
        }

        // Check 3: Backup enabled
        val allowBackup = isBackupAllowed()
        if (!allowBackup) {
            passedChecks++
        } else {
            failedChecks++
            issues.add(
                SecurityIssue(
                    severity = SecuritySeverity.HIGH,
                    category = "BACKUP",
                    message = "Backup is allowed - keys may be exposed"
                )
            )
        }

        // Check 4: Verify APK signature
        if (verifySignature()) {
            passedChecks++
        } else {
            failedChecks++
            issues.add(
                SecurityIssue(
                    severity = SecuritySeverity.CRITICAL,
                    category = "SIGNATURE",
                    message = "APK signature verification failed"
                )
            )
        }

        SecureLogger.info(
            TAG,
            "Audit complete: $passedChecks passed, $failedChecks failed"
        )

        return SecurityAuditResult(
            passedChecks = passedChecks,
            failedChecks = failedChecks,
            issues = issues
        )
    }

    private fun hasRequiredPermissions(): Boolean {
        val requiredPermissions = listOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.INTERNET
        )

        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun isDebugBuild(): Boolean {
        return (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    private fun isBackupAllowed(): Boolean {
        return (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP) != 0
    }

    private fun verifySignature(): Boolean {
        return try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
            // Signature verification would go here
            // For now, just verify it's signed
            packageInfo.signatures != null && packageInfo.signatures.isNotEmpty()
        } catch (e: Exception) {
            SecureLogger.error(TAG, "Signature verification failed", e)
            false
        }
    }
}
