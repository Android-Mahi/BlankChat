package com.invorel.blankchatpro.state

import com.invorel.blankchatpro.others.ErrorMessage

data class HomeUiState(
  val userImage: String = "",
  val userName: String = "",
  val userAbout: String = "",
  val isMessageSwitchChecked: Boolean = false,
  val isContactPermissionCardVisible: Boolean = false,
  val errorMessage: ErrorMessage? = null,
  val fbRequestInProcess: Boolean = false,
  val actionRequestContactsAccess: Boolean = false,
)
