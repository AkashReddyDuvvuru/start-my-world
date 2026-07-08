package com.stealthstream.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.stealthstream.R
import com.stealthstream.databinding.ActivityMainBinding
import com.stealthstream.domain.model.StreamingConfig
import com.stealthstream.domain.model.StreamStatus
import com.stealthstream.util.FormatUtil
import com.stealthstream.util.PermissionHelper
import com.stealthstream.util.SecureLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Main activity for user interaction.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var permissionHelper: PermissionHelper

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            SecureLogger.info("MainActivity", "Camera permission granted")
            viewModel.onPermissionGranted(Manifest.permission.CAMERA)
            startStreaming()
        } else {
            SecureLogger.info("MainActivity", "Camera permission denied")
            viewModel.onPermissionDenied(Manifest.permission.CAMERA)
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionHelper = PermissionHelper(this)
        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        binding.startButton.setOnClickListener {
            onStartStreamingClicked()
        }

        binding.stopButton.setOnClickListener {
            onStopStreamingClicked()
        }

        binding.stopButton.isEnabled = false
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.streamingStatus.collect { status ->
                    updateUI(status)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.streamingStats.collect { stats ->
                    updateStats(stats)
                }
            }
        }
    }

    private fun onStartStreamingClicked() {
        // Check camera permission
        if (!permissionHelper.isPermissionGranted(Manifest.permission.CAMERA)) {
            if (permissionHelper.shouldShowRationale(this, Manifest.permission.CAMERA)) {
                showPermissionRationale()
            } else {
                requestCameraPermission()
            }
        } else {
            startStreaming()
        }
    }

    private fun onStopStreamingClicked() {
        viewModel.stopStreaming()
    }

    private fun startStreaming() {
        val config = StreamingConfig(
            targetIp = binding.targetIpInput.text.toString().ifEmpty { "203.0.113.42" },
            targetPort = binding.targetPortInput.text.toString().toIntOrNull() ?: 40001,
            targetBitrateMbps = 0.5f,
            frameRateTarget = 30
        )

        viewModel.startStreaming(config)
    }

    private fun requestCameraPermission() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun showPermissionRationale() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_camera_title)
            .setMessage(R.string.permission_camera_rationale)
            .setPositiveButton(R.string.permission_grant) { _, _ ->
                requestCameraPermission()
            }
            .setNegativeButton(R.string.permission_cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_camera_title)
            .setMessage(R.string.permission_denied)
            .setPositiveButton(R.string.permission_grant) { _, _ ->
                requestCameraPermission()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun updateUI(status: StreamStatus) {
        binding.statusText.text = when (status) {
            StreamStatus.IDLE -> getString(R.string.status_idle)
            StreamStatus.INITIALIZING -> getString(R.string.status_idle) + " (init)"
            StreamStatus.STREAMING -> getString(R.string.status_streaming)
            StreamStatus.PAUSED -> "Paused"
            StreamStatus.STOPPING -> "Stopping"
            StreamStatus.ERROR -> getString(R.string.status_error)
            StreamStatus.STOPPED -> "Stopped"
        }

        val isStreaming = status == StreamStatus.STREAMING
        binding.startButton.isEnabled = !isStreaming
        binding.stopButton.isEnabled = isStreaming

        val statusColor = if (isStreaming) {
            ContextCompat.getColor(this, R.color.success_color)
        } else {
            ContextCompat.getColor(this, R.color.text_primary)
        }
        binding.statusText.setTextColor(statusColor)
    }

    private fun updateStats(stats: com.stealthstream.api.StreamingStats) {
        binding.fpsText.text = String.format(
            getString(R.string.stat_fps),
            stats.currentFps
        )
        binding.bytesText.text = String.format(
            getString(R.string.stat_bytes_sent),
            FormatUtil.formatBytes(stats.bytesSent)
        )
        binding.lossText.text = String.format(
            getString(R.string.stat_packet_loss),
            stats.packetLossPercent * 100
        )
        binding.uptimeText.text = String.format(
            getString(R.string.stat_uptime),
            FormatUtil.formatDuration(stats.uptimeSeconds * 1000)
        )
    }
}
