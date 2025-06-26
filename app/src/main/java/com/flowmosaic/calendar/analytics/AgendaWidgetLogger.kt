package com.flowmosaic.calendar.analytics

import android.content.Context
import com.flowmosaic.calendar.prefs.AgendaWidgetPrefs
import com.google.android.play.core.review.ReviewManagerFactory
import com.posthog.PostHog
import java.util.concurrent.TimeUnit


class AgendaWidgetLogger internal constructor(
    private val prefs: AgendaWidgetPrefs,
    private val context: Context
) {

    constructor(context: Context) : this(
        prefs = AgendaWidgetPrefs(context),
        context = context,
    )

    companion object {
        private const val PARAM_ITEM_NAME: String = "item_name"
        private const val PARAM_DESTINATION: String = "item_name"
        private const val PARAM_TYPE: String = "type"
        private const val PARAM_SUCCESS: String = "success"
        private const val PARAM_SKIPPED: String = "skipped"
        private const val PARAM_ACTIVITY: String = "activity"
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
        EXCEPTION("exception"),
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
        SHOW_LOCATION("show_location"),
        USE_12_HOUR("use_12_hour"),
        DATE_SEPARATOR("date_separator"),
        ALIGN_BOTTOM("align_bottom"),
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

    fun logException(additionalParams: Map<String, String>) {
        PostHog.capture(
            event = Event.EXCEPTION.eventName,
            properties = additionalParams
        )
    }

    fun logActivityStartedEvent(activity: Activity) {
        PostHog.capture(
            event = Event.ACTIVITY_STARTED.eventName,
            properties = mapOf(PARAM_ACTIVITY to activity.activityName)
        )
    }

    fun logNavigationEvent(destination: String?) {
        val eventDestination = destination ?: "not_set"
        PostHog.capture(
            event = Event.NAVIGATION.eventName,
            properties = mapOf(PARAM_DESTINATION to eventDestination)
        )
    }

    fun logPermissionsResultEvent(allPermissionsGranted: Boolean) {
        PostHog.capture(
            event = Event.PERMISSIONS_RESULT.eventName,
            properties = mapOf(PARAM_SUCCESS to allPermissionsGranted)
        )
    }

    fun logOnboardingCompleteEvent(skipped: Boolean) {
        PostHog.capture(
            event = Event.ONBOARDING_COMPLETE.eventName,
            properties = mapOf(PARAM_SKIPPED to skipped)
        )
    }

    fun logActionButtonEvent(actionButton: ActionButton) {
        PostHog.capture(
            event = Event.ACTION_BUTTON.eventName,
            properties = mapOf(PARAM_ITEM_NAME to actionButton.buttonName)
        )
    }

    fun logUpdatePrefEvent(name: PrefsScreenItemName) {
        launchInAppReview()
        PostHog.capture(
            event = Event.UPDATE_PREF.eventName,
            properties = mapOf(PARAM_ITEM_NAME to name.itemName)
        )
    }

    fun logSelectItemEvent(name: WidgetItemName) {
        PostHog.capture(
            event = Event.SELECT_ITEM.eventName,
            properties = mapOf(PARAM_ITEM_NAME to name.itemName)
        )
    }

    fun logWidgetLifecycleEvent(
        widgetStatus: WidgetStatus, additionalParams: Map<String, String>? = null
    ) {
        val properties = mutableMapOf(PARAM_TYPE to widgetStatus.status)
        additionalParams?.let { properties.putAll(it) }
        PostHog.capture(
            event = Event.WIDGET_LIFECYCLE_EVENT.eventName,
            properties = properties
        )
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
            PostHog.capture(
                event = Event.IN_APP_REVIEW.eventName,
                properties = mapOf(PARAM_SUCCESS to task.isSuccessful)
            )
        }
    }

}