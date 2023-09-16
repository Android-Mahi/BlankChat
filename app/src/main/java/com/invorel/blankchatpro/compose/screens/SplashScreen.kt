package com.invorel.blankchatpro.compose.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invorel.blankchatpro.R.drawable
import com.invorel.blankchatpro.R.string
import com.invorel.blankchatpro.compose.common.VerticalSpacer
import com.invorel.blankchatpro.constants.SPLASH_SCREEN_TIME_OUT_DELAY
import com.invorel.blankchatpro.ui.theme.black
import com.invorel.blankchatpro.ui.theme.white
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
  modifier: Modifier = Modifier,
  onTimeOut: () -> Unit,
) {

  val context = LocalContext.current

  LaunchedEffect(Unit) {
    delay(SPLASH_SCREEN_TIME_OUT_DELAY)
    onTimeOut.invoke()
  }

  Box(
    modifier = modifier
      .fillMaxSize()
  ) {
    Image(
      modifier = Modifier
        .size(100.dp)
        .align(Alignment.Center),
      painter = painterResource(id = drawable.logo),
      contentDescription = context.getString(string.cd_logo),
      contentScale = ContentScale.Fit
    )

    Column(
      modifier = Modifier.align(Alignment.BottomCenter),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = stringResource(id = string.app_name),
        fontWeight = FontWeight.Bold,
        color = white,
        fontFamily = FontFamily.SansSerif,
        fontSize = 40.sp
      )

      Text(
        text = stringResource(string.splash_desc),
        fontWeight = FontWeight.Normal,
        color = white,
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp
      )
      VerticalSpacer(space = 25)
    }

  }
}

