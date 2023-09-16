package com.invorel.blankchatpro.extensions

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.core.app.ActivityCompat
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun Modifier.noRippleEffect() {
  this.indication(MutableInteractionSource(), null)
}

fun Modifier.clickableWithoutRipple(onClick: () -> Unit): Modifier = composed {  clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {    onClick()  }}

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