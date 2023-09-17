package com.invorel.blankchatpro.compose.common

import android.annotation.SuppressLint
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.invorel.blankchatpro.R.drawable
import com.invorel.blankchatpro.R.string
import com.invorel.blankchatpro.compose.common.SwipeCardStatus.DEFAULT
import com.invorel.blankchatpro.constants.DEFAULT_PROFILE_MAN_IMAGE
import com.invorel.blankchatpro.ui.theme.black
import com.invorel.blankchatpro.ui.theme.btn_end_color
import com.invorel.blankchatpro.ui.theme.darkGrey
import com.invorel.blankchatpro.ui.theme.lightGrey
import kotlinx.coroutines.launch

@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BlankSwipeableCard(
  modifier: Modifier = Modifier,
  dataHolder: ReceivedChatDataHolder,
  onPrivateChatSwiped: () -> Unit,
  onArchiveChatSwiped: (() -> Unit),
) {
  val scope = rememberCoroutineScope()

  val swipeAbleState = remember {
    AnchoredDraggableState(
      initialValue = DEFAULT,
      animationSpec = tween(150),
      velocityThreshold = { 125f },
      positionalThreshold = { it * 0.6f }
    )
  }

  val maxWidthInPx = with(LocalDensity.current) {
    LocalConfiguration.current.screenWidthDp.dp.toPx()
  }

  val anchors = remember {
    DraggableAnchors {
      SwipeCardStatus.LEFT at maxWidthInPx * 0.25f
      SwipeCardStatus.RIGHT at -maxWidthInPx * 0.25f
      DEFAULT at 0f
    }
  }

  SideEffect {
    swipeAbleState.updateAnchors(anchors)
  }

  /* Tracks if left or right action card to be shown */
  val swipeLeftCardVisible = remember { mutableStateOf(false) }

  /* Disable swipe when card is animating back to default position */
  val swipeEnabled = remember { mutableStateOf(true) }

  val isLeftSwipeEnabled by remember {
    mutableStateOf(true)
  }

  val isRightSwipeEnabled by remember {
    mutableStateOf(true)
  }

  /* This surface is for action card which is below the main card */

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

    Surface(
      color = Color.Transparent,
      content = {
        if (swipeLeftCardVisible.value) {
          LeftCard(modifier = Modifier.fillMaxSize())
        } else {
          RightCard(modifier = Modifier.fillMaxSize())
        }
      },
      modifier = Modifier
        .fillMaxSize()
    )

    Surface(
      color = Color.Transparent,
      modifier = Modifier
        .fillMaxSize()
        .anchoredDraggable(
          state = swipeAbleState,
          orientation = Horizontal,
          enabled = swipeEnabled.value,
        )
        .offset {
          var offset = swipeAbleState.offset
          if (offset < 0 && isLeftSwipeEnabled.not()) offset = 0f
          if (offset > 0 && isRightSwipeEnabled.not()) offset = 0f
          IntOffset(offset.toInt(), 0)
        }

    ) {

      if (swipeAbleState.currentValue == SwipeCardStatus.LEFT && !swipeAbleState.isAnimationRunning) {
        onPrivateChatSwiped.invoke()
        scope.launch {
          swipeEnabled.value = false
          swipeAbleState.animateTo(DEFAULT)
          swipeEnabled.value = true
        }
      } else if (swipeAbleState.currentValue == SwipeCardStatus.RIGHT && !swipeAbleState.isAnimationRunning) {
        onArchiveChatSwiped.invoke()
        scope.launch {
          swipeEnabled.value = false
          swipeAbleState.animateTo(DEFAULT)
          swipeEnabled.value = true
        }
      }

      swipeLeftCardVisible.value = swipeAbleState.offset <= 0
      MainCard(dataHolder = dataHolder)
    }

  }
}

@Composable
fun MainCard(
  modifier: Modifier = Modifier,
  dataHolder: ReceivedChatDataHolder,
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(20.dp),
    shape = RoundedCornerShape(20.dp),
    color = lightGrey,
  ) {

    Row(
      modifier = Modifier
        .background(Color.White)
        .padding(horizontal = 10.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {

      HorizontalSpacer(space = 5)

      AsyncImage(
        modifier = Modifier.size(56.dp),
        model = dataHolder.receiverImage.ifEmpty { DEFAULT_PROFILE_MAN_IMAGE },
        contentScale = ContentScale.Crop,
        contentDescription = stringResource(string.cd_home_chat_receiver_profile_image)
      )

      HorizontalSpacer(space = 23)

      Column {

        Row(verticalAlignment = Alignment.CenterVertically) {

          StatusDot()

          HorizontalSpacer(space = 7)

          Text(
            text = dataHolder.receiverName,
            color = black,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
          )

        }

        VerticalSpacer(space = 3)

        Text(
          text = dataHolder.secondaryDesc,
          color = black,
          fontSize = 12.sp,
        )

      }

      Spacer(modifier = Modifier.weight(1f))

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
          modifier = Modifier.size(23.dp),
          painter = painterResource(id = drawable.ic_star),
          contentDescription = stringResource(string.cd_starred_chat_icon),
        )

        VerticalSpacer(space = 3)

        Text(text = dataHolder.lastMessageSentOrReceivedTime, fontSize = 12.sp, color = black)

      }

      HorizontalSpacer(space = 5)

    }
  }
}

@Composable
fun StatusDot(
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier.size(5.dp)
  ) {
    Canvas(
      modifier = Modifier.matchParentSize()
    ) {
      drawCircle(
        color = btn_end_color,
        center = Offset(size.width / 2, size.height / 2),
        radius = size.width / 2
      )
    }
  }
}

@Composable
fun LeftCard(modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 30.dp),
    shape = RoundedCornerShape(20.dp),
    color = darkGrey,
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(vertical = 15.dp)
    ) {
      Image(
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .padding(end = 20.dp)
          .size(35.dp),
        painter = painterResource(id = drawable.ic_archive_chat),
        contentDescription = "Done Icon",
      )
    }

  }
}

@Composable
fun RightCard(modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 30.dp),
    shape = RoundedCornerShape(20.dp),
    color = darkGrey,
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(vertical = 15.dp)
    ) {
      Image(
        modifier = Modifier
          .align(Alignment.CenterStart)
          .padding(start = 20.dp)
          .size(35.dp),
        painter = painterResource(id = drawable.ic_private_chat),
        contentDescription = "Delete Icon"
      )
    }
  }
}

data class ReceivedChatDataHolder(
  val receiverImage: String,
  val receiverName: String,
  val isReceiverOnline: Boolean,
  val isChatStarred: Boolean,
  val secondaryDesc: String,
  val lastMessageSentOrReceivedTime: String,
)

enum class SwipeCardStatus {
  DEFAULT,
  LEFT,
  RIGHT
}