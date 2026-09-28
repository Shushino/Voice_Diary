package com.shushino.voicediary

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.shushino.voicediary.data.SettingsDataStore
import com.shushino.voicediary.data.manager.NotificationHelper
import com.shushino.voicediary.data.manager.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class DiaryApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createChannels()
        reconcileReminders()
    }

    /**
     * WorkManager jobs can be lost (force-stop, some OEM "battery cleaners") while the
     * stored "reminders on" flag survives. Re-schedule on every start if the flag says
     * reminders should be on — the unique-name enqueues make this idempotent.
     */
    private fun reconcileReminders() {
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val enabled = settingsDataStore.reminderEnabled.first()
                if (enabled) {
                    val hour = settingsDataStore.reminderHour.first()
                    val minute = settingsDataStore.reminderMinute.first()
                    reminderScheduler.scheduleDailyReminder(hour, minute)
                    reminderScheduler.scheduleWeeklySummary()
                }
            } catch (_: Exception) {
                // Never block startup on reminder reconciliation.
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
