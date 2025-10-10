package com.thorfio.playzer.data.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException

class ThemePreferencesRepository(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val dataStore: DataStore<Preferences> = androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { context.preferencesDataStoreFile("theme_prefs") }
    )

    private val DARK_KEY = booleanPreferencesKey("dark_theme")
    private val DYNAMIC_KEY = booleanPreferencesKey("dynamic_color")

    val darkThemeFlow: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw it }
        .map { prefs -> prefs[DARK_KEY] ?: false }

    val dynamicColorFlow: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw it }
        .map { prefs -> prefs[DYNAMIC_KEY] ?: true }

    fun setDark(enabled: Boolean) {
        scope.launch { dataStore.edit { it[DARK_KEY] = enabled } }
    }

    fun setDynamic(enabled: Boolean) {
        scope.launch { dataStore.edit { it[DYNAMIC_KEY] = enabled } }
    }
}
