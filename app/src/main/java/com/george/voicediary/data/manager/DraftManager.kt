package com.george.voicediary.data.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.george.voicediary.domain.model.Mood
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "draft_prefs")

data class EntryDraft(
    val title: String,
    val body: String,
    val mood: String,
    val tags: List<String>
)

@Singleton
class DraftManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    private object Keys {
        val DRAFT = stringPreferencesKey("entry_draft")
    }

    val draftFlow: Flow<EntryDraft?> = context.dataStore.data.map { preferences ->
        preferences[Keys.DRAFT]?.let { json ->
            gson.fromJson(json, EntryDraft::class.java)
        }
    }

    suspend fun saveDraft(draft: EntryDraft) {
        context.dataStore.edit { preferences ->
            preferences[Keys.DRAFT] = gson.toJson(draft)
        }
    }

    suspend fun clearDraft() {
        context.dataStore.edit { preferences ->
            preferences.remove(Keys.DRAFT)
        }
    }
}