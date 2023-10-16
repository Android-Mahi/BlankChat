package com.invorel.blankchatpro.compose.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.ViewTreeObserver
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale.Companion
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.invorel.blankchatpro.R.drawable
import com.invorel.blankchatpro.R.string
import com.invorel.blankchatpro.compose.common.BackPressHandler
import com.invorel.blankchatpro.compose.common.BlankMoreIcon
import com.invorel.blankchatpro.compose.common.BlankTextField
import com.invorel.blankchatpro.compose.common.HorizontalSpacer
import com.invorel.blankchatpro.compose.common.VerticalSpacer
import com.invorel.blankchatpro.constants.DEFAULT_PROFILE_MAN_IMAGE
import com.invorel.blankchatpro.extensions.showToast
import com.invorel.blankchatpro.extensions.trimNameToMaxLength
import com.invorel.blankchatpro.ui.theme.black
import com.invorel.blankchatpro.ui.theme.darkGrey
import com.invorel.blankchatpro.ui.theme.lightGrey1
import com.invorel.blankchatpro.ui.theme.white
import com.invorel.blankchatpro.ui.theme.white1
import com.invorel.blankchatpro.utils.FirebaseUtils
import com.invorel.blankchatpro.utils.NetWorkUtils
import com.invorel.blankchatpro.viewModels.ChatReceiverDetails
import com.invorel.blankchatpro.viewModels.ChatsViewModel
import com.invorel.blankchatpro.viewModels.MessageUIModel

@SuppressLint("UnrememberedMutableInteractionSource", "CoroutineCreationDuringComposition")
@Composable
fun ChatScreen(
  modifier: Modifier = Modifier,
  chatRoomId: String = "",
  isCameFromHomeScreen: Boolean,
  viewModel: ChatsViewModel,
  localContactPhoto: Bitmap? = null,
  receiverDetails: ChatReceiverDetails,
  onBackClick: () -> Unit,
) {

  val context = LocalContext.current
  val state = viewModel.uiState.collectAsState().value
  val chatListState = rememberLazyListState()
  val keyboardState by keyboardAsState()

  LaunchedEffect(Unit) {
    viewModel.updateReceiverDetails(receiverDetails, localContactPhoto)
    if (chatRoomId.isNotEmpty()) {
      //Comes from Home Screen
      viewModel.updateChatRoomId(chatRoomId)
      viewModel.getLocalMessagesForTheChatRoom()
      viewModel.listenMessageUpdatesInCurrentChatRoomUpdateIfNeeded()
    } else {
      //Comes from Contacts Screen
      viewModel.checkIfAlreadyChatRoomExists()
      if (localContactPhoto != null) {
        viewModel.updateLocalReceiverImage()
      }
    }
  }

  //Navigates to the last message in chat when we enter this screen for the first time
  LaunchedEffect(state.messagesList) {
    if (state.messagesList.isNotEmpty()) {
      chatListState.scrollToItem(state.messagesList.lastIndex)
    }
  }

  LaunchedEffect(keyboardState) {
    if (keyboardState == Keyboard.Opened) {
      if (state.messagesList.isNotEmpty()) {
        chatListState.scrollToItem(state.messagesList.lastIndex)
      }
    }
  }

  LaunchedEffect(state.errorMessage) {
    if (state.errorMessage.isNotEmpty()) {
      context.showToast(state.errorMessage)
      viewModel.clearErrorMessage()
    }
  }

  LaunchedEffect(Unit) {
    viewModel.updateSenderDetails()
  }

  BackPressHandler {
    viewModel.cancelListeningMessageFromFirebase()
    onBackClick.invoke()
  }

  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {

    ChatScreenContactInfo(
      localContactPhoto = localContactPhoto,
      isFromHomeScreen = isCameFromHomeScreen,
      receiverDetails = receiverDetails,
      onContactsSettingsClicked = {
        context.showToast("Settings Clicked")
      },
      onBackArrowClicked = {
        viewModel.cancelListeningMessageFromFirebase()
        onBackClick.invoke()
      }
    )

    VerticalSpacer(space = 20)

    LazyColumn(
      state = chatListState,
      modifier = Modifier
        .padding(horizontal = 25.dp)
        .weight(1f)
    ) {
      itemsIndexed(state.messagesList) { _, chat ->
        MessageItem(details = chat)
      }
    }

    Row(
      modifier = Modifier
        .padding(horizontal = 12.dp)
        .fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {

      val currentTypedMessage = viewModel.currentTypedMessage.collectAsState().value

      BlankTextField(
        modifier = Modifier.weight(1f),
        value = currentTypedMessage,
        onValueChanged = {
          viewModel.updateCurrentTypedMessage(it)
          viewModel.updateCurrentMessage(viewModel.uiState.value.currentMessage.copy(message = it))
        },
        onClearClicked = { },
        onFieldUnFocused = {},
        hint = "Your Message",
        hintColor = black,
        isWrapContentWidth = true
      )

      HorizontalSpacer(space = 6)

      val disableSendButton =
        currentTypedMessage.isEmpty() || state.isMessageUpdateInProgress

      val owner = LocalLifecycleOwner.current

      Icon(
        modifier = Modifier
          .size(35.dp)
          .clickable
            (
            indication = null,
            interactionSource = MutableInteractionSource(),
            onClick = {
              if (disableSendButton.not()) {
                if (viewModel.currentChatRoomId.value.isEmpty()) {
                  viewModel.saveLocalChatRoomDetails(
                    NetWorkUtils.isNetworkConnected(context = context),
                    context = context,
                    owner = owner
                  )
                } else {
                  viewModel.saveMessageDetails(
                    context = context,
                    roomId = viewModel.currentChatRoomId.value,
                    owner = owner
                  )
                }
              }
            }),
        painter = painterResource(id = drawable.sms_send_ic),
        contentDescription = stringResource(
          string.cd_sms_send_icon
        ),
        tint = if (disableSendButton) darkGrey else white
      )

    }

    VerticalSpacer(space = 15)

    if (keyboardState == Keyboard.Opened) {
      Spacer(modifier = Modifier.padding(bottom = 10.dp))
    }
  }
}

@SuppressLint("UnrememberedMutableInteractionSource")
@Composable
fun ChatScreenContactInfo(
  modifier: Modifier = Modifier,
  localContactPhoto: Bitmap?,
  isFromHomeScreen: Boolean,
  receiverDetails: ChatReceiverDetails,
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
      model = if (isFromHomeScreen) receiverDetails.photo.ifEmpty { DEFAULT_PROFILE_MAN_IMAGE } else localContactPhoto
        ?: DEFAULT_PROFILE_MAN_IMAGE,
      contentDescription = stringResource(string.cd_chat_profile_photo),
      contentScale = Companion.Crop
    )

    HorizontalSpacer(space = 18)

    Column {

      Text(
        text = receiverDetails.name.trimNameToMaxLength(),
        fontSize = 20.sp,
        color = black,
        textAlign = TextAlign.Center
      )

      Text(
        //Todo listen db and update actual status here
        text = if (receiverDetails.isReceiverOnline) "ONLINE" else "OFFLINE",
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
fun MessageItem(
  modifier: Modifier = Modifier,
  details: MessageUIModel,
) {

  val context = LocalContext.current

  var isLongPressed by remember {
    mutableStateOf(false)
  }

  val maxDeviceWidth = LocalConfiguration.current.screenWidthDp

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
          .widthIn(max = (maxDeviceWidth * 0.6f).dp)
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

          if (details.isSentByMessageMode) {
            Box(modifier = Modifier.fillMaxHeight()) {
              /*Icon(
                modifier = Modifier
                  .padding(start = 6.dp, top = 15.dp)
                  .align(Alignment.BottomCenter)
                  .size(10.dp),
                imageVector = Icons.Filled.Email,
                contentDescription = stringResource(string.cd_offline_message_icon),
                tint = black
              )*/
              //todo remove below text after testing
              Text(
                text = details.status.toString(),
                color = black
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
fun keyboardAsState(): State<Keyboard> {
  val keyboardState = remember { mutableStateOf(Keyboard.Closed) }
  val view = LocalView.current
  DisposableEffect(view) {
    val onGlobalListener = ViewTreeObserver.OnGlobalLayoutListener {
      val rect = Rect()
      view.getWindowVisibleDisplayFrame(rect)
      val screenHeight = view.rootView.height
      val keypadHeight = screenHeight - rect.bottom
      keyboardState.value = if (keypadHeight > screenHeight * 0.15) {
        Keyboard.Opened
      } else {
        Keyboard.Closed
      }
    }
    view.viewTreeObserver.addOnGlobalLayoutListener(onGlobalListener)

    onDispose {
      view.viewTreeObserver.removeOnGlobalLayoutListener(onGlobalListener)
    }
  }

  return keyboardState
}

enum class Keyboard {
  Opened,
  Closed
}

