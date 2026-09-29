package com.shushino.voicediary.data.manager

import android.content.Context
import androidx.work.*
import com.shushino.voicediary.data.worker.DailyReminderWorker
import com.shushino.voicediary.data.worker.WeeklySummaryWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    /**
     * @param reanchor pass true when the user CHANGED the reminder time. The default
     * UPDATE policy keeps WorkManager's original period anchor, so the next fire would
     * still follow the old schedule; CANCEL_AND_REENQUEUE restarts the 24h cycle from
     * the newly chosen time. Pass false for enable/disable reconciliation.
     */
    fun scheduleDailyReminder(hour: Int, minute: Int, reanchor: Boolean = false) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val initialDelay = calendar.timeInMillis - System.currentTimeMillis()

        val dailyRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag("daily_reminder")
            .build()

        workManager.enqueueUniquePeriodicWork(
            "daily_reminder",
            if (reanchor) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
            else ExistingPeriodicWorkPolicy.UPDATE,
            dailyRequest
        )
    }

    fun scheduleWeeklySummary() {
        val weeklyRequest = PeriodicWorkRequestBuilder<WeeklySummaryWorker>(7, TimeUnit.DAYS)
            .addTag("weekly_summary")
            .build()

        workManager.enqueueUniquePeriodicWork(
            "weekly_summary",
            ExistingPeriodicWorkPolicy.UPDATE,
            weeklyRequest
        )
    }

    fun cancelAll() {
        workManager.cancelAllWorkByTag("daily_reminder")
        workManager.cancelAllWorkByTag("weekly_summary")
    }
}
