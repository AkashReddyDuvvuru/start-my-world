package com.stealthstream.logging

import android.util.Log

/**
 * Metrics collector for observability.
 */
interface MetricsCollector {
    /**
     * Record a counter metric.
     */
    fun recordCounter(name: String, value: Long)

    /**
     * Record a gauge metric.
     */
    fun recordGauge(name: String, value: Double)

    /**
     * Record a histogram/timing metric.
     */
    fun recordHistogram(name: String, value: Long)

    /**
     * Get all metrics.
     */
    fun getMetrics(): Map<String, List<Long>>
}

/**
 * Implementation of metrics collector.
 */
class MetricsCollectorImpl : MetricsCollector {

    private val metrics = mutableMapOf<String, MutableList<Long>>()
    private val maxMetricsPerName = 500

    companion object {
        private const val TAG = "MetricsCollector"
    }

    override fun recordCounter(name: String, value: Long) {
        val metricName = "counter.$name"
        metrics.getOrPut(metricName) { mutableListOf() }.apply {
            add(value)
            if (size > maxMetricsPerName) removeAt(0)
        }
        Log.d(TAG, "Counter $name: $value")
    }

    override fun recordGauge(name: String, value: Double) {
        val metricName = "gauge.$name"
        metrics.getOrPut(metricName) { mutableListOf() }.apply {
            add(value.toLong())
            if (size > maxMetricsPerName) removeAt(0)
        }
        Log.d(TAG, "Gauge $name: $value")
    }

    override fun recordHistogram(name: String, value: Long) {
        val metricName = "histogram.$name"
        metrics.getOrPut(metricName) { mutableListOf() }.apply {
            add(value)
            if (size > maxMetricsPerName) removeAt(0)
        }
        Log.d(TAG, "Histogram $name: $value")
    }

    override fun getMetrics(): Map<String, List<Long>> = metrics.mapValues { it.value.toList() }
}
