package com.invorel.blankchatpro.state

import com.invorel.blankchatpro.online.fb_collections.Message
import com.invorel.blankchatpro.viewModels.MessageUIModel

data class ChatsUiState(
  var messagesList: List<MessageUIModel> = listOf() ,
  val isMessageUpdateInProgress: Boolean = false,
  var currentMessage: Message = Message(1, "", "", "", false, 0, -1L),
  val errorMessage: String = "",
)