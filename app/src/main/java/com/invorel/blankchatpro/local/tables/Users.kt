package com.invorel.blankchatpro.local.tables

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.invorel.blankchatpro.constants.DEFAULT_USER_TABLE_NAME

@Entity(tableName = DEFAULT_USER_TABLE_NAME)
data class LocalUser(
  @PrimaryKey val userId: String,
  val number: String,
  val lastRoomCreatedAt: Long,
)
