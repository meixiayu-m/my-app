package com.calorielens.app.ui.camera

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

@Composable
fun InAppCameraScreen(
    onClose: () -> Unit,
    onCaptured: (Uri) -> Unit,
    onError: (String) -> Unit
) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val controller = remember { LifecycleCameraController(context) }
    var capturing by remember { mutableStateOf(false) }

    LaunchedEffect(lifecycleOwner) { controller.bindToLifecycle(lifecycleOwner) }
    DisposableEffect(controller) { onDispose { controller.unbind() } }
    DisposableEffect(view) {
        val window = (view.context as? android.app.Activity)?.window
        val originalColor = window?.statusBarColor
        val insets = window?.let { WindowInsetsControllerCompat(it, view) }
        val originalLightStatus = insets?.isAppearanceLightStatusBars
        if (window != null) window.statusBarColor = Color.Black.toArgb()
        if (insets != null) insets.isAppearanceLightStatusBars = false
        onDispose {
            if (window != null && originalColor != null) window.statusBarColor = originalColor
            if (insets != null && originalLightStatus != null) {
                insets.isAppearanceLightStatusBars = originalLightStatus
            }
        }
    }

    fun capture() {
        if (capturing) return
        capturing = true
        val directory = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File.createTempFile("camera_", ".jpg", directory)
        val output = ImageCapture.OutputFileOptions.Builder(file).build()
        controller.takePicture(
            output,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    capturing = false
                    val uri = FileProvider.getUriForFile(
                        context,
                        "com.calorielens.app.fileprovider",
                        file
                    )
                    onCaptured(uri)
                }

                override fun onError(exception: ImageCaptureException) {
                    capturing = false
                    onError("拍照失败：${exception.message ?: "请重新拍摄"}")
                }
            }
        )
    }

    Column(
        Modifier.fillMaxSize().background(Color(0xFF111713)).systemBarsPadding().padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onClose,
                enabled = !capturing,
                modifier = Modifier.background(Color.White.copy(alpha = 0.14f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "关闭相机", tint = Color.White)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🍽️ 拍下这一餐", color = Color.White, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("尽量让食物填满中间的相框", color = Color.White.copy(alpha = 0.68f), style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.size(48.dp))
        }

        Spacer(Modifier.height(18.dp))
        Box(
            Modifier.weight(1f).fillMaxWidth()
                .clip(RoundedCornerShape(34.dp))
                .background(Color.White)
                .border(3.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(34.dp))
                .padding(8.dp)
        ) {
            AndroidView(
                factory = { previewContext ->
                    PreviewView(previewContext).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        this.controller = controller
                    }
                },
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(27.dp))
            )
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.42f)
            ) {
                Text(
                    "白色相框就是取景范围",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        Text("拍歪了也没关系，下一步还可以裁剪", color = Color.White.copy(alpha = 0.58f), style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier.size(82.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(66.dp).clip(CircleShape).background(Color.White)
                    .clickable(enabled = !capturing) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    capture()
                },
                contentAlignment = Alignment.Center
            ) {
                if (capturing) {
                    CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 3.dp, color = Color(0xFF4F7F61))
                } else {
                    Text("📷", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
