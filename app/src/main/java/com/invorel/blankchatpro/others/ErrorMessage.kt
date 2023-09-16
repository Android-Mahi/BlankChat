package com.invorel.blankchatpro.others

import androidx.annotation.StringRes

sealed class ErrorMessage {
  class StringErrorMessage(val message: String) : ErrorMessage()
  class StringResErrorMessage(@StringRes val stringRes: Int) : ErrorMessage()
}
