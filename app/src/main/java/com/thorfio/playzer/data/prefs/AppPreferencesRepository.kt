package com.thorfio.playzer.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Repository for managing application preferences including UI settings and
 * functional preferences like music folder location
 */
class AppPreferencesRepository(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

        // Keys for preferences
        val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")
        val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
        val MUSIC_FOLDER_PATH_KEY = stringPreferencesKey("music_folder_path")
        val LAST_SCAN_TIMESTAMP_KEY = longPreferencesKey("last_scan_timestamp")
    }

    // UI Theme Preferences
    val darkThemeEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[DARK_THEME_KEY] ?: false
        }

    val dynamicColorEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[DYNAMIC_COLOR_KEY] ?: true
        }

    // Music Folder Preferences
    val musicFolderPath: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[MUSIC_FOLDER_PATH_KEY]
        }

    val lastScanTimestamp: Flow<Long> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[LAST_SCAN_TIMESTAMP_KEY] ?: 0L
        }

    // Functions to update preferences
    suspend fun setDarkThemeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_THEME_KEY] = enabled
        }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR_KEY] = enabled
        }
    }

    suspend fun setMusicFolderPath(path: String?) {
        context.dataStore.edit { preferences ->
            if (path != null) {
                preferences[MUSIC_FOLDER_PATH_KEY] = path
            } else {
                preferences.remove(MUSIC_FOLDER_PATH_KEY)
            }
        }
    }

    suspend fun updateLastScanTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_SCAN_TIMESTAMP_KEY] = timestamp
        }
    }
}
