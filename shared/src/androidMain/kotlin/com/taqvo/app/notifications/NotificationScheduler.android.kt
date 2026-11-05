package com.taqvo.app.notifications

import com.taqvo.app.settings.appContext

actual fun createNotificationScheduler(): NotificationScheduler = AndroidNotificationScheduler(appContext)
