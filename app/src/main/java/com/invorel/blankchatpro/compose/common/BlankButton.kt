package com.invorel.blankchatpro.compose.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.invorel.blankchatpro.ui.theme.btn_end_color
import com.invorel.blankchatpro.ui.theme.btn_start_color
import com.invorel.blankchatpro.ui.theme.white

@Composable
fun BlankButton(
  modifier: Modifier = Modifier,
  title: String,
  onClick: () -> Unit,
) {

  Text(
    modifier = modifier
      .background(
        brush = Brush.horizontalGradient(
          colors = listOf(
            btn_start_color,
            btn_end_color
          )
        ),
        shape = RoundedCornerShape(16.dp)
      )
      .padding(horizontal = 15.dp, vertical = 5.dp)
      .clickable(
        indication = null,
        interactionSource = MutableInteractionSource(),
        onClick = onClick
      ),
    text = title,
    textAlign = TextAlign.Center,
    maxLines = 1,
    color = white
  )
}