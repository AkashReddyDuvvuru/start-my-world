package com.stealthstream

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application entry point with Hilt initialization.
 */
@HiltAndroidApp
class StealthStreamApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupLogging()
    }

    private fun setupLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // Plant a non-verbose tree for production
            Timber.plant(ProductionTree())
        }
    }

    /**
     * Production tree that only logs errors.
     */
    private class ProductionTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // Only log errors in production
            if (priority >= android.util.Log.ERROR) {
                super.log(priority, tag, message, t)
            }
        }
    }
}
