package com.invorel.blankchatpro.local.tables

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.invorel.blankchatpro.constants.DEFAULT_CHAT_ROOM_TABLE_NAME

@Entity(
  tableName = DEFAULT_CHAT_ROOM_TABLE_NAME, foreignKeys = [ForeignKey(
    entity = LocalUser::class,
    parentColumns = ["userId"],
    childColumns = ["userId"],
    onDelete = ForeignKey.CASCADE
  )]
)
data class LocalChatRoom(
  @PrimaryKey val roomId: String,
  val userId: String,
  val lastMessageUpdatedAt: Long,
)