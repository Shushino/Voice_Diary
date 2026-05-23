package com.george.voicediary.domain.model

data class WritingStats(
    val totalEntries: Int,
    val totalWords: Int,
    val longestEntryWords: Int,
    val avgWordsPerEntry: Int,
    val currentStreakDays: Int,
    val moodDistribution: Map<Mood, Int>  // last 30 days
)
