package com.invorel.blankchatpro.state

import android.net.Uri
import com.invorel.blankchatpro.others.ErrorMessage

data class ProfileUiState(
  val selectedPhotoUri: Uri = Uri.EMPTY,
  val userName: String = "",
  val userAbout: String = "",
  val userNumber: String = "",
  val errorMessage: ErrorMessage? = null,
  val fbRequestInProcess: Boolean = false,
  val fireBaseProfileImgUrl: String = "",
)
