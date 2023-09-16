package com.invorel.blankchatpro.app

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.invorel.blankchatpro.constants.DEFAULT_DATA_STORE_FILE_NAME
import com.invorel.blankchatpro.constants.DEFAULT_ROOM_DB_NAME
import com.invorel.blankchatpro.constants.DataStoreManager
import com.invorel.blankchatpro.extensions.showToast
import com.invorel.blankchatpro.local.database.BlankLocalDatabase
import com.invorel.blankchatpro.managers.LocalDbManager
import com.invorel.blankchatpro.utils.FirebaseUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class BlankApp : Application() {

  lateinit var dataStore: DataStore<Preferences>

  lateinit var localDb: BlankLocalDatabase

  override fun onCreate() {
    super.onCreate()

    dataStore = PreferenceDataStoreFactory.create(
      produceFile = { this.preferencesDataStoreFile(DEFAULT_DATA_STORE_FILE_NAME) }
    )

    localDb = LocalDbManager.getDatabase(this)

    FirebaseUtils.getCurrentFCMToken(
      onTokenFetched = { token ->
        val manager = DataStoreManager(dataStore)
        GlobalScope.launch {
          manager.saveFCMToken(token)
        }
    }, onTokenFetchFailed = { errorMessage ->
      showToast(errorMessage)
    })
  }

  override fun onTerminate() {
    super.onTerminate()
    LocalDbManager.getDatabase(this).close()
  }
}