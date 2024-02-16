package com.flowmosaic.calendar.remoteviews

import android.content.Intent
import android.widget.RemoteViewsService

class EventsWidgetService : RemoteViewsService() {

    public companion object {
        const val KEY_WIDGET_ID = "key_widget_id"
    }

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val widgetId = intent.getStringExtra(KEY_WIDGET_ID)
        return EventsRemoteViewsFactory(applicationContext, if (widgetId.isNullOrEmpty()) "" else widgetId)
    }
}