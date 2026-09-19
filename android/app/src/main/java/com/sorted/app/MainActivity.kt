package com.sorted.app

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.result.IntentSenderRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.sorted.app.engine.CategorySource
import com.sorted.app.engine.Direction
import com.sorted.app.engine.ParsedTransaction
import com.sorted.app.engine.PaymentMode
import com.sorted.app.engine.SmsParser
import com.sorted.app.engine.TransactionStatus
import com.sorted.app.engine.TransactionType
import com.sorted.app.data.CategoryRuleEntity
import com.sorted.app.data.ImportRecord
import com.sorted.app.data.ImportSource
import com.sorted.app.data.FxRateEntity
import com.sorted.app.data.FxRateKey
import com.sorted.app.data.FxRateRepository
import com.sorted.app.data.TransactionEntity
import com.sorted.app.data.TransactionCorrection
import com.sorted.app.data.TransactionRepository
import com.sorted.app.data.stableHash
import com.sorted.app.fx.FxRateImporter
import com.sorted.app.gmail.GmailImportPlan
import com.sorted.app.gmail.GmailImportSummary
import com.sorted.app.gmail.GmailImporter
import com.sorted.app.gmail.GmailSyncPreferences
import com.sorted.app.gmail.GmailSyncScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appPreferences = remember { SortedAppPreferences(applicationContext) }
            var themeMode by remember { mutableStateOf(appPreferences.themeMode()) }

            SortedTheme(themeMode = themeMode) {
                SortedHome(
                    themeMode = themeMode,
                    onThemeModeChange = { mode ->
                        appPreferences.setThemeMode(mode)
                        themeMode = mode
                    }
                )
            }
        }
    }
}

private data class TransactionUi(
    val id: Long?,
    val sourceHash: String,
    val merchant: String,
    val detail: String,
    val amount: String,
    val amountValue: Double,
    val currency: String,
    val inrAmountValue: Double?,
    val paymentMode: String,
    val miscCategory: String,
    val category: String,
    val direction: DirectionUi,
    val transactionType: TransactionType,
    val transactionDate: String?,
    val source: String,
    val categorySource: CategorySource,
    val confidence: Double
)

private data class FeedState(
    val transactions: List<TransactionUi>,
    val label: String,
    val needsSmsPermission: Boolean
)

private data class GmailUiState(
    val label: String = "Not connected",
    val isImporting: Boolean = false,
    val error: String? = null,
    val autoSyncLabel: String? = null
)

private data class GmailSetupInfo(
    val packageName: String,
    val signingSha1: String?
)

private const val LogTag = "Sorted"

private data class SmsInboxMessage(
    val id: Long?,
    val address: String?,
    val body: String,
    val receivedAtMillis: Long?,
    val receivedDate: String?
) {
    fun sourceHash(): String {
        return id?.let { "sms:$it" }
            ?: "sms:${body.stableHash()}:${receivedAtMillis ?: receivedDate.orEmpty()}"
    }
}

private data class MonthBreakdown(
    val monthKey: String?,
    val debitCount: Int,
    val spendCount: Int,
    val totalDebits: Double,
    val spends: Double,
    val transfers: Double,
    val investments: Double,
    val recurringInvestments: Double,
    val oneTimeInvestments: Double,
    val refunds: Double,
    val income: Double,
    val rewards: Double,
    val fxConverted: Double
)

private data class ExplainBucket(
    val title: String,
    val amount: Double,
    val count: Int,
    val description: String,
    val transactions: List<TransactionUi>
)

private data class SummaryGroup(
    val label: String,
    val count: Int,
    val total: Double,
    val currency: String,
    val category: String
)

private data class RecentDateGroup(
    val dateKey: String,
    val label: String,
    val transactions: List<TransactionUi>,
    val outflow: Double
)

private data class ReviewFilter(
    val label: String,
    val predicate: (TransactionUi) -> Boolean
)

private data class MonthStoryItem(
    val label: String,
    val value: String,
    val detail: String,
    val category: String
)

private data class RecurringCandidate(
    val merchant: String,
    val expectedAmount: Double,
    val count: Int,
    val lastSeenDate: String?,
    val category: String,
    val transactionType: TransactionType,
    val confidenceLabel: String
)

private data class SourceHealthRow(
    val source: String,
    val totalCount: Int,
    val spendCount: Int,
    val reviewCount: Int,
    val fxCount: Int,
    val totalAmount: Double
)

private data class HomeConstellationNode(
    val id: String,
    val label: String,
    val value: String,
    val detail: String,
    val x: Float,
    val y: Float,
    val orbitAngle: Float,
    val orbitRadius: Float,
    val visibleAtZoom: Float = 1f,
    val accent: Color,
    val outsideSpend: Boolean = false,
    val needsAttention: Boolean = false,
    val onClick: () -> Unit
)

private data class ManualTransactionDraft(
    val merchant: String,
    val amount: Double,
    val date: String,
    val category: String,
    val miscCategory: String,
    val paymentMode: PaymentMode,
    val transactionType: TransactionType,
    val direction: Direction
)

private data class ManualSaveState(
    val isSaving: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

private data class CorrectionSaveState(
    val isSaving: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

private data class TransactionCorrectionDraft(
    val transaction: TransactionUi,
    val merchant: String,
    val miscCategory: String,
    val category: String,
    val transactionType: TransactionType,
    val rememberRule: Boolean
)

private data class DrilldownState(
    val title: String,
    val kind: DrilldownKind,
    val group: SummaryGroup,
    val spendOnly: Boolean = false,
    val monthKey: String? = null
)

private enum class DrilldownKind {
    Merchant,
    Category
}

private enum class SortedTab(
    val label: String,
    val icon: SortedNavIcon
) {
    Home("Tape", SortedNavIcon.Home),
    Insights("Index", SortedNavIcon.Insights),
    Capture("Add", SortedNavIcon.Capture),
    RuleCenter("Stamps", SortedNavIcon.RuleCenter)
}

private enum class SortedNavIcon {
    Home,
    Insights,
    Capture,
    Sources,
    RuleCenter,
    Settings,
    Sync
}

private enum class SortedRoute {
    Loading,
    Main,
    Drilldown,
    SpendExplanation,
    SortInbox,
    RuleCenter,
    Settings
}

private val SortedTapeFontFamily = FontFamily(
    Font(R.font.roboto_mono_regular, FontWeight.Normal),
    Font(R.font.roboto_mono_medium, FontWeight.Medium),
    Font(R.font.roboto_mono_medium, FontWeight.SemiBold),
    Font(R.font.roboto_mono_medium, FontWeight.Bold)
)

private enum class SyncSource {
    Sms,
    Gmail,
    All
}

private enum class DirectionUi {
    Debit,
    Credit
}

private enum class AppThemeMode(
    val storageValue: String,
    val label: String,
    val description: String
) {
    System("system", "System", "Follow phone"),
    Dark("dark", "Dark", "Pitch black"),
    Light("light", "Light", "Funky light");

    companion object {
        fun from(value: String?): AppThemeMode {
            return entries.firstOrNull { it.storageValue == value } ?: System
        }
    }
}

private class SortedAppPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "sorted_app_preferences",
        Context.MODE_PRIVATE
    )

    fun themeMode(): AppThemeMode {
        return AppThemeMode.from(preferences.getString("theme_mode", AppThemeMode.System.storageValue))
    }

    fun setThemeMode(mode: AppThemeMode) {
        preferences.edit().putString("theme_mode", mode.storageValue).apply()
    }
}

private fun parsedSampleTransactions(): List<TransactionUi> {
    return SampleSmsSource.messages
        .map(SmsParser::parse)
        .filter { it.isTransaction }
        .map { it.toTransactionUi() }
}

private fun loadRealSmsTransactions(context: Context): List<TransactionUi> {
    val repository = TransactionRepository(context)
    val messages = readRecentSmsMessages(context, limit = 10000)
    val parsedRecords = messages.map { message ->
        val parsed = SmsParser.parse(message.body, message.address).withReceivedDateCorrection(message.receivedDate)
        SmsScanRecord(
            smsId = message.id,
            sourceAddress = message.address,
            body = message.body,
            receivedDate = message.receivedDate,
            sourceHash = message.sourceHash(),
            parsed = parsed
        )
    }
    val records = parsedRecords.withRecurringInvestmentHints()
    DebugFeedWriter.write(context, records)
    repository.import(
        records.map { record ->
            ImportRecord(
                source = ImportSource.SMS,
                sourceHash = record.sourceHash,
                sourceReceivedDate = record.receivedDate,
                parsed = record.parsed
            )
        }
    )
    runCatching {
        FxRateImporter(context).refreshRatesFor(records.map { it.parsed })
    }.onFailure { error ->
        Log.w(LogTag, "SMS FX refresh failed", error)
    }
    val fxRates = FxRateRepository(context)
        .listRates()
        .associateBy { it.key }
    return repository.listTransactions().map { it.toTransactionUi(fxRates) }
}

private fun loadPersistedTransactions(context: Context): List<TransactionUi> {
    val fxRates = FxRateRepository(context)
        .listRates()
        .associateBy { it.key }
    return TransactionRepository(context).listTransactions().map { it.toTransactionUi(fxRates) }
}

private fun hasPersistedGmailTransactions(context: Context): Boolean {
    return TransactionRepository(context)
        .listTransactions(limit = 2_000)
        .any { it.source == ImportSource.GMAIL }
}

private fun loadFeedState(context: Context, hasSmsPermission: Boolean): FeedState {
    val transactions = if (hasSmsPermission) {
        loadRealSmsTransactions(context)
    } else {
        loadPersistedTransactions(context)
    }

    return if (transactions.isNotEmpty()) {
        FeedState(
            transactions = transactions,
            label = transactions.feedSourceLabel(),
            needsSmsPermission = !hasSmsPermission
        )
    } else {
        FeedState(
            transactions = parsedSampleTransactions(),
            label = "sample SMS",
            needsSmsPermission = !hasSmsPermission
        )
    }
}

private fun saveManualTransaction(context: Context, draft: ManualTransactionDraft) {
    val now = System.currentTimeMillis()
    val merchant = draft.merchant.trim()
    val parsed = ParsedTransaction(
        isTransaction = true,
        status = TransactionStatus.COMPLETED,
        amount = draft.amount,
        currency = "INR",
        direction = draft.direction,
        merchantRaw = merchant,
        merchantNormalized = merchant,
        miscCategory = draft.miscCategory,
        departmentCategory = draft.category,
        paymentMode = draft.paymentMode,
        accountHint = null,
        transactionDate = draft.date,
        transactionTime = null,
        transactionType = draft.transactionType,
        categorySource = CategorySource.USER_RULE,
        confidence = 1.0,
        ignoreReason = null
    )

    TransactionRepository(context).import(
        listOf(
            ImportRecord(
                source = ImportSource.MANUAL,
                sourceHash = "manual:$now:${merchant}:${draft.amount}:${draft.date}".stableHash(),
                sourceReceivedDate = draft.date,
                parsed = parsed
            )
        )
    )
}

private fun ParsedTransaction.toTransactionUi(
    source: String = "Parsed SMS",
    sourceHash: String = "sample:${merchantNormalized ?: merchantRaw}:${amount}:${transactionDate}".stableHash(),
    fxRates: Map<FxRateKey, FxRateEntity> = emptyMap()
): TransactionUi {
    val amountNumber = amount ?: 0.0
    val currencyCode = currency.normalizedCurrency()
    val fxRate = fxRateFor(transactionDate, currencyCode, fxRates)
    val inrEquivalent = inrEquivalentValue(amountNumber, currencyCode, fxRate)
    val payment = paymentMode.displayName()
    val misc = miscCategory ?: "Uncategorized"
    val category = departmentCategory ?: "Other"
    val date = transactionDate ?: "Date unknown"
    val directionUi = if (direction == Direction.CREDIT) DirectionUi.Credit else DirectionUi.Debit
    val fxDetail = fxRate?.let { rate ->
        "FX ${rate.rateDate} @ ${rate.rate.formatFxRate()} = ${inrEquivalent?.formatInr()}"
    }
    val detail = listOfNotNull(payment, misc, date, fxDetail).joinToString(" • ")

    return TransactionUi(
        id = null,
        sourceHash = sourceHash,
        merchant = merchantNormalized ?: merchantRaw ?: "Unknown",
        detail = detail,
        amount = amountNumber.formatMoney(currencyCode),
        amountValue = amountNumber,
        currency = currencyCode,
        inrAmountValue = inrEquivalent,
        paymentMode = payment,
        miscCategory = misc,
        category = category,
        direction = directionUi,
        transactionType = transactionType,
        transactionDate = transactionDate,
        source = source,
        categorySource = categorySource,
        confidence = confidence
    )
}

private fun TransactionEntity.toTransactionUi(
    fxRates: Map<FxRateKey, FxRateEntity> = emptyMap()
): TransactionUi {
    val amountNumber = amount ?: 0.0
    val currencyCode = currency.normalizedCurrency()
    val fxRate = fxRateFor(transactionDate, currencyCode, fxRates)
    val inrEquivalent = inrEquivalentValue(amountNumber, currencyCode, fxRate)
    val payment = paymentMode.displayName()
    val misc = miscCategory ?: "Uncategorized"
    val category = departmentCategory ?: "Other"
    val date = transactionDate ?: "Date unknown"
    val directionUi = if (direction == Direction.CREDIT) DirectionUi.Credit else DirectionUi.Debit
    val fxDetail = fxRate?.let { rate ->
        "FX ${rate.rateDate} @ ${rate.rate.formatFxRate()} = ${inrEquivalent?.formatInr()}"
    }
    val detail = listOfNotNull(payment, misc, date, fxDetail).joinToString(" • ")

    return TransactionUi(
        id = id,
        sourceHash = sourceHash,
        merchant = merchantNormalized ?: merchantRaw ?: "Unknown",
        detail = detail,
        amount = amountNumber.formatMoney(currencyCode),
        amountValue = amountNumber,
        currency = currencyCode,
        inrAmountValue = inrEquivalent,
        paymentMode = payment,
        miscCategory = misc,
        category = category,
        direction = directionUi,
        transactionType = transactionType,
        transactionDate = transactionDate,
        source = source.displayLabel(),
        categorySource = categorySource,
        confidence = confidence
    )
}

private fun ParsedTransaction.withReceivedDateCorrection(receivedDate: String?): ParsedTransaction {
    if (!isTransaction || receivedDate == null) return this
    val parsedDate = transactionDate?.toLocalDateOrNull()
    val smsDate = receivedDate.toLocalDateOrNull() ?: return this
    val today = LocalDate.now()

    return when {
        parsedDate == null -> copy(transactionDate = receivedDate)
        parsedDate.isAfter(today) -> copy(transactionDate = receivedDate)
        kotlin.math.abs(ChronoUnit.DAYS.between(smsDate, parsedDate)) > 7 -> copy(transactionDate = receivedDate)
        else -> this
    }
}

private fun readRecentSmsMessages(context: Context, limit: Int): List<SmsInboxMessage> {
    val uri = Uri.parse("content://sms/inbox")
    val projection = arrayOf("_id", "address", "body", "date")
    val messages = mutableListOf<SmsInboxMessage>()

    context.contentResolver.query(
        uri,
        projection,
        null,
        null,
        "date DESC"
)?.use { cursor ->
        val idIndex = cursor.getColumnIndex("_id")
        val addressIndex = cursor.getColumnIndex("address")
        val bodyIndex = cursor.getColumnIndex("body")
        val dateIndex = cursor.getColumnIndex("date")
        while (cursor.moveToNext() && messages.size < limit) {
            if (bodyIndex >= 0) {
                val id = if (idIndex >= 0 && !cursor.isNull(idIndex)) {
                    cursor.getLong(idIndex)
                } else {
                    null
                }
                val address = if (addressIndex >= 0 && !cursor.isNull(addressIndex)) {
                    cursor.getString(addressIndex)
                } else {
                    null
                }
                val body = cursor.getString(bodyIndex)
                val receivedAtMillis = if (dateIndex >= 0 && !cursor.isNull(dateIndex)) {
                    cursor.getLong(dateIndex)
                } else {
                    null
                }
                val receivedDate = receivedAtMillis?.toIsoDate()
                if (body != null) {
                    messages.add(
                        SmsInboxMessage(
                            id = id,
                            address = address,
                            body = body,
                            receivedAtMillis = receivedAtMillis,
                            receivedDate = receivedDate
                        )
                    )
                }
            }
        }
    }

    return messages
}

private fun Long.toIsoDate(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .toString()
}

private data class RecurringInvestmentHint(
    val date: String,
    val amountKey: Long
)

private data class MutualFundAllotmentCandidate(
    val hint: RecurringInvestmentHint,
    val parsed: ParsedTransaction
)

private data class IndexedMutualFundAllotmentCandidate(
    val recordIndex: Int,
    val candidate: MutualFundAllotmentCandidate
)

private data class PrimaryInvestmentDebit(
    val recordIndex: Int,
    val hint: RecurringInvestmentHint
)

private fun List<SmsScanRecord>.withRecurringInvestmentHints(): List<SmsScanRecord> {
    val hints = mapNotNull { record -> record.recurringInvestmentHint() }.toSet()
    val allotmentCandidates = mapIndexedNotNull { index, record ->
        record.mutualFundAllotmentCandidate()?.let { candidate ->
            IndexedMutualFundAllotmentCandidate(index, candidate)
        }
    }
    val usedAllotmentRecordIndexes = mutableSetOf<Int>()

    val hintedRecords = if (hints.isEmpty()) {
        this
    } else {
        map { record ->
            val parsed = record.parsed
            val amount = parsed.amount
            val date = parsed.transactionDate
            val matchingAllotment = if (parsed.isGenericInvestmentDebitCandidate()) {
                allotmentCandidates
                    .filter { it.recordIndex !in usedAllotmentRecordIndexes }
                    .filter { parsed.matchesInvestmentHint(it.candidate.hint) }
                    .minByOrNull { parsed.investmentHintDistanceDays(it.candidate.hint) }
            } else {
                null
            }

            when {
                matchingAllotment != null -> {
                    usedAllotmentRecordIndexes += matchingAllotment.recordIndex
                    record.copy(parsed = parsed.asInvestmentDebit(matchingAllotment.candidate.parsed))
                }

                parsed.isTransaction &&
                    parsed.direction == Direction.DEBIT &&
                    parsed.paymentMode == PaymentMode.UPI &&
                    parsed.merchantRaw.isNullOrBlank() &&
                    record.sourceAddress.orEmpty().contains("PNB", ignoreCase = true) &&
                    amount != null &&
                    date != null &&
                    RecurringInvestmentHint(date, amount.toInvestmentAmountKey()) in hints -> {
                    record.copy(
                        parsed = parsed.copy(
                            merchantRaw = "Indian Clearing",
                            merchantNormalized = "Indian Clearing Corporation",
                            miscCategory = "Mutual Fund",
                            departmentCategory = "Investment",
                            paymentMode = PaymentMode.NACH,
                            transactionType = TransactionType.INVESTMENT,
                            categorySource = CategorySource.KNOWN_MERCHANT_RULE,
                            confidence = maxOf(parsed.confidence, 0.95)
                        )
                    )
                }

                else -> record
            }
        }
    }

    val primaryInvestmentDebits = hintedRecords
        .mapIndexedNotNull { index, record ->
            val parsed = record.parsed
            val amount = parsed.amount ?: return@mapIndexedNotNull null
            val date = parsed.transactionDate ?: return@mapIndexedNotNull null
            val isAllotmentRecord = allotmentCandidates.any { it.recordIndex == index }
            if (!isAllotmentRecord && parsed.isTransaction && parsed.direction == Direction.DEBIT && parsed.transactionType == TransactionType.INVESTMENT) {
                PrimaryInvestmentDebit(index, RecurringInvestmentHint(date, amount.toInvestmentAmountKey()))
            } else {
                null
            }
        }
    val matchedPrimaryDebitIndexes = mutableSetOf<Int>()
    val includedConfirmationSignatures = mutableSetOf<String>()

    return hintedRecords.mapIndexed { index, record ->
        val parsed = record.parsed
        val candidate = record.mutualFundAllotmentCandidate()
        if (candidate == null) {
            record
        } else if (index in usedAllotmentRecordIndexes) {
            record.copy(parsed = parsed.asIgnoredInvestmentAllotment())
        } else {
            val matchingPrimaryDebit = primaryInvestmentDebits
                .filter { it.recordIndex !in matchedPrimaryDebitIndexes }
                .filter { candidate.hint.matchesNearby(it.hint) }
                .minByOrNull { candidate.hint.distanceDays(it.hint) }
            val signature = candidate.confirmationSignature()

            if (matchingPrimaryDebit != null) {
                matchedPrimaryDebitIndexes += matchingPrimaryDebit.recordIndex
                record.copy(parsed = parsed.asIgnoredInvestmentAllotment())
            } else if (signature in includedConfirmationSignatures) {
                record.copy(parsed = parsed.asIgnoredInvestmentAllotment())
            } else {
                includedConfirmationSignatures += signature
                record.copy(parsed = candidate.parsed)
            }
        }
    }
}

private fun SmsScanRecord.recurringInvestmentHint(): RecurringInvestmentHint? {
    pnbIndianClearingHint()?.let { return it }
    mutualFundAllotmentHint()?.let { return it }
    return null
}

private fun SmsScanRecord.pnbIndianClearingHint(): RecurringInvestmentHint? {
    if (!sourceAddress.orEmpty().contains("PNB", ignoreCase = true)) return null
    if (!body.contains("will be debited", ignoreCase = true)) return null
    if (!body.contains("Indian Clearing", ignoreCase = true)) return null
    val match = Regex(
        """will be debited for Rs\.?\s*([\d,]+(?:\.\d+)?)\s+on\s+(\d{2}-\d{2}-\d{2})""",
        RegexOption.IGNORE_CASE
    ).find(body) ?: return null
    val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
    val date = match.groupValues[2].toIsoDateFromDdMmYy() ?: return null
    return RecurringInvestmentHint(date, amount.toInvestmentAmountKey())
}

private fun SmsScanRecord.mutualFundAllotmentHint(): RecurringInvestmentHint? {
    return mutualFundAllotmentCandidate()?.hint
}

private fun SmsScanRecord.mutualFundAllotmentCandidate(): MutualFundAllotmentCandidate? {
    val lower = body.lowercase(Locale.US)
    val isAllotment = listOf(
        "sip installment",
        "sip instalment",
        "sip transaction",
        "sip purchase",
        "purchase request"
    ).any { it in lower } && listOf(
        "processed",
        "units are allotted",
        "units are alotted"
    ).any { it in lower }
    val isFundSource = Regex("""(?i)(AMC|MF|IPRUMF|HDFCMF|QNTAMC|EDLAMC|MOAMCL)""").containsMatchIn(sourceAddress.orEmpty()) ||
        listOf("mutual fund", "edelweiss asset management", "motilal oswal mf", "iprumf", "hdfcmf").any { it in lower }
    if (!isAllotment || !isFundSource) return null

    val amount = Regex("""(?i)\bfor\s+Rs\.?\s*([\d,]+(?:\.\d+)?)""")
        .find(body)
        ?.groupValues
        ?.get(1)
        ?.replace(",", "")
        ?.toDoubleOrNull()
        ?: return null
    val date = listOfNotNull(
        Regex("""(?i)\b(?:dated|on)\s+(\d{1,2}/\d{1,2}/\d{4})""").find(body)?.groupValues?.get(1)?.toIsoDateFromDdMmYyyy("/"),
        Regex("""(?i)\b(?:dated|on)\s+(\d{1,2}-[A-Za-z]{3}-\d{4})""").find(body)?.groupValues?.get(1)?.toIsoDateFromDdMmmYyyy(),
        Regex("""(?i)\byour\s+(\d{1,2}\s+[A-Za-z]{3}\s+\d{4})\s+SIP""").find(body)?.groupValues?.get(1)?.toIsoDateFromDdMmmYyyy(" "),
        receivedDate
    ).firstOrNull() ?: return null

    val merchant = mutualFundMerchantName()
    val parsed = ParsedTransaction(
        isTransaction = true,
        status = TransactionStatus.COMPLETED,
        amount = amount,
        currency = "INR",
        direction = Direction.DEBIT,
        merchantRaw = merchant,
        merchantNormalized = merchant,
        miscCategory = "Mutual Fund",
        departmentCategory = "Investment",
        paymentMode = PaymentMode.NACH,
        accountHint = null,
        transactionDate = date,
        transactionTime = null,
        transactionType = TransactionType.INVESTMENT,
        categorySource = CategorySource.KNOWN_MERCHANT_RULE,
        confidence = 0.92,
        ignoreReason = null
    )
    return MutualFundAllotmentCandidate(
        hint = RecurringInvestmentHint(date, amount.toInvestmentAmountKey()),
        parsed = parsed
    )
}

private fun SmsScanRecord.mutualFundMerchantName(): String {
    val source = sourceAddress.orEmpty()
    val lower = body.lowercase(Locale.US)
    return when {
        source.contains("IPRUMF", ignoreCase = true) || "iprumf" in lower -> "ICICI Prudential Mutual Fund"
        source.contains("HDFCMF", ignoreCase = true) || "hdfcmf" in lower -> "HDFC Mutual Fund"
        source.contains("QNTAMC", ignoreCase = true) || "quant mutual fund" in lower -> "Quant Mutual Fund"
        source.contains("EDLAMC", ignoreCase = true) || "edelweiss asset management" in lower -> "Edelweiss Mutual Fund"
        source.contains("MOAMCL", ignoreCase = true) || "motilal oswal mf" in lower -> "Motilal Oswal Mutual Fund"
        else -> "Mutual Fund"
    }
}

private fun String.toIsoDateFromDdMmYy(separator: String = "-"): String? {
    val parts = split(separator)
    if (parts.size != 3) return null
    val day = parts[0].padStart(2, '0')
    val month = parts[1].padStart(2, '0')
    val year = parts[2].padStart(2, '0')
    return "20$year-$month-$day"
}

private fun String.toIsoDateFromDdMmYyyy(separator: String): String? {
    val parts = split(separator)
    if (parts.size != 3) return null
    val day = parts[0].padStart(2, '0')
    val month = parts[1].padStart(2, '0')
    val year = parts[2]
    if (year.length != 4) return null
    return "$year-$month-$day"
}

private fun String.toIsoDateFromDdMmmYyyy(separator: String = "-"): String? {
    val parts = split(separator).filter(String::isNotBlank)
    if (parts.size != 3) return null
    val day = parts[0].padStart(2, '0')
    val month = monthNumberFromShortName(parts[1]) ?: return null
    val year = parts[2]
    if (year.length != 4) return null
    return "$year-$month-$day"
}

private fun monthNumberFromShortName(value: String): String? {
    return when (value.take(3).lowercase(Locale.US)) {
        "jan" -> "01"
        "feb" -> "02"
        "mar" -> "03"
        "apr" -> "04"
        "may" -> "05"
        "jun" -> "06"
        "jul" -> "07"
        "aug" -> "08"
        "sep" -> "09"
        "oct" -> "10"
        "nov" -> "11"
        "dec" -> "12"
        else -> null
    }
}

private fun Double.toInvestmentAmountKey(): Long {
    return kotlin.math.round(this).toLong()
}

private fun ParsedTransaction.isGenericInvestmentDebitCandidate(): Boolean {
    return isTransaction &&
        direction == Direction.DEBIT &&
        amount != null &&
        (amount >= 500.0) &&
        transactionDate != null &&
        (
            merchantRaw.isNullOrBlank() ||
                merchantNormalized.isNullOrBlank() ||
                (departmentCategory == "Other" && miscCategory == "Uncategorized")
            )
}

private fun ParsedTransaction.matchesInvestmentHint(hint: RecurringInvestmentHint): Boolean {
    val amount = amount ?: return false
    val date = transactionDate ?: return false
    return amount.toInvestmentAmountKey() == hint.amountKey &&
        RecurringInvestmentHint(date, amount.toInvestmentAmountKey()).matchesNearby(hint)
}

private fun ParsedTransaction.investmentHintDistanceDays(hint: RecurringInvestmentHint): Int {
    val date = transactionDate ?: return Int.MAX_VALUE
    return RecurringInvestmentHint(date, amount?.toInvestmentAmountKey() ?: Long.MIN_VALUE).distanceDays(hint)
}

private fun ParsedTransaction.asInvestmentDebit(source: ParsedTransaction): ParsedTransaction {
    return copy(
        merchantRaw = source.merchantRaw,
        merchantNormalized = source.merchantNormalized,
        miscCategory = "Mutual Fund",
        departmentCategory = "Investment",
        paymentMode = if (paymentMode == PaymentMode.UNKNOWN) source.paymentMode else paymentMode,
        transactionType = TransactionType.INVESTMENT,
        categorySource = CategorySource.KNOWN_MERCHANT_RULE,
        confidence = maxOf(confidence, 0.95)
    )
}

private fun ParsedTransaction.asIgnoredInvestmentAllotment(): ParsedTransaction {
    return copy(
        isTransaction = false,
        status = TransactionStatus.IGNORED,
        amount = null,
        currency = null,
        direction = Direction.UNKNOWN,
        merchantRaw = null,
        merchantNormalized = null,
        miscCategory = null,
        departmentCategory = null,
        paymentMode = PaymentMode.UNKNOWN,
        transactionDate = null,
        transactionTime = null,
        transactionType = TransactionType.UNKNOWN,
        categorySource = CategorySource.NONE,
        confidence = 0.0,
        ignoreReason = "duplicate_investment_allotment"
    )
}

private fun MutualFundAllotmentCandidate.confirmationSignature(): String {
    return listOf(
        parsed.merchantNormalized.orEmpty(),
        hint.date,
        hint.amountKey.toString()
    ).joinToString("|")
}

private fun RecurringInvestmentHint.matchesNearby(other: RecurringInvestmentHint): Boolean {
    return amountKey == other.amountKey && distanceDays(other) <= 1
}

private fun RecurringInvestmentHint.distanceDays(other: RecurringInvestmentHint): Int {
    val left = date.toLocalDateOrNull() ?: return Int.MAX_VALUE
    val right = other.date.toLocalDateOrNull() ?: return Int.MAX_VALUE
    return kotlin.math.abs(ChronoUnit.DAYS.between(left, right)).toInt()
}

private fun String.toLocalDateOrNull(): LocalDate? {
    return runCatching { LocalDate.parse(this) }.getOrNull()
}

private fun hasReadSmsPermission(context: Context): Boolean {
    return context.checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? {
    return when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }
}

private fun gmailSetupInfo(context: Context): GmailSetupInfo {
    return GmailSetupInfo(
        packageName = context.packageName,
        signingSha1 = context.signingCertificateSha1()
    )
}

private fun gmailAuthErrorMessage(error: ApiException, setupInfo: GmailSetupInfo): String {
    val rawMessage = error.message ?: "unknown"
    return when {
        error.statusCode == 8 && rawMessage.contains("UNREGISTERED_ON_API_CONSOLE", ignoreCase = true) ->
            "Google Cloud OAuth client missing or mismatched. Add an Android OAuth client with this package and SHA-1."
        error.statusCode == 12501 ->
            "Authorization was cancelled."
        else ->
            "Google auth failed (${error.statusCode}): $rawMessage"
    }
}

@Suppress("DEPRECATION")
private fun Context.signingCertificateSha1(): String? {
    val signatures = runCatching {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                .signingInfo
                ?.apkContentsSigners
        } else {
            packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                .signatures
        }
        packageInfo?.toList().orEmpty()
    }.getOrElse {
        Log.e(LogTag, "Unable to read app signing certificate", it)
        emptyList()
    }

    val certificate = signatures.firstOrNull()?.toByteArray() ?: return null
    return MessageDigest.getInstance("SHA-1")
        .digest(certificate)
        .joinToString(":") { byte -> "%02X".format(byte) }
}

@Suppress("DEPRECATION")
@Composable
private fun SortedTheme(
    themeMode: AppThemeMode,
    content: @Composable () -> Unit
) {
    val systemDarkMode = isSystemInDarkTheme()
    val darkMode = when (themeMode) {
        AppThemeMode.System -> systemDarkMode
        AppThemeMode.Dark -> true
        AppThemeMode.Light -> false
    }
    val appFontFamily = SortedTapeFontFamily
    val colors = if (darkMode) {
        darkColorScheme(
            background = Color(0xFF000000),
            surface = Color(0xFF050500),
            surfaceVariant = Color(0xFF121000),
            primary = Color(0xFFFBC02D),
            secondary = Color(0xFFFBC02D),
            tertiary = Color(0xFFC49018),
            onBackground = Color(0xFFFFFFFF),
            onSurface = Color(0xFFFFFFFF),
            onSurfaceVariant = Color(0xFFB8B8B8),
            onPrimary = Color(0xFF171000)
        )
    } else {
        lightColorScheme(
            background = Color(0xFFFFFAFE),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFEAF4FF),
            primary = Color(0xFFFF3F86),
            secondary = Color(0xFF00AEEF),
            tertiary = Color(0xFF7C4DFF),
            onBackground = Color(0xFF151018),
            onSurface = Color(0xFF151018),
            onSurfaceVariant = Color(0xFF665A68),
            onPrimary = Color(0xFFFFFFFF)
        )
    }
    val activity = LocalContext.current.findComponentActivity()

    SideEffect {
        activity?.window?.let { window ->
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.surface.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                var flags = window.decorView.systemUiVisibility
                flags = if (darkMode) {
                    flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                } else {
                    flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    flags = if (darkMode) {
                        flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                    } else {
                        flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                    }
                }
                window.decorView.systemUiVisibility = flags
            }
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography().withFontFamily(appFontFamily),
        content = content
    )
}

private fun Typography.withFontFamily(fontFamily: FontFamily): Typography {
    return copy(
        displayLarge = displayLarge.copy(fontFamily = fontFamily),
        displayMedium = displayMedium.copy(fontFamily = fontFamily),
        displaySmall = displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = titleLarge.copy(fontFamily = fontFamily),
        titleMedium = titleMedium.copy(fontFamily = fontFamily),
        titleSmall = titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = bodySmall.copy(fontFamily = fontFamily),
        labelLarge = labelLarge.copy(fontFamily = fontFamily),
        labelMedium = labelMedium.copy(fontFamily = fontFamily),
        labelSmall = labelSmall.copy(fontFamily = fontFamily)
    )
}

@Composable
private fun SortedOpeningScreen() {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        entered = true
    }

    val logoSize by animateDpAsState(
        targetValue = if (entered) 104.dp else 74.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "opening_logo_size"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 650),
        label = "opening_content_alpha"
    )
    val taglineAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 700, delayMillis = 280),
        label = "opening_tagline_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        OpeningConstellationBackdrop(
            modifier = Modifier
                .fillMaxSize()
                .alpha(contentAlpha)
        )
        Column(
            modifier = Modifier.alpha(contentAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SortedLogoMark(modifier = Modifier.size(logoSize))
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Sorted",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 42.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(13.dp))
            Row(
                modifier = Modifier.alpha(taglineAlpha),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transactions?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    letterSpacing = 0.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sorted",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            letterSpacing = 0.sp
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        OpeningCheckGlyph(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SortedLoadingScreen() {
    val isDark = isDarkModeActive()
    val transition = rememberInfiniteTransition(label = "loading_home_constellation")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loading_home_constellation_phase"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 420),
        label = "loading_content_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        ConstellationField(
            nodes = emptyList(),
            phase = phase,
            isDark = isDark,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.88f)
        )
        Column(
            modifier = Modifier.alpha(contentAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SortedLogoMark(modifier = Modifier.size(72.dp))
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Sorted",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(9.dp))
            Text(
                text = "Sorting your month",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                letterSpacing = 0.sp
            )
        }
    }
}

@Composable
private fun OpeningConstellationBackdrop(modifier: Modifier = Modifier) {
    val isDark = isDarkModeActive()
    val primary = MaterialTheme.colorScheme.primary
    val text = MaterialTheme.colorScheme.onBackground
    val transition = rememberInfiniteTransition(label = "opening_constellation")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "opening_constellation_phase"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width * 0.50f, size.height * 0.48f)
        val rx = size.width * 0.31f
        val ry = size.height * 0.13f
        listOf(-22f, 0f, 22f, 72f).forEachIndexed { index, degrees ->
            rotate(degrees = degrees, pivot = center) {
                drawOval(
                    color = primary.copy(alpha = if (isDark) 0.075f else 0.12f),
                    topLeft = Offset(center.x - rx, center.y - ry + index * 2.dp.toPx()),
                    size = Size(rx * 2f, ry * 2f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }
        repeat(56) { i ->
            val theta = ((i / 56f) * PI * 2.0 + phase * PI * 2.0 * 0.16).toFloat()
            val latitude = sin((i * 1.41f + phase) * PI.toFloat()) * 0.75f
            val depth = cos(theta) * 0.5f + 0.5f
            val px = center.x + cos(theta) * rx * cos(latitude)
            val py = center.y + sin(latitude) * ry * 1.5f
            drawCircle(
                color = if (i % 4 == 0) primary.copy(alpha = 0.10f + depth * 0.24f) else text.copy(alpha = 0.05f + depth * 0.16f),
                radius = (0.8f + depth * 1.4f).dp.toPx(),
                center = Offset(px, py)
            )
        }
    }
}

@Composable
private fun OpeningCheckGlyph(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.2.dp.toPx()
        drawLine(
            color = color,
            start = Offset(size.width * 0.18f, size.height * 0.55f),
            end = Offset(size.width * 0.42f, size.height * 0.78f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.42f, size.height * 0.78f),
            end = Offset(size.width * 0.84f, size.height * 0.22f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortedHome(
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val gmailSetupInfo = remember { gmailSetupInfo(appContext) }
    val gmailSyncPreferences = remember {
        GmailSyncPreferences(appContext).also { it.clearTransientErrors() }
    }
    var selected by remember { mutableStateOf<TransactionUi?>(null) }
    var drilldown by remember { mutableStateOf<DrilldownState?>(null) }
    var selectedTab by remember { mutableStateOf(SortedTab.Home) }
    var settingsOpen by remember { mutableStateOf(false) }
    var explainOpen by remember { mutableStateOf(false) }
    var sortInboxOpen by remember { mutableStateOf(false) }
    var ruleCenterOpen by remember { mutableStateOf(false) }
    var syncChooserOpen by remember { mutableStateOf(false) }
    var syncStatus by remember { mutableStateOf<String?>(null) }
    var selectedHomeMonthKey by remember { mutableStateOf<String?>(null) }
    var manualSaveState by remember { mutableStateOf(ManualSaveState()) }
    var correctionSaveState by remember { mutableStateOf(CorrectionSaveState()) }
    var hasPermission by remember { mutableStateOf(hasReadSmsPermission(context)) }
    var gmailState by remember {
        mutableStateOf(GmailUiState(autoSyncLabel = gmailSyncPreferences.statusLabel()))
    }
    var feedLoaded by remember { mutableStateOf(false) }
    var feedState by remember {
        mutableStateOf(
            FeedState(
                transactions = emptyList(),
                label = "Loading",
                needsSmsPermission = !hasPermission
            )
        )
    }
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }
    fun gmailStateWithAutoSync(
        label: String,
        isImporting: Boolean = false,
        error: String? = null
    ): GmailUiState {
        return GmailUiState(
            label = label,
            isImporting = isImporting,
            error = error,
            autoSyncLabel = gmailSyncPreferences.statusLabel()
        )
    }

    fun updateFeed(transactions: List<TransactionUi>) {
        val months = transactions.availableMonthKeys()
        feedState = FeedState(
            transactions = transactions,
            label = transactions.feedSourceLabel(),
            needsSmsPermission = !hasPermission
        )
        selectedHomeMonthKey = selectedHomeMonthKey
            ?.takeIf { it in months }
            ?: months.firstOrNull()
        feedLoaded = true
    }

    fun openTransaction(transaction: TransactionUi) {
        correctionSaveState = CorrectionSaveState()
        selected = transaction
    }

    fun saveManualDraft(draft: ManualTransactionDraft) {
        manualSaveState = ManualSaveState(isSaving = true)
        scope.launch {
            try {
                val transactions = withContext(Dispatchers.IO) {
                    saveManualTransaction(appContext, draft)
                    loadPersistedTransactions(appContext)
                }
                updateFeed(transactions)
                manualSaveState = ManualSaveState(message = "Added ${draft.merchant.trim()}")
            } catch (error: Throwable) {
                manualSaveState = ManualSaveState(
                    error = error.message?.take(160) ?: error.javaClass.simpleName
                )
            }
        }
    }

    fun saveCorrectionDraft(draft: TransactionCorrectionDraft) {
        val transactionId = draft.transaction.id
        if (transactionId == null) {
            correctionSaveState = CorrectionSaveState(error = "Sample transactions cannot be edited.")
            return
        }

        correctionSaveState = CorrectionSaveState(isSaving = true)
        scope.launch {
            try {
                val transactions = withContext(Dispatchers.IO) {
                    val saved = TransactionRepository(appContext).updateTransaction(
                        TransactionCorrection(
                            transactionId = transactionId,
                            merchantNormalized = draft.merchant.trim(),
                            miscCategory = draft.miscCategory.trim().ifBlank { "Uncategorized" },
                            departmentCategory = draft.category,
                            transactionType = draft.transactionType,
                            rememberRule = draft.rememberRule
                        )
                    )
                    if (!saved) {
                        throw IllegalStateException("Transaction was not found.")
                    }
                    loadPersistedTransactions(appContext)
                }
                updateFeed(transactions)
                selected = transactions.firstOrNull { it.id == transactionId }
                correctionSaveState = CorrectionSaveState(message = "Updated locally")
            } catch (error: Throwable) {
                correctionSaveState = CorrectionSaveState(
                    error = error.message?.take(160) ?: error.javaClass.simpleName
                )
            }
        }
    }

    fun ignoreTransaction(transaction: TransactionUi) {
        if (transaction.sourceHash.isBlank()) {
            correctionSaveState = CorrectionSaveState(error = "This transaction cannot be ignored.")
            return
        }

        correctionSaveState = CorrectionSaveState(isSaving = true)
        scope.launch {
            try {
                val transactions = withContext(Dispatchers.IO) {
                    TransactionRepository(appContext).ignoreTransaction(transaction.sourceHash)
                    loadPersistedTransactions(appContext)
                }
                updateFeed(transactions)
                selected = null
                correctionSaveState = CorrectionSaveState(message = "Ignored locally")
            } catch (error: Throwable) {
                correctionSaveState = CorrectionSaveState(
                    error = error.message?.take(160) ?: error.javaClass.simpleName
                )
            }
        }
    }

    fun runGmailImport(accessToken: String?, startLabel: String = "Reading Gmail") {
        if (accessToken.isNullOrBlank()) {
            syncStatus = "Gmail sync failed"
            gmailState = gmailStateWithAutoSync(
                label = "Gmail import failed",
                error = "Google did not return an access token."
            )
            return
        }

        gmailState = gmailStateWithAutoSync(label = startLabel, isImporting = true)
        syncStatus = startLabel
        scope.launch {
            try {
                val (summary, transactions) = withContext(Dispatchers.IO) {
                    val summary = GmailImporter(appContext).importLatest(accessToken)
                    GmailSyncScheduler.schedule(appContext)
                    gmailSyncPreferences.markSyncSuccess(summary)
                    val transactions = loadPersistedTransactions(appContext)
                    summary to transactions
                }
                updateFeed(transactions)
                gmailState = gmailStateWithAutoSync(label = summary.displayLabel())
                syncStatus = "Gmail synced"
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                gmailSyncPreferences.markSyncError(error.message ?: error.javaClass.simpleName)
                syncStatus = "Gmail sync failed"
                gmailState = gmailStateWithAutoSync(
                    label = "Gmail import failed",
                    error = error.message?.take(180) ?: error.javaClass.simpleName
                )
            }
        }
    }

    fun requestSilentGmailImport() {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(GmailImportPlan.RequiredScope)))
            .build()

        gmailState = gmailStateWithAutoSync(label = "Auto syncing Gmail", isImporting = true)
        Identity.getAuthorizationClient(context)
            .authorize(request)
            .addOnSuccessListener { authorizationResult ->
                if (authorizationResult.hasResolution()) {
                    gmailSyncPreferences.markNeedsManualAuth()
                    syncStatus = "Open Gmail sync"
                    gmailState = gmailStateWithAutoSync(
                        label = "Gmail auto sync paused",
                        error = "Tap Import to reconnect Gmail."
                    )
                } else {
                    runGmailImport(authorizationResult.accessToken, startLabel = "Auto syncing Gmail")
                }
            }
            .addOnFailureListener { error ->
                val apiError = error as? ApiException
                val message = if (apiError != null) {
                    gmailAuthErrorMessage(apiError, gmailSetupInfo)
                } else {
                    error.localizedMessage ?: "Google authorization failed."
                }
                gmailSyncPreferences.markSyncError(message)
                syncStatus = "Gmail sync failed"
                gmailState = gmailStateWithAutoSync(
                    label = "Gmail auto sync failed",
                    error = message
                )
            }
    }

    val gmailAuthorizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.data == null) {
            syncStatus = "Gmail cancelled"
            gmailState = gmailStateWithAutoSync(
                label = "Gmail not connected",
                error = "Authorization was cancelled or Google returned no result. Check OAuth setup and test-user access."
            )
            return@rememberLauncherForActivityResult
        }

        try {
            val authorizationResult = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(activityResult.data)
            val grantedScopes = authorizationResult.grantedScopes.toSet()
            if (GmailImportPlan.RequiredScope !in grantedScopes) {
                syncStatus = "Gmail permission missing"
                gmailState = gmailStateWithAutoSync(
                    label = "Gmail permission missing",
                    error = "Gmail read permission was not granted."
                )
                return@rememberLauncherForActivityResult
            }
            runGmailImport(authorizationResult.accessToken)
        } catch (error: ApiException) {
            Log.e(LogTag, "Gmail authorization failed", error)
            syncStatus = "Gmail sync failed"
            gmailState = gmailStateWithAutoSync(
                label = "Gmail import failed",
                error = gmailAuthErrorMessage(error, gmailSetupInfo)
            )
        } catch (error: Throwable) {
            Log.e(LogTag, "Gmail authorization result failed", error)
            syncStatus = "Gmail sync failed"
            gmailState = gmailStateWithAutoSync(
                label = "Gmail import failed",
                error = error.message ?: error.javaClass.simpleName
            )
        }
    }

    fun requestGmailImport() {
        val activity = context.findComponentActivity()
        if (activity == null) {
            syncStatus = "Gmail sync failed"
            gmailState = gmailStateWithAutoSync(
                label = "Gmail import failed",
                error = "Unable to open Google authorization from this screen."
            )
            return
        }

        gmailState = gmailStateWithAutoSync(label = "Opening Google consent", isImporting = true)
        syncStatus = "Opening Gmail"
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(GmailImportPlan.RequiredScope)))
            .build()

        Identity.getAuthorizationClient(activity)
            .authorize(request)
            .addOnSuccessListener { authorizationResult ->
                if (authorizationResult.hasResolution()) {
                    val pendingIntent = authorizationResult.pendingIntent
                    if (pendingIntent == null) {
                        syncStatus = "Gmail sync failed"
                        gmailState = gmailStateWithAutoSync(
                            label = "Gmail import failed",
                            error = "Google authorization needs consent but returned no prompt."
                        )
                    } else {
                        gmailAuthorizationLauncher.launch(
                            IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                        )
                    }
                } else {
                    runGmailImport(authorizationResult.accessToken)
                }
            }
            .addOnFailureListener { error ->
                Log.e(LogTag, "Gmail authorization request failed", error)
                val apiError = error as? ApiException
                syncStatus = "Gmail sync failed"
                gmailState = gmailStateWithAutoSync(
                    label = "Gmail import failed",
                    error = if (apiError != null) {
                        gmailAuthErrorMessage(apiError, gmailSetupInfo)
                    } else {
                        error.localizedMessage ?: "Google authorization failed."
                    }
                )
            }
    }

    fun syncSmsNow() {
        if (!hasPermission) {
            syncStatus = "SMS permission needed"
            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
            return
        }
        syncStatus = "Syncing SMS"
        scope.launch {
            try {
                val transactions = withContext(Dispatchers.IO) {
                    loadRealSmsTransactions(appContext)
                }
                updateFeed(transactions)
                syncStatus = "SMS synced"
            } catch (error: Throwable) {
                syncStatus = "SMS sync failed"
            }
        }
    }

    fun syncSource(source: SyncSource) {
        syncChooserOpen = false
        when (source) {
            SyncSource.Sms -> syncSmsNow()
            SyncSource.Gmail -> requestGmailImport()
            SyncSource.All -> {
                if (!hasPermission) {
                    syncStatus = "SMS permission needed"
                    smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                    return
                }
                syncStatus = "Syncing SMS + Gmail"
                scope.launch {
                    try {
                        val transactions = withContext(Dispatchers.IO) {
                            loadRealSmsTransactions(appContext)
                        }
                        updateFeed(transactions)
                        requestSilentGmailImport()
                        syncStatus = "SMS synced, Gmail running"
                    } catch (error: Throwable) {
                        syncStatus = "Full sync failed"
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val shouldAutoSync = withContext(Dispatchers.IO) {
            gmailSyncPreferences.isAutoSyncEnabled() || hasPersistedGmailTransactions(appContext)
        }
        if (shouldAutoSync) {
            GmailSyncScheduler.schedule(appContext)
            requestSilentGmailImport()
        }
    }

    LaunchedEffect(hasPermission) {
        feedLoaded = false
        val loadedFeedState = withContext(Dispatchers.IO) {
            loadFeedState(appContext, hasPermission)
        }
        feedState = loadedFeedState
        val months = loadedFeedState.transactions.availableMonthKeys()
        selectedHomeMonthKey = selectedHomeMonthKey
            ?.takeIf { it in months }
            ?: months.firstOrNull()
        feedLoaded = true
    }

    LaunchedEffect(syncStatus) {
        val message = syncStatus ?: return@LaunchedEffect
        if (!message.startsWith("Syncing") && !message.startsWith("Opening") && !message.startsWith("Reading") && !message.startsWith("Auto syncing")) {
            delay(2600)
            if (syncStatus == message) {
                syncStatus = null
            }
        }
    }

    val activeDrilldown = drilldown
    BackHandler(
        enabled = activeDrilldown != null ||
            settingsOpen ||
            explainOpen ||
            sortInboxOpen ||
            ruleCenterOpen ||
            selectedTab != SortedTab.Home
    ) {
        when {
            ruleCenterOpen -> ruleCenterOpen = false
            sortInboxOpen -> sortInboxOpen = false
            explainOpen -> explainOpen = false
            activeDrilldown != null -> drilldown = null
            settingsOpen -> settingsOpen = false
            selectedTab != SortedTab.Home -> selectedTab = SortedTab.Home
        }
    }

    val route = when {
        activeDrilldown != null -> SortedRoute.Drilldown
        explainOpen -> SortedRoute.SpendExplanation
        sortInboxOpen -> SortedRoute.SortInbox
        ruleCenterOpen -> SortedRoute.RuleCenter
        settingsOpen -> SortedRoute.Settings
        selectedTab == SortedTab.Home && !feedLoaded -> SortedRoute.Loading
        else -> SortedRoute.Main
    }

    Crossfade(
        targetState = route,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "sorted_route_crossfade"
    ) { currentRoute ->
        when (currentRoute) {
            SortedRoute.Drilldown -> {
                val state = activeDrilldown
                if (state == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    )
                } else {
                    DrilldownScreen(
                        state = state,
                        allTransactions = feedState.transactions,
                        onBack = { drilldown = null },
                        onTransactionClick = { openTransaction(it) }
                    )
                }
            }

            SortedRoute.SpendExplanation -> {
                SpendExplanationScreen(
                    feedState = feedState,
                    onBack = { explainOpen = false },
                    onTransactionClick = { openTransaction(it) },
                    onOpenReview = { sortInboxOpen = true }
                )
            }

            SortedRoute.SortInbox -> {
                SortInboxScreen(
                    feedState = feedState,
                    onBack = { sortInboxOpen = false },
                    onTransactionClick = { openTransaction(it) }
                )
            }

            SortedRoute.RuleCenter -> {
                RuleCenterScreen(
                    modifier = Modifier,
                    onBack = { ruleCenterOpen = false }
                )
            }

            SortedRoute.Settings -> {
                SettingsScreen(
                    themeMode = themeMode,
                    feedState = feedState,
                    gmailState = gmailState,
                    onThemeModeChange = onThemeModeChange,
                    onBack = { settingsOpen = false },
                    onOpenRuleCenter = {
                        settingsOpen = false
                        ruleCenterOpen = true
                    }
                )
            }

            SortedRoute.Loading -> {
                SortedLoadingScreen()
            }

            SortedRoute.Main -> {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        SortedBottomBar(
                            selectedTab = selectedTab,
                            onTabSelected = {
                                syncChooserOpen = false
                                selectedTab = it
                            }
                        )
                    }
                ) { padding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        Crossfade(
                            targetState = selectedTab,
                            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                            label = "sorted_tab_crossfade"
                        ) { tab ->
                            when (tab) {
                                SortedTab.Home -> HomeTabContent(
                                    feedState = feedState,
                                    selectedMonthKey = selectedHomeMonthKey,
                                    modifier = Modifier.padding(padding),
                                    onSettings = { settingsOpen = true },
                                    onOpenSync = { syncChooserOpen = !syncChooserOpen },
                                    onMonthSelected = { selectedHomeMonthKey = it },
                                    onExplainSpend = { explainOpen = true },
                                    onOpenReview = { sortInboxOpen = true },
                                    onRequestSmsPermission = {
                                        smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                                    },
                                    onTransactionClick = { openTransaction(it) },
                                    onMerchantClick = { group ->
                                        drilldown = DrilldownState(
                                            title = group.label,
                                            kind = DrilldownKind.Merchant,
                                            group = group,
                                            spendOnly = true,
                                            monthKey = selectedHomeMonthKey ?: feedState.transactions.selectedMonthKey()
                                        )
                                    },
                                    onCategoryClick = { group ->
                                        drilldown = DrilldownState(
                                            title = group.label,
                                            kind = DrilldownKind.Category,
                                            group = group,
                                            spendOnly = true,
                                            monthKey = selectedHomeMonthKey ?: feedState.transactions.selectedMonthKey()
                                        )
                                    }
                                )

                                SortedTab.Insights -> InsightsTabContent(
                                    feedState = feedState,
                                    selectedMonthKey = selectedHomeMonthKey,
                                    modifier = Modifier.padding(padding),
                                    onSettings = { settingsOpen = true },
                                    onMerchantClick = { group ->
                                        drilldown = DrilldownState(
                                            title = group.label,
                                            kind = DrilldownKind.Merchant,
                                            group = group,
                                            spendOnly = true,
                                            monthKey = selectedHomeMonthKey ?: feedState.transactions.selectedMonthKey()
                                        )
                                    },
                                    onCategoryClick = { group ->
                                        drilldown = DrilldownState(
                                            title = group.label,
                                            kind = DrilldownKind.Category,
                                            group = group,
                                            spendOnly = true,
                                            monthKey = selectedHomeMonthKey ?: feedState.transactions.selectedMonthKey()
                                        )
                                    },
                                    onTransactionClick = { openTransaction(it) },
                                    onOpenReview = { sortInboxOpen = true }
                                )

                                SortedTab.Capture -> CaptureTabContent(
                                    feedState = feedState,
                                    modifier = Modifier.padding(padding),
                                    saveState = manualSaveState,
                                    onSettings = { settingsOpen = true },
                                    onSave = { draft -> saveManualDraft(draft) }
                                )

                                SortedTab.RuleCenter -> RuleCenterScreen(
                                    modifier = Modifier.padding(padding),
                                    onSettings = { settingsOpen = true },
                                    onBack = null
                                )
                            }
                        }
                        SyncChooserBar(
                            visible = syncChooserOpen,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 172.dp),
                            onSync = { syncSource(it) }
                        )
                        SyncStatusPill(
                            message = syncStatus,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 150.dp)
                        )
                    }
                }
            }
        }
    }

    selected?.let { transaction ->
        ModalBottomSheet(
            onDismissRequest = {
                correctionSaveState = CorrectionSaveState()
                selected = null
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
        ) {
            TransactionDetail(
                transaction = transaction,
                saveState = correctionSaveState,
                onCorrect = { draft -> saveCorrectionDraft(draft) },
                onIgnore = { ignoreTransaction(transaction) }
            )
        }
    }
}

@Composable
private fun HomeTabContent(
    feedState: FeedState,
    selectedMonthKey: String?,
    modifier: Modifier,
    onSettings: () -> Unit,
    onOpenSync: () -> Unit,
    onMonthSelected: (String) -> Unit,
    onExplainSpend: () -> Unit,
    onOpenReview: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    onTransactionClick: (TransactionUi) -> Unit,
    onMerchantClick: (SummaryGroup) -> Unit,
    onCategoryClick: (SummaryGroup) -> Unit
) {
    val months = remember(feedState.transactions) { feedState.transactions.availableMonthKeys() }
    val activeMonthKey = selectedMonthKey ?: months.firstOrNull()
    TapeHome(
        feedState = feedState,
        months = months,
        selectedMonthKey = activeMonthKey,
        modifier = modifier,
        onSettings = onSettings,
        onOpenSync = onOpenSync,
        onMonthSelected = onMonthSelected,
        onExplainSpend = onExplainSpend,
        onOpenReview = onOpenReview,
        onRequestSmsPermission = onRequestSmsPermission,
        onTransactionClick = onTransactionClick
    )
}

private data class TapePalette(
    val desk: Color,
    val tape: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkFaint: Color,
    val rule: Color,
    val ruleFaint: Color,
    val amber: Color,
    val query: Color,
    val held: Color,
    val credit: Color
)

private data class TapeDayGroup(
    val dateKey: String,
    val label: String,
    val transactions: List<TransactionUi>,
    val spendSubtotal: Double
)

@Composable
private fun tapePalette(): TapePalette {
    return if (isDarkModeActive()) {
        TapePalette(
            desk = Color(0xFF0A0A0C),
            tape = Color(0xFF17150F),
            ink = Color(0xFFE7DCC0),
            inkSoft = Color(0xA6E7DCC0),
            inkFaint = Color(0x66E7DCC0),
            rule = Color(0x4DE7DCC0),
            ruleFaint = Color(0x2EE7DCC0),
            amber = Color(0xFFF2C14E),
            query = Color(0xFFD98B6A),
            held = Color(0xFF9AA79A),
            credit = Color(0xFF7FBF9A)
        )
    } else {
        TapePalette(
            desk = Color(0xFFE8DFCB),
            tape = Color(0xFFFFFDF6),
            ink = Color(0xFF2A2419),
            inkSoft = Color(0xA62A2419),
            inkFaint = Color(0x782A2419),
            rule = Color(0x522A2419),
            ruleFaint = Color(0x382A2419),
            amber = Color(0xFFA86A06),
            query = Color(0xFFB4501A),
            held = Color(0xFF4F5C4F),
            credit = Color(0xFF0F7250)
        )
    }
}

@Composable
private fun TapeHome(
    feedState: FeedState,
    months: List<String>,
    selectedMonthKey: String?,
    modifier: Modifier,
    onSettings: () -> Unit,
    onOpenSync: () -> Unit,
    onMonthSelected: (String) -> Unit,
    onExplainSpend: () -> Unit,
    onOpenReview: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    onTransactionClick: (TransactionUi) -> Unit
) {
    val palette = tapePalette()
    val monthTransactions = remember(feedState.transactions, selectedMonthKey) {
        feedState.transactions.latestMonthTransactions(selectedMonthKey)
            .filter { it.inrAmountValue != null }
    }
    val breakdown = remember(feedState.transactions, selectedMonthKey) {
        feedState.transactions.monthBreakdown(selectedMonthKey)
    }
    val reviewRows = remember(feedState.transactions, selectedMonthKey) {
        feedState.transactions.reviewCandidates(selectedMonthKey)
    }
    val dayGroups = remember(monthTransactions) {
        monthTransactions
            .sortedWith(
                compareByDescending<TransactionUi> { it.transactionDate.orEmpty() }
                    .thenByDescending { it.inrAmountValue ?: 0.0 }
            )
            .groupBy { it.transactionDate ?: "unknown" }
            .map { (dateKey, rows) ->
                TapeDayGroup(
                    dateKey = dateKey,
                    label = dateKey.recentDateLabel(),
                    transactions = rows,
                    spendSubtotal = rows
                        .filter { it.direction == DirectionUi.Debit && it.transactionType.countsAsSpend() }
                        .sumOf { it.inrAmountValue ?: 0.0 }
                )
            }
    }
    val sampleFallback = feedState.needsSmsPermission && feedState.label == "sample SMS"
    val sourceLabel = remember(monthTransactions, feedState) {
        monthTransactions.tapeSourceReceipt(feedState)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.desk)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TapeDeskBar(
                sourceLabel = if (sampleFallback) "NO SOURCE CONNECTED" else sourceLabel,
                palette = palette,
                onOpenSync = onOpenSync,
                onSettings = onSettings
            )
            TapePaper(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                palette = palette
            ) {
                if (sampleFallback || monthTransactions.isEmpty()) {
                    item {
                        TapeMonthSelector(
                            months = months,
                            selectedMonthKey = selectedMonthKey,
                            palette = palette,
                            onMonthSelected = onMonthSelected
                        )
                    }
                    item {
                        EmptyTapeState(
                            needsPermission = feedState.needsSmsPermission,
                            palette = palette,
                            onRequestSmsPermission = onRequestSmsPermission
                        )
                    }
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                } else {
                    item {
                        TapeMonthSelector(
                            months = months,
                            selectedMonthKey = selectedMonthKey,
                            palette = palette,
                            onMonthSelected = onMonthSelected
                        )
                    }
                    item {
                        TapeCloseOutBlock(
                            breakdown = breakdown,
                            reviewRows = reviewRows,
                            palette = palette,
                            onExplainSpend = onExplainSpend
                        )
                    }
                    if (reviewRows.isNotEmpty()) {
                        item {
                            TapeQueryStrip(
                                count = reviewRows.size,
                                amount = reviewRows.sumOf { it.inrAmountValue ?: 0.0 },
                                palette = palette,
                                onClick = onOpenReview
                            )
                        }
                    }
                    items(dayGroups, key = { it.dateKey }) { group ->
                        TapeDaySection(
                            group = group,
                            palette = palette,
                            onTransactionClick = onTransactionClick
                        )
                    }
                    item {
                        TapeSourceFooter(
                            sourceLabel = sourceLabel,
                            palette = palette
                        )
                    }
                    item { Spacer(modifier = Modifier.height(92.dp)) }
                }
            }
        }

        if (!sampleFallback && monthTransactions.isNotEmpty()) {
            TapePinnedStrip(
                breakdown = breakdown,
                reviewRows = reviewRows,
                palette = palette,
                modifier = Modifier.align(Alignment.BottomCenter),
                onExplainSpend = onExplainSpend,
                onOpenReview = onOpenReview
            )
        }
    }
}

@Composable
private fun TapeDeskBar(
    sourceLabel: String,
    palette: TapePalette,
    onOpenSync: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(start = 15.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "SORTED",
            color = palette.inkSoft,
            fontFamily = SortedTapeFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp,
            maxLines = 1
        )
        Text(
            text = sourceLabel.uppercase(Locale.US),
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onOpenSync)
                .padding(horizontal = 10.dp),
            color = palette.inkFaint,
            fontFamily = SortedTapeFontFamily,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 1.sp
        )
        IconButton(onClick = onSettings, modifier = Modifier.size(38.dp)) {
            SettingsGlyph(
                color = palette.inkFaint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun TapePaper(
    modifier: Modifier,
    palette: TapePalette,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    Column(
        modifier = modifier
            .background(palette.tape)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                drawLine(
                    color = palette.ruleFaint,
                    start = Offset(strokeWidth / 2f, 0f),
                    end = Offset(strokeWidth / 2f, size.height),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = palette.ruleFaint,
                    start = Offset(size.width - strokeWidth / 2f, 0f),
                    end = Offset(size.width - strokeWidth / 2f, size.height),
                    strokeWidth = strokeWidth
                )
            }
    ) {
        TapeTornEdge(palette = palette, top = true)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            content = content
        )
        TapeTornEdge(palette = palette, top = false)
    }
}

@Composable
private fun TapeTornEdge(
    palette: TapePalette,
    top: Boolean
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(palette.tape)
    ) {
        val teeth = 18
        val step = size.width / teeth
        val baseY = if (top) size.height else 0f
        for (i in 0 until teeth) {
            val x = i * step
            val peakY = if (top) 0f else size.height
            drawLine(
                color = palette.ruleFaint,
                start = Offset(x, baseY),
                end = Offset(x + step / 2f, peakY),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = palette.ruleFaint,
                start = Offset(x + step / 2f, peakY),
                end = Offset(x + step, baseY),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

@Composable
private fun TapeMonthSelector(
    months: List<String>,
    selectedMonthKey: String?,
    palette: TapePalette,
    onMonthSelected: (String) -> Unit
) {
    val selectedIndex = months.indexOf(selectedMonthKey).takeIf { it >= 0 } ?: 0
    val older = months.getOrNull(selectedIndex + 1)
    val newer = months.getOrNull(selectedIndex - 1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TapeArrowButton(
            label = "<",
            enabled = older != null,
            palette = palette,
            onClick = { older?.let(onMonthSelected) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        TapeStamp(
            text = selectedMonthKey?.monthStampLabel() ?: "NO MONTH",
            palette = palette,
            color = palette.amber
        )
        Spacer(modifier = Modifier.width(8.dp))
        TapeArrowButton(
            label = ">",
            enabled = newer != null,
            palette = palette,
            onClick = { newer?.let(onMonthSelected) }
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "SEARCH",
            color = palette.inkFaint,
            fontFamily = SortedTapeFontFamily,
            fontSize = 8.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun TapeArrowButton(
    label: String,
    enabled: Boolean,
    palette: TapePalette,
    onClick: () -> Unit
) {
    Text(
        text = label,
        modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(top = 3.dp),
        color = if (enabled) palette.inkSoft else palette.inkFaint.copy(alpha = 0.42f),
        fontFamily = SortedTapeFontFamily,
        fontSize = 15.sp,
        textAlign = TextAlign.Center,
        maxLines = 1
    )
}

@Composable
private fun TapeCloseOutBlock(
    breakdown: MonthBreakdown,
    reviewRows: List<TransactionUi>,
    palette: TapePalette,
    onExplainSpend: () -> Unit
) {
    val heldOut = (breakdown.totalDebits - breakdown.spends).coerceAtLeast(0.0)
    val reviewAmount = reviewRows.sumOf { it.inrAmountValue ?: 0.0 }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(1.dp, palette.rule)
            .clickable(onClick = onExplainSpend)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Text(
            text = "SPEND - MONTH TO DATE",
            color = palette.inkFaint,
            fontFamily = SortedTapeFontFamily,
            fontSize = 9.sp,
            letterSpacing = 1.5.sp,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = breakdown.spends.formatRupee(),
            color = palette.ink,
            fontFamily = SortedTapeFontFamily,
            fontSize = 31.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        TapeDoubleRule(palette = palette)
        TapeSummationRow(
            label = "${breakdown.spendCount} LINES INCLUDED",
            value = breakdown.spends.formatRupee(),
            palette = palette
        )
        TapeSummationRow(
            label = "${breakdown.debitCount - breakdown.spendCount} LINES HELD OUT",
            value = "(${heldOut.formatRupee()})",
            palette = palette
        )
        TapeSummationRow(
            label = "${reviewRows.size} LINES UNSTAMPED",
            value = reviewAmount.formatRupee(),
            palette = palette,
            color = palette.query
        )
        if (breakdown.refunds > 0.0) {
            TapeSummationRow(
                label = "REFUND SIGNALS",
                value = "NOT SUBTRACTED",
                palette = palette
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.ruleFaint)
                .padding(top = 10.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WHY THIS NUMBER",
                modifier = Modifier.weight(1f),
                color = palette.amber,
                fontFamily = SortedTapeFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.3.sp
            )
            Text(
                text = ">",
                color = palette.amber,
                fontFamily = SortedTapeFontFamily,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun TapeDoubleRule(palette: TapePalette) {
    Column(modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.rule)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.rule)
        )
    }
}

@Composable
private fun TapeSummationRow(
    label: String,
    value: String,
    palette: TapePalette,
    color: Color = palette.inkSoft
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = color,
            fontFamily = SortedTapeFontFamily,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.7.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            color = color,
            fontFamily = SortedTapeFontFamily,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun TapeQueryStrip(
    count: Int,
    amount: Double,
    palette: TapePalette,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .border(1.dp, palette.query)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TapeStamp(text = "?", palette = palette, color = palette.query)
        Spacer(modifier = Modifier.width(9.dp))
        Text(
            text = "$count LINES NEED A STAMP - ${amount.formatRupee()}",
            modifier = Modifier.weight(1f),
            color = palette.query,
            fontFamily = SortedTapeFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = ">",
            color = palette.query,
            fontFamily = SortedTapeFontFamily,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun TapeDaySection(
    group: TapeDayGroup,
    palette: TapePalette,
    onTransactionClick: (TransactionUi) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 0.dp, color = Color.Transparent)
                .padding(top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = group.label.uppercase(Locale.US),
                modifier = Modifier.weight(1f),
                color = palette.inkSoft,
                fontFamily = SortedTapeFontFamily,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.3.sp,
                maxLines = 1
            )
            Text(
                text = group.spendSubtotal.formatRupee(),
                color = palette.inkSoft,
                fontFamily = SortedTapeFontFamily,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.rule)
        )
        group.transactions.forEach { transaction ->
            TapeTransactionLine(
                transaction = transaction,
                palette = palette,
                onClick = { onTransactionClick(transaction) }
            )
        }
    }
}

@Composable
private fun TapeTransactionLine(
    transaction: TransactionUi,
    palette: TapePalette,
    onClick: () -> Unit
) {
    val style = transaction.tapeLineStyle(palette)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = style.glyph,
                modifier = Modifier.width(14.dp),
                color = style.accent,
                fontFamily = SortedTapeFontFamily,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = transaction.merchant.uppercase(Locale.US),
                modifier = Modifier.weight(1f),
                color = style.ink,
                fontFamily = SortedTapeFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.8.sp
            )
            Text(
                text = style.amount,
                color = style.ink,
                fontFamily = SortedTapeFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                textDecoration = style.textDecoration
            )
        }
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TapeStamp(
                text = style.stamp,
                palette = palette,
                color = style.accent,
                filled = style.filledStamp
            )
            Spacer(modifier = Modifier.width(6.dp))
            TapeStamp(
                text = transaction.source.uppercase(Locale.US),
                palette = palette,
                color = palette.inkFaint
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = style.note,
                color = palette.inkFaint,
                fontFamily = SortedTapeFontFamily,
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.5.sp
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 8.dp)
                .height(1.dp)
                .background(palette.ruleFaint)
        )
    }
}

private data class TapeLineStyle(
    val glyph: String,
    val stamp: String,
    val amount: String,
    val note: String,
    val accent: Color,
    val ink: Color,
    val filledStamp: Boolean,
    val textDecoration: androidx.compose.ui.text.style.TextDecoration? = null
)

private fun TransactionUi.tapeLineStyle(palette: TapePalette): TapeLineStyle {
    val value = inrAmountValue ?: amountValue
    val amountLabel = value.formatRupee()
    val review = needsReview()
    return when {
        review -> TapeLineStyle(
            glyph = "?",
            stamp = "?",
            amount = amountLabel,
            note = reviewReason().lowercase(Locale.US),
            accent = palette.query,
            ink = palette.ink,
            filledStamp = false
        )
        direction == DirectionUi.Credit -> TapeLineStyle(
            glyph = "+",
            stamp = transactionType.displayName().uppercase(Locale.US).take(8),
            amount = "+$amountLabel",
            note = "credit signal - not subtracted",
            accent = palette.credit,
            ink = palette.inkSoft,
            filledStamp = false
        )
        !transactionType.countsAsSpend() -> TapeLineStyle(
            glyph = "H",
            stamp = transactionType.displayName().uppercase(Locale.US).take(8),
            amount = amountLabel,
            note = "held out of spend",
            accent = palette.held,
            ink = palette.inkSoft,
            filledStamp = true,
            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
        )
        else -> TapeLineStyle(
            glyph = "",
            stamp = category.uppercase(Locale.US).take(9),
            amount = amountLabel,
            note = listOf(paymentMode, transactionDate.orEmpty()).filter(String::isNotBlank).joinToString(" - "),
            accent = palette.inkSoft,
            ink = palette.ink,
            filledStamp = false
        )
    }
}

@Composable
private fun TapeStamp(
    text: String,
    palette: TapePalette,
    color: Color,
    filled: Boolean = false
) {
    Text(
        text = text,
        modifier = Modifier
            .border(1.dp, color, RoundedCornerShape(2.dp))
            .background(if (filled) color else Color.Transparent, RoundedCornerShape(2.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        color = if (filled) palette.tape else color,
        fontFamily = SortedTapeFontFamily,
        fontSize = 8.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        letterSpacing = 0.8.sp
    )
}

@Composable
private fun TapeSourceFooter(
    sourceLabel: String,
    palette: TapePalette
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        TapeDoubleRule(palette = palette)
        Text(
            text = sourceLabel.uppercase(Locale.US),
            color = palette.inkFaint,
            fontFamily = SortedTapeFontFamily,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun TapePinnedStrip(
    breakdown: MonthBreakdown,
    reviewRows: List<TransactionUi>,
    palette: TapePalette,
    modifier: Modifier,
    onExplainSpend: () -> Unit,
    onOpenReview: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.tape)
            .border(1.dp, palette.ruleFaint)
            .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = breakdown.spends.formatRupee(),
            color = palette.ink,
            fontFamily = SortedTapeFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${breakdown.spendCount} IN - ${breakdown.debitCount - breakdown.spendCount} OUT",
            modifier = Modifier.weight(1f),
            color = palette.inkFaint,
            fontFamily = SortedTapeFontFamily,
            fontSize = 8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.7.sp
        )
        if (reviewRows.isNotEmpty()) {
            Text(
                text = "? ${reviewRows.size}",
                modifier = Modifier
                    .border(1.dp, palette.query, RoundedCornerShape(2.dp))
                    .clickable(onClick = onOpenReview)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                color = palette.query,
                fontFamily = SortedTapeFontFamily,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = "WHY?",
            modifier = Modifier
                .border(1.dp, palette.amber, RoundedCornerShape(2.dp))
                .clickable(onClick = onExplainSpend)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            color = palette.amber,
            fontFamily = SortedTapeFontFamily,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun EmptyTapeState(
    needsPermission: Boolean,
    palette: TapePalette,
    onRequestSmsPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 18.dp)
            .border(1.dp, palette.rule)
            .padding(horizontal = 18.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TapeDoubleRule(palette = palette)
        Text(
            text = "NO LINES YET",
            color = palette.inkSoft,
            fontFamily = SortedTapeFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(22.dp))
        Text(
            text = "Sorted prints a line for every transaction alert on this phone. Nothing is uploaded, and no account is linked.",
            color = palette.inkSoft,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center
        )
        if (needsPermission) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "ALLOW SORTED TO READ SMS",
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.amber)
                    .clickable(onClick = onRequestSmsPermission)
                    .padding(vertical = 13.dp),
                color = palette.amber,
                fontFamily = SortedTapeFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "or add a line by hand",
            color = palette.inkFaint,
            fontFamily = SortedTapeFontFamily,
            fontSize = 9.sp,
            letterSpacing = 1.sp
        )
    }
}

private fun List<TransactionUi>.tapeSourceReceipt(feedState: FeedState): String {
    if (isEmpty()) {
        return if (feedState.needsSmsPermission) "NO SOURCE CONNECTED" else feedState.label
    }
    val counts = groupBy { it.source.uppercase(Locale.US) }
        .entries
        .sortedBy { it.key }
        .joinToString(" - ") { "${it.value.size} ${it.key}" }
    val suffix = if (feedState.needsSmsPermission) " - SMS OFF" else " - SYNC 2M"
    return counts + suffix
}

private fun String.monthStampLabel(): String {
    val year = substringBefore("-", "")
    val month = monthNameLabel().uppercase(Locale.US)
    return if (year.length == 4) "$month $year" else month
}

@Composable
private fun InsightsTabContent(
    feedState: FeedState,
    selectedMonthKey: String?,
    modifier: Modifier,
    onSettings: () -> Unit,
    onMerchantClick: (SummaryGroup) -> Unit,
    onCategoryClick: (SummaryGroup) -> Unit,
    onTransactionClick: (TransactionUi) -> Unit,
    onOpenReview: () -> Unit
) {
    val palette = tapePalette()
    val months = remember(feedState.transactions) { feedState.transactions.availableMonthKeys() }
    val activeMonthKey = selectedMonthKey?.takeIf { it in months } ?: feedState.transactions.selectedMonthKey()
    val previousMonthKey = remember(months, activeMonthKey) {
        val index = months.indexOf(activeMonthKey)
        months.getOrNull(index + 1)
    }
    val breakdown = remember(feedState.transactions, activeMonthKey) {
        feedState.transactions.monthBreakdown(activeMonthKey)
    }
    val previousBreakdown = remember(feedState.transactions, previousMonthKey) {
        previousMonthKey?.let { feedState.transactions.monthBreakdown(it) }
    }
    val monthRows = remember(feedState.transactions, activeMonthKey) {
        feedState.transactions.latestMonthTransactions(activeMonthKey)
            .filter { it.inrAmountValue != null }
    }
    val categories = remember(feedState.transactions, activeMonthKey) {
        feedState.transactions.monthSpendCategoryGroups(activeMonthKey)
    }
    val merchants = remember(feedState.transactions, activeMonthKey) {
        feedState.transactions.monthSpendMerchantGroups(activeMonthKey)
    }
    val reviewRows = remember(feedState.transactions, activeMonthKey) {
        feedState.transactions.reviewCandidates(activeMonthKey)
    }
    val refundRows = remember(monthRows) {
        monthRows
            .filter { it.direction == DirectionUi.Credit }
            .filter { row ->
                row.transactionType == TransactionType.REFUND ||
                    row.transactionType == TransactionType.REWARD ||
                    row.category == "Refund" ||
                    row.category == "Reward" ||
                    row.detail.contains("refund", ignoreCase = true) ||
                    row.detail.contains("reversal", ignoreCase = true) ||
                    row.detail.contains("cashback", ignoreCase = true)
            }
            .sortedByDescending { it.inrAmountValue ?: 0.0 }
    }
    val recurringRows = remember(feedState.transactions, activeMonthKey) {
        feedState.transactions
            .filter { it.isInSelectedMonth(activeMonthKey) }
            .recurringCandidates()
    }
    val sourceRows = remember(monthRows) {
        monthRows
            .groupBy { it.source }
            .map { (source, rows) ->
                SourceHealthRow(
                    source = source,
                    totalCount = rows.size,
                    spendCount = rows.count { it.direction == DirectionUi.Debit && it.transactionType.countsAsSpend() },
                    reviewCount = rows.count(TransactionUi::needsReview),
                    fxCount = rows.count { !it.countsInInrTotals() },
                    totalAmount = rows
                        .filter { it.direction == DirectionUi.Debit }
                        .sumOf { it.inrAmountValue ?: 0.0 }
                )
            }
            .sortedByDescending { it.totalCount }
    }
    var activeSection by remember { mutableStateOf("WHERE") }
    val tabs = listOf("WHERE", "WHO", "REPEATS", "CHANGED", "HELD", "?", "SOURCES")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.desk)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            IndexDeskBar(
                monthKey = activeMonthKey,
                indexedCount = monthRows.size,
                palette = palette,
                onSettings = onSettings
            )
            IndexTabRail(
                tabs = tabs,
                activeTab = activeSection,
                palette = palette,
                onTabSelected = { activeSection = it }
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(palette.tape)
                    .drawBehind {
                        val strokeWidth = 1.dp.toPx()
                        drawLine(
                            color = palette.ruleFaint,
                            start = Offset(strokeWidth / 2f, 0f),
                            end = Offset(strokeWidth / 2f, size.height),
                            strokeWidth = strokeWidth
                        )
                        drawLine(
                            color = palette.ruleFaint,
                            start = Offset(size.width - strokeWidth / 2f, 0f),
                            end = Offset(size.width - strokeWidth / 2f, size.height),
                            strokeWidth = strokeWidth
                        )
                    },
                content = {
                    item {
                        IndexSummaryBlock(
                            breakdown = breakdown,
                            indexedCount = monthRows.size,
                            merchantCount = merchants.size,
                            categoryCount = categories.size,
                            reviewRows = reviewRows,
                            palette = palette,
                            onOpenReview = onOpenReview
                        )
                    }
                    item {
                        IndexShareRule(
                            groups = categories,
                            total = breakdown.spends,
                            palette = palette,
                            onGroupClick = onCategoryClick
                        )
                    }
                    if (activeSection == "WHERE" || activeSection == "?") {
                        item {
                            IndexGroupBlock(
                                heading = "WHERE IT WENT",
                                meta = "${categories.size} CATEGORIES",
                                groups = categories.take(if (activeSection == "WHERE") 8 else 4),
                                palette = palette,
                                emptyLabel = "NO CATEGORIES PRINTED YET",
                                onGroupClick = onCategoryClick
                            )
                        }
                    }
                    if (activeSection == "WHO" || activeSection == "?") {
                        item {
                            IndexGroupBlock(
                                heading = "WHO TOOK IT",
                                meta = "${merchants.size} MERCHANTS",
                                groups = merchants.take(8),
                                palette = palette,
                                emptyLabel = "NO MERCHANTS PRINTED YET",
                                onGroupClick = onMerchantClick
                            )
                        }
                    }
                    if (activeSection == "REPEATS") {
                        item {
                            IndexRepeatsBlock(
                                candidates = recurringRows,
                                palette = palette
                            )
                        }
                    }
                    if (activeSection == "CHANGED") {
                        item {
                            IndexChangedBlock(
                                activeMonthKey = activeMonthKey,
                                previousMonthKey = previousMonthKey,
                                breakdown = breakdown,
                                previousBreakdown = previousBreakdown,
                                categories = categories,
                                previousCategories = feedState.transactions.monthSpendCategoryGroups(previousMonthKey),
                                palette = palette
                            )
                        }
                    }
                    if (activeSection == "HELD") {
                        item {
                            IndexHeldBlock(
                                breakdown = breakdown,
                                palette = palette
                            )
                        }
                        item {
                            IndexRefundBlock(
                                refunds = refundRows,
                                palette = palette,
                                onTransactionClick = onTransactionClick
                            )
                        }
                    }
                    if (activeSection == "?") {
                        item {
                            IndexUnstampedBlock(
                                reviewRows = reviewRows,
                                palette = palette,
                                onTransactionClick = onTransactionClick,
                                onOpenReview = onOpenReview
                            )
                        }
                    }
                    if (activeSection == "SOURCES") {
                        item {
                            IndexSourcesBlock(
                                rows = sourceRows,
                                palette = palette
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(112.dp))
                    }
                }
            )
        }
        IndexPinnedStrip(
            breakdown = breakdown,
            reviewRows = reviewRows,
            palette = palette,
            modifier = Modifier.align(Alignment.BottomCenter),
            onOpenReview = onOpenReview
        )
    }
}

@Composable
private fun IndexDeskBar(
    monthKey: String?,
    indexedCount: Int,
    palette: TapePalette,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(start = 15.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "INDEX",
            color = palette.inkSoft,
            fontFamily = SortedTapeFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp,
            maxLines = 1
        )
        Text(
            text = "${monthKey?.monthStampLabel() ?: "CURRENT"} - $indexedCount LINES INDEXED",
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
            color = palette.inkFaint,
            fontFamily = SortedTapeFontFamily,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 1.sp
        )
        IconButton(onClick = onSettings, modifier = Modifier.size(38.dp)) {
            SettingsGlyph(
                color = palette.inkFaint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun IndexTabRail(
    tabs: List<String>,
    activeTab: String,
    palette: TapePalette,
    onTabSelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, bottom = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        items(tabs) { tab ->
            val active = tab == activeTab
            Box(
                modifier = Modifier
                    .border(
                        width = if (active) 2.dp else 1.dp,
                        color = if (active) palette.amber else palette.rule
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(
                    text = tab,
                    color = if (active) palette.amber else palette.inkFaint,
                    fontFamily = SortedTapeFontFamily,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun IndexSummaryBlock(
    breakdown: MonthBreakdown,
    indexedCount: Int,
    merchantCount: Int,
    categoryCount: Int,
    reviewRows: List<TransactionUi>,
    palette: TapePalette,
    onOpenReview: () -> Unit
) {
    val heldOut = (breakdown.totalDebits - breakdown.spends + breakdown.income + breakdown.rewards + breakdown.refunds)
        .coerceAtLeast(0.0)
    val reviewAmount = reviewRows.sumOf { it.inrAmountValue ?: 0.0 }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp)
            .border(1.dp, palette.rule)
            .padding(start = 13.dp, end = 13.dp, top = 12.dp, bottom = 2.dp)
    ) {
        Text(
            text = "INDEXED FROM THE TAPE",
            color = palette.inkFaint,
            fontFamily = SortedTapeFontFamily,
            fontSize = 9.sp,
            letterSpacing = 2.sp
        )
        Text(
            text = breakdown.spends.formatRupee(),
            modifier = Modifier.padding(top = 7.dp),
            color = palette.ink,
            fontFamily = SortedTapeFontFamily,
            fontSize = 31.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        TapeDoubleRule(palette = palette)
        TapeSummationRow(" $indexedCount LINES INDEXED", breakdown.spends.formatRupee(), palette)
        TapeSummationRow(" $merchantCount MERCHANTS - $categoryCount CATEGORIES", "", palette)
        TapeSummationRow(" ${breakdown.debitCount - breakdown.spendCount} LINES HELD OUT", "(${heldOut.formatRupee()})", palette)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenReview)
                .padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = " ${reviewRows.size} LINES UNSTAMPED",
                modifier = Modifier.weight(1f),
                color = palette.query,
                fontFamily = SortedTapeFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Text(
                text = reviewAmount.formatRupee(),
                color = palette.query,
                fontFamily = SortedTapeFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        TapeDoubleRule(palette = palette)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenReview)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WHY THIS INDEX",
                modifier = Modifier.weight(1f),
                color = palette.amber,
                fontFamily = SortedTapeFontFamily,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp
            )
            Text(
                text = ">",
                color = palette.amber,
                fontFamily = SortedTapeFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun IndexShareRule(
    groups: List<SummaryGroup>,
    total: Double,
    palette: TapePalette,
    onGroupClick: (SummaryGroup) -> Unit
) {
    if (groups.isEmpty() || total <= 0.0) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            groups.take(6).forEachIndexed { index, group ->
                val alpha = (0.82f - index * 0.10f).coerceAtLeast(0.24f)
                Box(
                    modifier = Modifier
                        .weight((group.total / total).toFloat().coerceAtLeast(0.03f))
                        .height(6.dp)
                        .background(palette.amber.copy(alpha = alpha))
                        .clickable { onGroupClick(group) }
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 7.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val first = groups.firstOrNull()
            val second = groups.getOrNull(1)
            Text(
                text = listOfNotNull(
                    first?.let { "${it.label.uppercase(Locale.US)} ${(it.total / total * 100).toInt()}%" },
                    second?.let { "${it.label.uppercase(Locale.US)} ${(it.total / total * 100).toInt()}%" }
                ).joinToString(" - "),
                modifier = Modifier.weight(1f),
                color = palette.inkFaint,
                fontFamily = SortedTapeFontFamily,
                fontSize = 8.sp,
                letterSpacing = 1.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "TAP A SEGMENT",
                color = palette.inkFaint,
                fontFamily = SortedTapeFontFamily,
                fontSize = 8.sp,
                letterSpacing = 1.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun IndexGroupBlock(
    heading: String,
    meta: String,
    groups: List<SummaryGroup>,
    palette: TapePalette,
    emptyLabel: String,
    onGroupClick: (SummaryGroup) -> Unit
) {
    IndexBlockShell(heading = heading, meta = meta, palette = palette) {
        if (groups.isEmpty()) {
            IndexEmptyLine(emptyLabel, palette)
        } else {
            groups.forEach { group ->
                IndexEntryRow(
                    name = group.label.uppercase(Locale.US),
                    meta = "${group.count} LINE${if (group.count == 1) "" else "S"}",
                    value = group.total.formatRupee(),
                    palette = palette,
                    onClick = { onGroupClick(group) }
                )
            }
            IndexBlockFoot("INDEXED TOTAL", groups.sumOf { it.total }.formatRupee(), palette)
        }
    }
}

@Composable
private fun IndexRepeatsBlock(
    candidates: List<RecurringCandidate>,
    palette: TapePalette
) {
    IndexBlockShell(
        heading = "WHAT REPEATS",
        meta = "${candidates.size} SIGNALS",
        palette = palette
    ) {
        IndexCalendarStrip(
            days = candidates.mapNotNull { it.lastSeenDate?.takeLast(2)?.toIntOrNull() },
            palette = palette
        )
        if (candidates.isEmpty()) {
            IndexEmptyLine("NOT ENOUGH LINES TO INDEX REPEATS YET", palette)
        } else {
            candidates.take(6).forEach { candidate ->
                IndexEntryRow(
                    name = candidate.merchant.uppercase(Locale.US),
                    meta = "${candidate.lastSeenDate?.takeLast(2) ?: "--"}TH - ${candidate.count} SEEN - ${candidate.confidenceLabel.uppercase(Locale.US)}",
                    value = candidate.expectedAmount.formatRupee(),
                    palette = palette,
                    delta = if (candidate.transactionType == TransactionType.INVESTMENT) "SIP" else null
                )
            }
            IndexBlockFoot("RECURRING TOTAL - INSIDE SPEND", candidates.sumOf { it.expectedAmount }.formatRupee(), palette)
        }
    }
}

@Composable
private fun IndexChangedBlock(
    activeMonthKey: String?,
    previousMonthKey: String?,
    breakdown: MonthBreakdown,
    previousBreakdown: MonthBreakdown?,
    categories: List<SummaryGroup>,
    previousCategories: List<SummaryGroup>,
    palette: TapePalette
) {
    val change = breakdown.spends - (previousBreakdown?.spends ?: 0.0)
    IndexBlockShell(
        heading = "WHAT CHANGED",
        meta = previousMonthKey?.let { "VS ${it.monthStampLabel()}" } ?: "NO PRIOR TAPE",
        palette = palette
    ) {
        if (previousBreakdown == null) {
            IndexEmptyLine("NO PREVIOUS TAPE TO COMPARE", palette)
            return@IndexBlockShell
        }
        IndexEntryRow(
            name = activeMonthKey?.monthStampLabel() ?: "CURRENT",
            meta = "${breakdown.spendCount} LINES",
            value = breakdown.spends.formatRupee(),
            palette = palette
        )
        IndexEntryRow(
            name = previousMonthKey?.monthStampLabel() ?: "PREVIOUS",
            meta = "${previousBreakdown.spendCount} LINES",
            value = previousBreakdown.spends.formatRupee(),
            palette = palette,
            dim = true
        )
        IndexEntryRow(
            name = "CHANGE",
            meta = "",
            value = if (change < 0) "(${(-change).formatRupee()})" else change.formatRupee(),
            palette = palette,
            delta = if (previousBreakdown.spends > 0.0) {
                val pct = change / previousBreakdown.spends * 100.0
                "${if (pct >= 0) "+" else ""}${pct.toInt()}%"
            } else {
                "NEW"
            },
            query = change > 0
        )
        val previousByLabel = previousCategories.associateBy { it.label }
        categories.take(4).forEach { group ->
            val old = previousByLabel[group.label]?.total ?: 0.0
            val deltaValue = group.total - old
            IndexEntryRow(
                name = group.label.uppercase(Locale.US),
                meta = if (old == 0.0) "FIRST SEEN" else "${group.count} LINES",
                value = if (deltaValue < 0) "(${(-deltaValue).formatRupee()})" else deltaValue.formatRupee(),
                palette = palette,
                delta = if (old == 0.0) "NEW" else {
                    val pct = deltaValue / old * 100.0
                    "${if (pct >= 0) "+" else ""}${pct.toInt()}%"
                },
                query = deltaValue > 0
            )
        }
    }
}

@Composable
private fun IndexHeldBlock(
    breakdown: MonthBreakdown,
    palette: TapePalette
) {
    IndexBlockShell(
        heading = "HELD OUT OF SPEND",
        meta = "${breakdown.debitCount - breakdown.spendCount} LINES",
        palette = palette
    ) {
        val rows = listOf(
            Triple("MOVED", "TRANSFERS", breakdown.transfers),
            Triple("INVESTED", "ORDERS", breakdown.investments),
            Triple("INCOME", "CREDITS", breakdown.income),
            Triple("REWARDS", "CREDITS", breakdown.rewards)
        ).filter { it.third > 0.0 }
        if (rows.isEmpty()) {
            IndexEmptyLine("NO HELD-OUT MONEY PRINTED", palette)
        } else {
            rows.forEach { (name, meta, value) ->
                IndexEntryRow(
                    mark = if (name == "INCOME" || name == "REWARDS") "↓" else "⤴",
                    name = name,
                    meta = meta,
                    value = value.formatRupee(),
                    palette = palette
                )
            }
            IndexBlockFoot("HELD OUT TOTAL", rows.sumOf { it.third }.formatRupee(), palette)
        }
    }
}

@Composable
private fun IndexRefundBlock(
    refunds: List<TransactionUi>,
    palette: TapePalette,
    onTransactionClick: (TransactionUi) -> Unit
) {
    IndexBlockShell(
        heading = "REFUND SIGNALS",
        meta = "${refunds.size} FOUND",
        palette = palette
    ) {
        if (refunds.isEmpty()) {
            IndexEmptyLine("NO REFUND SIGNALS THIS MONTH", palette)
        } else {
            refunds.take(4).forEach { refund ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTransactionClick(refund) }
                        .padding(vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(34.dp)
                            .border(1.dp, palette.credit)
                    )
                    Column(modifier = Modifier.padding(start = 9.dp).weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IndexEntryLine(
                                name = refund.merchant.uppercase(Locale.US),
                                meta = refund.transactionDate.orEmpty(),
                                value = "(${(refund.inrAmountValue ?: 0.0).formatRupee()})",
                                palette = palette,
                                valueColor = palette.credit
                            )
                        }
                        Text(
                            text = "POSSIBLE REFUND - NOT SUBTRACTED",
                            color = palette.inkFaint,
                            fontFamily = SortedTapeFontFamily,
                            fontSize = 8.sp,
                            letterSpacing = 1.sp,
                            maxLines = 1
                        )
                    }
                }
            }
            IndexBlockFoot("SIGNALLED - NOT SUBTRACTED", refunds.sumOf { it.inrAmountValue ?: 0.0 }.formatRupee(), palette)
        }
    }
}

@Composable
private fun IndexUnstampedBlock(
    reviewRows: List<TransactionUi>,
    palette: TapePalette,
    onTransactionClick: (TransactionUi) -> Unit,
    onOpenReview: () -> Unit
) {
    IndexBlockShell(
        heading = "NEEDS A STAMP",
        meta = "${reviewRows.size} LINES",
        palette = palette
    ) {
        if (reviewRows.isEmpty()) {
            IndexEmptyLine("NOTHING TO STAMP", palette)
        } else {
            reviewRows.take(6).forEach { transaction ->
                IndexEntryRow(
                    mark = "?",
                    name = transaction.merchant.uppercase(Locale.US),
                    meta = transaction.reviewReason().uppercase(Locale.US),
                    value = (transaction.inrAmountValue ?: 0.0).formatRupee(),
                    palette = palette,
                    query = true,
                    onClick = { onTransactionClick(transaction) }
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenReview)
                    .padding(top = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OPEN SORT INBOX",
                    modifier = Modifier.weight(1f),
                    color = palette.amber,
                    fontFamily = SortedTapeFontFamily,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Text(">", color = palette.amber, fontFamily = SortedTapeFontFamily, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun IndexSourcesBlock(
    rows: List<SourceHealthRow>,
    palette: TapePalette
) {
    IndexBlockShell(
        heading = "SOURCE HEALTH",
        meta = "ON DEVICE",
        palette = palette
    ) {
        if (rows.isEmpty()) {
            IndexEmptyLine("NO SOURCE LINES PRINTED", palette)
        } else {
            rows.forEach { row ->
                IndexEntryRow(
                    name = row.source.uppercase(Locale.US),
                    meta = "${row.totalCount} READ - ${row.spendCount} SPEND",
                    value = "${row.reviewCount} REVIEW",
                    palette = palette,
                    query = row.reviewCount > 0
                )
            }
            IndexBlockFoot("${rows.sumOf { it.totalCount }} MESSAGES READ - 0 UPLOADED", "", palette)
        }
    }
}

@Composable
private fun IndexBlockShell(
    heading: String,
    meta: String,
    palette: TapePalette,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 0.dp, color = Color.Transparent)
                .padding(top = 10.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(1.dp)
                    .background(palette.rule)
            )
            Text(
                text = " $heading",
                modifier = Modifier.weight(1f),
                color = palette.ink,
                fontFamily = SortedTapeFontFamily,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = meta,
                color = palette.inkFaint,
                fontFamily = SortedTapeFontFamily,
                fontSize = 8.sp,
                letterSpacing = 1.sp,
                maxLines = 1
            )
        }
        content()
    }
}

@Composable
private fun IndexEntryRow(
    name: String,
    meta: String,
    value: String,
    palette: TapePalette,
    mark: String = "",
    delta: String? = null,
    dim: Boolean = false,
    query: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val rowModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(rowModifier)
            .drawBehind {
                val y = size.height - 1.dp.toPx()
                drawLine(
                    color = palette.ruleFaint,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = mark,
            modifier = Modifier.width(if (mark.isBlank()) 0.dp else 14.dp),
            color = if (query) palette.query else palette.inkFaint,
            fontFamily = SortedTapeFontFamily,
            fontSize = 10.sp,
            maxLines = 1
        )
        IndexEntryLine(
            name = name,
            meta = meta,
            value = value,
            palette = palette,
            valueColor = if (query) palette.query else if (dim) palette.inkSoft else palette.ink,
            delta = delta,
            dim = dim
        )
    }
}

@Composable
private fun RowScope.IndexEntryLine(
    name: String,
    meta: String,
    value: String,
    palette: TapePalette,
    valueColor: Color = palette.ink,
    delta: String? = null,
    dim: Boolean = false
) {
    Text(
        text = name,
        modifier = Modifier.widthIn(max = 136.dp),
        color = if (dim) palette.inkSoft else palette.ink,
        fontFamily = SortedTapeFontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
    Box(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 7.dp)
            .height(1.dp)
            .background(palette.ruleFaint)
    )
    Text(
        text = meta,
        modifier = Modifier.widthIn(max = 82.dp),
        color = palette.inkFaint,
        fontFamily = SortedTapeFontFamily,
        fontSize = 8.sp,
        letterSpacing = 1.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
        text = value,
        color = valueColor,
        fontFamily = SortedTapeFontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1
    )
    if (delta != null) {
        Text(
            text = " $delta",
            modifier = Modifier.width(42.dp),
            color = if (delta.startsWith("+") || delta == "NEW") palette.query else if (delta.startsWith("-")) palette.credit else palette.inkFaint,
            fontFamily = SortedTapeFontFamily,
            fontSize = 8.sp,
            textAlign = TextAlign.End,
            maxLines = 1
        )
    }
}

@Composable
private fun IndexBlockFoot(
    label: String,
    value: String,
    palette: TapePalette
) {
    TapeDoubleRule(palette = palette)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = palette.ink,
            fontFamily = SortedTapeFontFamily,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            color = palette.ink,
            fontFamily = SortedTapeFontFamily,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun IndexCalendarStrip(
    days: List<Int>,
    palette: TapePalette
) {
    val marked = days.toSet()
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        for (row in 0 until 4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                for (col in 1..8) {
                    val day = row * 8 + col
                    if (day <= 31) {
                        val active = day in marked
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(18.dp)
                                .border(1.dp, if (active) palette.amber else palette.ruleFaint)
                                .background(if (active) palette.amber else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.toString(),
                                color = if (active) palette.tape else palette.inkFaint,
                                fontFamily = SortedTapeFontFamily,
                                fontSize = 8.sp,
                                maxLines = 1
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
        }
    }
}

@Composable
private fun IndexEmptyLine(
    label: String,
    palette: TapePalette
) {
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        color = palette.inkFaint,
        fontFamily = SortedTapeFontFamily,
        fontSize = 9.sp,
        letterSpacing = 1.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun IndexPinnedStrip(
    breakdown: MonthBreakdown,
    reviewRows: List<TransactionUi>,
    palette: TapePalette,
    modifier: Modifier,
    onOpenReview: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.tape)
            .border(1.dp, palette.ruleFaint)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = breakdown.spends.formatRupee(),
            color = palette.ink,
            fontFamily = SortedTapeFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Text(
            text = "  ${breakdown.spendCount} INDEXED",
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            color = palette.inkFaint,
            fontFamily = SortedTapeFontFamily,
            fontSize = 8.sp,
            letterSpacing = 1.sp,
            maxLines = 1
        )
        Box(
            modifier = Modifier
                .border(1.dp, palette.query)
                .clickable(onClick = onOpenReview)
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Text(
                text = "? ${reviewRows.size}",
                color = palette.query,
                fontFamily = SortedTapeFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SpendExplanationScreen(
    feedState: FeedState,
    onBack: () -> Unit,
    onTransactionClick: (TransactionUi) -> Unit,
    onOpenReview: () -> Unit
) {
    val breakdown = remember(feedState.transactions) { feedState.transactions.monthBreakdown() }
    val buckets = remember(feedState.transactions) { feedState.transactions.explainBuckets() }
    val reviewTransactions = remember(feedState.transactions) { feedState.transactions.reviewCandidates() }
    val sourceRows = remember(feedState.transactions) { feedState.transactions.sourceHealthRows() }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Header(
                    title = "Why this number",
                    onSettings = {},
                    onBack = onBack,
                    showActions = false
                )
            }
            item {
                SpendExplanationHero(breakdown = breakdown, feedLabel = feedState.label)
            }
            items(
                buckets.filter { it.count > 0 || it.title == "Included spend" },
                key = { it.title }
            ) { bucket ->
                ExplainBucketCard(
                    bucket = bucket,
                    onTransactionClick = onTransactionClick
                )
            }
            item {
                SourceHealthMiniCard(sourceRows = sourceRows)
            }
            if (reviewTransactions.isNotEmpty()) {
                item {
                    ReviewQueueCard(
                        transactions = reviewTransactions,
                        onTransactionClick = onTransactionClick,
                        onOpenInbox = onOpenReview
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun SpendExplanationHero(
    breakdown: MonthBreakdown,
    feedLabel: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .animateContentSize(
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            ),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = breakdown.monthKey?.monthSpendLabel() ?: "Current spend",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = breakdown.spends.formatInr(),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 31.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniMetric(
                    label = "Included",
                    value = "${breakdown.spendCount} spends",
                    modifier = Modifier.weight(1f)
                )
                MiniMetric(
                    label = "Source",
                    value = feedLabel,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniMetric(
                    label = "Total outflow",
                    value = breakdown.totalDebits.formatInr(),
                    modifier = Modifier.weight(1f)
                )
                MiniMetric(
                    label = "Excluded",
                    value = (breakdown.transfers + breakdown.investments).formatInr(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ExplainBucketCard(
    bucket: ExplainBucket,
    onTransactionClick: (TransactionUi) -> Unit
) {
    val previewRows = bucket.transactions
        .sortedByDescending { it.inrAmountValue ?: 0.0 }
        .take(5)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bucket.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = bucket.description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = 0.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = bucket.amount.formatInr(),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        letterSpacing = 0.sp
                    )
                    Text(
                        text = "${bucket.count} rows",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        letterSpacing = 0.sp
                    )
                }
            }
            if (previewRows.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                previewRows.forEachIndexed { index, transaction ->
                    RecentInlineTransactionRow(
                        transaction = transaction,
                        onClick = { onTransactionClick(transaction) }
                    )
                    if (index != previewRows.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SortInboxScreen(
    feedState: FeedState,
    onBack: () -> Unit,
    onTransactionClick: (TransactionUi) -> Unit
) {
    val reviewTransactions = remember(feedState.transactions) { feedState.transactions.reviewCandidates() }
    val filters = remember {
        listOf(
            ReviewFilter("All") { true },
            ReviewFilter("Other") { it.category == "Other" || it.miscCategory == "Uncategorized" },
            ReviewFilter("Confidence") { it.confidence < 0.70 || it.categorySource == CategorySource.FALLBACK },
            ReviewFilter("Merchant") { it.merchant.looksLikeRawPaymentHandle() },
            ReviewFilter("Gmail") { it.source == "Gmail" },
            ReviewFilter("FX") { !it.countsInInrTotals() }
        )
    }
    var selectedFilter by remember { mutableStateOf(filters.first().label) }
    val activeFilter = filters.firstOrNull { it.label == selectedFilter } ?: filters.first()
    val filteredTransactions = remember(reviewTransactions, selectedFilter) {
        reviewTransactions.filter(activeFilter.predicate)
    }
    val total = filteredTransactions.sumOf { it.inrAmountValue ?: 0.0 }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Header(
                    title = "Sort Inbox",
                    onSettings = {},
                    onBack = onBack,
                    showActions = false
                )
            }
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "${filteredTransactions.size} review rows",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = total.formatInr(),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            letterSpacing = 0.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ChoiceRail(
                            title = "Filter",
                            choices = filters.map { it.label },
                            selected = selectedFilter,
                            onSelected = { selectedFilter = it }
                        )
                    }
                }
            }
            if (filteredTransactions.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "Nothing to review",
                        detail = "Current month transactions look sorted."
                    )
                }
            } else {
                items(filteredTransactions, key = { it.sourceHash }) { transaction ->
                    ReviewCandidateRow(
                        transaction = transaction,
                        reason = transaction.reviewReason(),
                        onClick = { onTransactionClick(transaction) }
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun MonthStoryCard(items: List<MonthStoryItem>) {
    if (items.isEmpty()) return

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Month story",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items) { story ->
                    MonthStoryTile(item = story)
                }
            }
        }
    }
}

@Composable
private fun MonthStoryTile(item: MonthStoryItem) {
    Column(
        modifier = Modifier
            .width(152.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(categoryContainerColor(item.category))
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(categoryColor(item.category))
        )
        Spacer(modifier = Modifier.height(9.dp))
        Text(
            text = item.label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.sp
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = item.detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun RefundSignalsCard(
    transactions: List<TransactionUi>,
    onTransactionClick: (TransactionUi) -> Unit
) {
    val total = transactions.sumOf { it.inrAmountValue ?: 0.0 }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Refund signals",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${transactions.size} candidate${if (transactions.size == 1) "" else "s"}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        letterSpacing = 0.sp
                    )
                }
                Text(
                    text = total.formatInr(),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    letterSpacing = 0.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (transactions.isEmpty()) {
                Text(
                    text = "No refund signals this month",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    letterSpacing = 0.sp
                )
            } else {
                transactions.take(5).forEachIndexed { index, transaction ->
                    ReviewCandidateRow(
                        transaction = transaction,
                        reason = "Credit signal",
                        onClick = { onTransactionClick(transaction) }
                    )
                    if (index != transactions.take(5).lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurringRadarCard(candidates: List<RecurringCandidate>) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Recurring radar",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp
                )
                Text(
                    text = "${candidates.size} found",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    letterSpacing = 0.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (candidates.isEmpty()) {
                Text(
                    text = "No repeated patterns yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    letterSpacing = 0.sp
                )
            } else {
                candidates.take(5).forEach { candidate ->
                    RecurringCandidateRow(candidate = candidate)
                }
            }
        }
    }
}

@Composable
private fun RecurringCandidateRow(candidate: RecurringCandidate) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryMiniDot(candidate.category)
        Spacer(modifier = Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = candidate.merchant,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${candidate.transactionType.displayName()} • ${candidate.count} rows • ${candidate.lastSeenDate ?: "Unknown"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = candidate.expectedAmount.formatInr(),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                letterSpacing = 0.sp
            )
            Text(
                text = candidate.confidenceLabel,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                letterSpacing = 0.sp
            )
        }
    }
}

@Composable
private fun ReviewQueueCard(
    transactions: List<TransactionUi>,
    onTransactionClick: (TransactionUi) -> Unit,
    onOpenInbox: () -> Unit
) {
    val previewRows = transactions.take(6)
    val total = transactions.sumOf { it.inrAmountValue ?: 0.0 }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Unsorted",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = total.formatInr(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        maxLines = 1,
                        letterSpacing = 0.sp
                    )
                }
                Text(
                    text = "${transactions.size} rows",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (previewRows.isEmpty()) {
                Text(
                    text = "No review items",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    letterSpacing = 0.sp
                )
            } else {
                previewRows.forEachIndexed { index, transaction ->
                    ReviewCandidateRow(
                        transaction = transaction,
                        onClick = { onTransactionClick(transaction) }
                    )
                    if (index != previewRows.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            if (transactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = onOpenInbox) {
                    Text("Open Sort Inbox")
                }
            }
        }
    }
}

@Composable
private fun ReviewCandidateRow(
    transaction: TransactionUi,
    reason: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .clickable(onClick = onClick)
            .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryMiniDot(transaction.category)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.merchant,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = reason ?: "${transaction.miscCategory} • ${transaction.source}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = transaction.inrAmountValue?.formatInr() ?: transaction.amount,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun InsightPulseCard(
    breakdown: MonthBreakdown,
    transactions: List<TransactionUi>,
    feedLabel: String
) {
    val activeDays = transactions.mapNotNull { it.transactionDate }.map { it.takeLast(2) }.toSet().size
    val averageDebit = if (breakdown.debitCount > 0) breakdown.totalDebits / breakdown.debitCount else 0.0
    val largestDebit = transactions.maxByOrNull { it.inrAmountValue ?: 0.0 }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = breakdown.monthKey?.monthMovementLabel() ?: "Current movement",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = breakdown.totalDebits.formatInr(),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 29.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniMetric(
                    label = "Avg debit",
                    value = averageDebit.formatInr(),
                    modifier = Modifier.weight(1f)
                )
                MiniMetric(
                    label = "Active days",
                    value = activeDays.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniMetric(
                    label = "Largest",
                    value = largestDebit?.merchant ?: "None",
                    modifier = Modifier.weight(1f)
                )
                MiniMetric(
                    label = "Sources",
                    value = feedLabel,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MiniMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp
            )
        }
    }
}

@Composable
private fun InsightBreakdownCard(
    title: String,
    groups: List<SummaryGroup>,
    emptyLabel: String,
    onGroupClick: (SummaryGroup) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp
                )
                if (groups.isNotEmpty()) {
                    Text(
                        text = "${groups.size} groups",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (groups.isEmpty()) {
                Text(
                    text = emptyLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    letterSpacing = 0.sp
                )
            } else {
                groups.forEachIndexed { index, group ->
                    InsightGroupRow(
                        group = group,
                        rank = index + 1,
                        onClick = { onGroupClick(group) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightMixCard(
    sourceGroups: List<SummaryGroup>,
    paymentGroups: List<SummaryGroup>
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Mix",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Sources",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            sourceGroups.forEach { group ->
                CompactAmountRow(group = group)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Payment modes",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            paymentGroups.forEach { group ->
                CompactAmountRow(group = group)
            }
        }
    }
}

@Composable
private fun InsightGroupRow(
    group: SummaryGroup,
    rank: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = rank.toString().padStart(2, '0'),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.width(9.dp))
            CategoryMiniDot(group.category)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.label,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 0.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${group.count} transaction${if (group.count == 1) "" else "s"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 0.sp
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = group.total.formatInr(),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                letterSpacing = 0.sp
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun RecentSectionTitle(totalGroups: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Recent",
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp
        )
        Text(
            text = "$totalGroups days",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            maxLines = 1,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun RecentDateGroupCard(
    group: RecentDateGroup,
    expanded: Boolean,
    onToggle: () -> Unit,
    onTransactionClick: (TransactionUi) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onToggle)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.label,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = 0.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${group.transactions.size} transaction${if (group.transactions.size == 1) "" else "s"}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        letterSpacing = 0.sp
                    )
                }
                Text(
                    text = group.outflow.formatInr(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    letterSpacing = 0.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                ChevronGlyph(
                    expanded = expanded,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(animationSpec = tween(durationMillis = 160)) +
                    expandVertically(animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)),
                exit = fadeOut(animationSpec = tween(durationMillis = 120)) +
                    shrinkVertically(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    group.transactions.forEachIndexed { index, transaction ->
                        if (index > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                        RecentInlineTransactionRow(
                            transaction = transaction,
                            onClick = { onTransactionClick(transaction) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentInlineTransactionRow(
    transaction: TransactionUi,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryMiniDot(transaction.category)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.merchant,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = transaction.detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = if (transaction.direction == DirectionUi.Credit) "+${transaction.amount}" else transaction.amount,
            color = if (transaction.direction == DirectionUi.Credit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun ChevronGlyph(
    expanded: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.dp.toPx()
        val start = if (expanded) {
            Offset(size.width * 0.22f, size.height * 0.38f)
        } else {
            Offset(size.width * 0.30f, size.height * 0.22f)
        }
        val middle = if (expanded) {
            Offset(size.width * 0.50f, size.height * 0.66f)
        } else {
            Offset(size.width * 0.66f, size.height * 0.50f)
        }
        val end = if (expanded) {
            Offset(size.width * 0.78f, size.height * 0.38f)
        } else {
            Offset(size.width * 0.30f, size.height * 0.78f)
        }
        drawLine(color, start, middle, strokeWidth, StrokeCap.Round)
        drawLine(color, middle, end, strokeWidth, StrokeCap.Round)
    }
}

@Composable
private fun CompactAmountRow(group: SummaryGroup) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryMiniDot(group.category)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${group.label} (${group.count})",
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.sp
        )
        Text(
            text = group.total.formatInr(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun CaptureTabContent(
    feedState: FeedState,
    modifier: Modifier,
    saveState: ManualSaveState,
    onSettings: () -> Unit,
    onSave: (ManualTransactionDraft) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Header(title = "Capture", onSettings = onSettings)
        }
        item {
            ManualAddCard(
                feedState = feedState,
                saveState = saveState,
                onSave = onSave
            )
        }
        item {
            Spacer(modifier = Modifier.height(104.dp))
        }
    }
}

@Composable
private fun ManualAddCard(
    feedState: FeedState,
    saveState: ManualSaveState,
    onSave: (ManualTransactionDraft) -> Unit
) {
    var merchant by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var category by remember { mutableStateOf("Food") }
    var miscCategory by remember { mutableStateOf("Manual") }
    var paymentMode by remember { mutableStateOf(PaymentMode.UPI) }
    var transactionType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var direction by remember { mutableStateOf(Direction.DEBIT) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val categories = listOf(
        "Food",
        "Groceries",
        "Shopping",
        "Subscriptions",
        "Transport",
        "Utilities",
        "Health",
        "Entertainment",
        "Investment",
        "Transfer",
        "Income",
        "Refund",
        "Reward",
        "Other"
    )
    val paymentModes = listOf(
        PaymentMode.UPI,
        PaymentMode.CARD,
        PaymentMode.CASH,
        PaymentMode.WALLET,
        PaymentMode.BANK_TRANSFER,
        PaymentMode.NACH
    )
    val transactionTypes = listOf(
        TransactionType.EXPENSE,
        TransactionType.SUBSCRIPTION,
        TransactionType.TRANSFER,
        TransactionType.INVESTMENT,
        TransactionType.INCOME,
        TransactionType.REFUND,
        TransactionType.REWARD
    )
    val recentMerchants = remember(feedState.transactions) {
        feedState.transactions
            .filter { it.merchant != "Unknown" }
            .distinctBy { it.merchant.uppercase(Locale.US) }
            .take(10)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Add transaction",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (recentMerchants.isNotEmpty()) {
                ChoiceRail(
                    title = "Recent merchants",
                    choices = recentMerchants.map { it.merchant },
                    selected = merchant,
                    onSelected = { selectedMerchant ->
                        val template = recentMerchants.firstOrNull { it.merchant == selectedMerchant }
                        merchant = selectedMerchant
                        if (template != null) {
                            category = template.category
                            miscCategory = template.miscCategory
                            paymentMode = PaymentMode.entries.firstOrNull {
                                it.displayName() == template.paymentMode
                            } ?: paymentMode
                            transactionType = template.transactionType
                            direction = if (template.direction == DirectionUi.Credit) Direction.CREDIT else Direction.DEBIT
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectableChip(
                    label = "Debit",
                    selected = direction == Direction.DEBIT,
                    modifier = Modifier.weight(1f),
                    onClick = { direction = Direction.DEBIT }
                )
                SelectableChip(
                    label = "Credit",
                    selected = direction == Direction.CREDIT,
                    modifier = Modifier.weight(1f),
                    onClick = { direction = Direction.CREDIT }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = { value ->
                    amount = value.filter { it.isDigit() || it == '.' }.take(12)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Amount") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it.take(48) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Merchant") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = date,
                onValueChange = { date = it.take(10) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Date") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(14.dp))
            ChoiceRail(
                title = "Category",
                choices = categories,
                selected = category,
                onSelected = { selected ->
                    category = selected
                    if (selected == "Investment") transactionType = TransactionType.INVESTMENT
                    if (selected == "Transfer") transactionType = TransactionType.TRANSFER
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ChoiceRail(
                title = "Type",
                choices = transactionTypes.map { it.displayName() },
                selected = transactionType.displayName(),
                onSelected = { selected ->
                    transactionType = transactionTypes.first { it.displayName() == selected }
                    category = when (transactionType) {
                        TransactionType.INVESTMENT -> "Investment"
                        TransactionType.TRANSFER -> "Transfer"
                        TransactionType.INCOME -> "Income"
                        TransactionType.REFUND -> "Refund"
                        TransactionType.REWARD -> "Reward"
                        else -> category
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ChoiceRail(
                title = "Payment",
                choices = paymentModes.map { it.displayName() },
                selected = paymentMode.displayName(),
                onSelected = { selected ->
                    paymentMode = paymentModes.first { it.displayName() == selected }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = miscCategory,
                onValueChange = { miscCategory = it.take(36) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Merchant tag") },
                singleLine = true
            )
            val statusText = validationError ?: saveState.error ?: saveState.message
            if (statusText != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = statusText,
                    color = if (validationError == null && saveState.error == null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color(0xFFFF8E8E)
                    },
                    fontSize = 13.sp,
                    letterSpacing = 0.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        amount = ""
                        merchant = ""
                        date = LocalDate.now().toString()
                        category = "Food"
                        miscCategory = "Manual"
                        paymentMode = PaymentMode.UPI
                        transactionType = TransactionType.EXPENSE
                        direction = Direction.DEBIT
                        validationError = null
                    }
                ) {
                    Text("Clear")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = !saveState.isSaving,
                    onClick = {
                        val parsedAmount = amount.toDoubleOrNull()
                        validationError = when {
                            parsedAmount == null || parsedAmount <= 0.0 -> "Enter a valid amount."
                            merchant.isBlank() -> "Enter a merchant."
                            date.toLocalDateOrNull() == null -> "Use date as YYYY-MM-DD."
                            else -> null
                        }
                        if (validationError == null && parsedAmount != null) {
                            onSave(
                                ManualTransactionDraft(
                                    merchant = merchant.trim(),
                                    amount = parsedAmount,
                                    date = date,
                                    category = category,
                                    miscCategory = miscCategory.ifBlank { "Manual" },
                                    paymentMode = paymentMode,
                                    transactionType = transactionType,
                                    direction = direction
                                )
                            )
                        }
                    }
                ) {
                    Text(if (saveState.isSaving) "Saving" else "Save")
                }
            }
        }
    }
}

@Composable
private fun ChoiceRail(
    title: String,
    choices: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Column {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp
        )
        Spacer(modifier = Modifier.height(7.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(choices) { choice ->
                SelectableChip(
                    label = choice,
                    selected = choice == selected,
                    onClick = { onSelected(choice) }
                )
            }
        }
    }
}

@Composable
private fun SelectableChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun SourcesTabContent(
    feedState: FeedState,
    gmailState: GmailUiState,
    gmailSetupInfo: GmailSetupInfo,
    modifier: Modifier,
    onSettings: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    onImportGmail: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Header(title = "Sources", onSettings = onSettings)
        }
        item {
            SourceHealthCard(
                feedState = feedState,
                gmailState = gmailState
            )
        }
        if (feedState.needsSmsPermission) {
            item {
                PermissionPrompt(onRequestPermission = onRequestSmsPermission)
            }
        }
        item {
            GmailImportCard(
                state = gmailState,
                setupInfo = gmailSetupInfo,
                onImport = onImportGmail
            )
        }
        item {
            Spacer(modifier = Modifier.height(104.dp))
        }
    }
}

@Composable
private fun SettingsScreen(
    themeMode: AppThemeMode,
    feedState: FeedState,
    gmailState: GmailUiState,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onOpenRuleCenter: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Header(
                    title = "Settings",
                    onSettings = {},
                    onBack = onBack,
                    showActions = false
                )
            }
            item {
                ThemeSettingsCard(
                    selected = themeMode,
                    onSelected = onThemeModeChange
                )
            }
            item {
                LocalDataSettingsCard(feedState = feedState)
            }
            item {
                SourceSettingsCard(
                    feedState = feedState,
                    gmailState = gmailState
                )
            }
            item {
                RuleCenterEntryCard(onOpenRuleCenter = onOpenRuleCenter)
            }
            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
private fun ThemeSettingsCard(
    selected: AppThemeMode,
    onSelected: (AppThemeMode) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Appearance",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppThemeMode.entries.forEach { mode ->
                    ThemeModeTile(
                        mode = mode,
                        selected = mode == selected,
                        onClick = { onSelected(mode) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeModeTile(
    mode: AppThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        label = "theme_tile_container"
    )
    val titleColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val detailColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val swatchColor = if (selected && mode != AppThemeMode.Dark) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        mode.swatchColor()
    }

    Surface(
        onClick = onClick,
        modifier = modifier.height(74.dp),
        color = containerColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(swatchColor)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = mode.label,
                    color = titleColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 0.sp
                )
            }
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = mode.description,
                color = detailColor,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp
            )
        }
    }
}

private fun AppThemeMode.swatchColor(): Color {
    return when (this) {
        AppThemeMode.System -> Color(0xFF7C4DFF)
        AppThemeMode.Dark -> Color(0xFF000000)
        AppThemeMode.Light -> Color(0xFFFF4D8D)
    }
}

@Composable
private fun LocalDataSettingsCard(feedState: FeedState) {
    val sourceCounts = feedState.transactions.groupingBy { it.source }.eachCount().toSortedMap()
    val monthBreakdown = feedState.transactions.monthBreakdown()
    val reviewCount = feedState.transactions.reviewCandidates().size
    val sourceSummary = if (sourceCounts.isEmpty()) {
        "None"
    } else {
        sourceCounts.entries.joinToString(" / ") { (source, count) -> "$source $count" }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Local data",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SettingsMetricCell(
                    label = "Transactions",
                    value = feedState.transactions.size.toString(),
                    modifier = Modifier.weight(1f)
                )
                SettingsMetricCell(
                    label = "Current month",
                    value = monthBreakdown.monthKey ?: "Unknown",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SettingsMetricCell(
                    label = "Month spend",
                    value = monthBreakdown.spends.formatInr(),
                    modifier = Modifier.weight(1f)
                )
                SettingsMetricCell(
                    label = "Unsorted",
                    value = reviewCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            SettingsMetricCell(
                label = "Sources",
                value = sourceSummary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SettingsMetricCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun SourceSettingsCard(
    feedState: FeedState,
    gmailState: GmailUiState
) {
    val gmailLabel = if (gmailState.error != null) "Needs attention" else gmailState.label
    val autoSyncLabel = gmailState.autoSyncLabel
        ?.removePrefix("Auto sync: ")
        ?.replaceFirstChar { it.titlecase(Locale.getDefault()) }
        ?: "Manual"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Sources",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            SettingsInfoRow("SMS", if (feedState.needsSmsPermission) "Permission needed" else "Enabled")
            SettingsInfoRow("Gmail", gmailLabel)
            SettingsInfoRow("Auto sync", autoSyncLabel)
            SettingsInfoRow("Storage", "Local only")
        }
    }
}

@Composable
private fun RuleCenterEntryCard(onOpenRuleCenter: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onOpenRuleCenter),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Rule Center",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Learned merchant corrections and category overrides.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 0.sp
                )
            }
            Text(
                text = "Open",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )
        }
    }
}

@Composable
private fun RuleCenterScreen(
    modifier: Modifier = Modifier,
    onSettings: () -> Unit = {},
    onBack: (() -> Unit)?
) {
    val appContext = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var rules by remember { mutableStateOf<List<CategoryRuleEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }

    fun reloadRules() {
        isLoading = true
        scope.launch {
            rules = withContext(Dispatchers.IO) {
                TransactionRepository(appContext).listCategoryRules(limit = 300)
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        reloadRules()
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Header(
                    title = "Rule Center",
                    onSettings = onSettings,
                    onBack = onBack,
                    showActions = onBack == null
                )
            }
            item {
                RuleCenterSummaryCard(
                    rules = rules,
                    isLoading = isLoading,
                    message = message
                )
            }
            if (!isLoading && rules.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No learned rules yet",
                        detail = "Corrections saved with Remember will appear here."
                    )
                }
            } else {
                items(rules, key = { it.id }) { rule ->
                    RuleRow(
                        rule = rule,
                        onDisable = {
                            scope.launch {
                                val disabled = withContext(Dispatchers.IO) {
                                    TransactionRepository(appContext)
                                        .setCategoryRuleEnabled(rule.id, enabled = false)
                                }
                                message = if (disabled) "Rule disabled" else "Rule was not updated"
                                reloadRules()
                            }
                        }
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun RuleCenterSummaryCard(
    rules: List<CategoryRuleEntity>,
    isLoading: Boolean,
    message: String?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isLoading) "Loading rules" else "${rules.size} active rules",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message ?: "Local rules override parser defaults during SMS and Gmail imports.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp
            )
        }
    }
}

@Composable
private fun RuleRow(
    rule: CategoryRuleEntity,
    onDisable: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryMiniDot(rule.departmentCategory ?: "Other")
                Spacer(modifier = Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rule.merchantNormalized ?: rule.pattern,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = 0.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${rule.pattern} • ${rule.matchType} • ${rule.source}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = 0.sp
                    )
                }
                TextButton(onClick = onDisable) {
                    Text("Disable")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    listOfNotNull(
                        rule.departmentCategory,
                        rule.miscCategory,
                        rule.transactionType.displayName()
                    )
                ) { label ->
                    DetailChip(label)
                }
            }
        }
    }
}

@Composable
private fun SourceHealthMiniCard(sourceRows: List<SourceHealthRow>) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Coverage",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (sourceRows.isEmpty()) {
                Text(
                    text = "No source rows yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    letterSpacing = 0.sp
                )
            } else {
                sourceRows.forEach { row ->
                    SourceHealthInlineRow(row = row)
                }
            }
        }
    }
}

@Composable
private fun SettingsInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            modifier = Modifier.width(96.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.sp
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Start,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun SourceHealthCard(
    feedState: FeedState,
    gmailState: GmailUiState
) {
    val sourceRows = feedState.transactions.sourceHealthRows()
    val reviewCount = feedState.transactions.reviewCandidates().size
    val gmailRows = feedState.transactions.latestMonthTransactions().count { it.source == "Gmail" }
    val fxRows = feedState.transactions.latestMonthTransactions().count { !it.countsInInrTotals() }
    val gmailLabel = if (gmailState.error != null) "Needs attention" else gmailState.label

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Source health",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(11.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniMetric(
                    label = "Review",
                    value = reviewCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                MiniMetric(
                    label = "Gmail",
                    value = gmailRows.toString(),
                    modifier = Modifier.weight(1f)
                )
                MiniMetric(
                    label = "FX",
                    value = fxRows.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            SettingsInfoRow("SMS", if (feedState.needsSmsPermission) "Permission needed" else "Enabled")
            SettingsInfoRow("Gmail", gmailLabel)
            SettingsInfoRow("Storage", "Local only")
            if (sourceRows.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                sourceRows.forEach { row ->
                    SourceHealthInlineRow(row = row)
                }
            }
        }
    }
}

@Composable
private fun SourceHealthInlineRow(row: SourceHealthRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryMiniDot(row.source)
        Spacer(modifier = Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.source,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp
            )
            Text(
                text = "${row.spendCount} spends • ${row.reviewCount} review • ${row.fxCount} FX",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp
            )
        }
        Text(
            text = "${row.totalCount}",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    detail: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp
            )
        }
    }
}

@Composable
private fun SourceStatusCard(feedState: FeedState) {
    val sourceCounts = feedState.transactions
        .groupingBy { it.source }
        .eachCount()
        .toSortedMap()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Connected data",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (sourceCounts.isEmpty()) {
                Text(
                    text = "No transactions imported yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    letterSpacing = 0.sp
                )
            } else {
                sourceCounts.forEach { (source, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = source,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            letterSpacing = 0.sp
                        )
                        Text(
                            text = "$count transaction${if (count == 1) "" else "s"}",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GmailImportCard(
    state: GmailUiState,
    setupInfo: GmailSetupInfo,
    onImport: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = !state.isImporting, onClick = onImport),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Gmail import",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = state.error ?: state.label,
                    color = if (state.error == null) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFFF8E8E),
                    fontSize = 13.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 0.sp
                )
                state.autoSyncLabel?.let { label ->
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = 0.sp
                    )
                }
                if (state.error != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "OAuth setup: package ${setupInfo.packageName}, SHA-1 ${setupInfo.signingSha1 ?: "unavailable"}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        letterSpacing = 0.sp
                    )
                }
            }
            Text(
                text = if (state.isImporting) "Reading" else "Import",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )
        }
    }
}

@Composable
private fun Header(
    title: String,
    onSettings: () -> Unit,
    onBack: (() -> Unit)? = null,
    showActions: Boolean = true
) {
    val showLogo = title == "Sorted" && onBack == null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
        if (showLogo) {
            SortedLogoMark(modifier = Modifier.size(34.dp))
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 31.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.sp
        )
        if (showActions) {
            IconButton(onClick = onSettings) {
                SettingsGlyph(
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun SortedLogoMark(modifier: Modifier = Modifier) {
    val containerColor = MaterialTheme.colorScheme.primary
    val markColor = MaterialTheme.colorScheme.onPrimary

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeHeight = h * 0.10f
        val radius = CornerRadius(strokeHeight * 0.5f, strokeHeight * 0.5f)

        drawCircle(
            color = containerColor,
            radius = size.minDimension * 0.46f,
            center = Offset(w * 0.5f, h * 0.5f)
        )
        drawRoundRect(
            color = markColor,
            topLeft = Offset(w * 0.27f, h * 0.34f),
            size = Size(w * 0.46f, strokeHeight),
            cornerRadius = radius
        )
        drawRoundRect(
            color = markColor,
            topLeft = Offset(w * 0.27f, h * 0.50f),
            size = Size(w * 0.34f, strokeHeight),
            cornerRadius = radius
        )
        drawRoundRect(
            color = markColor,
            topLeft = Offset(w * 0.27f, h * 0.66f),
            size = Size(w * 0.22f, strokeHeight),
            cornerRadius = radius
        )
    }
}

@Composable
private fun SortedBottomBar(
    selectedTab: SortedTab,
    onTabSelected: (SortedTab) -> Unit
) {
    val palette = tapePalette()
    val tabs = listOf(SortedTab.Home, SortedTab.Insights, SortedTab.Capture, SortedTab.RuleCenter)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = palette.desk,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(palette.ruleFaint)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val selected = tab == selectedTab
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) palette.ink else palette.inkFaint,
                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                        label = "bottom_bar_content_${tab.label}"
                    )
                    val markerColor by animateColorAsState(
                        targetValue = if (selected) palette.amber else Color.Transparent,
                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                        label = "bottom_bar_marker_${tab.label}"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clickable {
                                if (!selected) onTabSelected(tab)
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .width(26.dp)
                                .height(2.dp)
                                .background(markerColor)
                        )
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SortedNavGlyph(
                                icon = tab.icon,
                                color = contentColor,
                                active = selected,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tab.label.uppercase(Locale.US),
                                color = contentColor,
                                fontFamily = SortedTapeFontFamily,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncChooserBar(
    visible: Boolean,
    modifier: Modifier = Modifier,
    onSync: (SyncSource) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(durationMillis = 160)) +
            expandVertically(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(durationMillis = 130)) +
            shrinkVertically(animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing))
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            shape = RoundedCornerShape(22.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
            ),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SyncChoiceChip("SMS", SyncSource.Sms, onSync)
                SyncChoiceChip("Gmail", SyncSource.Gmail, onSync)
            }
        }
    }
}

@Composable
private fun SyncStatusPill(
    message: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(durationMillis = 160)) +
            expandVertically(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(durationMillis = 140)) +
            shrinkVertically(animationSpec = tween(durationMillis = 170, easing = FastOutSlowInEasing))
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
            )
        ) {
            Text(
                text = message.orEmpty(),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                letterSpacing = 0.sp
            )
        }
    }
}

@Composable
private fun SyncChoiceChip(
    label: String,
    source: SyncSource,
    onSync: (SyncSource) -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(17.dp))
            .clickable { onSync(source) },
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(17.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun SettingsGlyph(
    color: Color,
    modifier: Modifier = Modifier
) {
    SortedNavGlyph(
        icon = SortedNavIcon.Settings,
        color = color,
        modifier = modifier
    )
}

@Composable
private fun MonthArrowGlyph(
    direction: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.dp.toPx()
        val startX = if (direction < 0) size.width * 0.64f else size.width * 0.36f
        val endX = if (direction < 0) size.width * 0.36f else size.width * 0.64f
        drawLine(
            color = color,
            start = Offset(startX, size.height * 0.22f),
            end = Offset(endX, size.height * 0.50f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(endX, size.height * 0.50f),
            end = Offset(startX, size.height * 0.78f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun SortedNavGlyph(
    icon: SortedNavIcon,
    color: Color,
    active: Boolean = false,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = (if (active) 2.dp else 1.5.dp).toPx()
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Square)
        val w = size.width
        val h = size.height
        fun p(x: Float, y: Float) = Offset(w * (x / 24f), h * (y / 24f))
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(color, p(x1, y1), p(x2, y2), strokeWidth, StrokeCap.Square)
        }
        fun polyline(points: List<Offset>) {
            points.zipWithNext().forEach { (start, end) ->
                drawLine(color, start, end, strokeWidth, StrokeCap.Square)
            }
        }

        when (icon) {
            SortedNavIcon.Home -> {
                polyline(listOf(
                    p(5f, 3.75f),
                    p(19f, 3.75f),
                    p(19f, 16.5f),
                    p(17f, 18.25f),
                    p(15f, 16.5f),
                    p(13f, 18.25f),
                    p(11f, 16.5f),
                    p(9f, 18.25f),
                    p(7f, 16.5f),
                    p(5f, 18.25f),
                    p(5f, 3.75f)
                ))
                line(8.25f, 8f, 15.75f, 8f)
                line(8.25f, 11.5f, 13.5f, 11.5f)
            }

            SortedNavIcon.Insights -> {
                line(4f, 6.25f, 12f, 6.25f)
                line(16f, 6.25f, 20f, 6.25f)
                line(4f, 12f, 10f, 12f)
                line(14f, 12f, 20f, 12f)
                line(4f, 17.75f, 13f, 17.75f)
                line(17f, 17.75f, 20f, 17.75f)
            }

            SortedNavIcon.Capture -> {
                line(12f, 4.5f, 12f, 14.5f)
                line(7f, 9.5f, 17f, 9.5f)
                line(4f, 19.25f, 20f, 19.25f)
            }

            SortedNavIcon.Sources -> {
                drawCircle(color, radius = w * 0.10f, center = Offset(w * 0.24f, h * 0.33f))
                drawCircle(color, radius = w * 0.10f, center = Offset(w * 0.24f, h * 0.68f))
                drawLine(color, Offset(w * 0.42f, h * 0.33f), Offset(w * 0.78f, h * 0.33f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.42f, h * 0.68f), Offset(w * 0.78f, h * 0.68f), strokeWidth, StrokeCap.Round)
            }

            SortedNavIcon.RuleCenter -> {
                polyline(listOf(p(9.25f, 9.25f), p(9.25f, 6f), p(14.75f, 6f), p(14.75f, 9.25f)))
                polyline(listOf(
                    p(4.5f, 9.25f),
                    p(19.5f, 9.25f),
                    p(19.5f, 19f),
                    p(4.5f, 19f),
                    p(4.5f, 9.25f)
                ))
                line(9f, 14.25f, 15f, 14.25f)
            }

            SortedNavIcon.Settings -> {
                drawCircle(
                    color = color,
                    radius = w * (3.25f / 24f),
                    center = p(12f, 12f),
                    style = stroke
                )
                line(12f, 3f, 12f, 5.5f)
                line(12f, 18.5f, 12f, 21f)
                line(4.2f, 7.5f, 6.4f, 8.75f)
                line(17.6f, 15.25f, 19.8f, 16.5f)
                line(4.2f, 16.5f, 6.4f, 15.25f)
                line(17.6f, 8.75f, 19.8f, 7.5f)
            }

            SortedNavIcon.Sync -> {
                drawCircle(
                    color = color,
                    radius = w * 0.34f,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = stroke
                )
                drawLine(color, Offset(w * 0.50f, h * 0.28f), Offset(w * 0.50f, h * 0.62f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.50f, h * 0.62f), Offset(w * 0.34f, h * 0.48f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.50f, h * 0.62f), Offset(w * 0.66f, h * 0.48f), strokeWidth, StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun HomeLoadingSummary() {
    val isDark = isDarkModeActive()
    val transition = rememberInfiniteTransition(label = "home_loading_constellation")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "home_loading_phase"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(626.dp)
            .padding(horizontal = 8.dp)
    ) {
        ConstellationField(
            nodes = emptyList(),
            phase = phase,
            isDark = isDark,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Monthly spend",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "₹••,•••.••",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 38.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "reading local sources",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.50f else 0.78f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.18f else 0.30f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Preparing view",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp
                )
            }
        }
    }
}

@Composable
private fun HomeLoadingTile(
    blockColor: Color,
    softBlockColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(softBlockColor)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.42f)
                .height(10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(blockColor)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .height(18.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(blockColor)
        )
    }
}

@Composable
private fun HomeConstellationHeader(
    months: List<String>,
    selectedMonthKey: String?,
    onMonthSelected: (String) -> Unit,
    onSettings: () -> Unit
) {
    val selectedIndex = months.indexOf(selectedMonthKey).takeIf { it >= 0 } ?: 0
    val hasNewer = selectedIndex > 0
    val hasOlder = selectedIndex >= 0 && selectedIndex < months.lastIndex
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 18.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SortedLogoMark(modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Sorted",
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            letterSpacing = 0.sp
        )
        if (months.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (hasOlder) onMonthSelected(months[selectedIndex + 1])
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        MonthArrowGlyph(
                            direction = -1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (hasOlder) 1f else 0.28f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = selectedMonthKey?.monthShortLabel() ?: "Month",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        letterSpacing = 0.sp
                    )
                    IconButton(
                        onClick = {
                            if (hasNewer) onMonthSelected(months[selectedIndex - 1])
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        MonthArrowGlyph(
                            direction = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (hasNewer) 1f else 0.28f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
        IconButton(onClick = onSettings) {
            SettingsGlyph(
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(21.dp)
            )
        }
    }
}

@Composable
private fun ConstellationHome(
    feedState: FeedState,
    selectedMonthKey: String?,
    onExplainSpend: () -> Unit,
    onMerchantClick: (SummaryGroup) -> Unit,
    onCategoryClick: (SummaryGroup) -> Unit
) {
    val isDark = isDarkModeActive()
    val breakdown = feedState.transactions.monthBreakdown(selectedMonthKey)
    val merchantGroups = feedState.transactions.monthSpendMerchantGroups(selectedMonthKey)
    val categoryGroups = feedState.transactions.monthSpendCategoryGroups(selectedMonthKey)
    val topMerchant = merchantGroups.firstOrNull()
    val topCategory = categoryGroups.firstOrNull()
    val reviewCandidates = feedState.transactions.reviewCandidates(selectedMonthKey)

    val nodes = listOfNotNull(
        topMerchant?.let { group ->
            HomeConstellationNode(
                id = "merchant",
                label = group.label,
                value = group.total.formatRupeeCompact(),
                detail = "${group.count} transactions",
                x = 0.5f,
                y = 340f / 650f,
                orbitAngle = 210f,
                orbitRadius = 92f,
                visibleAtZoom = 1.12f,
                accent = if (isDark) Color(0xFFFBC02D) else Color(0xFF0F8F7A),
                onClick = { onMerchantClick(group) }
            )
        },
        topCategory?.let { group ->
            val share = if (breakdown.spends > 0.0) ((group.total / breakdown.spends) * 100.0).roundToInt() else 0
            HomeConstellationNode(
                id = "category",
                label = group.label,
                value = "$share%",
                detail = "of spend",
                x = 0.5f,
                y = 340f / 650f,
                orbitAngle = 150f,
                orbitRadius = 94f,
                visibleAtZoom = 1.16f,
                accent = if (isDark) Color(0xFFFBC02D) else Color(0xFF7A4FD8),
                onClick = { onCategoryClick(group) }
            )
        },
        if (reviewCandidates.isNotEmpty()) {
            HomeConstellationNode(
                id = "review",
                label = "To review",
                value = reviewCandidates.size.toString(),
                detail = reviewCandidates.sumOf { it.inrAmountValue ?: 0.0 }.formatRupeeCompact(),
                x = 0.5f,
                y = 340f / 650f,
                orbitAngle = 330f,
                orbitRadius = 98f,
                visibleAtZoom = 1.18f,
                accent = if (isDark) Color(0xFFFBC02D) else Color(0xFFE8622F),
                needsAttention = true,
                onClick = onExplainSpend
            )
        } else {
            null
        },
        if (breakdown.recurringInvestments > 0.0) {
            HomeConstellationNode(
                id = "invest",
                label = "SIPs",
                value = breakdown.recurringInvestments.formatRupeeCompact(),
                detail = "recurring",
                x = 0.5f,
                y = 340f / 650f,
                orbitAngle = 42f,
                orbitRadius = 106f,
                visibleAtZoom = 1.22f,
                accent = if (isDark) Color(0xFFFBC02D) else Color(0xFF0F9A54),
                outsideSpend = true,
                onClick = onExplainSpend
            )
        } else {
            null
        },
        if (breakdown.oneTimeInvestments > 0.0) {
            HomeConstellationNode(
                id = "invest_once",
                label = "One-time",
                value = breakdown.oneTimeInvestments.formatRupeeCompact(),
                detail = "investment",
                x = 0.5f,
                y = 340f / 650f,
                orbitAngle = 14f,
                orbitRadius = 118f,
                visibleAtZoom = 1.36f,
                accent = if (isDark) Color(0xFFFBC02D) else Color(0xFF2B83F6),
                outsideSpend = true,
                onClick = onExplainSpend
            )
        } else {
            null
        },
        if (breakdown.transfers > 0.0) {
            HomeConstellationNode(
                id = "moved",
                label = "Money moved",
                value = breakdown.transfers.formatRupeeCompact(),
                detail = "not spend",
                x = 0.5f,
                y = 340f / 650f,
                orbitAngle = 108f,
                orbitRadius = 108f,
                visibleAtZoom = 1.24f,
                accent = if (isDark) Color(0xFFFBC02D) else Color(0xFF1F74C7),
                outsideSpend = true,
                onClick = onExplainSpend
            )
        } else {
            null
        },
        merchantGroups.getOrNull(1)?.let { group ->
            HomeConstellationNode(
                id = "merchant_2",
                label = group.label,
                value = group.total.formatRupeeCompact(),
                detail = "${group.count} transactions",
                x = 0.5f,
                y = 340f / 650f,
                orbitAngle = 258f,
                orbitRadius = 126f,
                visibleAtZoom = 1.42f,
                accent = if (isDark) Color(0xFFFBC02D) else Color(0xFFE64E89),
                onClick = { onMerchantClick(group) }
            )
        },
        merchantGroups.getOrNull(2)?.let { group ->
            HomeConstellationNode(
                id = "merchant_3",
                label = group.label,
                value = group.total.formatRupeeCompact(),
                detail = "${group.count} transactions",
                x = 0.5f,
                y = 340f / 650f,
                orbitAngle = 18f,
                orbitRadius = 128f,
                visibleAtZoom = 1.56f,
                accent = if (isDark) Color(0xFFFBC02D) else Color(0xFF2B83F6),
                onClick = { onMerchantClick(group) }
            )
        },
        categoryGroups.getOrNull(1)?.let { group ->
            val share = if (breakdown.spends > 0.0) ((group.total / breakdown.spends) * 100.0).roundToInt() else 0
            HomeConstellationNode(
                id = "category_2",
                label = group.label,
                value = "$share%",
                detail = "of spend",
                x = 0.5f,
                y = 340f / 650f,
                orbitAngle = 294f,
                orbitRadius = 134f,
                visibleAtZoom = 1.48f,
                accent = if (isDark) Color(0xFFFBC02D) else Color(0xFF00A7A5),
                onClick = { onCategoryClick(group) }
            )
        },
        categoryGroups.getOrNull(2)?.let { group ->
            val share = if (breakdown.spends > 0.0) ((group.total / breakdown.spends) * 100.0).roundToInt() else 0
            HomeConstellationNode(
                id = "category_3",
                label = group.label,
                value = "$share%",
                detail = "of spend",
                x = 0.5f,
                y = 340f / 650f,
                orbitAngle = 72f,
                orbitRadius = 136f,
                visibleAtZoom = 1.64f,
                accent = if (isDark) Color(0xFFFBC02D) else Color(0xFFFFB000),
                onClick = { onCategoryClick(group) }
            )
        }
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(626.dp)
            .padding(horizontal = 8.dp)
    ) {
        val density = LocalDensity.current
        var targetClusterZoom by remember { mutableStateOf(1f) }
        var zoomFocus by remember { mutableStateOf(Offset(0.5f, 340f / 650f)) }
        var deepZoomArmed by remember { mutableStateOf(true) }
        val clusterZoom by animateFloatAsState(
            targetValue = targetClusterZoom,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "cluster_zoom"
        )
        var phase by remember { mutableStateOf(0f) }
        LaunchedEffect(Unit) {
            val startMillis = withFrameMillis { it }
            while (true) {
                phase = (withFrameMillis { it } - startMillis) / 9000f
            }
        }

        ConstellationField(
            nodes = nodes,
            phase = phase,
            isDark = isDark,
            clusterZoom = clusterZoom,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressedCount = event.changes.count { it.pressed }
                            if (pressedCount == 0) break
                            if (pressedCount >= 2) {
                                val centroid = event.calculateCentroid(useCurrent = true)
                                val zoom = event.calculateZoom()
                                if (zoom.isFinite() && zoom > 0f) {
                                    zoomFocus = Offset(
                                        x = (centroid.x / size.width).coerceIn(0f, 1f),
                                        y = (centroid.y / size.height).coerceIn(0f, 1f)
                                    )
                                    targetClusterZoom = (targetClusterZoom * zoom).coerceIn(1f, 2.85f)
                                }
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
        )

        LaunchedEffect(targetClusterZoom, zoomFocus) {
            if (targetClusterZoom < 1.35f) {
                deepZoomArmed = true
            }
            if (targetClusterZoom >= 2.52f && deepZoomArmed) {
                val focusNode = nodes
                    .filter { it.id.startsWith("merchant") || it.id.startsWith("category") }
                    .minByOrNull { node ->
                        val nodeOffset = node.orbitOffset(
                            phase = phase,
                            zoom = targetClusterZoom,
                            widthPx = with(density) { maxWidth.toPx() },
                            heightPx = with(density) { maxHeight.toPx() }
                        )
                        val dx = zoomFocus.x - nodeOffset.x / with(density) { maxWidth.toPx() }
                        val dy = zoomFocus.y - nodeOffset.y / with(density) { maxHeight.toPx() }
                        dx * dx + dy * dy
                }
                if (focusNode != null) {
                    deepZoomArmed = false
                    targetClusterZoom = 2.85f
                    delay(180)
                    focusNode.onClick()
                    targetClusterZoom = 1f
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${breakdown.monthKey?.monthNameLabel() ?: "Current"} spend",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(9.dp))
            Text(
                text = breakdown.spends.formatRupee(),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 43.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "${breakdown.spendCount} transactions",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(11.dp))
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onExplainSpend),
                color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.50f else 0.78f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.24f else 0.36f)
                )
            ) {
                Text(
                    text = "Why this number?",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            ZoomCueArrow(
                color = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.62f else 0.72f),
                modifier = Modifier.size(width = 20.dp, height = 24.dp)
            )
        }

        nodes.forEach { node ->
            val visibility = if (node.visibleAtZoom <= 1f) {
                1f
            } else {
                ((clusterZoom - node.visibleAtZoom) / 0.14f).coerceIn(0f, 1f)
            }
            if (visibility <= 0.01f) return@forEach

            val nodeOffset = node.orbitOffset(
                phase = phase,
                zoom = clusterZoom,
                widthPx = with(density) { maxWidth.toPx() },
                heightPx = with(density) { maxHeight.toPx() }
            )
            val x = with(density) { nodeOffset.x.toDp() }
            val y = with(density) { nodeOffset.y.toDp() }
            ConstellationDataSphere(
                node = node,
                modifier = Modifier
                    .offset(
                        x = x - 28.dp,
                        y = y - 28.dp
                    )
                    .graphicsLayer {
                        alpha = visibility
                        scaleX = 0.92f + 0.08f * visibility
                        scaleY = 0.92f + 0.08f * visibility
                    }
            )
        }

    }
}

private fun HomeConstellationNode.orbitOffset(
    phase: Float,
    zoom: Float,
    widthPx: Float,
    heightPx: Float
): Offset {
    val orbitSpeed = when (id) {
        "merchant" -> 0.24f
        "review" -> -0.22f
        "category" -> 0.20f
        "invest" -> -0.18f
        "invest_once" -> 0.16f
        "moved" -> 0.19f
        "merchant_2" -> -0.34f
        "merchant_3" -> 0.30f
        "category_2" -> 0.32f
        "category_3" -> -0.28f
        else -> 0.22f
    }
    val wobble = sin(phase * PI.toFloat() * 2f + orbitAngle) * 3.5f
    val angle = ((orbitAngle + phase * 360f * orbitSpeed + wobble) * PI.toFloat()) / 180f
    val baseScale = widthPx / 384f
    val radius = orbitRadius * baseScale * (0.62f + (zoom - 1f) * 0.20f)
    val centerX = widthPx * x
    val centerY = heightPx * y
    return Offset(
        x = centerX + radius * cos(angle),
        y = centerY + radius * 0.70f * sin(angle)
    )
}

@Composable
private fun ConstellationField(
    nodes: List<HomeConstellationNode>,
    phase: Float,
    isDark: Boolean,
    clusterZoom: Float = 1f,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val text = MaterialTheme.colorScheme.onBackground
    Canvas(modifier = modifier) {
        val center = Offset(size.width * 0.50f, size.height * (340f / 650f))
        val baseScale = size.width / 384f
        val zoom = clusterZoom.coerceIn(1f, 1.85f)
        val sphereRx = 154f * baseScale * zoom
        val sphereRy = 148f * baseScale * zoom
        val cycle = phase * PI.toFloat() * 2f
        val breathe = 1f + (if (isDark) 0.018f else 0.03f) * sin(phase * PI.toFloat() * 2f)
        val detailAlpha = ((zoom - 1f) / 0.55f).coerceIn(0f, 1f)

        fun noise(seed: Int): Float {
            val x = sin(seed * 12.9898f + 78.233f) * 43758.5453f
            return x - kotlin.math.floor(x)
        }

        fun particlePosition(i: Int): Offset {
            val ph = noise(i * 19 + 7) * PI.toFloat() * 2f
            val baseLongitude = noise(i * 17 + 3) * PI.toFloat() * 2f
            val baseLatitude = kotlin.math.asin((noise(i * 31 + 9) * 2f - 1f).coerceIn(-0.96f, 0.96f))
            val longitudeDrift = 0.16f * sin(cycle + ph) + 0.05f * sin(cycle * 2f + ph * 0.7f)
            val latitudeDrift = 0.06f * cos(cycle + ph * 1.4f)
            val longitude = baseLongitude + longitudeDrift
            val latitude = (baseLatitude + latitudeDrift).coerceIn(-1.20f, 1.20f)
            val shell = (
                0.42f +
                    0.58f * kotlin.math.sqrt(noise(i * 47 + 21)) +
                    0.025f * sin(cycle + ph * 1.9f)
                ).coerceIn(0.36f, 1.03f)
            val depth = cos(longitude) * cos(latitude)
            val projected = 0.82f + 0.18f * ((depth + 1f) / 2f)
            val wob = (1f + noise(i * 13 + 5) * 3f) * baseScale
            val x = center.x + sphereRx * shell * projected * cos(latitude) * sin(longitude) * breathe + wob * sin(cycle + ph)
            val y = center.y + sphereRy * shell * sin(latitude) * breathe + wob * cos(cycle + ph * 1.3f)
            return Offset(x, y)
        }

        val particleCount = 1800
        for (i in 0 until particleCount step 6) {
            val a = particlePosition(i)
            val b = particlePosition((i + 17).coerceAtMost(particleCount - 1))
            val maxDistance = 46f * baseScale * (1f + detailAlpha * 0.46f)
            val distance = kotlin.math.hypot(a.x - b.x, a.y - b.y)
            if (distance < maxDistance) {
                val alpha = (if (isDark) 0.068f else 0.12f) * (1f - distance / maxDistance) * (1f + detailAlpha * 0.5f)
                drawLine(
                    color = text.copy(alpha = alpha.coerceIn(0f, 1f)),
                    start = a,
                    end = b,
                    strokeWidth = 0.6.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        for (i in 0 until particleCount) {
            val point = particlePosition(i)
            if (point.x < -8f || point.x > size.width + 8f || point.y < -8f || point.y > size.height + 8f) continue
            val spark = noise(i * 23 + 11) < 0.05f
            val base = if (spark) 0.55f + noise(i * 29 + 13) * 0.3f else 0.06f + noise(i * 29 + 13) * 0.2f
            val twinkleCycle = 1f + (i % 3).toFloat()
            val twinkle = 0.6f + 0.4f * sin(cycle * twinkleCycle + noise(i * 41 + 17) * PI.toFloat() * 2f)
            val alpha = (base * twinkle * if (isDark) 1.18f + detailAlpha * 0.30f else 1.70f + detailAlpha * 0.36f)
                .coerceIn(0f, 1f)
            val radius = ((if (spark) 1.1f + noise(i * 43 + 19) * 0.7f else 0.4f + noise(i * 43 + 19) * 0.7f) * baseScale * (1f + detailAlpha * 0.18f)).coerceAtLeast(0.35f)
            drawCircle(
                color = when (i % 6) {
                    0, 3 -> primary.copy(alpha = alpha)
                    else -> text.copy(alpha = alpha)
                },
                radius = radius,
                center = point
            )
            if (spark) {
                drawCircle(
                    color = primary.copy(alpha = (alpha * 0.14f).coerceIn(0f, 1f)),
                    radius = radius * 3.4f,
                    center = point
                )
            }
        }

        nodes.forEach { node ->
            val visibility = ((zoom - node.visibleAtZoom) / 0.14f).coerceIn(0f, 1f)
            if (visibility <= 0.01f) return@forEach

            val labelOffset = node.orbitOffset(
                phase = phase,
                zoom = zoom,
                widthPx = size.width,
                heightPx = size.height
            )
            val dx = center.x - labelOffset.x
            val dy = center.y - labelOffset.y
            val len = kotlin.math.hypot(dx, dy).coerceAtLeast(1f)
            val nodeOffset = Offset(
                labelOffset.x + (dx / len) * 34f * baseScale,
                labelOffset.y + (dy / len) * 34f * baseScale
            )
            drawLine(
                color = node.accent.copy(alpha = (if (isDark) 0.20f else 0.30f) * visibility),
                start = nodeOffset,
                end = labelOffset,
                strokeWidth = 0.8.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(
                color = node.accent.copy(alpha = (if (node.needsAttention) 0.22f else 0.12f) * visibility),
                radius = if (node.needsAttention) 10.dp.toPx() else 6.dp.toPx(),
                center = nodeOffset
            )
            drawCircle(
                color = node.accent.copy(alpha = visibility),
                radius = if (node.needsAttention) 3.5.dp.toPx() else 2.7.dp.toPx(),
                center = nodeOffset
            )
        }
    }
}

@Composable
private fun ZoomCueArrow(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.dp.toPx()
        drawLine(
            color = color,
            start = Offset(size.width * 0.50f, size.height * 0.16f),
            end = Offset(size.width * 0.50f, size.height * 0.78f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.26f, size.height * 0.54f),
            end = Offset(size.width * 0.50f, size.height * 0.78f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.74f, size.height * 0.54f),
            end = Offset(size.width * 0.50f, size.height * 0.78f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun ConstellationDataSphere(
    node: HomeConstellationNode,
    modifier: Modifier = Modifier
) {
    val isDark = isDarkModeActive()
    val sphereSize = if (node.needsAttention) 64.dp else 56.dp
    Surface(
        modifier = modifier
            .size(sphereSize)
            .clip(CircleShape)
            .clickable(onClick = node.onClick),
        color = node.accent.copy(alpha = if (isDark) 0.16f else 0.28f),
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            node.accent.copy(alpha = if (node.needsAttention) 0.42f else 0.30f)
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = node.accent.copy(alpha = if (isDark) 0.16f else 0.20f),
                    radius = size.minDimension * 0.48f,
                    center = Offset(size.width * 0.50f, size.height * 0.50f)
                )
                drawCircle(
                    color = node.accent.copy(alpha = if (isDark) 0.34f else 0.42f),
                    radius = size.minDimension * 0.13f,
                    center = Offset(size.width * 0.38f, size.height * 0.34f)
                )
            }
            Column(
                modifier = Modifier.padding(horizontal = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = node.value,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.sp
                )
                Text(
                    text = node.label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.sp
                )
            }
        }
    }
}

@Composable
private fun MonthSummary(
    feedState: FeedState,
    onExplainSpend: () -> Unit
) {
    val breakdown = feedState.transactions.monthBreakdown()
    val monthTransactions = feedState.transactions.latestMonthSpendTransactions()
        .filter { it.inrAmountValue != null }
    val categoryGroups = feedState.transactions.monthSpendCategoryGroups().take(5)
    val activeDays = monthTransactions.mapNotNull { it.transactionDate }.toSet().size
    val averageSpend = if (breakdown.spendCount > 0) breakdown.spends / breakdown.spendCount else 0.0
    val topMerchant = feedState.transactions.monthSpendMerchantGroups().firstOrNull()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onExplainSpend),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = breakdown.monthKey?.monthSpendLabel() ?: "Tracked INR spend",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.sp
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = breakdown.spends.formatInr(),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 31.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = 0.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${breakdown.spendCount} spends",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.sp
                    )
                    Text(
                        text = feedState.label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        letterSpacing = 0.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            HomeCategoryMixStrip(
                groups = categoryGroups,
                total = breakdown.spends
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HomeMetricTile(
                    label = "Total outflow",
                    value = breakdown.totalDebits.formatInr(),
                    accent = categoryColor("Food"),
                    modifier = Modifier.weight(1f)
                )
                HomeMetricTile(
                    label = "Recurring SIPs",
                    value = breakdown.recurringInvestments.formatInr(),
                    accent = categoryColor("Investment"),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HomeMetricTile(
                    label = "Avg spend",
                    value = averageSpend.formatInr(),
                    accent = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                HomeMetricTile(
                    label = "Top merchant",
                    value = topMerchant?.label ?: "None",
                    accent = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HomeSignalChip(
                    label = "$activeDays days",
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                HomeSignalChip(
                    label = "${breakdown.debitCount} moves",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                if (breakdown.fxConverted > 0.0) {
                    HomeSignalChip(
                        label = "FX ${breakdown.fxConverted.formatCompactInr()}",
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeCategoryMixStrip(
    groups: List<SummaryGroup>,
    total: Double
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (groups.isEmpty() || total <= 0.0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            } else {
                groups.forEach { group ->
                    val weight = ((group.total / total).toFloat()).coerceAtLeast(0.04f)
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .height(14.dp)
                            .background(homeMixColor(group.category))
                    )
                }
            }
        }
        if (groups.isNotEmpty()) {
            Spacer(modifier = Modifier.height(9.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(groups) { group ->
                    HomeLegendChip(group = group)
                }
            }
        }
    }
}

@Composable
private fun HomeLegendChip(group: SummaryGroup) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(homeMixContainerColor(group.category))
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(homeMixColor(group.category))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = group.label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun HomeMetricTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = if (isDarkModeActive()) 0.16f else 0.14f))
            .padding(11.dp)
    ) {
        Box(
            modifier = Modifier
                .width(26.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accent)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.sp
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun HomeSignalChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = if (isDarkModeActive()) 0.15f else 0.13f))
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.sp
        )
    }
}

private fun List<TransactionUi>.monthBreakdown(monthKey: String? = selectedMonthKey()): MonthBreakdown {
    val monthTransactions = filter {
        it.inrAmountValue != null && it.isInSelectedMonth(monthKey)
    }
    val debitTransactions = monthTransactions.filter { it.direction == DirectionUi.Debit }
    val spendTransactions = debitTransactions.filter { it.transactionType.countsAsSpend() }
    val fxConverted = debitTransactions
        .filter { !it.countsInInrTotals() }
        .sumOf { it.inrAmountValue ?: 0.0 }
    val spends = spendTransactions.sumOf { it.inrAmountValue ?: 0.0 }
    val transfers = debitTransactions
        .filter { it.transactionType == TransactionType.TRANSFER }
        .sumOf { it.inrAmountValue ?: 0.0 }
    val investmentTransactions = debitTransactions.filter { it.transactionType == TransactionType.INVESTMENT }
    val recurringInvestmentTransactions = investmentTransactions.filter { it.isRecurringInvestmentPattern(this) }
    val investments = investmentTransactions.sumOf { it.inrAmountValue ?: 0.0 }
    val recurringInvestments = recurringInvestmentTransactions.sumOf { it.inrAmountValue ?: 0.0 }
    val oneTimeInvestments = investments - recurringInvestments
    val creditTransactions = monthTransactions.filter { it.direction == DirectionUi.Credit }
    val refunds = creditTransactions
        .filter { it.transactionType == TransactionType.REFUND || it.category == "Refund" }
        .sumOf { it.inrAmountValue ?: 0.0 }
    val income = creditTransactions
        .filter { it.transactionType == TransactionType.INCOME || it.category == "Income" }
        .sumOf { it.inrAmountValue ?: 0.0 }
    val rewards = creditTransactions
        .filter { it.transactionType == TransactionType.REWARD || it.category == "Reward" }
        .sumOf { it.inrAmountValue ?: 0.0 }

    return MonthBreakdown(
        monthKey = monthKey,
        debitCount = debitTransactions.size,
        spendCount = spendTransactions.size,
        totalDebits = debitTransactions.sumOf { it.inrAmountValue ?: 0.0 },
        spends = spends,
        transfers = transfers,
        investments = investments,
        recurringInvestments = recurringInvestments,
        oneTimeInvestments = oneTimeInvestments,
        refunds = refunds,
        income = income,
        rewards = rewards,
        fxConverted = fxConverted
    )
}

private fun List<TransactionUi>.latestMonthDebitTransactions(monthKey: String? = selectedMonthKey()): List<TransactionUi> {
    return filter {
        it.direction == DirectionUi.Debit &&
            it.isInSelectedMonth(monthKey)
    }
}

private fun List<TransactionUi>.latestMonthTransactions(monthKey: String? = selectedMonthKey()): List<TransactionUi> {
    return filter { it.isInSelectedMonth(monthKey) }
}

private fun List<TransactionUi>.latestMonthCreditTransactions(monthKey: String? = selectedMonthKey()): List<TransactionUi> {
    return filter {
        it.direction == DirectionUi.Credit &&
            it.isInSelectedMonth(monthKey)
    }
}

private fun List<TransactionUi>.latestMonthSpendTransactions(monthKey: String? = selectedMonthKey()): List<TransactionUi> {
    return filter {
        it.direction == DirectionUi.Debit &&
            it.transactionType.countsAsSpend() &&
            it.isInSelectedMonth(monthKey)
    }
}

private fun TransactionUi.isRecurringInvestmentPattern(allTransactions: List<TransactionUi>): Boolean {
    if (direction != DirectionUi.Debit || transactionType != TransactionType.INVESTMENT) return false
    val amount = inrAmountValue ?: return false
    if (amount < 500.0) return false

    val merchantKey = merchant.trim().lowercase(Locale.US)
    val knownSipMerchant = listOf(
        "indian clearing corporation",
        "quant mutual fund",
        "edelweiss mutual fund",
        "hdfc mutual fund",
        "icici prudential mutual fund",
        "motilal oswal mutual fund"
    ).any { merchantKey == it }
    val recurringRail = paymentMode.equals(PaymentMode.NACH.displayName(), ignoreCase = true) ||
        paymentMode.equals(PaymentMode.UPI_MANDATE.displayName(), ignoreCase = true)
    if ((knownSipMerchant || recurringRail) && amount <= 10_000.0) return true

    val amountKey = amount.toInvestmentAmountKey()
    val recurringMonths = allTransactions
        .asSequence()
        .filter { transaction ->
            transaction.direction == DirectionUi.Debit &&
                transaction.transactionType == TransactionType.INVESTMENT &&
                transaction.merchant.equals(merchant, ignoreCase = true) &&
                transaction.transactionDate?.take(7) != null &&
                (transaction.inrAmountValue ?: 0.0).toInvestmentAmountKey() == amountKey
        }
        .mapNotNull { it.transactionDate?.take(7) }
        .distinct()
        .count()

    return amount <= 10_000.0 && recurringMonths >= 3
}

private fun List<TransactionUi>.explainBuckets(): List<ExplainBucket> {
    val monthTransactions = latestMonthTransactions()
        .filter { it.inrAmountValue != null }
    val debitTransactions = monthTransactions.filter { it.direction == DirectionUi.Debit }
    val spendTransactions = debitTransactions.filter { it.transactionType.countsAsSpend() }
    val transferTransactions = debitTransactions.filter { it.transactionType == TransactionType.TRANSFER }
    val investmentTransactions = debitTransactions.filter { it.transactionType == TransactionType.INVESTMENT }
    val recurringInvestmentTransactions = investmentTransactions.filter { it.isRecurringInvestmentPattern(this) }
    val oneTimeInvestmentTransactions = investmentTransactions - recurringInvestmentTransactions.toSet()
    val otherDebitTransactions = debitTransactions.filter {
        !it.transactionType.countsAsSpend() &&
            it.transactionType != TransactionType.TRANSFER &&
            it.transactionType != TransactionType.INVESTMENT
    }
    val refundTransactions = monthRefundSignals()
    val incomeTransactions = latestMonthCreditTransactions()
        .filter { it.transactionType == TransactionType.INCOME || it.category == "Income" }
    val rewardTransactions = latestMonthCreditTransactions()
        .filter { it.transactionType == TransactionType.REWARD || it.category == "Reward" }
    val fxTransactions = monthTransactions.filter { !it.countsInInrTotals() }

    return listOf(
        ExplainBucket(
            title = "Included spend",
            amount = spendTransactions.sumOf { it.inrAmountValue ?: 0.0 },
            count = spendTransactions.size,
            description = "Expense and subscription debits.",
            transactions = spendTransactions
        ),
        ExplainBucket(
            title = "Recurring SIPs",
            amount = recurringInvestmentTransactions.sumOf { it.inrAmountValue ?: 0.0 },
            count = recurringInvestmentTransactions.size,
            description = "Pattern-matched monthly SIP and mutual fund deductions.",
            transactions = recurringInvestmentTransactions
        ),
        ExplainBucket(
            title = "One-time investments",
            amount = oneTimeInvestmentTransactions.sumOf { it.inrAmountValue ?: 0.0 },
            count = oneTimeInvestmentTransactions.size,
            description = "Lump-sum broker, fund, or investment transfers.",
            transactions = oneTimeInvestmentTransactions
        ),
        ExplainBucket(
            title = "Transfers",
            amount = transferTransactions.sumOf { it.inrAmountValue ?: 0.0 },
            count = transferTransactions.size,
            description = "Money moved between people, cards, wallets, and accounts.",
            transactions = transferTransactions
        ),
        ExplainBucket(
            title = "Other debits",
            amount = otherDebitTransactions.sumOf { it.inrAmountValue ?: 0.0 },
            count = otherDebitTransactions.size,
            description = "Debits that are not trusted as spend yet.",
            transactions = otherDebitTransactions
        ),
        ExplainBucket(
            title = "Refund signals",
            amount = refundTransactions.sumOf { it.inrAmountValue ?: 0.0 },
            count = refundTransactions.size,
            description = "Credits that look like refunds or reversals. Not netted yet.",
            transactions = refundTransactions
        ),
        ExplainBucket(
            title = "Income",
            amount = incomeTransactions.sumOf { it.inrAmountValue ?: 0.0 },
            count = incomeTransactions.size,
            description = "Salary, interest, payouts, or other income-like credits.",
            transactions = incomeTransactions
        ),
        ExplainBucket(
            title = "Rewards",
            amount = rewardTransactions.sumOf { it.inrAmountValue ?: 0.0 },
            count = rewardTransactions.size,
            description = "Cashback, rewards, or loyalty credits.",
            transactions = rewardTransactions
        ),
        ExplainBucket(
            title = "FX converted",
            amount = fxTransactions.sumOf { it.inrAmountValue ?: 0.0 },
            count = fxTransactions.size,
            description = "Non-INR transactions converted using stored daily FX rates.",
            transactions = fxTransactions
        )
    )
}

private fun List<TransactionUi>.monthMerchantGroups(): List<SummaryGroup> {
    return latestMonthDebitTransactions()
        .filter { it.inrAmountValue != null }
        .groupBy { it.merchant }
        .map { (merchant, transactions) ->
            SummaryGroup(
                label = merchant,
                count = transactions.size,
                total = transactions.sumOf { it.inrAmountValue ?: 0.0 },
                currency = "INR",
                category = transactions.firstOrNull()?.category ?: "Other"
            )
        }
        .sortedByDescending { it.total }
}

private fun List<TransactionUi>.monthSpendMerchantGroups(monthKey: String? = selectedMonthKey()): List<SummaryGroup> {
    return latestMonthSpendTransactions(monthKey)
        .filter { it.inrAmountValue != null }
        .groupBy { it.merchant }
        .map { (merchant, transactions) ->
            SummaryGroup(
                label = merchant,
                count = transactions.size,
                total = transactions.sumOf { it.inrAmountValue ?: 0.0 },
                currency = "INR",
                category = transactions.firstOrNull()?.category ?: "Other"
            )
        }
        .sortedByDescending { it.total }
}

private fun List<TransactionUi>.monthCategoryGroups(): List<SummaryGroup> {
    return latestMonthDebitTransactions()
        .filter { it.inrAmountValue != null }
        .groupBy { it.category }
        .map { (category, transactions) ->
            SummaryGroup(
                label = category,
                count = transactions.size,
                total = transactions.sumOf { it.inrAmountValue ?: 0.0 },
                currency = "INR",
                category = category
            )
        }
        .sortedByDescending { it.total }
}

private fun List<TransactionUi>.monthSpendCategoryGroups(monthKey: String? = selectedMonthKey()): List<SummaryGroup> {
    return latestMonthSpendTransactions(monthKey)
        .filter { it.inrAmountValue != null }
        .groupBy { it.category }
        .map { (category, transactions) ->
            SummaryGroup(
                label = category,
                count = transactions.size,
                total = transactions.sumOf { it.inrAmountValue ?: 0.0 },
                currency = "INR",
                category = category
            )
        }
        .sortedByDescending { it.total }
}

private fun List<TransactionUi>.reviewCandidates(monthKey: String? = selectedMonthKey()): List<TransactionUi> {
    return latestMonthTransactions(monthKey)
        .filter { it.inrAmountValue != null }
        .filter(TransactionUi::needsReview)
        .sortedByDescending { it.inrAmountValue ?: 0.0 }
}

private fun TransactionUi.needsReview(): Boolean {
    val amount = inrAmountValue ?: 0.0
    return category == "Other" ||
        miscCategory == "Uncategorized" ||
        categorySource == CategorySource.FALLBACK ||
        confidence < 0.70 ||
        merchant.looksLikeRawPaymentHandle() ||
        (source == "Gmail" && amount >= 10_000.0) ||
        (!countsInInrTotals() && amount > 0.0)
}

private fun TransactionUi.reviewReason(): String {
    val amount = inrAmountValue ?: 0.0
    return when {
        category == "Other" -> "Category needs sorting"
        miscCategory == "Uncategorized" -> "Merchant tag missing"
        categorySource == CategorySource.FALLBACK -> "Fallback categorization"
        confidence < 0.70 -> "Low parser confidence"
        merchant.looksLikeRawPaymentHandle() -> "Merchant needs cleanup"
        source == "Gmail" && amount >= 10_000.0 -> "High-value Gmail row"
        !countsInInrTotals() && amount > 0.0 -> "FX conversion review"
        else -> "Review"
    }
}

private fun List<TransactionUi>.monthRefundSignals(): List<TransactionUi> {
    return latestMonthCreditTransactions()
        .filter { it.inrAmountValue != null }
        .filter { transaction ->
            transaction.transactionType == TransactionType.REFUND ||
                transaction.transactionType == TransactionType.REWARD ||
                transaction.category == "Refund" ||
                transaction.category == "Reward" ||
                transaction.merchant.contains("refund", ignoreCase = true) ||
                transaction.detail.contains("refund", ignoreCase = true) ||
                transaction.detail.contains("reversal", ignoreCase = true) ||
                transaction.detail.contains("cashback", ignoreCase = true)
        }
        .sortedByDescending { it.inrAmountValue ?: 0.0 }
}

private fun List<TransactionUi>.recurringCandidates(): List<RecurringCandidate> {
    return filter {
        it.direction == DirectionUi.Debit &&
            it.inrAmountValue != null &&
            !it.transactionDate.isNullOrBlank()
    }
        .groupBy { it.merchant.uppercase(Locale.US).trim() }
        .mapNotNull { (_, rows) ->
            val datedRows = rows.sortedByDescending { it.transactionDate.orEmpty() }
            if (datedRows.size < 2) return@mapNotNull null

            val amounts = datedRows.mapNotNull { it.inrAmountValue }
            val averageAmount = amounts.average()
            val closeAmountCount = amounts.count { amount ->
                kotlin.math.abs(amount - averageAmount) <= maxOf(20.0, averageAmount * 0.12)
            }
            val distinctMonths = datedRows.mapNotNull { it.transactionDate?.take(7) }.distinct().size
            val subscriptionSignal = datedRows.any {
                it.transactionType == TransactionType.SUBSCRIPTION ||
                    it.transactionType == TransactionType.INVESTMENT ||
                    it.paymentMode.contains("mandate", ignoreCase = true) ||
                    it.paymentMode == PaymentMode.NACH.displayName() ||
                    it.merchant.contains("netflix", ignoreCase = true) ||
                    it.merchant.contains("mutual", ignoreCase = true) ||
                    it.merchant.contains("clearing", ignoreCase = true)
            }
            val shouldShow = subscriptionSignal || distinctMonths >= 2 || closeAmountCount >= 2
            if (!shouldShow) return@mapNotNull null

            val confidence = when {
                subscriptionSignal && datedRows.size >= 3 -> "High"
                distinctMonths >= 2 && closeAmountCount >= 2 -> "Medium"
                subscriptionSignal -> "Medium"
                else -> "Watch"
            }
            val latest = datedRows.first()
            RecurringCandidate(
                merchant = latest.merchant,
                expectedAmount = averageAmount,
                count = datedRows.size,
                lastSeenDate = latest.transactionDate,
                category = latest.category,
                transactionType = latest.transactionType,
                confidenceLabel = confidence
            )
        }
        .sortedWith(
            compareByDescending<RecurringCandidate> {
                when (it.confidenceLabel) {
                    "High" -> 3
                    "Medium" -> 2
                    else -> 1
                }
            }.thenByDescending { it.expectedAmount }
        )
        .take(8)
}

private fun List<TransactionUi>.sourceHealthRows(): List<SourceHealthRow> {
    return latestMonthTransactions()
        .filter { it.inrAmountValue != null }
        .groupBy { it.source }
        .map { (source, rows) ->
            SourceHealthRow(
                source = source,
                totalCount = rows.size,
                spendCount = rows.count { it.direction == DirectionUi.Debit && it.transactionType.countsAsSpend() },
                reviewCount = rows.count(TransactionUi::needsReview),
                fxCount = rows.count { !it.countsInInrTotals() },
                totalAmount = rows
                    .filter { it.direction == DirectionUi.Debit }
                    .sumOf { it.inrAmountValue ?: 0.0 }
            )
        }
        .sortedByDescending { it.totalCount }
}

private fun List<TransactionUi>.monthStoryItems(): List<MonthStoryItem> {
    val breakdown = monthBreakdown()
    val spendTransactions = latestMonthSpendTransactions()
        .filter { it.inrAmountValue != null }
    val topMerchant = monthSpendMerchantGroups().firstOrNull()
    val topCategory = monthSpendCategoryGroups().firstOrNull()
    val biggestDay = spendTransactions
        .groupBy { it.transactionDate ?: "Unknown" }
        .mapValues { entry -> entry.value.sumOf { it.inrAmountValue ?: 0.0 } }
        .maxByOrNull { it.value }
    val largestSpend = spendTransactions.maxByOrNull { it.inrAmountValue ?: 0.0 }
    val reviewCount = reviewCandidates().size
    val gmailOnlyCount = latestMonthTransactions().count { it.source == "Gmail" }
    val refundTotal = monthRefundSignals().sumOf { it.inrAmountValue ?: 0.0 }

    return listOfNotNull(
        topMerchant?.let {
            MonthStoryItem(
                label = "Top merchant",
                value = it.label,
                detail = it.total.formatInr(),
                category = it.category
            )
        },
        topCategory?.let {
            MonthStoryItem(
                label = "Top category",
                value = it.label,
                detail = it.total.formatInr(),
                category = it.category
            )
        },
        biggestDay?.let {
            MonthStoryItem(
                label = "Biggest day",
                value = it.key.recentDateLabel(),
                detail = it.value.formatInr(),
                category = largestSpend?.category ?: "Other"
            )
        },
        if (breakdown.recurringInvestments > 0.0) {
            MonthStoryItem(
                label = "Kept separate",
                value = "Recurring SIPs",
                detail = breakdown.recurringInvestments.formatInr(),
                category = "Investment"
            )
        } else {
            null
        },
        if (breakdown.oneTimeInvestments > 0.0) {
            MonthStoryItem(
                label = "One-time move",
                value = "Investments",
                detail = breakdown.oneTimeInvestments.formatInr(),
                category = "Investment"
            )
        } else {
            null
        },
        if (refundTotal > 0.0) {
            MonthStoryItem(
                label = "Signals",
                value = "Refunds",
                detail = refundTotal.formatInr(),
                category = "Refund"
            )
        } else {
            null
        },
        if (gmailOnlyCount > 0) {
            MonthStoryItem(
                label = "Coverage",
                value = "Gmail rows",
                detail = "$gmailOnlyCount found",
                category = "Utilities"
            )
        } else {
            null
        },
        if (reviewCount > 0) {
            MonthStoryItem(
                label = "Needs review",
                value = "$reviewCount rows",
                detail = reviewCandidates().sumOf { it.inrAmountValue ?: 0.0 }.formatInr(),
                category = "Other"
            )
        } else {
            null
        }
    )
}

private fun String.looksLikeRawPaymentHandle(): Boolean {
    return contains("@") || (any(Char::isDigit) && Regex("""^[A-Za-z0-9._-]{8,}$""").matches(this))
}

private fun List<TransactionUi>.selectedMonthKey(): String? {
    val validMonths = mapNotNull { transaction ->
        transaction.transactionDate
            ?.take(7)
            ?.takeIf { Regex("""\d{4}-\d{2}""").matches(it) }
    }
    val currentMonth = LocalDate.now().toString().take(7)
    return when {
        currentMonth in validMonths -> currentMonth
        else -> validMonths.maxOrNull()
    }
}

private fun List<TransactionUi>.availableMonthKeys(): List<String> {
    return mapNotNull { transaction ->
        transaction.transactionDate
            ?.take(7)
            ?.takeIf { Regex("""\d{4}-\d{2}""").matches(it) }
    }
        .distinct()
        .sortedDescending()
}

private fun TransactionUi.isInSelectedMonth(monthKey: String?): Boolean {
    return monthKey == null || transactionDate?.startsWith(monthKey) == true
}

private fun List<TransactionUi>.feedSourceLabel(): String {
    val sourceSet = map { it.source }.toSet()
    return when {
        sourceSet.isEmpty() -> "sample SMS"
        sourceSet.size == 1 && "SMS" in sourceSet -> "device SMS"
        sourceSet.size == 1 -> sourceSet.first()
        sourceSet.containsAll(listOf("SMS", "Gmail", "Manual")) -> "SMS + Gmail + Manual"
        sourceSet.containsAll(listOf("SMS", "Gmail")) -> "SMS + Gmail"
        sourceSet.containsAll(listOf("SMS", "Manual")) -> "SMS + Manual"
        sourceSet.containsAll(listOf("Gmail", "Manual")) -> "Gmail + Manual"
        else -> sourceSet.sorted().joinToString(" + ")
    }
}

private fun GmailImportSummary.displayLabel(): String {
    val fxLabel = when {
        fxRateFailures > 0 -> " FX lookup failed for $fxRateFailures."
        fxRatesUpdated > 0 -> " FX updated for $fxRatesUpdated."
        else -> ""
    }
    return "Imported $importedTransactions of $transactionsDetected detected. $skippedDuplicates matched SMS.$fxLabel"
}

@Composable
private fun DrilldownScreen(
    state: DrilldownState,
    allTransactions: List<TransactionUi>,
    onBack: () -> Unit,
    onTransactionClick: (TransactionUi) -> Unit
) {
    val palette = tapePalette()
    val transactions = remember(state, allTransactions) {
        state.filteredTransactions(allTransactions)
    }
    val amountRows = remember(transactions) {
        transactions.filter { it.inrAmountValue != null }
    }
    val total = remember(amountRows) {
        amountRows.sumOf { it.inrAmountValue ?: 0.0 }
    }
    val average = remember(amountRows, total) {
        if (amountRows.isNotEmpty()) total / amountRows.size else 0.0
    }
    val largest = remember(amountRows) {
        amountRows.maxByOrNull { it.inrAmountValue ?: 0.0 }
    }
    val reviewCount = remember(amountRows) {
        amountRows.count(TransactionUi::needsReview)
    }
    val sourceMix = remember(amountRows) {
        amountRows
            .groupingBy { it.source.uppercase(Locale.US) }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .joinToString(" - ") { "${it.value} ${it.key}" }
            .ifBlank { "NO SOURCE" }
    }
    val splitRows = remember(transactions, state.kind) {
        val grouped = when (state.kind) {
            DrilldownKind.Merchant -> transactions.groupBy { it.category }
            DrilldownKind.Category -> transactions.groupBy { it.merchant }
        }
        grouped
            .map { (label, rows) ->
                SummaryGroup(
                    label = label,
                    count = rows.size,
                    total = rows.sumOf { it.inrAmountValue ?: 0.0 },
                    currency = "INR",
                    category = rows.firstOrNull()?.category ?: label
                )
            }
            .sortedByDescending { it.total }
    }
    val dateGroups = remember(transactions) {
        transactions
            .sortedWith(
                compareByDescending<TransactionUi> { it.transactionDate.orEmpty() }
                    .thenByDescending { it.inrAmountValue ?: 0.0 }
            )
            .groupBy { it.transactionDate.recentDateLabel().uppercase(Locale.US) }
    }

    Scaffold(
        containerColor = palette.desk
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SegmentDeskBar(
                state = state,
                transactions = transactions,
                palette = palette,
                onBack = onBack
            )
            TapePaper(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                palette = palette
            ) {
                item {
                    SegmentCloseOutBlock(
                        state = state,
                        total = total,
                        average = average,
                        largest = largest,
                        reviewCount = reviewCount,
                        sourceMix = sourceMix,
                        lineCount = transactions.size,
                        palette = palette
                    )
                }
                item {
                    SegmentShareRule(
                        rows = splitRows,
                        total = total,
                        palette = palette
                    )
                }
                item {
                    SegmentSplitBlock(
                        state = state,
                        rows = splitRows,
                        palette = palette
                    )
                }
                item {
                    IndexBlockShell(
                        heading = "LINES ON THIS SEGMENT",
                        meta = "${transactions.size} PRINTED",
                        palette = palette
                    ) {
                        if (transactions.isEmpty()) {
                            IndexEmptyLine("NO LINES FOUND ON THIS SEGMENT", palette)
                        }
                    }
                }
                dateGroups.forEach { (label, rows) ->
                    item {
                        SegmentDateHeader(
                            label = label,
                            total = rows.sumOf { it.inrAmountValue ?: 0.0 },
                            palette = palette
                        )
                    }
                    items(rows, key = { it.sourceHash }) { transaction ->
                        SegmentTransactionLine(
                            transaction = transaction,
                            palette = palette,
                            onClick = { onTransactionClick(transaction) }
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun SegmentDeskBar(
    state: DrilldownState,
    transactions: List<TransactionUi>,
    palette: TapePalette,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.desk)
            .padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "<",
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onBack)
                .padding(top = 5.dp),
            color = palette.amber,
            fontFamily = SortedTapeFontFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "INDEX",
            color = palette.inkSoft,
            fontFamily = SortedTapeFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 3.sp,
            maxLines = 1
        )
        Text(
            text = " / ${state.kind.segmentStamp()}",
            color = palette.inkFaint,
            fontFamily = SortedTapeFontFamily,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            maxLines = 1
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "${transactions.size} LINES",
            color = palette.inkFaint,
            fontFamily = SortedTapeFontFamily,
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun SegmentCloseOutBlock(
    state: DrilldownState,
    total: Double,
    average: Double,
    largest: TransactionUi?,
    reviewCount: Int,
    sourceMix: String,
    lineCount: Int,
    palette: TapePalette
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .border(1.dp, palette.rule)
            .padding(horizontal = 12.dp, vertical = 13.dp)
    ) {
        Text(
            text = state.kind.segmentTitle(),
            color = palette.inkFaint,
            fontFamily = SortedTapeFontFamily,
            fontSize = 9.sp,
            letterSpacing = 1.7.sp,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = state.title.uppercase(Locale.US),
            color = palette.ink,
            fontFamily = SortedTapeFontFamily,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 27.sp,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(9.dp))
        Text(
            text = total.formatRupee(),
            color = palette.ink,
            fontFamily = SortedTapeFontFamily,
            fontSize = 32.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        TapeDoubleRule(palette = palette)
        TapeSummationRow(
            label = "$lineCount LINES PRINTED",
            value = total.formatRupee(),
            palette = palette
        )
        TapeSummationRow(
            label = "AVERAGE LINE",
            value = average.formatRupee(),
            palette = palette
        )
        TapeSummationRow(
            label = "LARGEST LINE",
            value = largest?.inrAmountValue?.formatRupee() ?: "NONE",
            palette = palette
        )
        TapeSummationRow(
            label = "$reviewCount LINES UNSTAMPED",
            value = if (reviewCount == 0) "CLEAR" else "NEEDS STAMP",
            palette = palette,
            color = if (reviewCount == 0) palette.inkSoft else palette.query
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.ruleFaint)
        )
        Text(
            text = sourceMix,
            modifier = Modifier.padding(top = 9.dp),
            color = palette.inkFaint,
            fontFamily = SortedTapeFontFamily,
            fontSize = 8.sp,
            letterSpacing = 1.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SegmentShareRule(
    rows: List<SummaryGroup>,
    total: Double,
    palette: TapePalette
) {
    if (rows.isEmpty() || total <= 0.0) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(8.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        rows.take(8).forEach { row ->
            Box(
                modifier = Modifier
                    .weight(((row.total / total).coerceAtLeast(0.04)).toFloat())
                    .fillMaxSize()
                    .background(if (row.count > 1) palette.amber else palette.rule)
            )
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = rows.take(2).joinToString(" - ") { "${it.label.uppercase(Locale.US)} ${(it.total / total * 100).toInt()}%" },
            modifier = Modifier.weight(1f),
            color = palette.inkFaint,
            fontFamily = SortedTapeFontFamily,
            fontSize = 8.sp,
            letterSpacing = 1.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "SEGMENT MIX",
            color = palette.inkFaint,
            fontFamily = SortedTapeFontFamily,
            fontSize = 8.sp,
            letterSpacing = 1.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun SegmentSplitBlock(
    state: DrilldownState,
    rows: List<SummaryGroup>,
    palette: TapePalette
) {
    IndexBlockShell(
        heading = if (state.kind == DrilldownKind.Merchant) "CATEGORY SPLIT" else "MERCHANT SPLIT",
        meta = "${rows.size} INDEXED",
        palette = palette
    ) {
        if (rows.isEmpty()) {
            IndexEmptyLine("NO SPLIT PRINTED", palette)
        } else {
            rows.take(6).forEach { row ->
                IndexEntryRow(
                    name = row.label.uppercase(Locale.US),
                    meta = "${row.count} LINE${if (row.count == 1) "" else "S"}",
                    value = row.total.formatRupee(),
                    palette = palette
                )
            }
        }
    }
}

@Composable
private fun SegmentDateHeader(
    label: String,
    total: Double,
    palette: TapePalette
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(18.dp)
                .height(1.dp)
                .background(palette.rule)
        )
        Text(
            text = " $label",
            modifier = Modifier.weight(1f),
            color = palette.inkFaint,
            fontFamily = SortedTapeFontFamily,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = total.formatRupee(),
            color = palette.inkFaint,
            fontFamily = SortedTapeFontFamily,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun SegmentTransactionLine(
    transaction: TransactionUi,
    palette: TapePalette,
    onClick: () -> Unit
) {
    val value = transaction.inrAmountValue ?: transaction.amountValue
    val query = transaction.needsReview()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .drawBehind {
                val y = size.height - 1.dp.toPx()
                drawLine(
                    color = palette.ruleFaint,
                    start = Offset(16.dp.toPx(), y),
                    end = Offset(size.width - 16.dp.toPx(), y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (query) "?" else "",
            modifier = Modifier.width(14.dp),
            color = palette.query,
            fontFamily = SortedTapeFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = transaction.merchant.uppercase(Locale.US),
                    modifier = Modifier.weight(1f),
                    color = palette.ink,
                    fontFamily = SortedTapeFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (transaction.direction == DirectionUi.Credit) "(${value.formatRupee()})" else value.formatRupee(),
                    color = if (transaction.direction == DirectionUi.Credit) palette.credit else if (query) palette.query else palette.ink,
                    fontFamily = SortedTapeFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SegmentStamp(transaction.category.uppercase(Locale.US), palette)
                Spacer(modifier = Modifier.width(6.dp))
                SegmentStamp(transaction.source.uppercase(Locale.US), palette)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = transaction.detail.ifBlank { transaction.paymentMode }.take(42),
                    modifier = Modifier.weight(1f),
                    color = palette.inkFaint,
                    fontFamily = SortedTapeFontFamily,
                    fontSize = 9.sp,
                    letterSpacing = 0.9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SegmentStamp(
    text: String,
    palette: TapePalette
) {
    Text(
        text = text,
        modifier = Modifier
            .border(1.dp, palette.rule, RoundedCornerShape(2.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        color = palette.inkSoft,
        fontFamily = SortedTapeFontFamily,
        fontSize = 8.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.7.sp,
        maxLines = 1
    )
}

private fun DrilldownKind.segmentStamp(): String {
    return when (this) {
        DrilldownKind.Merchant -> "MERCHANT"
        DrilldownKind.Category -> "SEGMENT"
    }
}

private fun DrilldownKind.segmentTitle(): String {
    return when (this) {
        DrilldownKind.Merchant -> "MERCHANT TAPE"
        DrilldownKind.Category -> "SEGMENT TAPE"
    }
}

@Composable
private fun DrilldownBreakdownRow(label: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryMiniDot(label)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.sp
        )
        Text(
            text = "$count",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp
        )
    }
}

private fun DrilldownState.filteredTransactions(
    allTransactions: List<TransactionUi>
): List<TransactionUi> {
    val sourceTransactions = if (spendOnly) {
        allTransactions.latestMonthSpendTransactions(monthKey)
    } else {
        allTransactions.latestMonthDebitTransactions(monthKey)
    }

    return sourceTransactions
        .filter { it.inrAmountValue != null }
        .filter { transaction ->
            when (kind) {
                DrilldownKind.Merchant -> transaction.merchant == group.label
                DrilldownKind.Category -> transaction.category == group.label
            }
        }
}

private fun DrilldownKind.label(): String {
    return when (this) {
        DrilldownKind.Merchant -> "Merchant transactions"
        DrilldownKind.Category -> "Category transactions"
    }
}

@Composable
private fun SummaryRail(
    title: String,
    groups: List<SummaryGroup>,
    onGroupClick: (SummaryGroup) -> Unit
) {
    if (groups.isEmpty()) return

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp
            )
            Text(
                text = "Top ${groups.size}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                letterSpacing = 0.sp
            )
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(modifier = Modifier.width(10.dp))
            }
            items(groups) { group ->
                SummaryGroupCard(
                    group = group,
                    onClick = { onGroupClick(group) }
                )
            }
            item {
                Spacer(modifier = Modifier.width(10.dp))
            }
        }
    }
}

@Composable
private fun SummaryGroupCard(
    group: SummaryGroup,
    onClick: () -> Unit
) {
    val accent = categoryColor(group.category)

    Surface(
        onClick = onClick,
        modifier = Modifier.width(184.dp),
        color = if (isDarkModeActive()) MaterialTheme.colorScheme.surface else categoryContainerColor(group.category),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .width(34.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent)
            )
            Spacer(modifier = Modifier.height(11.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryMiniDot(group.category)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = group.displayLabel(),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 0.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = group.total.formatMoney(group.currency),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${group.count} transaction${if (group.count == 1) "" else "s"}",
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                letterSpacing = 0.sp
            )
        }
    }
}

private fun SummaryGroup.displayLabel(): String {
    return if (currency.equals("INR", ignoreCase = true)) label else "$label ($currency)"
}

@Composable
private fun CategoryMiniDot(category: String) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(categoryColor(category))
    )
}

@Composable
private fun PermissionPrompt(onRequestPermission: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onRequestPermission)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Read transaction SMS",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Sorted scans messages locally and keeps transactions on this phone.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    letterSpacing = 0.sp
                )
            }
            Text(
                text = "Allow",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )
        }
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(
        text = label,
        modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 2.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp
    )
}

@Composable
private fun TransactionRow(
    transaction: TransactionUi,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val background by animateColorAsState(
        targetValue = if (pressed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        label = "rowBackground"
    )
    val elevation by animateDpAsState(
        targetValue = if (pressed) 1.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "rowElevation"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                pressed = !pressed
                onClick()
            },
        color = background,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = elevation
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryDot(transaction.category)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchant,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 0.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = transaction.detail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 0.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (transaction.direction == DirectionUi.Credit) "+${transaction.amount}" else transaction.amount,
                    color = if (transaction.direction == DirectionUi.Credit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    letterSpacing = 0.sp
                )
                Spacer(modifier = Modifier.height(5.dp))
                SourcePill(transaction.source)
            }
        }
    }
}

@Composable
private fun CategoryDot(category: String) {
    val color = categoryColor(category)

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
private fun categoryColor(category: String): Color {
    return if (isDarkModeActive()) {
        when (category) {
            "Food",
            "Groceries",
            "Investment",
            "Refund",
            "Subscriptions",
            "Reward",
            "Transfer",
            "Shopping",
            "Health",
            "Entertainment",
            "Transport",
            "Utilities",
            "Fuel" -> Color(0xFFFBC02D)
            else -> Color(0xFFFBC02D)
        }
    } else {
        when (category) {
            "Food" -> Color(0xFFFF6B6B)
            "Groceries" -> Color(0xFF00C853)
            "Investment" -> Color(0xFF2979FF)
            "Refund" -> Color(0xFFFFB300)
            "Subscriptions" -> Color(0xFFAA00FF)
            "Reward" -> Color(0xFFFFEA00)
            "Transfer" -> Color(0xFF536DFE)
            "Shopping" -> Color(0xFFFF4081)
            "Health" -> Color(0xFFFF1744)
            "Entertainment" -> Color(0xFF7C4DFF)
            "Transport" -> Color(0xFF00B8D4)
            "Utilities" -> Color(0xFFFF9100)
            "Fuel" -> Color(0xFF64DD17)
            else -> MaterialTheme.colorScheme.primary
        }
    }
}

@Composable
private fun categoryContainerColor(category: String): Color {
    val color = categoryColor(category)
    return color.copy(alpha = if (isDarkModeActive()) 0.16f else 0.11f)
}

@Composable
private fun homeMixColor(category: String): Color {
    return if (isDarkModeActive()) {
        when (category) {
            "Investment",
            "Transfer",
            "Shopping",
            "Groceries",
            "Food",
            "Utilities",
            "Subscriptions",
            "Health",
            "Refund",
            "Reward",
            "Transport",
            "Fuel" -> Color(0xFFFBC02D)
            else -> Color(0xFFFBC02D)
        }
    } else {
        categoryColor(category)
    }
}

@Composable
private fun homeMixContainerColor(category: String): Color {
    return homeMixColor(category).copy(alpha = if (isDarkModeActive()) 0.18f else 0.11f)
}

@Composable
private fun isDarkModeActive(): Boolean {
    return MaterialTheme.colorScheme.background == Color(0xFF000000)
}

@Composable
private fun SourcePill(source: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = source,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun TransactionDetail(
    transaction: TransactionUi,
    saveState: CorrectionSaveState,
    onCorrect: (TransactionCorrectionDraft) -> Unit,
    onIgnore: () -> Unit
) {
    var editing by remember(transaction.id, transaction.sourceHash) { mutableStateOf(false) }
    var confirmIgnore by remember(transaction.id, transaction.sourceHash) { mutableStateOf(false) }
    var merchant by remember(transaction.id, transaction.merchant) { mutableStateOf(transaction.merchant) }
    var category by remember(transaction.id, transaction.category) { mutableStateOf(transaction.category) }
    var miscCategory by remember(transaction.id, transaction.miscCategory) { mutableStateOf(transaction.miscCategory) }
    var transactionType by remember(transaction.id, transaction.transactionType) { mutableStateOf(transaction.transactionType) }
    var rememberRule by remember(transaction.id, transaction.sourceHash) { mutableStateOf(true) }
    var validationError by remember(transaction.id, transaction.sourceHash) { mutableStateOf<String?>(null) }
    val categories = listOf(
        "Food",
        "Groceries",
        "Shopping",
        "Subscriptions",
        "Transport",
        "Utilities",
        "Health",
        "Entertainment",
        "Investment",
        "Transfer",
        "Income",
        "Refund",
        "Reward",
        "Other"
    )
    val transactionTypes = listOf(
        TransactionType.EXPENSE,
        TransactionType.SUBSCRIPTION,
        TransactionType.TRANSFER,
        TransactionType.INVESTMENT,
        TransactionType.INCOME,
        TransactionType.REFUND,
        TransactionType.REWARD
    )
    val statusText = validationError ?: saveState.error ?: saveState.message

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 22.dp, end = 22.dp, bottom = 34.dp)
    ) {
        Text(
            text = transaction.merchant,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = transaction.amount,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 16.sp,
            letterSpacing = 0.sp
        )
        Spacer(modifier = Modifier.height(18.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                listOf(
                    transaction.category,
                    transaction.transactionType.displayName(),
                    transaction.source,
                    if (transaction.direction == DirectionUi.Credit) "Credit" else "Debit"
                )
            ) { label ->
                DetailChip(label)
            }
        }
        AnimatedVisibility(visible = true) {
            Text(
                text = transaction.detail,
                modifier = Modifier.padding(top = 18.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                letterSpacing = 0.sp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                modifier = Modifier.weight(1f),
                enabled = !saveState.isSaving,
                onClick = {
                    editing = !editing
                    confirmIgnore = false
                    validationError = null
                }
            ) {
                Text(if (editing) "Close edit" else "Correct")
            }
            TextButton(
                modifier = Modifier.weight(1f),
                enabled = !saveState.isSaving,
                onClick = {
                    confirmIgnore = !confirmIgnore
                    editing = false
                }
            ) {
                Text(if (confirmIgnore) "Cancel" else "Ignore")
            }
        }
        AnimatedVisibility(visible = confirmIgnore) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                Text(
                    text = "Hide this transaction from Sorted",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "It will stay ignored on future scans.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    letterSpacing = 0.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !saveState.isSaving,
                    onClick = onIgnore
                ) {
                    Text(if (saveState.isSaving) "Saving" else "Ignore transaction")
                }
            }
        }
        AnimatedVisibility(visible = editing) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it.take(48) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Merchant") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = miscCategory,
                    onValueChange = { miscCategory = it.take(36) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Merchant tag") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                ChoiceRail(
                    title = "Category",
                    choices = categories,
                    selected = category,
                    onSelected = { selected ->
                        category = selected
                        if (selected == "Investment") transactionType = TransactionType.INVESTMENT
                        if (selected == "Transfer") transactionType = TransactionType.TRANSFER
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                ChoiceRail(
                    title = "Type",
                    choices = transactionTypes.map { it.displayName() },
                    selected = transactionType.displayName(),
                    onSelected = { selected ->
                        transactionType = transactionTypes.first { it.displayName() == selected }
                        category = when (transactionType) {
                            TransactionType.INVESTMENT -> "Investment"
                            TransactionType.TRANSFER -> "Transfer"
                            TransactionType.INCOME -> "Income"
                            TransactionType.REFUND -> "Refund"
                            TransactionType.REWARD -> "Reward"
                            else -> category
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SelectableChip(
                        label = "Remember",
                        selected = rememberRule,
                        modifier = Modifier.weight(1f),
                        onClick = { rememberRule = true }
                    )
                    SelectableChip(
                        label = "This only",
                        selected = !rememberRule,
                        modifier = Modifier.weight(1f),
                        onClick = { rememberRule = false }
                    )
                }
                if (statusText != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = statusText,
                        color = if (validationError == null && saveState.error == null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color(0xFFFF8E8E)
                        },
                        fontSize = 13.sp,
                        letterSpacing = 0.sp
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !saveState.isSaving,
                    onClick = {
                        validationError = when {
                            merchant.isBlank() -> "Enter a merchant."
                            category.isBlank() -> "Pick a category."
                            miscCategory.isBlank() -> "Enter a merchant tag."
                            else -> null
                        }
                        if (validationError == null) {
                            onCorrect(
                                TransactionCorrectionDraft(
                                    transaction = transaction,
                                    merchant = merchant,
                                    miscCategory = miscCategory,
                                    category = category,
                                    transactionType = transactionType,
                                    rememberRule = rememberRule
                                )
                            )
                        }
                    }
                ) {
                    Text(if (saveState.isSaving) "Saving" else "Save correction")
                }
            }
        }
        if (!editing && !confirmIgnore && statusText != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = statusText,
                color = if (saveState.error == null) MaterialTheme.colorScheme.primary else Color(0xFFFF8E8E),
                fontSize = 13.sp,
                letterSpacing = 0.sp
            )
        }
    }
}

@Composable
private fun DetailChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            letterSpacing = 0.sp
        )
    }
}

private fun Double.formatInr(): String {
    return "INR " + String.format(Locale.US, "%,.2f", this)
}

private fun Double.formatRupee(): String {
    return "₹" + String.format(Locale.US, "%,.2f", this)
}

private fun Double.formatCompactInr(): String {
    val magnitude = if (this < 0.0) -this else this
    val value = when {
        magnitude >= 100_000.0 -> String.format(Locale.US, "%.1fL", this / 100_000.0)
        magnitude >= 1_000.0 -> String.format(Locale.US, "%.1fK", this / 1_000.0)
        else -> String.format(Locale.US, "%.0f", this)
    }.replace(".0", "")
    return "INR $value"
}

private fun Double.formatRupeeCompact(): String {
    val magnitude = if (this < 0.0) -this else this
    val value = when {
        magnitude >= 100_000.0 -> String.format(Locale.US, "%.1fL", this / 100_000.0)
        magnitude >= 1_000.0 -> String.format(Locale.US, "%.1fK", this / 1_000.0)
        else -> String.format(Locale.US, "%.0f", this)
    }.replace(".0", "")
    return "₹$value"
}

private fun Double.formatMoney(currency: String?): String {
    val currencyCode = currency.normalizedCurrency()
    return when (currencyCode) {
        "INR" -> formatInr()
        "USD" -> "USD " + String.format(Locale.US, "%,.2f", this)
        else -> "$currencyCode " + String.format(Locale.US, "%,.2f", this)
    }
}

private fun fxRateFor(
    transactionDate: String?,
    currency: String,
    fxRates: Map<FxRateKey, FxRateEntity>
): FxRateEntity? {
    if (transactionDate.isNullOrBlank() || currency.equals("INR", ignoreCase = true)) return null
    return fxRates[FxRateKey(transactionDate, currency.uppercase(Locale.US), "INR")]
}

private fun inrEquivalentValue(
    amount: Double,
    currency: String,
    fxRate: FxRateEntity?
): Double? {
    return when {
        currency.equals("INR", ignoreCase = true) -> amount
        fxRate != null -> amount * fxRate.rate
        else -> null
    }
}

private fun Double.formatFxRate(): String {
    return String.format(Locale.US, "%,.4f", this)
}

private fun TransactionUi.countsInInrTotals(): Boolean {
    return currency.equals("INR", ignoreCase = true)
}

private fun String?.normalizedCurrency(): String {
    return orEmpty().ifBlank { "INR" }.uppercase(Locale.US)
}

private fun PaymentMode.displayName(): String {
    return when (this) {
        PaymentMode.UPI -> "UPI"
        PaymentMode.UPI_MANDATE -> "UPI mandate"
        PaymentMode.CARD -> "Card"
        PaymentMode.NET_BANKING -> "Net banking"
        PaymentMode.NEFT -> "NEFT"
        PaymentMode.IMPS -> "IMPS"
        PaymentMode.RTGS -> "RTGS"
        PaymentMode.NACH -> "NACH"
        PaymentMode.ECS -> "ECS"
        PaymentMode.BANK_TRANSFER -> "Bank transfer"
        PaymentMode.ATM -> "ATM"
        PaymentMode.CHEQUE -> "Cheque"
        PaymentMode.CASH -> "Cash"
        PaymentMode.WALLET -> "Wallet"
        PaymentMode.PPI -> "PPI"
        PaymentMode.BILLPAY -> "BillPay"
        PaymentMode.PAYMENT_GATEWAY -> "Payment gateway"
        PaymentMode.FASTAG -> "FASTag"
        PaymentMode.PROVIDENT_FUND -> "Provident fund"
        PaymentMode.UNKNOWN -> "Unknown"
    }
}

private fun ImportSource.displayLabel(): String {
    return when (this) {
        ImportSource.SMS -> "SMS"
        ImportSource.GMAIL -> "Gmail"
        ImportSource.MANUAL -> "Manual"
    }
}

private fun TransactionType.countsAsSpend(): Boolean {
    return this == TransactionType.EXPENSE || this == TransactionType.SUBSCRIPTION
}

private fun TransactionType.displayName(): String {
    return when (this) {
        TransactionType.EXPENSE -> "Spend"
        TransactionType.INCOME -> "Income"
        TransactionType.REFUND -> "Refund"
        TransactionType.TRANSFER -> "Transfer"
        TransactionType.INVESTMENT -> "Investment"
        TransactionType.SUBSCRIPTION -> "Subscription"
        TransactionType.REWARD -> "Reward"
        TransactionType.UNKNOWN -> "Other"
    }
}

private fun String.monthSpendLabel(): String {
    return "${monthNameLabel()} INR spend"
}

private fun String.monthMovementLabel(): String {
    return "${monthNameLabel()} money moved"
}

private fun String.monthNameLabel(): String {
    val monthName = when (substringAfter("-")) {
        "01" -> "January"
        "02" -> "February"
        "03" -> "March"
        "04" -> "April"
        "05" -> "May"
        "06" -> "June"
        "07" -> "July"
        "08" -> "August"
        "09" -> "September"
        "10" -> "October"
        "11" -> "November"
        "12" -> "December"
        else -> "Month"
    }
    return monthName
}

private fun String.monthShortLabel(): String {
    val year = substringBefore("-", "")
    val shortMonth = when (substringAfter("-")) {
        "01" -> "Jan"
        "02" -> "Feb"
        "03" -> "Mar"
        "04" -> "Apr"
        "05" -> "May"
        "06" -> "Jun"
        "07" -> "Jul"
        "08" -> "Aug"
        "09" -> "Sep"
        "10" -> "Oct"
        "11" -> "Nov"
        "12" -> "Dec"
        else -> "Month"
    }
    return if (year.length == 4) "$shortMonth ${year.takeLast(2)}" else shortMonth
}

private fun String?.recentDateLabel(): String {
    val date = this?.toLocalDateOrNull() ?: return "Date unknown"
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> "${date.dayOfMonth} ${date.monthValue.shortMonthLabel()} ${date.year}"
    }
}

private fun Int.shortMonthLabel(): String {
    return when (this) {
        1 -> "Jan"
        2 -> "Feb"
        3 -> "Mar"
        4 -> "Apr"
        5 -> "May"
        6 -> "Jun"
        7 -> "Jul"
        8 -> "Aug"
        9 -> "Sep"
        10 -> "Oct"
        11 -> "Nov"
        12 -> "Dec"
        else -> "Date"
    }
}
