package com.invorel.blankchatpro.managers

import android.content.Context
import androidx.room.Room
import com.invorel.blankchatpro.constants.DEFAULT_ROOM_DB_NAME
import com.invorel.blankchatpro.local.database.BlankLocalDatabase

object LocalDbManager {

  private var instance: BlankLocalDatabase? = null

  fun getDatabase(context: Context): BlankLocalDatabase {
    return instance ?: synchronized(this) {
      instance ?: buildDatabase(context).also { instance = it }
    }
  }

  private fun buildDatabase(context: Context): BlankLocalDatabase {
    return Room.databaseBuilder(
      context.applicationContext,
      BlankLocalDatabase::class.java,
      DEFAULT_ROOM_DB_NAME
    ).build()
  }
}