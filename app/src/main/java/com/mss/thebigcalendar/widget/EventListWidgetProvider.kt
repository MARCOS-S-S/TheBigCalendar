package com.mss.thebigcalendar.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.mss.thebigcalendar.MainActivity
import com.mss.thebigcalendar.R
import android.util.Log

class EventListWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "EventListWidgetProvider"
        const val ACTION_REFRESH_EVENT_LIST_WIDGET = "com.mss.thebigcalendar.ACTION_REFRESH_EVENT_LIST_WIDGET"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "🔄 onUpdate() chamado para ${appWidgetIds.size} widgets")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        
        // Forçar atualização dos dados do RemoteViewsFactory
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.event_list_view)
        Log.d(TAG, "📱 notifyAppWidgetViewDataChanged() chamado")
    }


    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.event_list_widget)

        // Set up the intent that points to the RemoteViewsService that will
        // provide the views for the ListView.
        val serviceIntent = Intent(context, EventListWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }

        // Set up the ListView
        views.setRemoteAdapter(R.id.event_list_view, serviceIntent)

        // Set up the empty view
        views.setEmptyView(R.id.event_list_view, R.id.empty_view)

        // Set up the click listener for the widget title to open the app
        val appIntent = Intent(context, MainActivity::class.java)
        val appPendingIntent = PendingIntent.getActivity(
            context,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_title, appPendingIntent)

        // Set up the click listener for the refresh button
        val refreshIntent = Intent(context, EventListWidgetProvider::class.java).apply {
            action = ACTION_REFRESH_EVENT_LIST_WIDGET
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)

        // Notify the AppWidgetManager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.event_list_view)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        Log.d(TAG, "📨 onReceive() chamado com action: ${intent?.action}")
        
        if (intent?.action == ACTION_REFRESH_EVENT_LIST_WIDGET) {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            Log.d(TAG, "🔄 Refresh manual solicitado para widget ID: $appWidgetId")
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                context?.let {
                    val appWidgetManager = AppWidgetManager.getInstance(it)
                    updateAppWidget(it, appWidgetManager, appWidgetId)
                }
            }
        } else if (intent?.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            Log.d(TAG, "🔄 Atualização automática recebida")
        }
    }
}