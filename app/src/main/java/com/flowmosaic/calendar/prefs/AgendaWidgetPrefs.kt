package com.flowmosaic.calendar.prefs

import android.content.Context
import android.content.SharedPreferences
import com.flowmosaic.calendar.data.CalendarData

object AgendaWidgetPrefs {

    private const val PREF_SELECTED_CALENDARS = "selected_calendars"
    const val PREF_SHOW_END_TIME = "key_show_end_time"

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

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
    }

}