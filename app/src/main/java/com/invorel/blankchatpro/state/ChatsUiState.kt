package com.invorel.blankchatpro.state

import com.invorel.blankchatpro.online.fb_collections.Message

data class ChatsUiState(
  val chats: MutableList<Message> = mutableListOf(),
  val fetchInProgress: Boolean = false,
  val currentMessage: Message = Message(0, "", "", "", false, 0, -1L),
  val errorMessage: String = "",
)