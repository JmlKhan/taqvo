package com.taqvo.app.notifications

actual fun createNotificationScheduler(): NotificationScheduler = IosNotificationScheduler()
