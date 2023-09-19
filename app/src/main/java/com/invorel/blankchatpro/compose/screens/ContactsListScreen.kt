package com.invorel.blankchatpro.compose.screens

import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextAlign.Companion
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.invorel.blankchatpro.R.string
import com.invorel.blankchatpro.compose.common.BackPressHandler
import com.invorel.blankchatpro.compose.common.BlankTextField
import com.invorel.blankchatpro.compose.common.HorizontalSpacer
import com.invorel.blankchatpro.compose.common.VerticalSpacer
import com.invorel.blankchatpro.constants.DEFAULT_PROFILE_MAN_IMAGE
import com.invorel.blankchatpro.extensions.clickableWithoutRipple
import com.invorel.blankchatpro.state.Contact
import com.invorel.blankchatpro.ui.theme.black
import com.invorel.blankchatpro.ui.theme.lightGrey
import com.invorel.blankchatpro.ui.theme.white
import com.invorel.blankchatpro.utils.ContentResolverUtils
import com.invorel.blankchatpro.viewModels.ContactsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(VERSION_CODES.O) @Composable fun ContactsListScreen(
  modifier: Modifier = Modifier,
  viewModel: ContactsViewModel,
  onContactPicked: (Contact) -> Unit,
  onBackPressed: () -> Unit
) {

  val state = viewModel.uiState.collectAsState().value
  val context = LocalContext.current
  val filteredContacts =  remember(state.searchQuery) {
    derivedStateOf {
      state.contactsList.filter { it.name.contains(state.searchQuery, ignoreCase = true) || it.number.contains(state.searchQuery, ignoreCase = true) }
    }
  }

  BackPressHandler {
    if (state.isSearchBarState) {
      viewModel.updateSearchQuery("")
      viewModel.showSearchBar(false)
    } else {
      onBackPressed.invoke()
    }
  }

  Column(
    modifier = modifier,
  ) {
    
    VerticalSpacer(space = 15)

    AnimatedVisibility(visible = state.isSearchBarState) {
      BlankTextField(
        modifier = Modifier.padding(horizontal = 15.dp),
        value = state.searchQuery,
        hint = stringResource(string.search_contact_hint),
        onValueChanged = { viewModel.updateSearchQuery(it) },
        onClearClicked = { viewModel.updateSearchQuery("") },
        showClearIcon = true,
        onFieldUnFocused = {})
    }

    AnimatedVisibility(visible = state.isSearchBarState.not()) {

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically
      )
      {
        Text(
          modifier = Modifier.weight(1f),
          text = stringResource(string.pick_contact),
          fontWeight = FontWeight.Bold,
          fontSize = 25.sp,
          textAlign = TextAlign.Start
        )

        AnimatedVisibility(visible = state.fetchInProgress.not()) {
          Icon(
            modifier = Modifier
              .padding(end = 15.dp)
              .size(24.dp)
              .clickableWithoutRipple {
                viewModel.showSearchBar(true)
              },
            imageVector = Icons.Filled.Search,
            contentDescription = stringResource(string.cd_contacts_refresh_icon),
            tint = white
          )
        }

        HorizontalSpacer(space = 10)

        AnimatedVisibility(visible = state.fetchInProgress.not()) {
          Icon(
            modifier = Modifier
              .padding(end = 15.dp)
              .size(24.dp)
              .clickableWithoutRipple {
                viewModel.clearExistingContacts()
                viewModel.updateActionRefreshContacts(true)
              },
            imageVector = Icons.Filled.Refresh,
            contentDescription = stringResource(string.cd_contacts_refresh_icon),
            tint = white
          )

        }
      }

    }

    VerticalSpacer(space = 25)

    if (state.fetchInProgress) {
      Box(modifier = modifier.fillMaxSize()) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = white)
      }
    } else {
      if (state.contactsList.isEmpty()) {

        Box(modifier = modifier.fillMaxSize()) {
          Text(
            modifier = Modifier.align(Alignment.Center),
            text = "No contacts available for now",
            textAlign = Companion.Center
          )
        }
      } else {
        LazyColumn(modifier = Modifier.background(black)) {
          itemsIndexed(filteredContacts.value) { _, item ->
            ContactItem(
              modifier = Modifier,
              data = item,
              onClick = onContactPicked
            )
          }
        }
      }
    }

  }

  LaunchedEffect(Unit) {
    viewModel.retrieveContacts()
  }

  LaunchedEffect(state.actionFetchContacts) {
    if (state.actionFetchContacts) {
      viewModel.showLoading()
      this.launch(Dispatchers.IO) {
        ContentResolverUtils.getContactsWithNumber(
          contentResolver = context.contentResolver,
          onContactsFetched = { phoneContacts ->
            if (phoneContacts.isNotEmpty()) {
              viewModel.updateActionRefreshContacts(false)
              viewModel.saveContactsInLocalDb(phoneContacts)
            }
          }
        )
      }
    }
  }
}

@Composable
fun ContactItem(
  modifier: Modifier = Modifier,
  data: Contact,
  onClick: (Contact) -> Unit,
) {

  Row(
    modifier = modifier
      .padding(15.dp)
      .clip(RoundedCornerShape(15.dp))
      .fillMaxWidth()
      .background(lightGrey)
      .padding(15.dp)
      .clickable { onClick.invoke(data) },
    verticalAlignment = Alignment.CenterVertically
  ) {

    AsyncImage(
      modifier = Modifier
        .size(50.dp)
        .clip(RoundedCornerShape(10.dp)),
      model = data.photo ?: DEFAULT_PROFILE_MAN_IMAGE,
      contentDescription = "Contact Photo",
      contentScale = ContentScale.Crop
    )

    HorizontalSpacer(space = 15)

    Column(
      horizontalAlignment = Alignment.Start,
      verticalArrangement = Arrangement.Center
    ) {
      Text(
        modifier = Modifier.clickable {
          onClick.invoke(data)
        },
        text = data.name,
        color = black
      )

      VerticalSpacer(space = 5)

      Text(
        modifier = Modifier.clickable {
          onClick.invoke(data)
        },
        text = data.number,
        color = black
      )

    }

  }
}

