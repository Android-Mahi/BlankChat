package com.invorel.blankchatpro.workers

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.invorel.blankchatpro.app.BlankApp
import com.invorel.blankchatpro.constants.DEFAULT_CHAT_ROOM_SEPARATOR
import com.invorel.blankchatpro.extensions.isNumberTypeChatRoom
import com.invorel.blankchatpro.local.repo.LocalRepo
import com.invorel.blankchatpro.local.repo.LocalRepo.LocalDbResult
import com.invorel.blankchatpro.mappers.toLocalModel
import com.invorel.blankchatpro.online.fb_collections.Message
import com.invorel.blankchatpro.utils.FirebaseUtils
import com.invorel.blankchatpro.utils.FirebaseUtils.ChatRoomExistStatus.Exist
import com.invorel.blankchatpro.utils.FirebaseUtils.ChatRoomExistStatus.NotExist
import com.invorel.blankchatpro.utils.FirebaseUtils.FireStoreResult
import com.invorel.blankchatpro.utils.FirebaseUtils.FireStoreResult.Success
import com.invorel.blankchatpro.workers.CreateChatRoomWorker.Companion
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

class AddMessageWorker(
  context: Context,
  workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters) {

  @OptIn(DelicateCoroutinesApi::class)
  override suspend fun doWork(): Result {

    val localRepo = LocalRepo((applicationContext as BlankApp).localDb)

    val senderNumber = inputData.getString(SENDER_NUMBER).orEmpty()

    if (senderNumber.isEmpty()) {
      val output =
        constructOutputData(errorMessage = "Got SenderNumber as Empty in AddMessageWorker")
      return Result.failure(output)
    }

    val receiverNumber = inputData.getString(RECEIVER_NUMBER).orEmpty()

    if (receiverNumber.isEmpty()) {
      val output =
        constructOutputData(errorMessage = "Got ReceiverNumber as Empty in AddMessageWorker")
      return Result.failure(output)
    }

    var roomId = inputData.getString(ROOM_ID).orEmpty()

    if (roomId.isEmpty()) {
      val output = constructOutputData(errorMessage = "Got RoomId as  Empty in AddMessageWorker")
      return Result.failure(output)
    }

    val messageJson = inputData.getString(MESSAGE_JSON_KEY).orEmpty()

    if (messageJson.isEmpty()) {
      val output =
        constructOutputData(errorMessage = "Got Message Json as  Empty in AddMessageWorker")
      return Result.failure(output)
    }

    val message = Gson().fromJson(messageJson, Message::class.java)

    val isCurrentChatRoomIsNumberType =
      inputData.getBoolean(IS_CURRENT_CHATROOM_IS_NUMBER_TYPE, false)

    if (isCurrentChatRoomIsNumberType) {

      //we already confirmed currentChatRoomIsNumber this is a safety check
      val checkExistNumberChatRoomResult =
        FirebaseUtils.checkIfNumberTypeChatRoomExistsOrNotInDb(
          senderTag = senderNumber,
          receiverTag = receiverNumber
        )

      if (checkExistNumberChatRoomResult is FireStoreResult.Error) {
        val output = constructOutputData(errorMessage = checkExistNumberChatRoomResult.errorMessage)
        return Result.retry()
      }

      when ((checkExistNumberChatRoomResult as Success).data) {
        NotExist -> {
          // Rare case due to we already found that i.e chatroom is number type
          // No Migration from Number to UserIdType Actions Needed we can directly send messages

          //For future proof and safety case adding below code
          val updateMessageChatRoomResult = FirebaseUtils.updateMessageInChatRoom(
            roomId = roomId,
            message = message
          )

          if (updateMessageChatRoomResult is FireStoreResult.Error) {
            val output =
              constructOutputData(errorMessage = updateMessageChatRoomResult.errorMessage)
            return Result.retry()
          }

          // we successfully updated userIdType Room's id to sender & receiver table
          return Result.success()
        }

        is Exist -> {

          val checkIfReceiverExistsWithLoginResult =
            FirebaseUtils.checkIfReceiverExistsWithLoginInFireBase(receiverNumber)

          if (checkIfReceiverExistsWithLoginResult is FireStoreResult.Error) {
            val output =
              constructOutputData(errorMessage = checkIfReceiverExistsWithLoginResult.errorMessage)
            return Result.retry()
          }

          val isReceiverExistsWithLogin =
            (checkIfReceiverExistsWithLoginResult as Success).data.first

          if (isReceiverExistsWithLogin) {

            if (FirebaseUtils.currentUser == null) {
              val output =
                constructOutputData(
                  errorMessage = "Got FirebaseUtils.currentUser as null"
                )
              return Result.failure(output)
            }

            val senderUserId = FirebaseUtils.currentUser!!.uid
            val receiverUserId = checkIfReceiverExistsWithLoginResult.data.second

            val userIdTypeChatRoomId = senderUserId.plus(DEFAULT_CHAT_ROOM_SEPARATOR).plus(receiverUserId)

            if (roomId.isNumberTypeChatRoom()) {

              val migrationWorkErrorData: Data = GlobalScope.async {

                val localChatRoomResult = localRepo.getLocalChatRoom(scope = GlobalScope, roomId)

                if (localChatRoomResult is LocalDbResult.LocalError) {
                  return@async constructOutputData(errorMessage = localChatRoomResult.errorMessage)
                }

                val oldLocalChatRoom = (localChatRoomResult as LocalDbResult.LocalSuccess).data

                val chatRoomMessageResult =
                  localRepo.getMessagesForChatRoom(scope = GlobalScope, roomId)

                if (chatRoomMessageResult is LocalDbResult.LocalError) {
                  return@async constructOutputData(errorMessage = chatRoomMessageResult.errorMessage)
                }

                val oldChatRoomMessages = (chatRoomMessageResult as LocalDbResult.LocalSuccess).data

                val oldChatReceiverResult =
                  localRepo.getReceiverDetailsOfRoom(scope = GlobalScope, roomId)

                if (oldChatReceiverResult is LocalDbResult.LocalError) {
                  return@async constructOutputData(errorMessage = oldChatReceiverResult.errorMessage)
                }

                val oldChatRoomReceiver = (oldChatReceiverResult as LocalDbResult.LocalSuccess).data

                val newLocalChatRoom = oldLocalChatRoom.copy(roomId = userIdTypeChatRoomId)
                val newLocalMessages =
                  oldChatRoomMessages.map { it.copy(roomId = userIdTypeChatRoomId) }
                val newLocalReceiverDetail = oldChatRoomReceiver.copy(roomId = userIdTypeChatRoomId, userId = receiverUserId)

                val deleteLocalChatRoomResult = localRepo.deleteChatRoomDetail(
                  scope = GlobalScope,
                  localChatRoom = oldLocalChatRoom
                )
                if (deleteLocalChatRoomResult is LocalDbResult.LocalError) {
                  return@async constructOutputData(errorMessage = deleteLocalChatRoomResult.errorMessage)
                }
                val saveLocalChatRoomResult =
                  localRepo.saveChatRoomDetail(scope = GlobalScope, chatRoom = newLocalChatRoom)
                if (saveLocalChatRoomResult is LocalDbResult.LocalError) {
                  return@async constructOutputData(errorMessage = saveLocalChatRoomResult.errorMessage)
                }

                val saveLocalReceiverDetailResult = localRepo.saveReceiverDetail(
                  scope = GlobalScope,
                  localReceiverDetails = newLocalReceiverDetail
                )
                if (saveLocalReceiverDetailResult is LocalDbResult.LocalError) {
                  return@async constructOutputData(errorMessage = saveLocalReceiverDetailResult.errorMessage)
                }

                if (newLocalMessages.size == 1) {
                  val localSaveMessageResult = localRepo.saveMessage(
                    scope = GlobalScope,
                    localMessage = newLocalMessages.first().toLocalModel()
                  )

                  if (localSaveMessageResult is LocalDbResult.LocalError) {
                    return@async constructOutputData(errorMessage = localSaveMessageResult.errorMessage)
                  }

                  // we successfully saved new localMessage, newReceiverDetails, newLocalChatRoom in LocalDatabase


                  // we successfully deleted LocalChatRoom, ReceiverDetail, Messages
                  constructOutputData(errorMessage = "")
                } else {

                  val localSaveMessageResult = localRepo.saveMessages(
                    scope = GlobalScope,
                    localMessages = newLocalMessages.map { it.toLocalModel() }
                  )
                  if (localSaveMessageResult is LocalDbResult.LocalError) {
                    return@async constructOutputData(errorMessage = localSaveMessageResult.errorMessage)
                  }
                  // we successfully saved new localMessage, newReceiverDetails, newLocalChatRoom in LocalDatabase

                  return@async constructOutputData(errorMessage = "")
                }
              }.await()

              val localMigrationWorkError = migrationWorkErrorData.getString(CreateChatRoomWorker.CREATE_CHAT_OUTPUT_ERROR_MESSAGE).orEmpty()

              if (localMigrationWorkError.isNotEmpty()) {
                return Result.failure(constructOutputData(errorMessage = localMigrationWorkError))
              }

            }

            //Receiver Exists in DB with Login we can migration chatRoom from NumberType to UserIdType
            val numberTypeRoomId = (checkExistNumberChatRoomResult.data as Exist).roomId
            // Sender previously sent message to receiver when receiver not logged In
            // now we have receiver UserId we can delete old Number type chatRoom and
            // create UserId Type chatRoom with existing number type chatRoom Data
            val changeExistingNumberChatRoomResult =
              FirebaseUtils.changeExistingRoomTypeFromNumberToUserId(
                receiverId = receiverUserId,
                existingRoomId = numberTypeRoomId
              )

            if (changeExistingNumberChatRoomResult is FireStoreResult.Error) {
              val output =
                constructOutputData(errorMessage = changeExistingNumberChatRoomResult.errorMessage)
              return Result.retry()
            }

            // We have successfully migrated from NumberType ChatRoom to UserIdType ChatRoom
            roomId = (changeExistingNumberChatRoomResult as Success).data

            //due to we deleted old document the fields of the document will be deleted so we need to update those again
            val updateParticipantsResult = FirebaseUtils.updateParticipantsDetailsInChatRoom(
              senderNumber = senderNumber,
              receiverNumber = receiverNumber,
              chatRoomId = roomId
            )

            if (updateParticipantsResult is FireStoreResult.Error) {
              val output = constructOutputData(errorMessage = updateParticipantsResult.errorMessage)
              return Result.retry()
            }

            // we saved participants numbers in ChatRoom
            //Todo change from NumberType to UserIdType in LocalDB also
            // affected Tables: ChatRoom, LocalReceiverDetail, ChatMessage
          }

          val updateMessageChatRoomResult = FirebaseUtils.updateMessageInChatRoom(
            roomId = roomId,
            message = message
          )

          if (updateMessageChatRoomResult is FireStoreResult.Error) {
            val output =
              constructOutputData(errorMessage = updateMessageChatRoomResult.errorMessage)
            return Result.retry()
          }

          // we successfully updated userIdType Room's id to sender & receiver table
          val successOutput = constructOutputData(roomId = roomId, messageId = message.id)
          return Result.success(successOutput)
        }
      }
    } else {

      val updateMessageChatRoomResult = FirebaseUtils.updateMessageInChatRoom(
        roomId = roomId,
        message = message
      )

      if (updateMessageChatRoomResult is FireStoreResult.Error) {
        val output = constructOutputData(errorMessage = updateMessageChatRoomResult.errorMessage)
        return Result.retry()
      }

      // we successfully updated userIdType Room's id to sender & receiver table
      val successOutput = constructOutputData(roomId = roomId, messageId = message.id)
      return Result.success(successOutput)
    }
  }

  companion object {
    const val SENDER_NUMBER = "SENDER_NUMBER"
    const val RECEIVER_NUMBER = "RECEIVER_NUMBER"
    const val IS_CURRENT_CHATROOM_IS_NUMBER_TYPE = "IS_CURRENT_CHATROOM_IS_NUMBER_TYPE"
    const val ROOM_ID = "ROOM_ID"
    const val MESSAGE_JSON_KEY = "MESSAGE_JSON_KEY"

    const val ADD_MESSAGE_OUTPUT_ROOM_ID = "ROOM_ID"
    const val ADD_MESSAGE_OUTPUT_ERROR_MESSAGE = "ERROR_MESSAGE"
    const val ADD_MESSAGE_OUTPUT_MESSAGE_ID = "MESSAGE_ID"

    @SuppressLint("RestrictedApi")
    fun constructOutputData(
      errorMessage: String? = null,
      roomId: String? = null,
      messageId: Int = -1,
    ): Data {
      val builder = Data.Builder()
      if (errorMessage != null) {
        builder.put(ADD_MESSAGE_OUTPUT_ERROR_MESSAGE, errorMessage)
      }
      if (roomId != null) {
        builder.put(ADD_MESSAGE_OUTPUT_ROOM_ID, roomId)
      }
      if (messageId != -1) {
        builder.put(ADD_MESSAGE_OUTPUT_MESSAGE_ID, messageId)
      }
      return builder.build()
    }
  }

  sealed class AddMessageResult {
    data class AddMessageSuccess(val roomId: String, val messageId: Int) : AddMessageResult()
    data class AddMessageFailed(val errorMessage: String) : AddMessageResult()
  }
}