package com.invorel.blankchatpro.local.repo

import com.invorel.blankchatpro.local.database.BlankLocalDatabase
import com.invorel.blankchatpro.local.tables.LocalChatRoom
import com.invorel.blankchatpro.local.tables.LocalMessage
import com.invorel.blankchatpro.local.tables.LocalReceiverDetail
import com.invorel.blankchatpro.local.tables.LocalUser
import com.invorel.blankchatpro.mappers.toLatestMessageUIModel
import com.invorel.blankchatpro.mappers.toUIModel
import com.invorel.blankchatpro.mappers.toUiModel
import com.invorel.blankchatpro.viewModels.HomeChatUIModel
import com.invorel.blankchatpro.viewModels.MessageUIModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.lang.Exception

class LocalRepo(private val localDb: BlankLocalDatabase) {

  fun saveUserDetails(
    scope: CoroutineScope,
    user: LocalUser,
    onFailed: (String) -> Unit,
    onSuccess: () -> Unit,
  ) {
    scope.launch(Dispatchers.IO) {
      val result = localDb.userDao().insertUser(user)
      if (result == -1L) {
        onFailed.invoke("Failed to Save User Details Locally")
        return@launch
      }
      onSuccess.invoke()
    }
  }

  suspend fun saveChatRoomDetail(
    scope: CoroutineScope,
    chatRoom: LocalChatRoom,
  ): LocalDbResult<Unit> {
    val result = scope.async(Dispatchers.IO) {
      localDb.chatRoomDao().insertChatRoom(chatRoom)
    }.await()
    return if (result == -1L) {
      LocalDbResult.LocalError("Failed to Save Local ChatRoom Detail")
    } else {
      LocalDbResult.LocalSuccess(Unit)
    }
  }

  fun isChatRoomAlreadyExists(
    scope: CoroutineScope,
    receiverNumber: String,
    onNoChatRoomExists: () -> Unit,
    onChatRoomExists: (String) -> Unit,
  ) {
    scope.launch(Dispatchers.IO) {
      val roomId = localDb.receiverDetailDao().checkIfChatRoomExists(receiverNumber)

      if (roomId == null) {
        onNoChatRoomExists.invoke()
      } else {
        onChatRoomExists.invoke(roomId)
      }
    }
  }

  private var messageCollectJob: Job? = null

  /* suspend fun getMessagesForChatRoomFlow(
     scope: CoroutineScope,
     roomId: String,
     onNoMessageFound: () -> Unit,
     onMessagesFetched: (List<MessageUIModel>) -> Unit,
   ) {
     messageCollectJob?.cancel()
     messageCollectJob = scope.launch(Dispatchers.IO) {
       val messages = localDb.messageDao().getChatRoomMessagesFlow(roomId)
       messages.collect {
         if (it.isEmpty()) {
           onNoMessageFound.invoke()
         } else {
           val uiList = it.map { it.toUIModel() }
           onMessagesFetched.invoke(uiList)
         }
       }
     }
   }*/

  suspend fun getMessagesForChatRoom(
    scope: CoroutineScope,
    roomId: String,
  ): LocalDbResult<List<MessageUIModel>> {
    val localMessages = scope.async(Dispatchers.IO) {
      localDb.messageDao().getChatRoomMessages(roomId)
    }.await()

    return if (localMessages.isEmpty()) {
      LocalDbResult.LocalError("empty local Messages")
    } else {
      val uiList = localMessages.map { it.toUIModel() }
      LocalDbResult.LocalSuccess(uiList)
    }
  }

  suspend fun saveMessage(
    scope: CoroutineScope,
    localMessage: LocalMessage,
  ): LocalDbResult<Int> {
    val result = scope.async(Dispatchers.IO) {
      localDb.messageDao().insertMessage(localMessage)
    }.await()
    return if (result == -1L) {
      LocalDbResult.LocalError("Failed save Local Message")
    } else {
      LocalDbResult.LocalSuccess(localMessage.id)
    }
  }

  suspend fun saveMessages(
    scope: CoroutineScope,
    localMessages: List<LocalMessage>,
  ): LocalDbResult<Unit> {
    val isSaved = scope.async(Dispatchers.IO) {
      localDb.messageDao().insertMessages(localMessages)
    }.await()
    return if (isSaved) {
      LocalDbResult.LocalSuccess(Unit)
    } else {
      LocalDbResult.LocalError("Failed to save local messages with transaction")
    }
  }

  suspend fun saveReceiverDetail(
    scope: CoroutineScope,
    localReceiverDetails: LocalReceiverDetail,
  ): LocalDbResult<Unit> {
    val result = scope.async(Dispatchers.IO) {
      localDb.receiverDetailDao().insertReceiver(localReceiverDetails)
    }.await()
    return if (result == -1L) {
      LocalDbResult.LocalError("Failed to save Receiver Details")
    } else {
      LocalDbResult.LocalSuccess(Unit)
    }
  }

  fun updateMessageStatus(
    scope: CoroutineScope,
    status: Int,
    roomId: String,
    messageId: Int,
    onFailed: (String) -> Unit,
    onSuccess: () -> Unit,
  ) {
    scope.launch(Dispatchers.IO) {
      var localMessage = localDb.messageDao().getMessageByRoomIdAndMessageId(roomId, messageId)

      if (localMessage == null) {
        onFailed.invoke("Got localMessage as null while update message status")
      } else {
        localMessage = localMessage.copy(status = status)

        val result = localDb.messageDao().updateMessage(localMessage)

        if (result == -1) {
          onFailed.invoke("Failed to update message status in local DB")
        } else {
          onSuccess.invoke()
        }
      }
    }
  }

  fun getChatRoomsForCurrentUser(
    scope: CoroutineScope,
    userId: String,
    onFailed: (String) -> Unit,
    onSuccess: (List<HomeChatUIModel>) -> Unit,
  ) {
    scope.launch(Dispatchers.IO) {

      val localChatRoomList = localDb.chatRoomDao().getChatRoomsForCurrentUser(userId)

      val finalHomeChatList = localChatRoomList.map {
        val roomId = it.roomId
        val receiverDetail = localDb.receiverDetailDao().getReceiverDetail(roomId)?.toUiModel()

        val latestMessage =
          localDb.messageDao().getLatestMessageOfChatRoom(roomId)?.toLatestMessageUIModel()

        if (receiverDetail == null) {
          onFailed.invoke("Got receiverDetails as null while Mapping HomeChatUIModel")
          return@launch
        }

        if (latestMessage == null) {
          onFailed.invoke("Got latestMessage as null while Mapping HomeChatUIModel")
          return@launch
        }

        //roomCreatedAt value only stored in Firebase collection not needed in local version we are storing last updated roomCreatedAt in profile
        HomeChatUIModel(roomId, receiverDetail, latestMessage, roomCreatedAt = -1)
      }

      onSuccess.invoke(finalHomeChatList)

    }
  }

  suspend fun getChatRoomIdsForCurrentUser(
    scope: CoroutineScope,
    userId: String,
  ): LocalDbResult<List<String>> {
    return try {
      val localChatRoomList =
        scope.async(Dispatchers.IO) { localDb.chatRoomDao().getChatRoomsForCurrentUser(userId) }
          .await()

      val currentUserChatRoomIds = localChatRoomList.map { it.roomId }
      LocalDbResult.LocalSuccess(currentUserChatRoomIds)
    } catch (e: Exception) {
      LocalDbResult.LocalError(
        e.message ?: "exception got while getting LocalChatRoomIds for Current User Id: $userId"
      )
    }
  }

  fun getUserByUserId(
    scope: CoroutineScope,
    userId: String,
    onFailed: (String) -> Unit,
    onSuccess: (LocalUser) -> Unit,
  ) {
    scope.launch(Dispatchers.IO) {
      val user = localDb.userDao().getUserByUserId(userId)
      if (user == null) {
        onFailed.invoke("Got null for the user while get using userId")
      } else {
        onSuccess.invoke(user)
      }
    }
  }

  fun updateLocalUser(
    scope: CoroutineScope,
    user: LocalUser,
    onFailed: (String) -> Unit,
    onSuccess: () -> Unit,
  ) {
    scope.launch(Dispatchers.IO) {
      val result = localDb.userDao().updateUser(user)
      if (result == -1) {
        onFailed.invoke("Couldn't update user object locally")
      } else {
        onSuccess.invoke()
      }
    }
  }

  suspend fun getLocalChatRoom(
    scope: CoroutineScope,
    roomId: String,
  ): LocalDbResult<LocalChatRoom> {

    val localChatRoom = scope.async(Dispatchers.IO) {
      localDb.chatRoomDao().getChatRoomByRoomId(roomId = roomId)
    }.await()

    return if (localChatRoom == null) {
      LocalDbResult.LocalError("Got LocalChatRoom as null")
    } else {
      LocalDbResult.LocalSuccess(localChatRoom)
    }
  }

  suspend fun getReceiverDetailsOfRoom(
    scope: CoroutineScope,
    roomId: String,
  ): LocalDbResult<LocalReceiverDetail> {

    val localReceiverDetails = scope.async(Dispatchers.IO) {
      localDb.receiverDetailDao().getReceiverDetail(roomId)
    }.await()

    return if (localReceiverDetails == null) {
      LocalDbResult.LocalError("Got localReceiverDetails as null")
    } else {
      LocalDbResult.LocalSuccess(localReceiverDetails)
    }
  }

  fun getLatestRoomUpdatedAtValueInProfile(
    scope: CoroutineScope,
    userPhoneNo: String,
    onFailed: (String) -> Unit,
    onSuccess: (Long) -> Unit,
  ) {
    scope.launch(Dispatchers.IO) {
      val lastRoomUpdatedAt = localDb.userDao().getLastRoomUpdatedAt(userPhoneNo)
      if (lastRoomUpdatedAt == null) {
        onFailed.invoke("Got lastRoomUpdatedAt as null while retrieving LastRoomUpdated Local value in Profile")
      } else {
        onSuccess.invoke(lastRoomUpdatedAt)
      }
    }
  }

  //once we delete chatRoom the messages & receiverDetails will be deleted due to foreign key relationShip
  suspend fun deleteChatRoomDetail(
    scope: CoroutineScope,
    localChatRoom: LocalChatRoom,
  ): LocalDbResult<Unit> {
    val result = scope.async(Dispatchers.IO) {
      localDb.chatRoomDao().deleteChatRoom(localChatRoom)
    }.await()
    return if (result == 1) {
      LocalDbResult.LocalSuccess(Unit)
    } else {
      LocalDbResult.LocalError("couldn't delete localChatRoom")
    }
  }

  sealed class LocalDbResult<out T> {
    data class LocalSuccess<out T>(val data: T) : LocalDbResult<T>()
    data class LocalError(val errorMessage: String) : LocalDbResult<Nothing>()
  }
}