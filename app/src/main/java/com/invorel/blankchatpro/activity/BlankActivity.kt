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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.invorel.blankchatpro.activity.CHATNavArgs.HasPhoto
import com.invorel.blankchatpro.activity.CHATNavArgs.Id
import com.invorel.blankchatpro.activity.CHATNavArgs.IsCameFromHomeScreen
import com.invorel.blankchatpro.activity.CHATNavArgs.Name
import com.invorel.blankchatpro.activity.CHATNavArgs.Number
import com.invorel.blankchatpro.activity.Screens.HOME
import com.invorel.blankchatpro.activity.Screens.LOGIN
import com.invorel.blankchatpro.activity.Screens.PROFILE
import com.invorel.blankchatpro.activity.Screens.SPLASH
import com.invorel.blankchatpro.activity.Screens.TERMS
import com.invorel.blankchatpro.activity.Screens.CONTACTS
import com.invorel.blankchatpro.activity.Screens.CHAT
import com.invorel.blankchatpro.app.BlankApp
import com.invorel.blankchatpro.compose.common.HideStatusBar
import com.invorel.blankchatpro.compose.screens.ChatScreen
import com.invorel.blankchatpro.compose.screens.ContactsListScreen
import com.invorel.blankchatpro.compose.screens.HomeScreen
import com.invorel.blankchatpro.compose.screens.LoginScreen
import com.invorel.blankchatpro.compose.screens.ProfileUpdateScreen
import com.invorel.blankchatpro.compose.screens.SplashScreen
import com.invorel.blankchatpro.compose.screens.TermsConditionsScreen
import com.invorel.blankchatpro.state.Contact
import com.invorel.blankchatpro.ui.theme.BlankChatProTheme
import com.invorel.blankchatpro.ui.theme.black
import com.invorel.blankchatpro.utils.ContentResolverUtils
import com.invorel.blankchatpro.utils.FirebaseUtils
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
    val loginViewModel = LoginViewModel(appContext.dataStore)
    val profileViewModel = ProfileViewModel(appContext.dataStore)
    val homeViewModel = HomeViewModel(appContext.dataStore)
    val contactsViewModel = ContactsViewModel(appContext.localDb)
    val chatsViewModel = ChatsViewModel(appContext.localDb)
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
            })
        }

        composable(CONTACTS.route) {
          ContactsListScreen(
            viewModel = contactsViewModel,
            onContactPicked = {
              val chatNavRoute = "Chat"
                .plus("/${it.number}")
                .plus("/${it.name}")
                .plus("/${it.id}")
                .plus("/${it.photo != null}")
                .plus("/${false}")
              navController.navigate(chatNavRoute) {
                popUpTo(CONTACTS.route) {
                  inclusive = true
                }
              }
            })
        }

        composable(
          CHAT.route,
          arguments = listOf(
            navArgument(Number.name) { type = NavType.StringType },
            navArgument(Name.name) { type = NavType.StringType },
            navArgument(Id.name) { type = NavType.LongType },
            navArgument(HasPhoto.name) { type = NavType.BoolType },
            navArgument(HasPhoto.name) { type = NavType.BoolType },
          ),
        ) { backStackEntry ->

          if (backStackEntry.arguments == null) return@composable

          val contactNumber =
            backStackEntry.arguments!!.getString(Number.name).orEmpty()
          if (contactNumber.isEmpty()) return@composable

          val contactId = backStackEntry.arguments!!.getLong(Id.name)
          val contactName = backStackEntry.arguments!!.getString(Name.name).orEmpty()
          val isCameFromHomeScreen = backStackEntry.arguments!!.getBoolean(IsCameFromHomeScreen.name)

          var photo: Bitmap? = null
          if (backStackEntry.arguments!!.getBoolean(HasPhoto.name, false)) {
            photo = ContentResolverUtils.retrieveThumbNailPhoto(
              contactId = contactId,
              contentResolver = LocalContext.current.contentResolver
            )
          }

          //Todo id is unused inside compose remove it later
          val contact = Contact(
            id = contactId,
            name = contactName,
            number = contactNumber,
            photo = photo
          )

          ChatScreen(contact = contact,
            viewModel = chatsViewModel,
            onBackClick = {
            navController.popBackStack()
          }, isCameFromHomeScreen =  isCameFromHomeScreen
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
  CHAT("Chat/{$Number}/{$Name}/{$Id}/{$HasPhoto}/{$IsCameFromHomeScreen}"),
}

enum class CHATNavArgs {
  Number,
  Name,
  Id,
  HasPhoto,
  IsCameFromHomeScreen
}