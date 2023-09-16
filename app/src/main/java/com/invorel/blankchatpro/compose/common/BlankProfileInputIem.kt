package com.invorel.blankchatpro.compose.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invorel.blankchatpro.R.drawable
import com.invorel.blankchatpro.R.string

@Composable
fun BlankProfileInputIem(
  modifier: Modifier = Modifier,
  @DrawableRes iconRes: Int,
  @StringRes iconContentDesc: Int,
  title: String,
  value: String,
  isEditable: Boolean = true,
  clearFocus: Boolean = false,
  onValueUpdated: (String) -> Unit,
  onValueCleared: () -> Unit,
) {

  val isInEditMode = remember {
    mutableStateOf(false)
  }

  if (clearFocus) {
    if (isInEditMode.value) {
      isInEditMode.value = false
    }
  }

  Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {

    Icon(
      modifier = Modifier.size(17.dp),
      painter = painterResource(id = iconRes),
      contentDescription = stringResource(id = iconContentDesc)
    )

    HorizontalSpacer(space = 11)

    var firstTime = true

    if (isInEditMode.value) {
      BlankTextField(
        value = value, hint = title, clearFocus = clearFocus,
        onValueChanged = {
          onValueUpdated.invoke(it)
        },
        onClearClicked = {
          onValueCleared.invoke()
        },
        onFieldUnFocused = {
          firstTime = false
          if (!firstTime) {
            if (isInEditMode.value) {
              isInEditMode.value = false
            }
          }
        },
        showClearIcon = true,
      )
    } else {
      Column(verticalArrangement = Arrangement.Center) {

        Text(text = title)

        VerticalSpacer(space = 2)

        Text(
          text = value,
          fontWeight = FontWeight.Bold,
          fontSize = 20.sp
        )
      }
    }

    Spacer(modifier = Modifier.weight(1f))

    if (isEditable) {
      Icon(
        modifier = Modifier
          .size(17.dp)
          .clickable { isInEditMode.value = isInEditMode.value.not() },
        painter = painterResource(id = drawable.edit_ic),
        contentDescription = stringResource(id = string.cd_profile_edit_ic)
      )
    }

  }

  VerticalSpacer(space = 25)
}