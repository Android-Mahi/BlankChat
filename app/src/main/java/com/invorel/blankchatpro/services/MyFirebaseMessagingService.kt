package com.invorel.blankchatpro.services

import com.google.firebase.messaging.FirebaseMessagingService

class MyFirebaseMessagingService : FirebaseMessagingService() {
  override fun onNewToken(token: String) {
    super.onNewToken(token)
    //Todo Update to Firebase & After that saves in DataStore
  }
}