package com.invorel.blankchatpro.compose.common

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource

@SuppressLint("UnrememberedMutableInteractionSource")
@Composable
fun BlankMoreIcon(
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
  @StringRes contentDescRes: Int
){
  Icon(
    modifier = modifier.clickable(
      indication = null,
      interactionSource = MutableInteractionSource(),
      onClick = {
        onClick.invoke()
      }),
    imageVector = Outlined.MoreVert,
    contentDescription = stringResource(contentDescRes),
    tint = Color.Black
  )
}