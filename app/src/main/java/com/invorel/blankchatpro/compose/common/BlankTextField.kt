package com.invorel.blankchatpro.compose.common

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.invorel.blankchatpro.R.string
import com.invorel.blankchatpro.ui.theme.black
import com.invorel.blankchatpro.ui.theme.grey

@SuppressLint("UnrememberedMutableInteractionSource")
@Composable
fun BlankTextField(
  modifier: Modifier = Modifier,
  value: String,
  keyboardType: KeyboardType = KeyboardType.Text,
  clearFocus: Boolean = false,
  hint: String = "",
  hintColor: Color = black,
  isWrapContentWidth: Boolean = false,
  showClearIcon: Boolean = false,
  onValueChanged: (String) -> Unit,
  onClearClicked: () -> Unit,
  onFieldUnFocused: () -> Unit,
) {

  val isHintVisible = remember {
    mutableStateOf(value.isEmpty() && hint.isNotEmpty())
  }

  val isTextFieldFocused = remember {
    mutableStateOf(false)
  }

  val isFieldFocusedProperlyOneTime = remember {
    mutableStateOf(false)
  }

  val focusManager = LocalFocusManager.current

  LaunchedEffect(clearFocus) {
    if (clearFocus) {
      focusManager.clearFocus()
    }
  }

  val textFieldModifier = if (!isWrapContentWidth) {
    modifier.fillMaxWidth()
  } else {
    modifier
  }

  Box (modifier = textFieldModifier) {
    BasicTextField(
      modifier = Modifier
        .background(shape = RoundedCornerShape(20.dp), color = grey)
        .onFocusChanged {
          isTextFieldFocused.value = it.isFocused
          if (!it.isFocused) {
            if (isFieldFocusedProperlyOneTime.value) {
              onFieldUnFocused.invoke()
            }
          } else {
            isFieldFocusedProperlyOneTime.value = true
          }
        },
      value = value,
      onValueChange = {
        isHintVisible.value = false
        onValueChanged.invoke(it)
      },
      decorationBox = { innerTextField ->
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(15.dp)
            .background(color = Color.Transparent), // Set a transparent background color
          content = { innerTextField() }
        )
      },
      keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
      keyboardActions = KeyboardActions(
        onDone = {
          focusManager.clearFocus()
        }
      )
    )

    if (isHintVisible.value) {
      Text(
        modifier = Modifier
          .padding(13.dp),
        text = hint,
        color = hintColor,
        textAlign = TextAlign.Center
      )
    }

    if (showClearIcon && value.isNotEmpty() && isTextFieldFocused.value) {
      Icon(
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .padding(horizontal = 5.dp)
          .clickable(
            indication = null,
            interactionSource = MutableInteractionSource(),
            onClick = { onClearClicked.invoke() }),
        imageVector = Icons.Outlined.Clear,
        contentDescription = stringResource(id = string.cd_clear_icon),
        tint = black
      )
    }

  }
}