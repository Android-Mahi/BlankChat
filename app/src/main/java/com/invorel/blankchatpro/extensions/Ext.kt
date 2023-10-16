package com.invorel.blankchatpro.extensions

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.core.app.ActivityCompat
import com.invorel.blankchatpro.constants.DEFAULT_CHAT_ROOM_SEPARATOR
import com.invorel.blankchatpro.constants.RECEIVER_NAME_MAX_LENGTH_IN_CHAT
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun Modifier.clickableWithoutRipple(onClick: () -> Unit): Modifier = composed {
  clickable(
    indication = null,
    interactionSource = remember { MutableInteractionSource() }) { onClick() }
}

fun Context.showToast(message: String) {
  Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Context.isPermissionGranted(permission: String) =
  this.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED

fun Context.isPermissionDeniedPreviously(permission: String) =
  this.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_DENIED

fun Activity.isRationaleDialogNeeded(permission: String) =
  ActivityCompat.shouldShowRequestPermissionRationale(this, permission)

@OptIn(ExperimentalContracts::class) fun CharSequence?.isNotNullAndNotEmpty(): Boolean {
  contract {
    returns(true) implies (this@isNotNullAndNotEmpty != null)
  }
  return !this.isNullOrEmpty()
}

fun String.buildProperPhoneNumber(): String {
  val cleanedNumber = this.replace("[^0-9+]".toRegex(), "")
    .replace("\\s".toRegex(), "") // Remove non-digit, non-plus, and white space characters
  return if (!cleanedNumber.startsWith("+91")) {
    // Add +91 if it doesn't exist
    "+91$cleanedNumber"
  } else {
    val firstCodeRemovedNumber = cleanedNumber.substring(3)
    //check if countryCode exists two times
    if (firstCodeRemovedNumber.startsWith("+91")) {
      firstCodeRemovedNumber
    } else {
      cleanedNumber
    }
  }
}

fun String.trimNameToMaxLength() = this.take(RECEIVER_NAME_MAX_LENGTH_IN_CHAT).plus("..")

fun String.isNumberTypeChatRoom(): Boolean {
  val participants = this.split(DEFAULT_CHAT_ROOM_SEPARATOR)
  //country code +91 included in size
  return participants.first().length == 13 && participants.last().length == 13
}
