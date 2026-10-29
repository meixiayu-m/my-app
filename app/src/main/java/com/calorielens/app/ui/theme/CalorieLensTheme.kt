package com.calorielens.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.calorielens.app.data.AppAppearance
import com.calorielens.app.data.AppTheme
import com.calorielens.app.data.AppWallpaper

@Composable
fun CalorieLensTheme(appearance: AppAppearance, content: @Composable () -> Unit) {
    val dark = appearance.theme == AppTheme.NIGHT
    val base = if (dark) {
        darkColorScheme(
            primary = Color(0xFFA8D9BD),
            secondary = Color(0xFFFFB39E),
            tertiary = Color(0xFFFFD47A),
            background = Color(0xFF171A18),
            surface = Color(0xFF202522),
            surfaceVariant = Color(0xFF303832)
        )
    } else {
        val primary = when (appearance.theme) {
            AppTheme.PEACH -> Color(0xFFB86D58)
            AppTheme.LAVENDER -> Color(0xFF7766A8)
            else -> Color(0xFF5C8D70)
        }
        val secondary = when (appearance.theme) {
            AppTheme.LAVENDER -> Color(0xFFE5A7D0)
            AppTheme.PEACH -> Color(0xFFE69A83)
            else -> Color(0xFFE69A83)
        }
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            secondary = secondary,
            tertiary = Color(0xFFF2C66D),
            background = when (appearance.wallpaper) {
                AppWallpaper.GREEN -> Color(0xFFF3FAF3)
                AppWallpaper.PINK -> Color(0xFFFFF5F1)
                AppWallpaper.BLUE -> Color(0xFFF3F7FB)
                AppWallpaper.CREAM -> Color(0xFFFFFBF5)
            },
            surface = Color.White,
            surfaceVariant = Color(0xFFF2F4EC)
        )
    }
    val scheme = if (appearance.glass) base.copy(
        surface = base.surface.copy(alpha = if (dark) 0.84f else 0.76f),
        surfaceVariant = base.surfaceVariant.copy(alpha = if (dark) 0.78f else 0.70f)
    ) else base

    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val window = (view.context as? android.app.Activity)?.window
    DisposableEffect(lifecycleOwner, window, view, dark, scheme.background) {
        val originalStatusBarColor = window?.statusBarColor
        val originalNavigationBarColor = window?.navigationBarColor
        val controller = window?.let { WindowInsetsControllerCompat(it, view) }
        val originalLightStatusBars = controller?.isAppearanceLightStatusBars
        val originalLightNavigationBars = controller?.isAppearanceLightNavigationBars

        fun applyTheme() {
            window ?: return
            WindowCompat.setDecorFitsSystemWindows(window, true)
            window.statusBarColor = scheme.background.toArgb()
            window.navigationBarColor = scheme.background.toArgb()
            controller?.let {
                it.isAppearanceLightStatusBars = !dark
                it.isAppearanceLightNavigationBars = !dark
            }
        }

        fun restoreSystemBars() {
            if (window != null && originalStatusBarColor != null) window.statusBarColor = originalStatusBarColor
            if (window != null && originalNavigationBarColor != null) window.navigationBarColor = originalNavigationBarColor
            originalLightStatusBars?.let { value -> controller?.let { it.isAppearanceLightStatusBars = value } }
            originalLightNavigationBars?.let { value -> controller?.let { it.isAppearanceLightNavigationBars = value } }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> applyTheme()
                Lifecycle.Event.ON_STOP -> restoreSystemBars()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        applyTheme()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            restoreSystemBars()
        }
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
