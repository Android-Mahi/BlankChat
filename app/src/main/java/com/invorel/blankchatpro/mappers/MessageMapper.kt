package com.invorel.blankchatpro.mappers

import com.invorel.blankchatpro.local.tables.LocalMessage
import com.invorel.blankchatpro.online.fb_collections.Message
import com.invorel.blankchatpro.viewModels.MessageUIModel

fun LocalMessage.toUIModel() =
  MessageUIModel(
    roomId = roomId,
    id = id,
    message = message,
    senderId = senderId,
    receiverId = receiverId,
    sentTime = sentTime,
    receivedTime = receivedTime,
    status = status,
    isSentByMessageMode = isSentByMessageMode
  )

fun Message.toUIModel() =
  MessageUIModel(
    roomId = "",
    id = id,
    message = message,
    senderId = senderId,
    receiverId = receiverId,
    sentTime = sentTime,
    receivedTime = receivedTime,
    status = status,
    isSentByMessageMode = isSentByMessageMode
  )