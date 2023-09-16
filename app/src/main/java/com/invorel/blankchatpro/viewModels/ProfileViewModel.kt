package com.invorel.blankchatpro.viewModels

import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invorel.blankchatpro.R.string
import com.invorel.blankchatpro.constants.DEFAULT_FB_NAME_ABOUT_SEPARATOR
import com.invorel.blankchatpro.constants.DataStoreManager
import com.invorel.blankchatpro.extensions.isNotNullAndNotEmpty
import com.invorel.blankchatpro.others.ErrorMessage
import com.invorel.blankchatpro.others.ErrorMessage.StringErrorMessage
import com.invorel.blankchatpro.others.ErrorMessage.StringResErrorMessage
import com.invorel.blankchatpro.state.ProfileUiState
import com.invorel.blankchatpro.utils.FirebaseUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileViewModel(dataStore: DataStore<Preferences>) : ViewModel() {

  private val _uiState = MutableStateFlow(ProfileUiState())
  val uiState = _uiState.asStateFlow()

  private val dataStoreManager = DataStoreManager(dataStore)

  init {
    refreshDataFromFB()
  }

  fun refreshDataFromFB() {
    val nameAndAbout = FirebaseUtils.currentUser?.displayName.orEmpty()
    if (nameAndAbout.isNotEmpty()) {
      with(nameAndAbout.split(DEFAULT_FB_NAME_ABOUT_SEPARATOR)) {
        updateUserName(this[0])
        updateUserAbout(this[1])
      }
    }
    updateUserPhoneNo(FirebaseUtils.currentUser?.phoneNumber.orEmpty())
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

  fun updateSelectedPhotoUri(uri: Uri) {
    _uiState.value = _uiState.value.copy(selectedPhotoUri = uri)
  }

  fun updateUserName(name: String) {
    _uiState.value = _uiState.value.copy(userName = name)
  }

  fun clearUserName() {
    _uiState.value = _uiState.value.copy(userName = "")
  }

  fun updateUserAbout(about: String) {
    _uiState.value = _uiState.value.copy(userAbout = about)
  }

  fun clearUserAbout() {
    _uiState.value = _uiState.value.copy(userAbout = "")
  }

  private fun updateUserPhoneNo(no: String) {
    _uiState.value = _uiState.value.copy(userNumber = no)
  }

  private fun updateErrorMessage(message: ErrorMessage) {
    _uiState.value = _uiState.value.copy(errorMessage = message)
  }

  fun clearErrorMessage() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }

  private fun showLoading() {
    _uiState.value = _uiState.value.copy(fbRequestInProcess = true)
  }

  private fun hideLoading() {
    _uiState.value = _uiState.value.copy(fbRequestInProcess = false)
  }

  private fun isInputDataValid(): Boolean {
    with(uiState.value) {
      if (userName.isEmpty() &&
        userAbout.isEmpty() &&
        selectedPhotoUri == Uri.EMPTY
      ) {
        return false
      }
    }
    return true
  }

  private fun updateFireBaseProfileImgUrl(downloadUrl: String) {
    _uiState.value = _uiState.value.copy(fireBaseProfileImgUrl = downloadUrl)
    saveFirebaseProfileImageUrlInDataStore(downloadUrl)
  }

  fun updateProfileDetailsInFirebase() {

    if (uiState.value.selectedPhotoUri != Uri.EMPTY) {
      updateProfilePhotoInFirebaseStorage(uiState.value.selectedPhotoUri)
    }

    if (isInputDataValid().not()) {
      updateErrorMessage(StringResErrorMessage(string.no_updates_detected))
      return
    }

    var nameAndAbout: String
    with(uiState.value) {
      nameAndAbout = userName.plus(DEFAULT_FB_NAME_ABOUT_SEPARATOR).plus(userAbout)
    }

    showLoading()
    FirebaseUtils.updateNameAndAboutInFirebaseAuthUserProfile(
      nameAndAbout = nameAndAbout,
      onFailed = {
        if (it.isEmpty()) {
          updateErrorMessage(StringResErrorMessage(string.profile_update_failed))
        } else {
          updateErrorMessage(StringErrorMessage(it))
        }
        hideLoading()
      },
      onSuccess = {
        saveProfileDetailsInBackend()
        hideLoading()
      })
  }

  private fun saveProfileDetailsInBackend() {
    FirebaseUtils.updateUserDetailsInFirebase(
      mobileNumber = uiState.value.userNumber,
      name = uiState.value.userName,
      about = uiState.value.userAbout,
      profilePhoto = uiState.value.fireBaseProfileImgUrl,
      onFailed = { updateErrorMessage(StringErrorMessage(it)) },
      onUserDetailsUpdated = {
        updateErrorMessage(StringResErrorMessage(string.profile_updated))
      },
      //Todo update below field value properly after the Gender Option is implemented
      gender = 0,
    )
  }

  private fun updateProfilePhotoInFirebaseStorage(
    imageUri: Uri,
  ) {
    showLoading()
    FirebaseUtils.uploadPhotoInFirebase(
      imageUri = imageUri,
      onProfileImageUploaded = { downloadUrl ->
        updateFireBaseProfileImgUrl(downloadUrl)
        hideLoading()
      },
      onProfileImageUploadFailed = { errorMessage ->
        updateErrorMessage(StringErrorMessage(errorMessage))
        hideLoading()
      }
    )
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

  public override fun onCleared() {
    super.onCleared()
  }
}