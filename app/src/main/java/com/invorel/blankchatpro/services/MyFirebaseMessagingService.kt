package com.invorel.blankchatpro.services

import com.google.firebase.messaging.FirebaseMessagingService
import com.invorel.blankchatpro.app.BlankApp

class MyFirebaseMessagingService : FirebaseMessagingService() {
  override fun onNewToken(token: String) {
    super.onNewToken(token)
    (applicationContext as BlankApp).also {
      it.saveFCMTokenInDataStore(token)
    }
  }
}