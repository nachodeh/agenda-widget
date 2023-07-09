package com.flowmosaic.calendar.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.flowmosaic.calendar.data.CalendarData

object AgendaWidgetPrefs {

    private const val PREF_SELECTED_CALENDARS = "selected_calendars"
    private const val PREF_SHOW_END_TIME = "key_show_end_time"
    private const val PREF_NUMBER_OF_DAYS = "key_number_of_days"
    private const val PREF_TEXT_COLOR = "key_text_color"

    fun getSelectedCalendars(
        context: Context,
        allCalendars: List<CalendarData>?
    ): MutableSet<String> {
        val selectedCalendars = getPreferences(context).getStringSet(PREF_SELECTED_CALENDARS, null)

        return selectedCalendars?.toMutableSet()
            ?: if (allCalendars == null) {
                mutableSetOf()
            } else {
                // Selected calendars not found in preferences, save allCalendars as selected
                val allCalendarIds = allCalendars.map { it.id.toString() }.toMutableSet()
                setSelectedCalendars(context, allCalendarIds)
                allCalendarIds
            }
    }

    fun setSelectedCalendars(context: Context, selectedCalendarIds: Set<String>) {
        getPreferences(context).edit()
            .putStringSet(PREF_SELECTED_CALENDARS, selectedCalendarIds)
            .apply()
    }

    fun getShowEndTime(context: Context): Boolean {
        return getPreferences(context).getBoolean(PREF_SHOW_END_TIME, false)
    }

    fun setShowEndTime(context: Context, showEndTime: Boolean) {
        getPreferences(context).edit().putBoolean(PREF_SHOW_END_TIME, showEndTime).apply()
    }

    fun getNumberOfDays(context: Context): Int {
        return getPreferences(context).getInt(PREF_NUMBER_OF_DAYS, 7)
    }

    fun setNumberOfDays(context: Context, numberOfDays: Int) {
        getPreferences(context).edit().putInt(PREF_NUMBER_OF_DAYS, numberOfDays).apply()
    }

    fun getTextColor(context: Context): Color {
        val defaultColor = Color.White.toArgb()
        val colorInt = getPreferences(context).getInt(PREF_TEXT_COLOR, defaultColor)
        return Color(colorInt)
    }

    fun setTextColor(context: Context, textColor: Color) {
        val colorInt = textColor.toArgb()
        getPreferences(context).edit().putInt(PREF_TEXT_COLOR, colorInt).apply()
    }


    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
    }

}