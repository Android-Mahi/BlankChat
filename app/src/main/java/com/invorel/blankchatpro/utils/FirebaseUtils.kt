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
import com.invorel.blankchatpro.constants.DEFAULT_CHATROOM_COLLECTION_NAME
import com.invorel.blankchatpro.constants.DEFAULT_CHAT_ROOM_SEPARATOR
import com.invorel.blankchatpro.constants.DEFAULT_COUNTRY_CODE
import com.invorel.blankchatpro.constants.DEFAULT_DIRECTORY_NAME_FOR_PROFILE_IN_FIREBASE
import com.invorel.blankchatpro.constants.DEFAULT_OTP_RESEND_TIME_SECONDS
import com.invorel.blankchatpro.constants.DEFAULT_STATUS_COLLECTION_NAME
import com.invorel.blankchatpro.constants.DEFAULT_USERS_COLLECTION_NAME
import com.invorel.blankchatpro.constants.DEFAULT_USER_NAME
import com.invorel.blankchatpro.constants.DEFAULT__MESSAGE_COLLECTION_NAME
import com.invorel.blankchatpro.extensions.isNumberTypeChatRoom
import com.invorel.blankchatpro.online.fb_collections.Message
import com.invorel.blankchatpro.online.fb_collections.Status
import com.invorel.blankchatpro.online.fb_collections.User
import com.invorel.blankchatpro.utils.FirebaseUtils.FireStoreResult.Success
import com.invorel.blankchatpro.utils.FirebaseUtils.ReceiverExistStatus.RECEIVER_EXISTS_WITHOUT_LOGIN
import com.invorel.blankchatpro.utils.FirebaseUtils.ReceiverExistStatus.RECEIVER_EXISTS_WITH_LOGIN
import com.invorel.blankchatpro.utils.FirebaseUtils.ReceiverExistStatus.RECEIVER_NOT_EXISTS
import com.invorel.blankchatpro.viewModels.ChatReceiverDetails
import com.invorel.blankchatpro.viewModels.HomeChatUIModel
import com.invorel.blankchatpro.viewModels.LatestHomeChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit.SECONDS

object FirebaseUtils {

  sealed class ReceiverExistStatus {
    object RECEIVER_NOT_EXISTS : ReceiverExistStatus()
    object RECEIVER_EXISTS_WITHOUT_LOGIN : ReceiverExistStatus()
    data class RECEIVER_EXISTS_WITH_LOGIN(val userId: String) : ReceiverExistStatus()
  }

  sealed class FireStoreResult<out T> {
    data class Success<out T>(val data: T) : FireStoreResult<T>()
    data class Error(val errorMessage: String) : FireStoreResult<Nothing>()
  }

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

  fun updateChosenProfilePhotoInFbStorage(
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
      updateChosenProfilePhotoInFbStorage(
        imageUri = imageUri,
        onProfileImageUploaded = onProfileImageUploaded,
        onProfileImageUploadFailed = onProfileImageUploadFailed
      )
    }, onImageDeleted = {
      // we can upload this new image
      updateChosenProfilePhotoInFbStorage(
        imageUri = imageUri,
        onProfileImageUploaded = onProfileImageUploaded,
        onProfileImageUploadFailed = onProfileImageUploadFailed
      )
    }, onError = {
      onProfileImageUploadFailed.invoke(it)
    })
  }

  private fun updateChosenProfilePhotoInFbStorage(
    imageUri: Uri,
    onProfileImageUploaded: (String) -> Unit,
    onProfileImageUploadFailed: (String) -> Unit,
  ) {
    currentUserProfileImageStorageRef.putFile(imageUri).addOnSuccessListener {
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
    currentUserProfileImageStorageRef.list(1).addOnSuccessListener { resultList ->
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
    currentUserProfileImageStorageRef.delete().addOnSuccessListener {
      onExistingImageDeleted.invoke()
    }.addOnFailureListener {
      onExistingImageDeleteFailed.invoke(it.message ?: "Delete Failed")
    }
  }

  fun getDownloadUrlOfUploadedImage(
    onDownloadUrlFetched: (String) -> Unit,
    onDownloadUrlFetchFailed: (String) -> Unit,
  ) {
    currentUserProfileImageStorageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
      onDownloadUrlFetched.invoke(downloadUrl.toString())
    }.addOnFailureListener { e ->
      //onDownloadUrlFetchFailed.invoke(e.message ?: "Unable to get Download Url")
      onDownloadUrlFetchFailed.invoke("update your nice photo")
    }
  }

  private val currentUserProfileImageStorageRef: StorageReference
    get() = FirebaseStorage.getInstance().reference.child(
      DEFAULT_DIRECTORY_NAME_FOR_PROFILE_IN_FIREBASE
    ).child(getDefaultSenderProfileImageName())

  private fun getDefaultSenderProfileImageName(): String {
    with(currentUser!!) {
      return uid.plus(".jpg")
    }
  }

  private fun getDefaultReceiverProfileImageName(mobileNumber: String): String {
    return mobileNumber.plus(".jpg")
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
    onFailed: (String) -> Unit,
  ) {
    FirebaseFirestore.getInstance()
      .collection(DEFAULT_USERS_COLLECTION_NAME)
      .document(mobileNumber)
      .get().addOnSuccessListener {
        onUserExistStatusFetched.invoke(it.exists())
      }
      .addOnFailureListener {
        onFailed.invoke(it.message ?: "Failed to get Existing User")
      }
  }

  fun saveUserDetailsInFirebase(
    user: User,
    onUserDetailsSaved: () -> Unit,
    onUserDetailsFailedToSave: (String) -> Unit,
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
    onFailed: (String) -> Unit,
  ) {

    FirebaseFirestore.getInstance().collection(DEFAULT_USERS_COLLECTION_NAME)
      .document(mobileNumber)
      .update(
        mapOf(
          User.userIdKey to userId,
          User.fcmTokenKey to fcmToken,
        )
      )
      .addOnSuccessListener { onUserDetailsUpdated.invoke() }
      .addOnFailureListener {
        onFailed.invoke(
          it.message ?: "Failed to Update Login details of existing User"
        )
      }
  }

  /*---------------------------------------LOGIN SCREEN METHODS ENDS------------------------------------------------------------------*/

  /*---------------------------------------PROFILE SCREEN METHODS STARTS------------------------------------------------------------------*/

  fun updateUserDetailsInFirebase(
    mobileNumber: String,
    name: String,
    about: String,
    gender: Int,
    profilePhoto: String,
    onUserDetailsUpdated: () -> Unit,
    onFailed: (String) -> Unit,
  ) {
    FirebaseFirestore.getInstance().collection(DEFAULT_USERS_COLLECTION_NAME)
      .document(mobileNumber)
      .update(
        mapOf(
          User.nameKey to name,
          User.aboutKey to about,
          User.profilePhotoKey to profilePhoto,
          User.genderKey to gender,
          User.lastProfileUpdatedAt to System.currentTimeMillis()
        )
      )
      .addOnSuccessListener { onUserDetailsUpdated.invoke() }
      .addOnFailureListener {
        onFailed.invoke(
          it.message ?: "Failed to Update Profile details of existing User"
        )
      }
  }

  /*---------------------------------------PROFILE SCREEN METHODS ENDS------------------------------------------------------------------*/

  /*---------------------------------------HOME SCREEN METHODS STARTS------------------------------------------------------------------*/

  fun updateUserStatus(
    isOnline: Boolean,
    onFailed: (String) -> Unit,
    onUserStatusUpdated: () -> Unit,
  ) {

    if (currentUser == null) {
      onFailed.invoke("Current FB User is Null")
      return
    }

    FirebaseFirestore.getInstance().collection(DEFAULT_STATUS_COLLECTION_NAME)
      .document(currentUser!!.uid)
      .set(
        mapOf(
          User.isOnlineKey to isOnline
        )
      )
      .addOnFailureListener {
        onFailed.invoke(
          it.message ?: " Failed to Update Status of the User"
        )
      }
      .addOnSuccessListener {
        onUserStatusUpdated.invoke()
      }
  }

  suspend fun getHomeChatListForChatRoomIds(
    scope: CoroutineScope,
    chatRoomIds: List<String>,
  ): FireStoreResult<List<HomeChatUIModel>> {

    if (chatRoomIds.isEmpty()) {
      return FireStoreResult.Error("ChatRoomIds got as empty while getting Home ChatList")
    }

    val homeChatList = chatRoomIds.map { roomId ->

      scope.async {

        val latestMessageInChat = getLatestMessageOfTheChatRoom(chatRoomId = roomId)

        if (latestMessageInChat is FireStoreResult.Error) {
          FireStoreResult.Error(latestMessageInChat.errorMessage)
          return@async null
        }

        val getRoomCreatedAtResult = getRoomCreatedAtValueOfTheChatRoom(roomId)

        if (getRoomCreatedAtResult is FireStoreResult.Error) {
          FireStoreResult.Error(getRoomCreatedAtResult.errorMessage)
          return@async null
        }

        val receiverNumberOfChat = getReceiverNumberOfChatRoom(chatRoomId = roomId)

        if (receiverNumberOfChat is FireStoreResult.Error) {
          FireStoreResult.Error(receiverNumberOfChat.errorMessage)
          return@async null
        }

        val receiverDetails =
          getReceiverDetailsOfChat((receiverNumberOfChat as Success).data)

        if ((receiverDetails as Success).data.userId.isEmpty()) {
          //Receiver Not Logged In we can set the status to offline
          HomeChatUIModel(
            roomId = roomId,
            receiverDetails = receiverDetails.data.copy(isReceiverOnline = false),
            lastMessageInChatRoom = (latestMessageInChat as Success).data,
            roomCreatedAt = (getRoomCreatedAtResult as Success).data
          )
        } else {
          val isReceiverOnline = getReceiverOnlineStatus(userId = receiverDetails.data.userId)

          HomeChatUIModel(
            roomId = roomId,
            receiverDetails = receiverDetails.data.copy(isReceiverOnline = (isReceiverOnline as Success).data),
            lastMessageInChatRoom = (latestMessageInChat as Success).data,
            roomCreatedAt = (getRoomCreatedAtResult as Success).data
          )
        }
      }
    }.awaitAll().filterNotNull()

    return Success(homeChatList)
  }

  private suspend fun getLatestMessageOfTheChatRoom(
    chatRoomId: String,
  ): FireStoreResult<LatestHomeChatMessage> {

    return try {
      val messagesSnap = FirebaseFirestore.getInstance()
        .collection(DEFAULT_CHATROOM_COLLECTION_NAME)
        .document(chatRoomId)
        .get().await()

      val latestMessage = messagesSnap.data as HashMap<String, HashMap<String, Any>>
      val latestMessageValueMap = latestMessage[Message.latestMessageKey]

      val message = Message(
        id = (latestMessageValueMap?.get(Message.idKey) as Long).toInt(),
        message = latestMessageValueMap[Message.messageKey] as String,
        senderId = latestMessageValueMap[Message.senderIdKey] as String,
        receiverId = latestMessageValueMap[Message.receiverIdKey] as String,
        isSentByMessageMode = latestMessageValueMap[Message.isSentByMessageModeKey] as Boolean,
        status = (latestMessageValueMap[Message.statusKey] as Long).toInt(),
        sentTime = latestMessageValueMap[Message.sentTimeKey] as Long,
        receivedTime = latestMessageValueMap[Message.receivedTimeKey] as Long,
      )

      //val message = messagesSnap.toObject(Message::class.java) ?: return FireStoreResult.Error("Data parsed as null while getting latest Message of the chat Room")

      val latestHomeChatMessage = LatestHomeChatMessage(
        senderId = message.senderId,
        receiverId = message.receiverId,
        message = message.message,
        status = message.status,
        receivedTime = message.receivedTime,
        sentTime = message.sentTime,
        isSentInMessageMode = message.isSentByMessageMode,
        messageId = message.id
      )
      Success(latestHomeChatMessage)
    } catch (e: Exception) {
      FireStoreResult.Error(e.message ?: "Failed to get Latest Message of the Chat Room")
    }
  }

  private suspend fun getRoomCreatedAtValueOfTheChatRoom(
    chatRoomId: String,
  ): FireStoreResult<Long> {
    return try {
      val chatRoomSnap = FirebaseFirestore.getInstance()
        .collection(DEFAULT_CHATROOM_COLLECTION_NAME)
        .document(chatRoomId)
        .get().await()

      val roomCreatedAt = chatRoomSnap.get(Message.roomCreatedAtKey) as Long

      if (roomCreatedAt == -1L) {
        FireStoreResult.Error("Got roomCreatedAt value as -1")
      } else {
        Success(roomCreatedAt)
      }
    } catch (e: Exception) {
      FireStoreResult.Error(e.message ?: "Failed to get Latest Message of the Chat Room")
    }
  }

  private suspend fun getReceiverNumberOfChatRoom(
    chatRoomId: String,
  ): FireStoreResult<String> {

    return try {
      val dataSnap = FirebaseFirestore.getInstance()
        .collection(DEFAULT_CHATROOM_COLLECTION_NAME)
        .document(chatRoomId)
        .get().await()

      val data = dataSnap.data ?: return FireStoreResult.Error("Data is from ChatRoomId Document")

      val participantDetails = data[Message.participantsKey] as String

      val participantList = participantDetails.split(DEFAULT_CHAT_ROOM_SEPARATOR).toMutableList()

      if (currentUser == null) {
        return FireStoreResult.Error("Current FB User is Null")
      }

      if (currentUser!!.phoneNumber == null) {
        return FireStoreResult.Error("Current FB User is Null")
      }

      val currentUserNumber = currentUser!!.phoneNumber!!

      val currentUserIndexInParticipantList = participantList.indexOf(currentUserNumber)

      if (currentUserIndexInParticipantList == -1) {
        return FireStoreResult.Error("Unable to find current user number in participant list")
      }

      participantList.removeAt(currentUserIndexInParticipantList)

      val receiverNumber = participantList.first()

      if (receiverNumber == currentUserNumber) {
        return FireStoreResult.Error("Sender And Receiver Number Same after removing operations in list")
      }

      return Success(receiverNumber)
    } catch (e: Exception) {
      FireStoreResult.Error(e.message ?: "Failed to get Participants Details of ChatRoomId")
    }
  }

  private suspend fun getReceiverDetailsOfChat(
    mobileNumber: String,
  ): FireStoreResult<ChatReceiverDetails> {

    return try {
      val snap = FirebaseFirestore.getInstance()
        .collection(DEFAULT_USERS_COLLECTION_NAME)
        .document(mobileNumber)
        .get().await()

      val receiver = snap.toObject(User::class.java)
        ?: return FireStoreResult.Error("User parsed as null while getting receiver details")

      Success(
        ChatReceiverDetails(
          userId = receiver.userId,
          fcmToken = receiver.fcmToken,
          number = receiver.mobileNumber,
          name = receiver.name.ifEmpty { DEFAULT_USER_NAME },
          photo = receiver.photo,
        )
      )
    } catch (e: Exception) {
      FireStoreResult.Error(e.message ?: "Failed to get Receiver Details of the chat")
    }
  }

  private suspend fun getReceiverOnlineStatus(
    userId: String,
  ): FireStoreResult<Boolean> {

    return try {

      val snap = FirebaseFirestore.getInstance().collection(DEFAULT_STATUS_COLLECTION_NAME)
        .document(userId)
        .get().await()

      val data = snap.data
        ?: return FireStoreResult.Error("Data parsed as null while getting receiver details")

      val isOnline = data[Status.isOnlineKey] as Boolean
      Success(isOnline)
    } catch (e: Exception) {
      FireStoreResult.Error(e.message ?: "Failed to get Receiver Online Status of the user")
    }
  }

  suspend fun getLastRoomUpdatedAtValueInUser(phoneNo: String): FireStoreResult<Long> {
    val userDoc =
      FirebaseFirestore.getInstance().collection(DEFAULT_USERS_COLLECTION_NAME).document(phoneNo)
        .get().await()

    val user = userDoc.toObject(User::class.java)
      ?: return FireStoreResult.Error("Got User Document as null phoneNo: $phoneNo in FB")

    return if (user.lastRoomCreatedAt == -1L) {
      FireStoreResult.Error("Got lastRoomCreatedAt as -1 in User phoneNo: $phoneNo in FB")
    } else {
      Success(user.lastRoomCreatedAt)
    }
  }

  suspend fun listenChatRoomUpdates(
    scope: CoroutineScope,
    onFailed: (String) -> Unit,
    onSuccess: (List<HomeChatUIModel>) -> Unit,
  ) {

    if (currentUser == null) {
      onFailed.invoke("Got Current User as null")
      return
    }

    val currentUserNo = currentUser!!.phoneNumber.orEmpty()

    if (currentUserNo.isEmpty()) {
      onFailed.invoke("Got currentUserNo as Empty")
      return
    }

    FirebaseFirestore.getInstance()
      .collection(DEFAULT_CHATROOM_COLLECTION_NAME)
      .addSnapshotListener { updatedValues, error ->
        if (error != null) {
          onFailed.invoke(error.message ?: "Error Got while listening ChatRoom Updates")
          return@addSnapshotListener
        }

        if (updatedValues == null) {
          onFailed.invoke("Got updatedValues as null")
          return@addSnapshotListener
        }

        scope.launch {

          val updatedChatsDocIdsForCurrentUser =
            updatedValues.documents.map { it.id }.filter { roomId ->
              if(roomId.isNumberTypeChatRoom()) {
                val roomParticipants = roomId.split(DEFAULT_CHAT_ROOM_SEPARATOR)
                roomParticipants.contains(currentUserNo)
              } else {
                val participantsResult = scope.async { getParticipantsDetailsOfChatRoom(roomId) }.await()

                if (participantsResult is FireStoreResult.Error) {
                  onFailed.invoke("Got Participants as empty")
                  false
                } else {
                  val roomParticipants = (participantsResult as Success).data.split(DEFAULT_CHAT_ROOM_SEPARATOR)
                  roomParticipants.contains(currentUserNo)
                }
              }
            }.map { roomId ->

              val latestMessageInChat =
                scope.async { getLatestMessageOfTheChatRoom(chatRoomId = roomId) }.await()

              if (latestMessageInChat is FireStoreResult.Error) {
                onFailed.invoke(latestMessageInChat.errorMessage)
                return@map null
              }

              val getRoomCreatedAtResult =
                scope.async { getRoomCreatedAtValueOfTheChatRoom(roomId) }.await()

              if (getRoomCreatedAtResult is FireStoreResult.Error) {
                onFailed.invoke(getRoomCreatedAtResult.errorMessage)
                return@map null
              }

              val receiverNumberOfChat =
                scope.async { getReceiverNumberOfChatRoom(chatRoomId = roomId) }.await()

              if (receiverNumberOfChat is FireStoreResult.Error) {
                onFailed.invoke(receiverNumberOfChat.errorMessage)
                return@map null
              }

              val receiverDetails =
                scope.async { getReceiverDetailsOfChat((receiverNumberOfChat as Success).data) }
                  .await()

              if ((receiverDetails as Success).data.userId.isEmpty()) {
                //Receiver Not Logged In we can set the status to offline
                HomeChatUIModel(
                  roomId = roomId,
                  receiverDetails = receiverDetails.data.copy(isReceiverOnline = false),
                  lastMessageInChatRoom = (latestMessageInChat as Success).data,
                  roomCreatedAt = (getRoomCreatedAtResult as Success).data
                )
              } else {
                val isReceiverOnline =
                  scope.async { getReceiverOnlineStatus(userId = receiverDetails.data.userId) }
                    .await()

                HomeChatUIModel(
                  roomId = roomId,
                  receiverDetails = receiverDetails.data.copy(isReceiverOnline = (isReceiverOnline as Success).data),
                  lastMessageInChatRoom = (latestMessageInChat as Success).data,
                  roomCreatedAt = (getRoomCreatedAtResult as Success).data
                )
              }

            }.filterNotNull()

          onSuccess.invoke(updatedChatsDocIdsForCurrentUser)

        }

      }
  }

  private suspend fun getParticipantsDetailsOfChatRoom(roomId: String): FireStoreResult<String> {

    return try {
      val getChatRoomDocResult =
        FirebaseFirestore.getInstance().collection(DEFAULT_CHATROOM_COLLECTION_NAME)
          .document(roomId)
          .get().await()

      val chatRoomResult = getChatRoomDocResult.data
        ?: return FireStoreResult.Error("Got ChatRoomResult as null")

      val participants = chatRoomResult[Message.participantsKey] as? String

      if (participants.isNullOrEmpty()) {
        return FireStoreResult.Error("Got Participants as empty")
      }

      Success(participants)
    } catch (e: FirebaseException) {
      FireStoreResult.Error(e.message ?: "Got exception while getting participants details")
    }
  }

  suspend fun getChatRoomIdsForCurrentUser(): FireStoreResult<List<String>> {
    return try {
      if (currentUser == null) {
        FireStoreResult.Error("Got Current User as null")
      } else if (currentUser!!.phoneNumber.isNullOrEmpty()) {
        FireStoreResult.Error("Got Current User phone Number as null or Empty")
      } else {
        val userDoc = FirebaseFirestore.getInstance().collection(DEFAULT_USERS_COLLECTION_NAME)
          .document(currentUser!!.phoneNumber!!)
          .get().await()

        val user = userDoc.toObject(User::class.java)

        if (user == null) {
          FireStoreResult.Error("User object parsed as null")
        } else {
          Success(user.chatRoomIds)
        }
      }
    } catch (e: FirebaseException) {
      FireStoreResult.Error("Got exception while Getting ChatRoomIds for user")
    }
  }

  /*---------------------------------------HOME SCREEN METHODS ENDS------------------------------------------------------------------*/

  /*---------------------------------------CHAT SCREEN METHODS STARTS------------------------------------------------------------------*/

  suspend fun checkReceiverDetailsInFireBase(
    mobileNumber: String,
  ): FireStoreResult<ReceiverExistStatus> {

    return try {

      val receiverSnap = FirebaseFirestore.getInstance().collection(DEFAULT_USERS_COLLECTION_NAME)
        .document(mobileNumber)
        .get().await()

      if (receiverSnap.exists()) {
        // Receiver Details Exists in DB
        val user = receiverSnap.toObject(User::class.java)
          ?: return FireStoreResult.Error("Receiver's user object parsed as null")

        if (user.userId.isNotEmpty()) {
          //Receiver logged In
          Success(RECEIVER_EXISTS_WITH_LOGIN(user.userId))
        } else {
          //Receiver Not logged In
          Success(RECEIVER_EXISTS_WITHOUT_LOGIN)
        }
      } else {
        // Receiver Details Not Present in DB
        Success(RECEIVER_NOT_EXISTS)
      }
    } catch (e: FirebaseException) {
      FireStoreResult.Error(
        e.message ?: "Unable to get Receiver Details from Firebase while creating initial chatRoom"
      )
    }
  }

  suspend fun checkIfReceiverExistsWithLoginInFireBase(
    mobileNumber: String,
  ): FireStoreResult<Pair<Boolean, String>> {
    return try {
      val receiverSnap = FirebaseFirestore.getInstance().collection(DEFAULT_USERS_COLLECTION_NAME)
        .document(mobileNumber)
        .get().await()

      if (receiverSnap.exists()) {
        // Receiver Details Exists in DB
        val user = receiverSnap.toObject(User::class.java)
          ?: return FireStoreResult.Error("Receiver's user object parsed as null")

        if (user.userId.isNotEmpty()) {
          //Receiver logged In
          Success(Pair(true, user.userId))
        } else {
          //Receiver Not logged In
          Success(Pair(false, ""))
        }
      } else {
        // Receiver Details Not Present in DB
        Success(Pair(false, ""))
      }
    } catch (e: FirebaseException) {
      FireStoreResult.Error(
        e.message ?: "Unable to get Receiver Details from Firebase while adding message to chatRoom"
      )
    }
  }

  sealed class ChatRoomExistStatus {
    data class Exist(val roomId: String) : ChatRoomExistStatus()
    object NotExist : ChatRoomExistStatus()
  }

  suspend fun checkIfNumberTypeChatRoomExistsOrNotInDb(
    senderTag: String,
    receiverTag: String,
  ): FireStoreResult<ChatRoomExistStatus> {

    return try {

      if (currentUser == null) {
        FireStoreResult.Error("Current FB User is Null")
      } else if (currentUser!!.phoneNumber == null) {
        FireStoreResult.Error("Current FB User PhoneNumber is Null")
      } else {

        //chat room may be initiated by user or receiver - chat Room id will be senderTag#receiverTag
        // we are checks roomId in both cases
        val oldChatRoomId1 = senderTag.plus(DEFAULT_CHAT_ROOM_SEPARATOR).plus(receiverTag)
        val oldChatRoomId2 = receiverTag.plus(DEFAULT_CHAT_ROOM_SEPARATOR).plus(senderTag)

        val resultSnap =
          FirebaseFirestore.getInstance().collection(DEFAULT_CHATROOM_COLLECTION_NAME)
            .document(oldChatRoomId1)
            .get().await()

        if (resultSnap.exists()) {
          Success(ChatRoomExistStatus.Exist(oldChatRoomId1))
        } else {
          val resultSnap2 =
            FirebaseFirestore.getInstance().collection(DEFAULT_CHATROOM_COLLECTION_NAME)
              .document(oldChatRoomId2)
              .get().await()

          if (resultSnap2.exists()) {
            Success(ChatRoomExistStatus.Exist(oldChatRoomId2))
          } else {
            Success(ChatRoomExistStatus.NotExist)
          }
        }
      }
    } catch (e: FirebaseException) {
      FireStoreResult.Error(e.message ?: "Failed to get existing Number Type Room Document 1 or 2")
    }
  }

  suspend fun changeExistingRoomTypeFromNumberToUserId(
    receiverId: String,
    existingRoomId: String,
  ): FireStoreResult<String> {

    if (currentUser == null) {
      return FireStoreResult.Error("Got Current User Null")
    }

    val senderId = currentUser!!.uid
    val newChatRoomId = senderId.plus(DEFAULT_CHAT_ROOM_SEPARATOR).plus(receiverId)

    return try {
      val resultSnap = FirebaseFirestore.getInstance().collection(DEFAULT_CHATROOM_COLLECTION_NAME)
        .document(existingRoomId)
        .get().await()

      val existingData = resultSnap.data
      if (existingData == null) {
        FireStoreResult.Error("Got Existing Room Data Null")
      } else {

        //Creates new UserId type Room
        val newChatRoomRef =
          FirebaseFirestore.getInstance().collection(DEFAULT_CHATROOM_COLLECTION_NAME)
            .document(newChatRoomId)

        FirebaseFirestore.getInstance().runTransaction {
          it.set(newChatRoomRef, existingData)
        }.await()

        val oldNumberChatRoomRef =
          FirebaseFirestore.getInstance().collection(DEFAULT_CHATROOM_COLLECTION_NAME)
            .document(existingRoomId)

        //Deleting Old Number Type Room
        FirebaseFirestore.getInstance().runTransaction {
          it.delete(oldNumberChatRoomRef)
        }.await()

        Success(newChatRoomId)
      }
    } catch (e: FirebaseException) {
      FireStoreResult.Error(e.message ?: "exception in changeExistingRoomTypeFromNumberToUserId")
    }
  }

  suspend fun createNotLoggedInReceiverDetailsInDB(
    mobileNumber: String,
    name: String,
    photo: String,
    chatRoomIds: List<String>,
  ): FireStoreResult<Unit> {

    val ref = FirebaseFirestore.getInstance().collection(DEFAULT_USERS_COLLECTION_NAME)
      .document(mobileNumber)

    val notLoggedInReceiver = User(
      mobileNumber = mobileNumber,
      userId = "",
      fcmToken = "",
      name = name,
      photo = photo,
      chatRoomIds = chatRoomIds
    )

    return try {
      FirebaseFirestore.getInstance().runTransaction {
        it.set(ref, notLoggedInReceiver)
      }.await()
      Success(Unit)
    } catch (e: FirebaseException) {
      FireStoreResult.Error(e.message ?: "Failed to Create not Logged In Receiver User Details")
    }
  }

  suspend fun updateChatRoomIdInSendUserDocument(
    chatRoomIds: List<String>,
  ): FireStoreResult<Unit> {
    return try {
      if (currentUser == null) {
        FireStoreResult.Error("Got Null for Fb Current User")
      } else if (currentUser!!.phoneNumber == null) {
        FireStoreResult.Error("Got Null for Fb Current User Phone No")
      } else {

        val ref = FirebaseFirestore.getInstance()
          .collection(DEFAULT_USERS_COLLECTION_NAME)
          .document(currentUser!!.phoneNumber!!)

        val data = mapOf(User.chatRoomIdsKey to chatRoomIds)

        FirebaseFirestore.getInstance().runTransaction {
          it.update(ref, data)
        }.await()

        Success(Unit)
      }
    } catch (e: FirebaseException) {
      FireStoreResult.Error(e.message ?: "Failed to update ChatRoomIds in Sender")
    }
  }

  suspend fun getExistingChatRoomIdsForUser(
    mobileNumber: String,
  ): FireStoreResult<List<String>> {
    return try {
      val resultSnap = FirebaseFirestore.getInstance().collection(DEFAULT_USERS_COLLECTION_NAME)
        .document(mobileNumber)
        .get().await()

      val user = resultSnap.toObject(User::class.java)
      if (user == null) {
        FireStoreResult.Error("User Parsed as null while checking Existing ChatRoomIds")
      } else {
        val existingChatRooms = user.chatRoomIds
        Success(existingChatRooms)
      }
    } catch (e: FirebaseException) {
      FireStoreResult.Error(e.message ?: "Failed to fetch existing chatRoomIds")
    }
  }

  suspend fun createChatRoomInFirebase(
    chatRoomId: String,
    message: Message,
  ): FireStoreResult<String> {
    return try {
      val messageRef = FirebaseFirestore
        .getInstance()
        .collection(DEFAULT_CHATROOM_COLLECTION_NAME)
        .document(chatRoomId)
        .collection(DEFAULT__MESSAGE_COLLECTION_NAME)
        .document(message.id.toString())

      FirebaseFirestore.getInstance().runTransaction {
        val data = mapOf(message.id.toString() to message)
        it.set(messageRef, data)
      }

      val createLastMessageResult = setLatestMessageInChatRoomDocument(
        chatRoomId = chatRoomId,
        message = message,
      )

      if (createLastMessageResult is FireStoreResult.Error) {
        FireStoreResult.Error(errorMessage = createLastMessageResult.errorMessage)
      } else {
        Success(chatRoomId)
      }
    } catch (e: FirebaseException) {
      FireStoreResult.Error(e.message ?: "Firebase Exception: While Creating ChatRoom")
    }
  }

  private suspend fun setLatestMessageInChatRoomDocument(
    chatRoomId: String,
    message: Message,
  ): FireStoreResult<Unit> {
    return try {
      val chatRoomRef = FirebaseFirestore
        .getInstance()
        .collection(DEFAULT_CHATROOM_COLLECTION_NAME)
        .document(chatRoomId)

      FirebaseFirestore.getInstance().runTransaction {
        val data = mapOf(Message.latestMessageKey to message)
        it.set(chatRoomRef, data)
      }.await()

      Success(Unit)
    } catch (e: FirebaseException) {
      FireStoreResult.Error(e.message ?: "Failed to update latest Message in ChatRoom Document")
    }
  }

  private suspend fun updateLatestMessageInChatRoomDocument(
    chatRoomId: String,
    message: Message,
  ): FireStoreResult<Unit> {

    val chatRoomRef = FirebaseFirestore
      .getInstance()
      .collection(DEFAULT_CHATROOM_COLLECTION_NAME)
      .document(chatRoomId)

    val data = mapOf(Message.latestMessageKey to message)

    return try {
      FirebaseFirestore.getInstance().runTransaction {
        it.update(chatRoomRef, data)
      }.await()
      Success(Unit)
    } catch (e: FirebaseException) {
      FireStoreResult.Error(e.message ?: "Failed to update latest Message in ChatRoom Document")
    }
  }

  suspend fun updateParticipantsDetailsInChatRoom(
    senderNumber: String,
    receiverNumber: String,
    chatRoomId: String,
  ): FireStoreResult<Unit> {

    val participantDetails = senderNumber.plus(DEFAULT_CHAT_ROOM_SEPARATOR).plus(receiverNumber)

    return try {
      FirebaseFirestore.getInstance().runTransaction {
        val ref = FirebaseFirestore.getInstance()
          .collection(DEFAULT_CHATROOM_COLLECTION_NAME)
          .document(chatRoomId)
        val data = mapOf(Message.participantsKey to participantDetails)
        it.update(ref, data)
      }.await()
      Success(Unit)
    } catch (e: FirebaseException) {
      FireStoreResult.Error(e.message ?: "Failed to update participant details in ChatRoom")
    }
  }

  suspend fun updateChatRoomIdsInSenderAndReceiverDetails(
    chatRoomId: String,
    senderNumber: String,
    receiverMobileNumber: String,
  ): FireStoreResult<Unit> {

    //Getting Old ChatRoom Ids for Sender
    val existingSenderChatRoomsResult = getExistingChatRoomIdsForUser(senderNumber)

    if (existingSenderChatRoomsResult is FireStoreResult.Error) {
      return FireStoreResult.Error(existingSenderChatRoomsResult.errorMessage)
    }

    val senderChatRooms = mutableListOf<String>()
    senderChatRooms.addAll((existingSenderChatRoomsResult as Success).data)
    senderChatRooms.add(chatRoomId)
    //Updating new ChatRoomId appended with OldChatRoomIds to Sender

    try {
      FirebaseFirestore.getInstance().runTransaction {
        val senderRef = FirebaseFirestore.getInstance().collection(DEFAULT_USERS_COLLECTION_NAME)
          .document(senderNumber)
        val data = mapOf(User.chatRoomIdsKey to senderChatRooms)
        it.update(senderRef, data)
      }.await()
    } catch (e: FirebaseException) {
      FireStoreResult.Error(e.message ?: "Failed to update new Chat Room Id to Sender")
    }

    //Getting Old ChatRoom Ids for Receiver
    val existReceiverChatRoomsResult = getExistingChatRoomIdsForUser(receiverMobileNumber)

    if (existReceiverChatRoomsResult is FireStoreResult.Error) {
      return FireStoreResult.Error(existReceiverChatRoomsResult.errorMessage)
    }

    val receiverChatRooms = mutableListOf<String>()
    receiverChatRooms.addAll((existReceiverChatRoomsResult as Success).data)
    receiverChatRooms.add(chatRoomId)

    return try {
      //Updating new ChatRoomId appended with OldChatRoomIds to Receiver

      FirebaseFirestore.getInstance().runTransaction {
        val receiverRef = FirebaseFirestore.getInstance().collection(DEFAULT_USERS_COLLECTION_NAME)
          .document(receiverMobileNumber)
        val data = mapOf(User.chatRoomIdsKey to receiverChatRooms)

        it.update(receiverRef, data)
      }.await()

      Success(Unit)
    } catch (e: FirebaseException) {
      FireStoreResult.Error(e.message ?: "Failed to update new Chat Room Id to Receiver")
    }
  }

  suspend fun getMessagesFromChatRoom(
    chatRoomId: String,
    onFailed: (String) -> Unit,
    onMessagesFetched: (List<Message>) -> Unit,
  ) {

    try {

      val messageSnap = FirebaseFirestore
        .getInstance()
        .collection(DEFAULT_CHATROOM_COLLECTION_NAME)
        .document(chatRoomId)
        .collection(DEFAULT__MESSAGE_COLLECTION_NAME)
        .get().await()

      val messages = messageSnap.documents.mapNotNull { document ->

        val messageMap = document.data as HashMap<String, HashMap<String, Any>>
        val messageId = document.id
        val messageValueMap = messageMap[messageId]

        val message = Message(
          id = (messageValueMap?.get(Message.idKey) as Long).toInt(),
          message = messageValueMap[Message.messageKey] as String,
          senderId = messageValueMap[Message.senderIdKey] as String,
          receiverId = messageValueMap[Message.receiverIdKey] as String,
          isSentByMessageMode = messageValueMap[Message.isSentByMessageModeKey] as Boolean,
          status = (messageValueMap[Message.statusKey] as Long).toInt(),
          sentTime = messageValueMap[Message.sentTimeKey] as Long,
          receivedTime = messageValueMap[Message.receivedTimeKey] as Long,
        )

        message
      }

      if (messages.isEmpty()) {
        onFailed.invoke("Messages list is empty for current chatRoom")
        return
      }

      onMessagesFetched.invoke(messages.sortedBy { it.id })
    } catch (e: Exception) {
      onFailed.invoke(e.message ?: "Failed to get Messages From ChatRoom")
    }
  }

  suspend fun updateMessageStatus(
    status: Int,
    roomId: String,
    messageId: Int,
  ): FireStoreResult<Unit> {

    val existingMessageRef = FirebaseFirestore
      .getInstance()
      .collection(DEFAULT_CHATROOM_COLLECTION_NAME)
      .document(roomId)
      .collection(DEFAULT__MESSAGE_COLLECTION_NAME)
      .document(messageId.toString())

    val existingMessageMapDoc = existingMessageRef.get().await()

    val existingMessageMap = existingMessageMapDoc.data as HashMap<String, HashMap<String, Any>>
    val messageValueMap = existingMessageMap[messageId.toString()]

    val updatedMessage = Message(
      id = (messageValueMap?.get(Message.idKey) as Long).toInt(),
      message = messageValueMap[Message.messageKey] as String,
      senderId = messageValueMap[Message.senderIdKey] as String,
      receiverId = messageValueMap[Message.receiverIdKey] as String,
      isSentByMessageMode = messageValueMap[Message.isSentByMessageModeKey] as Boolean,
      status = status,
      sentTime = messageValueMap[Message.sentTimeKey] as Long,
      receivedTime = messageValueMap[Message.receivedTimeKey] as Long,
    )

    return try {
      FirebaseFirestore.getInstance().runTransaction {
        it.set(existingMessageRef, updatedMessage)
      }.await()

      val updateLatestMessageResult = updateLatestMessageStatus(status = status, roomId = roomId)

      if (updateLatestMessageResult is FireStoreResult.Error) {
        FireStoreResult.Error(updateLatestMessageResult.errorMessage)
      } else {
        Success(Unit)
      }
    } catch (e: FirebaseException) {
      FireStoreResult.Error(e.message ?: "exception got while updating message staus")
    }
  }

  private suspend fun updateLatestMessageStatus(
    status: Int,
    roomId: String
  ): FireStoreResult<Unit> {

    val latestMessageRef =
      FirebaseFirestore.getInstance()
      .collection(DEFAULT_CHATROOM_COLLECTION_NAME)
      .document(roomId)

    val latestMessageMapDoc = latestMessageRef.get().await()

    val latestMessageMap = latestMessageMapDoc.data as HashMap<String, HashMap<String, Any>>
    val latestMessageValueMap = latestMessageMap[Message.latestMessageKey]

    val updatedLatestMessage = Message(
      id = (latestMessageValueMap?.get(Message.idKey) as Long).toInt(),
      message = latestMessageValueMap[Message.messageKey] as String,
      senderId = latestMessageValueMap[Message.senderIdKey] as String,
      receiverId = latestMessageValueMap[Message.receiverIdKey] as String,
      isSentByMessageMode = latestMessageValueMap[Message.isSentByMessageModeKey] as Boolean,
      status = status,
      sentTime = latestMessageValueMap[Message.sentTimeKey] as Long,
      receivedTime = latestMessageValueMap[Message.receivedTimeKey] as Long,
    )

    return try {
      FirebaseFirestore.getInstance().runTransaction {
        it.update(latestMessageRef, mapOf(Message.latestMessageKey to updatedLatestMessage))
      }.await()

      Success(Unit)
    } catch (e: FirebaseException) {
      FireStoreResult.Error(e.message ?: "exception got while updating latest message staus")
    }
  }

  suspend fun updateMessageInChatRoom(
    roomId: String,
    message: Message,
  ): FireStoreResult<Message> {
    val checkExistChatRoomResult = checkChatRoomExistsOrNotInDb(roomId = roomId)

    return if (checkExistChatRoomResult is FireStoreResult.Error) {
      FireStoreResult.Error(checkExistChatRoomResult.errorMessage)
    } else {

      when ((checkExistChatRoomResult as Success).data) {
        true -> {
          // ChatRoom Exists we can update the message

          val messageRef = FirebaseFirestore.getInstance()
            .collection(DEFAULT_CHATROOM_COLLECTION_NAME)
            .document(roomId)
            .collection(DEFAULT__MESSAGE_COLLECTION_NAME)
            .document(message.id.toString())

          val data = mapOf(message.id.toString() to message)

          try {
            FirebaseFirestore.getInstance().runTransaction {
              it.set(messageRef, data)
            }.await()

            val updateLatestMessageResult = updateLatestMessageInChatRoomDocument(
              chatRoomId = roomId,
              message = message
            )

            if (updateLatestMessageResult is FireStoreResult.Error) {
              FireStoreResult.Error(updateLatestMessageResult.errorMessage)
            } else {
              Success(message)
            }
          } catch (e: FirebaseException) {
            FireStoreResult.Error(e.message ?: "Failed to update Message in ChatRoom")
          }
        }

        false -> {
          //ChatRoom Not Exists
          FireStoreResult.Error("Provided ChatRoom not exist. can't able to send  thisMessage")
        }
      }
    }
  }

  private suspend fun checkChatRoomExistsOrNotInDb(
    roomId: String,
  ): FireStoreResult<Boolean> {
    return try {
      val resultSnap = FirebaseFirestore.getInstance()
        .collection(DEFAULT_CHATROOM_COLLECTION_NAME)
        .document(roomId)
        .get().await()

      Success(resultSnap.exists())
    } catch (e: FirebaseException) {
      FireStoreResult.Error(
        e.message ?: "Failed while checking existing chatRoom before send new Messsage"
      )
    }
  }

  fun listenMessagesForChatRoom(
    chatRoomId: String,
    onFailed: (String) -> Unit,
    onMessageUpdated: (List<Message>) -> Unit,
  ) {
    FirebaseFirestore.getInstance().collection(DEFAULT_CHATROOM_COLLECTION_NAME)
      .document(chatRoomId)
      .collection(DEFAULT__MESSAGE_COLLECTION_NAME)
      .addSnapshotListener { value, error ->
        if (error != null) {
          onFailed.invoke(error.message ?: "Failed to listen message in room Id:  $chatRoomId")
        } else {
          if (value == null) {
            onFailed.invoke("Got value null")
          } else if (value.documents.isEmpty()) {
            onFailed.invoke("Got Message Documents Empty")
          } else {
            val messages = value.documents.mapNotNull { it.toObject(Message::class.java) }
            if (messages.isEmpty()) {
              onFailed.invoke("Got Message Documents Empty after parsing")
            } else {
              onMessageUpdated.invoke(messages)
            }
          }
        }
      }
  }

  fun uploadReceiverPhotoInFireBaseWithByteArray(
    receiverNumber: String,
    photoData: ByteArray,
    onFailed: (String) -> Unit,
    onSuccess: (String) -> Unit,
  ) {
    val imageRef = FirebaseStorage.getInstance().reference.child(
      DEFAULT_DIRECTORY_NAME_FOR_PROFILE_IN_FIREBASE
    ).child(getDefaultReceiverProfileImageName(receiverNumber))

    imageRef.putBytes(photoData)
      .addOnSuccessListener {
        imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
          onSuccess.invoke(downloadUrl.toString())
        }.addOnFailureListener { e ->
          onFailed.invoke(e.message ?: "Unable to get Download Url of Receiver Local Photo")
        }
      }
      .addOnFailureListener {
        onFailed.invoke(it.message ?: "Failed to upload contact byteArray in Firebase")
      }
  }

  fun updateLastChatRoomUpdatedAtInProfile(
    currentTimeInMillis: Long,
  ): FireStoreResult<Unit> {
    return if (currentUser == null) {
      FireStoreResult.Error("Got Null for Fb Current User")
    } else if (currentUser!!.phoneNumber == null) {
      FireStoreResult.Error("Got Null for Fb Current User Phone No")
    } else {
      val profileRef = FirebaseFirestore.getInstance().collection(DEFAULT_USERS_COLLECTION_NAME)
        .document(currentUser!!.phoneNumber!!)

      val data = mapOf(User.lastRoomCreatedAtKey to currentTimeInMillis)

      try {
        FirebaseFirestore.getInstance().runTransaction {
          it.update(profileRef, data)
        }
        Success(Unit)
      } catch (e: FirebaseException) {
        FireStoreResult.Error(e.message ?: "Failed to update lastRoomCreatedAtKey in User")
      }
    }
  }

  fun updateLastChatRoomUpdatedAtInChatRoom(
    roomId: String,
    currentTimeInMillis: Long,
  ): FireStoreResult<Unit> {
    return if (currentUser == null) {
      FireStoreResult.Error("Got Null for Fb Current User")
    } else if (currentUser!!.phoneNumber == null) {
      FireStoreResult.Error("Got Null for Fb Current User Phone No")
    } else {
      val chatRoomRef = FirebaseFirestore.getInstance().collection(DEFAULT_CHATROOM_COLLECTION_NAME)
        .document(roomId)
      val data = mapOf(Message.roomCreatedAtKey to currentTimeInMillis)
      try {
        FirebaseFirestore.getInstance().runTransaction {
          it.update(chatRoomRef, data)
        }
        Success(Unit)
      } catch (e: FirebaseException) {
        FireStoreResult.Error(e.message ?: "Failed to update lastRoomCreatedAtKey in User")
      }
    }
  }

  //Todo remove below method if not needed
  fun getLastMessageIdInChatRoom(
    roomId: String,
    onFailed: (String) -> Unit,
    onLastMessageIdFetched: (Int) -> Unit,
  ) {
    FirebaseFirestore.getInstance()
      .collection(DEFAULT_CHATROOM_COLLECTION_NAME)
      .document(roomId)
      .get()
      .addOnFailureListener {
        onFailed.invoke(
          it.message ?: "Failed to get LastMessage Id In ChatRoom"
        )
      }
      .addOnSuccessListener {
        val message = it.toObject(Message::class.java)

        if (message == null) {
          onFailed.invoke("Message parsed null while getting LastMessage Id In ChatRoom")
          return@addOnSuccessListener
        }

        onLastMessageIdFetched.invoke(message.id)
      }
  }

  /*---------------------------------------CHAT SCREEN METHODS ENDS------------------------------------------------------------------*/
}