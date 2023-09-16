package com.invorel.blankchatpro.utils

object TimerUtils  {
  fun getRemainingSeconds(remainingTimeToResendOtp: Int): String {
    val seconds = when {
      remainingTimeToResendOtp >= 10 -> {
        "0: $remainingTimeToResendOtp"
      }

      else -> {
        "0: 0$remainingTimeToResendOtp"
      }

    }
    return seconds.plus(" ").plus("Sec")
  }
}
