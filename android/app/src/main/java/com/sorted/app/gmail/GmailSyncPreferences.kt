package com.sorted.app.gmail

import android.content.Context

class GmailSyncPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)

    fun isAutoSyncEnabled(): Boolean {
        return prefs.getBoolean(KeyAutoSyncEnabled, false)
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KeyAutoSyncEnabled, enabled)
            .apply()
    }

    fun markSyncSuccess(summary: GmailImportSummary) {
        prefs.edit()
            .putBoolean(KeyAutoSyncEnabled, true)
            .putLong(KeyLastSyncAt, System.currentTimeMillis())
            .putInt(KeyLastImported, summary.importedTransactions)
            .putInt(KeyLastDetected, summary.transactionsDetected)
            .putInt(KeyLastDuplicates, summary.skippedDuplicates)
            .putInt(KeyLastFxUpdated, summary.fxRatesUpdated)
            .remove(KeyLastError)
            .apply()
    }

    fun markNeedsManualAuth() {
        prefs.edit()
            .putString(KeyLastError, "Manual Gmail reconnect needed")
            .apply()
    }

    fun markSyncError(message: String) {
        if (message.isTransientCancellation()) return
        prefs.edit()
            .putString(KeyLastError, message.take(160))
            .apply()
    }

    fun clearTransientErrors() {
        val lastError = prefs.getString(KeyLastError, null) ?: return
        if (lastError.isTransientCancellation()) {
            prefs.edit()
                .remove(KeyLastError)
                .apply()
        }
    }

    fun statusLabel(): String {
        val lastError = prefs.getString(KeyLastError, null)
        if (!lastError.isNullOrBlank()) return "Auto sync paused: $lastError"

        val lastSyncAt = prefs.getLong(KeyLastSyncAt, 0L)
        if (lastSyncAt <= 0L) {
            return "Auto sync: every $AutoSyncIntervalHours hours when internet is available"
        }

        val imported = prefs.getInt(KeyLastImported, 0)
        val detected = prefs.getInt(KeyLastDetected, 0)
        val duplicates = prefs.getInt(KeyLastDuplicates, 0)
        val fxUpdated = prefs.getInt(KeyLastFxUpdated, 0)
        val fxLabel = if (fxUpdated > 0) ", FX updated $fxUpdated" else ""
        return "Auto sync: every $AutoSyncIntervalHours hours. Last: $imported/$detected imported, $duplicates matched SMS$fxLabel."
    }

    companion object {
        const val AutoSyncIntervalHours = 6L
        private const val PrefsName = "gmail_sync"
        private const val KeyAutoSyncEnabled = "auto_sync_enabled"
        private const val KeyLastSyncAt = "last_sync_at"
        private const val KeyLastImported = "last_imported"
        private const val KeyLastDetected = "last_detected"
        private const val KeyLastDuplicates = "last_duplicates"
        private const val KeyLastFxUpdated = "last_fx_updated"
        private const val KeyLastError = "last_error"
    }
}

private fun String.isTransientCancellation(): Boolean {
    return contains("rememberCoroutineScope left", ignoreCase = true) ||
        contains("job was cancelled", ignoreCase = true)
}
