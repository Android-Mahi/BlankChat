package com.invorel.blankchatpro.viewModels

import android.app.Activity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
import com.invorel.blankchatpro.constants.DEFAULT_OTP_RESEND_TIME_SECONDS
import com.invorel.blankchatpro.constants.DataStoreManager
import com.invorel.blankchatpro.online.fb_collections.User
import com.invorel.blankchatpro.state.LoginUiState
import com.invorel.blankchatpro.utils.FirebaseUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LoginViewModel(private val dataStore: DataStore<Preferences>) : ViewModel() {

  private val _uiState = MutableStateFlow(LoginUiState())
  val uiState = _uiState.asStateFlow()

  private val fbVerificationId = MutableStateFlow("")
  private val fbResendingToken: MutableLiveData<ForceResendingToken> = MutableLiveData()
  private val fbPhoneAUthCredential: MutableLiveData<PhoneAuthCredential> = MutableLiveData()

  fun updatePhoneNo(no: String) {
    _uiState.value = _uiState.value.copy(phoneNo = no)
  }

  fun toggleRememberMeCheckedState() {
    _uiState.value =
      _uiState.value.copy(isRememberMeChecked = _uiState.value.isRememberMeChecked.not())
  }

  fun updateOTP(value: String) {
    _uiState.value = _uiState.value.copy(otp = value)
  }

  fun updatePhoneNoState() {
    _uiState.value = _uiState.value.copy(isInOtpMode = false)
  }

  private fun updateOtpScreenState() {
    _uiState.value = _uiState.value.copy(isInOtpMode = true, otp = "")
  }

  fun updateRemainingTime(remainingTime: Int) {
    _uiState.value = _uiState.value.copy(remainingTimeToResendOtp = remainingTime)
  }

  fun updateTimerStarted() {
    _uiState.value = _uiState.value.copy(isTimerInProgress = true)
  }

  fun updateTimerCompleted() {
    _uiState.value = _uiState.value.copy(
      isTimerInProgress = false,
      remainingTimeToResendOtp = DEFAULT_OTP_RESEND_TIME_SECONDS
    )
  }

  fun showExitBottomSheet() {
    _uiState.value = _uiState.value.copy(isExitSheetShown = true)
  }

  fun hideExitBottomSheet() {
    _uiState.value = _uiState.value.copy(isExitSheetShown = false)
  }

  fun updateResendOtpClicked(value: Boolean) {
    _uiState.value = _uiState.value.copy(isResendClicked = value)
  }

  private fun updateErrorMessage(message: String) {
    _uiState.value = _uiState.value.copy(errorMessage = message)
  }

  fun clearErrorMessage() {
    _uiState.value = _uiState.value.copy(errorMessage = "")
  }

  private fun updateSignInSuccess() {
    _uiState.value = _uiState.value.copy(signInSuccess = true)
  }

  fun clearSignInState() {
    _uiState.value = _uiState.value.copy(signInSuccess = false)
  }

  private fun showLoading() {
    _uiState.value = _uiState.value.copy(isFbRequestInProgress = true)
  }

  private fun hideLoading() {
    _uiState.value = _uiState.value.copy(isFbRequestInProgress = false)
  }

  //Firebase Related Methods

  fun clearFirebaseData() {
    fbVerificationId.value = ""
    fbResendingToken.value = null
    fbPhoneAUthCredential.value = null
  }

  fun sendOTPRequestToFirebase(activity: Activity) {
    showLoading()
    FirebaseUtils.verifyPhoneNumber(
      phoneNo = uiState.value.phoneNo,
      activity = activity,
      onCodeSent = { verificationId, resendToken ->
        fbVerificationId.value = verificationId
        fbResendingToken.value = resendToken
        updateOtpScreenState()
        hideLoading()
      },
      onVerificationFailed = {
        updateErrorMessage(it)
        hideLoading()
      },
      onVerificationSuccess = {
        fbPhoneAUthCredential.value = it
        signInWithFireBase()
        hideLoading()
      }
    )
  }

  fun signInWithFireBase() {
    showLoading()
    if (fbPhoneAUthCredential.value == null) {
      val phoneAuthCredential = PhoneAuthProvider.getCredential(
        /* verificationId = */ fbVerificationId.value,
        /* smsCode = */ uiState.value.otp
      )
      fbPhoneAUthCredential.value = phoneAuthCredential
    }

    if (fbPhoneAUthCredential.value == null) {
      updateErrorMessage("No Credentials Found")
      hideLoading()
      return
    }

    FirebaseUtils.signInWithPhoneAuthCredentials(
      credential = fbPhoneAUthCredential.value!!,
      onSignInSuccess = { fbUser ->

        if (fbUser == null) {
          updateErrorMessage("Got Null Fb user")
          return@signInWithPhoneAuthCredentials
        }

        saveUserDetailsInBackend(
          fbUser = fbUser,
          onUserDetailsUpdatedInBacked = { updateSignInSuccess() }
        )

        hideLoading()
      }, onSignInFailed = {
        updateErrorMessage(it)
        hideLoading()
      }
    )
  }

  private fun saveUserDetailsInBackend(fbUser: FirebaseUser, onUserDetailsUpdatedInBacked: () -> Unit) {
    viewModelScope.launch(Dispatchers.IO) {
      val manager = DataStoreManager(dataStore)
      manager.fcmTokenKey.collectLatest { token ->
        if (token == null) {
          updateErrorMessage("Got Null FCM Token from DataStore")
          return@collectLatest
        }

        FirebaseUtils.checkIfTheUserExistsInFirebase(
          mobileNumber = fbUser.phoneNumber ?: uiState.value.phoneNo,
          onFailed = ::updateErrorMessage,
          onUserExistStatusFetched = { isUserExists ->
            //update user details in firebase
            val userDocument = User(
              userId = fbUser.uid,
              mobileNumber = fbUser.phoneNumber ?: uiState.value.phoneNo,
              fcmToken = token,
              chatRoomIds = listOf(),
            )
            if (isUserExists) {
              FirebaseUtils.updateUserDetailsInFirebase(
                mobileNumber = userDocument.mobileNumber,
                userId = fbUser.uid,
                fcmToken = token,
                onUserDetailsUpdated = { onUserDetailsUpdatedInBacked.invoke() },
                onFailed = ::updateErrorMessage,
              )
            } else {
              FirebaseUtils.saveUserDetailsInFirebase(
                user = userDocument,
                onUserDetailsSaved = { onUserDetailsUpdatedInBacked.invoke() },
                onUserDetailsFailedToSave = ::updateErrorMessage
              )
            }
          })

      }
    }
  }

}