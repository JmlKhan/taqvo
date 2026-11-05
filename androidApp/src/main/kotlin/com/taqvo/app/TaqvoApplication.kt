package com.taqvo.app

import android.app.Application
import com.taqvo.app.crash.crashLogStore
import com.taqvo.app.crash.installCrashHandler
import com.taqvo.app.notifications.NotificationTopUpWorker
import com.taqvo.app.notifications.notificationSmallIconResId
import com.taqvo.app.settings.appContext
import com.taqvo.app.widget.TaqvoWidgets
import com.taqvo.app.widget.WidgetPlacement
import com.taqvo.app.widget.androidAyahWidgetUpdateHook
import com.taqvo.app.widget.androidWidgetPinHook
import android.appwidget.AppWidgetManager
import com.taqvo.app.widget.androidWidgetPlacementHook
import com.taqvo.app.widget.androidWidgetUpdateHook
import com.taqvo.app.widget.anyAyahWidgetPlaced
import com.taqvo.app.widget.anyWidgetPlaced
import com.taqvo.app.widget.TaqvoAyahWidgetReceiver
import com.taqvo.app.widget.TaqvoDuaWidgetReceiver

class TaqvoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        // First, so that a crash anywhere below is already recorded (crash spec §2).
        installCrashHandler(crashLogStore)
        notificationSmallIconResId = R.drawable.ic_stat_taqvo
        androidWidgetUpdateHook = { TaqvoWidgets.updateAll(applicationContext) }
        androidAyahWidgetUpdateHook = { TaqvoWidgets.updateAyah(applicationContext) }
        androidWidgetPinHook = { widget -> TaqvoWidgets.requestPin(applicationContext, widget) }
        // The same two queries the refresh alarms already trust to decide whether to keep
        // ticking — read on demand, never cached, because widgets come and go outside the app.
        // A device with no app-widget host at all — a kiosk launcher, some work profiles — has no
        // AppWidgetManager, and the two queries below answer "not placed" for want of anywhere to
        // look. That is the one answer this must never give: it would offer to add a widget the
        // launcher cannot hold. Null, "cannot tell", which shows no status and offers nothing, is
        // the honest reading. Android has no Lock Screen widget, so it never reports one.
        androidWidgetPlacementHook = {
            if (AppWidgetManager.getInstance(applicationContext) == null) {
                null
            } else {
                WidgetPlacement(
                    prayer = anyWidgetPlaced(applicationContext),
                    ayah = anyAyahWidgetPlaced(applicationContext, TaqvoAyahWidgetReceiver::class.java),
                    dua = anyAyahWidgetPlaced(applicationContext, TaqvoDuaWidgetReceiver::class.java),
                )
            }
        }
        // Unlocking is when the home screen is actually read, so it is when the countdown most
        // needs to be current. Runtime registration is not a choice: ACTION_USER_PRESENT is one of
        // the broadcasts the platform refuses to deliver to manifest-declared receivers.
        TaqvoWidgets.registerUnlockRefresh(this)
        // The twice-daily rebuild of the alarm plan, for the cases no broadcast covers; see the
        // worker. WorkManager is already initialised here for the widgets and the downloads.
        NotificationTopUpWorker.ensureScheduled(this)
    }
}
