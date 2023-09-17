package com.invorel.blankchatpro.utils

import android.app.Activity
import android.net.Uri
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import com.invorel.blankchatpro.constants.DEFAULT_CHATROOM_COLLECTION_NAME
import com.invorel.blankchatpro.constants.DEFAULT_CHAT_ROOM_SEPARATOR
import com.invorel.blankchatpro.constants.DEFAULT_COUNTRY_CODE
import com.invorel.blankchatpro.constants.DEFAULT_OTP_RESEND_TIME_SECONDS
import com.invorel.blankchatpro.constants.DEFAULT_DIRECTORY_NAME_FOR_PROFILE_IN_FIREBASE
import com.invorel.blankchatpro.constants.DEFAULT_STATUS_COLLECTION_NAME
import com.invorel.blankchatpro.constants.DEFAULT_USERS_COLLECTION_NAME
import com.invorel.blankchatpro.constants.DEFAULT__MESSAGE_COLLECTION_NAME
import com.invorel.blankchatpro.online.fb_collections.Message
import com.invorel.blankchatpro.online.fb_collections.User
import java.util.concurrent.TimeUnit.SECONDS

object FirebaseUtils {

  private val auth = FirebaseAuth.getInstance()

  var currentUser = auth.currentUser

  private fun getAuthOptions(
    phoneNo: String,
    activity: Activity,
    callBacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks,
  ) = PhoneAuthOptions.newBuilder(auth).setPhoneNumber(DEFAULT_COUNTRY_CODE.plus(phoneNo))
    .setTimeout(DEFAULT_OTP_RESEND_TIME_SECONDS.toLong(), SECONDS).setActivity(activity)
    .setCallbacks(callBacks).build()

  fun verifyPhoneNumber(
    phoneNo: String,
    activity: Activity,
    onCodeSent: (String, ForceResendingToken) -> Unit,
    onVerificationFailed: (String) -> Unit,
    onVerificationSuccess: (PhoneAuthCredential) -> Unit,
  ) {

    val callBacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

      override fun onCodeSent(verificationId: String, token: ForceResendingToken) {
        super.onCodeSent(verificationId, token)
        onCodeSent.invoke(verificationId, token)
      }

      override fun onVerificationCompleted(credential: PhoneAuthCredential) {
        onVerificationSuccess.invoke(credential)
      }

      override fun onVerificationFailed(e: FirebaseException) {
        when (e) {
          is FirebaseAuthInvalidCredentialsException -> {
            onVerificationFailed.invoke("Invalid Credentials Provided")
          }

          is FirebaseTooManyRequestsException -> {
            onVerificationFailed.invoke("Too Many request sent for this PhoneNumber")
          }

          is FirebaseAuthMissingActivityForRecaptchaException -> {
            onVerificationFailed.invoke("No activity provided for Recaptcha")
          }

          else -> {
            onVerificationFailed.invoke("Verification Failed")
          }
        }
      }
    }

    val options = getAuthOptions(
      phoneNo = phoneNo, activity = activity, callBacks = callBacks
    )
    PhoneAuthProvider.verifyPhoneNumber(options)
  }

  fun signInWithPhoneAuthCredentials(
    credential: PhoneAuthCredential,
    onSignInSuccess: (FirebaseUser?) -> Unit,
    onSignInFailed: (String) -> Unit,
  ) {
    auth.signInWithCredential(credential).addOnCompleteListener { task ->
        if (task.isSuccessful) {
          val user = task.result.user
          onSignInSuccess.invoke(user)
          currentUser = user
        } else {

          when (task.exception) {
            is FirebaseAuthInvalidCredentialsException -> {
              onSignInFailed.invoke("Invalid OTP Code Provided")
            }

            else -> onSignInFailed.invoke(task.exception?.message ?: "SignInFailed")
          }
        }
      }
  }

  fun updateNameAndAboutInFirebaseAuthUserProfile(
    nameAndAbout: String,
    onSuccess: () -> Unit,
    onFailed: (String) -> Unit,
  ) {

    if (currentUser == null) return

    val profileUpdateRequest =
      UserProfileChangeRequest.Builder().setDisplayName(nameAndAbout).build()

    currentUser!!.updateProfile(profileUpdateRequest).addOnCompleteListener { task ->
      if (task.isSuccessful) {
        onSuccess.invoke()
      } else {
        onFailed.invoke(task.exception?.message.orEmpty())
      }
    }
  }

  fun uploadPhotoInFirebase(
    imageUri: Uri,
    onProfileImageUploaded: (String) -> Unit,
    onProfileImageUploadFailed: (String) -> Unit,
  ) {

    if (currentUser == null) return

    checkIfProfileImageAlreadyExistsInFireBase(onNoImageExists = {
      // we can upload this fresh Image
      updateNameAndAboutInFirebaseAuthUserProfile(
        imageUri = imageUri,
        onProfileImageUploaded = onProfileImageUploaded,
        onProfileImageUploadFailed = onProfileImageUploadFailed
      )
    }, onImageDeleted = {
      // we can upload this new image
      updateNameAndAboutInFirebaseAuthUserProfile(
        imageUri = imageUri,
        onProfileImageUploaded = onProfileImageUploaded,
        onProfileImageUploadFailed = onProfileImageUploadFailed
      )
    }, onError = {
      onProfileImageUploadFailed.invoke(it)
    })
  }

  private fun updateNameAndAboutInFirebaseAuthUserProfile(
    imageUri: Uri,
    onProfileImageUploaded: (String) -> Unit,
    onProfileImageUploadFailed: (String) -> Unit,
  ) {
    profileImageStorageRef.putFile(imageUri).addOnSuccessListener {
        getDownloadUrlOfUploadedImage(onDownloadUrlFetched = { downloadUrl ->
          onProfileImageUploaded.invoke(downloadUrl)
        }, onDownloadUrlFetchFailed = { errorMessage ->
          onProfileImageUploadFailed.invoke(errorMessage)
        })
      }.addOnFailureListener { e ->
        onProfileImageUploadFailed.invoke(e.message ?: "Upload Image Failed")
      }
  }

  private fun checkIfProfileImageAlreadyExistsInFireBase(
    onImageDeleted: () -> Unit,
    onNoImageExists: () -> Unit,
    onError: (String) -> Unit,
  ) {
    profileImageStorageRef.list(1).addOnSuccessListener { resultList ->
        if (resultList.items.isEmpty()) {
          //No Previous  UploadedImage
          onNoImageExists.invoke()
        } else {
          //Image Exists deletion Needed
          deleteExistingProfileImageInFirebase(onExistingImageDeleted = {
            onImageDeleted.invoke()
          }, onExistingImageDeleteFailed = {
            onError.invoke(it)
          })
        }
      }.addOnFailureListener {
        onError.invoke(it.message ?: "Check Image in Fb Failed")
      }
  }

  private fun deleteExistingProfileImageInFirebase(
    onExistingImageDeleted: () -> Unit,
    onExistingImageDeleteFailed: (String) -> Unit,
  ) {
    profileImageStorageRef.delete().addOnSuccessListener {
        onExistingImageDeleted.invoke()
      }.addOnFailureListener {
        onExistingImageDeleteFailed.invoke(it.message ?: "Delete Failed")
      }
  }

  fun getDownloadUrlOfUploadedImage(
    onDownloadUrlFetched: (String) -> Unit,
    onDownloadUrlFetchFailed: (String) -> Unit,
  ) {
    profileImageStorageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
      onDownloadUrlFetched.invoke(downloadUrl.toString())
    }.addOnFailureListener { e ->

        onDownloadUrlFetchFailed.invoke(e.message ?: "Unable to get Download Url")
      }
  }

  private val profileImageStorageRef: StorageReference
    get() = FirebaseStorage.getInstance().reference.child(
      DEFAULT_DIRECTORY_NAME_FOR_PROFILE_IN_FIREBASE
    ).child(getDefaultProfileImageName())

  private fun getDefaultProfileImageName(): String {
    with(currentUser!!) {
      return uid.plus(".jpg")
    }
  }

  fun logOutUser() {
    auth.signOut()
  }

  // Fetching FCM Token
  fun getCurrentFCMToken(
    onTokenFetched: (String) -> Unit,
    onTokenFetchFailed: (String) -> Unit,
  ) {
    FirebaseMessaging.getInstance().token.addOnSuccessListener {
        onTokenFetched.invoke(it)
      }.addOnFailureListener {
        onTokenFetchFailed.invoke(it.message ?: "Failed to Get FCM Token")
      }
  }


  /*---------------------------------------LOGIN SCREEN METHODS STARTS------------------------------------------------------------------*/

  //Login Related Methods
  fun checkIfTheUserExistsInFirebase(
    mobileNumber: String,
    onUserExistStatusFetched: (Boolean) -> Unit,
    onFailed: (String) -> Unit
  ) {
    FirebaseFirestore.getInstance()
      .collection(DEFAULT_USERS_COLLECTION_NAME)
      .document(mobileNumber)
      .get().addOnSuccessListener {
        if (it.exists()) {
          onUserExistStatusFetched.invoke(true)
        } else {
          onUserExistStatusFetched.invoke(false)
        }
      }
      .addOnFailureListener {
        onFailed.invoke(it.message ?: "Failed to get Existing User")
      }
  }

  fun saveUserDetailsInFirebase(
    user: User,
    onUserDetailsSaved: () -> Unit,
    onUserDetailsFailedToSave: (String) -> Unit
    ) {
    FirebaseFirestore.getInstance().collection(DEFAULT_USERS_COLLECTION_NAME)
      .document(user.mobileNumber)
      .set(user)
      .addOnSuccessListener {
        onUserDetailsSaved.invoke()
      }
      .addOnFailureListener {
        onUserDetailsFailedToSave.invoke(it.message ?: "Failed to Save User Details")
      }
  }

  fun updateUserDetailsInFirebase(
    mobileNumber: String,
    userId: String,
    fcmToken: String,
    onUserDetailsUpdated: () -> Unit,
    onFailed: (String) -> Unit
  ) {

    FirebaseFirestore.getInstance().collection(DEFAULT_USERS_COLLECTION_NAME)
      .document(mobileNumber)
      .update(mapOf(
        User.userIdKey to userId,
        User.fcmTokenKey to fcmToken,
      ))
      .addOnSuccessListener { onUserDetailsUpdated.invoke() }
      .addOnFailureListener { onFailed.invoke(it.message ?: "Failed to Update Login details of existing User")  }
  }

  /*---------------------------------------LOGIN SCREEN METHODS ENDS------------------------------------------------------------------*/

  /*---------------------------------------PROFILE SCREEN METHODS STARTS------------------------------------------------------------------*/

  fun updateUserDetailsInFirebase(
    mobileNumber: String,
    name: String,
    about: String,
    gender: Byte,
    profilePhoto: String,
    onUserDetailsUpdated: () -> Unit,
    onFailed: (String) -> Unit
  ) {
    FirebaseFirestore.getInstance().collection(DEFAULT_USERS_COLLECTION_NAME)
      .document(mobileNumber)
      .update(mapOf(
        User.nameKey to name,
        User.aboutKey to about,
        User.profilePhotoKey to profilePhoto,
        User.genderKey to gender
      ))
      .addOnSuccessListener { onUserDetailsUpdated.invoke() }
      .addOnFailureListener { onFailed.invoke(it.message ?: "Failed to Update Profile details of existing User")  }
  }

  /*---------------------------------------PROFILE SCREEN METHODS ENDS------------------------------------------------------------------*/

  /*---------------------------------------HOME SCREEN METHODS STARTS------------------------------------------------------------------*/

  fun updateUserStatus(
    isOnline: Boolean,
    onFailed: (String) -> Unit,
    onUserStatusUpdated: () -> Unit
  ) {

    if (currentUser == null) {
      onFailed.invoke("Current FB User is Null")
      return
    }

    FirebaseFirestore.getInstance().collection(DEFAULT_STATUS_COLLECTION_NAME)
      .document(currentUser!!.uid)
      .set(mapOf(
        User.isOnlineKey to isOnline
      ))
      .addOnFailureListener { onFailed.invoke(it.message ?: " Failed to Update Status of the User") }
      .addOnSuccessListener {
        onUserStatusUpdated.invoke()
      }
  }

  fun listenChatRoomUpdates(
    roomId: String,
    onChatUpdated: () -> Unit,
    onError: (String) -> Unit
  ) {

    FirebaseFirestore.getInstance().collection(DEFAULT_CHATROOM_COLLECTION_NAME)
      .document(roomId)
      .addSnapshotListener { value, error ->
        if (error != null) {
          onError.invoke(error.message ?: "Unable to List $roomId updates")
        } else {
          onChatUpdated
        }
      }
  }

  /*---------------------------------------HOME SCREEN METHODS ENDS------------------------------------------------------------------*/


  /*---------------------------------------CHAT SCREEN METHODS STARTS------------------------------------------------------------------*/

  fun checkReceiverDetailsInFireBase(
    mobileNumber: String,
    onFailed: (String) -> Unit,
    onReceiverExistsWithLogin: (String) -> Unit,
    onReceiverExistsWithoutLogin: () -> Unit,
    onReceiverNotExists: () -> Unit
  ) {
    FirebaseFirestore.getInstance().collection(DEFAULT_USERS_COLLECTION_NAME).document(mobileNumber)
      .get()
      .addOnFailureListener { onFailed.invoke(it.message ?: "Failed to get Receiver User Details")  }
      .addOnSuccessListener {
        if (it.exists()) {
          // Receiver Details Exists in DB
          val user = it.toObject(User::class.java)
          if (user == null)  onFailed.invoke(" Receiver's user object parsed as null")

          if (user!!.userId.isNotEmpty()) {
            //Receiver logged In
            onReceiverExistsWithLogin.invoke(user.userId)
          } else {
            //Receiver Not logged In
            onReceiverExistsWithoutLogin.invoke()
          }
        } else {
          // Receiver Details Not Present in DB
          onReceiverNotExists.invoke()
        }
      }
  }

  fun checkIfChatRoomExistsOrNotInDb(
    senderTag: String,
    receiverTag: String,
    onFailed: (String) -> Unit,
    onChatRoomExist: (String) -> Unit,
    onChatRoomDoesNotExist: () -> Unit,
  ) {

    if (currentUser == null) {
      onFailed.invoke("Current FB User is Null")
      return
    }

    if (currentUser!!.phoneNumber == null) {
      onFailed.invoke("Current FB User PhoneNumber is Null")
      return
    }

    //chat room may be initiated by user or receiver - chat Room id will be senderTag#receiverTag
    // we are checks roomId in both cases
    val oldChatRoomId1 = senderTag.plus(DEFAULT_CHAT_ROOM_SEPARATOR).plus(receiverTag)
    val oldChatRoomId2 = receiverTag.plus(DEFAULT_CHAT_ROOM_SEPARATOR).plus(senderTag)

    FirebaseFirestore.getInstance().collection(DEFAULT_CHATROOM_COLLECTION_NAME)
      .document(oldChatRoomId1)
      .get()
      .addOnSuccessListener {
        if (it.exists()) {
          onChatRoomExist.invoke(oldChatRoomId1)
        } else {
          FirebaseFirestore.getInstance().collection(DEFAULT_CHATROOM_COLLECTION_NAME)
            .document(oldChatRoomId2)
            .get()
            .addOnSuccessListener { doc ->
              if (doc.exists()) {
                onChatRoomExist.invoke(oldChatRoomId2)
              } else {
               onChatRoomDoesNotExist.invoke()
              }
            }
            .addOnFailureListener { e ->
              onFailed.invoke(e.message ?: "Failed to get existing Number Type Room Document 2")
            }
        }
      }
      .addOnFailureListener { e ->
        onFailed.invoke(e.message ?: "Failed to get existing Number Type Room Document 1")
      }

  }

  fun changeExistingRoomTypeFromNumberToUserId(
    receiverId: String,
    existingRoomId: String,
    onFailed: (String) -> Unit,
    onSuccess: (String) -> Unit,
  ) {

    if (currentUser == null) {
      onFailed.invoke("Got Current User Null")
      return
    }

    val senderId = currentUser!!.uid
    val newChatRoomId = senderId.plus(DEFAULT_CHAT_ROOM_SEPARATOR).plus(receiverId)

    //Getting Existing Number Type Room
    FirebaseFirestore.getInstance()
      .collection(DEFAULT_CHATROOM_COLLECTION_NAME)
      .document(existingRoomId)
      .get()
      .addOnFailureListener { e ->
        onFailed.invoke(
          e.message ?: "Failed to get existing Room while renaming"
        )
      }
      .addOnSuccessListener {
        val existingData = it.data

        if (existingData == null) {
          onFailed.invoke("Got Existing Room Data Null")
        }

        //Creates new UserId type Room
        FirebaseFirestore.getInstance().collection(DEFAULT_CHATROOM_COLLECTION_NAME)
          .document(newChatRoomId)
          .set(existingData ?: mapOf( DEFAULT__MESSAGE_COLLECTION_NAME to ""))
          .addOnFailureListener { e ->
            onFailed.invoke(
              e.message ?: "Failed to create userIdType Room while renaming"
            )
          }
          .addOnSuccessListener {

            //Deleting Old Number Type Room
            FirebaseFirestore.getInstance().collection(DEFAULT_CHATROOM_COLLECTION_NAME)
              .document(existingRoomId)
              .delete()
              .addOnFailureListener { e ->
                onFailed.invoke(
                  e.message ?: "Failed to delete existing Room while renaming"
                )
              }
              .addOnSuccessListener {
                onSuccess.invoke(newChatRoomId)
              }
          }
      }
  }

  fun createNotLoggedInReceiverDetailsInDB(
    mobileNumber: String,
    name: String,
    photo: String,
    chatRoomIds: JsonArray,
    onFailed: (String) -> Unit,
    onNotLoggedInReceiverCreated: () -> Unit
  ) {

    val notLoggedInReceiver = User(
      mobileNumber = mobileNumber,
      userId = "",
      fCMToken = "",
      name = name,
      photo = photo,
      chatRoomIds = chatRoomIds
    )
    FirebaseFirestore.getInstance().collection(DEFAULT_USERS_COLLECTION_NAME).document(mobileNumber)
      .set(notLoggedInReceiver)
      .addOnFailureListener { onFailed.invoke(it.message ?: "Failed to Create not Logged In Receiver User Details")  }
      .addOnSuccessListener {
        onNotLoggedInReceiverCreated.invoke()
      }
  }

  fun updateChatRoomIdInSendUserDocument(
    chatRoomIds: JsonArray,
    onFailed: (String) -> Unit,
    onChatRoomIdsUpdated: () -> Unit
  ) {

    if (currentUser == null) {
      onFailed.invoke("Got Null for Fb Current User")
    }

    if (currentUser!!.phoneNumber == null) {
      onFailed.invoke("Got Null for Fb Current User Phone No")
    }

    FirebaseFirestore.getInstance()
      .collection(DEFAULT_USERS_COLLECTION_NAME)
      .document(currentUser!!.phoneNumber!!)
      .update(mapOf(User.chatRoomIdsKey to chatRoomIds))
      .addOnFailureListener { onFailed.invoke(it.message ?: "Failed to update ChatRoomIds in Sender") }
      .addOnSuccessListener { onChatRoomIdsUpdated.invoke() }
  }

  fun getExistingChatRoomIdsForUser(
    mobileNumber: String,
    onExistingChatRoomIdsFetched:(JsonArray) -> Unit,
    onFailed:(String) -> Unit,
  )
  { FirebaseFirestore.getInstance().collection(DEFAULT_USERS_COLLECTION_NAME)
      .document(mobileNumber)
      .get()
      .addOnSuccessListener {
        val existingChatRooms = it.data?.get(User.chatRoomIdsKey) as JsonArray
        onExistingChatRoomIdsFetched.invoke(existingChatRooms)
      }
      .addOnFailureListener {
        onFailed.invoke(it.message ?: "Failed to fetch existing chatRoomIds")
      }
  }

  fun createChatRoomInFirebase(
    chatRoomId: String,
    message: Message,
    onChatRoomCreated: (String) -> Unit,
    onChatRoomCreateFailed: (String) -> Unit
  ) {
    FirebaseFirestore
      .getInstance()
      .collection(DEFAULT_CHATROOM_COLLECTION_NAME)
      .document(chatRoomId)
      .collection(DEFAULT__MESSAGE_COLLECTION_NAME)
      .document(message.id.toString())
      .set(mapOf(message.id to message))
      .addOnSuccessListener {
        onChatRoomCreated.invoke(chatRoomId)
      }
      .addOnFailureListener {
        onChatRoomCreateFailed.invoke(it.message ?: "Failed to create Chat Room")
      }
  }

  fun updateParticipantsDetailsInChatRoom(
    senderNumber: String,
    receiverNumber: String,
    chatRoomId: String,
    onFailed: (String) -> Unit,
    onParticipantDetailsUpdated: () -> Unit
  ) {

    val participantDetails = senderNumber.plus(DEFAULT_CHAT_ROOM_SEPARATOR).plus(receiverNumber)

    FirebaseFirestore.getInstance()
      .collection(DEFAULT_CHATROOM_COLLECTION_NAME)
      .document(chatRoomId)
      .update(mapOf(Message.participantsKey to participantDetails))
      .addOnFailureListener { onFailed.invoke(it.message ?: "Failed to update participant details in ChatRoom") }
      .addOnSuccessListener {
        onParticipantDetailsUpdated.invoke()
      }
  }

  fun updateChatRoomIdsInSenderAndReceiverDetails(
    chatRoomId: String,
    senderNumber: String,
    receiverMobileNumber: String,
    onFailed: (String) -> Unit,
    onSuccess: () -> Unit
  ) {

    //Getting Old ChatRoom Ids for Sender
    getExistingChatRoomIdsForUser(senderNumber,
      onFailed = onFailed,
      onExistingChatRoomIdsFetched = { oldSenderRoomIds ->
        //Todo test below line
        oldSenderRoomIds.add(JsonPrimitive(chatRoomId))
        //Updating new ChatRoomId appended with OldChatRoomIds to Sender
        FirebaseFirestore.getInstance().collection(DEFAULT_USERS_COLLECTION_NAME)
          .document(senderNumber)
          .update(mapOf(User.chatRoomIdsKey to oldSenderRoomIds))
          .addOnFailureListener { onFailed.invoke(it.message ?: "Failed to update new Chat Room Id to Sender") }
          .addOnSuccessListener {
            //Getting Old ChatRoom Ids for Receiver
            getExistingChatRoomIdsForUser(receiverMobileNumber, onFailed = onFailed, onExistingChatRoomIdsFetched = { oldReceiverRoomIds ->
              //Todo test below line
              oldReceiverRoomIds.add(JsonPrimitive(chatRoomId))
              //Updating new ChatRoomId appended with OldChatRoomIds to Receiver
              FirebaseFirestore.getInstance().collection(DEFAULT_USERS_COLLECTION_NAME)
                .document(receiverMobileNumber)
                .update(mapOf(User.chatRoomIdsKey to oldSenderRoomIds))
                .addOnFailureListener { onFailed.invoke(it.message ?: "Failed to update new Chat Room Id to Receiver") }
                .addOnSuccessListener {
                  onSuccess.invoke()
                }
            })
          }
    })
  }

  /*---------------------------------------CHAT SCREEN METHODS ENDS------------------------------------------------------------------*/

}