package com.invorel.blankchatpro.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object NetWorkUtils {

  fun isNetworkConnected(context: Context): Boolean {
    val connectivityManager =
      context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val networkCapabilities = connectivityManager.activeNetwork ?: return false
    val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false

    return when {
      actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
      actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
      // For other transports like Ethernet, Bluetooth, etc.
      else -> false
    }
  }
}
