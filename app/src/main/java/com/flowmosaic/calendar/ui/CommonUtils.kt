package com.flowmosaic.calendar.ui

import android.graphics.Color
import com.flowmosaic.calendar.R
import kotlin.collections.listOf

fun getCalendarIcons(): List<Int> {
    return listOf(
        R.drawable.close,
        R.drawable.airplane,
        R.drawable.alarm,
        R.drawable.bank,
        R.drawable.baseball,
        R.drawable.cake,
        R.drawable.calendar,
        R.drawable.car,
        R.drawable.cycling,
        R.drawable.football,
        R.drawable.gaming,
        R.drawable.gavel,
        R.drawable.holiday,
        R.drawable.messages,
        R.drawable.music,
        R.drawable.pram,
        R.drawable.present,
        R.drawable.soccer,
        R.drawable.suitcase,
        R.drawable.ticket,
        R.drawable.train,
        R.drawable.university,
        R.drawable.wallet,
        R.drawable.weather,
        R.drawable.weights,
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
    return luminanceLimit > 0.5
}
