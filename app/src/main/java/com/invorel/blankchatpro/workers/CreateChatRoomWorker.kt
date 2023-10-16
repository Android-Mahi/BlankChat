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
import com.invorel.blankchatpro.utils.FirebaseUtils.ReceiverExistStatus.RECEIVER_EXISTS_WITHOUT_LOGIN
import com.invorel.blankchatpro.utils.FirebaseUtils.ReceiverExistStatus.RECEIVER_EXISTS_WITH_LOGIN
import com.invorel.blankchatpro.utils.FirebaseUtils.ReceiverExistStatus.RECEIVER_NOT_EXISTS
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.json.JSONObject

class CreateChatRoomWorker(
  context: Context,
  params: WorkerParameters,
) : CoroutineWorker(context, params) {
  @OptIn(DelicateCoroutinesApi::class)
  override suspend fun doWork(): Result {

    val localRepo = LocalRepo((applicationContext as BlankApp).localDb)

    val receiverNumber = inputData.getString(RECEIVER_NUMBER).orEmpty()

    if (receiverNumber.isEmpty()) {
      val output = constructOutputData(errorMessage = "Got Receiver Number as empty for inputData")
      return Result.failure(output)
    }

    val receiverName = inputData.getString(RECEIVER_NAME).orEmpty()

    if (receiverName.isEmpty()) {
      val output = constructOutputData(errorMessage = "Got Receiver Name as empty for inputData")
      return Result.failure(output)
    }

    val receiverImage = inputData.getString(RECEIVER_IMAGE).orEmpty()

    val senderNumber = inputData.getString(SENDER_NUMBER).orEmpty()

    if (senderNumber.isEmpty()) {
      val output = constructOutputData(errorMessage = "Got Sender Number as empty for inputData")
      return Result.failure(output)
    }

    val lastRoomCreatedAt = inputData.getLong(ROOM_CREATED_AT, -1L)

    if (lastRoomCreatedAt == -1L) {
      val output =
        constructOutputData(errorMessage = "Got lastRoomCreatedAt as empty for inputData")
      return Result.failure(output)
    }

    val messageJson = inputData.getString(MESSAGE_JSON_KEY).orEmpty()

    if (messageJson.isEmpty()) {
      val output = constructOutputData(errorMessage = "Got Message Json as empty for inputData")
      return Result.failure(output)
    }

    var message = Gson().fromJson(messageJson, Message::class.java)

    val receiverStatus = FirebaseUtils.checkReceiverDetailsInFireBase(receiverNumber)

    if (receiverStatus is FireStoreResult.Error) {
      val output = constructOutputData(errorMessage = receiverStatus.errorMessage)
      return Result.retry()
    }

    when ((receiverStatus as Success).data) {
      is RECEIVER_NOT_EXISTS -> {

        val createChatRoomResult =
          FirebaseUtils.createChatRoomInFirebase(
            chatRoomId = senderNumber.plus(DEFAULT_CHAT_ROOM_SEPARATOR).plus(receiverNumber),
            //Todo get org switch status and use here
            message = message
          )

        if (createChatRoomResult is FireStoreResult.Error) {
          val output = constructOutputData(errorMessage = createChatRoomResult.errorMessage)
          return Result.retry()
        }

        val updateRoomCreatedAtInProfileResult =
          FirebaseUtils.updateLastChatRoomUpdatedAtInProfile(lastRoomCreatedAt)

        if (updateRoomCreatedAtInProfileResult is FireStoreResult.Error) {
          val output =
            constructOutputData(errorMessage = updateRoomCreatedAtInProfileResult.errorMessage)
          return Result.retry()
        }

        val createdChatRoomId = (createChatRoomResult as Success).data

        val updateRoomCreatedAtInChatRoomResult =
          FirebaseUtils.updateLastChatRoomUpdatedAtInChatRoom(
            roomId = createdChatRoomId, currentTimeInMillis = lastRoomCreatedAt
          )

        if (updateRoomCreatedAtInChatRoomResult is FireStoreResult.Error) {
          val output =
            constructOutputData(errorMessage = updateRoomCreatedAtInChatRoomResult.errorMessage)
          return Result.retry()
        }

        val receiverChatRoomIds = mutableListOf<String>()
        receiverChatRoomIds.add(createdChatRoomId)
        /*viewModelScope.launch {
          delay(50)
        }*/

        val createNotLoggedInReceiverResult = FirebaseUtils.createNotLoggedInReceiverDetailsInDB(
          mobileNumber = receiverNumber,
          name = receiverName,
          photo = receiverImage,
          chatRoomIds = receiverChatRoomIds
        )

        if (createNotLoggedInReceiverResult is FireStoreResult.Error) {
          val output =
            constructOutputData(errorMessage = createNotLoggedInReceiverResult.errorMessage)
          return Result.retry()
        }

        //Getting existing Room Ids for the Sender (current User)
        val senderExistsChatRoomIdsResult = FirebaseUtils.getExistingChatRoomIdsForUser(
          mobileNumber = senderNumber
        )

        if (senderExistsChatRoomIdsResult is FireStoreResult.Error) {
          val output =
            constructOutputData(errorMessage = senderExistsChatRoomIdsResult.errorMessage)
          return Result.retry()
        }

        val senderChatRoomIds = mutableListOf<String>()
        senderChatRoomIds.addAll((senderExistsChatRoomIdsResult as Success).data)
        senderChatRoomIds.add(createdChatRoomId)
        /*viewModelScope.launch {
          delay(50)
        }*/
        //update latest room Ids appended with old Ids in Firebase
        val updateChatRoomIdResult =
          FirebaseUtils.updateChatRoomIdInSendUserDocument(senderChatRoomIds)

        if (updateChatRoomIdResult is FireStoreResult.Error) {
          val output = constructOutputData(errorMessage = updateChatRoomIdResult.errorMessage)
          return Result.retry()
        }

        // we updated chat room in Both receiver and sender
        val updateParticipantsResult = FirebaseUtils.updateParticipantsDetailsInChatRoom(
          senderNumber = senderNumber,
          receiverNumber = receiverNumber,
          chatRoomId = createdChatRoomId
        )

        if (updateParticipantsResult is FireStoreResult.Error) {
          val output = constructOutputData(errorMessage = updateParticipantsResult.errorMessage)
          return Result.retry()
        }

        // we saved participants numbers in ChatRoom
        val successOutput = constructOutputData(roomId = createdChatRoomId)
        //Todo send invite message to receiver via SmsManager API
        return Result.success(successOutput)
      }

      is RECEIVER_EXISTS_WITHOUT_LOGIN -> {

        val checkExistChatRoomResult = FirebaseUtils.checkIfNumberTypeChatRoomExistsOrNotInDb(
          senderTag = senderNumber,
          receiverTag = receiverNumber
        )

        if (checkExistChatRoomResult is FireStoreResult.Error) {
          val output = constructOutputData(errorMessage = checkExistChatRoomResult.errorMessage)
          return Result.retry()
        }

        return when ((checkExistChatRoomResult as Success).data) {
          is Exist -> {
            //No need to create
            //we have Number type Chat Room for not logged In User
            // Rare case bcz most of the time user and receiver are logged In into Blank Chat
            val successOutput =
              constructOutputData(roomId = (checkExistChatRoomResult.data as Exist).roomId)
            Result.success(successOutput)
          }

          NotExist -> {
            //Another user may be created chatRoom for this receiver in DataBase.
            // So we need to create chatRoom With this sender Number
            val numberTypeRoomId =
              senderNumber.plus(DEFAULT_CHAT_ROOM_SEPARATOR).plus(receiverNumber)

            val createChatRoomResult = FirebaseUtils.createChatRoomInFirebase(
              chatRoomId = numberTypeRoomId,
              message = message
            )

            if (createChatRoomResult is FireStoreResult.Error) {
              val output = constructOutputData(errorMessage = createChatRoomResult.errorMessage)
              return Result.retry()
            }

            val updateRoomCreatedAtInProfileResult =
              FirebaseUtils.updateLastChatRoomUpdatedAtInProfile(lastRoomCreatedAt)

            if (updateRoomCreatedAtInProfileResult is FireStoreResult.Error) {
              val output =
                constructOutputData(errorMessage = updateRoomCreatedAtInProfileResult.errorMessage)
              return Result.retry()
            }

            val createdChatRoomId = (createChatRoomResult as Success).data

            val updateRoomCreatedAtInChatRoomResult =
              FirebaseUtils.updateLastChatRoomUpdatedAtInChatRoom(
                roomId = createdChatRoomId, currentTimeInMillis = lastRoomCreatedAt
              )

            if (updateRoomCreatedAtInChatRoomResult is FireStoreResult.Error) {
              val output =
                constructOutputData(errorMessage = updateRoomCreatedAtInChatRoomResult.errorMessage)
              return Result.retry()
            }

            val successOutput = constructOutputData(roomId = createdChatRoomId)
            Result.success(successOutput)
          }
        }
      }

      is RECEIVER_EXISTS_WITH_LOGIN -> {

        val receiverUserId = (receiverStatus.data as RECEIVER_EXISTS_WITH_LOGIN).userId

        message = message.copy(receiverId = receiverUserId)
        //todo save above latest message object in localDatabase
        // update lastUpdatedAt value for receiver table

        if (FirebaseUtils.currentUser == null) {
          val output = constructOutputData(errorMessage = "Got FirebaseUtils.currentUser as null")
          return Result.failure(output)
        }

        val senderUserId = FirebaseUtils.currentUser!!.uid

        //Safety Check i.e not mandatory
        val checkExistUserIdTypeChatRoomResult =
          FirebaseUtils.checkIfNumberTypeChatRoomExistsOrNotInDb(
            senderTag = senderUserId,
            receiverTag = receiverUserId
          )

        if (checkExistUserIdTypeChatRoomResult is FireStoreResult.Error) {
          val output =
            constructOutputData(errorMessage = checkExistUserIdTypeChatRoomResult.errorMessage)
          return Result.retry()
        }

        return when ((checkExistUserIdTypeChatRoomResult as Success).data) {
          is Exist -> {
            // we have already userId Type room  for this chat in Db.
            // no need to create.
            // we can listen update message message only in this chat
            val successOutput =
              constructOutputData(roomId = (checkExistUserIdTypeChatRoomResult.data as Exist).roomId)
            Result.success(successOutput)
          }

          NotExist -> {
            val userIdTypeChatRoomId =
              senderUserId.plus(DEFAULT_CHAT_ROOM_SEPARATOR).plus(receiverUserId)

            val roomId = JSONObject(messageJson).optString("roomId")

            if (roomId == "null") {
              val output = constructOutputData(errorMessage = "Got RoomId as null")
              return Result.failure(output)
            }

            //Safety check to ensure the current RoomId is numberType
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
                val newLocalReceiverDetail =
                  oldChatRoomReceiver.copy(roomId = userIdTypeChatRoomId, userId = receiverUserId)

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

              val localMigrationWorkError =
                migrationWorkErrorData.getString(CREATE_CHAT_OUTPUT_ERROR_MESSAGE).orEmpty()

              if (localMigrationWorkError.isNotEmpty()) {
                return Result.failure(constructOutputData(errorMessage = localMigrationWorkError))
              }
            }

            val createChatRoomResult = FirebaseUtils.createChatRoomInFirebase(
              chatRoomId = userIdTypeChatRoomId,
              message = message
            )

            if (createChatRoomResult is FireStoreResult.Error) {
              val output = constructOutputData(errorMessage = createChatRoomResult.errorMessage)
              return Result.retry()
            }

            val updateRoomCreatedAtInProfileResult =
              FirebaseUtils.updateLastChatRoomUpdatedAtInProfile(lastRoomCreatedAt)

            if (updateRoomCreatedAtInProfileResult is FireStoreResult.Error) {
              val output =
                constructOutputData(errorMessage = updateRoomCreatedAtInProfileResult.errorMessage)
              return Result.retry()
            }

            val createdChatRoomId = (createChatRoomResult as Success).data

            val updateRoomCreatedAtInChatRoomResult =
              FirebaseUtils.updateLastChatRoomUpdatedAtInChatRoom(
                roomId = createdChatRoomId, currentTimeInMillis = lastRoomCreatedAt
              )

            if (updateRoomCreatedAtInChatRoomResult is FireStoreResult.Error) {
              val output =
                constructOutputData(errorMessage = updateRoomCreatedAtInChatRoomResult.errorMessage)
              return Result.retry()
            }

            val updateChatRoomIdsToSenderReceiverResult =
              FirebaseUtils.updateChatRoomIdsInSenderAndReceiverDetails(
                chatRoomId = createdChatRoomId,
                senderNumber = senderNumber,
                receiverMobileNumber = receiverNumber
              )

            if (updateChatRoomIdsToSenderReceiverResult is FireStoreResult.Error) {
              val output =
                constructOutputData(errorMessage = updateChatRoomIdsToSenderReceiverResult.errorMessage)
              return Result.retry()
            }

            // we successfully created UserId Type ChatRoom and updated It's id in user and receiver object.
            val updateParticipantsResult = FirebaseUtils.updateParticipantsDetailsInChatRoom(
              senderNumber = senderNumber,
              receiverNumber = receiverNumber,
              chatRoomId = createdChatRoomId
            )

            if (updateParticipantsResult is FireStoreResult.Error) {
              val output = constructOutputData(errorMessage = updateParticipantsResult.errorMessage)
              return Result.retry()
            }

            // we saved participants numbers in ChatRoom
            val successOutput = constructOutputData(roomId = createdChatRoomId)
            Result.success(successOutput)
          }
        }
      }
    }
  }

  companion object {
    const val SENDER_NUMBER = "SENDER_NUMBER"
    const val RECEIVER_NUMBER = "RECEIVER_NUMBER"
    const val MESSAGE_JSON_KEY = "MESSAGE_JSON"
    const val RECEIVER_NAME = "RECEIVER_NAME"
    const val RECEIVER_IMAGE = "RECEIVER_IMAGE"
    const val ROOM_CREATED_AT = "ROOM_CREATED_AT"

    const val CREATE_CHAT_OUTPUT_ROOM_ID = "ROOM_ID"
    const val CREATE_CHAT_OUTPUT_ERROR_MESSAGE = "ERROR_MESSAGE"

    @SuppressLint("RestrictedApi")
    fun constructOutputData(errorMessage: String? = null, roomId: String? = null): Data {
      val builder = Data.Builder()
      if (errorMessage != null) {
        builder.put(CREATE_CHAT_OUTPUT_ERROR_MESSAGE, errorMessage)
      }
      if (roomId != null) {
        builder.put(CREATE_CHAT_OUTPUT_ROOM_ID, roomId)
      }
      return builder.build()
    }
  }
}