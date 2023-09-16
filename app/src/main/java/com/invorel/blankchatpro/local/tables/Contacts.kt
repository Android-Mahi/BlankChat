package com.invorel.blankchatpro.local.tables

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.invorel.blankchatpro.constants.DEFAULT_CONTACT_TABLE_NAME

@Entity(tableName = DEFAULT_CONTACT_TABLE_NAME)
data class Contacts(
  @PrimaryKey(autoGenerate = false) val id: Long,
  val name: String,
  val number: String,
  val photo: Bitmap?,
)
