package tn.poslik.caisse.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import tn.poslik.caisse.CaisseApp
import java.util.concurrent.TimeUnit

/** WorkManager : persiste la demande de synchro, attend le réseau, relance avec backoff. */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val syncer = (applicationContext as CaisseApp).container.salesSyncer
        return when (val result = syncer.syncAll()) {
            is SalesSyncer.Result.Success -> {
                Log.i(TAG, "Synchro OK : ${result.pushed} vente(s) envoyée(s)")
                Result.success()
            }
            is SalesSyncer.Result.Failure -> {
                Log.w(TAG, "Synchro interrompue après ${result.pushed} vente(s)", result.cause)
                Result.retry()
            }
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
        private const val WORK_NAME = "sales-sync"

        fun request(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()
            // APPEND_OR_REPLACE : une vente arrivée pendant une synchro déclenche un passage de plus.
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }
    }
}
