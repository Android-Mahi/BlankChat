package com.invorel.blankchatpro.compose.common

import android.app.Activity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.invorel.blankchatpro.ui.theme.black
import com.invorel.blankchatpro.R.string

@Composable
fun VerticalSpacer(space: Int) {
  Spacer(modifier = Modifier.height(space.dp))
}

@Composable
fun HorizontalSpacer(space: Int) {
  Spacer(modifier = Modifier.width(space.dp))
}

@Composable
fun BackPressHandler(
  onBackPress: () -> Unit,
) {
  val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

  val callback = rememberUpdatedState(onBackPress)

  DisposableEffect(callback) {
    val onBackPressedCallback = object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        callback.value()
      }
    }

    dispatcher?.addCallback(onBackPressedCallback)

    onDispose {
      onBackPressedCallback.remove()
    }
  }
}

@Composable
fun HideStatusBar(value: Boolean = true) {
  val currentView = LocalView.current
  val currentWindow = (currentView.context as Activity).window
  currentWindow.statusBarColor = black.toArgb()
  WindowCompat.getInsetsController(currentWindow, currentView).isAppearanceLightStatusBars = value
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExitBottomSheet(
  modifier: Modifier = Modifier,
  onDismissClick: () -> Unit,
  onPositiveClick: () -> Unit,
  onNegativeClick: () -> Unit,
) {
  ModalBottomSheet(
    modifier = modifier,
    onDismissRequest = { onDismissClick.invoke() },
    dragHandle = {},
  ) {

    Column(modifier = Modifier.padding(15.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = stringResource(id = string.are_you_sure_to_exit),
        textAlign = TextAlign.Center,
        fontSize = 25.sp,
        color = Color.Black,
        fontWeight = FontWeight.Bold
      )

      VerticalSpacer(space = 15)

      HorizontalDivider()

      VerticalSpacer(space = 15)

      Row(modifier = Modifier.padding(horizontal = 55.dp)) {
        Text(
          modifier = Modifier
            .padding(top = 5.dp)
            .clickable { onPositiveClick.invoke() },
          text = stringResource(id = string.yes),
          color = Color.Black,
        )

        Spacer(modifier = Modifier.weight(1f))

        BlankButton(title = stringResource(id = string.no)) {
          onNegativeClick.invoke()
        }

      }

      VerticalSpacer(space = 15)

      HorizontalDivider()

      VerticalSpacer(space = 15)

    }

  }
}
