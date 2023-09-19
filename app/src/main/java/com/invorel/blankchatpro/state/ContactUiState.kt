package com.invorel.blankchatpro.state

import android.graphics.Bitmap

data class ContactUiState(
  val contactsList: List<Contact> = listOf(),
  val actionFetchContacts: Boolean = false,
  val fetchInProgress: Boolean = false,
  val isSearchBarState: Boolean = false,
  val searchQuery: String = "",
)

data class Contact(
  val id: Long,
  val name: String,
  val number: String,
  val photo: Bitmap?,
)
