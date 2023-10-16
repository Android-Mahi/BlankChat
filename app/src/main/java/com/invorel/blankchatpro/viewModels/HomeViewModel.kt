package com.invorel.blankchatpro.viewModels

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invorel.blankchatpro.constants.DEFAULT_FB_NAME_ABOUT_SEPARATOR
import com.invorel.blankchatpro.constants.DataStoreManager
import com.invorel.blankchatpro.extensions.isNotNullAndNotEmpty
import com.invorel.blankchatpro.local.repo.LocalRepo
import com.invorel.blankchatpro.local.repo.LocalRepo.LocalDbResult
import com.invorel.blankchatpro.local.tables.LocalChatRoom
import com.invorel.blankchatpro.local.tables.LocalMessage
import com.invorel.blankchatpro.local.tables.LocalReceiverDetail
import com.invorel.blankchatpro.others.ErrorMessage
import com.invorel.blankchatpro.others.ErrorMessage.StringErrorMessage
import com.invorel.blankchatpro.state.HomeUiState
import com.invorel.blankchatpro.utils.FirebaseUtils
import com.invorel.blankchatpro.utils.FirebaseUtils.FireStoreResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeViewModel(dataStore: DataStore<Preferences>, val localRepo: LocalRepo) : ViewModel() {

  private val _uiState = MutableStateFlow(HomeUiState())
  val uiState = _uiState.asStateFlow()

  private val dataStoreManager = DataStoreManager(dataStore)

  init {
    refreshUserProfileDataFromFb()
  }

  private fun updateChatList(list: List<HomeChatUIModel>) {
    _uiState.value = _uiState.value.copy(homeChatList = list)
  }

  fun refreshUserProfileDataFromFb() {
    val nameAndAbout = FirebaseUtils.currentUser?.displayName.orEmpty()
    if (nameAndAbout.isNotEmpty()) {
      with(nameAndAbout.split(DEFAULT_FB_NAME_ABOUT_SEPARATOR)) {
        updateUserName(this[0])
        updateUserAbout(this[1])
      }
    }
    viewModelScope.launch {
      dataStoreManager.fbProfileImageUrl.collectLatest { storedProfileUrl ->
        if (storedProfileUrl.isNotNullAndNotEmpty()) {
          updateFireBaseProfileImgUrl(storedProfileUrl)
        } else {
          if (FirebaseUtils.currentUser == null) return@collectLatest
          getProfileImageDownloadUrlFromFirebase()
        }
      }
    }
  }

  private fun updateUserName(name: String) {
    _uiState.value = _uiState.value.copy(userName = name)
  }

  private fun updateUserAbout(about: String) {
    _uiState.value = _uiState.value.copy(userAbout = about)
  }

  private fun updateErrorMessage(message: ErrorMessage) {
    _uiState.value = _uiState.value.copy(errorMessage = message)
  }

  fun resetErrorMessage() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }

  private fun showLoading() {
    _uiState.value = _uiState.value.copy(fbRequestInProcess = true)
  }

  private fun hideLoading() {
    _uiState.value = _uiState.value.copy(fbRequestInProcess = false)
  }

  fun toggleMessageSwitchState() {
    _uiState.value =
      _uiState.value.copy(isMessageSwitchChecked = _uiState.value.isMessageSwitchChecked.not())
  }

  fun showContactPermissionCard(value: Boolean) {
    _uiState.value = _uiState.value.copy(isContactPermissionCardVisible = value)
  }

  fun updateRequestActionContactsAccess(value: Boolean) {
    _uiState.value = _uiState.value.copy(actionRequestContactsAccess = value)
  }

  //Firebase Methods

  private fun updateFireBaseProfileImgUrl(downloadUrl: String) {
    _uiState.value = _uiState.value.copy(userImage = downloadUrl)
    saveFirebaseProfileImageUrlInDataStore(downloadUrl)
  }

  private fun getProfileImageDownloadUrlFromFirebase() {
    FirebaseUtils.getDownloadUrlOfUploadedImage(
      onDownloadUrlFetched = {
        updateFireBaseProfileImgUrl(it)
      }, onDownloadUrlFetchFailed = {
        updateErrorMessage(StringErrorMessage(it))
      })
  }

  private fun saveFirebaseProfileImageUrlInDataStore(url: String) {
    viewModelScope.launch(Dispatchers.IO) {
      dataStoreManager.saveFbProfileImageUrl(url)
    }
  }

  fun updateUserStatusOnlineInBackend() {
    FirebaseUtils.updateUserStatus(
      isOnline = true,
      onFailed = {
      updateErrorMessage(StringErrorMessage(it))
    }, onUserStatusUpdated = {}
    )
  }

  fun checkLatestChatsFromBackendAndUpdateIfNeeded() {

    viewModelScope.launch(Dispatchers.IO) {

      val currentUserChatRoomIdsResult = FirebaseUtils.getChatRoomIdsForCurrentUser()

      if (currentUserChatRoomIdsResult is FireStoreResult.Error) {
        updateErrorMessage(StringErrorMessage(currentUserChatRoomIdsResult.errorMessage))
        return@launch
      }

      val currentUserFirebaseChatRoomIds =
        (currentUserChatRoomIdsResult as FireStoreResult.Success).data

      if (currentUserFirebaseChatRoomIds.isEmpty()) {
        updateErrorMessage(StringErrorMessage("Got currentUserChatRoomIds as empty"))
        return@launch
      }

      if (FirebaseUtils.currentUser == null) {
        updateErrorMessage(StringErrorMessage("Got Firebase Current user as null"))
        return@launch
      }

      val getLocalChatRoomIdsResult = localRepo.getChatRoomIdsForCurrentUser(
        scope = viewModelScope,
        userId = FirebaseUtils.currentUser!!.uid
      )

      if (getLocalChatRoomIdsResult is LocalDbResult.LocalError) {
        updateErrorMessage(StringErrorMessage(getLocalChatRoomIdsResult.errorMessage))
        return@launch
      }

      val currentUserLocalChatRoomIds =
        (getLocalChatRoomIdsResult as LocalDbResult.LocalSuccess).data

      val newChatRoomIds = if (currentUserLocalChatRoomIds.isEmpty()) {
        //Local Data is empty we can get all chatRoom details from firebase
        currentUserFirebaseChatRoomIds.toSet()
      } else {
        currentUserFirebaseChatRoomIds.toSet() - currentUserLocalChatRoomIds.toSet()
      }

      if (newChatRoomIds.isEmpty()) {
        // Each online ChatRoom synced with Local ChatRooms no updated needed
      } else {

        val homeChatListResult =
          FirebaseUtils.getHomeChatListForChatRoomIds(viewModelScope, newChatRoomIds.toList())

        if (homeChatListResult is FireStoreResult.Error) {
          updateErrorMessage(StringErrorMessage(homeChatListResult.errorMessage))
          return@launch
        }
        val homeChatList = (homeChatListResult as FireStoreResult.Success).data

        homeChatList.map { homeChatUIModel ->

          val localChatRoom = LocalChatRoom(
            roomId = homeChatUIModel.roomId,
            userId = FirebaseUtils.currentUser!!.uid,
            lastMessageUpdatedAt = -1,
          )

          val saveLocalChatRoomResult = localRepo.saveChatRoomDetail(
            scope = viewModelScope,
            chatRoom = localChatRoom
          )

          if (saveLocalChatRoomResult is LocalDbResult.LocalError) {
            updateErrorMessage(StringErrorMessage(saveLocalChatRoomResult.errorMessage))
            return@map
          }

          val latestLocalMessage = LocalMessage(
            id = homeChatUIModel.lastMessageInChatRoom.messageId,
            roomId = homeChatUIModel.roomId,
            isSentByMessageMode = homeChatUIModel.lastMessageInChatRoom.isSentInMessageMode,
            message = homeChatUIModel.lastMessageInChatRoom.message,
            receivedTime = homeChatUIModel.lastMessageInChatRoom.receivedTime,
            receiverId = homeChatUIModel.lastMessageInChatRoom.receiverId,
            senderId = homeChatUIModel.lastMessageInChatRoom.senderId,
            sentTime = homeChatUIModel.lastMessageInChatRoom.sentTime,
            status = homeChatUIModel.lastMessageInChatRoom.status
          )

          val saveLocalMessageResult = localRepo.saveMessage(
            scope = viewModelScope,
            localMessage = latestLocalMessage
          )

          if (saveLocalMessageResult is LocalDbResult.LocalError) {
            updateErrorMessage(StringErrorMessage(saveLocalMessageResult.errorMessage))
            return@map
          }

          val localReceiverDetails = LocalReceiverDetail(
            number = homeChatUIModel.receiverDetails.number,
            userId = homeChatUIModel.receiverDetails.userId,
            roomId = homeChatUIModel.roomId,
            name = homeChatUIModel.receiverDetails.name,
            photo = homeChatUIModel.receiverDetails.photo,
            //todo save proper timestamp later
            lastProfileUpdatedAt = -1
          )

          val saveLocalReceiverDetailResult = localRepo.saveReceiverDetail(
            scope = viewModelScope,
            localReceiverDetails = localReceiverDetails
          )

          if (saveLocalReceiverDetailResult is LocalDbResult.LocalError) {
            updateErrorMessage(StringErrorMessage(saveLocalReceiverDetailResult.errorMessage))
            return@map
          }

          // we saved updated Chat list from Firebase into LocalDB
          // ChatRoom, ReceiverDetail, Message

          getLocalHomeChats()
          viewModelScope.launch(Dispatchers.IO) {
            listenLatestHomeChatsUpdateIfNeeded()
          }

        }
      }
    }
  }

  private var listenHomeChatListJOb: Job? =  null

  private suspend fun listenLatestHomeChatsUpdateIfNeeded() {
    listenHomeChatListJOb?.cancel()
    listenHomeChatListJOb = viewModelScope.launch {
      FirebaseUtils.listenChatRoomUpdates(
        scope = viewModelScope,
        onFailed = { updateErrorMessage(StringErrorMessage(it)) },
        onSuccess = { currentUserHomeChatUIModelList ->

          if (currentUserHomeChatUIModelList.isEmpty()) {
            return@listenChatRoomUpdates
          }

          if (FirebaseUtils.currentUser == null) {
            updateErrorMessage(StringErrorMessage("Got Firebase Current User as null"))
            return@listenChatRoomUpdates
          }

          viewModelScope.launch {

            val localChatRoomIdsResult =
              localRepo.getChatRoomIdsForCurrentUser(
                viewModelScope,
                FirebaseUtils.currentUser!!.uid
              )

            if (localChatRoomIdsResult is LocalDbResult.LocalError) {
              updateErrorMessage(StringErrorMessage(localChatRoomIdsResult.errorMessage))
              return@launch
            }

            val localChatRoomIds = (localChatRoomIdsResult as LocalDbResult.LocalSuccess).data
            val firebaseChatRoomIds = currentUserHomeChatUIModelList.map { it.roomId }

            val firebaseChatRoomIdsMap = currentUserHomeChatUIModelList.associateBy { it.roomId }

            val newChatRoomIds = firebaseChatRoomIds.toSet() - localChatRoomIds.toSet()


            if (newChatRoomIds.isEmpty()) {
              //each chatRoomId aleady exists in local DB we can ignore this scenario
              return@launch
            }

            val newHomeChatUIModelList = newChatRoomIds.mapNotNull { firebaseChatRoomIdsMap[it] }

            if (newHomeChatUIModelList.isEmpty()) {
              //no new updates listened from firebase we can ignore this update
            } else {
              newHomeChatUIModelList.map { homeChatUIModel ->

                val localChatRoom = LocalChatRoom(
                  roomId = homeChatUIModel.roomId,
                  userId = FirebaseUtils.currentUser!!.uid,
                  lastMessageUpdatedAt = -1,
                )

                val localReceiverDetails = LocalReceiverDetail(
                  number = homeChatUIModel.receiverDetails.number,
                  userId = homeChatUIModel.receiverDetails.userId,
                  roomId = homeChatUIModel.roomId,
                  name = homeChatUIModel.receiverDetails.name,
                  photo = homeChatUIModel.receiverDetails.photo,
                  //todo save proper timestamp later
                  lastProfileUpdatedAt = -1
                )

                val latestLocalMessage = LocalMessage(
                  id = homeChatUIModel.lastMessageInChatRoom.messageId,
                  roomId = homeChatUIModel.roomId,
                  isSentByMessageMode = homeChatUIModel.lastMessageInChatRoom.isSentInMessageMode,
                  message = homeChatUIModel.lastMessageInChatRoom.message,
                  receivedTime = homeChatUIModel.lastMessageInChatRoom.receivedTime,
                  receiverId = homeChatUIModel.lastMessageInChatRoom.receiverId,
                  senderId = homeChatUIModel.lastMessageInChatRoom.senderId,
                  sentTime = homeChatUIModel.lastMessageInChatRoom.sentTime,
                  status = homeChatUIModel.lastMessageInChatRoom.status
                )

                 if (listenHomeChatListJOb == null) return@launch

                //todo test if homeChatLocalMessages contains messages which is already present in Local DB

                  val saveChatRoomDetailsResult = localRepo.saveChatRoomDetail(
                    scope = viewModelScope,
                    chatRoom = localChatRoom
                  )
                  if (saveChatRoomDetailsResult is LocalDbResult.LocalError) {
                    updateErrorMessage(StringErrorMessage(saveChatRoomDetailsResult.errorMessage))
                    return@launch
                  }

                  val saveReceiverDetailResult = localRepo.saveReceiverDetail(
                    scope = viewModelScope,
                    localReceiverDetails = localReceiverDetails
                  )
                  if (saveReceiverDetailResult is LocalDbResult.LocalError) {
                    updateErrorMessage(StringErrorMessage(saveReceiverDetailResult.errorMessage))
                    return@launch
                  }

                  val saveLocalMessageResult = localRepo.saveMessage(
                    scope = viewModelScope,
                    localMessage = latestLocalMessage
                  )

                  if (saveLocalMessageResult is LocalDbResult.LocalError) {
                    updateErrorMessage(StringErrorMessage(saveLocalMessageResult.errorMessage))
                    return@launch
                  }

                  // we saved updated Chat list from Firebase into LocalDB
                  // ChatRoom, ReceiverDetail, Message
                  getLocalHomeChats()
              }
            }

          }
        }
      )
    }
  }

  fun cancelFirebaseHomeChatListener() {
    listenHomeChatListJOb?.cancel()
    listenHomeChatListJOb = null
  }

  fun getLocalHomeChats() {
    localRepo.getChatRoomsForCurrentUser(
      scope = viewModelScope,
      userId = FirebaseUtils.currentUser!!.uid,
      onFailed = {
        updateErrorMessage(StringErrorMessage(it))
      },
      onSuccess = {
        updateChatList(it)
      })
  }

}

data class HomeChatUIModel(
  val roomId: String,
  val receiverDetails: ChatReceiverDetails,
  val lastMessageInChatRoom: LatestHomeChatMessage,
  val roomCreatedAt: Long
)

data class ChatReceiverDetails(
  val userId: String,
  val fcmToken: String,
  val number: String,
  val name: String,
  val photo: String,
  val isReceiverOnline: Boolean = false
)

data class LatestHomeChatMessage(
  val sentTime: Long,
  val receivedTime: Long,
  val senderId: String,
  val receiverId: String,
  val message: String,
  val status: Int,
  val isSentInMessageMode: Boolean,
  val messageId: Int
)