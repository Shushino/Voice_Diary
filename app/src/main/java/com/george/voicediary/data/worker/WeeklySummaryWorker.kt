package com.george.voicediary.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.george.voicediary.data.manager.NotificationHelper
import com.george.voicediary.domain.model.Mood
import com.george.voicediary.domain.repository.DiaryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar

@HiltWorker
class WeeklySummaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationHelper: NotificationHelper,
    private val diaryRepository: DiaryRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startTime = calendar.timeInMillis

        val entries = diaryRepository.getEntriesInDateRange(startTime, endTime).first()

        if (entries.isNotEmpty()) {
            val moodCounts = entries.groupBy { it.mood }.mapValues { it.value.size }
            val topMood = moodCounts.maxByOrNull { it.value }?.key ?: Mood.NEUTRAL
            notificationHelper.showWeeklySummary(entries.size, topMood)
        }

        return Result.success()
    }
}