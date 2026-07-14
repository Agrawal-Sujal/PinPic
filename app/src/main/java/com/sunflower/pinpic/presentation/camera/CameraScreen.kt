package com.sunflower.pinpic.presentation.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaActionSound
import android.os.Environment
import android.util.Log
import android.util.Size
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.video.AudioConfig
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import coil.transform.RoundedCornersTransformation
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor

@Composable
fun CameraScreen(
    onClose: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Check permissions initially
    var hasPermissions by remember {
        mutableStateOf(
            hasRequiredPermissions(context)
        )
    }
    val shutterSound = remember { MediaActionSound() }

    DisposableEffect(Unit) {
        shutterSound.load(MediaActionSound.SHUTTER_CLICK)
        onDispose { shutterSound.release() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.entries.all { it.value }
    }

    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val latestPhotoUri by viewModel.latestPhotoUri.collectAsStateWithLifecycle()


    LaunchedEffect(Unit) {
        if (hasPermissions) viewModel.startLocationUpdates()
    }

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    if (!hasPermissions) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Permissions not granted")
        }
        return
    }

    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(
                LifecycleCameraController.IMAGE_CAPTURE or
                        LifecycleCameraController.VIDEO_CAPTURE
            )
            imageCaptureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
            imageCaptureFlashMode = ImageCapture.FLASH_MODE_OFF
        }
    }

    var isRecording by remember { mutableStateOf(false) }
    var recording: Recording? by remember { mutableStateOf(null) }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    controller = cameraController
                    cameraController.bindToLifecycle(lifecycleOwner)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Close Button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
        }

        // Camera Controls
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Switch Camera
            IconButton(
                onClick = {
                    cameraController.cameraSelector =
                        if (cameraController.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    Icons.Filled.Cameraswitch,
                    contentDescription = "Switch Camera",
                    tint = Color.White
                )
            }

            // Zoom In
            IconButton(
                onClick = {
                    val currentZoom = cameraController.zoomState.value?.zoomRatio ?: 1f
                    cameraController.setZoomRatio(currentZoom + 0.5f)
                },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom In", tint = Color.White)
            }

            // Zoom Out
            IconButton(
                onClick = {
                    val currentZoom = cameraController.zoomState.value?.zoomRatio ?: 1f
                    cameraController.setZoomRatio((currentZoom - 0.5f).coerceAtLeast(1f))
                },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom Out", tint = Color.White)
            }
        }

        // Bottom Controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            if (latestPhotoUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(latestPhotoUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Latest Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                )
            } else {
                Spacer(modifier = Modifier.size(56.dp))
            }

            // Photo Button
            FloatingActionButton(
                onClick = {
                    if (viewModel.startSaving()) {
                        takePhoto(
                            context = context,
                            controller = cameraController,
                            executor = mainExecutor,
                            onPhotoSaved = { uri ->
                                viewModel.onPhotoCaptured(uri)
                            },
                            onError = {
                                viewModel.stopSaving()
                            }
                        )
                        shutterSound.play(MediaActionSound.SHUTTER_CLICK)
                    }
                },
                containerColor = if (isSaving) Color.Gray else Color.White,
                contentColor = Color.Black
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = "Take Photo")
                }
            }

            // Video Button
            FloatingActionButton(
                onClick = {
                    if (isRecording) {
                        recording?.stop()
                    } else {
                        recording = recordVideo(context, cameraController, mainExecutor) { event ->
                            when (event) {
                                is VideoRecordEvent.Start -> {
                                    isRecording = true
                                }

                                is VideoRecordEvent.Finalize -> {
                                    isRecording = false
                                    if (!event.hasError()) {
                                        Toast.makeText(
                                            context,
                                            "Video saved successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Video error: ${event.error}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                    }
                },
                containerColor = if (isRecording) Color.Red else Color.White,
                contentColor = if (isRecording) Color.White else Color.Black
            ) {
                Icon(Icons.Filled.Videocam, contentDescription = "Record Video")
            }
        }
    }
}

private fun hasRequiredPermissions(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
}

private fun takePhoto(
    context: Context,
    controller: LifecycleCameraController,
    executor: Executor,
    onPhotoSaved: (String) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val photoFile = File(
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
        "PinPic-${
            SimpleDateFormat(
                "yyyy-MM-dd-HH-mm-ss-SSS",
                Locale.US
            ).format(System.currentTimeMillis())
        }.jpg"
    )
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    controller.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                Toast.makeText(context, "Photo saved successfully", Toast.LENGTH_SHORT).show()
                onPhotoSaved(photoFile.absolutePath)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraScreen", "Photo capture failed: ${exception.message}", exception)
                Toast.makeText(context, "Photo capture failed", Toast.LENGTH_SHORT).show()
                onError(exception)
            }
        }
    )
}

@SuppressLint("MissingPermission")
private fun recordVideo(
    context: Context,
    controller: LifecycleCameraController,
    executor: Executor,
    onEvent: (VideoRecordEvent) -> Unit
): Recording {
    val videoFile = File(
        context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
        "PinPic-${
            SimpleDateFormat(
                "yyyy-MM-dd-HH-mm-ss-SSS",
                Locale.US
            ).format(System.currentTimeMillis())
        }.mp4"
    )
    val outputOptions = FileOutputOptions.Builder(videoFile).build()

    return controller.startRecording(
        outputOptions,
        AudioConfig.create(true),
        executor
    ) { event -> onEvent(event) }
}
