package com.example.intentblocker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppPrefs(private val context: Context) {

    companion object {
        val BLOCKED_PACKAGES = stringSetPreferencesKey("blocked_packages")
        val FRICTION_SENTENCE = stringPreferencesKey("friction_sentence")
        val ALLOW_DURATION = androidx.datastore.preferences.core.longPreferencesKey("allow_duration")
        const val DEFAULT_SENTENCE = "I am conscious of my choice"
        const val DEFAULT_DURATION = 60000L
    }

    val blockedPackages: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[BLOCKED_PACKAGES] ?: emptySet()
        }

    val frictionSentence: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[FRICTION_SENTENCE] ?: DEFAULT_SENTENCE
        }

    suspend fun addBlockedPackage(packageName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[BLOCKED_PACKAGES] ?: emptySet()
            preferences[BLOCKED_PACKAGES] = current + packageName
        }
    }

    suspend fun removeBlockedPackage(packageName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[BLOCKED_PACKAGES] ?: emptySet()
            preferences[BLOCKED_PACKAGES] = current - packageName
        }
    }

    suspend fun setFrictionSentence(sentence: String) {
        context.dataStore.edit { preferences ->
            preferences[FRICTION_SENTENCE] = sentence
        }
    }

    val allowDuration: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[ALLOW_DURATION] ?: DEFAULT_DURATION
        }

    suspend fun setAllowDuration(duration: Long) {
        context.dataStore.edit { preferences ->
            preferences[ALLOW_DURATION] = duration
        }
    }
}
