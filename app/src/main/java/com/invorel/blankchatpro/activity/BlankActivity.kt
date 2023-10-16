package com.invorel.blankchatpro.activity

import android.graphics.Bitmap
import android.os.Build.VERSION_CODES
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.invorel.blankchatpro.activity.Screens.CHAT
import com.invorel.blankchatpro.activity.Screens.CONTACTS
import com.invorel.blankchatpro.activity.Screens.HOME
import com.invorel.blankchatpro.activity.Screens.LOGIN
import com.invorel.blankchatpro.activity.Screens.PROFILE
import com.invorel.blankchatpro.activity.Screens.SPLASH
import com.invorel.blankchatpro.activity.Screens.TERMS
import com.invorel.blankchatpro.app.BlankApp
import com.invorel.blankchatpro.compose.common.HideStatusBar
import com.invorel.blankchatpro.compose.screens.ChatScreen
import com.invorel.blankchatpro.compose.screens.ContactsListScreen
import com.invorel.blankchatpro.compose.screens.HomeScreen
import com.invorel.blankchatpro.compose.screens.LoginScreen
import com.invorel.blankchatpro.compose.screens.ProfileUpdateScreen
import com.invorel.blankchatpro.compose.screens.SplashScreen
import com.invorel.blankchatpro.compose.screens.TermsConditionsScreen
import com.invorel.blankchatpro.constants.DEFAULT_USER_NAME
import com.invorel.blankchatpro.local.repo.LocalRepo
import com.invorel.blankchatpro.ui.theme.BlankChatProTheme
import com.invorel.blankchatpro.ui.theme.black
import com.invorel.blankchatpro.utils.ContentResolverUtils
import com.invorel.blankchatpro.utils.FirebaseUtils
import com.invorel.blankchatpro.utils.NavigationUtils
import com.invorel.blankchatpro.utils.NavigationUtils.CHATNavArgs.ChatRoomId
import com.invorel.blankchatpro.utils.NavigationUtils.CHATNavArgs.FBUserId
import com.invorel.blankchatpro.utils.NavigationUtils.CHATNavArgs.FBUserImage
import com.invorel.blankchatpro.utils.NavigationUtils.CHATNavArgs.FCMToken
import com.invorel.blankchatpro.utils.NavigationUtils.CHATNavArgs.HasPhoneContactPhoto
import com.invorel.blankchatpro.utils.NavigationUtils.CHATNavArgs.ImgToken
import com.invorel.blankchatpro.utils.NavigationUtils.CHATNavArgs.IsCameFromHomeScreen
import com.invorel.blankchatpro.utils.NavigationUtils.CHATNavArgs.IsReceiverOnline
import com.invorel.blankchatpro.utils.NavigationUtils.CHATNavArgs.Name
import com.invorel.blankchatpro.utils.NavigationUtils.CHATNavArgs.Number
import com.invorel.blankchatpro.utils.NavigationUtils.CHATNavArgs.PhoneContactId
import com.invorel.blankchatpro.utils.NavigationUtils.ChatRouteType.ChatFromContactsList
import com.invorel.blankchatpro.utils.NavigationUtils.ChatRouteType.ChatFromHomeList
import com.invorel.blankchatpro.utils.NavigationUtils.buildChatScreenNavRoute
import com.invorel.blankchatpro.utils.NavigationUtils.chatScreenArgsList
import com.invorel.blankchatpro.viewModels.ChatReceiverDetails
import com.invorel.blankchatpro.viewModels.ChatsViewModel
import com.invorel.blankchatpro.viewModels.ContactsViewModel
import com.invorel.blankchatpro.viewModels.HomeViewModel
import com.invorel.blankchatpro.viewModels.LoginViewModel
import com.invorel.blankchatpro.viewModels.ProfileViewModel

class BlankActivity : ComponentActivity() {

  @RequiresApi(VERSION_CODES.O)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val appContext = (applicationContext as BlankApp)
    val loginViewModel = LoginViewModel(dataStore = appContext.dataStore, localRepo = LocalRepo(appContext.localDb))
    val profileViewModel = ProfileViewModel(dataStore = appContext.dataStore, localRepo = LocalRepo(appContext.localDb))
    val homeViewModel = HomeViewModel(dataStore = appContext.dataStore, localRepo = LocalRepo(appContext.localDb))
    val contactsViewModel = ContactsViewModel(localDatabase = appContext.localDb)
    val chatsViewModel = ChatsViewModel(localRepo = LocalRepo(appContext.localDb))
    setContent {
      RootCompose(
        loginViewModel = loginViewModel,
        profileViewModel = profileViewModel,
        homeViewModel = homeViewModel,
        contactsViewModel = contactsViewModel,
        chatsViewModel = chatsViewModel,
      )
    }
  }
}

@RequiresApi(VERSION_CODES.O)
@Composable
fun RootCompose(
  loginViewModel: LoginViewModel,
  profileViewModel: ProfileViewModel,
  homeViewModel: HomeViewModel,
  contactsViewModel: ContactsViewModel,
  chatsViewModel: ChatsViewModel,
) {
  BlankChatProTheme {
    // A surface container using the 'background' color from the theme
    Surface(modifier = Modifier.fillMaxSize(), color = black) {

      val navController = rememberNavController()

      //todo call HideStatusBar(false) function based on conditional flow page from splash
      NavHost(navController = navController, startDestination = SPLASH.route) {

        composable(SPLASH.route) {
          HideStatusBar()
          SplashScreen(onTimeOut = {
            if (FirebaseUtils.currentUser != null) {
              navController.navigate(HOME.route) {
                popUpTo(0)
              }
            } else {
              navController.navigate(TERMS.route) {
                popUpTo(0)
              }
            }
          })
        }

        composable(TERMS.route) {
          HideStatusBar(false)
          TermsConditionsScreen {
            navController.navigate(LOGIN.route)
          }
        }

        composable(LOGIN.route) {
          LoginScreen(
            viewModel = loginViewModel,
            onBackPressed = {
              navController.popBackStack()
            },
            onSignInCompleted = {
              loginViewModel.clearSignInState()
              navController.navigate(PROFILE.route) {
                popUpTo(0)
              }
            }
          )
        }

        composable(PROFILE.route) {
          HideStatusBar(false)
          ProfileUpdateScreen(
            viewModel = profileViewModel,
            onSheetDismissed = {
              navController.navigate(HOME.route) {
                popUpTo(0)
              }
            }
          )
        }

        composable(HOME.route) {
          HideStatusBar(false)
          HomeScreen(viewModel = homeViewModel,
            onMoreClickedInProfile = {
              navController.navigate(PROFILE.route)
            }, onNewChatOptionClick = {
              navController.navigate(CONTACTS.route)
            }, onChatRoomPicked = { chatRoomDetails ->
              val route = buildChatScreenNavRoute(
                type = ChatFromHomeList(
                  number = chatRoomDetails.receiverDetails.number,
                  name = chatRoomDetails.receiverDetails.name.ifEmpty { DEFAULT_USER_NAME },
                  userId = chatRoomDetails.receiverDetails.userId,
                  fcmToken = chatRoomDetails.receiverDetails.fcmToken,
                  isOnline = chatRoomDetails.receiverDetails.isReceiverOnline,
                  roomId = chatRoomDetails.roomId,
                  userImage = chatRoomDetails.receiverDetails.photo
                )
              )
              navController.navigate(route)
            })
        }

        composable(CONTACTS.route) {
          ContactsListScreen(
            viewModel = contactsViewModel,
            onContactPicked = { contact ->
              val route = buildChatScreenNavRoute(
                ChatFromContactsList(
                number = contact.number,
                name = contact.name,
                contactId = contact.id,
                isContactHadThumbnailPhoto = contact.photo != null
              )
              )
              navController.navigate(route) {
                popUpTo(CONTACTS.route) {
                  inclusive = true
                }
              }
            },
            onBackPressed = { navController.popBackStack() }
            )
        }

        composable(
          route = CHAT.route,
          arguments = chatScreenArgsList,
        ) { backStackEntry ->

          if (backStackEntry.arguments == null) return@composable

          val contactNumber =
            backStackEntry.arguments!!.getString(Number.name).orEmpty()
          if (contactNumber.isEmpty()) return@composable

          var localContactPhoto: Bitmap? = null

          val contactName = backStackEntry.arguments!!.getString(Name.name).orEmpty()
          val isCameFromHomeScreen = backStackEntry.arguments!!.getBoolean(IsCameFromHomeScreen.name)

          val fBUSerId = backStackEntry.arguments!!.getString(FBUserId.name).orEmpty()
          val contactId = backStackEntry.arguments!!.getLong(PhoneContactId.name)

          if (backStackEntry.arguments!!.getBoolean(HasPhoneContactPhoto.name, false)) {
            localContactPhoto = ContentResolverUtils.retrieveThumbNailPhoto(
              contactId = contactId,
              contentResolver = LocalContext.current.contentResolver
            )
          }

          val fbProfileImage  = backStackEntry.arguments!!.getString(FBUserImage.name).orEmpty()
          val imgToken = backStackEntry.arguments!!.getString(ImgToken.name).orEmpty()
          val chatRoomId = backStackEntry.arguments!!.getString(ChatRoomId.name).orEmpty()
          val fcmToken = backStackEntry.arguments!!.getString(FCMToken.name).orEmpty()
          val isReceiverOnline = backStackEntry.arguments!!.getBoolean(IsReceiverOnline.name, false)

          val urlWithoutSpace = if (fbProfileImage.isNotEmpty()) fbProfileImage.plus("&token=").plus(imgToken) else ""
          val finalImgUrl = if (urlWithoutSpace.isNotEmpty()) urlWithoutSpace.replace("/Profile_Pictures/+", "/Profile_Pictures%2F%2B") else ""

          val receiverDetails = ChatReceiverDetails(
            name = contactName,
            number = contactNumber,
            userId = fBUSerId,
            fcmToken = fcmToken,
            photo = finalImgUrl,
            isReceiverOnline = isReceiverOnline
          )

          ChatScreen(
            localContactPhoto = localContactPhoto,
            receiverDetails = receiverDetails,
            chatRoomId = chatRoomId,
            viewModel = chatsViewModel,
            onBackClick = { navController.popBackStack() },
            //Todo remove below flag if it is not needed
            isCameFromHomeScreen =  isCameFromHomeScreen
          )
        }

      }

    }
  }
}

enum class Screens(val route: String) {
  SPLASH("Splash"),
  TERMS("TermsAndConditions"),
  LOGIN("Login"),
  PROFILE("Profile"),
  HOME("Home"),
  CONTACTS("Contacts"),
  CHAT(NavigationUtils.ChatScreenRoute),
}
