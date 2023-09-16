package com.invorel.blankchatpro.viewModels

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invorel.blankchatpro.constants.DEFAULT_FB_NAME_ABOUT_SEPARATOR
import com.invorel.blankchatpro.constants.DataStoreManager
import com.invorel.blankchatpro.extensions.isNotNullAndNotEmpty
import com.invorel.blankchatpro.others.ErrorMessage
import com.invorel.blankchatpro.others.ErrorMessage.StringErrorMessage
import com.invorel.blankchatpro.state.HomeUiState
import com.invorel.blankchatpro.utils.FirebaseUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeViewModel(dataStore: DataStore<Preferences>) : ViewModel() {

  private val _uiState = MutableStateFlow(HomeUiState())
  val uiState = _uiState.asStateFlow()

  private val dataStoreManager = DataStoreManager(dataStore)

  init {
    refreshDataFromFb()
  }

  fun refreshDataFromFb() {
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

  fun updateUserName(name: String) {
    _uiState.value = _uiState.value.copy(userName = name)
  }

  fun updateUserAbout(about: String) {
    _uiState.value = _uiState.value.copy(userAbout = about)
  }

  private fun updateErrorMessage(message: ErrorMessage) {
    _uiState.value = _uiState.value.copy(errorMessage = message)
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
    FirebaseUtils.updateUserStatus(true, onFailed = {
      updateErrorMessage(StringErrorMessage(it))
    }, onUserStatusUpdated = {

    })
  }

  public override fun onCleared() {
    super.onCleared()
  }
}