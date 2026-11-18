package com.kfasa.aicamera

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var statusText: TextView
    private lateinit var zoomLabel: TextView
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var frontCamera = false
    private var flashEnabled = false
    private var macroMode = false

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else statusText.text = "Нужно разрешение на камеру"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        previewView = findViewById(R.id.previewView)
        statusText = findViewById(R.id.statusText)
        zoomLabel = findViewById(R.id.zoomLabel)
        cameraExecutor = Executors.newSingleThreadExecutor()

        findViewById<Button>(R.id.captureButton).setOnClickListener { takePhoto() }
        findViewById<Button>(R.id.flipButton).setOnClickListener { frontCamera = !frontCamera; startCamera() }
        findViewById<Button>(R.id.flashButton).setOnClickListener {
            flashEnabled = !flashEnabled
            imageCapture?.flashMode = if (flashEnabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
        }
        findViewById<Button>(R.id.macroButton).setOnClickListener {
            macroMode = !macroMode
            statusText.text = if (macroMode) "Макро: автофокус и проверка резкости" else "Обычный режим"
        }
        findViewById<Button>(R.id.galleryButton).setOnClickListener {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { startActivity(intent) }.onFailure { Toast.makeText(this, "Галерея не найдена", Toast.LENGTH_SHORT).show() }
        }
        findViewById<SeekBar>(R.id.zoomSeek).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val state = camera?.cameraInfo?.zoomState?.value ?: return
                val ratio = state.minZoomRatio + (state.maxZoomRatio - state.minZoomRatio) * (progress / 100f)
                camera?.cameraControl?.setZoomRatio(ratio)
                zoomLabel.text = String.format(Locale.US, "%.1f×", ratio)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera()
        else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val selector = if (frontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()
            try {
                provider.unbindAll()
                camera = provider.bindToLifecycle(this, selector, preview, imageCapture)
                statusText.text = if (macroMode) "Макро: приблизь объект и дождись фокуса" else "Готово к съёмке"
            } catch (e: Exception) { statusText.text = "Не удалось запустить камеру: ${e.localizedMessage}" }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val capture = imageCapture ?: return
        val name = "AICAM_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AI Camera")
        }
        val options = ImageCapture.OutputFileOptions.Builder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values).build()
        statusText.text = "Сохраняем оригинал…"
        capture.takePicture(options, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                runOnUiThread { statusText.text = "Оригинал сохранён. Открой миниатюру в галерее для AI-обработки (следующий этап)." }
            }
            override fun onError(exception: ImageCaptureException) {
                runOnUiThread { statusText.text = "Ошибка съёмки: ${exception.message}" }
            }
        })
    }

    override fun onDestroy() { super.onDestroy(); cameraExecutor.shutdown() }
}
