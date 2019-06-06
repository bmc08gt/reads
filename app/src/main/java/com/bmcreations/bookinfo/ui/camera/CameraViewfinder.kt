package com.bmcreations.bookinfo.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Rational
import android.util.Size
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.bmcreations.bookinfo.R
import com.bmcreations.bookinfo.extensions.FLAGS_FULLSCREEN
import com.bmcreations.bookinfo.extensions.getViewModel
import com.bmcreations.bookinfo.extensions.isPermissionGranted
import com.bmcreations.bookinfo.network.Outcome
import kotlinx.android.synthetic.main.camera_viewfinder.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

class CameraViewfinder: AppCompatActivity(), LifecycleOwner, AnkoLogger {

    private var lensFacing = CameraX.LensFacing.BACK
    private var imageCapture: ImageCapture? = null

    private val vm by lazy {
        getViewModel { BookCaptureViewModel.create(this) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_viewfinder)

        up_nav.setOnClickListener { finish() }
        capture.setOnClickListener { vm.capture(imageCapture) }

        // Request camera permissions
        if (allPermissionsGranted()) {
            view_finder.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        observe()
    }

    private fun observe() {
        vm.results.observe(this, Observer {
            when (it) {
                is Outcome.Success -> info {
                    "kind=${it.data.kind}, count=${it.data.count}, results=${it.data.results.joinToString(separator = ",") { v -> v.volumeInfo?.title ?: "unknown" }}"
                }
                is Outcome.Failure -> info { it.e.localizedMessage }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        root.systemUiVisibility = FLAGS_FULLSCREEN
    }

    /** Declare and bind preview, capture and analysis use cases */
    private fun bindCameraUseCases() {

        // Make sure that there are no other use cases bound to CameraX
        CameraX.unbindAll()

        val metrics = DisplayMetrics().also { view_finder.display.getRealMetrics(it) }
        val screenSize = Size(metrics.widthPixels, metrics.heightPixels)
        val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)
        info { "Metrics: ${metrics.widthPixels} x ${metrics.heightPixels}" }

        // Set up the view finder use case to display camera preview
        val viewFinderConfig = PreviewConfig.Builder().apply {
            setLensFacing(lensFacing)
            // We request a specific resolution matching screen size
            setTargetResolution(screenSize)
            // We also provide an aspect ratio in case the exact resolution is not available
            setTargetAspectRatio(screenAspectRatio)
            setTargetRotation(view_finder.display.rotation)
        }.build()

        // Use the auto-fit preview builder to automatically handle size and orientation changes
        val preview = AutoFitPreviewBuilder.build(viewFinderConfig, view_finder)

        // Set up the capture use case to allow users to take photos
        val imageCaptureConfig = ImageCaptureConfig.Builder().apply {
            setLensFacing(lensFacing)
            setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
            // We request aspect ratio but no resolution to match preview config but letting
            // CameraX optimize for whatever specific resolution best fits requested capture mode
            setTargetAspectRatio(screenAspectRatio)
            setTargetRotation(view_finder.display.rotation)
        }.build()

        imageCapture = ImageCapture(imageCaptureConfig)

        // Apply declared configs to CameraX using the same lifecycle owner
        CameraX.bindToLifecycle(
            this, preview, imageCapture)
    }

    @SuppressLint("RestrictedApi")
    private fun startCamera() {
        try {
            // Only bind use cases if we can query a camera with this orientation
            CameraX.getCameraWithLensFacing(lensFacing)
            bindCameraUseCases()
        } catch (exc: Exception) {
            // Do nothing
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                view_finder.post { startCamera() }
            } else {
                toast("Permissions not granted by the user.")
                finish()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (!isPermissionGranted(permission)) {
                return false
            }
        }
        return true
    }

    companion object {
        const val REQUEST_CODE_PERMISSIONS = 1337
        val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}