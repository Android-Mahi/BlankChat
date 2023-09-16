package com.invorel.blankchatpro.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object IntentUtils {
  fun openSettingsPage(context: Context) {
    Intent().apply {
      action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
      data = Uri.fromParts("package", context.packageName, null)
    }.also { context.startActivity(it) }
  }
}