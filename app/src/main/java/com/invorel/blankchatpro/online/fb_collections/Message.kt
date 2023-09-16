package com.invorel.blankchatpro.online.fb_collections

data class Message(
  val id: Int,
  val message: String,
  val senderId: String,
  val receiverId: String,
  val isMessageModeOn: Boolean,
  val status: Byte,
  val sentTime: Long,
  val receivedTime: Long = -1L,
)

//status
// 0 - processing
// 1 - Sent
// 2 - Received
// 3 - Seen
