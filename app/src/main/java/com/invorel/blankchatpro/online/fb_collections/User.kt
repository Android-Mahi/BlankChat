package com.invorel.blankchatpro.online.fb_collections

import com.google.firebase.firestore.DocumentId
import com.google.gson.JsonArray

data class User(
  @DocumentId val userId: String,
  val mobileNumber: String,
  val fCMToken: String,
  val gender: Byte = 0,
  val lastRoomCreatedAt: Long = System.currentTimeMillis(),
  val name: String = "",
  val about: String = "",
  val photo: String = "",
  val chatRoomIds: JsonArray
) {
  companion object {
    const val chatRoomIdsKey ="ChatRoomIds"
    const val userIdKey ="userId"
    const val fcmTokenKey ="fCMToken"
    const val nameKey ="name"
    const val aboutKey ="about"
    const val profilePhotoKey ="photo"
    const val genderKey ="gender"
    const val isOnlineKey ="isOnline"
  }
}

//Gender
// 0 - Male
// 1 - Female