package com.invorel.blankchatpro.local.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.invorel.blankchatpro.local.tables.LocalMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalChatMessageDao {

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  fun insertMessage(message: LocalMessage): Long

  @Query("SELECT * FROM `BLANK-MESSAGE` WHERE roomId = :roomId")
  fun getChatRoomMessagesFlow(roomId: String): Flow<List<LocalMessage>>

  @Query("SELECT * FROM `BLANK-MESSAGE` WHERE roomId = :roomId")
  fun getChatRoomMessages(roomId: String): List<LocalMessage>

  @Query("SELECT * FROM `BLANK-MESSAGE` WHERE roomId = :roomId AND id = :messageId")
  fun getMessageByRoomIdAndMessageId(roomId: String, messageId: Int): LocalMessage?

  @Update
  fun updateMessage(message: LocalMessage): Int

  @Delete
  fun deleteMessage(message: LocalMessage): Int

  @Query("SELECT * FROM `BLANK-MESSAGE` WHERE roomId =:roomId ORDER BY id DESC LIMIT 1")
  fun getLatestMessageOfChatRoom(roomId: String): LocalMessage?

  @Transaction
  fun insertMessages(localMessages: List<LocalMessage>): Boolean {
    for (message in localMessages) {
      val result = insertMessage(message = message)
      if (result == -1L) {
        return false
      }
    }
    return true
  }
}