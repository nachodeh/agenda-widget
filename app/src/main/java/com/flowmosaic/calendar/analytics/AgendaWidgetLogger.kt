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



class AgendaWidgetLogger internal constructor(
    private val amplitude: Amplitude,
    private val firebaseAnalytics: FirebaseAnalytics,
    private val prefs: AgendaWidgetPrefs,
    private val context: Context
) {

    constructor(context: Context) : this(
        amplitude = Amplitude(
            // Assuming you have a method similar to this to initialize Amplitude
            Configuration(
                apiKey = "040dc24f4d338b42206d69f262a0b6b5",
                context = context.applicationContext,
                defaultTracking = DefaultTrackingOptions.ALL,
            )
        ),
        firebaseAnalytics = FirebaseAnalytics.getInstance(context),
        prefs = AgendaWidgetPrefs(context),
        context = context,
    )

    companion object {
        private const val PARAM_ITEM_NAME: String = "item_name"
        private const val PARAM_DESTINATION: String = "item_name"
        private const val PARAM_TYPE: String = "type"
        private const val PARAM_SUCCESS: String = "success"
        private const val PARAM_SKIPPED: String = "skipped"
    }

    internal enum class Event(val eventName: String) {
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

    fun logActivityStartedEvent(activity: Activity) {
        val bundle = Bundle().apply {
            putString("activity", activity.activityName)
        }
        firebaseAnalytics.logEvent(Event.ACTIVITY_STARTED.eventName, bundle)

        val propertiesMap = mutableMapOf<String, Any?>(
            "activity" to activity.activityName,
        )
        amplitude.track(Event.ACTIVITY_STARTED.eventName, propertiesMap)
    }

    fun logNavigationEvent(destination: String?) {
        val eventDestination = destination ?: "not_set"

        firebaseAnalytics.logEvent(Event.NAVIGATION.eventName, Bundle().apply {
            putString(PARAM_DESTINATION, eventDestination)
        })

        amplitude.track(
            Event.NAVIGATION.eventName, mutableMapOf<String, Any?>(
                PARAM_DESTINATION to eventDestination,
            )
        )
    }

    fun logPermissionsResultEvent(allPermissionsGranted: Boolean) {
        firebaseAnalytics.logEvent(Event.PERMISSIONS_RESULT.eventName, Bundle().apply {
            putBoolean(PARAM_SUCCESS, allPermissionsGranted)
        })

        amplitude.track(
            Event.PERMISSIONS_RESULT.eventName, mutableMapOf<String, Any?>(
                PARAM_SUCCESS to allPermissionsGranted,
            )
        )
    }

    fun logOnboardingCompleteEvent(skipped: Boolean) {
        firebaseAnalytics.logEvent(Event.ONBOARDING_COMPLETE.eventName, Bundle().apply {
            putBoolean(PARAM_SKIPPED, skipped)
        })

        amplitude.track(
            Event.ONBOARDING_COMPLETE.eventName, mutableMapOf<String, Any?>(
                PARAM_SKIPPED to skipped,
            )
        )
    }

    fun logActionButtonEvent(actionButton: ActionButton) {
        firebaseAnalytics
            .logEvent(Event.ACTION_BUTTON.eventName, Bundle().apply {
                putString(PARAM_ITEM_NAME, actionButton.buttonName)
            })

        amplitude.track(
            Event.ACTION_BUTTON.eventName, mutableMapOf<String, Any?>(
                PARAM_ITEM_NAME to actionButton.buttonName,
            )
        )
    }

    fun logUpdatePrefEvent(name: PrefsScreenItemName) {
        firebaseAnalytics
            .logEvent(Event.UPDATE_PREF.eventName, Bundle().apply {
                putString(PARAM_ITEM_NAME, name.itemName)
            })

        amplitude.track(
            Event.UPDATE_PREF.eventName, mutableMapOf<String, Any?>(
                PARAM_ITEM_NAME to name.itemName,
            )
        )

        launchInAppReview()
    }

    fun logSelectItemEvent(name: WidgetItemName) {
        firebaseAnalytics
            .logEvent(Event.SELECT_ITEM.eventName, Bundle().apply {
                putString(PARAM_ITEM_NAME, name.itemName)
            })

        amplitude.track(
            Event.SELECT_ITEM.eventName, mutableMapOf<String, Any?>(
                PARAM_ITEM_NAME to name.itemName,
            )
        )
    }

    fun logWidgetLifecycleEvent(
        widgetStatus: WidgetStatus, additionalParams: Map<String, String>? = null
    ) {
        firebaseAnalytics
            .logEvent(Event.WIDGET_LIFECYCLE_EVENT.eventName, Bundle().apply {
                putString(PARAM_TYPE, widgetStatus.status)
                additionalParams?.let { params ->
                    for ((key, value) in params) {
                        putString(key, value)
                    }
                }
            })

        amplitude.track(Event.WIDGET_LIFECYCLE_EVENT.eventName,
            mutableMapOf<String, Any?>(PARAM_TYPE to widgetStatus.status)
                .apply { putAll(additionalParams ?: emptyMap()) })
    }


    private fun launchInAppReview() {
        val pm = context.packageManager
        val pi = pm.getPackageInfo(context.packageName, 0)
        val currentTimeMs = System.currentTimeMillis()
        val lastReviewPrompt = prefs.getLastReviewPrompt()

        if ((currentTimeMs - TimeUnit.DAYS.toMillis(2) < pi.firstInstallTime)
            || (currentTimeMs - TimeUnit.DAYS.toMillis(15) < lastReviewPrompt)
        ) {
            return
        }

        prefs.setLastReviewPrompt(currentTimeMs)

        val manager = ReviewManagerFactory.create(context)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            firebaseAnalytics
                .logEvent(Event.IN_APP_REVIEW.eventName, Bundle().apply {
                    putBoolean(PARAM_SUCCESS, task.isSuccessful)
                })
            amplitude.track(
                Event.IN_APP_REVIEW.eventName, mapOf(
                    PARAM_SUCCESS to task.isSuccessful
                )
            )
        }
    }

    fun flushEvents() {
        amplitude.flush()
    }

}