package com.sorted.app.gmail

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GmailAutoSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = GmailSyncPreferences(applicationContext)
        if (!prefs.isAutoSyncEnabled()) return@withContext Result.success()

        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(GmailImportPlan.RequiredScope)))
            .build()

        val authorizationResult = runCatching {
            Tasks.await(Identity.getAuthorizationClient(applicationContext).authorize(request))
        }.getOrElse { error ->
            prefs.markSyncError(error.message ?: error.javaClass.simpleName)
            return@withContext Result.retry()
        }

        val accessToken = authorizationResult.accessToken
        if (authorizationResult.hasResolution() || accessToken.isNullOrBlank()) {
            prefs.markNeedsManualAuth()
            return@withContext Result.success()
        }

        runCatching {
            GmailImporter(applicationContext).importLatest(accessToken)
        }.onSuccess { summary ->
            prefs.markSyncSuccess(summary)
        }.onFailure { error ->
            prefs.markSyncError(error.message ?: error.javaClass.simpleName)
            return@withContext Result.retry()
        }

        Result.success()
    }
}
