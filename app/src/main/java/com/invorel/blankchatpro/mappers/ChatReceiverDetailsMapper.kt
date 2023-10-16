package com.invorel.blankchatpro.mappers

import com.invorel.blankchatpro.local.tables.LocalReceiverDetail
import com.invorel.blankchatpro.viewModels.ChatReceiverDetails

fun LocalReceiverDetail.toUiModel() =
  ChatReceiverDetails(
    userId = userId,
    fcmToken = "",
    number = number,
    name = name,
    photo = photo,
  )