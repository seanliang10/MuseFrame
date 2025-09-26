package com.museframe.app.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
        private val DEVICE_ID_KEY = stringPreferencesKey("device_id")
        private val PUSHY_TOKEN_KEY = stringPreferencesKey("pushy_token")
        private val DISPLAY_NAME_KEY = stringPreferencesKey("display_name")
        private val VOLUME_KEY = floatPreferencesKey("volume")
        private val BRIGHTNESS_KEY = floatPreferencesKey("brightness")
        private val SLIDESHOW_DURATION_KEY = intPreferencesKey("slideshow_duration")
        private val IS_PAUSED_KEY = booleanPreferencesKey("is_paused")
    }

    val authToken: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading auth token")
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[AUTH_TOKEN_KEY]
        }

    val deviceId: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading device ID")
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[DEVICE_ID_KEY]
        }

    val pushyToken: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading Pushy token")
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PUSHY_TOKEN_KEY]
        }

    val displaySettings: Flow<DisplaySettingsData> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading display settings")
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            DisplaySettingsData(
                name = preferences[DISPLAY_NAME_KEY] ?: "Muse Frame",
                volume = preferences[VOLUME_KEY] ?: 1.0f,
                brightness = preferences[BRIGHTNESS_KEY] ?: 1.0f,
                slideshowDuration = preferences[SLIDESHOW_DURATION_KEY] ?: 30,
                isPaused = preferences[IS_PAUSED_KEY] ?: false
            )
        }

    suspend fun saveAuthToken(token: String) {
        dataStore.edit { preferences ->
            preferences[AUTH_TOKEN_KEY] = token
        }
    }

    suspend fun clearAuthToken() {
        dataStore.edit { preferences ->
            preferences.remove(AUTH_TOKEN_KEY)
        }
    }

    suspend fun saveDeviceId(deviceId: String) {
        dataStore.edit { preferences ->
            preferences[DEVICE_ID_KEY] = deviceId
        }
    }

    suspend fun savePushyToken(token: String) {
        dataStore.edit { preferences ->
            preferences[PUSHY_TOKEN_KEY] = token
        }
    }

    suspend fun updateDisplaySettings(
        name: String? = null,
        volume: Float? = null,
        brightness: Float? = null,
        duration: Int? = null,
        isPaused: Boolean? = null
    ) {
        dataStore.edit { preferences ->
            name?.let { preferences[DISPLAY_NAME_KEY] = it }
            volume?.let { preferences[VOLUME_KEY] = it }
            brightness?.let { preferences[BRIGHTNESS_KEY] = it }
            duration?.let { preferences[SLIDESHOW_DURATION_KEY] = it }
            isPaused?.let { preferences[IS_PAUSED_KEY] = it }
        }
    }

    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    data class DisplaySettingsData(
        val name: String,
        val volume: Float,
        val brightness: Float,
        val slideshowDuration: Int,
        val isPaused: Boolean
    )
}