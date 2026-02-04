package com.flowmosaic.calendar.ui

import android.graphics.Color

fun getCommonEmoji(): List<String> {
    return listOf(
        "📅", "🎂", "🎉", "✈️", "🏃", "💼", "🎵", "🏈", "⚽", "🎮",
        "⏰", "🏠", "💰", "📧", "🎓", "🚗", "🚂", "🎁", "👶", "💪",
        "⚖️", "🌴", "💬", "🎟️", "☀️", "🏦", "⚾", "🚴", "❤️"
    )
}

fun isColorLight(color: Int): Boolean {
    return isColorLight(color, 0.5)
}

fun isColorLight(color: Int, luminanceLimit: Double): Boolean {
    val red = Color.red(color) / 255.0
    val green = Color.green(color) / 255.0
    val blue = Color.blue(color) / 255.0

    val luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue
    return luminance > luminanceLimit
}
