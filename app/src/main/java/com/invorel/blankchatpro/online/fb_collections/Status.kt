package com.invorel.blankchatpro.online.fb_collections

data class Status(
  val userId: String,
  val isOnline: Boolean = false
) {
  companion object {
    const val isOnlineKey = "isOnline"
  }
}