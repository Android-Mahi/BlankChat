package com.invorel.blankchatpro.state

import android.graphics.Bitmap

data class ContactUiState(
  val contactsList: List<Contact> = listOf(),
  val actionFetchContacts: Boolean = false,
  val fetchInProgress: Boolean = false
)

data class Contact(
  val id: Long,
  val name: String,
  val number: String,
  val photo: Bitmap?,
  val isOnline: Boolean = false,
)