package com.flowmosaic.calendar.analytics

import android.content.Context
import android.os.Bundle
import com.amplitude.android.Amplitude
import com.amplitude.android.Configuration
import com.amplitude.android.DefaultTrackingOptions
import com.google.firebase.analytics.FirebaseAnalytics
import com.mixpanel.android.mpmetrics.MixpanelAPI
import org.json.JSONObject

object FirebaseLogger {

    enum class WidgetStatus(val status: String) {
        ACTIVE("active"),
        ENABLED("enabled"),
        DELETED("deleted"),
        DISABLED("disabled"),
    }

    enum class ScreenName(val screenName: String) {
        REQUEST_PERMISSIONS("request_permissions"),
        WIDGET("widget"),
        PREFS("prefs")
    }

    enum class RequestPermissionsItemName(val itemName: String) {
        REQUEST_PERMISSIONS_BUTTON("request_permissions_button"),
    }

    enum class WidgetItemName(val itemName: String) {
        DATE("date"),
        EVENT("event")
    }

    enum class PrefsScreenItemName(val itemName: String) {
        SELECT_CALENDARS("select_calendars"),
        NUMBER_DAYS("number_of_days"),
        SHOW_END_TIME("show_end_time"),
        TEXT_COLOR("text_color")
    }

    fun getAmplitudeInstance(context: Context): Amplitude {
        return Amplitude(
            Configuration(
                apiKey = "040dc24f4d338b42206d69f262a0b6b5",
                context = context.applicationContext,
                defaultTracking = DefaultTrackingOptions.ALL,
            )
        )
    }

    private fun getMixpanelInstance(context: Context): MixpanelAPI {
        return MixpanelAPI.getInstance(context, "30601539929e247063f85a5d72a925e3", true)
    }

    fun logScreenShownEvent(context: Context, screenName: ScreenName) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName.screenName)
        }
        FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)

        // Mixpanel log
        val properties = JSONObject()
        properties.put(FirebaseAnalytics.Param.SCREEN_NAME, screenName.screenName)
        val propertiesMap = mutableMapOf<String, Any?>(
            FirebaseAnalytics.Param.SCREEN_NAME to screenName.screenName,
        )
        getMixpanelInstance(context).track(FirebaseAnalytics.Event.SCREEN_VIEW, properties)
        getAmplitudeInstance(context).track(FirebaseAnalytics.Event.SCREEN_VIEW, propertiesMap)
    }

    fun logWidgetLifecycleEvent(
        context: Context, widgetStatus: WidgetStatus, additionalParams: Map<String, String>? = null
    ) {
        val bundle = Bundle().apply {
            putString("type", widgetStatus.status)
            additionalParams?.let { params ->
                for ((key, value) in params) {
                    putString(key, value)
                }
            }
        }
        FirebaseAnalytics.getInstance(context).logEvent("widget_lifecycle_event", bundle)

        // Mixpanel log
        val properties = JSONObject().apply {
            put("type", widgetStatus.status)
            additionalParams?.let { params ->
                for ((key, value) in params) {
                    put(key, value)
                }
            }
        }
        val propertiesMap = mutableMapOf<String, Any?>(
            "type" to widgetStatus.status
        )
        additionalParams?.let { params ->
            propertiesMap.putAll(params)
        }
        getMixpanelInstance(context).track("widget_lifecycle_event", properties)
        getAmplitudeInstance(context).track("widget_lifecycle_event", propertiesMap)
    }

    fun logSelectItemEvent(context: Context, screenName: ScreenName, name: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName.screenName)
            putString(FirebaseAnalytics.Param.ITEM_NAME, name)
        }
        FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.SELECT_ITEM, bundle)

        // Mixpanel log
        val properties = JSONObject().apply {
            put(FirebaseAnalytics.Param.SCREEN_NAME, screenName.screenName)
            put(FirebaseAnalytics.Param.ITEM_NAME, name)
        }
        val propertiesMap = mutableMapOf<String, Any?>(
            FirebaseAnalytics.Param.SCREEN_NAME to screenName.screenName,
            FirebaseAnalytics.Param.ITEM_NAME to name,
        )
        getMixpanelInstance(context).track(FirebaseAnalytics.Event.SELECT_ITEM, properties)
        getAmplitudeInstance(context).track(FirebaseAnalytics.Event.SELECT_ITEM, propertiesMap)
    }

    fun flushMixpanelEvents(context: Context) {
        getMixpanelInstance(context).flush()
        getAmplitudeInstance(context).flush()
    }

}