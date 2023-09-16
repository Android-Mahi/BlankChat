package com.invorel.blankchatpro.constants

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class DataStoreManager(private val dataStore: DataStore<Preferences>) {

  suspend fun saveFbProfileImageUrl(url: String) {
    dataStore.edit { preferences ->
      preferences[DataStoreKeys.fbProfileImageKey] = url
    }
  }

  val fbProfileImageUrl: Flow<String?> = dataStore.data.catch { exception ->
    if (exception is IOException) {
      emit(emptyPreferences())
    } else {
      throw exception
    }
  }.map { preferences ->
    preferences[DataStoreKeys.fbProfileImageKey]
  }

  suspend fun saveFCMToken(token: String) {
    dataStore.edit { preferences ->
      preferences[DataStoreKeys.fcmTokenKey] = token
    }
  }

  val fcmTokenKey: Flow<String?> = dataStore.data.catch { exception ->
    if (exception is IOException) {
      emit(emptyPreferences())
    } else {
      throw exception
    }
  }.map { preferences ->
    preferences[DataStoreKeys.fcmTokenKey]
  }
}

object DataStoreKeys {
  val fbProfileImageKey = stringPreferencesKey("FB_PROFILE_IMAGE_URL_PREF")
  val fcmTokenKey = stringPreferencesKey("FB_FCM_TOKEN_PREF")
}