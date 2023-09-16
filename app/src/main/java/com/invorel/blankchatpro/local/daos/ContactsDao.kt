package com.invorel.blankchatpro.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.invorel.blankchatpro.local.tables.Contacts

@Dao
interface ContactsDao {

  @Insert (onConflict = OnConflictStrategy.REPLACE)
  fun insertContacts(contacts: List<Contacts>): List<Long>

  @Query("SELECT * FROM `BLANK-CONTACTS` ORDER BY name ASC")
  fun getContacts(): List<Contacts>

}