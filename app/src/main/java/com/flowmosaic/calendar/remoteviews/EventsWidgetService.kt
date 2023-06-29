package com.flowmosaic.calendar.remoteviews

import android.content.Intent
import android.widget.RemoteViewsService

class EventsWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return EventsRemoteViewsFactory(applicationContext)
    }
}