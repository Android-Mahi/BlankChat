package com.invorel.blankchatpro.viewModels

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import com.google.gson.Gson
import com.invorel.blankchatpro.constants.DEFAULT_CHAT_ROOM_SEPARATOR
import com.invorel.blankchatpro.extensions.isNumberTypeChatRoom
import com.invorel.blankchatpro.local.repo.LocalRepo
import com.invorel.blankchatpro.local.repo.LocalRepo.LocalDbResult
import com.invorel.blankchatpro.local.tables.LocalChatRoom
import com.invorel.blankchatpro.local.tables.LocalMessage
import com.invorel.blankchatpro.local.tables.LocalReceiverDetail
import com.invorel.blankchatpro.mappers.toUIModel
import com.invorel.blankchatpro.online.fb_collections.Message
import com.invorel.blankchatpro.state.ChatsUiState
import com.invorel.blankchatpro.utils.BitMapUtils
import com.invorel.blankchatpro.utils.FirebaseUtils
import com.invorel.blankchatpro.utils.FirebaseUtils.FireStoreResult
import com.invorel.blankchatpro.utils.FirebaseUtils.FireStoreResult.Success
import com.invorel.blankchatpro.utils.FirebaseUtils.ReceiverExistStatus.RECEIVER_EXISTS_WITHOUT_LOGIN
import com.invorel.blankchatpro.utils.FirebaseUtils.ReceiverExistStatus.RECEIVER_EXISTS_WITH_LOGIN
import com.invorel.blankchatpro.utils.FirebaseUtils.ReceiverExistStatus.RECEIVER_NOT_EXISTS
import com.invorel.blankchatpro.utils.WorkerUtils
import com.invorel.blankchatpro.workers.AddMessageWorker
import com.invorel.blankchatpro.workers.CreateChatRoomWorker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

class ChatsViewModel(val localRepo: LocalRepo) : ViewModel() {

  private val _uiState = MutableStateFlow(ChatsUiState())
  val uiState = _uiState.asStateFlow()

  private val _currentTypedMessage = MutableStateFlow("")
  val currentTypedMessage = _currentTypedMessage.asStateFlow()

  fun updateCurrentTypedMessage(message: String) {
    _currentTypedMessage.value = message
  }

  private fun clearCurrentTypedMessage() {
    _currentTypedMessage.value = ""
  }

  private val receiverContactDetails = MutableLiveData<ChatReceiverDetails>()

  private val senderNumber = MutableLiveData("")
  private val senderId = MutableLiveData("")

  val currentChatRoomId = MutableStateFlow("")
  private val lastMessageId = MutableStateFlow(1)

  private val localReceiverContactPhoto = MutableStateFlow<Bitmap?>(null)

  fun checkIfAlreadyChatRoomExists() {
    if (receiverContactDetails.value == null) {
      updateErrorMessage("Got receiverContactDetails null while checking isChatRoomAlready Exists")
      return
    }
    localRepo.isChatRoomAlreadyExists(
      scope = viewModelScope,
      receiverNumber = receiverContactDetails.value!!.number,
      onNoChatRoomExists = {
        //New Chat contact chosen by user
        clearPreviousChatDetails()
      }, onChatRoomExists = { existingRoomId ->
        //Already existing chat Contact chose by user
        currentChatRoomId.value = existingRoomId
        getLocalMessagesForTheChatRoom()
        listenMessageUpdatesInCurrentChatRoomUpdateIfNeeded()
      }
    )
  }

  private fun updateLocalReceiverContactPhoto(bitmap: Bitmap?) {
    localReceiverContactPhoto.value = bitmap
  }

  fun updateSenderDetails() {
    if (FirebaseUtils.currentUser == null) {
      updateErrorMessage("Got Firebase Current User as null")
      return
    }

    if (FirebaseUtils.currentUser!!.phoneNumber == null) {
      updateErrorMessage("Got Firebase Current User Phone Number as null")
      return
    }

    senderNumber.value = FirebaseUtils.currentUser!!.phoneNumber!!
    senderId.value = FirebaseUtils.currentUser!!.uid
    updateSenderIdInMessage(senderId.value!!)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  suspend fun updateLocalReceiverImage() {
    //todo pass below function result to create chat Worker for receiverImage Param
    val receiverImageUrl = suspendCancellableCoroutine { continuation ->
      val receiverPhotoUpload = viewModelScope.async {
        if (localReceiverContactPhoto.value != null) {
          val photoData =
            BitMapUtils.convertBitMapToByteArray(localReceiverContactPhoto.value!!)

          if (receiverContactDetails.value == null) {
            updateErrorMessage("Got receiverContactDetails value as null")
            return@async
          }

          FirebaseUtils.uploadReceiverPhotoInFireBaseWithByteArray(
            receiverContactDetails.value!!.number,
            photoData,
            onFailed = {
              updateErrorMessage(it)
            },
            onSuccess = { imageUrl ->
              continuation.resume(imageUrl, onCancellation = null)
            })
        } else {
          continuation.resume("", onCancellation = null)
        }
      }
      continuation.invokeOnCancellation {
        receiverPhotoUpload.cancel()
      }
    }
    if (receiverContactDetails.value == null) {
      updateErrorMessage("Got receiverContactDetails value as null")
      return
    }
    receiverContactDetails.value = receiverContactDetails.value!!.copy(photo = receiverImageUrl)
  }

  fun updateReceiverDetails(receiverDetails: ChatReceiverDetails, localContactPhoto: Bitmap?) {
    updateLocalReceiverContactPhoto(localContactPhoto)
    receiverContactDetails.value = receiverDetails
    updateReceiverIdInMessage(receiverDetails.userId)
  }

  private fun updateLastMessageId(id: Int) {
    lastMessageId.value = id
    updateMessageIdInMessage(id)
  }

  // Message Details
  fun updateCurrentMessage(message: Message) {
    _uiState.value = _uiState.value.copy(currentMessage = message)
  }

  private fun updateMessageIdInMessage(id: Int) {
    _uiState.value.currentMessage = _uiState.value.currentMessage.copy(id = id)
  }

  private fun updateSenderIdInMessage(senderId: String) {
    _uiState.value.currentMessage = _uiState.value.currentMessage.copy(senderId = senderId)
  }

  private fun updateReceiverIdInMessage(receiverId: String) {
    _uiState.value.currentMessage = _uiState.value.currentMessage.copy(receiverId = receiverId)
  }

  private fun clearLastMessageDetails() {
    _uiState.value.currentMessage =
      _uiState.value.currentMessage.copy(message = "", sentTime = -1, status = 0)
  }

  private fun updateErrorMessage(message: String) {
    _uiState.value = _uiState.value.copy(errorMessage = message)
  }

  fun clearErrorMessage() {
    _uiState.value = _uiState.value.copy(errorMessage = "")
  }

  fun updateChatRoomId(roomId: String) {
    currentChatRoomId.value = roomId
  }

  private fun clearPreviousChatDetails() {
    updateLastMessageId(1)
    currentChatRoomId.value = ""
    if (uiState.value.messagesList.isNotEmpty()) {
      uiState.value.messagesList = listOf()
    }
    clearLastMessageDetails()
  }

  @SuppressLint("RestrictedApi")
  @OptIn(ExperimentalCoroutinesApi::class)
  fun saveLocalChatRoomDetails(
    isNetworkConnected: Boolean,
    context: Context,
    owner: LifecycleOwner,
  ) {

    if (receiverContactDetails.value == null) {
      updateErrorMessage("Got receiverContactDetails null while saving local chatRoomDetails")
      return
    }

    if (FirebaseUtils.currentUser == null) {
      updateErrorMessage("Got CurrentUser null while saving local chatRoomDetails")
      return
    }

    val localChatRoom = LocalChatRoom(
      roomId = "",
      userId = FirebaseUtils.currentUser!!.uid,
      lastMessageUpdatedAt = -1
    )

    viewModelScope.launch {
      val properRoomId = suspendCancellableCoroutine { continuation ->
        if (isNetworkConnected) {
          this.launch {
            val receiverDetailsResult = FirebaseUtils.checkReceiverDetailsInFireBase(
              mobileNumber = receiverContactDetails.value!!.number
            )

            if (receiverDetailsResult is FireStoreResult.Error) {
              updateErrorMessage(receiverDetailsResult.errorMessage)
            } else {

              when ((receiverDetailsResult as Success).data) {
                is RECEIVER_EXISTS_WITH_LOGIN -> {
                  val userIdTypeRoomID =
                    senderId.value.plus(DEFAULT_CHAT_ROOM_SEPARATOR)
                      .plus((receiverDetailsResult.data as RECEIVER_EXISTS_WITH_LOGIN).userId)
                  continuation.resume(userIdTypeRoomID, onCancellation = null)
                }

                RECEIVER_EXISTS_WITHOUT_LOGIN -> {
                  //Someone initiated chat with this receiver but receiver not logged in with BlankChat
                  if (receiverContactDetails.value == null) {
                    updateErrorMessage("Got receiverContactDetails null while creating Local ChatRoom Details onReceiverNotExists (Number) type")
                  } else {
                    val numberTypeRoomID = senderNumber.value.plus(DEFAULT_CHAT_ROOM_SEPARATOR)
                      .plus(receiverContactDetails.value!!.number)
                    continuation.resume(numberTypeRoomID, onCancellation = null)
                  }
                }

                RECEIVER_NOT_EXISTS -> {
                  //receiver not logged in with BlankChat & no-one initiated chat with this number
                  if (receiverContactDetails.value == null) {
                    updateErrorMessage("Got receiverContactDetails null while creating Local ChatRoom Details onReceiverNotExists (Number) type")
                  } else {
                    val numberTypeRoomID =
                      senderNumber.value.plus(DEFAULT_CHAT_ROOM_SEPARATOR)
                        .plus(receiverContactDetails.value!!.number)
                    continuation.resume(numberTypeRoomID, onCancellation = null)
                  }
                }
              }
            }
          }
        } else {
          if (receiverContactDetails.value == null) {
            updateErrorMessage("Got receiverContactDetails null while creating Local ChatRoom Details onReceiverNotExists (Number) type")
          } else {
            val numberTypeRoomID = senderNumber.value.plus(DEFAULT_CHAT_ROOM_SEPARATOR)
              .plus(receiverContactDetails.value!!.number)
            continuation.resume(numberTypeRoomID, onCancellation = null)
          }
        }
      }

      val saveLocalChatRoomResult = localRepo.saveChatRoomDetail(
        scope = viewModelScope,
        chatRoom = localChatRoom.copy(roomId = properRoomId)
      )

      if (saveLocalChatRoomResult is LocalDbResult.LocalError) {
        updateErrorMessage(saveLocalChatRoomResult.errorMessage)
        return@launch
      }

      val currentLocalTimeInMillis = System.currentTimeMillis()

      localRepo.getUserByUserId(
        scope = viewModelScope,
        userId = FirebaseUtils.currentUser!!.uid,
        onFailed = ::updateErrorMessage,
        onSuccess = {
          val updatedUser = it.copy(lastRoomCreatedAt = currentLocalTimeInMillis)
          localRepo.updateLocalUser(
            viewModelScope,
            updatedUser,
            onFailed = ::updateErrorMessage,
            onSuccess = {
              //we successfully save lastRoomUpdated TimeStamp locally pass this value also in CreateChatWorker
            })
        })

      val localMessage = LocalMessage(
        roomId = properRoomId,
        //since it's the first message of chatRoom
        id = 1,
        //todo get proper switch status here
        isSentByMessageMode = false,
        message = uiState.value.currentMessage.message,
        receivedTime = -1,
        receiverId = receiverContactDetails.value!!.userId.ifEmpty { receiverContactDetails.value!!.number },
        senderId = senderId.value!!.ifEmpty { senderNumber.value!! },
        sentTime = System.currentTimeMillis(),
        status = 0
      )

      val saveMessageResult = localRepo.saveMessage(
        scope = viewModelScope,
        localMessage = localMessage
      )

      if (saveMessageResult is LocalDbResult.LocalError) {
        updateErrorMessage(saveMessageResult.errorMessage)
        return@launch
      }

      //To clear message in Bottom Typing Bar
      clearCurrentTypedMessage()

      updateChatRoomId(properRoomId)

      getLocalMessagesForTheChatRoom()
      listenMessageUpdatesInCurrentChatRoomUpdateIfNeeded()

      if (receiverContactDetails.value == null) {
        updateErrorMessage("Got receiverContactDetails value as null")
        return@launch
      }

      val localReceiverDetails = LocalReceiverDetail(
        number = receiverContactDetails.value!!.number,
        name = receiverContactDetails.value!!.name,
        photo = receiverContactDetails.value!!.photo,
        roomId = properRoomId,
        userId = receiverContactDetails.value!!.userId,
        //Todo think if the proper timestamp is needed or not
        lastProfileUpdatedAt = -1L
      )

      val saveLocalReceiverDetailsResult = localRepo.saveReceiverDetail(
        scope = viewModelScope,
        localReceiverDetails
      )

      if (saveLocalReceiverDetailsResult is LocalDbResult.LocalError) {
        updateErrorMessage(saveLocalReceiverDetailsResult.errorMessage)
        return@launch
      }

      val messageJson = Gson().toJson(localMessage)

      if (receiverContactDetails.value == null) {
        updateErrorMessage("Got receiverContactDetails value as null")
        return@launch
      }

      if (senderNumber.value == null) {
        updateErrorMessage("Got senderNumber value as null")
        return@launch
      }

      if (senderNumber.value!!.isEmpty()) {
        updateErrorMessage("Got senderNumber value as empty")
        return@launch
      }

      val receiverName = receiverContactDetails.value!!.name
      val receiverNumber = receiverContactDetails.value!!.number
      val receiverImage = receiverContactDetails.value!!.photo

      val senderNumber = senderNumber.value!!

      val createChatRoomWorkerTag = WorkerUtils.getCreateChatRoomWorkerTag(
        senderNumber = senderNumber,
        receiverNumber = receiverNumber
      )

      WorkerUtils.enqueueOneTimeWork(
        context = context,
        tag = createChatRoomWorkerTag,
        worker = CreateChatRoomWorker::class.java,
        input = Data(
          mapOf(
            CreateChatRoomWorker.RECEIVER_NUMBER to receiverNumber,
            CreateChatRoomWorker.RECEIVER_NAME to receiverName,
            CreateChatRoomWorker.RECEIVER_IMAGE to receiverImage,
            CreateChatRoomWorker.SENDER_NUMBER to senderNumber,
            CreateChatRoomWorker.ROOM_CREATED_AT to currentLocalTimeInMillis,
            CreateChatRoomWorker.MESSAGE_JSON_KEY to messageJson,
          )
        )
      )

      WorkerUtils.listenResultOfWorker(
        scope = viewModelScope,
        tag = createChatRoomWorkerTag,
        context = context,
        owner = owner
      ) { createChatOutputData ->

        when {
          createChatOutputData.getString(CreateChatRoomWorker.CREATE_CHAT_OUTPUT_ERROR_MESSAGE) != null -> {
            //Failure Result
            updateErrorMessage(createChatOutputData.getString(CreateChatRoomWorker.CREATE_CHAT_OUTPUT_ERROR_MESSAGE)!!)

            /*WorkerUtils.enqueueOneTimeWork(
              context = context,
              tag = createChatRoomWorkerTag,
              worker = CreateChatRoomWorker::class.java,
              input = Data(
                mapOf(
                  CreateChatRoomWorker.RECEIVER_NUMBER to receiverNumber,
                  CreateChatRoomWorker.RECEIVER_NAME to receiverName,
                  CreateChatRoomWorker.RECEIVER_IMAGE to receiverImage,
                  CreateChatRoomWorker.SENDER_NUMBER to senderNumber,
                  CreateChatRoomWorker.ROOM_CREATED_AT to currentLocalTimeInMillis,
                  CreateChatRoomWorker.MESSAGE_JSON_KEY to messageJson,
                )
              )
            )*/
          }

          else -> {
            //Success Result

            // update Message status to Sent
            // we create chatRoom if not exists after the first message created by user so
            // first message id of the chatRoom will be 1

            val roomId =
              createChatOutputData.getString(CreateChatRoomWorker.CREATE_CHAT_OUTPUT_ROOM_ID)

            if (roomId == null) {
              updateErrorMessage("Got RoomId as null in output of createChatWorker")
              return@listenResultOfWorker
            }

            updateMessageStatus(
              status = Message.SENT,
              roomId = roomId,
              messageId = 1
            )
          }

        }

      }

    }
  }

  @SuppressLint("RestrictedApi") fun saveMessageDetails(
    context: Context,
    roomId: String,
    owner: LifecycleOwner,
  ) {

    val localMessage = LocalMessage(
      roomId = roomId,
      id = lastMessageId.value,
      //todo get proper switch status here
      isSentByMessageMode = false,
      message = uiState.value.currentMessage.message,
      receivedTime = -1,
      receiverId = receiverContactDetails.value!!.userId.ifEmpty { receiverContactDetails.value!!.number },
      senderId = senderId.value!!.ifEmpty { senderNumber.value!! },
      sentTime = System.currentTimeMillis(),
      status = 0
    )

    viewModelScope.launch {

      val saveMessageResult = localRepo.saveMessage(
        scope = viewModelScope,
        localMessage = localMessage
      )
      if (saveMessageResult is LocalDbResult.LocalError) {
        updateErrorMessage(saveMessageResult.errorMessage)
        return@launch
      }

      //To clear message in Bottom Typing Bar
      getLocalMessagesForTheChatRoom()
      clearCurrentTypedMessage()

      val addMessageWorkerTag = WorkerUtils.getAddMessageToChatRoomWorkerTag(
        roomId = roomId,
        messageId = localMessage.id.toString()
      )

      if (WorkerUtils.checkIfCreateChatWorkerEnqueued(
          context = context,
          tag = WorkerUtils.getCreateChatRoomWorkerTag(
            senderNumber.value!!,
            receiverContactDetails.value!!.number
          )
        )
      ) {
        val messageJson = Gson().toJson(localMessage)

        val senderNumber = senderNumber.value
        val receiverNumber = receiverContactDetails.value!!.number

        val childWorker = WorkerUtils.createOneTimeRequest(
          worker = AddMessageWorker::class.java,
          tag = addMessageWorkerTag,
          input = Data(
            mapOf(
              AddMessageWorker.SENDER_NUMBER to senderNumber,
              AddMessageWorker.RECEIVER_NUMBER to receiverNumber,
              AddMessageWorker.ROOM_ID to roomId,
              AddMessageWorker.IS_CURRENT_CHATROOM_IS_NUMBER_TYPE to roomId.isNumberTypeChatRoom(),
              AddMessageWorker.MESSAGE_JSON_KEY to messageJson
            )
          )
        )

        //Todo test if parent job runs after enqueued before the child job added
        WorkerUtils.enqueueOneTimeWorkWithParentJob(childWorker)

        WorkerUtils.listenResultOfWorker(
          scope = viewModelScope,
          tag = addMessageWorkerTag,
          context = context,
          owner = owner
        ) { addMessageOutputData ->

          when {
            addMessageOutputData.getString(AddMessageWorker.ADD_MESSAGE_OUTPUT_ERROR_MESSAGE) != null -> {
              updateErrorMessage(addMessageOutputData.getString(AddMessageWorker.ADD_MESSAGE_OUTPUT_ERROR_MESSAGE)!!)
            }

            else -> {
              // update Message status to Sent
              val outputRoomId =
                addMessageOutputData.getString(AddMessageWorker.ADD_MESSAGE_OUTPUT_ROOM_ID)
              val messageId =
                addMessageOutputData.getString(AddMessageWorker.ADD_MESSAGE_OUTPUT_MESSAGE_ID)

              if (outputRoomId == null) {
                updateErrorMessage("Got RoomId as null in output for AddMessageWorker Output")
                return@listenResultOfWorker
              }

              if (messageId == null) {
                updateErrorMessage("Got messageId as null in output for AddMessageWorker Output")
                return@listenResultOfWorker
              }

              // update Message status to Sent
              updateMessageStatus(
                status = Message.SENT,
                roomId = outputRoomId,
                messageId = messageId.toInt()
              )
            }
          }
        }
      } else {
        //Create chat worker is not enqueued we can enqueue only add Message worker
        val messageJson = Gson().toJson(localMessage)

        val senderNumber = senderNumber.value
        val receiverNumber = receiverContactDetails.value!!.number

        WorkerUtils.enqueueOneTimeWork(
          worker = AddMessageWorker::class.java,
          context = context,
          tag = addMessageWorkerTag,
          input = Data(
            mapOf(
              AddMessageWorker.SENDER_NUMBER to senderNumber,
              AddMessageWorker.RECEIVER_NUMBER to receiverNumber,
              AddMessageWorker.ROOM_ID to roomId,
              AddMessageWorker.IS_CURRENT_CHATROOM_IS_NUMBER_TYPE to roomId.isNumberTypeChatRoom(),
              AddMessageWorker.MESSAGE_JSON_KEY to messageJson
            )
          )
        )

        WorkerUtils.listenResultOfWorker(
          scope = viewModelScope,
          tag = addMessageWorkerTag,
          context = context,
          owner = owner
        ) { addMessageOutputData ->

          when {
            addMessageOutputData.getString(AddMessageWorker.ADD_MESSAGE_OUTPUT_ERROR_MESSAGE) != null -> {
              updateErrorMessage(addMessageOutputData.getString(AddMessageWorker.ADD_MESSAGE_OUTPUT_ERROR_MESSAGE)!!)
            }

            else -> {
              // update Message status to Sent
              val outputRoomId =
                addMessageOutputData.getString(AddMessageWorker.ADD_MESSAGE_OUTPUT_ROOM_ID)
              val messageId =
                addMessageOutputData.getInt(AddMessageWorker.ADD_MESSAGE_OUTPUT_MESSAGE_ID, -1)

              if (outputRoomId == null) {
                updateErrorMessage("Got RoomId as null in output for AddMessageWorker Output")
                return@listenResultOfWorker
              }

              if (messageId == -1) {
                updateErrorMessage("Got messageId as null in output for AddMessageWorker Output")
                return@listenResultOfWorker
              }

              // update Message status to Sent
              updateMessageStatus(
                status = Message.SENT,
                roomId = outputRoomId,
                messageId = messageId
              )
            }
          }
        }
      }

    }
  }

  private fun updateMessageStatus(status: Int, roomId: String, messageId: Int) {
    localRepo.updateMessageStatus(
      scope = viewModelScope,
      status = status,
      roomId = roomId,
      messageId = messageId,
      onFailed = ::updateErrorMessage,
      onSuccess = {
        viewModelScope.launch {
          val updateMessageResult = viewModelScope.async {
            FirebaseUtils.updateMessageStatus(
              status = status,
              roomId = roomId,
              messageId = messageId
            )
          }.await()

          if (updateMessageResult is FireStoreResult.Error) {
            updateErrorMessage(updateMessageResult.errorMessage)
            return@launch
          }

          // We successfully updated message Status in Firebase

        }
      })
  }

  fun getLocalMessagesForTheChatRoom() {

    if (currentChatRoomId.value.isEmpty()) {
      updateErrorMessage("Got ChatRoom Id empty while getting messages for chat Room")
      return
    }

    viewModelScope.launch {

      val localChatRoomResult = viewModelScope.async {
        localRepo.getMessagesForChatRoom(
          scope = viewModelScope,
          roomId = currentChatRoomId.value,
          )
      }.await()


      if (localChatRoomResult is LocalDbResult.LocalError) {
        updateErrorMessage(localChatRoomResult.errorMessage)
        return@launch
      }

      val localMessages = (localChatRoomResult as LocalDbResult.LocalSuccess).data

      if (localMessages.isEmpty()) {
        // Impossible case as per our Impl due to chatRoom always created with initial message
      } else {
        val currentChatRoomMessages =
          localMessages.filter { it.roomId == currentChatRoomId.value }
        _uiState.value = _uiState.value.copy(messagesList = currentChatRoomMessages)
        if (currentChatRoomMessages.isNotEmpty()) {
          updateLastMessageId(uiState.value.messagesList.size.plus(1))
        }
      }

    }
    }

  private var listenMessagesJob: Job? = null

  fun listenMessageUpdatesInCurrentChatRoomUpdateIfNeeded() {
    listenMessagesJob?.cancel()
    listenMessagesJob = viewModelScope.launch {

      FirebaseUtils.listenMessagesForChatRoom(
        chatRoomId = currentChatRoomId.value,
        onFailed = ::updateErrorMessage,
        onMessageUpdated = { fbMessages ->

          viewModelScope.launch {
            val localMessages = mutableListOf<MessageUIModel>()

            val localMessagesResult = localRepo.getMessagesForChatRoom(
              scope = viewModelScope,
              roomId = currentChatRoomId.value
            )

            if (localMessagesResult is LocalDbResult.LocalError) {
              updateErrorMessage(localMessagesResult.errorMessage)
              return@launch
            }

            localMessages.addAll((localMessagesResult as LocalDbResult.LocalSuccess).data)

            val lastLocalMessageId = localMessages.maxOfOrNull { it.id } ?: -1

            val lastFbMessageId = fbMessages.maxOfOrNull { it.id } ?: -1

            if (lastFbMessageId == -1) {
              updateErrorMessage("Got lastFbMessageId as -1")
              return@launch
            }

            if (lastLocalMessageId == -1) {
              updateErrorMessage("Got lastLocalMessageId as -1")
              return@launch
            }

            if (localMessages.isEmpty()) {
              updateErrorMessage("Got localMessages as empty")
              return@launch
            }

            if (fbMessages.isEmpty()) {
              updateErrorMessage("Got fbMessages as empty")
              return@launch
            }

            if (localMessages.size == fbMessages.size && lastLocalMessageId == lastFbMessageId) {
              updateErrorMessage("No Updates Needed")
              return@launch
            }

            val localMessageIds = localMessages.map { it.id }

            //Eliminating items in fbMessages which was already available in localDb
            fbMessages.map { it.toUIModel() }.filter { !localMessageIds.contains(it.id) }.map {

                val localMessage = LocalMessage(
                  id = it.id,
                  roomId = currentChatRoomId.value,
                  message = it.message,
                  senderId = it.senderId,
                  receiverId = it.receiverId,
                  sentTime = it.sentTime,
                  receivedTime = it.receivedTime,
                  status = it.status,
                  isSentByMessageMode = it.isSentByMessageMode
                )

                if (localMessage.id == -1) return@launch

                if (listenMessagesJob == null) return@map

                val saveMessageResult = localRepo.saveMessage(
                  scope = viewModelScope,
                  localMessage = localMessage
                )

                if (saveMessageResult is LocalDbResult.LocalError) {
                  updateErrorMessage(saveMessageResult.errorMessage)
                  return@map
                }

                // we save messages from fb into local DB.
                getLocalMessagesForTheChatRoom()

              }

          }

        }
      )

    }
  }

  fun cancelListeningMessageFromFirebase() {
    listenMessagesJob?.cancel()
    listenMessagesJob = null
  }

}

data class MessageUIModel(
  val id: Int,
  val roomId: String,
  val message: String,
  val senderId: String,
  val receiverId: String,
  val sentTime: Long,
  val receivedTime: Long,
  val status: Int,
  val isSentByMessageMode: Boolean,
)

/*
private fun updateReceiverId(userId: String) {
    if (receiverContactDetails.value == null) {
      updateErrorMessage("Got receiverContactDetails null in ChatsViewModel")
      return
    }
    receiverContactDetails.value = receiverContactDetails.value!!.copy(userId = userId)
    updateReceiverIdInMessage(userId)
  }

  private fun addMessageInCurrentList(message: Message) {
    val newList = _uiState.value.messagesList.toMutableList()
    newList.add(message.toUIModel())
    _uiState.value = _uiState.value.copy(messagesList = newList)
  }

  private fun incrementLastMessageId() {
    updateLastMessageId(lastMessageId.value + 1)
  }

   fun updateCurrentMessageInBackEnd() {
    val currentMessage = _uiState.value.currentMessage
    if (currentMessage.message.isEmpty()) return

    if (receiverContactDetails.value == null) {
      updateErrorMessage("Got Receiver Details Null in viewModel")
      return
    }

    updateSentTimeInMessage(System.currentTimeMillis())

    viewModelScope.launch {
      if (currentChatRoomId.value.isEmpty()) {
        when {
          receiverContactDetails.value == null -> {
            updateErrorMessage("Got null for receiverContactDetails")
          }

          FirebaseUtils.currentUser == null -> {
            updateErrorMessage("Got Fb CurrentUser null")
          }

          FirebaseUtils.currentUser!!.displayName == null -> {
            updateErrorMessage("Got Fb CurrentUser Name null")
          }

          senderNumber.value == null -> {
            updateErrorMessage("Got senderNumber value null")
          }

          senderId.value == null -> {
            updateErrorMessage("Got senderId value null")
          }

          else -> {
            updateMessageInProgress(true)
            /*createChatRoomAndUpdateMessage(
               receiverNumber = receiverContactDetails.value!!.number,
               receiverName = receiverContactDetails.value!!.name,
               receiverUid = receiverContactDetails.value!!.userId,
               senderNumber = senderNumber.value!!,
               senderUid = senderId.value!!,
               message = uiState.value.currentMessage.copy(message = uiState.value.currentMessage.message.trim()),
               onMessageUpdatedInBackEnd = { updatedMessage ->
                 FirebaseUtils.updateLastChatRoomUpdatedAtInProfile(
                   onFailed = ::updateErrorMessage,
                   onSuccess = {
                     updateMessageInProgress(false)
                     addMessageInCurrentList(updatedMessage)
                     incrementLastMessageId()
                     clearLastMessageDetails()
                     listenMessageUpdatesInCurrentChatRoom()
                   })
               }
             )*/
            // we have created the chatRoom sent the first message. so we can hope the message id will be 1 for the first message
            //updateLastMessageId(1)
          }
        }
      } else {
        updateMessageInProgress(true)
        /*updateCurrentMessageInBackEndChatRoom(
          message = uiState.value.currentMessage.copy(message = uiState.value.currentMessage.message.trim()),
          onMessageAddedInCurrentChatRoom = {
            updateMessageInProgress(false)
            addMessageInCurrentList(it)
            incrementLastMessageId()
            clearLastMessageDetails()
            listenMessageUpdatesInCurrentChatRoom()
          })*/
      }
    }
  }

   private fun updateSentTimeInMessage(sentTime: Long) {
    _uiState.value.currentMessage = _uiState.value.currentMessage.copy(sentTime = sentTime)
  }

   private fun updateMessageInProgress(value: Boolean) {
    _uiState.value = _uiState.value.copy(isMessageUpdateInProgress = value)
  }


 //TOdo use this method only if the local timestamp less than timestamp in firebase
  private fun getMessagesOfCurrentChatRoomInBackend() {
    viewModelScope.launch(Dispatchers.IO) {
      FirebaseUtils.getMessagesFromChatRoom(
        chatRoomId = currentChatRoomId.value,
        onFailed = ::updateErrorMessage,
        onMessagesFetched = { dbMessages ->
          updateMessagesList(dbMessages)
          if (dbMessages.isEmpty()) {
            updateLastMessageId(1)
          } else if (dbMessages.size == 1) {
            updateLastMessageId(2)
          } else {
            val lastMessageId = dbMessages.maxOfOrNull { it.id }
            if (lastMessageId != null && lastMessageId != -1) {
              //new message id will be the incremented version of last one
              updateLastMessageId(lastMessageId + 1)
            }
          }
        }
      )
    }
  }

  /* @OptIn(ExperimentalCoroutinesApi::class)
   private suspend fun createChatRoomAndUpdateMessage(
     receiverNumber: String,
     receiverName: String,
     receiverUid: String,
     senderNumber: String,
     senderUid: String,
     message: Message,
     onMessageUpdatedInBackEnd: (Message) -> Unit,
   ) {

     if (receiverContactDetails.value == null) {
       updateErrorMessage("Got null for receiverContactDetails")
       return
     }

     FirebaseUtils.checkReceiverDetailsInFireBase(
       mobileNumber = receiverNumber,
       onFailed = ::updateErrorMessage,
       onReceiverNotExists = {
         viewModelScope.launch {
           val receiverImageUrl = suspendCancellableCoroutine { continuation ->
             val receiverPhotoUpload = viewModelScope.async {
               if (localReceiverContactPhoto.value != null) {
                 val photoData =
                   BitMapUtils.convertBitMapToByteArray(localReceiverContactPhoto.value!!)
                 FirebaseUtils.uploadReceiverPhotoInFireBaseWithByteArray(
                   receiverNumber,
                   photoData,
                   onFailed = ::updateErrorMessage,
                   onSuccess = { imageUrl ->
                     continuation.resume(imageUrl, onCancellation = null)
                   })
               } else {
                 continuation.resume("", onCancellation = null)
               }
             }
             continuation.invokeOnCancellation {
               receiverPhotoUpload.cancel()
             }
           }

           FirebaseUtils.createChatRoomInFirebase(
             chatRoomId = senderNumber.plus(DEFAULT_CHAT_ROOM_SEPARATOR).plus(receiverNumber),
             //Todo get org switch status and use here
             message = message,
             onChatRoomCreated = { freshCreatedRoomId ->
               val receiverChatRoomIds = mutableListOf<String>()
               receiverChatRoomIds.add(freshCreatedRoomId)
               viewModelScope.launch {
                 delay(50)
               }
               FirebaseUtils.createNotLoggedInReceiverDetailsInDB(
                 mobileNumber = receiverNumber,
                 name = receiverName,
                 photo = receiverImageUrl,
                 chatRoomIds = receiverChatRoomIds,
                 onFailed = ::updateErrorMessage,
                 onNotLoggedInReceiverCreated = {
                   //Getting existing Room Ids for the Sender (current User)
                   FirebaseUtils.getExistingChatRoomIdsForUser(
                     mobileNumber = senderNumber,
                     onExistingChatRoomIdsFetched = { oldRoomIds ->
                       val senderChatRoomIds = mutableListOf<String>()
                       senderChatRoomIds.addAll(oldRoomIds)
                       senderChatRoomIds.add(freshCreatedRoomId)
                       viewModelScope.launch {
                         delay(50)
                       }
                       //update latest room Ids appended with old Ids in Firebase
                       FirebaseUtils.updateChatRoomIdInSendUserDocument(
                         senderChatRoomIds,
                         onFailed = ::updateErrorMessage,
                         onChatRoomIdsUpdated = {
                           // we updated chat room in Both receiver and sender
                           if (currentChatRoomId.value.isEmpty()) {
                             updateChatRoomId(freshCreatedRoomId)
                           }
                           FirebaseUtils.updateParticipantsDetailsInChatRoom(
                             senderNumber = senderNumber,
                             receiverNumber = receiverNumber,
                             chatRoomId = freshCreatedRoomId,
                             onFailed = ::updateErrorMessage,
                             onParticipantDetailsUpdated = {
                               // we saved participants numbers in ChatRoom
                               onMessageUpdatedInBackEnd.invoke(message)
                             })
                         })
                     }, onFailed = ::updateErrorMessage
                   )

                 }
               )
             },
             onChatRoomCreateFailed = ::updateErrorMessage
           )
         }
       },
       onReceiverExistsWithoutLogin = {
         //No need to create
         //we have Number type Chat Room for not logged In User
         // Rare case bcz most of the time user and receiver are logged In into Blank Chat

         FirebaseUtils.checkIfChatRoomExistsOrNotInDb(
           senderTag = senderNumber,
           receiverTag = receiverNumber,
           onFailed = ::updateErrorMessage,
           onChatRoomExist = { existingNumberTypeRoomId ->
             if (currentChatRoomId.value.isEmpty()) {
               currentChatRoomId.value = existingNumberTypeRoomId
             }
             FirebaseUtils.updateMessageInChatRoom(
               roomId = existingNumberTypeRoomId,
               message = message, onFailed = ::updateErrorMessage,
               onMessageUpdatedInChatRoom = {
                 onMessageUpdatedInBackEnd.invoke(it)
               })
           },
           onChatRoomDoesNotExist = {
             // we can hope this method never invoked due to if the Not Logged In receiver details exists in DB.
             // Definitely the sender only invoked chatRoom Will be there in DB
             // which is handled in onReceiverNotExists callback in this function.
           }
         )
       },
       onReceiverExistsWithLogin = { userId ->
         //todo re-think & remove below line if not needed
         updateReceiverId(userId)
         //Checks Number Type Room
         FirebaseUtils.checkIfChatRoomExistsOrNotInDb(
           senderTag = senderNumber,
           receiverTag = receiverNumber,
           onFailed = ::updateErrorMessage,
           onChatRoomDoesNotExist = {
             //Checks UserId Type Room
             FirebaseUtils.checkIfChatRoomExistsOrNotInDb(
               senderTag = senderUid,
               receiverTag = receiverUid,
               onFailed = ::updateErrorMessage,
               onChatRoomExist = { existingUserIdTypeRoomId ->
                 // we have already userId Type room  for this chat in Db.
                 // no need to create.
                 // we can listen update message message only in this chat
                 if (currentChatRoomId.value.isEmpty()) {
                   updateChatRoomId(existingUserIdTypeRoomId)
                 }
                 FirebaseUtils.updateMessageInChatRoom(
                   roomId = existingUserIdTypeRoomId,
                   message = message, onFailed = ::updateErrorMessage, onMessageUpdatedInChatRoom = {
                     onMessageUpdatedInBackEnd.invoke(it)
                   })
               },
               onChatRoomDoesNotExist = {
                 val chatRoomId =
                   senderUid.plus(DEFAULT_CHAT_ROOM_SEPARATOR).plus(receiverUid)
                 FirebaseUtils.createChatRoomInFirebase(
                   chatRoomId = chatRoomId,
                   onChatRoomCreateFailed = ::updateErrorMessage,
                   message = message,
                   onChatRoomCreated = { newUserIdTypeRoomId ->
                     FirebaseUtils.updateChatRoomIdsInSenderAndReceiverDetails(
                       newUserIdTypeRoomId,
                       senderNumber,
                       receiverNumber,
                       onFailed = ::updateErrorMessage,
                       onSuccess = {
                         // we successfully created UserId Type ChatRoom and updated It's id in user and receiver object.
                         if (currentChatRoomId.value.isEmpty()) {
                           updateChatRoomId(newUserIdTypeRoomId)
                         }
                         FirebaseUtils.updateParticipantsDetailsInChatRoom(
                           senderNumber = senderNumber,
                           receiverNumber = receiverNumber,
                           chatRoomId = newUserIdTypeRoomId,
                           onFailed = ::updateErrorMessage,
                           onParticipantDetailsUpdated = {
                             // we saved participants numbers in ChatRoom
                             onMessageUpdatedInBackEnd.invoke(message)
                           })
                       })
                   }
                 )
               })
           },
           onChatRoomExist = { numberTypeRoomId ->
             // Sender previously sent message to receiver when receiver not logged In
             // now we have receiver UserId we can delete old Number type chatRoom and
             // create UserId Type chatRoom with existing number type chatRoom Data
             FirebaseUtils.changeExistingRoomTypeFromNumberToUserId(
               receiverId = receiverUid,
               existingRoomId = numberTypeRoomId,
               onFailed = ::updateErrorMessage,
               onSuccess = { newUserIdTypeRoomId ->
                 if (currentChatRoomId.value.isEmpty()) {
                   updateChatRoomId(newUserIdTypeRoomId)
                 }
                 FirebaseUtils.updateMessageInChatRoom(
                   roomId = newUserIdTypeRoomId,
                   message = message,
                   onFailed = ::updateErrorMessage,
                   onMessageUpdatedInChatRoom = {
                     FirebaseUtils.updateChatRoomIdsInSenderAndReceiverDetails(
                       chatRoomId = newUserIdTypeRoomId,
                       senderNumber = senderNumber,
                       receiverMobileNumber = receiverNumber,
                       onFailed = ::updateErrorMessage,
                       onSuccess = {
                         // we renamed existing Number chatRoom to UserId Type and updated it's id to sender & receiver table
                         FirebaseUtils.updateParticipantsDetailsInChatRoom(
                           senderNumber = senderNumber,
                           receiverNumber = receiverNumber,
                           chatRoomId = newUserIdTypeRoomId,
                           onFailed = ::updateErrorMessage,
                           onParticipantDetailsUpdated = {
                             // we saved participants numbers in ChatRoom
                             onMessageUpdatedInBackEnd.invoke(it)
                           })
                       }
                     )
                   }
                 )
               })
           }
         )
       }
     )
   }*/

   private fun updateCurrentMessageInBackEndChatRoom(
    message: Message,
    onMessageAddedInCurrentChatRoom: (Message) -> Unit,
  ) {
    FirebaseUtils.updateMessageInChatRoom(
      roomId = currentChatRoomId.value,
      message = message,
      onFailed = ::updateErrorMessage,
      onMessageUpdatedInChatRoom = { onMessageAddedInCurrentChatRoom.invoke(it) }
    )
  }

    private fun updateMessagesList(list: List<Message>) {
    _uiState.value = _uiState.value.copy(messagesList = list.map { it.toUIModel() })
  }


 */
