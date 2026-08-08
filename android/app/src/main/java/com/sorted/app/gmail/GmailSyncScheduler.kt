package com.sorted.app.gmail

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object GmailSyncScheduler {
    private const val WorkName = "gmail_auto_sync"

    fun schedule(context: Context) {
        GmailSyncPreferences(context).setAutoSyncEnabled(true)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<GmailAutoSyncWorker>(
            GmailSyncPreferences.AutoSyncIntervalHours,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(WorkName, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
