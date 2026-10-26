package com.calorielens.app.data

import android.content.Context

enum class AppTheme(val label: String) {
    MINT("薄荷清新"), PEACH("蜜桃暖色"), LAVENDER("薰衣草"), NIGHT("夜间深色")
}

enum class AppWallpaper(val label: String) {
    CREAM("奶油纸张"), GREEN("薄荷纸张"), PINK("桃粉纸张"), BLUE("蓝灰纸张")
}

data class AppAppearance(val theme: AppTheme = AppTheme.MINT, val wallpaper: AppWallpaper = AppWallpaper.CREAM)

object AppearanceStore {
    private const val PREFS = "private_settings"
    private const val THEME = "appearance_theme"
    private const val WALLPAPER = "appearance_wallpaper"

    fun read(context: Context): AppAppearance {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val theme = runCatching { AppTheme.valueOf(prefs.getString(THEME, null).orEmpty()) }.getOrDefault(AppTheme.MINT)
        val wallpaper = runCatching { AppWallpaper.valueOf(prefs.getString(WALLPAPER, null).orEmpty()) }.getOrDefault(AppWallpaper.CREAM)
        return AppAppearance(theme, wallpaper)
    }

    fun save(context: Context, appearance: AppAppearance) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(THEME, appearance.theme.name)
            .putString(WALLPAPER, appearance.wallpaper.name)
            .apply()
    }
}
