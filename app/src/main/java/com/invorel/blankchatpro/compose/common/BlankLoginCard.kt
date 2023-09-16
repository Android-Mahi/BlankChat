package com.invorel.blankchatpro.compose.common

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invorel.blankchatpro.R.drawable
import com.invorel.blankchatpro.R.string
import com.invorel.blankchatpro.constants.DEFAULT_COUNTRY_CODE
import com.invorel.blankchatpro.ui.theme.black
import com.invorel.blankchatpro.ui.theme.grey

@SuppressLint("UnrememberedMutableInteractionSource")
@Composable fun BlankLoginCard(
  modifier: Modifier = Modifier,
  isOTPMode: Boolean = false,
  phoneNumber: String,
  otp: String,
  isTimerInProgress: Boolean,
  isFbRequestInProgress: Boolean,
  remainingTime: String,
  isRememberMeChecked: Boolean,
  onPhoneNoChanged: (String) -> Unit,
  onOTPChanged: (String) -> Unit,
  onRememberCheckChanged: () -> Unit,
  onProcessClicked: () -> Unit,
  onSubmitClicked: () -> Unit,
  onWrongNumberClicked: () -> Unit,
  onResendOtpClicked: () -> Unit,
) {

  Surface(
    modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp)
  ) {

    Column(
      modifier = Modifier.padding(25.dp),
    ) {

      Text(
        modifier = Modifier.fillMaxWidth(),
        text = if (isOTPMode) stringResource(
          id = string.enter_verification_code,
          formatArgs = arrayOf("$DEFAULT_COUNTRY_CODE $phoneNumber")
        ) else stringResource(id = string.enter_phone_number),
        color = black,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        fontSize = 20.sp
      )

      VerticalSpacer(space = 15)

      Text(
        modifier = Modifier
          .fillMaxWidth()
          .clickable(
            indication = null,
            interactionSource = MutableInteractionSource(),
            onClick = {
              if (isOTPMode) onWrongNumberClicked.invoke()
            }),
        text = stringResource(id = if (isOTPMode) string.wrong_number else string.receive_verification_code),
        color = black,
        fontWeight = FontWeight.Normal,
        textAlign = if (isOTPMode) TextAlign.End else TextAlign.Center,
        fontSize = 13.sp
      )

      if (isOTPMode) {
        VerticalSpacer(space = 10)
      }

      if (!isOTPMode) {
        VerticalSpacer(space = 15)
      }

      Row(
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
      ) {

        if (phoneNumber.isNotEmpty() && !isOTPMode) {
          Text(text = DEFAULT_COUNTRY_CODE)
        } else {
          Image(
            modifier = Modifier.size(25.dp),
            painter = painterResource(id = if (isOTPMode) drawable.otp_ic else drawable.phone_ic),
            contentDescription = stringResource(
              id = if (isOTPMode) string.cd_otp_ic else string.cd_phone_ic
            )
          )
        }



        HorizontalSpacer(space = 15)

        if (isOTPMode) {
          BlankOTPTextField(otpText = otp) { otp ->
            onOTPChanged.invoke(otp)
          }
        } else {
          BlankTextField(
            value = phoneNumber,
            hint = stringResource(id = string.hint_enter_your_number),
            keyboardType = KeyboardType.Number,
            onValueChanged = onPhoneNoChanged,
            onClearClicked = {},
            onFieldUnFocused = {})
        }

      }

      if (!isOTPMode) {
        Row(
          modifier = Modifier.padding(start = 30.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Start
        ) {

          Checkbox(checked = isRememberMeChecked, onCheckedChange = {
            onRememberCheckChanged.invoke()
          })

          Text(
            text = stringResource(id = string.remember_me),
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = black
          )

        }
      }

      if (isOTPMode) {
        VerticalSpacer(space = 15)
        Text(
          modifier = Modifier
            .fillMaxWidth()
            .clickable(indication = null,
              interactionSource = MutableInteractionSource(),
              onClick = {
                if (!isTimerInProgress) onResendOtpClicked.invoke()
              }),
          text = if (isTimerInProgress) remainingTime else stringResource(id = string.resend_otp),
          color = if (isTimerInProgress) grey else black,
          fontWeight = FontWeight.Normal,
          textAlign = TextAlign.Center,
          fontSize = 13.sp
        )
        VerticalSpacer(space = 15)
      }

      Row {

        Spacer(modifier = Modifier.weight(1f))

        AnimatedVisibility(visible = isFbRequestInProgress) {
          CircularProgressIndicator()
        }

        AnimatedVisibility(visible = isFbRequestInProgress.not()) {
          BlankButton(
            modifier = Modifier,
            title = stringResource(id = if (isOTPMode) string.btn_submit else string.btn_process)
          ) {
            if (isOTPMode) {
              onSubmitClicked.invoke()
            } else {
              onProcessClicked.invoke()
            }
          }
        }

      }

    }

  }
}