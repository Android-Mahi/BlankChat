package com.invorel.blankchatpro.compose.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.imageLoader
import com.invorel.blankchatpro.R.drawable
import com.invorel.blankchatpro.R.string
import com.invorel.blankchatpro.compose.common.BlankButton
import com.invorel.blankchatpro.compose.common.BlankProfileInputIem
import com.invorel.blankchatpro.compose.common.VerticalSpacer
import com.invorel.blankchatpro.constants.DEFAULT_PROFILE_PICK_IMAGE_URL
import com.invorel.blankchatpro.extensions.showToast
import com.invorel.blankchatpro.others.ErrorMessage.StringErrorMessage
import com.invorel.blankchatpro.others.ErrorMessage.StringResErrorMessage
import com.invorel.blankchatpro.ui.theme.white
import com.invorel.blankchatpro.utils.FirebaseUtils
import com.invorel.blankchatpro.viewModels.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileUpdateScreen(
  modifier: Modifier = Modifier,
  viewModel: ProfileViewModel,
  onSheetDismissed: () -> Unit,
) {

  val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val context = LocalContext.current
  val state = viewModel.uiState.collectAsState().value

  val profilePicLauncher = rememberLauncherForActivityResult(contract = PickVisualMedia()) { uri ->
    if (uri != null) {
      viewModel.updateSelectedPhotoUri(uri)
    } else {
      context.showToast(context.getString(string.invalid_photo_pick))
    }
  }

  if (state.userNumber.isEmpty()) {
    viewModel.refreshDataFromFB()
  }

  BackHandler {
    onSheetDismissed.invoke()
  }

  Text(
    modifier = Modifier
      .padding(top = 35.dp)
      .clickable { FirebaseUtils.logOutUser() },
    text = stringResource(id = string.profile),
    color = white,
    fontWeight = FontWeight.Bold,
    textAlign = TextAlign.Center,
    fontSize = 30.sp
  )

  ModalBottomSheet(
    sheetState = bottomSheetState,
    onDismissRequest = onSheetDismissed
  ) {

    Column(
      modifier = modifier.padding(25.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {

      VerticalSpacer(space = 25)

      Box(modifier = Modifier.clickable {
        profilePicLauncher.launch(PickVisualMediaRequest(ImageOnly))
      }) {
        AsyncImage(
          modifier = Modifier
            .size(150.dp)
            .clip(RoundedCornerShape(20.dp)),
          model = if (state.selectedPhotoUri != Uri.EMPTY) {
            //user picked image now from gallery
            state.selectedPhotoUri
          } else if (state.fireBaseProfileImgUrl.isNotEmpty()) {
            //Previously user picked & uploaded Image
            state.fireBaseProfileImgUrl
          } else {
            // no image picked & no image in Firebase we can show default image
            DEFAULT_PROFILE_PICK_IMAGE_URL
          },
          contentDescription = stringResource(id = string.cd_profile_photo),
          imageLoader = context.imageLoader,
          contentScale = ContentScale.Crop
        )
      }

      VerticalSpacer(space = 40)

      BlankProfileInputIem(
        iconRes = drawable.user_ic,
        iconContentDesc = string.cd_profile_name_ic,
        title = stringResource(id = string.name),
        value = state.userName,
        clearFocus = state.fbRequestInProcess,
        onValueUpdated = viewModel::updateUserName,
        onValueCleared = {
          viewModel.clearUserName()
        }
      )

      BlankProfileInputIem(
        iconRes = drawable.about_ic,
        iconContentDesc = string.cd_profile_about_ic,
        title = stringResource(id = string.about),
        value = state.userAbout,
        clearFocus = state.fbRequestInProcess,
        onValueUpdated = viewModel::updateUserAbout,
        onValueCleared = {
          viewModel.clearUserAbout()
        }
      )

      BlankProfileInputIem(
        iconRes = drawable.phone_black_ic,
        iconContentDesc = string.cd_profile_number_ic,
        title = stringResource(id = string.number),
        value = state.userNumber,
        isEditable = false,
        onValueUpdated = {
          //This will never invoke due to it's not editable
        },
        onValueCleared = {
          //This will never invoke due to it's not editable
        }
      )

      Row {

        Spacer(modifier = Modifier.weight(1f))

        AnimatedVisibility(!state.fbRequestInProcess) {
          BlankButton(title = stringResource(id = string.update)) {
            viewModel.updateProfileDetailsInFirebase()
          }
        }

        AnimatedVisibility(state.fbRequestInProcess) {
          CircularProgressIndicator()
        }

      }

      VerticalSpacer(space = 30)

    }

  }

  LaunchedEffect(state.errorMessage) {
    if (state.errorMessage != null) {
      val message = when (state.errorMessage) {
        is StringErrorMessage -> state.errorMessage.message
        is StringResErrorMessage -> context.getString(state.errorMessage.stringRes)
      }
      context.showToast(message)
      viewModel.clearErrorMessage()
    }
  }
}