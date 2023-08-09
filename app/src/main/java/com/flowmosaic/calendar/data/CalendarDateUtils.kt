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
        val weekLaterDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }

        return when {
            isSameDay(eventCalendar, currentDate) -> context.getString(R.string.today)
            isSameDay(eventCalendar, tomorrowDate) -> context.getString(R.string.tomorrow)
            eventCalendar.after(weekLaterDate) -> {
                val sdf = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
                sdf.format(eventCalendar.time)
            }
            else -> eventCalendar.getDisplayName(
                Calendar.DAY_OF_WEEK,
                Calendar.LONG,
                Locale.getDefault()
            ) as String
        }
    }

    fun getCalendarEventText(calendarEvent: CalendarEvent, context: Context): String {
        return if (calendarEvent.isAllDay) {
            calendarEvent.title
        } else {
            val formattedTimeRange =
                getFormattedTime(calendarEvent, AgendaWidgetPrefs.getShowEndTime(context))
            "$formattedTimeRange | ${calendarEvent.title}"
        }
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

    private fun getFormattedTime(calendarEvent: CalendarEvent, showEndTime: Boolean): String {
        val startTimeMillis = calendarEvent.startTimeInMillis
        val endTimeMillis = calendarEvent.endTimeInMillis
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val isEndAtMidnightOfNextDay = Calendar.getInstance().apply {
            timeInMillis = endTimeMillis
            add(Calendar.MILLISECOND, -1) // subtracting 1 millisecond to get to the previous day
        }.let {
            it.get(Calendar.HOUR_OF_DAY) == 23 &&
                    it.get(Calendar.MINUTE) == 59 &&
                    it.get(Calendar.SECOND) == 59
        }

        return if (!showEndTime) {
            dateFormat.format(Date(startTimeMillis))
        } else {
            "${dateFormat.format(Date(startTimeMillis))} - ${
                if (isEndAtMidnightOfNextDay) "24:00" else dateFormat.format(Date(endTimeMillis))
            }"
        }
    }


    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }
}
