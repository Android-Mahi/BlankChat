package com.invorel.blankchatpro.viewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import com.invorel.blankchatpro.constants.DEFAULT_CHAT_ROOM_SEPARATOR
import com.invorel.blankchatpro.local.database.BlankLocalDatabase
import com.invorel.blankchatpro.online.fb_collections.Message
import com.invorel.blankchatpro.state.ChatsUiState
import com.invorel.blankchatpro.state.Contact
import com.invorel.blankchatpro.utils.FirebaseUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatsViewModel(private val localDatabase: BlankLocalDatabase) : ViewModel() {

  private val _uiState = MutableStateFlow(ChatsUiState())
  val uiState = _uiState.asStateFlow()

  private val receiverContactDetails = MutableLiveData<Contact>()
  private val receiverId = MutableStateFlow("")

  //Todo handle null in below line
  private val senderNumber = MutableStateFlow(FirebaseUtils.currentUser!!.phoneNumber!!)
  private val senderId = MutableStateFlow(FirebaseUtils.currentUser!!.uid)

  private val currentChatRoomId = MutableStateFlow("")

  fun updateCurrentMessage(message: Message) {
    _uiState.value = _uiState.value.copy(currentMessage = message)
  }

  private fun updateReceiverId(userIdOrNumber: String) {
    receiverId.value = userIdOrNumber
  }

  fun updateReceiverDetails(contact: Contact) {
    receiverContactDetails.value = contact
    updateReceiverId(contact.number)
  }

  fun addCurrentMessageToChat() {
    val currentMessage = _uiState.value.currentMessage
    if (currentMessage.message.isEmpty()) return

    if (receiverContactDetails.value == null) {
      updateErrorMessage("Got Receiver Details Null in viewModel")
      return
    }

    viewModelScope.launch {
      checkReceiverDetailsAndCreateChatRoomInBackendIfNeeded()

    }

    /*val messageDocument = Message(
      id = 0,
      message = currentMessage,
      senderId = senderId,
      receiverId = receiverId.value,
      status = 0,
      //Todo save Home Switch state in prefs in get the value here
      isMessageModeOn =false,
      //Todo update this only received from User
      //receivedTimeStamp = Date(System.currentTimeMillis()),
      sentTime = System.currentTimeMillis(),
    )*/

    /* FirebaseUtils.createChatRoomInFirebase(
       senderTag = senderId, receiverTag = receiverId.value, message = messageDocument, onChatRoomCreated = {

       }, onChatRoomCreateFailed = ::updateErrorMessage
     )*/
  }

  private fun updateErrorMessage(message: String) {
    _uiState.value = _uiState.value.copy(errorMessage = message)
  }

  fun updateChatRoomId(roomId: String) {
    currentChatRoomId.value = roomId
  }

  private suspend fun checkReceiverDetailsAndCreateChatRoomInBackendIfNeeded() {

    if (receiverContactDetails.value == null) {
      updateErrorMessage("Got null for receiverContactDetails")
      return
    }

    val receiverNumber = receiverContactDetails.value!!.number

    FirebaseUtils.checkReceiverDetailsInFireBase(
      mobileNumber = receiverNumber,
      onFailed = ::updateErrorMessage,
      onReceiverNotExists = {
        //Todo First upload contactPhoto to FB Storage if exists & use as profile photo in line no 113

        FirebaseUtils.createChatRoomInFirebase(
          chatRoomId = senderNumber.value.plus(DEFAULT_CHAT_ROOM_SEPARATOR).plus(receiverNumber),
          //Todo get org switch status and use here
          message = uiState.value.currentMessage,
          onChatRoomCreated = { freshCreatedRoomId ->
            val receiverChatRoomIds = JsonArray()
            receiverChatRoomIds.add(JsonPrimitive(freshCreatedRoomId))
            viewModelScope.launch {
              delay(50)
            }
            FirebaseUtils.createNotLoggedInReceiverDetailsInDB(
              mobileNumber = receiverNumber,
              name = receiverContactDetails.value!!.name,
              photo = "",
              chatRoomIds = receiverChatRoomIds,
              onFailed = ::updateErrorMessage,
              onNotLoggedInReceiverCreated = {
                //Getting existing Room Ids for the Sender (current User)
                FirebaseUtils.getExistingChatRoomIdsForUser(
                  mobileNumber = senderNumber.value,
                  onExistingChatRoomIdsFetched = { oldRoomIds ->
                    //Todo test if latest roomId appended with old Room Ids
                    oldRoomIds.add(JsonPrimitive(freshCreatedRoomId))
                    viewModelScope.launch {
                      delay(50)
                    }
                    //update latest room Ids appended with old Ids in Firebase
                    FirebaseUtils.updateChatRoomIdInSendUserDocument(
                      oldRoomIds,
                      onFailed = ::updateErrorMessage,
                      onChatRoomIdsUpdated = {
                        // we updated chat room in Both receiver and sender
                        if (currentChatRoomId.value.isEmpty()) {
                          currentChatRoomId.value = freshCreatedRoomId
                        }
                        FirebaseUtils.updateParticipantsDetailsInChatRoom(
                          senderNumber = senderNumber.value,
                          receiverNumber = receiverNumber,
                          chatRoomId = freshCreatedRoomId,
                          onFailed = ::updateErrorMessage,
                          onParticipantDetailsUpdated = {
                          // we saved participants numbers in ChatRoom
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
          senderTag = senderNumber.value,
          receiverTag = receiverNumber,
          onFailed = ::updateErrorMessage,
          onChatRoomExist = { existingNumberTypeRoomId ->
            if (currentChatRoomId.value.isEmpty()) {
              currentChatRoomId.value = existingNumberTypeRoomId
            }
          },
          onChatRoomDoesNotExist = {
            // we can hope this method never invoked due to if the Not Logged In receiver details exists in DB.
            // Definitely the sender only invoked chatRoom Will be there in DB
            // which is handled in onReceiverNotExists callback in this function.
          }
        )


      },
      onReceiverExistsWithLogin = { userId ->
        updateReceiverId(userId)

        FirebaseUtils.checkIfChatRoomExistsOrNotInDb(
          senderTag = senderNumber.value,
          receiverTag = receiverNumber,
          onFailed = ::updateErrorMessage,
          onChatRoomDoesNotExist = {

            FirebaseUtils.checkIfChatRoomExistsOrNotInDb(
              senderTag = senderId.value,
              receiverTag = receiverId.value,
              onFailed = ::updateErrorMessage,
              onChatRoomExist = { existingUserIdTypeRoomId ->
                // we have already userId Type room  for this chat in Db.
                // no need to create.
                // we can listen update message message only in this chat
                if (currentChatRoomId.value.isEmpty()) {
                  currentChatRoomId.value = existingUserIdTypeRoomId
                }
              },
              onChatRoomDoesNotExist = {
                val chatRoomId =
                  senderId.value.plus(DEFAULT_CHAT_ROOM_SEPARATOR).plus(receiverId.value)
                FirebaseUtils.createChatRoomInFirebase(
                  chatRoomId = chatRoomId,
                  onChatRoomCreateFailed = ::updateErrorMessage,
                  message = uiState.value.currentMessage,
                  onChatRoomCreated = { newUserIdTypeRoomId ->
                    FirebaseUtils.updateChatRoomIdsInSenderAndReceiverDetails(
                      newUserIdTypeRoomId,
                      senderNumber.value,
                      receiverNumber,
                      onFailed = ::updateErrorMessage,
                      onSuccess = {
                        // we successfully created UserId Type ChatRoom and updated It's id in user and receiver object.
                        if (currentChatRoomId.value.isEmpty()) {
                          currentChatRoomId.value = newUserIdTypeRoomId
                        }
                        FirebaseUtils.updateParticipantsDetailsInChatRoom(
                          senderNumber = senderNumber.value,
                          receiverNumber = receiverNumber,
                          chatRoomId = newUserIdTypeRoomId,
                          onFailed = ::updateErrorMessage,
                          onParticipantDetailsUpdated = {
                            // we saved participants numbers in ChatRoom
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
              receiverId = receiverId.value,
              existingRoomId = numberTypeRoomId,
              onFailed = ::updateErrorMessage,
              onSuccess = { newUserIdTypeRoomId ->
                if (currentChatRoomId.value.isEmpty()) {
                  currentChatRoomId.value = newUserIdTypeRoomId
                }
                FirebaseUtils.updateChatRoomIdsInSenderAndReceiverDetails(
                  chatRoomId = newUserIdTypeRoomId,
                  senderNumber = senderNumber.value,
                  receiverMobileNumber = receiverNumber,
                  onFailed = ::updateErrorMessage,
                  onSuccess = {
                    // we renamed existing Number chatRoom to UserId Type and updated it's id to sender & receiver table
                    FirebaseUtils.updateParticipantsDetailsInChatRoom(
                      senderNumber = senderNumber.value,
                      receiverNumber = receiverNumber,
                      chatRoomId = newUserIdTypeRoomId,
                      onFailed = ::updateErrorMessage,
                      onParticipantDetailsUpdated = {
                        // we saved participants numbers in ChatRoom
                      })
                  }
                )
              })
          }
         )
      }
    )
  }

  fun showLoading() {
    _uiState.value = _uiState.value.copy(fetchInProgress = true)
  }

  fun hideLoading() {
    _uiState.value = _uiState.value.copy(fetchInProgress = false)
  }
}