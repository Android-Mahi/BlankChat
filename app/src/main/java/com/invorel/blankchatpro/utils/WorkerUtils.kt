package com.invorel.blankchatpro.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType.CONNECTED
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequest.Builder
import androidx.work.WorkContinuation
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.invorel.blankchatpro.workers.CreateChatRoomWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object WorkerUtils {

  fun getCreateChatRoomWorkerTag(senderNumber: String, receiverNumber: String) =
    CREATE_CHAT_ROOM.plus(senderNumber).plus(WORKER_TAG_SEPARATOR).plus(receiverNumber)

  fun getAddMessageToChatRoomWorkerTag(roomId: String, messageId: String) =
    ADD_MESSAGE.plus(roomId).plus(
      WORKER_TAG_SEPARATOR
    ).plus(messageId)

  private const val CREATE_CHAT_ROOM = "CREATE_CHAT_ROOM_WORKER"
  private const val ADD_MESSAGE = "ADD_MESSAGE"
  private const val WORKER_TAG_SEPARATOR = "($)"

  private var createChatWorkerContinuation: WorkContinuation? = null

  @SuppressLint("EnqueueWork") fun enqueueOneTimeWork(
    worker: Class<out CoroutineWorker>,
    context: Context,
    input: Data,
    tag: String,
  ) {
    val oneTimeWorkRequest = Builder(worker)
      .setInputData(input)
      .addTag(tag)
      .setBackoffCriteria(
        backoffPolicy = BackoffPolicy.LINEAR,
        backoffDelay = 10L,
        timeUnit = TimeUnit.SECONDS
      )
      .setConstraints(Constraints.Builder().setRequiredNetworkType(CONNECTED).build())
      .build()
    WorkManager.getInstance(context).enqueue(oneTimeWorkRequest)

    if (worker.simpleName == CreateChatRoomWorker::class.simpleName) {
      createChatWorkerContinuation = WorkManager.getInstance(context).beginWith(oneTimeWorkRequest)
    }
  }

  fun enqueueOneTimeWorkWithParentJob(
    childWorkerRequest: OneTimeWorkRequest,
  ) {
    if (createChatWorkerContinuation == null) {
      Log.d(
        "WorkerUtils",
        "Got createChatWorkerContinuation as null while enqueueing the child job with parent job"
      )
    }
    createChatWorkerContinuation!!.then(childWorkerRequest).enqueue()
  }

  fun createOneTimeRequest(worker: Class<out CoroutineWorker>, input: Data, tag: String) =
    Builder(worker)
      .setInputData(input)
      .addTag(tag)
      .setBackoffCriteria(
        backoffPolicy = BackoffPolicy.LINEAR,
        backoffDelay = 10L,
        timeUnit = TimeUnit.SECONDS
      )
      .setConstraints(Constraints.Builder().setRequiredNetworkType(CONNECTED).build())
      .build()

  fun checkIfCreateChatWorkerEnqueued(context: Context, tag: String): Boolean {
    val workInfo =
      WorkManager.getInstance(context).getWorkInfosByTag(tag).get().ifEmpty { return false }[0]
    return workInfo.state == WorkInfo.State.ENQUEUED
  }

  fun listenResultOfWorker(
    tag: String,
    scope: CoroutineScope,
    context: Context,
    owner: LifecycleOwner,
    onResultFetched: (Data) -> Unit,
  ) {
    scope.launch(Dispatchers.Main) {
      WorkManager.getInstance(context).getWorkInfosByTagLiveData(tag).observe(owner) { workInfo ->
        onResultFetched.invoke(workInfo[0].outputData)
      }
    }
  }
}