package com.adriansoftware.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.*
import androidx.camera.core.impl.VideoCaptureConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executors

private const val tag = "RecordActivity"
private const val REQUEST_CODE_PERMISSIONS = 10

class MainActivity : AppCompatActivity(), CameraXConfig.Provider {
    private lateinit var camera: Camera
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var videoCapture: VideoCapture
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (!allPermissionsGranted()) {
            Log.v(tag, "Attempting to request permissions...")
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (!allPermissionsGranted()) {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun getCameraXConfig(): CameraXConfig {
        return Camera2Config
            .defaultConfig()
    }

    fun startCameraOne(view: View) {
        startCameraProvider(findViewById(R.id.preview1))
    }

    fun startCameraTwo(view: View) {
        startCameraProvider(findViewById(R.id.preview2))
    }


    fun startCameraProvider(previewView: PreviewView) {
        cameraProviderFuture = ProcessCameraProvider.getInstance(baseContext)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(previewView, cameraProvider)
        }, ContextCompat.getMainExecutor(baseContext))
    }

    @SuppressLint("RestrictedApi")
    private fun bindPreview(previewView: PreviewView, cameraProvider: ProcessCameraProvider) {
        CameraX.unbindAll()
        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()

        val preview: Preview = Preview.Builder()
            .setCameraSelector(cameraSelector)
            .build()
        videoCapture = VideoCaptureConfig.Builder().apply {
            setCameraSelector(cameraSelector)
        }.build()

        camera = cameraProvider.bindToLifecycle(this as LifecycleOwner,
            cameraSelector,
            videoCapture,
            preview)
        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
        preview.setSurfaceProvider((previewView as PreviewView).createSurfaceProvider(camera.cameraInfo))
    }
}
