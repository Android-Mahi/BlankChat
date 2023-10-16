package com.invorel.blankchatpro.local.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.invorel.blankchatpro.local.tables.LocalReceiverDetail

@Dao
interface LocalReceiverDetailDao {

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  fun insertReceiver(localReceiverDetails: LocalReceiverDetail): Long

  @Query("SELECT roomId FROM `BLANK-RECEIVER` WHERE number = :receiverNumber")
  fun checkIfChatRoomExists(receiverNumber: String): String?

  @Query("SELECT * FROM `BLANK-RECEIVER` WHERE roomId = :roomId")
  fun getReceiverDetail(roomId: String): LocalReceiverDetail?
}