package com.flowmosaic.calendar.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.flowmosaic.calendar.R
import com.flowmosaic.calendar.data.CalendarData
import java.util.concurrent.TimeUnit

object AgendaWidgetPrefs {

    private const val PREF_SELECTED_CALENDARS = "selected_calendars"
    private const val PREF_SHOW_END_TIME = "key_show_end_time"
    private const val PREF_NUMBER_OF_DAYS = "key_number_of_days"
    private const val PREF_TEXT_COLOR = "key_text_color"
    private const val PREF_LAST_LOGGED = "lastLogged"
    private const val PREF_FONT_SIZE = "key_font_size"
    private const val PREF_TEXT_ALIGNMENT = "key_text_alignment"
    private const val PREF_OPACITY = "key_opacity"
    private const val PREF_HOUR_FORMAT_12 = "key_hour_format_12"
    private const val LAST_REVIEW_PROMPT = "key_last_review_prompt"
    private const val SEPARATOR_VISIBLE = "key_separator_visible"

    enum class FontSize(private val displayResId: Int) {
        SMALL(R.string.font_size_small),
        MEDIUM(R.string.font_size_medium),
        LARGE(R.string.font_size_large);

        fun getDisplayText(context: Context): String {
            return context.getString(displayResId)
        }
    }

    enum class TextAlignment(private val displayResId: Int) {
        LEFT(R.string.text_alignment_left),
        CENTER(R.string.text_alignment_center),
        RIGHT(R.string.text_alignment_right);

        fun getDisplayText(context: Context): String {
            return context.getString(displayResId)
        }
    }

    fun getShouldLogWidgetActivityEvent(context: Context): Boolean {
        val lastLogged = getPreferences(context).getLong(PREF_LAST_LOGGED, 0)
        return (System.currentTimeMillis() - lastLogged) > TimeUnit.DAYS.toMillis(1)
    }

    fun setWidgetActivityEventLastLoggedTimestamp(context: Context) {
        getPreferences(context).edit().putLong(PREF_LAST_LOGGED, System.currentTimeMillis()).apply()
    }

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

    fun getFontSize(context: Context): FontSize {
        val size = getPreferences(context).getString(PREF_FONT_SIZE, FontSize.MEDIUM.name)
            ?: FontSize.MEDIUM.name
        return FontSize.valueOf(size)
    }

    fun setFontSize(context: Context, fontSize: FontSize) {
        getPreferences(context).edit().putString(PREF_FONT_SIZE, fontSize.name).apply()
    }

    fun getTextAlignment(context: Context): TextAlignment {
        val size = getPreferences(context).getString(PREF_TEXT_ALIGNMENT, TextAlignment.LEFT.name)
            ?: TextAlignment.LEFT.name
        return TextAlignment.valueOf(size)
    }

    fun setTextAlignment(context: Context, textAlignment: TextAlignment) {
        getPreferences(context).edit().putString(PREF_TEXT_ALIGNMENT, textAlignment.name).apply()
    }

    fun getOpacity(context: Context): Float {
        return getPreferences(context).getFloat(PREF_OPACITY, 0f)
    }

    fun setOpacity(context: Context, opacity: Float) {
        getPreferences(context).edit().putFloat(PREF_OPACITY, opacity).apply()
    }

    fun getHourFormat12(context: Context): Boolean {
        return getPreferences(context).getBoolean(PREF_HOUR_FORMAT_12, false)
    }

    fun setSeparatorVisible(context: Context, visible: Boolean) {
        getPreferences(context).edit().putBoolean(SEPARATOR_VISIBLE, visible).apply()
    }

    fun getSeparatorVisible(context: Context): Boolean {
        return getPreferences(context).getBoolean(SEPARATOR_VISIBLE, false)
    }

    fun setHourFormat12(context: Context, use12HourFormat: Boolean) {
        getPreferences(context).edit().putBoolean(PREF_HOUR_FORMAT_12, use12HourFormat).apply()
    }

    fun getLastReviewPrompt(context: Context): Long {
        return getPreferences(context).getLong(LAST_REVIEW_PROMPT, 0)
    }

    fun setLastReviewPrompt(context: Context, lastReviewPrompt: Long) {
        getPreferences(context).edit().putLong(LAST_REVIEW_PROMPT, lastReviewPrompt).apply()
    }

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(
            context.packageName + "_preferences",
            Context.MODE_PRIVATE
        )
    }

}