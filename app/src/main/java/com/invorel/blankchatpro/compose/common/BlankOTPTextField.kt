package com.invorel.blankchatpro.compose.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.invorel.blankchatpro.constants.DEFAULT_MAX_LENGTH_OTP

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BlankOTPTextField(
  modifier: Modifier = Modifier,
  otpText: String,
  otpMaxCount: Int = DEFAULT_MAX_LENGTH_OTP,
  onOTPTextChange: (String) -> Unit
) {

  val focusRequester = FocusRequester()
  val keyboardController = LocalSoftwareKeyboardController.current

  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
  }

  BasicTextField(
    modifier = modifier.focusRequester(focusRequester),
    value = otpText,
    onValueChange = {
      if (it.length <= otpMaxCount) {
        onOTPTextChange.invoke(it)
      }
      if (it.length == otpMaxCount) {
        keyboardController?.hide()
      }
    },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
    decorationBox = {
      Row(horizontalArrangement = Arrangement.Center) {
        repeat(otpMaxCount) { index ->
          CharView(index = index, text = otpText)
          Spacer(modifier = Modifier.width(8.dp))
        }
      }
    }
  )
}

@Composable
fun CharView(
  index: Int,
  text: String
) {
  val isFocused = text.length == index

  val char = when {
    index == text.length -> "|"
    index > text.length -> ""
    else -> text[index].toString()
  }

  Text(
    text = char,
    modifier = Modifier
      .width(40.dp)
      .border(
        1.dp, when {
          isFocused -> Color.Blue
          else -> Color.Black
        }, RoundedCornerShape(8.dp)
      )
      .padding(2.dp),
    textAlign = TextAlign.Center,
    style = MaterialTheme.typography.headlineMedium,
    color = if (isFocused) Color.Blue else Color.Black
  )
}