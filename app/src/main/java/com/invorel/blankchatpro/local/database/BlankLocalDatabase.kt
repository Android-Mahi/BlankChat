package com.invorel.blankchatpro.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.invorel.blankchatpro.local.daos.ContactsDao
import com.invorel.blankchatpro.local.tables.Contacts
import com.invorel.blankchatpro.local.typeconverters.BitmapTypeConverter

@Database(entities = [Contacts::class], version = 1, exportSchema = false)
@TypeConverters(BitmapTypeConverter::class)
abstract class BlankLocalDatabase: RoomDatabase() {
  abstract fun contactsDao(): ContactsDao
}