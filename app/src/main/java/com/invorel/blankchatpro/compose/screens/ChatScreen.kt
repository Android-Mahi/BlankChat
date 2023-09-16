package com.invorel.blankchatpro.compose.screens

import android.annotation.SuppressLint
import android.view.ViewTreeObserver
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ContentScale.Companion
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.invorel.blankchatpro.constants.DEFAULT_PROFILE_MAN_IMAGE
import com.invorel.blankchatpro.state.Contact
import com.invorel.blankchatpro.R.string
import com.invorel.blankchatpro.compose.common.BlankMoreIcon
import com.invorel.blankchatpro.compose.common.BlankTextField
import com.invorel.blankchatpro.compose.common.HorizontalSpacer
import com.invorel.blankchatpro.compose.common.VerticalSpacer
import com.invorel.blankchatpro.extensions.showToast
import com.invorel.blankchatpro.ui.theme.black
import com.invorel.blankchatpro.ui.theme.darkGrey
import com.invorel.blankchatpro.ui.theme.lightGrey1
import com.invorel.blankchatpro.ui.theme.white
import com.invorel.blankchatpro.ui.theme.white1
import com.invorel.blankchatpro.viewModels.ChatsViewModel
import com.invorel.blankchatpro.R.drawable
import com.invorel.blankchatpro.online.fb_collections.Message
import com.invorel.blankchatpro.utils.FirebaseUtils

@SuppressLint("UnrememberedMutableInteractionSource")
@Composable
fun ChatScreen(
  modifier: Modifier = Modifier,
  chatRoomId: String = "",
  isCameFromHomeScreen: Boolean,
  viewModel: ChatsViewModel,
  contact: Contact,
  onBackClick: () -> Unit,
) {

  val context = LocalContext.current
  val state = viewModel.uiState.collectAsState().value

  LaunchedEffect(Unit) {
    viewModel.updateReceiverDetails(contact)
    //Todo if the isCameFromHomeScreen false & chatRoomId is empty checks if the
    if (isCameFromHomeScreen.not() && chatRoomId.isEmpty()) {
      // User Opened chat from Contacts Picking Screen
      //viewModel.checkReceiverDetailsAndCreateChatRoomInBackendIfNeeded()
    }
    if (chatRoomId.isNotEmpty()) {
      viewModel.updateChatRoomId(chatRoomId)
    }
  }

  var isKeyboardVisible by remember { mutableStateOf(false) }

  KeyboardVisibilityObserver {
    isKeyboardVisible = it
  }

  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {

    ChatScreenContactInfo(
      contact = contact,
      onContactsSettingsClicked = {
        context.showToast("Settings Clicked")
      },
      onBackArrowClicked = onBackClick
    )

    VerticalSpacer(space = 20)

    LazyColumn(
      modifier = Modifier
        .padding(horizontal = 25.dp)
        .weight(1f)
    ) {
      itemsIndexed(state.chats) { _, chat ->
        ChatItem(details = chat)
      }
    }

    Row(
      modifier = Modifier
        .padding(horizontal = 12.dp)
        .fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {

      BlankTextField(
        modifier = Modifier.weight(1f),
        value = state.currentMessage.message,
        onValueChanged = {
          viewModel.updateCurrentMessage(viewModel.uiState.value.currentMessage.copy(message = it))
        },
        onClearClicked = { },
        onFieldUnFocused = {},
        hint = "Your Message",
        hintColor = black,
        isWrapContentWidth = true
      )

      HorizontalSpacer(space = 6)

      Image(
        modifier = Modifier
          .size(35.dp)
          .clickable(indication = null, interactionSource = MutableInteractionSource(), onClick = {
            viewModel.addCurrentMessageToChat()
          }),
        painter = painterResource(id = drawable.sms_send_ic),
        contentDescription = stringResource(
          string.cd_sms_send_icon
        ),
        contentScale = ContentScale.Crop
      )

    }

    VerticalSpacer(space = 15)

    if (isKeyboardVisible) {
      Spacer(modifier = Modifier.padding(bottom = 10.dp))
    }
  }
}

@SuppressLint("UnrememberedMutableInteractionSource")
@Composable
fun ChatScreenContactInfo(
  modifier: Modifier = Modifier,
  contact: Contact,
  onContactsSettingsClicked: () -> Unit,
  onBackArrowClicked: () -> Unit,
) {

  VerticalSpacer(space = 20)

  Row(
    modifier = Modifier
      .padding(horizontal = 20.dp)
      .background(color = lightGrey1, shape = RoundedCornerShape(20.dp))
      .fillMaxWidth()
      .padding(6.dp),
    verticalAlignment = Alignment.CenterVertically
  )
  {

    Icon(
      modifier = Modifier
        .size(24.dp)
        .clickable(
          indication = null,
          interactionSource = MutableInteractionSource(),
          onClick = { onBackArrowClicked.invoke() }),
      imageVector = Icons.Outlined.KeyboardArrowLeft,
      contentDescription = stringResource(string.cd_chat_back_icon),
      tint = black
    )

    HorizontalSpacer(space = 20)

    AsyncImage(
      modifier = modifier
        .size(55.dp)
        .clip(RoundedCornerShape(10.dp)),
      model = contact.photo ?: DEFAULT_PROFILE_MAN_IMAGE,
      contentDescription = stringResource(string.cd_chat_profile_photo),
      contentScale = Companion.Crop
    )

    HorizontalSpacer(space = 18)

    Column {

      Text(
        text = contact.name,
        fontSize = 20.sp,
        color = black,
        textAlign = TextAlign.Center
      )

      Text(
        text = if (contact.isOnline) "ONLINE" else "OFFLINE",
        fontSize = 15.sp,
        color = black,
        textAlign = TextAlign.Center
      )

    }

    Spacer(modifier = Modifier.weight(1f))

    BlankMoreIcon(
      modifier = Modifier.padding(end = 7.dp),
      onClick = { onContactsSettingsClicked.invoke() },
      contentDescRes = string.cd_chat_settings_icon
    )

  }
}

@SuppressLint("UnrememberedMutableInteractionSource")
@Composable
fun ChatItem(
  modifier: Modifier = Modifier,
  details: Message,
) {

  val context = LocalContext.current

  var isLongPressed by remember {
    mutableStateOf(false)
  }

  if (FirebaseUtils.currentUser == null) {
    context.showToast("Got Current User null from FB")
    return
  }

  val isCurrentUserSentMessage = FirebaseUtils.currentUser!!.uid == details.senderId

  Row {

    if (isCurrentUserSentMessage) {
      Spacer(modifier = Modifier.weight(1f))
    }

    Column(
      modifier = Modifier.clickable(
        indication = null,
        interactionSource = MutableInteractionSource(),
        onClick = { isLongPressed = !isLongPressed }),
      horizontalAlignment = Alignment.End
    ) {

      Box(
        modifier = modifier
          .background(
            if (isCurrentUserSentMessage) darkGrey else white1,
            shape = RoundedCornerShape(10.dp)
          )
          .padding(8.dp)
      ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

          Text(
            text = details.message,
            color = black
          )

          if (details.isMessageModeOn) {
            Box(modifier = Modifier.fillMaxHeight()) {
              Icon(
                modifier = Modifier
                  .padding(start = 6.dp, top = 15.dp)
                  .align(Alignment.BottomCenter)
                  .size(10.dp),
                imageVector = Icons.Filled.Email,
                contentDescription = stringResource(string.cd_offline_message_icon),
                tint = black
              )
            }
          }

        }

      }

      VerticalSpacer(space = 4)

      AnimatedVisibility(visible = isLongPressed) {
        Text(
          //Todo convert TimeStamp to Proper Date details from dateTimeConverter
          text = if (isCurrentUserSentMessage) details.sentTime.toString() else details.receivedTime.toString(),
          fontWeight = FontWeight.Bold,
          fontSize = 7.sp,
          color = white,
          textAlign = TextAlign.End,
          lineHeight = 10.sp
        )
      }

    }

    VerticalSpacer(space = 13)

    if (isCurrentUserSentMessage.not()) {
      Spacer(modifier = Modifier.weight(1f))
    }
  }
}

@Composable
fun KeyboardVisibilityObserver(
  onKeyboardVisibilityChanged: (Boolean) -> Unit,
) {
  val rootView = LocalView.current
  val context = LocalContext.current
  val density = LocalDensity.current.density

  DisposableEffect(context) {
    val listener = ViewTreeObserver.OnGlobalLayoutListener {
      val rect = android.graphics.Rect()
      rootView.getWindowVisibleDisplayFrame(rect)
      val screenHeight = rootView.height
      val keypadHeight = screenHeight - rect.bottom

      // If the keypad height is greater than a threshold (e.g., 100dp), keyboard is open
      val isKeyboardVisible = keypadHeight > 100 * density
      onKeyboardVisibilityChanged(isKeyboardVisible)
    }

    rootView.viewTreeObserver.addOnGlobalLayoutListener(listener)

    onDispose {
      rootView.viewTreeObserver.removeOnGlobalLayoutListener(listener)
    }
  }
}