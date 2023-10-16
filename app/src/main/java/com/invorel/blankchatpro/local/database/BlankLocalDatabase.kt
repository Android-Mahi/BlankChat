package com.invorel.blankchatpro.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.invorel.blankchatpro.local.daos.ContactsDao
import com.invorel.blankchatpro.local.daos.LocalChatMessageDao
import com.invorel.blankchatpro.local.daos.LocalChatRoomDao
import com.invorel.blankchatpro.local.daos.LocalReceiverDetailDao
import com.invorel.blankchatpro.local.daos.LocalUsersDao
import com.invorel.blankchatpro.local.tables.Contacts
import com.invorel.blankchatpro.local.tables.LocalChatRoom
import com.invorel.blankchatpro.local.tables.LocalMessage
import com.invorel.blankchatpro.local.tables.LocalReceiverDetail
import com.invorel.blankchatpro.local.tables.LocalUser
import com.invorel.blankchatpro.local.typeconverters.BitmapTypeConverter

@Database(
  entities = [Contacts::class, LocalUser::class, LocalChatRoom::class, LocalReceiverDetail::class, LocalMessage::class],
  version = 1,
  exportSchema = false
)
@TypeConverters(BitmapTypeConverter::class)
abstract class BlankLocalDatabase : RoomDatabase() {
  abstract fun contactsDao(): ContactsDao
  abstract fun userDao(): LocalUsersDao
  abstract fun chatRoomDao(): LocalChatRoomDao
  abstract fun receiverDetailDao(): LocalReceiverDetailDao
  abstract fun messageDao(): LocalChatMessageDao
}