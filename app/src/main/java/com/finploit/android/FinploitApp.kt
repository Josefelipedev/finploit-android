package com.finploit.android

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.finploit.android.notifications.NotificationHelper
import com.finploit.android.notifications.WorkManagerScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FinploitApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notificationHelper: NotificationHelper

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createNotificationChannel()
        WorkManagerScheduler.scheduleDailyReminder(this)
        WorkManagerScheduler.scheduleWeeklyShoppingReminder(this)
    }
}
