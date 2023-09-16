package com.invorel.blankchatpro.compose.common

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.invorel.blankchatpro.R.drawable
import com.invorel.blankchatpro.R.string
import com.invorel.blankchatpro.ui.theme.black
import com.invorel.blankchatpro.ui.theme.lightGrey

@SuppressLint("UnrememberedMutableInteractionSource")
@Composable
fun BlankProfileCard(
  modifier: Modifier = Modifier,
  profileImageUr: String,
  userName: String,
  userAbout: String,
  userPhoneNo: String = "",
  isVerified: Boolean,
  isTrailingNavigationIconVisible: Boolean = false,
  onMoreClicked: () -> Unit,
) {

  val aboutTextWidth = remember {
    mutableStateOf(0.dp)
  }

  val nameTextWidth = remember {
    mutableStateOf(0.dp)
  }

  val localDensity = LocalDensity.current

  Row(
    modifier = modifier.background(color = lightGrey, shape = RoundedCornerShape(20.dp)),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceEvenly
  ) {

    HorizontalSpacer(space = 20)

    AsyncImage(
      modifier = Modifier
        .size(60.dp)
        .clip(RoundedCornerShape(10.dp)),
      model = profileImageUr,
      contentDescription = stringResource(id = string.cd_profile_photo),
      contentScale = ContentScale.Crop
    )

    HorizontalSpacer(space = 15)

    Column(
      modifier = Modifier
        .padding(vertical = 20.dp)
        .weight(1f)
    ) {

      Row {
        Text(
          modifier = Modifier.onGloballyPositioned { layoutCoOrdinates ->
            // Here Getting the About Text Width manually
            val textWidthPx = layoutCoOrdinates.size.width
            val density = localDensity.density
            nameTextWidth.value = (textWidthPx / density).toInt().dp
          },
          text = userName, color = black, fontWeight = FontWeight.Bold
        )

        HorizontalSpacer(space = 5)

        if (isVerified) {
          Image(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = drawable.verified_ic),
            contentDescription = stringResource(string.cd_verified_icon),
          )
        }
      }

      HorizontalDivider(
        modifier = Modifier
          .padding(top = 3.dp, bottom = 3.dp)
          .width(maxOf(nameTextWidth.value, aboutTextWidth.value)),
        color = Color.Black
      )

      Text(
        modifier = Modifier.onGloballyPositioned { layoutCoOrdinates ->
          // Here Getting the About Text Width manually
          val textWidthPx = layoutCoOrdinates.size.width
          val density = localDensity.density
          aboutTextWidth.value = (textWidthPx / density).toInt().dp
        },
        text = userAbout, color = black, maxLines = 1
      )

      if (userPhoneNo.isNotEmpty()) {
        Text(text = userPhoneNo, color = black)
      }

    }


    if (isTrailingNavigationIconVisible) {
      BlankMoreIcon(onClick = { onMoreClicked.invoke() }, contentDescRes = string.cd_profile_settings_icon )

      HorizontalSpacer(space = 15)
    }

  }
}