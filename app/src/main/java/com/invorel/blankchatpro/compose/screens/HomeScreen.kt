package com.invorel.blankchatpro.compose.screens

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invorel.blankchatpro.R.drawable
import com.invorel.blankchatpro.R.string
import com.invorel.blankchatpro.compose.common.BlankProfileCard
import com.invorel.blankchatpro.compose.common.BlankSwipeAbleCard
import com.invorel.blankchatpro.compose.common.HorizontalSpacer
import com.invorel.blankchatpro.compose.common.VerticalSpacer
import com.invorel.blankchatpro.constants.DEFAULT_PROFILE_MAN_IMAGE
import com.invorel.blankchatpro.extensions.isPermissionGranted
import com.invorel.blankchatpro.extensions.showToast
import com.invorel.blankchatpro.others.ErrorMessage.StringErrorMessage
import com.invorel.blankchatpro.others.ErrorMessage.StringResErrorMessage
import com.invorel.blankchatpro.ui.theme.black
import com.invorel.blankchatpro.ui.theme.btn_end_color
import com.invorel.blankchatpro.ui.theme.btn_start_color
import com.invorel.blankchatpro.ui.theme.grey
import com.invorel.blankchatpro.ui.theme.lightGrey
import com.invorel.blankchatpro.ui.theme.white
import com.invorel.blankchatpro.utils.IntentUtils
import com.invorel.blankchatpro.viewModels.ChatReceiverDetails
import com.invorel.blankchatpro.viewModels.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("UnrememberedMutableInteractionSource")
@Composable
fun HomeScreen(
  modifier: Modifier = Modifier,
  viewModel: HomeViewModel,
  onMoreClickedInProfile: () -> Unit,
  onNewChatOptionClick: () -> Unit,
  onChatRoomPicked: (ChatRoomDataHolder) -> Unit
) {

  val context = LocalContext.current
  val state = viewModel.uiState.collectAsState().value
  val scope = rememberCoroutineScope()

  val contactsPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = { isGranted ->
      if (isGranted) {
        onNewChatOptionClick.invoke()
      } else {
        context.showToast(context.getString(string.contacts_permission_denied_rational_message))
        scope.launch {
          delay(1000)
          IntentUtils.openSettingsPage(context)
        }
      }
    })

  LaunchedEffect(Unit) {
    with(viewModel) {
      checkLatestChatsFromBackendAndUpdateIfNeeded()
      refreshUserProfileDataFromFb()
      updateUserStatusOnlineInBackend()
      //Todo call this method based on localTimeStamp later
      //Todo listen for each chatRooms updates separately from backend
      getLocalHomeChats()
      //listenLatestHomeChatsUpdateIfNeeded()
    }
  }

  LaunchedEffect(state.errorMessage) {
    if (state.errorMessage != null) {
      val message = when(state.errorMessage) {
        is StringErrorMessage -> state.errorMessage.message
        is StringResErrorMessage -> context.getString(state.errorMessage.stringRes)
      }

      if (message.isNotEmpty()) {
        context.showToast(message)
        viewModel.resetErrorMessage()
      }
    }
  }

  Column(modifier = modifier) {

    BlankProfileCard(
      profileImageUr = state.userImage.ifEmpty { DEFAULT_PROFILE_MAN_IMAGE },
      userName = state.userName,
      userAbout = state.userAbout,
      isTrailingNavigationIconVisible = true,
      isVerified = true,
      onMoreClicked = onMoreClickedInProfile
    )

    VerticalSpacer(space = 25)

    Row(
      modifier = Modifier.padding(horizontal = 15.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {

      Text(
        text = if (state.isMessageSwitchChecked) stringResource(string.message) else
          stringResource(string.internet),
        color = white,
        fontSize = 25.sp
      )

      Spacer(modifier = Modifier.weight(1f))

      //Todo hide this icon if the chat list is empty
      Icon(
        modifier = Modifier.size(30.dp),
        imageVector = Icons.Outlined.Search,
        contentDescription = stringResource(string.cd_search_icon),
        tint = white
      )

      HorizontalSpacer(space = 30)

      Switch(
        modifier = Modifier
          .width(41.dp)
          .height(11.dp),
        checked = state.isMessageSwitchChecked,
        colors = SwitchColors(
          checkedThumbColor = btn_end_color,
          uncheckedThumbColor = btn_end_color,
          checkedBorderColor = white,
          uncheckedBorderColor = white,
          checkedIconColor = white,
          uncheckedIconColor = white,
          checkedTrackColor = white,
          uncheckedTrackColor = white,
          disabledCheckedBorderColor = grey,
          disabledCheckedIconColor = grey,
          disabledCheckedThumbColor = grey,
          disabledCheckedTrackColor = grey,
          disabledUncheckedBorderColor = grey,
          disabledUncheckedIconColor = grey,
          disabledUncheckedThumbColor = grey,
          disabledUncheckedTrackColor = grey
        ),
        onCheckedChange = {
          viewModel.toggleMessageSwitchState()
        })

    }

    Column(
      modifier
        .weight(1f)
        .fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {

      Box(
        modifier = Modifier
          .fillMaxSize()
          .clickable(indication = null, interactionSource = MutableInteractionSource(), onClick = {
            if (state.isContactPermissionCardVisible) viewModel.showContactPermissionCard(false)
          })
      ) {

        if (state.homeChatList.isNotEmpty()) {
          LazyColumn {
            itemsIndexed(state.homeChatList) { _, item ->
              BlankSwipeAbleCard(
                homeChatUIModel = item,
                onPrivateChatSwiped = {
                  context.showToast("OnPrivate Chat Swiped")
                }, onArchiveChatSwiped = {
                  context.showToast("OnArchive Chat Swiped")
                }, onChatClicked = { chatRoomDetails ->
                  viewModel.cancelFirebaseHomeChatListener()
                  onChatRoomPicked.invoke(chatRoomDetails)
                })
            }
          }

        } else {
          NoChatAvailable(modifier = Modifier.align(Alignment.Center))
        }

        NewChatPencilIcon(modifier = Modifier.align(Alignment.BottomEnd)) {
          if (context.isPermissionGranted(Manifest.permission.READ_CONTACTS)) {
            onNewChatOptionClick.invoke()
          } else {
            viewModel.showContactPermissionCard(true)
          }
        }

        if (state.isContactPermissionCardVisible && !context.isPermissionGranted(Manifest.permission.READ_CONTACTS)) {
          ContactPermissionCard(onAllowClicked = {
            viewModel.showContactPermissionCard(false)
            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
          })
        }

      }

    }

  }
}

@Composable
fun NoChatAvailable(modifier: Modifier) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Text(
      text = stringResource(string.no_chat_message),
      textAlign = TextAlign.Center,
      color = grey
    )

    Image(
      modifier = Modifier.size(300.dp),
      painter = painterResource(id = drawable.no_chat_img),
      contentDescription = stringResource(
        string.cd_no_chat_image
      )
    )

  }
}

@Composable
fun NewChatPencilIcon(modifier: Modifier, onClick: () -> Unit) {
  Box(
    modifier = modifier
      .padding(end = 20.dp, bottom = 20.dp)
      .background(color = lightGrey, shape = RoundedCornerShape(17.dp))
      .padding(14.dp)
      .clickable { onClick.invoke() }
  ) {
    Icon(
      modifier = Modifier
        .size(28.dp),
      imageVector = Icons.Filled.Edit,
      contentDescription = stringResource(string.cd_chat_edit_pencil_icon),
      tint = Color.Black
    )
  }
}

@SuppressLint("UnrememberedMutableInteractionSource")
@Composable
fun ContactPermissionCard(onAllowClicked: () -> Unit) {

  Column(
    modifier = Modifier
      .padding(20.dp)
      .clip(RoundedCornerShape(30.dp))
      .background(white),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {

    Box(
      modifier = Modifier
        .background(grey)
        .fillMaxWidth()
        .height(110.dp)
        .padding(vertical = 40.dp, horizontal = 105.dp)
        .size(30.dp),
      contentAlignment = Alignment.Center
    ) {

      Image(
        modifier = Modifier,
        painter = painterResource(id = drawable.contacts_img), contentDescription = stringResource(
          string.cd_contacts_permission_img
        )
      )

    }

    VerticalSpacer(space = 40)

    Text(
      modifier = Modifier.padding(horizontal = 10.dp),
      text = stringResource(id = string.contacts_permission_message),
      textAlign = TextAlign.Center,
      color = black
    )

    VerticalSpacer(space = 25)

    Text(
      modifier = Modifier
        .padding(horizontal = 10.dp)
        .clickable(
          indication = null,
          interactionSource = MutableInteractionSource(),
          onClick = onAllowClicked
        ),
      text = stringResource(string.allow),
      textAlign = TextAlign.Center,
      color = btn_start_color
    )

    VerticalSpacer(space = 25)

  }
}

data class ChatRoomDataHolder(
  val roomId: String,
  val receiverDetails: ChatReceiverDetails,
)