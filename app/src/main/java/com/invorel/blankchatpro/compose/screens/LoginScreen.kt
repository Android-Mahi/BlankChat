package com.invorel.blankchatpro.compose.screens

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.invorel.blankchatpro.R.drawable
import com.invorel.blankchatpro.R.string
import com.invorel.blankchatpro.compose.common.BackPressHandler
import com.invorel.blankchatpro.compose.common.BlankLoginCard
import com.invorel.blankchatpro.compose.common.ExitBottomSheet
import com.invorel.blankchatpro.constants.DEFAULT_MAX_LENGTH_OTP
import com.invorel.blankchatpro.constants.DEFAULT_MAX_LENGTH_PHONE_NO
import com.invorel.blankchatpro.extensions.showToast
import com.invorel.blankchatpro.utils.TimerUtils
import com.invorel.blankchatpro.viewModels.LoginViewModel
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
  modifier: Modifier = Modifier,
  viewModel: LoginViewModel,
  onBackPressed: () -> Unit,
  onSignInCompleted: () -> Unit,
) {

  val state = viewModel.uiState.collectAsState().value
  val context = LocalContext.current
  val keyboardController = LocalSoftwareKeyboardController.current

  if (state.signInSuccess) {
    viewModel.updateTimerCompleted()
    onSignInCompleted.invoke()
  }

  Box(modifier = modifier) {
    BlankLoginCard(
      modifier = Modifier
        .align(Alignment.Center)
        .padding(15.dp),
      phoneNumber = state.phoneNo,
      otp = state.otp,
      isTimerInProgress = state.isTimerInProgress,
      isFbRequestInProgress = state.isFbRequestInProgress,
      remainingTime = TimerUtils.getRemainingSeconds(state.remainingTimeToResendOtp),
      isOTPMode = state.isInOtpMode,
      isRememberMeChecked = state.isRememberMeChecked,
      onOTPChanged = viewModel::updateOTP,
      onSubmitClicked = {
        if (state.isFbRequestInProgress) return@BlankLoginCard

        if (state.otp.isEmpty()) {
          context.showToast(context.getString(string.empty_otp_message))
          return@BlankLoginCard
        }

        if (state.otp.length < DEFAULT_MAX_LENGTH_OTP) {
          context.showToast(context.getString(string.not_full_otp_message))
          return@BlankLoginCard
        }
        viewModel.signInWithFireBase()
      },
      onPhoneNoChanged = {

        if (it.length > DEFAULT_MAX_LENGTH_PHONE_NO) {
          return@BlankLoginCard
        }
        if (it.length == DEFAULT_MAX_LENGTH_PHONE_NO) {
          keyboardController?.hide()
        }
        if (state.isFbRequestInProgress) return@BlankLoginCard
        viewModel.updatePhoneNo(it)
      },
      onRememberCheckChanged = {
        if (state.isFbRequestInProgress) return@BlankLoginCard
        viewModel.toggleRememberMeCheckedState()
      },
      onProcessClicked = {
        if (state.isFbRequestInProgress) return@BlankLoginCard

        if (state.phoneNo.isEmpty()) {
          context.showToast(context.getString(string.empty_phone_no_message))
          return@BlankLoginCard
        }

        if (state.phoneNo.length < DEFAULT_MAX_LENGTH_PHONE_NO) {
          context.showToast(context.getString(string.not_full_phone_no_message))
          return@BlankLoginCard
        }

        viewModel.sendOTPRequestToFirebase(context as Activity)
      },
      onWrongNumberClicked = {
        if (state.isTimerInProgress) {
          viewModel.showExitBottomSheet()
        } else {
          viewModel.clearFirebaseData()
          viewModel.updateTimerCompleted()
          viewModel.updatePhoneNoState()
        }
      },
      onResendOtpClicked = {
        // Todo request Otp with resend token in FB
        viewModel.updateResendOtpClicked(true)
      }
    )

    Image(
      modifier = Modifier
        .width(250.dp)
        .align(Alignment.BottomEnd),
      painter = painterResource(id = drawable.halflogo),
      contentDescription = stringResource(id = string.cd_login_logo),
      contentScale = ContentScale.Crop
    )

    if (state.isExitSheetShown) {
      ExitBottomSheet(
        onDismissClick = { viewModel.hideExitBottomSheet() },
        onNegativeClick = { viewModel.hideExitBottomSheet() },
        onPositiveClick = {
          viewModel.hideExitBottomSheet()
          viewModel.clearFirebaseData()
          viewModel.updateTimerCompleted()
          viewModel.updatePhoneNoState()
        }
      )
    }

  }

  BackPressHandler {
    if (state.isInOtpMode) {
      if (state.isTimerInProgress) {
        viewModel.showExitBottomSheet()
        //Todo show AlertDialog Confirmation
      } else {
        viewModel.updatePhoneNoState()
      }
    } else onBackPressed.invoke()
  }

  LaunchedEffect(state.errorMessage) {
    if (state.errorMessage.isNotEmpty()) {
      context.showToast(state.errorMessage)
      viewModel.clearErrorMessage()
    }
  }

  //Timer when OTP screen is recomposed initially
  LaunchedEffect(state.isInOtpMode) {
    if (state.isInOtpMode) {
      viewModel.updateTimerStarted()
      var remainingTime = state.remainingTimeToResendOtp
      while (remainingTime > 0) {
        delay(1000)
        remainingTime--
        viewModel.updateRemainingTime(remainingTime)
      }

      if (remainingTime == 0) {
        viewModel.updateTimerCompleted()
        viewModel.updateResendOtpClicked(false)
      }
    }
  }

  //Timer when Resend Button is Clicked
  LaunchedEffect(state.isResendClicked) {
    if (state.isResendClicked) {
      viewModel.updateTimerStarted()
      var remainingTime = state.remainingTimeToResendOtp
      while (remainingTime > 0) {
        delay(1000)
        remainingTime--
        viewModel.updateRemainingTime(remainingTime)
      }

      if (remainingTime == 0) {
        viewModel.updateTimerCompleted()
        viewModel.updateResendOtpClicked(false)
      }
    }
  }

  // we are using different launchedEffect Blocks for OTP Screen Initial Launch & Resend Button Clicks
  // because if we combine due to below condition the effect will be launched again.
  // viewModel.updateResendOtpClicked(false)
  // and due to we are in OTP Screen state.isInOtpMode will be true
  // so the timer will be triggered again once more after clicks the resend button.
  // so due to above conflict we are using different effects for both scenarios with same functionality inside the effect.
}



