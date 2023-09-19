package com.invorel.blankchatpro.online.fb_collections

import com.google.firebase.firestore.PropertyName

data class Message(
  @get:PropertyName("id")
  val id: Int = -1,
  @get:PropertyName("message")
  val message: String = "",
  @get:PropertyName("senderId")
  val senderId: String = "",
  @get:PropertyName("receiverId")
  val receiverId: String = "",
  @get:PropertyName("isMessageModeOn")
  val isMessageModeOn: Boolean = false,
  @get:PropertyName("status")
  val status: Int = -1,
  @get:PropertyName("sentTime")
  val sentTime: Long = -1L,
  @get:PropertyName("receivedTime")
  val receivedTime: Long = -1L,
) {
  companion object {
    const val participantsKey = "Participants"
    const val latestMessageKey = "LatestMessage"

    const val idKey = "id"
    const val messageKey = "message"
    const val senderIdKey = "senderId"
    const val receiverIdKey = "receiverId"
    const val isMessageModeOnKey = "isMessageModeOn"
    const val statusKey = "status"
    const val sentTimeKey = "sentTime"
    const val receivedTimeKey = "receivedTime"
  }
}

//status
// 0 - processing
// 1 - Sent
// 2 - Received
// 3 - Seen
