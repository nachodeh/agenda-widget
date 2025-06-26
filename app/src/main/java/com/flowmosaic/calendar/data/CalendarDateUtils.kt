package com.flowmosaic.calendar.data

import android.content.Context
import com.flowmosaic.calendar.R
import com.flowmosaic.calendar.prefs.AgendaWidgetPrefs
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object CalendarDateUtils {

    fun getFormattedDate(context: Context, startTimeInMillis: Long): String {
        val eventCalendar = Calendar.getInstance().apply { timeInMillis = startTimeInMillis }
        val currentDate = Calendar.getInstance()
        val tomorrowDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val weekLaterDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 6) }

        return when {
            isSameDay(eventCalendar, currentDate) -> context.getString(R.string.today)
            isSameDay(eventCalendar, tomorrowDate) -> context.getString(R.string.tomorrow)
            eventCalendar.after(weekLaterDate) -> {
                val sdf = SimpleDateFormat("EE, d MMM", Locale.getDefault())
                sdf.format(eventCalendar.time)
            }

            else -> {
                val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
                sdf.format(eventCalendar.time)
            }
        }.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    fun getCalendarEventText(
        calendarEvent: CalendarEvent,
        context: Context,
        widgetId: String,
        locationAllowed: Boolean,
    ): String {
        var text = if (calendarEvent.isAllDay) {
            calendarEvent.title
        } else {
            val prefs = AgendaWidgetPrefs(context)
            val formattedTimeRange =
                getFormattedTime(
                    calendarEvent,
                    prefs.getShowEndTime(widgetId),
                    prefs.getHourFormat12(widgetId)
                )
            "$formattedTimeRange | ${calendarEvent.title}"
        }

        if (locationAllowed && !calendarEvent.location.isNullOrBlank()) {
            text += " @ ${calendarEvent.location}"
        }
        return text
    }

    fun getDateFromTimestamp(timestamp: Long): Date {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    private fun getFormattedTime(
        calendarEvent: CalendarEvent,
        showEndTime: Boolean,
        use12HourFormat: Boolean
    ): String {
        val startTimeMillis = calendarEvent.startTimeInMillis
        val endTimeMillis = calendarEvent.endTimeInMillis

        val pattern = if (use12HourFormat) "h:mm a" else "HH:mm"
        val dateFormat = SimpleDateFormat(pattern, Locale.getDefault())
        val formattedStartTime = dateFormat.format(Date(startTimeMillis))
        val formattedEndTime = dateFormat.format(Date(endTimeMillis))

        return if (!showEndTime) formattedStartTime  else "$formattedStartTime - $formattedEndTime"
    }


    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }
}
