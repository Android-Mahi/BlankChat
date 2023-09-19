package com.invorel.blankchatpro.state

import com.invorel.blankchatpro.online.fb_collections.Message

data class ChatsUiState(
  var messagesList: List<Message> = listOf(),
  val isMessageUpdateInProgress: Boolean = false,
  var currentMessage: Message = Message(1, "", "", "", false, 0, -1L),
  val errorMessage: String = "",
)