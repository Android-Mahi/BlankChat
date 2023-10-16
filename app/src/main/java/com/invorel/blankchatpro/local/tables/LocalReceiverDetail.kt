package com.invorel.blankchatpro.local.tables

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.invorel.blankchatpro.constants.DEFAULT_RECEIVER_TABLE_NAME

@Entity(
  tableName = DEFAULT_RECEIVER_TABLE_NAME, foreignKeys = [ForeignKey(
    entity = LocalChatRoom::class,
    parentColumns = ["roomId"],
    childColumns = ["roomId"],
    onDelete = ForeignKey.CASCADE
  )]
)
data class LocalReceiverDetail(
  @PrimaryKey val number: String,
  val userId: String,
  val roomId: String,
  val name: String,
  val photo: String,
  val lastProfileUpdatedAt: Long,
)
