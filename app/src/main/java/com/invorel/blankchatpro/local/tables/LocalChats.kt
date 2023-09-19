package com.invorel.blankchatpro.local.tables

import androidx.room.Entity
import com.invorel.blankchatpro.constants.DEFAULT_CHAT_TABLE_NAME
import com.invorel.blankchatpro.online.fb_collections.Message

@Entity(tableName = DEFAULT_CHAT_TABLE_NAME)
data class LocalChats (
  val roomId: String,
  val senderName: String,
  val receiverName: String,
  val messages: List<Message>,
  val lastUpdatedAt: Long
)