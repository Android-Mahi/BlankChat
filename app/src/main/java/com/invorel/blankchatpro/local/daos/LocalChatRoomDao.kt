package com.invorel.blankchatpro.local.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.invorel.blankchatpro.local.tables.LocalChatRoom

@Dao
interface LocalChatRoomDao {

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  fun insertChatRoom(localChatRoom: LocalChatRoom): Long

  @Query("SELECT * FROM `BLANK-CHATROOM` WHERE userId = :userId")
  fun getChatRoomsForCurrentUser(userId: String): List<LocalChatRoom>

  @Query("SELECT * FROM `BLANK-CHATROOM` WHERE roomId = :roomId")
  fun getChatRoomByRoomId(roomId: String): LocalChatRoom?

  @Delete
  fun deleteChatRoom(localChatRoom: LocalChatRoom): Int
}