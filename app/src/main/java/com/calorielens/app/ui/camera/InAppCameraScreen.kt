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
        val originalStatusColor = window?.statusBarColor
        val originalNavigationColor = window?.navigationBarColor
        val insets = window?.let { WindowInsetsControllerCompat(it, view) }
        val originalLightStatus = insets?.isAppearanceLightStatusBars
        val originalLightNavigation = insets?.isAppearanceLightNavigationBars
        val cameraBackground = Color(0xFFFFFCF7)
        if (window != null) {
            window.statusBarColor = cameraBackground.toArgb()
            window.navigationBarColor = cameraBackground.toArgb()
        }
        if (insets != null) {
            insets.isAppearanceLightStatusBars = true
            insets.isAppearanceLightNavigationBars = true
        }
        onDispose {
            if (window != null && originalStatusColor != null) window.statusBarColor = originalStatusColor
            if (window != null && originalNavigationColor != null) window.navigationBarColor = originalNavigationColor
            if (insets != null && originalLightStatus != null) {
                insets.isAppearanceLightStatusBars = originalLightStatus
            }
            if (insets != null && originalLightNavigation != null) {
                insets.isAppearanceLightNavigationBars = originalLightNavigation
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
        Modifier.fillMaxSize().background(Color(0xFFFFFCF7)).systemBarsPadding().padding(horizontal = 18.dp),
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
                modifier = Modifier.background(Color(0xFFF0F4F0), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "关闭相机", tint = Color(0xFF29362E))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🍽️ 拍下这一餐", color = Color(0xFF29362E), style = androidx.compose.material3.MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("把食物放进中间的取景画面", color = Color(0xFF6E7C73), style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.size(48.dp))
        }

        Spacer(Modifier.height(18.dp))
        Box(
            Modifier.weight(1f).fillMaxWidth()
                .clip(RoundedCornerShape(34.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFDCE6DE), RoundedCornerShape(34.dp))
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
        }
        Spacer(Modifier.height(22.dp))
        Text("拍歪了也没关系，下一步还可以裁剪", color = Color(0xFF78857C), style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier.size(82.dp).clip(CircleShape).background(Color(0xFFDCEBE1)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(66.dp).clip(CircleShape).background(Color.White)
                    .border(2.dp, Color(0xFF6E947A), CircleShape)
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
