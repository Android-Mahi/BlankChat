package com.invorel.blankchatpro.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invorel.blankchatpro.local.database.BlankLocalDatabase
import com.invorel.blankchatpro.mappers.toModels
import com.invorel.blankchatpro.mappers.toPojos
import com.invorel.blankchatpro.state.Contact
import com.invorel.blankchatpro.state.ContactUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContactsViewModel(private val localDatabase: BlankLocalDatabase) : ViewModel() {

  private val _uiState = MutableStateFlow(ContactUiState())
  val uiState = _uiState.asStateFlow()

  init {
    retrieveContacts()
  }

  private fun updateContactsList(list: List<Contact>) {
    _uiState.value = _uiState.value.copy(contactsList = list)
  }

  fun retrieveContacts() {
    viewModelScope.launch (Dispatchers.IO){
      val contacts = localDatabase.contactsDao().getContacts()
      if (contacts.isNotEmpty()) {
        updateContactsList(contacts.toPojos())
      } else {
        updateActionRefreshContacts(true)
      }
    }
  }

  fun saveContactsInLocalDb(list: List<Contact>) {
    val isContactsSavedLocally =
      localDatabase.contactsDao().insertContacts(list.toModels()).all { it != -1L }
    if (isContactsSavedLocally) {
      hideLoading()
      updateContactsList(list)
    }
  }

  fun updateActionRefreshContacts(value: Boolean) {
    _uiState.value = _uiState.value.copy(actionFetchContacts = value)
  }

  fun clearExistingContacts() {
    _uiState.value = _uiState.value.copy(contactsList = listOf())
  }

  fun showLoading() {
    _uiState.value = _uiState.value.copy(fetchInProgress = true)
  }

  private fun hideLoading() {
    _uiState.value = _uiState.value.copy(fetchInProgress = false)
  }

  fun showSearchBar(value: Boolean) {
    _uiState.value = _uiState.value.copy(isSearchBarState = value)
  }

  fun updateSearchQuery(query: String) {
    _uiState.value = _uiState.value.copy(searchQuery = query)
  }

}