package com.invorel.blankchatpro.compose.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invorel.blankchatpro.R.drawable
import com.invorel.blankchatpro.R.string
import com.invorel.blankchatpro.compose.common.BlankButton
import com.invorel.blankchatpro.compose.common.VerticalSpacer
import com.invorel.blankchatpro.ui.theme.black
import com.invorel.blankchatpro.ui.theme.white

@Composable
fun TermsConditionsScreen(
  modifier: Modifier = Modifier,
  onContinueClicked: () -> Unit,
) {

  val context = LocalContext.current

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(black),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {

    Image(
      modifier = Modifier
        .clip(RoundedCornerShape(30.dp))
        .size(350.dp),
      painter = painterResource(id = drawable.terms_ic),
      contentDescription = LocalContext.current.getString(string.cd_terms_pic),
      contentScale = ContentScale.Fit
    )

    VerticalSpacer(space = 25)

    Text(
      text = context.getString(string.privacy_message),
      color = white,
      fontFamily = FontFamily.SansSerif,
      textAlign = TextAlign.Center,
      fontSize = 25.sp
    )

    VerticalSpacer(space = 25)

    Text(
      text = context.getString(string.label_terms_privacy_policy),
      color = white,
      fontFamily = FontFamily.SansSerif,
      textAlign = TextAlign.Center,
      fontSize = 15.sp,
    )

    VerticalSpacer(space = 25)

    BlankButton(title = "Continue") {
      onContinueClicked.invoke()
    }

    VerticalSpacer(space = 25)
  }
}


