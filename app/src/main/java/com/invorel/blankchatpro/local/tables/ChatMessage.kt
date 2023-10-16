package com.invorel.blankchatpro.local.tables

import androidx.room.Entity
import androidx.room.ForeignKey
import com.invorel.blankchatpro.constants.DEFAULT_MESSAGE_TABLE_NAME

@Entity(
  tableName = DEFAULT_MESSAGE_TABLE_NAME, foreignKeys = [ForeignKey(
    entity = LocalChatRoom::class,
    parentColumns = ["roomId"],
    childColumns = ["roomId"],
    onDelete = ForeignKey.CASCADE
  )], primaryKeys = ["roomId", "id"]
)
data class LocalMessage(
  val id: Int,
  val roomId: String,
  val message: String,
  val senderId: String,
  val receiverId: String,
  val sentTime: Long,
  val receivedTime: Long,
  var status: Int,
  val isSentByMessageMode: Boolean,
)