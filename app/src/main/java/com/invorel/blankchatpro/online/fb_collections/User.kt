package com.invorel.blankchatpro.online.fb_collections

data class User(
  val userId: String = "",
  val mobileNumber: String = "",
  val fcmToken: String = "",
  val gender: Int = 0,
  val lastRoomCreatedAt: Long = -1L,
  val name: String = "",
  val about: String = "",
  val photo: String = "",
  val chatRoomIds: List<String> = listOf()
) {
  companion object {
    const val chatRoomIdsKey ="chatRoomIds"
    const val userIdKey ="userId"
    const val fcmTokenKey ="fcmToken"
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