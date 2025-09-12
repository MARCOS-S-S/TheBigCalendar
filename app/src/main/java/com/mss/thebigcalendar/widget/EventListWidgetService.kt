package com.mss.thebigcalendar.widget

import android.content.Intent
import android.widget.RemoteViewsService

class EventListWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return EventListRemoteViewsFactory(this.applicationContext, intent)
    }
}