package com.invorel.blankchatpro.mappers

import com.invorel.blankchatpro.local.tables.LocalMessage
import com.invorel.blankchatpro.viewModels.MessageUIModel

fun MessageUIModel.toLocalModel() =
  LocalMessage(
    id = id,
    roomId = roomId,
    message = message,
    senderId = senderId,
    receiverId = receiverId,
    sentTime = sentTime,
    receivedTime = receivedTime,
    status = status,
    isSentByMessageMode = isSentByMessageMode
  )