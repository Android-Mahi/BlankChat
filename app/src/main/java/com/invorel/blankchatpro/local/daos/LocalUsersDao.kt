package com.invorel.blankchatpro.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.invorel.blankchatpro.local.tables.LocalUser

@Dao
interface LocalUsersDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insertUser(user: LocalUser): Long

  @Update
  fun updateUser(user: LocalUser): Int

  @Query("SELECT * FROM `BLANK-USERS` WHERE userId = :userId")
  fun getUserByUserId(userId: String): LocalUser?

  @Query("SELECT lastRoomCreatedAt FROM `BLANK-USERS` WHERE number = :phoneNo")
  fun getLastRoomUpdatedAt(phoneNo: String): Long?
}