package com.invorel.blankchatpro.utils

import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.invorel.blankchatpro.utils.NavigationUtils.CHATNavArgs.ChatRoomId
import com.invorel.blankchatpro.utils.NavigationUtils.CHATNavArgs.FBUserId
import com.invorel.blankchatpro.utils.NavigationUtils.CHATNavArgs.FBUserImage
import com.invorel.blankchatpro.utils.NavigationUtils.CHATNavArgs.FCMToken
import com.invorel.blankchatpro.utils.NavigationUtils.CHATNavArgs.HasPhoneContactPhoto
import com.invorel.blankchatpro.utils.NavigationUtils.CHATNavArgs.IsCameFromHomeScreen
import com.invorel.blankchatpro.utils.NavigationUtils.CHATNavArgs.IsReceiverOnline
import com.invorel.blankchatpro.utils.NavigationUtils.CHATNavArgs.Name
import com.invorel.blankchatpro.utils.NavigationUtils.CHATNavArgs.Number
import com.invorel.blankchatpro.utils.NavigationUtils.CHATNavArgs.PhoneContactId
import com.invorel.blankchatpro.utils.NavigationUtils.ChatRouteType.ChatFromContactsList
import com.invorel.blankchatpro.utils.NavigationUtils.ChatRouteType.ChatFromHomeList
import java.lang.StringBuilder

object NavigationUtils {

  enum class CHATNavArgs {
    Number,
    Name,
    IsCameFromHomeScreen,
    FBUserId, //Optional
    PhoneContactId, //Optional
    HasPhoneContactPhoto, //Optional
    FBUserImage, //Optional
    ChatRoomId, //Optional
    FCMToken, //Optional
    IsReceiverOnline, //Optional
  }

  val ChatScreenRoute =
    "Chat/Number={$Number}/Name={$Name}/IsCameFromHomeScreen={$IsCameFromHomeScreen}/IsReceiverOnline={$IsReceiverOnline}?FBUserId={$FBUserId}&PhoneContactId={$PhoneContactId}&HasPhoneContactPhoto={$HasPhoneContactPhoto}&FBUserImage={$FBUserImage}&ChatRoomId={$ChatRoomId}&FCMToken={$FCMToken}"

  val chatScreenArgsList = listOf(
    navArgument(Number.name) {
      type = NavType.StringType
      defaultValue = ""
    },
    navArgument(Name.name) {
      type = NavType.StringType
      defaultValue = ""
    },
    navArgument(IsCameFromHomeScreen.name) {
      type = NavType.BoolType
      defaultValue = false
    },
    navArgument(FBUserId.name) {
      type = NavType.StringType
      defaultValue = ""
    },
    navArgument(PhoneContactId.name) {
      type = NavType.LongType
      defaultValue = -1L
    },
    navArgument(HasPhoneContactPhoto.name) {
      type = NavType.BoolType
      defaultValue = false
    },
    navArgument(FBUserImage.name) {
      type = NavType.StringType
      defaultValue = ""
    },
    navArgument(ChatRoomId.name) {
      type = NavType.StringType
      defaultValue = ""
    },
    navArgument(FCMToken.name) {
      type = NavType.StringType
      defaultValue = ""
    },
    navArgument(IsReceiverOnline.name) {
      type = NavType.BoolType
      defaultValue = false
    },
  )

  sealed class ChatRouteType {
    data class ChatFromContactsList(
      val number: String,
      val name: String,
      val contactId: Long,
      val isContactHadThumbnailPhoto: Boolean,
    ) : ChatRouteType()

    data class ChatFromHomeList(
      val number: String,
      val name: String,
      val userId: String,
      //Image from Firebase
      val userImage: String,
      val roomId: String,
      val fcmToken: String,
      val isOnline: Boolean,
    ) : ChatRouteType()
  }

  fun buildChatScreenNavRoute(type: ChatRouteType): String {

    val routeStringBuilder = StringBuilder().also {
      it.append("Chat")
    }

    when (type) {
      is ChatFromContactsList -> {
        routeStringBuilder.append("/$Number=")
          .append(type.number)
          .append("/$Name=")
          .append(type.name)
          .append("/$IsCameFromHomeScreen=")
          .append(false)
          .append("/$IsReceiverOnline=")
          //if the user picks the already chat started contact receiver
          // status will be updated once we entered into chat Screen
          .append(false)
          //Optional Arguments exist only in one type not both
          .append("?$PhoneContactId=")
          .append(type.contactId)
          .append("&$HasPhoneContactPhoto=")
          .append(type.isContactHadThumbnailPhoto)
      }

      is ChatFromHomeList -> {
        routeStringBuilder.append("/$Number=")
          .append(type.number)
          .append("/$Name=")
          .append(type.name)
          .append("/$IsCameFromHomeScreen=")
          .append(true)
          .append("/$IsReceiverOnline=")
          .append(type.isOnline)
          //Optional Arguments exist only in one type not both
          .append("?$FBUserId=")
          .append(type.userId)
          .append("&$FBUserImage=")
          .append(type.userImage)
          .append("&$ChatRoomId=")
          .append(type.roomId)
          .append("&$FCMToken=")
          .append(type.fcmToken)
      }
    }

    return routeStringBuilder.toString()
  }
}