package com.invorel.blankchatpro.state

import com.invorel.blankchatpro.constants.DEFAULT_OTP_RESEND_TIME_SECONDS

data class LoginUiState(
  val phoneNo: String = "",
  val isRememberMeChecked: Boolean = false,
  val otp: String = "",
  val isInOtpMode: Boolean = false,
  val remainingTimeToResendOtp: Int = DEFAULT_OTP_RESEND_TIME_SECONDS,
  val isTimerInProgress: Boolean = false,
  val isExitSheetShown: Boolean = false,
  val isResendClicked: Boolean = false,
  val errorMessage: String = "",
  val signInSuccess: Boolean = false,
  val isFbRequestInProgress: Boolean = false,
)
