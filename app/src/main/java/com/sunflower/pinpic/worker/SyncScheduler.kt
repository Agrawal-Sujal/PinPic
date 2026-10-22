package com.sunflower.pinpic.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager by lazy { WorkManager.getInstance(context) }

    /**
     * Observable flow indicating whether the sync worker is actively queued or running.
     */
    val isSyncingFlow: Flow<Boolean>
        get() = workManager
            .getWorkInfosForUniqueWorkFlow(SyncWorker.UNIQUE_WORK_NAME)
            .map { list ->
                list.any { !it.state.isFinished }
            }

    /**
     * Enqueues an expedited one-time sync worker job to process pending operations immediately.
     */
    fun scheduleSync() {
        Log.d(TAG, "scheduleSync called - scheduling immediate sync work")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            SyncWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    companion object {
        private const val TAG = "SyncScheduler"
    }
}
