package com.george.voicediary.data.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.george.voicediary.MainActivity
import com.george.voicediary.domain.model.Mood
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_DAILY_REMINDER = "daily_reminder"
        const val CHANNEL_WEEKLY_SUMMARY = "weekly_summary"
        const val NOTIFICATION_ID_DAILY = 1001
        const val NOTIFICATION_ID_WEEKLY = 1002
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val dailyChannel = NotificationChannel(
                CHANNEL_DAILY_REMINDER,
                "Daily Reminder",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminds you to write in your diary daily"
            }

            val weeklyChannel = NotificationChannel(
                CHANNEL_WEEKLY_SUMMARY,
                "Weekly Summary",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Weekly statistics about your diary entries"
            }

            notificationManager.createNotificationChannel(dailyChannel)
            notificationManager.createNotificationChannel(weeklyChannel)
        }
    }

    fun showDailyReminder() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_REMINDER)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("My Diary 📝")
            .setContentText("Have you written today?")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_DAILY, notification)
    }

    fun showWeeklySummary(entryCount: Int, topMood: Mood) {
        val notification = NotificationCompat.Builder(context, CHANNEL_WEEKLY_SUMMARY)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Weekly Summary")
            .setContentText("You wrote $entryCount entries this week. Most common mood: ${topMood.emoji}")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_WEEKLY, notification)
    }
}