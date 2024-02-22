package com.flowmosaic.calendar.analytics

import android.content.Context
import android.os.Bundle
import com.amplitude.android.Amplitude
import com.amplitude.android.Configuration
import com.amplitude.android.DefaultTrackingOptions
import com.flowmosaic.calendar.prefs.AgendaWidgetPrefs
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.analytics.FirebaseAnalytics
import java.util.concurrent.TimeUnit

object AgendaWidgetLogger {

    private const val PARAM_ITEM_NAME: String = "item_name"
    private const val PARAM_DESTINATION: String = "item_name"
    private const val PARAM_TYPE: String = "type"
    private const val PARAM_SUCCESS: String = "success"
    private const val PARAM_SKIPPED: String = "skipped"

    private enum class Event(val eventName: String) {
        ACTIVITY_STARTED("activity_started"),
        NAVIGATION("navigation"),
        ACTION_BUTTON("action_button"),
        SELECT_ITEM("select_item"),
        UPDATE_PREF("update_pref"),
        WIDGET_LIFECYCLE_EVENT("widget_lifecycle_event"),
        IN_APP_REVIEW("in_app_review"),
        PERMISSIONS_RESULT("permissions_result"),
        ONBOARDING_COMPLETE("onboarding_complete"),
    }

    enum class Activity(val activityName: String) {
        MAIN_ACTIVITY("main_activity"),
        PREFERENCES_ACTIVITY("preferences_activity"),
        PERMISSIONS_ACTIVITY("permissions_activity"),
    }

    enum class ActionButton(val buttonName: String) {
        REFRESH("refresh"),
        ADD_EVENT("add_event"),
    }

    enum class WidgetItemName(val itemName: String) {
        DATE("date"),
        EVENT("event")
    }

    enum class PrefsScreenItemName(val itemName: String) {
        SELECT_CALENDARS("select_calendars"),
        NUMBER_DAYS("number_of_days"),
        SHOW_END_TIME("show_end_time"),
        SHOW_ACTION_BUTTONS("show_action_buttons"),
        SHOW_NO_EVENTS_TEXT("show_no_events_text"),
        USE_12_HOUR("use_12_hour"),
        DATE_SEPARATOR("date_separator"),
        TEXT_COLOR("text_color"),
        FONT_SIZE("font_size"),
        TEXT_ALIGNMENT("text_alignment"),
        OPACITY("opacity"),
    }

    enum class WidgetStatus(val status: String) {
        ACTIVE("active"),
        ENABLED("enabled"),
        DELETED("deleted"),
        DISABLED("disabled"),
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

    fun logActivityStartedEvent(context: Context, activity: Activity) {
        val bundle = Bundle().apply {
            putString("activity", activity.activityName)
        }
        FirebaseAnalytics.getInstance(context).logEvent(Event.ACTIVITY_STARTED.eventName, bundle)

        val propertiesMap = mutableMapOf<String, Any?>(
            "activity" to activity.activityName,
        )
        getAmplitudeInstance(context).track(Event.ACTIVITY_STARTED.eventName, propertiesMap)
    }

    fun logNavigationEvent(context: Context, destination: String?) {
        val eventDestination = destination ?: "not_set"

        FirebaseAnalytics.getInstance(context).logEvent(Event.NAVIGATION.eventName, Bundle().apply {
            putString(PARAM_DESTINATION, eventDestination)
        })

        getAmplitudeInstance(context).track(
            Event.NAVIGATION.eventName, mutableMapOf<String, Any?>(
                PARAM_DESTINATION to eventDestination,
            )
        )
    }

    fun logPermissionsResultEvent(context: Context, allPermissionsGranted: Boolean) {
        FirebaseAnalytics.getInstance(context).logEvent(Event.PERMISSIONS_RESULT.eventName, Bundle().apply {
            putBoolean(PARAM_SUCCESS, allPermissionsGranted)
        })

        getAmplitudeInstance(context).track(
            Event.PERMISSIONS_RESULT.eventName, mutableMapOf<String, Any?>(
                PARAM_SUCCESS to allPermissionsGranted,
            )
        )
    }

    fun logOnboardingCompleteEvent(context: Context, skipped: Boolean) {
        FirebaseAnalytics.getInstance(context).logEvent(Event.ONBOARDING_COMPLETE.eventName, Bundle().apply {
            putBoolean(PARAM_SKIPPED, skipped)
        })

        getAmplitudeInstance(context).track(
            Event.ONBOARDING_COMPLETE.eventName, mutableMapOf<String, Any?>(
                PARAM_SKIPPED to skipped,
            )
        )
    }

    fun logActionButtonEvent(context: Context, actionButton: ActionButton) {
        FirebaseAnalytics.getInstance(context)
            .logEvent(Event.ACTION_BUTTON.eventName, Bundle().apply {
                putString(PARAM_ITEM_NAME, actionButton.buttonName)
            })

        getAmplitudeInstance(context).track(
            Event.ACTION_BUTTON.eventName, mutableMapOf<String, Any?>(
                PARAM_ITEM_NAME to actionButton.buttonName,
            )
        )
    }

    fun logUpdatePrefEvent(context: Context, name: PrefsScreenItemName) {
        FirebaseAnalytics.getInstance(context)
            .logEvent(Event.UPDATE_PREF.eventName, Bundle().apply {
                putString(PARAM_ITEM_NAME, name.itemName)
            })

        getAmplitudeInstance(context).track(
            Event.UPDATE_PREF.eventName, mutableMapOf<String, Any?>(
                PARAM_ITEM_NAME to name.itemName,
            )
        )

        launchInAppReview(context)
    }

    fun logSelectItemEvent(context: Context, name: WidgetItemName) {
        FirebaseAnalytics.getInstance(context)
            .logEvent(Event.SELECT_ITEM.eventName, Bundle().apply {
                putString(PARAM_ITEM_NAME, name.itemName)
            })

        getAmplitudeInstance(context).track(
            Event.SELECT_ITEM.eventName, mutableMapOf<String, Any?>(
                PARAM_ITEM_NAME to name.itemName,
            )
        )
    }

    fun logWidgetLifecycleEvent(
        context: Context, widgetStatus: WidgetStatus, additionalParams: Map<String, String>? = null
    ) {
        FirebaseAnalytics.getInstance(context)
            .logEvent(Event.WIDGET_LIFECYCLE_EVENT.eventName, Bundle().apply {
                putString(PARAM_TYPE, widgetStatus.status)
                additionalParams?.let { params ->
                    for ((key, value) in params) {
                        putString(key, value)
                    }
                }
            })

        getAmplitudeInstance(context).track(Event.WIDGET_LIFECYCLE_EVENT.eventName,
            mutableMapOf<String, Any?>(PARAM_TYPE to widgetStatus.status)
                .apply { putAll(additionalParams ?: emptyMap()) })
    }


    private fun launchInAppReview(context: Context) {
        val pm = context.packageManager
        val pi = pm.getPackageInfo(context.packageName, 0)
        val currentTimeMs = System.currentTimeMillis()
        val lastReviewPrompt = AgendaWidgetPrefs.getLastReviewPrompt(context)

        if ((currentTimeMs - TimeUnit.DAYS.toMillis(2) < pi.firstInstallTime)
            || (currentTimeMs - TimeUnit.DAYS.toMillis(15) < lastReviewPrompt)
        ) {
            return
        }

        AgendaWidgetPrefs.setLastReviewPrompt(context, currentTimeMs)

        val manager = ReviewManagerFactory.create(context)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            FirebaseAnalytics.getInstance(context)
                .logEvent(Event.IN_APP_REVIEW.eventName, Bundle().apply {
                    putBoolean(PARAM_SUCCESS, task.isSuccessful)
                })
            getAmplitudeInstance(context).track(
                Event.IN_APP_REVIEW.eventName, mapOf(
                    PARAM_SUCCESS to task.isSuccessful
                )
            )
        }
    }

    fun flushEvents(context: Context) {
        getAmplitudeInstance(context).flush()
    }

}