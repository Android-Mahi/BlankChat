package com.invorel.blankchatpro.mappers

import com.invorel.blankchatpro.local.tables.LocalMessage
import com.invorel.blankchatpro.viewModels.LatestHomeChatMessage

fun LocalMessage.toLatestMessageUIModel() =
  LatestHomeChatMessage(
    sentTime = sentTime,
    receivedTime = receivedTime,
    senderId = senderId,
    receiverId = receiverId,
    message = message,
    status = status,
    isSentInMessageMode = isSentByMessageMode,
    messageId = id
  )