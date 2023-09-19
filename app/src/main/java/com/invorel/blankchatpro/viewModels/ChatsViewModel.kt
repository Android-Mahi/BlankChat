package com.invorel.blankchatpro.viewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invorel.blankchatpro.constants.DEFAULT_CHAT_ROOM_SEPARATOR
import com.invorel.blankchatpro.local.database.BlankLocalDatabase
import com.invorel.blankchatpro.online.fb_collections.Message
import com.invorel.blankchatpro.state.ChatsUiState
import com.invorel.blankchatpro.utils.FirebaseUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatsViewModel(private val localDatabase: BlankLocalDatabase) : ViewModel() {

  private val _uiState = MutableStateFlow(ChatsUiState())
  val uiState = _uiState.asStateFlow()

  private val receiverContactDetails = MutableLiveData<ChatReceiverDetails>()

  private val senderNumber = MutableLiveData("")
  private val senderId = MutableLiveData("")

  private val currentChatRoomId = MutableStateFlow("")
  private val lastMessageId = MutableStateFlow(1)

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

  private fun updateLastMessageId(id: Int) {
    lastMessageId.value = id
    updateMessageIdInMessage(id)
  }

  // Message Details
  fun updateCurrentMessage(message: Message) {
    _uiState.value = _uiState.value.copy(currentMessage = message)
  }

  private fun updateMessageIdInMessage(id: Int) {
    _uiState.value.currentMessage = _uiState.value.currentMessage.copy(id= id)
  }

  private fun updateSenderIdInMessage(senderId: String) {
    _uiState.value.currentMessage = _uiState.value.currentMessage.copy(senderId= senderId)
  }

  private fun updateReceiverIdInMessage(receiverId: String) {
    _uiState.value.currentMessage = _uiState.value.currentMessage.copy(receiverId = receiverId)
  }

  private fun updateSentTimeInMessage(sentTime: Long) {
    _uiState.value.currentMessage = _uiState.value.currentMessage.copy(sentTime = sentTime)
  }

  private fun updateMessagesList(list: List<Message>) {
    _uiState.value = _uiState.value.copy(messagesList = list)
  }

  private fun addMessageInCurrentList(message: Message) {
    val newList = _uiState.value.messagesList.toMutableList()
    newList.add(message)
    _uiState.value = _uiState.value.copy(messagesList = newList)
  }

  private fun updateReceiverId(userId: String) {
    if(receiverContactDetails.value == null) {
      updateErrorMessage("Got receiverContactDetails null in ChatsViewModel")
      return
    }
    receiverContactDetails.value = receiverContactDetails.value!!.copy(userId = userId)
    updateReceiverIdInMessage(userId)
  }

  fun updateReceiverDetails(receiverDetails: ChatReceiverDetails) {
    receiverContactDetails.value = receiverDetails
  }

  private fun incrementLastMessageId() {
    updateLastMessageId(lastMessageId.value+1)
  }

  private fun clearLastMessageDetails() {
    _uiState.value.currentMessage = _uiState.value.currentMessage.copy(message = "", sentTime = -1, status = 0)
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

        if (receiverContactDetails.value == null) {
          updateErrorMessage("Got null for receiverContactDetails")
          return@launch
        }

        if (FirebaseUtils.currentUser == null) {
          updateErrorMessage("Got Fb CurrentUser null")
          return@launch
        }

        if (FirebaseUtils.currentUser!!.displayName == null) {
          updateErrorMessage("Got Fb CurrentUser Name null")
          return@launch
        }

        if (senderNumber.value == null) {
          updateErrorMessage("Got senderNumber value null")
          return@launch
        }

        if (senderId.value == null) {
          updateErrorMessage("Got senderId value null")
          return@launch
        }

        createChatRoomAndUpdateMessage(
          receiverNumber = receiverContactDetails.value!!.number,
          receiverName = receiverContactDetails.value!!.name,
          receiverUid = receiverContactDetails.value!!.userId,
          senderNumber = senderNumber.value!!,
          senderUid = senderId.value!!,
          message = uiState.value.currentMessage.copy(message = uiState.value.currentMessage.message.trim()),
          onMessageUpdatedInBackEnd = {
            addMessageInCurrentList(it)
            incrementLastMessageId()
            clearLastMessageDetails()
            listenMessageUpdatesInCurrentChatRoom()
          }
        )
        // we have created the chatRoom sent the first message. so we can hope the message id will be 1 for the first message
        //updateLastMessageId(1)
      } else {
        updateCurrentMessageInBackEndChatRoom(
          message = uiState.value.currentMessage.copy(message = uiState.value.currentMessage.message.trim()),
          onMessageAddedInCurrentChatRoom = {
            addMessageInCurrentList(it)
            incrementLastMessageId()
            clearLastMessageDetails()
            listenMessageUpdatesInCurrentChatRoom()
          })
      }
    }
  }

  fun getMessagesOfCurrentChatRoomInBackend() {
    viewModelScope.launch (Dispatchers.IO){
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
        //Todo First upload contactPhoto to FB Storage if exists & use as profile photo in line no 113

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
              photo = "",
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
  }

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

  private fun listenMessageUpdatesInCurrentChatRoom() {
    viewModelScope.launch(Dispatchers.IO) {
      FirebaseUtils.listenMessagesForChatRoom(currentChatRoomId.value, onFailed = ::updateErrorMessage, onMessageUpdated = {
        getMessagesOfCurrentChatRoomInBackend()
      })
    }
  }

  fun showLoading() {
    _uiState.value = _uiState.value.copy(fetchInProgress = true)
  }

  fun hideLoading() {
    _uiState.value = _uiState.value.copy(fetchInProgress = false)
  }
}