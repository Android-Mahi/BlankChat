package com.invorel.blankchatpro.online.fb_collections

import com.google.firebase.firestore.PropertyName

data class User(
  val userId: String = "",
  val mobileNumber: String = "",
  val fcmToken: String = "",
  val gender: Int = 0,
  val lastRoomCreatedAt: Long = -1L,
  val name: String = "",
  val about: String = "",
  val photo: String = "",
  val chatRoomIds: List<String> = listOf(),
  @field:PropertyName("lastProfileUpdatedAt")
  val lastProfileUpdatedAt: Long = -1L,
) {
  companion object {
    const val chatRoomIdsKey = "chatRoomIds"
    const val userIdKey = "userId"
    const val fcmTokenKey = "fcmToken"
    const val nameKey = "name"
    const val aboutKey = "about"
    const val profilePhotoKey = "photo"
    const val genderKey = "gender"
    const val isOnlineKey = "isOnline"
    const val lastRoomCreatedAtKey = "lastRoomCreatedAt"
    const val lastProfileUpdatedAt = "lastProfileUpdatedAt"
  }
}

//Gender
// 0 - Male
// 1 - Female