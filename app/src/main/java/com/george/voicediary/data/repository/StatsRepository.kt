package com.george.voicediary.data.repository

import com.george.voicediary.data.local.dao.EntryDao
import com.george.voicediary.domain.model.Mood
import com.george.voicediary.domain.model.WritingStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsRepository @Inject constructor(
    private val entryDao: EntryDao
) {
    fun getStats(): Flow<WritingStats> {
        return entryDao.getAllEntries().map { entriesWithMetadata ->
            val entries = entriesWithMetadata.map { it.entry }
            val totalEntries = entries.size
            
            if (totalEntries == 0) {
                return@map WritingStats(0, 0, 0, 0, 0, emptyMap())
            }

            val wordCounts = entries.map { entry ->
                entry.body.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
            }

            val totalWords = wordCounts.sum()
            val longestEntryWords = wordCounts.maxOrNull() ?: 0
            val avgWordsPerEntry = totalWords / totalEntries

            // Streak calculation
            val entryDates = entries.map {
                Instant.ofEpochMilli(it.createdAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }.toSet()

            var currentStreak = 0
            var checkDate = LocalDate.now()
            
            // If there's no entry today, check if there was one yesterday to continue a streak
            if (!entryDates.contains(checkDate)) {
                checkDate = checkDate.minusDays(1)
            }

            while (entryDates.contains(checkDate)) {
                currentStreak++
                checkDate = checkDate.minusDays(1)
            }

            // Mood distribution (last 30 days)
            val thirtyDaysAgo = LocalDate.now().minusDays(30)
            val moodDistribution = entries.filter {
                val entryDate = Instant.ofEpochMilli(it.createdAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                !entryDate.isBefore(thirtyDaysAgo)
            }.groupBy { it.mood }
                .mapValues { it.value.size }
                .filter { it.value > 0 }

            WritingStats(
                totalEntries = totalEntries,
                totalWords = totalWords,
                longestEntryWords = longestEntryWords,
                avgWordsPerEntry = avgWordsPerEntry,
                currentStreakDays = currentStreak,
                moodDistribution = moodDistribution
            )
        }
    }
}
