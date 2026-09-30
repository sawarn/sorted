package com.sorted.app

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.app.DatePickerDialog
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
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.sorted.app.engine.OutflowPolicy
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
    val status: TransactionStatus,
    val transactionType: TransactionType,
    val transactionDate: String?,
    val transactionTime: String?,
    val sourceReceivedDate: String?,
    val note: String?,
    val accountHint: String?,
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
    val creditCount: Int,
    val totalCredits: Double,
    val transfers: Double,
    val investments: Double,
    val recurringInvestments: Double,
    val oneTimeInvestments: Double,
    val refunds: Double,
    val income: Double,
    val rewards: Double,
    val fxConverted: Double
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
    val error: String? = null,
    val sourceHash: String? = null,
    val draft: ManualTransactionDraft? = null
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
    val amount: Double,
    val transactionDate: String?,
    val note: String,
    val rememberRule: Boolean
)

private data class DrilldownState(
    val title: String,
    val kind: DrilldownKind,
    val group: SummaryGroup,
    val spendOnly: Boolean = false,
    val monthKey: String? = null,
    val monthKeys: List<String>? = null
)

private enum class DrilldownKind {
    Merchant,
    Category
}

private enum class SortedTab(
    val label: String,
    val icon: SortedNavIcon
) {
    Home("Home", SortedNavIcon.Home),
    Insights("Insights", SortedNavIcon.Insights),
    Capture("Add", SortedNavIcon.Capture),
    RuleCenter("Rules", SortedNavIcon.RuleCenter)
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

private val SortedHomeWeight = FontWeight(608)

@OptIn(ExperimentalTextApi::class)
private fun sortedHomeFont(weight: FontWeight) = Font(
    R.font.inter_tight,
    weight,
    variationSettings = FontVariation.Settings(weight, FontStyle.Normal)
)

private val SortedHomeDesignFontFamily = FontFamily(
    sortedHomeFont(FontWeight.Normal),
    sortedHomeFont(FontWeight.Medium),
    sortedHomeFont(FontWeight.SemiBold),
    sortedHomeFont(FontWeight.Bold),
    sortedHomeFont(SortedHomeWeight)
)
private val SortedHomeFontFamily = SortedHomeDesignFontFamily

private enum class SyncSource {
    Sms,
    Gmail,
    All
}

private enum class DirectionUi {
    Debit,
    Credit,
    Unknown
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

private fun saveManualTransaction(context: Context, draft: ManualTransactionDraft): String {
    val now = System.currentTimeMillis()
    val merchant = draft.merchant.trim()
    val sourceHash = "manual:$now:${merchant}:${draft.amount}:${draft.date}".stableHash()
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
                sourceHash = sourceHash,
                sourceReceivedDate = draft.date,
                parsed = parsed
            )
        )
    )
    return sourceHash
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
    val directionUi = when (direction) {
        Direction.DEBIT -> DirectionUi.Debit
        Direction.CREDIT -> DirectionUi.Credit
        Direction.UNKNOWN -> DirectionUi.Unknown
    }
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
        status = status,
        transactionType = transactionType,
        transactionDate = transactionDate,
        transactionTime = transactionTime,
        sourceReceivedDate = null,
        note = note,
        accountHint = null,
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
    val directionUi = when (direction) {
        Direction.DEBIT -> DirectionUi.Debit
        Direction.CREDIT -> DirectionUi.Credit
        Direction.UNKNOWN -> DirectionUi.Unknown
    }
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
        status = status,
        transactionType = transactionType,
        transactionDate = transactionDate,
        transactionTime = transactionTime,
        sourceReceivedDate = sourceReceivedDate,
        note = note,
        accountHint = accountHint,
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
            background = Color(0xFF05110F),
            surface = Color(0xFF0D2522),
            surfaceVariant = Color(0xFF123330),
            primary = Color(0xFFD79A3F),
            secondary = Color(0xFF7FB3A4),
            tertiary = Color(0xFFD79A3F),
            onBackground = Color(0xFFE7F0EC),
            onSurface = Color(0xFFE7F0EC),
            onSurfaceVariant = Color(0xFF93AAA4),
            onPrimary = Color(0xFF05110F)
        )
    } else {
        lightColorScheme(
            background = Color(0xFFF6F8F2),
            surface = Color(0xFFF6F8F2),
            surfaceVariant = Color(0xFFE9EFE2),
            primary = Color(0xFFA9522A),
            secondary = Color(0xFF4E8471),
            tertiary = Color(0xFFC0642F),
            onBackground = Color(0xFF17241E),
            onSurface = Color(0xFF17241E),
            onSurfaceVariant = Color(0xFF566A5E),
            onPrimary = Color(0xFFF6F8F2)
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

    CompositionLocalProvider(LocalSortedDarkMode provides darkMode) {
        MaterialTheme(
            colorScheme = colors,
            typography = Typography().withFontFamily(appFontFamily),
            content = content
        )
    }
}

private val LocalSortedDarkMode = staticCompositionLocalOf { false }

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
                fontWeight = FontWeight.Bold,
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
private fun SortedTallyMark(
    ink: Color,
    clay: Color,
    modifier: Modifier = Modifier,
    tickAlphas: List<Float> = List(5) { 1f }
) {
    Canvas(modifier = modifier) {
        val sx = size.width / 48f
        val sy = size.height / 48f
        listOf(7f, 16f, 25f, 34f).forEachIndexed { index, x ->
            drawRoundRect(
                color = ink.copy(alpha = tickAlphas.getOrElse(index) { 1f }),
                topLeft = Offset(x * sx, 10f * sy),
                size = Size(4.6f * sx, 28f * sy),
                cornerRadius = CornerRadius(2.3f * sx, 2.3f * sy)
            )
        }
        rotate(degrees = -13f, pivot = Offset(24f * sx, 24f * sy)) {
            drawRoundRect(
                color = clay.copy(alpha = tickAlphas.getOrElse(4) { 1f }),
                topLeft = Offset(2.5f * sx, 21.7f * sy),
                size = Size(43f * sx, 4.6f * sy),
                cornerRadius = CornerRadius(2.3f * sx, 2.3f * sy)
            )
        }
    }
}

@Composable
private fun SortedLoadingScreen() {
    val isDark = isDarkModeActive()
    val background = if (isDark) Color(0xFF0F2C28) else Color(0xFFDFEACF)
    val ink = if (isDark) Color(0xFFE7F0EC) else Color(0xFF17241E)
    val muted = if (isDark) Color(0xFF93AAA4) else Color(0xFF566A5E)
    val clay = if (isDark) Color(0xFFD79A3F) else Color(0xFFA9522A)
    val activity = LocalContext.current.findComponentActivity()
    SideEffect {
        activity?.window?.let { window ->
            window.statusBarColor = background.toArgb()
            window.navigationBarColor = background.toArgb()
        }
    }
    val easeInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
    val transition = rememberInfiniteTransition(label = "sorted_loading_tally")
    val breathe by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loading_tick_breathe"
    )
    val slide by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = easeInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loading_rule_slide"
    )
    val tickAlphas = List(5) { index ->
        val shift = index * 0.14f / 1.6f
        val wave = sin(((breathe - shift) * 2.0 * PI)).toFloat()
        0.22f + 0.78f * ((wave + 1f) / 2f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
        ) {
            val radius = 1.6.dp.toPx()
            val step = 10.dp.toPx()
            var x = 5.dp.toPx()
            while (x < size.width) {
                drawCircle(
                    color = ink.copy(alpha = 0.13f),
                    radius = radius,
                    center = Offset(x, 5.dp.toPx())
                )
                x += step
            }
        }
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SortedTallyMark(
                ink = ink,
                clay = clay,
                tickAlphas = tickAlphas,
                modifier = Modifier.size(76.dp)
            )
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "Sorted",
                color = ink,
                fontFamily = SortedHomeDesignFontFamily,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-1).sp,
                lineHeight = 34.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(9.dp))
            Text(
                text = "YOUR MONEY, COUNTED",
                color = muted,
                fontFamily = SortedHomeDesignFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.9.sp,
                maxLines = 1
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(46.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(ink.copy(alpha = 0.18f))
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = 28.dp * slide)
                        .width(18.dp)
                        .height(2.dp)
                        .background(ink)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Stays on this phone",
                color = muted,
                fontFamily = SortedHomeDesignFontFamily,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
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
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            val snapshot = feedState.transactions
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        val output = appContext.contentResolver.openOutputStream(uri)
                            ?: error("Could not open the selected file.")
                        output.bufferedWriter(Charsets.UTF_8).use { writer ->
                            writer.appendLine("date,merchant,amount,currency,direction,category,type,import")
                            snapshot.forEach { transaction ->
                                listOf(
                                    transaction.transactionDate.orEmpty(),
                                    transaction.merchant,
                                    transaction.amountValue.toString(),
                                    transaction.currency,
                                    transaction.direction.name,
                                    transaction.category,
                                    transaction.transactionType.name,
                                    transaction.source
                                ).joinToString(",") { it.toCsvCell() }.also(writer::appendLine)
                            }
                        }
                    }
                }.onSuccess {
                    syncStatus = "Transactions exported"
                }.onFailure { error ->
                    syncStatus = error.message?.take(100) ?: "Export failed"
                }
            }
        }
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
                val saved = withContext(Dispatchers.IO) {
                    val sourceHash = saveManualTransaction(appContext, draft)
                    sourceHash to loadFeedState(appContext, hasPermission)
                }
                updateFeed(saved.second.transactions)
                manualSaveState = ManualSaveState(
                    message = "Payment added",
                    sourceHash = saved.first,
                    draft = draft
                )
            } catch (error: Throwable) {
                manualSaveState = ManualSaveState(
                    error = error.message?.take(160) ?: error.javaClass.simpleName
                )
            }
        }
    }

    fun undoManualDraft(sourceHash: String) {
        manualSaveState = manualSaveState.copy(isSaving = true, error = null)
        scope.launch {
            try {
                val refreshed = withContext(Dispatchers.IO) {
                    check(TransactionRepository(appContext).deleteManualTransaction(sourceHash)) {
                        "This payment could not be undone."
                    }
                    loadFeedState(appContext, hasPermission)
                }
                updateFeed(refreshed.transactions)
                manualSaveState = ManualSaveState()
            } catch (error: Throwable) {
                manualSaveState = manualSaveState.copy(
                    isSaving = false,
                    error = error.message?.take(160) ?: "Could not undo this payment."
                )
            }
        }
    }

    fun saveCorrectionDraft(
        draft: TransactionCorrectionDraft,
        showDetailsAfterSave: Boolean = true
    ) {
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
                            rememberRule = draft.rememberRule,
                            amount = draft.amount,
                            transactionDate = draft.transactionDate,
                            note = draft.note
                        )
                    )
                    if (!saved) {
                        throw IllegalStateException("Transaction was not found.")
                    }
                    loadPersistedTransactions(appContext)
                }
                updateFeed(transactions)
                selected = if (showDetailsAfterSave) {
                    transactions.firstOrNull { it.id == transactionId }
                } else {
                    null
                }
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
            Log.e(LogTag, "Gmail authorization returned no result data")
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
                    MerchantCategoryDetailScreen(
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
                    selectedMonthKey = selectedHomeMonthKey ?: feedState.transactions.selectedMonthKey(),
                    onBack = { explainOpen = false },
                    onTransactionClick = { openTransaction(it) },
                    onOpenReview = { sortInboxOpen = true }
                )
            }

            SortedRoute.SortInbox -> {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        SortedBottomBar(
                            selectedTab = selectedTab,
                            hasReview = feedState.transactions.reviewCandidates(
                                selectedHomeMonthKey ?: feedState.transactions.selectedMonthKey()
                            ).isNotEmpty(),
                            reviewSelected = true,
                            onTabSelected = { tab ->
                                sortInboxOpen = false
                                selectedTab = tab
                            },
                            onOpenSync = {
                                sortInboxOpen = false
                                syncChooserOpen = true
                            },
                            onOpenReview = {}
                        )
                    }
                ) { padding ->
                    SortInboxScreen(
                        feedState = feedState,
                        selectedMonthKey = selectedHomeMonthKey,
                        modifier = Modifier.padding(padding),
                        onBack = { sortInboxOpen = false },
                        onTransactionClick = { openTransaction(it) },
                        onCorrect = { transaction, category, transactionType, rememberRule ->
                            saveCorrectionDraft(
                                draft = TransactionCorrectionDraft(
                                    transaction = transaction,
                                    merchant = transaction.merchant,
                                    miscCategory = transaction.miscCategory,
                                    category = category,
                                    transactionType = transactionType,
                                    amount = transaction.amountValue,
                                    transactionDate = transaction.transactionDate,
                                    note = transaction.note.orEmpty(),
                                    rememberRule = rememberRule
                                ),
                                showDetailsAfterSave = false
                            )
                        },
                        onOpenSync = {
                            sortInboxOpen = false
                            syncChooserOpen = true
                        },
                        onOpenRules = {
                            sortInboxOpen = false
                            ruleCenterOpen = true
                        }
                    )
                }
            }

            SortedRoute.RuleCenter -> {
                RuleCenterScreen(
                    modifier = Modifier,
                    onBack = { ruleCenterOpen = false }
                )
            }

            SortedRoute.Settings -> {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        SortedBottomBar(
                            selectedTab = selectedTab,
                            hasReview = feedState.transactions.reviewCandidates(selectedHomeMonthKey ?: feedState.transactions.selectedMonthKey()).isNotEmpty(),
                            highlightSelection = false,
                            onTabSelected = { tab -> settingsOpen = false; selectedTab = tab },
                            onOpenSync = { settingsOpen = false; syncChooserOpen = true },
                            onOpenReview = { settingsOpen = false; sortInboxOpen = true }
                        )
                    }
                ) { padding ->
                    SettingsScreen(
                        themeMode = themeMode,
                        feedState = feedState,
                        gmailState = gmailState,
                        modifier = Modifier.padding(padding),
                        onThemeModeChange = onThemeModeChange,
                        onBack = { settingsOpen = false },
                        onRequestSmsPermission = { smsPermissionLauncher.launch(Manifest.permission.READ_SMS) },
                        onOpenSmsSettings = {
                            appContext.startActivity(
                                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${appContext.packageName}")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        },
                        onImportGmail = { requestGmailImport() },
                        onExport = { exportLauncher.launch("sorted-transactions.csv") },
                        onDeleteLocalData = {
                            scope.launch {
                                withContext(Dispatchers.IO) { TransactionRepository(appContext).deleteAllLocalData() }
                                updateFeed(emptyList())
                            }
                        },
                        onOpenRuleCenter = {
                            settingsOpen = false
                            ruleCenterOpen = true
                        }
                    )
                }
            }

            SortedRoute.Loading -> {
                SortedLoadingScreen()
            }

            SortedRoute.Main -> {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        if (selectedTab != SortedTab.Capture) {
                            SortedBottomBar(
                                selectedTab = selectedTab,
                                hasReview = feedState.transactions.reviewCandidates(
                                    selectedHomeMonthKey ?: feedState.transactions.selectedMonthKey()
                                ).isNotEmpty(),
                                onTabSelected = {
                                    syncChooserOpen = false
                                    selectedTab = it
                                },
                                onOpenSync = { syncChooserOpen = !syncChooserOpen },
                                onOpenReview = { sortInboxOpen = true }
                            )
                        }
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
                                    onMerchantClick = { group, monthKeys ->
                                        drilldown = DrilldownState(
                                            title = group.label,
                                            kind = DrilldownKind.Merchant,
                                            group = group,
                                            spendOnly = true,
                                            monthKey = selectedHomeMonthKey ?: feedState.transactions.selectedMonthKey(),
                                            monthKeys = monthKeys
                                        )
                                    },
                                    onCategoryClick = { group, monthKeys ->
                                        drilldown = DrilldownState(
                                            title = group.label,
                                            kind = DrilldownKind.Category,
                                            group = group,
                                            spendOnly = true,
                                            monthKey = selectedHomeMonthKey ?: feedState.transactions.selectedMonthKey(),
                                            monthKeys = monthKeys
                                        )
                                    },
                                    onTransactionClick = { openTransaction(it) },
                                    onOpenReview = { sortInboxOpen = true }
                                )

                                SortedTab.Capture -> CaptureTabContent(
                                    feedState = feedState,
                                    modifier = Modifier.padding(padding),
                                    saveState = manualSaveState,
                                    onSave = { draft -> saveManualDraft(draft) },
                                    onUndo = { sourceHash -> undoManualDraft(sourceHash) },
                                    onClose = {
                                        manualSaveState = ManualSaveState()
                                        selectedTab = SortedTab.Home
                                    },
                                    onResetSaved = { manualSaveState = ManualSaveState() }
                                )

                                SortedTab.RuleCenter -> RuleCenterScreen(
                                    modifier = Modifier.padding(padding),
                                    onSettings = { settingsOpen = true },
                                    onBack = null
                                )
                            }
                        }
                        if (selectedTab != SortedTab.Capture) {
                            SyncChooserBar(
                                visible = syncChooserOpen,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 10.dp),
                                onSync = { syncSource(it) }
                            )
                            SyncStatusPill(
                                message = syncStatus,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = if (syncChooserOpen) 170.dp else 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    selected?.let { transaction ->
        val sheetPalette = homePalette()
        val categoryMonthRows = feedState.transactions.filter {
            it.transactionDate?.take(7) == transaction.transactionDate?.take(7) &&
                it.countsTowardSpentTotal()
        }
        val categoryRows = categoryMonthRows.filter { it.category == transaction.category }
        val categoryTotal = categoryRows.sumOf { it.inrAmountValue ?: 0.0 }
        val monthTotal = categoryMonthRows.sumOf { it.inrAmountValue ?: 0.0 }
        val categoryShare = if (monthTotal > 0.0) (categoryTotal / monthTotal * 100).toInt() else 0
        ModalBottomSheet(
            onDismissRequest = {
                correctionSaveState = CorrectionSaveState()
                selected = null
            },
            containerColor = sheetPalette.background,
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
        ) {
            TransactionDetail(
                transaction = transaction,
                saveState = correctionSaveState,
                onCorrect = { draft -> saveCorrectionDraft(draft) },
                onIgnore = { ignoreTransaction(transaction) },
                categoryShare = categoryShare,
                onClose = {
                    correctionSaveState = CorrectionSaveState()
                    selected = null
                },
                onOpenCategory = {
                    drilldown = DrilldownState(
                        title = transaction.category,
                        kind = DrilldownKind.Category,
                        group = SummaryGroup(
                            label = transaction.category,
                            count = categoryRows.size,
                            total = categoryTotal,
                            currency = "INR",
                            category = transaction.category
                        ),
                        spendOnly = transaction.countsTowardSpentTotal(),
                        monthKey = transaction.transactionDate?.take(7),
                        monthKeys = listOfNotNull(transaction.transactionDate?.take(7))
                    )
                    selected = null
                }
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
        onTransactionClick = onTransactionClick,
        onMerchantClick = onMerchantClick,
        onCategoryClick = onCategoryClick
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
    onTransactionClick: (TransactionUi) -> Unit,
    onMerchantClick: (SummaryGroup) -> Unit,
    onCategoryClick: (SummaryGroup) -> Unit
) {
    val palette = homePalette()
    val activity = LocalContext.current.findComponentActivity()
    SideEffect {
        activity?.window?.let { window ->
            window.statusBarColor = palette.header.toArgb()
            window.navigationBarColor = palette.nav.toArgb()
        }
    }
    val activeMonthKey = selectedMonthKey ?: months.firstOrNull()
    val monthTransactions = remember(feedState.transactions, activeMonthKey) {
        feedState.transactions.latestMonthTransactions(activeMonthKey)
    }
    val spendTransactions = remember(feedState.transactions, activeMonthKey) {
        feedState.transactions.latestMonthSpendTransactions(activeMonthKey)
    }
    val breakdown = remember(feedState.transactions, activeMonthKey) {
        feedState.transactions.monthBreakdown(activeMonthKey)
    }
    val reviewRows = remember(feedState.transactions, activeMonthKey) {
        feedState.transactions.reviewCandidates(activeMonthKey)
    }
    val merchantGroups = remember(feedState.transactions, activeMonthKey) {
        feedState.transactions.monthSpendMerchantGroups(activeMonthKey).take(3)
    }
    val categoryGroups = remember(feedState.transactions, activeMonthKey) {
        feedState.transactions.monthSpendCategoryGroups(activeMonthKey).take(3)
    }
    val recentRows = remember(spendTransactions) {
        spendTransactions.sortedWith(
            compareByDescending<TransactionUi> { it.transactionDate.orEmpty() }
                .thenByDescending { it.inrAmountValue ?: 0.0 }
        ).take(4)
    }
    val sampleFallback = feedState.needsSmsPermission && feedState.label == "sample SMS"
    val importLabel = if (sampleFallback) "Imports need permission" else "Imports up to date"
    val moneyInCount = breakdown.creditCount
    val moneyInAmount = breakdown.totalCredits

    ProvideTextStyle(
        MaterialTheme.typography.bodyMedium.copy(
            fontFamily = SortedHomeDesignFontFamily,
            fontWeight = SortedHomeWeight
        )
    ) {
        Column(
            modifier = Modifier
                .then(modifier)
                .fillMaxSize()
                .background(palette.background)
        ) {
            HomeHeader(
                palette = palette,
                monthKey = activeMonthKey,
                importLabel = importLabel,
                months = months,
                onMonthSelected = onMonthSelected,
                onOpenSync = onOpenSync,
                onSettings = onSettings
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                content = {
                    if (sampleFallback || monthTransactions.isEmpty()) {
                        item {
                            ModernHomeEmptyState(
                                needsPermission = feedState.needsSmsPermission,
                                palette = palette,
                                onRequestSmsPermission = onRequestSmsPermission
                            )
                        }
                    } else {
                        item {
                            HomeHeroSpend(
                                amount = breakdown.spends,
                                paymentCount = breakdown.spendCount,
                                palette = palette,
                                onClick = onExplainSpend
                            )
                        }
                        if (reviewRows.isNotEmpty()) {
                            item {
                                HomeReviewStamp(
                                    count = reviewRows.size,
                                    palette = palette,
                                    onClick = onOpenReview
                                )
                            }
                        }
                        if (merchantGroups.isNotEmpty()) {
                            item {
                                HomeTopMerchants(
                                    merchants = merchantGroups,
                                    palette = palette,
                                    onMerchantClick = onMerchantClick
                                )
                            }
                        }
                        if (categoryGroups.isNotEmpty()) {
                            item {
                                HomeShareStrip(
                                    categories = categoryGroups,
                                    total = breakdown.spends,
                                    palette = palette,
                                    onCategoryClick = onCategoryClick
                                )
                            }
                        }
                        item {
                            HomeRecentSpending(
                                rows = recentRows,
                                paymentCount = breakdown.spendCount,
                                notCount = moneyInCount,
                                notAmount = moneyInAmount,
                                palette = palette,
                                onTransactionClick = onTransactionClick,
                                onOpenAll = onExplainSpend,
                                onOpenNotCounted = onExplainSpend
                            )
                        }
                        item { Spacer(modifier = Modifier.height(18.dp)) }
                    }
                }
            )
        }
    }
}

private data class HomePalette(
    val background: Color,
    val header: Color,
    val band: Color,
    val nav: Color,
    val ink: Color,
    val muted: Color,
    val softFill: Color,
    val rule: Color,
    val faintRule: Color,
    val review: Color,
    val reviewDot: Color,
    val categoryOne: Color,
    val categoryTwo: Color,
    val categoryThree: Color,
    val credit: Color
)

@Composable
private fun homePalette(): HomePalette {
    return if (isDarkModeActive()) {
        HomePalette(
            background = Color(0xFF0D2522),
            header = Color(0xFF123330),
            band = Color(0xFF123330),
            nav = Color(0xFF16403A),
            ink = Color(0xFFE7F0EC),
            muted = Color(0xFF93AAA4),
            softFill = Color(0x14E7F0EC),
            rule = Color(0x21E7F0EC),
            faintRule = Color(0x14E7F0EC),
            review = Color(0xFFD79A3F),
            reviewDot = Color(0xFFD79A3F),
            categoryOne = Color(0xFFE7F0EC),
            categoryTwo = Color(0xFF7FB3A4),
            categoryThree = Color(0xFFD79A3F),
            credit = Color(0xFF7FB3A4)
        )
    } else {
        HomePalette(
            background = Color(0xFFE8EEDD),
            header = Color(0xFFDFEACF),
            band = Color(0xFFDCE6CE),
            nav = Color(0xFFD3DFC1),
            ink = Color(0xFF17241E),
            muted = Color(0xFF566A5E),
            softFill = Color(0x214E8471),
            rule = Color(0x2117241E),
            faintRule = Color(0x1717241E),
            review = Color(0xFFA9522A),
            reviewDot = Color(0xFFC0642F),
            categoryOne = Color(0xFF17241E),
            categoryTwo = Color(0xFF4E8471),
            categoryThree = Color(0xFFA9522A),
            credit = Color(0xFF4E8471)
        )
    }
}

@Composable
private fun HomeHeader(
    palette: HomePalette,
    monthKey: String?,
    importLabel: String,
    months: List<String>,
    onMonthSelected: (String) -> Unit,
    onOpenSync: () -> Unit,
    onSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.header)
            .drawBehind {
                drawLine(
                    color = palette.rule,
                    start = Offset(0f, size.height - 1.dp.toPx()),
                    end = Offset(size.width, size.height - 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(top = 18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SortedTallyMark(
                ink = palette.ink,
                clay = palette.review,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = importLabel,
                modifier = Modifier.clickable(onClick = onOpenSync),
                color = palette.muted,
                fontSize = 12.sp,
                fontWeight = SortedHomeWeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = onSettings, modifier = Modifier.size(38.dp)) {
                HomeSettingsSlidersGlyph(color = palette.muted, modifier = Modifier.size(20.dp))
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = monthKey?.monthNameLabel() ?: "Month",
                modifier = Modifier.weight(1f),
                color = palette.ink,
                fontSize = 15.sp,
                fontWeight = SortedHomeWeight,
                maxLines = 1
            )
            Text(
                text = monthKey?.substringBefore("-") ?: LocalDate.now().year.toString(),
                color = palette.muted,
                fontSize = 11.5.sp,
                fontWeight = SortedHomeWeight
            )
        }
        HomeMonthRuler(
            selectedMonthKey = monthKey,
            availableMonths = months,
            palette = palette,
            onMonthSelected = onMonthSelected
        )
    }
}

@Composable
private fun HomeMonthRuler(
    selectedMonthKey: String?,
    availableMonths: List<String>,
    palette: HomePalette,
    onMonthSelected: (String) -> Unit
) {
    val year = selectedMonthKey?.substringBefore("-") ?: availableMonths.firstOrNull()?.substringBefore("-")
        ?: LocalDate.now().year.toString()
    val available = availableMonths.toSet()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 9.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        (1..12).forEach { month ->
            val key = "$year-${month.toString().padStart(2, '0')}"
            val selected = key == selectedMonthKey
            val enabled = key in available
            Box(
                modifier = Modifier
                    .weight(if (selected) 1.9f else 1f)
                    .height(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        when {
                            selected -> palette.ink
                            else -> palette.softFill
                        }
                    )
                    .alpha(if (enabled || selected) 1f else 0.4f)
                    .clickable(enabled = enabled) { onMonthSelected(key) },
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Text(
                        text = month.shortMonthLabel(),
                        color = palette.background,
                        fontSize = 11.5.sp,
                        fontWeight = SortedHomeWeight,
                        maxLines = 1
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = month.monthLetter(),
                            color = palette.muted,
                            fontSize = 10.5.sp,
                            fontWeight = SortedHomeWeight,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(palette.ink.copy(alpha = 0.22f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSettingsSlidersGlyph(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stroke = 1.6.dp.toPx()
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(
                color = color,
                start = Offset(size.width * x1, size.height * y1),
                end = Offset(size.width * x2, size.height * y2),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
        line(0.12f, 0.28f, 0.62f, 0.28f)
        line(0.84f, 0.28f, 0.95f, 0.28f)
        line(0.12f, 0.72f, 0.34f, 0.72f)
        line(0.56f, 0.72f, 0.95f, 0.72f)
        drawCircle(
            color = color,
            radius = size.minDimension * 0.11f,
            center = Offset(size.width * 0.73f, size.height * 0.28f),
            style = Stroke(width = stroke)
        )
        drawCircle(
            color = color,
            radius = size.minDimension * 0.11f,
            center = Offset(size.width * 0.45f, size.height * 0.72f),
            style = Stroke(width = stroke)
        )
    }
}

@Composable
private fun HomeHeroSpend(
    amount: Double,
    paymentCount: Int,
    palette: HomePalette,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 20.dp, top = 20.dp)
    ) {
        Text(
            text = "Spent this month",
            color = palette.muted,
            fontSize = 13.sp,
            fontWeight = SortedHomeWeight
        )
        Row(
            modifier = Modifier.padding(top = 5.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            HomeUltraHeavyAmount(
                text = amount.formatHomeRupee(),
                modifier = Modifier.weight(1f, fill = false),
                color = palette.ink
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "$paymentCount payments >",
                modifier = Modifier.padding(bottom = 9.dp),
                color = palette.muted,
                fontSize = 13.sp,
                fontWeight = SortedHomeWeight,
                maxLines = 1
            )
        }
        Text(
            text = "Includes transfers and investments",
            modifier = Modifier.padding(top = 3.dp),
            color = palette.muted,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun HomeUltraHeavyAmount(
    text: String,
    modifier: Modifier = Modifier,
    color: Color
) {
    Box(modifier = modifier) {
        val baseOffsets = listOf(
            0.dp to 0.dp,
            0.42.dp to 0.dp,
            (-0.42).dp to 0.dp,
            0.dp to 0.36.dp,
            0.dp to (-0.36).dp,
            0.30.dp to 0.28.dp,
            (-0.30).dp to 0.28.dp,
            0.30.dp to (-0.28).dp,
            (-0.30).dp to (-0.28).dp,
            0.58.dp to 0.dp,
            (-0.58).dp to 0.dp,
            0.dp to 0.52.dp,
            0.dp to (-0.52).dp
        )
        val offsets = baseOffsets.flatMap { (x, y) ->
            listOf(0.58f, 0.81f, 1.04f).map { scale ->
                (x * scale) to (y * scale)
            }
        }
        offsets.forEach { (x, y) ->
            Text(
                text = text,
                modifier = Modifier.offset(x = x, y = y),
                color = color,
                fontSize = 58.sp,
                fontWeight = SortedHomeWeight,
                lineHeight = 58.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = (-1.2).sp
            )
        }
    }
}

@Composable
private fun HomeReviewStamp(
    count: Int,
    palette: HomePalette,
    onClick: () -> Unit
) {
    Row(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp)) {
        Row(
            modifier = Modifier
                .graphicsLayer(rotationZ = -1.5f)
                .drawBehind {
                    drawRoundRect(
                        color = palette.review,
                        topLeft = Offset.Zero,
                        size = size,
                        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()),
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(5.dp.toPx(), 4.dp.toPx()),
                                0f
                            )
                        )
                    )
                }
                .clickable(onClick = onClick)
                .padding(horizontal = 13.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(palette.reviewDot)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$count need review".uppercase(Locale.US),
                color = palette.review,
                fontSize = 12.sp,
                fontWeight = SortedHomeWeight,
                letterSpacing = 1.1.sp
            )
        }
    }
}

private fun Modifier.dottedOutline(color: Color): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        topLeft = Offset.Zero,
        size = size,
        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()),
        style = Stroke(
            width = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(2.dp.toPx(), 2.dp.toPx()),
                0f
            )
        )
    )
}

@Composable
private fun HomeTopMerchants(
    merchants: List<SummaryGroup>,
    palette: HomePalette,
    onMerchantClick: (SummaryGroup) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp)
            .background(palette.band)
            .drawBehind {
                drawLine(palette.rule, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
                drawLine(palette.rule, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            }
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "TOP MERCHANTS",
                modifier = Modifier.weight(1f),
                color = palette.muted,
                fontSize = 11.sp,
                fontWeight = SortedHomeWeight,
                letterSpacing = 1.7.sp
            )
            Text(
                text = merchantTickLegend(merchantTickUnit(merchants.maxOf { it.count })),
                color = palette.muted,
                fontSize = 11.5.sp,
                fontWeight = SortedHomeWeight
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        val tickUnit = merchantTickUnit(merchants.maxOf { it.count })
        merchants.forEach { merchant ->
            HomeMerchantTickRow(
                merchant = merchant,
                tickUnit = tickUnit,
                palette = palette,
                onClick = { onMerchantClick(merchant) }
            )
        }
    }
}

@Composable
private fun HomeMerchantTickRow(
    merchant: SummaryGroup,
    tickUnit: Int,
    palette: HomePalette,
    onClick: () -> Unit
) {
    val tickCount = merchantTickCount(merchant.count, tickUnit)
    val paymentLabel = if (merchant.count == 1) "1 payment" else "${merchant.count} payments"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .drawBehind {
                drawLine(
                    color = palette.faintRule,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = merchant.label,
                modifier = Modifier.weight(1f),
                color = palette.ink,
                fontSize = 15.sp,
                fontWeight = SortedHomeWeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = merchant.total.formatHomeRupee(),
                color = palette.ink,
                fontSize = 15.sp,
                fontWeight = SortedHomeWeight,
                maxLines = 1
            )
        }
        Row(
            modifier = Modifier.padding(top = 7.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            repeat(tickCount) { index ->
                if (index > 0 && index % 5 == 0) Spacer(modifier = Modifier.width(5.dp))
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(if (index % 5 == 4) palette.categoryTwo else palette.ink)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = paymentLabel,
                color = palette.muted,
                fontSize = 11.5.sp,
                fontWeight = SortedHomeWeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeShareStrip(
    categories: List<SummaryGroup>,
    total: Double,
    palette: HomePalette,
    onCategoryClick: (SummaryGroup) -> Unit
) {
    val colors = listOf(palette.categoryOne, palette.categoryTwo, palette.categoryThree)
    val rest = (total - categories.sumOf { it.total }).coerceAtLeast(0.0)
    val restPct = ((rest / total.coerceAtLeast(1.0)) * 100).roundToInt()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "TOP SPENDING",
                modifier = Modifier.weight(1f),
                color = palette.muted,
                fontSize = 11.sp,
                fontWeight = SortedHomeWeight,
                letterSpacing = 1.7.sp
            )
            Text(
                text = "Share of the month",
                color = palette.muted,
                fontSize = 11.5.sp,
                fontWeight = SortedHomeWeight
            )
        }
        Row(
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .height(34.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            categories.forEachIndexed { index, category ->
                Box(
                    modifier = Modifier
                        .weight(category.total.coerceAtLeast(1.0).toFloat())
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(colors.getOrElse(index) { palette.muted })
                )
            }
            if (rest > 0.0) {
                Box(
                    modifier = Modifier
                        .weight(rest.toFloat())
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(palette.ink.copy(alpha = 0.14f))
                )
            }
        }
        Row(
            modifier = Modifier
                .padding(top = 6.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${total.formatHomeRupee()} spent",
                color = palette.muted,
                fontSize = 11.sp,
                fontWeight = SortedHomeWeight
            )
            Text(
                text = "Everything else $restPct%",
                color = palette.muted,
                fontSize = 11.sp,
                fontWeight = SortedHomeWeight
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        categories.forEachIndexed { index, category ->
            HomeCategoryShareRow(
                category = category,
                total = total,
                color = colors.getOrElse(index) { palette.muted },
                palette = palette,
                onClick = { onCategoryClick(category) }
            )
        }
    }
}

@Composable
private fun HomeCategoryShareRow(
    category: SummaryGroup,
    total: Double,
    color: Color,
    palette: HomePalette,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .drawBehind {
                drawLine(
                    color = palette.faintRule,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = category.label,
            modifier = Modifier.weight(1f),
            color = palette.ink,
            fontSize = 14.5.sp,
            fontWeight = SortedHomeWeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = category.total.formatHomeRupee(),
            color = palette.ink,
            fontSize = 14.5.sp,
            fontWeight = SortedHomeWeight,
            maxLines = 1
        )
        Text(
            text = "${((category.total / total.coerceAtLeast(1.0)) * 100).roundToInt()}%",
            modifier = Modifier.width(38.dp),
            color = palette.muted,
            fontSize = 12.5.sp,
            fontWeight = SortedHomeWeight,
            textAlign = TextAlign.End,
            maxLines = 1
        )
    }
}

@Composable
private fun HomeRecentSpending(
    rows: List<TransactionUi>,
    paymentCount: Int,
    notCount: Int,
    notAmount: Double,
    palette: HomePalette,
    onTransactionClick: (TransactionUi) -> Unit,
    onOpenAll: () -> Unit,
    onOpenNotCounted: () -> Unit
) {
    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp)) {
        Text(
            text = "RECENT SPENDING",
            color = palette.muted,
            fontSize = 11.sp,
            fontWeight = SortedHomeWeight,
            letterSpacing = 1.7.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        rows.forEach { transaction ->
            HomeRecentRow(
                transaction = transaction,
                palette = palette,
                onClick = { onTransactionClick(transaction) }
            )
        }
        Row(
            modifier = Modifier
                .clickable(onClick = onOpenAll)
                .padding(top = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "See all $paymentCount payments",
                color = palette.muted,
                fontSize = 12.5.sp,
                fontWeight = SortedHomeWeight
            )
            Text(text = " >", color = palette.muted, fontSize = 12.5.sp, fontWeight = SortedHomeWeight)
        }
        if (notCount > 0 || notAmount > 0.0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .drawBehind {
                        drawLine(palette.rule, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
                    }
                    .clickable(onClick = onOpenNotCounted)
                    .padding(vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Money in · $notCount payments",
                        color = palette.muted,
                        fontSize = 13.sp,
                        fontWeight = SortedHomeWeight
                    )
                    Text(
                        text = "Refunds, rewards and income",
                        color = palette.muted,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = notAmount.formatHomeRupee(),
                    color = palette.muted,
                    fontSize = 13.sp,
                    fontWeight = SortedHomeWeight,
                    maxLines = 1
                )
                Text(text = " >", color = palette.muted, fontSize = 13.sp, fontWeight = SortedHomeWeight)
            }
        }
        Text(
            text = "Stays on this phone",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 18.dp),
            color = palette.muted,
            fontSize = 11.5.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HomeRecentRow(
    transaction: TransactionUi,
    palette: HomePalette,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .drawBehind {
                drawLine(
                    color = palette.faintRule,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(palette.softFill),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = transaction.merchant.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = palette.muted,
                fontSize = 13.5.sp,
                fontWeight = SortedHomeWeight
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.merchant,
                color = palette.ink,
                fontSize = 15.5.sp,
                fontWeight = SortedHomeWeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOf(transaction.category, transaction.transactionDate.recentDateLabel())
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                color = palette.muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (transaction.needsReview()) {
            Text(
                text = "REVIEW",
                modifier = Modifier
                    .graphicsLayer(rotationZ = -2f)
                    .dottedOutline(palette.review)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                color = palette.review,
                fontSize = 10.sp,
                fontWeight = SortedHomeWeight,
                letterSpacing = 0.9.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = transaction.displayAmount(),
            color = palette.ink,
            fontSize = 15.5.sp,
            fontWeight = SortedHomeWeight,
            maxLines = 1
        )
    }
}

@Composable
private fun ModernHomeEmptyState(
    needsPermission: Boolean,
    palette: HomePalette,
    onRequestSmsPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Spent this month",
            color = palette.muted,
            fontSize = 13.sp,
            fontWeight = SortedHomeWeight
        )
        HomeUltraHeavyAmount(
            text = "₹0",
            modifier = Modifier.padding(top = 6.dp),
            color = palette.ink
        )
        Text(
            text = "No payments found yet",
            color = palette.muted,
            fontSize = 13.sp,
            fontWeight = SortedHomeWeight
        )
        if (needsPermission) {
            Text(
                text = "Allow SMS access",
                modifier = Modifier
                    .padding(top = 22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(palette.ink)
                    .clickable(onClick = onRequestSmsPermission)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                color = palette.background,
                fontSize = 13.sp,
                fontWeight = SortedHomeWeight
            )
        }
        Text(
            text = "Stays on this phone",
            modifier = Modifier.padding(top = 18.dp),
            color = palette.muted,
            fontSize = 11.5.sp
        )
    }
}

private fun merchantTickUnit(maxCount: Int): Int {
    return when {
        maxCount <= 16 -> 1
        maxCount <= 40 -> 2
        maxCount <= 80 -> 5
        maxCount <= 160 -> 10
        else -> 25
    }
}

private fun merchantTickCount(count: Int, unit: Int): Int {
    if (count <= 0) return 0
    return (count.toDouble() / unit).roundToInt().coerceIn(1, count)
}

private fun merchantTickLegend(unit: Int): String {
    return if (unit == 1) "One tick per payment" else "1 tick = $unit payments"
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
            text = "SPENT THIS MONTH",
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
            label = "${breakdown.spendCount} PAYMENTS",
            value = breakdown.spends.formatRupee(),
            palette = palette
        )
        TapeSummationRow(
            label = "ALL OUTGOING PAYMENTS",
            value = breakdown.totalDebits.formatRupee(),
            palette = palette
        )
        TapeSummationRow(
            label = "${reviewRows.size} NEED REVIEW",
            value = reviewAmount.formatRupee(),
            palette = palette,
            color = palette.query
        )
        if (breakdown.refunds > 0.0) {
            TapeSummationRow(
                label = "REFUND SIGNALS",
                value = "SHOWN SEPARATELY",
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
            .dottedOutline(palette.query)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TapeStamp(text = "?", palette = palette, color = palette.query)
        Spacer(modifier = Modifier.width(9.dp))
        Text(
            text = "$count PAYMENTS NEED REVIEW - ${amount.formatRupee()}",
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
    val amountLabel = inrAmountValue?.formatRupee() ?: amount
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
            note = "money in - shown separately",
            accent = palette.credit,
            ink = palette.inkSoft,
            filledStamp = false
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
            text = "${breakdown.spendCount} PAYMENTS",
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
                    .dottedOutline(palette.query)
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
            text = "NO PAYMENTS YET",
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

@Composable
private fun TapeBackDeskBar(
    title: String,
    meta: String,
    palette: TapePalette,
    onBack: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.desk)
            .padding(start = 12.dp, end = 10.dp, top = 13.dp, bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            Text(
                text = "<",
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onBack)
                    .padding(top = 4.dp),
                color = palette.amber,
                fontFamily = SortedTapeFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(7.dp))
        }
        Text(
            text = title.uppercase(Locale.US),
            color = palette.inkSoft,
            fontFamily = SortedTapeFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 3.sp,
            maxLines = 1
        )
        Text(
            text = " - ${meta.uppercase(Locale.US)}",
            modifier = Modifier.weight(1f),
            color = palette.inkFaint,
            fontFamily = SortedTapeFontFamily,
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (onSettings != null) {
            IconButton(onClick = onSettings, modifier = Modifier.size(36.dp)) {
                SettingsGlyph(
                    color = palette.inkFaint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TapeRoute(
    title: String,
    meta: String,
    onBack: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null,
    content: androidx.compose.foundation.lazy.LazyListScope.(TapePalette) -> Unit
) {
    val palette = tapePalette()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.desk)
    ) {
        TapeBackDeskBar(
            title = title,
            meta = meta,
            palette = palette,
            onBack = onBack,
            onSettings = onSettings
        )
        TapePaper(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 14.dp),
            palette = palette
        ) {
            content(palette)
            item { Spacer(modifier = Modifier.height(92.dp)) }
        }
    }
}

@Composable
private fun TapeLedgerBlock(
    heading: String,
    meta: String,
    palette: TapePalette,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(1.dp)
                    .background(palette.rule)
            )
            Text(
                text = " ${heading.uppercase(Locale.US)}",
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
                text = meta.uppercase(Locale.US),
                color = palette.inkFaint,
                fontFamily = SortedTapeFontFamily,
                fontSize = 8.sp,
                letterSpacing = 1.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        content()
    }
}

@Composable
private fun TapeActionText(
    label: String,
    palette: TapePalette,
    color: Color = palette.amber,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Text(
        text = label.uppercase(Locale.US),
        modifier = modifier
            .border(1.dp, if (enabled) color else palette.rule, RoundedCornerShape(2.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        color = if (enabled) color else palette.inkFaint,
        fontFamily = SortedTapeFontFamily,
        fontSize = 9.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun TapeFilterRail(
    title: String,
    choices: List<String>,
    selected: String,
    palette: TapePalette,
    onSelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(Locale.US),
            color = palette.inkFaint,
            fontFamily = SortedTapeFontFamily,
            fontSize = 8.sp,
            letterSpacing = 1.5.sp,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(7.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(choices) { choice ->
                AddStamp(
                    text = choice.uppercase(Locale.US),
                    selected = choice == selected,
                    palette = palette,
                    color = palette.amber,
                    onClick = { onSelected(choice) }
                )
            }
        }
    }
}

@Composable
private fun TapeInlineLine(
    transaction: TransactionUi,
    palette: TapePalette,
    reason: String? = null,
    onClick: () -> Unit
) {
    val value = transaction.inrAmountValue ?: transaction.amountValue
    val style = transaction.tapeLineStyle(palette)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .drawBehind {
                val y = size.height - 1.dp.toPx()
                drawLine(
                    color = palette.ruleFaint,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = style.glyph,
                modifier = Modifier.width(14.dp),
                color = style.accent,
                fontFamily = SortedTapeFontFamily,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = transaction.merchant.uppercase(Locale.US),
                modifier = Modifier.weight(1f),
                color = style.ink,
                fontFamily = SortedTapeFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (transaction.direction == DirectionUi.Credit) "+${value.formatRupee()}" else value.formatRupee(),
                color = style.ink,
                fontFamily = SortedTapeFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                textDecoration = style.textDecoration
            )
        }
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TapeStamp(style.stamp, palette, style.accent, style.filledStamp)
            Spacer(modifier = Modifier.width(6.dp))
            TapeStamp(transaction.source.uppercase(Locale.US), palette, palette.inkFaint)
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = (reason ?: style.note).uppercase(Locale.US),
                modifier = Modifier.weight(1f),
                color = if (reason != null) palette.query else palette.inkFaint,
                fontFamily = SortedTapeFontFamily,
                fontSize = 8.sp,
                letterSpacing = 0.7.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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
@OptIn(ExperimentalMaterial3Api::class)
private fun InsightsTabContent(
    feedState: FeedState,
    selectedMonthKey: String?,
    modifier: Modifier,
    onSettings: () -> Unit,
    onMerchantClick: (SummaryGroup, List<String>) -> Unit,
    onCategoryClick: (SummaryGroup, List<String>) -> Unit,
    onTransactionClick: (TransactionUi) -> Unit,
    onOpenReview: () -> Unit
) {
    val palette = homePalette()
    val allRows = feedState.transactions.filter { it.inrAmountValue != null }
    val today = remember { LocalDate.now() }
    val thisMonth = today.toString().take(7)
    val months = remember(today) { (1..12).map { month -> "%04d-%02d".format(today.year, month) } }
    var range by remember { mutableStateOf("This month") }
    val selectedKey = when (range) {
        "Last month" -> today.minusMonths(1).toString().take(7)
        else -> thisMonth
    }
    val keys = if (range == "This year") months.filter { it <= thisMonth } else listOf(selectedKey)
    val spend = allRows.filter { it.transactionDate?.take(7) in keys && it.countsTowardSpentTotal() }
    val periodTotal = spend.sumOf { it.inrAmountValue ?: 0.0 }
    val comparisonKey = if (range == "Last month") today.minusMonths(2).toString().take(7) else today.minusMonths(1).toString().take(7)
    val comparisonRows = allRows.filter { it.transactionDate?.take(7) == comparisonKey && it.countsTowardSpentTotal() }
    val comparisonTotal = comparisonRows.sumOf { it.inrAmountValue ?: 0.0 }
    val average = if (range == "This year") periodTotal / keys.size.coerceAtLeast(1) else allRows.filter { it.countsTowardSpentTotal() }.groupBy { it.transactionDate?.take(7) }.values.map { rows -> rows.sumOf { it.inrAmountValue ?: 0.0 } }.average().takeIf { it.isFinite() } ?: 0.0
    val groups = spend.groupBy { it.category.ifBlank { "Other" } }.map { (label, rows) -> SummaryGroup(label, rows.size, rows.sumOf { it.inrAmountValue ?: 0.0 }, "INR", label) }.sortedByDescending { it.total }
    val merchants = spend.groupBy { it.merchant.ifBlank { "Unknown" } }.map { (label, rows) -> SummaryGroup(label, rows.size, rows.sumOf { it.inrAmountValue ?: 0.0 }, "INR", rows.first().category) }.sortedByDescending { it.total }
    val moneyIn = allRows.filter { it.transactionDate?.take(7) in keys && it.countsTowardMoneyIn() }
    var sheetTitle by remember { mutableStateOf<String?>(null) }
    var sheetRows by remember { mutableStateOf(emptyList<TransactionUi>()) }
    val openRows: (String, List<TransactionUi>) -> Unit = { title, rows -> sheetTitle = title; sheetRows = rows }

    Column(modifier = modifier.fillMaxSize().background(palette.background)) {
        Row(Modifier.fillMaxWidth().background(if (isDarkModeActive()) Color(0xFF0F2C28) else palette.header).padding(start = 20.dp, end = 8.dp, top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("INSIGHTS", Modifier.weight(1f), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.53.sp)
            Text(if (feedState.needsSmsPermission) "Imports need permission" else "${allRows.size} payments", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            IconButton(onClick = onSettings) { HomeSettingsSlidersGlyph(color = palette.muted, modifier = Modifier.size(20.dp)) }
        }
        Row(Modifier.fillMaxWidth().background(if (isDarkModeActive()) Color(0xFF0F2C28) else palette.header).padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            listOf("This month", "Last month", "This year").forEach { label ->
                val active = range == label
                Box(Modifier.weight(1f).background(if (active) palette.ink else palette.softFill, RoundedCornerShape(6.dp)).clickable { range = label }.padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                    Text(label, color = if (active) palette.background else palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 12.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium)
                }
            }
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            item {
                Column(Modifier.fillMaxWidth().clickable { openRows("${range} payments", spend) }.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 18.dp)) {
                    Text(if (range == "This year") "Spent this year" else "Spent in ${selectedKey.monthNameLabel()}", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(periodTotal.formatHomeRupee(), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 54.sp, lineHeight = 54.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
                    Text("${spend.size} payments", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    val delta = periodTotal - comparisonTotal
                    Text(if (comparisonTotal == 0.0) "No previous month to compare" else "${kotlin.math.abs(delta).formatHomeRupee()} ${if (delta >= 0) "more" else "less"} than ${comparisonKey.monthNameLabel()}", Modifier.padding(top = 10.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                }
            }
            item {
                Column(Modifier.fillMaxWidth().background(palette.band).padding(horizontal = 20.dp, vertical = 18.dp)) {
                    InsightsSectionTitle("Month by month", "${today.year}", palette)
                    Row(Modifier.fillMaxWidth().height(106.dp).padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
                        months.forEachIndexed { index, key ->
                            val sum = allRows.filter { it.transactionDate?.take(7) == key && it.countsTowardSpentTotal() }.sumOf { it.inrAmountValue ?: 0.0 }
                            val max = months.map { m -> allRows.filter { it.transactionDate?.take(7) == m && it.countsTowardSpentTotal() }.sumOf { it.inrAmountValue ?: 0.0 } }.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
                            val future = key > thisMonth
                            Column(Modifier.weight(1f).fillMaxHeight().clickable(enabled = !future) { openRows(key.monthNameLabel(), allRows.filter { it.transactionDate?.take(7) == key && it.countsTowardSpentTotal() }) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                                Spacer(Modifier.fillMaxWidth().height((70 * (sum / max).coerceAtLeast(if (sum > 0) .08 else 0.015)).dp).background(if (future) palette.faintRule else if (range == "This year" && !future) palette.categoryTwo else if (key == selectedKey) palette.ink else palette.categoryTwo.copy(alpha = .42f), RoundedCornerShape(2.dp)))
                                Text(key.substring(5).toInt().let { listOf("J","F","M","A","M","J","J","A","S","O","N","D")[it-1] }, Modifier.padding(top = 5.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 9.5.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Usual month", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 12.sp)
                        Text(average.formatHomeRupee(), Modifier.clickable { openRows("Payments in a usual month", allRows.filter { it.countsTowardSpentTotal() }) }, color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
                    InsightsSectionTitle("Where it went", if (range == "This year") "A usual month" else "vs ${comparisonKey.monthNameLabel()}", palette)
                    Row(Modifier.fillMaxWidth().height(30.dp).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        groups.take(5).forEachIndexed { index, group -> Box(Modifier.weight(group.total.toFloat().coerceAtLeast(.01f)).fillMaxHeight().background(insightsCategoryColor(index, palette), RoundedCornerShape(3.dp)).clickable { onCategoryClick(group, keys) }) }
                        if (groups.isEmpty()) Spacer(Modifier.weight(1f))
                    }
                    groups.take(5).forEachIndexed { index, group ->
                        val previous = comparisonRows.filter { it.category == group.label }.sumOf { it.inrAmountValue ?: 0.0 }
                        val change = if (previous <= 0) "New" else "${if (group.total >= previous) "+" else "−"}${((kotlin.math.abs(group.total - previous) / previous) * 100).toInt()}%"
                        Row(Modifier.fillMaxWidth().clickable { onCategoryClick(group, keys) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(9.dp).background(insightsCategoryColor(index, palette), RoundedCornerShape(2.dp)))
                            Column(Modifier.weight(1f).padding(start = 10.dp)) { Text(group.label, color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 14.5.sp, fontWeight = FontWeight.Medium); Text("${group.count} payments", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.5.sp) }
                            Column(horizontalAlignment = Alignment.End) { Text(group.total.formatHomeRupee(), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold); Text(change, color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.5.sp, fontWeight = FontWeight.Medium) }
                        }
                        if (index < groups.take(5).lastIndex) Spacer(Modifier.fillMaxWidth().height(1.dp).background(palette.faintRule))
                    }
                }
            }
            item {
                Column(Modifier.fillMaxWidth().background(palette.band).padding(horizontal = 20.dp, vertical = 18.dp)) {
                    InsightsSectionTitle("Biggest changes", "vs ${comparisonKey.monthNameLabel()}", palette)
                    (groups.take(3)).forEach { group ->
                        val old = comparisonRows.filter { it.category == group.label }.sumOf { it.inrAmountValue ?: 0.0 }
                        InsightsTextRow("${group.label} ${if (old == 0.0) "is new" else "${(group.total - old).formatHomeRupee()} ${if (group.total >= old) "more" else "less"}"}", "${group.count} payments", palette) { onCategoryClick(group, keys) }
                    }
                }
            }
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
                    InsightsSectionTitle("Top merchants", "${merchants.size} places", palette)
                    merchants.take(5).forEach { group ->
                        InsightsTextRow(group.label, "${group.count} payments  ·  ${group.total.formatHomeRupee()}", palette) { onMerchantClick(group, keys) }
                    }
                }
            }
            item {
                Column(Modifier.fillMaxWidth().background(palette.band).padding(horizontal = 20.dp, vertical = 18.dp)) {
                    InsightsSectionTitle("Biggest payments", "${spend.size} payments", palette)
                    spend.sortedByDescending { it.inrAmountValue ?: 0.0 }.take(5).forEach { row -> InsightsTransactionRow(row, palette, onTransactionClick) }
                }
            }
            item {
                val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                val dayTotals = dayNames.mapIndexed { i, _ -> spend.filter { it.transactionDate?.toLocalDateOrNull()?.dayOfWeek?.value == i + 1 }.sumOf { it.inrAmountValue ?: 0.0 } }
                val dayMax = dayTotals.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
                    InsightsSectionTitle("When you spend", "${range}", palette)
                    Row(Modifier.fillMaxWidth().height(110.dp).padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                        dayNames.forEachIndexed { i, day ->
                            Column(Modifier.weight(1f).fillMaxHeight().clickable { openRows("$day payments", spend.filter { it.transactionDate?.toLocalDateOrNull()?.dayOfWeek?.value == i + 1 }) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                                Spacer(Modifier.fillMaxWidth().height((72 * (dayTotals[i] / dayMax).coerceAtLeast(.025)).dp).background(if (dayTotals[i] == dayMax && dayTotals[i] > 0) palette.ink else palette.categoryTwo.copy(alpha = .55f), RoundedCornerShape(2.dp)))
                                Text(day, Modifier.padding(top = 5.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 10.sp)
                            }
                        }
                    }
                    val busiestDay = dayTotals.indices.maxByOrNull { dayTotals[it] } ?: 0
                    Text(if (dayTotals[busiestDay] == 0.0) "No payments in this period" else "Busiest day: ${dayNames[busiestDay]}", Modifier.padding(top = 12.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 12.sp)
                }
            }
            item {
                Column(Modifier.fillMaxWidth().background(palette.band).padding(horizontal = 20.dp, vertical = 18.dp)) {
                    InsightsSectionTitle("Money in", "${moneyIn.size} payments", palette)
                    listOf(TransactionType.REFUND to "Refunds", TransactionType.REWARD to "Rewards", TransactionType.INCOME to "Income").forEach { (type, label) ->
                        val rows = moneyIn.filter { it.transactionType == type }
                        if (rows.isNotEmpty()) InsightsTextRow(label, "${rows.size} payments  ·  ${rows.sumOf { it.inrAmountValue ?: 0.0 }.formatHomeRupee()}", palette, amountColor = if (type == TransactionType.INCOME || type == TransactionType.REFUND || type == TransactionType.REWARD) palette.credit else palette.ink) { openRows(label, rows) }
                    }
                    val otherMoneyIn = moneyIn.filter { it.transactionType !in setOf(TransactionType.REFUND, TransactionType.REWARD, TransactionType.INCOME) }
                    if (otherMoneyIn.isNotEmpty()) InsightsTextRow("Other", "${otherMoneyIn.size} payments  ·  ${otherMoneyIn.sumOf { it.inrAmountValue ?: 0.0 }.formatHomeRupee()}", palette, amountColor = palette.credit) { openRows("Other money in", otherMoneyIn) }
                }
            }
            item { Text("Worked out on this phone", Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.5.sp, textAlign = TextAlign.Center) }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
    if (sheetTitle != null) {
        ModalBottomSheet(onDismissRequest = { sheetTitle = null }, containerColor = palette.background, shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Text(sheetTitle.orEmpty(), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp)) { items(sheetRows) { row -> InsightsTransactionRow(row, palette, onTransactionClick) } }
            }
        }
    }
}

@Composable
private fun InsightsSectionTitle(title: String, meta: String, palette: HomePalette) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(Locale.US), Modifier.weight(1f), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.76.sp)
        Text(meta, color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun InsightsTextRow(title: String, detail: String, palette: HomePalette, amountColor: Color = palette.ink, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 14.5.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail.substringAfterLast("  ·  ", detail), color = amountColor, fontFamily = SortedHomeFontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        if (detail.contains("  ·  ").not()) Text(detail, color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.5.sp)
        Spacer(Modifier.fillMaxWidth().padding(top = 9.dp).height(1.dp).background(palette.faintRule))
    }
}

@Composable
private fun InsightsTransactionRow(transaction: TransactionUi, palette: HomePalette, onClick: (TransactionUi) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick(transaction) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(transaction.merchant.ifBlank { "Payment" }, color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 15.5.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${transaction.category}  ·  ${transaction.transactionDate.orEmpty()}", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text((transaction.inrAmountValue ?: 0.0).formatHomeRupee(), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun insightsCategoryColor(index: Int, palette: HomePalette): Color = when (index % 5) {
    0 -> palette.categoryOne
    1 -> palette.categoryTwo
    2 -> palette.categoryThree
    3 -> if (isDarkModeActive()) Color(0xFFA8CFC2) else Color(0xFF6F9A86)
    else -> if (isDarkModeActive()) Color(0xFFC2B27A) else Color(0xFF8C7A4A)
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
            text = "${monthKey?.monthStampLabel() ?: "CURRENT"} - $indexedCount PAYMENTS",
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
    val reviewAmount = reviewRows.sumOf { it.inrAmountValue ?: 0.0 }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp)
            .border(1.dp, palette.rule)
            .padding(start = 13.dp, end = 13.dp, top = 12.dp, bottom = 2.dp)
    ) {
        Text(
            text = "THIS MONTH",
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
        TapeSummationRow(" $indexedCount PAYMENTS", breakdown.spends.formatRupee(), palette)
        TapeSummationRow(" $merchantCount MERCHANTS - $categoryCount CATEGORIES", "", palette)
        TapeSummationRow(" ALL OUTGOING PAYMENTS", breakdown.totalDebits.formatRupee(), palette)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenReview)
                .padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = " ${reviewRows.size} NEED REVIEW",
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
            IndexBlockFoot("CATEGORY TOTAL", groups.sumOf { it.total }.formatRupee(), palette)
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
            IndexEmptyLine("NOT ENOUGH PAYMENTS TO SPOT A PATTERN YET", palette)
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
            meta = "${breakdown.spendCount} PAYMENTS",
            value = breakdown.spends.formatRupee(),
            palette = palette
        )
        IndexEntryRow(
            name = previousMonthKey?.monthStampLabel() ?: "PREVIOUS",
            meta = "${previousBreakdown.spendCount} PAYMENTS",
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
                meta = if (old == 0.0) "FIRST SEEN" else "${group.count} PAYMENTS",
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
        heading = "OUTGOING CATEGORIES",
        meta = "${breakdown.debitCount} PAYMENTS IN TOTAL",
        palette = palette
    ) {
        val otherOutgoing = (breakdown.totalDebits - breakdown.transfers - breakdown.investments).coerceAtLeast(0.0)
        val rows = listOf(
            Triple("EVERYDAY", "PURCHASES AND BILLS", otherOutgoing),
            Triple("INVESTED", "INVESTMENTS", breakdown.investments),
            Triple("MOVED", "TRANSFERS", breakdown.transfers)
        ).filter { it.third > 0.0 }
        if (rows.isEmpty()) {
            IndexEmptyLine("NO OUTGOING PAYMENTS THIS MONTH", palette)
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
            IndexBlockFoot("ALL INCLUDED", breakdown.totalDebits.formatRupee(), palette)
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
                            text = "POSSIBLE REFUND - SHOWN SEPARATELY",
                            color = palette.inkFaint,
                            fontFamily = SortedTapeFontFamily,
                            fontSize = 8.sp,
                            letterSpacing = 1.sp,
                            maxLines = 1
                        )
                    }
                }
            }
            IndexBlockFoot("REFUNDS SHOWN SEPARATELY", refunds.sumOf { it.inrAmountValue ?: 0.0 }.formatRupee(), palette)
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
        heading = "NEED REVIEW",
        meta = "${reviewRows.size} PAYMENTS",
        palette = palette
    ) {
        if (reviewRows.isEmpty()) {
            IndexEmptyLine("NOTHING NEEDS REVIEW", palette)
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
        heading = "IMPORTS",
        meta = "ON DEVICE",
        palette = palette
    ) {
        if (rows.isEmpty()) {
            IndexEmptyLine("NO PAYMENTS IMPORTED YET", palette)
        } else {
            rows.forEach { row ->
                IndexEntryRow(
                    name = row.source.uppercase(Locale.US),
                    meta = "${row.totalCount} PAYMENTS - ${row.spendCount} IN TOTAL",
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
            text = "  ${breakdown.spendCount} PAYMENTS",
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
                .dottedOutline(palette.query)
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
@OptIn(ExperimentalMaterial3Api::class)
private fun SpendExplanationScreen(
    feedState: FeedState,
    selectedMonthKey: String?,
    onBack: () -> Unit,
    onTransactionClick: (TransactionUi) -> Unit,
    onOpenReview: () -> Unit
) {
    val palette = homePalette()
    val monthRows = remember(feedState.transactions, selectedMonthKey) {
        feedState.transactions.latestMonthTransactions(selectedMonthKey)
    }
    val outgoing = remember(monthRows) { monthRows.filter { it.countsTowardSpentTotal() } }
    val moneyIn = remember(monthRows) { monthRows.filter { it.countsTowardMoneyIn() } }
    val unresolvedDebits = remember(monthRows) {
        monthRows.filter {
            it.direction == DirectionUi.Debit && it.status != TransactionStatus.COMPLETED ||
                it.direction == DirectionUi.Debit && it.amountValue > 0.0 && it.inrAmountValue == null
        }
    }
    val total = outgoing.sumOf { it.inrAmountValue ?: 0.0 }
    val categories = remember(outgoing) {
        outgoing.groupBy { it.category.ifBlank { "Other" } }
            .map { (category, rows) -> SummaryGroup(category, rows.size, rows.sumOf { it.inrAmountValue ?: 0.0 }, "INR", category) }
            .sortedByDescending { it.total }
    }
    val moneyInGroups = remember(moneyIn) {
        moneyIn.groupBy { it.transactionType.displayName() }
            .map { (label, rows) -> SummaryGroup(label, rows.size, rows.sumOf { it.inrAmountValue ?: 0.0 }, "INR", label) }
            .sortedByDescending { it.total }
    }
    val reviewTransactions = remember(outgoing) { outgoing.filter(TransactionUi::needsReview) }
    var sheetTitle by remember { mutableStateOf<String?>(null) }
    var sheetRows by remember { mutableStateOf(emptyList<TransactionUi>()) }
    val openRows: (String, List<TransactionUi>) -> Unit = { title, rows -> sheetTitle = title; sheetRows = rows }

    Column(Modifier.fillMaxSize().background(palette.background)) {
        Row(
            Modifier.fillMaxWidth().background(palette.header).padding(start = 8.dp, end = 20.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.ink)
            }
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text("This month's spending", color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(selectedMonthKey?.monthNameLabel() ?: "This month", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 12.sp)
            }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column(
                    Modifier.fillMaxWidth().clickable { openRows("Outgoing payments", outgoing) }
                        .padding(horizontal = 20.dp, vertical = 22.dp)
                ) {
                    Text("Spent this month", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(total.formatHomeRupee(), Modifier.padding(top = 3.dp), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 48.sp, lineHeight = 50.sp, fontWeight = FontWeight.SemiBold)
                    Text("${outgoing.size} payments", Modifier.padding(top = 2.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("Includes transfers and investments.", Modifier.padding(top = 10.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 12.5.sp)
                }
            }
            item {
                Column(Modifier.fillMaxWidth().background(palette.band).padding(horizontal = 20.dp, vertical = 16.dp)) {
                    InsightsSectionTitle("By category", "${categories.size} categories", palette)
                    categories.forEach { group ->
                        val share = if (total > 0.0) (group.total / total).toFloat().coerceIn(0f, 1f) else 0f
                        Column(Modifier.fillMaxWidth().clickable { openRows(group.label, outgoing.filter { it.category.ifBlank { "Other" } == group.label }) }.padding(vertical = 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(group.label, Modifier.weight(1f), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(group.total.formatHomeRupee(), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(palette.softFill)) {
                                    Box(Modifier.fillMaxWidth(share).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(palette.categoryTwo))
                                }
                                Text("${(share * 100).roundToInt()}% · ${group.count}", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    if (categories.isEmpty()) Text("No outgoing payments this month.", Modifier.padding(top = 12.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 13.sp)
                }
            }
            if (moneyIn.isNotEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
                        InsightsSectionTitle("Money in", "Shown separately", palette)
                        Text("Refunds, rewards and income do not reduce spending.", Modifier.padding(top = 4.dp, bottom = 8.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 12.5.sp)
                        moneyInGroups.forEach { group ->
                            InsightsTextRow(group.label, "${group.count} payments · ${group.total.formatHomeRupee()}", palette, amountColor = palette.credit) {
                                openRows(group.label, moneyIn.filter { it.transactionType.displayName() == group.label })
                            }
                        }
                    }
                }
            }
            if (unresolvedDebits.isNotEmpty()) {
                item {
                    Row(Modifier.fillMaxWidth().background(palette.band).clickable { openRows("Not in total yet", unresolvedDebits) }.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Not in the total yet", color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("${unresolvedDebits.size} payments need an amount, currency or status check.", Modifier.padding(top = 2.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.5.sp)
                        }
                        Text("›", color = palette.muted, fontSize = 18.sp)
                    }
                }
            }
            if (reviewTransactions.isNotEmpty()) {
                item {
                    TextButton(onClick = onOpenReview, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text("Review ${reviewTransactions.size} payments", color = palette.review, fontFamily = SortedHomeFontFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            item { Text("Stays on this phone", Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.5.sp, textAlign = TextAlign.Center) }
        }
    }
    if (sheetTitle != null) {
        ModalBottomSheet(onDismissRequest = { sheetTitle = null }, containerColor = palette.background, shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Text(sheetTitle.orEmpty(), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 540.dp)) {
                    items(sheetRows, key = { it.sourceHash }) { row -> InsightsTransactionRow(row, palette, onTransactionClick) }
                }
            }
        }
    }
}

private data class ReviewCorrectionChoice(
    val transaction: TransactionUi,
    val category: String,
    val transactionType: TransactionType
)

private val ReviewCategories = listOf(
    "Food",
    "Shopping",
    "Bills",
    "Travel",
    "Health",
    "Home",
    "Fun",
    "Gifts",
    "Investment",
    "Transfer"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortInboxScreen(
    feedState: FeedState,
    selectedMonthKey: String?,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onTransactionClick: (TransactionUi) -> Unit,
    onCorrect: (TransactionUi, String, TransactionType, Boolean) -> Unit,
    onOpenSync: () -> Unit,
    onOpenRules: () -> Unit
) {
    val palette = homePalette()
    val persistedCandidates = remember(feedState.transactions, selectedMonthKey) {
        feedState.transactions.reviewCandidates(selectedMonthKey)
    }
    val initialReviewIds = remember { persistedCandidates.map(TransactionUi::sourceHash).toSet() }
    var handledHashes by remember { mutableStateOf(emptySet<String>()) }
    var currentIndex by remember { mutableStateOf(0) }
    var categoriesOpen by remember { mutableStateOf(false) }
    var pendingCorrection by remember { mutableStateOf<ReviewCorrectionChoice?>(null) }
    var madeRules by remember { mutableStateOf(0) }

    val candidates = remember(persistedCandidates, handledHashes) {
        persistedCandidates.filterNot { it.sourceHash in handledHashes }
    }
    val persistedHashes = persistedCandidates.map(TransactionUi::sourceHash).toSet()
    LaunchedEffect(persistedHashes) {
        handledHashes = handledHashes.filterTo(mutableSetOf()) { it in persistedHashes }
    }
    LaunchedEffect(candidates.size) {
        currentIndex = if (candidates.isEmpty()) 0 else currentIndex.coerceAtMost(candidates.lastIndex)
    }

    val monthKey = selectedMonthKey ?: feedState.transactions.selectedMonthKey()
    val monthSpend = feedState.transactions.latestMonthSpendTransactions(monthKey)
        .sumOf { it.inrAmountValue ?: 0.0 }
    val initialMonthSpend = remember { monthSpend }
    val resolvedCount = initialReviewIds.count { sourceHash ->
        candidates.none { it.sourceHash == sourceHash }
    }
    val progressTotal = maxOf(initialReviewIds.size, resolvedCount + candidates.size)
    val current = candidates.getOrNull(currentIndex)

    fun submitCorrection(choice: ReviewCorrectionChoice, rememberRule: Boolean) {
        onCorrect(choice.transaction, choice.category, choice.transactionType, rememberRule)
        handledHashes = handledHashes + choice.transaction.sourceHash
        if (rememberRule) madeRules += 1
        pendingCorrection = null
        categoriesOpen = false
    }

    fun offerCorrection(choice: ReviewCorrectionChoice) {
        if (choice.transaction.canCreateReviewRule()) {
            pendingCorrection = choice
        } else {
            submitCorrection(choice, rememberRule = false)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        ReviewProgressHeader(
            resolvedCount = resolvedCount,
            totalCount = progressTotal,
            hasCurrent = current != null,
            palette = palette,
            onBack = onBack
        )

        if (feedState.needsSmsPermission) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.review.copy(alpha = 0.10f))
                    .clickable(onClick = onOpenSync)
                    .padding(horizontal = 20.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(palette.reviewDot)
                )
                Text(
                    text = "SMS imports paused",
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp),
                    color = palette.review,
                    fontFamily = SortedHomeFontFamily,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Fix",
                    color = palette.review,
                    fontFamily = SortedHomeFontFamily,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (current == null) {
            ReviewFinishedState(
                reviewedCount = resolvedCount,
                monthLabel = monthKey?.monthNameLabel() ?: "This month",
                monthSpend = monthSpend,
                change = monthSpend - initialMonthSpend,
                madeRules = madeRules,
                palette = palette,
                onBackHome = onBack,
                onOpenRules = onOpenRules
            )
        } else {
            ReviewCurrentPayment(
                transaction = current,
                monthSpend = monthSpend,
                palette = palette,
                onChooseCategory = { category ->
                    offerCorrection(
                        ReviewCorrectionChoice(
                            transaction = current,
                            category = category,
                            transactionType = current.reviewTypeForCategory(category)
                        )
                    )
                },
                onSuggestedType = {
                    val type = current.reviewSuggestedOutgoingType()
                    offerCorrection(
                        ReviewCorrectionChoice(
                            transaction = current,
                            category = if (type == TransactionType.INVESTMENT) "Investment" else "Transfer",
                            transactionType = type
                        )
                    )
                },
                onKeep = {
                    submitCorrection(
                        ReviewCorrectionChoice(
                            transaction = current,
                            category = current.category,
                            transactionType = current.transactionType
                        ),
                        rememberRule = false
                    )
                },
                onEdit = { onTransactionClick(current) },
                onAllCategories = { categoriesOpen = true },
                onSkip = {
                    currentIndex = if (candidates.size <= 1) {
                        currentIndex
                    } else {
                        (currentIndex + 1) % candidates.size
                    }
                }
            )
        }
    }

    if (categoriesOpen && current != null) {
        ModalBottomSheet(
            onDismissRequest = { categoriesOpen = false },
            containerColor = if (isDarkModeActive()) Color(0xFF143A35) else Color(0xFFF5F8EE),
            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
        ) {
            ReviewCategorySheet(
                transaction = current,
                palette = palette,
                onCategory = { category ->
                    categoriesOpen = false
                    offerCorrection(
                        ReviewCorrectionChoice(
                            transaction = current,
                            category = category,
                            transactionType = current.reviewTypeForCategory(category)
                        )
                    )
                }
            )
        }
    }

    pendingCorrection?.let { choice ->
        ModalBottomSheet(
            onDismissRequest = { pendingCorrection = null },
            containerColor = if (isDarkModeActive()) Color(0xFF143A35) else Color(0xFFF5F8EE),
            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
        ) {
            ReviewRuleOfferSheet(
                choice = choice,
                palette = palette,
                onJustThisOne = { submitCorrection(choice, rememberRule = false) },
                onMakeRule = { submitCorrection(choice, rememberRule = true) }
            )
        }
    }
}

@Composable
private fun ReviewProgressHeader(
    resolvedCount: Int,
    totalCount: Int,
    hasCurrent: Boolean,
    palette: HomePalette,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.header)
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = palette.ink
                )
            }
            Text(
                text = "Review",
                modifier = Modifier.padding(start = 2.dp),
                color = palette.ink,
                fontFamily = SortedHomeFontFamily,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = when {
                    hasCurrent -> "${(resolvedCount + 1).coerceAtMost(totalCount)} of $totalCount"
                    totalCount == 0 -> "Nothing to do"
                    else -> "All done"
                },
                color = palette.muted,
                fontFamily = SortedHomeFontFamily,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
        if (totalCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(totalCount) { index ->
                    val color = when {
                        index < resolvedCount -> palette.ink
                        index == resolvedCount && hasCurrent -> palette.review
                        else -> palette.ink.copy(alpha = 0.14f)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color)
                    )
                }
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(palette.rule)
    )
}

@Composable
private fun ReviewCurrentPayment(
    transaction: TransactionUi,
    monthSpend: Double,
    palette: HomePalette,
    onChooseCategory: (String) -> Unit,
    onSuggestedType: () -> Unit,
    onKeep: () -> Unit,
    onEdit: () -> Unit,
    onAllCategories: () -> Unit,
    onSkip: () -> Unit
) {
    val reason = transaction.reviewPromptTitle()
    val hint = transaction.reviewPromptHint()
    val categoryGuess = transaction.reviewCategoryGuess()
    val secondCategory = transaction.reviewSecondaryCategory(categoryGuess)
    val suggestionFirst = reason == "Looks like a transfer" || reason == "Looks like an investment"

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .graphicsLayer(rotationZ = -1.5f)
                        .dottedOutline(palette.review)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(palette.reviewDot)
                    )
                    Text(
                        text = reason.uppercase(Locale.US),
                        modifier = Modifier.padding(start = 8.dp),
                        color = palette.review,
                        fontFamily = SortedHomeFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.08.sp
                    )
                }
                Text(
                    text = hint,
                    modifier = Modifier.padding(top = 8.dp),
                    color = palette.muted,
                    fontFamily = SortedHomeFontFamily,
                    fontSize = 12.5.sp
                )
                Text(
                    text = transaction.merchant,
                    modifier = Modifier.padding(top = 18.dp),
                    color = palette.ink,
                    fontFamily = SortedHomeFontFamily,
                    fontSize = 26.sp,
                    lineHeight = 29.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = transaction.reviewMeta(),
                    modifier = Modifier.padding(top = 5.dp),
                    color = palette.muted,
                    fontFamily = SortedHomeFontFamily,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = transaction.displayAmount(),
                    modifier = Modifier.padding(top = 14.dp),
                    color = palette.ink,
                    fontFamily = SortedHomeFontFamily,
                    fontSize = 46.sp,
                    lineHeight = 46.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }

        item {
            ReviewAlertBand(transaction = transaction, palette = palette)
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                if (suggestionFirst) {
                    ReviewChoiceButton(
                        label = "Sort as ${transaction.reviewSuggestedOutgoingType().displayName()}",
                        note = "Sorted's guess",
                        primary = true,
                        palette = palette,
                        onClick = onSuggestedType
                    )
                    ReviewChoiceButton(
                        label = "Sort as $categoryGuess",
                        note = null,
                        primary = false,
                        palette = palette,
                        onClick = { onChooseCategory(categoryGuess) }
                    )
                } else {
                    ReviewChoiceButton(
                        label = "Sort as $categoryGuess",
                        note = "Sorted's guess",
                        primary = true,
                        palette = palette,
                        onClick = { onChooseCategory(categoryGuess) }
                    )
                    ReviewChoiceButton(
                        label = "Sort as $secondCategory",
                        note = null,
                        primary = false,
                        palette = palette,
                        onClick = { onChooseCategory(secondCategory) }
                    )
                    ReviewChoiceButton(
                        label = "Sort as ${transaction.reviewSuggestedOutgoingType().displayName()}",
                        note = null,
                        primary = false,
                        palette = palette,
                        onClick = onSuggestedType
                    )
                }
                ReviewTextAction(label = "Keep as is", palette = palette, onClick = onKeep)
                ReviewTextAction(label = "Edit transaction", palette = palette, onClick = onEdit)
                ReviewTextAction(label = "All categories", palette = palette, onClick = onAllCategories)
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(
                            color = palette.faintRule,
                            start = Offset.Zero,
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Spent this month",
                        color = palette.muted,
                        fontFamily = SortedHomeFontFamily,
                        fontSize = 11.5.sp
                    )
                    Text(
                        text = monthSpend.formatHomeRupee(),
                        modifier = Modifier.padding(top = 2.dp),
                        color = palette.ink,
                        fontFamily = SortedHomeFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "Skip for now",
                    modifier = Modifier.clickable(onClick = onSkip),
                    color = palette.muted,
                    fontFamily = SortedHomeFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ReviewAlertBand(
    transaction: TransactionUi,
    palette: HomePalette
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.band)
            .drawBehind {
                drawLine(palette.rule, Offset.Zero, Offset(size.width, 0f), 1.dp.toPx())
                drawLine(palette.rule, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                val step = 21.dp.toPx()
                var y = 43.dp.toPx()
                while (y < size.height) {
                    drawLine(
                        palette.ink.copy(alpha = 0.07f),
                        Offset(20.dp.toPx(), y),
                        Offset(size.width - 20.dp.toPx(), y),
                        1.dp.toPx()
                    )
                    y += step
                }
            }
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Text(
            text = "WHAT THE ALERT SAID",
            color = palette.muted,
            fontFamily = SortedHomeFontFamily,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.47.sp
        )
        Text(
            text = transaction.reviewAlertCopy(),
            modifier = Modifier.padding(top = 8.dp),
            color = palette.ink,
            fontFamily = SortedHomeFontFamily,
            fontSize = 12.5.sp,
            lineHeight = 21.sp
        )
    }
}

@Composable
private fun ReviewChoiceButton(
    label: String,
    note: String?,
    primary: Boolean,
    palette: HomePalette,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (primary) palette.ink else palette.softFill)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = if (primary) palette.background else palette.ink,
            fontFamily = SortedHomeFontFamily,
            fontSize = 15.5.sp,
            fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!note.isNullOrBlank()) {
            Text(
                text = note,
                modifier = Modifier.padding(start = 10.dp),
                color = if (primary) palette.background.copy(alpha = 0.75f) else palette.muted,
                fontFamily = SortedHomeFontFamily,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ReviewTextAction(
    label: String,
    palette: HomePalette,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = palette.muted,
            fontFamily = SortedHomeFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "  ›",
            color = palette.muted,
            fontFamily = SortedHomeFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ReviewFinishedState(
    reviewedCount: Int,
    monthLabel: String,
    monthSpend: Double,
    change: Double,
    madeRules: Int,
    palette: HomePalette,
    onBackHome: () -> Unit,
    onOpenRules: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SortedTallyMark(
            ink = palette.ink,
            clay = palette.review,
            modifier = Modifier
                .padding(top = 52.dp)
                .size(60.dp)
        )
        Text(
            text = "Nothing needs review",
            modifier = Modifier.padding(top = 16.dp),
            color = palette.ink,
            fontFamily = SortedHomeFontFamily,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = if (reviewedCount == 0) "$monthLabel is up to date." else "$reviewedCount payments reviewed.",
            modifier = Modifier.padding(top = 7.dp, start = 24.dp, end = 24.dp),
            color = palette.muted,
            fontFamily = SortedHomeFontFamily,
            fontSize = 13.5.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp)
                .background(palette.band)
                .drawBehind {
                    drawLine(palette.rule, Offset.Zero, Offset(size.width, 0f), 1.dp.toPx())
                    drawLine(palette.rule, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                }
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            ReviewSummaryRow("Spent this month", monthSpend.formatHomeRupee(), palette)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .height(1.dp)
                    .background(palette.faintRule)
            )
            ReviewSummaryRow(
                label = "Change after review",
                value = when {
                    change > 0.0 -> "+${change.formatHomeRupee()}"
                    change < 0.0 -> "−${kotlin.math.abs(change).formatHomeRupee()}"
                    else -> "—"
                },
                palette = palette
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            ReviewChoiceButton(
                label = "Back to home",
                note = null,
                primary = true,
                palette = palette,
                onClick = onBackHome
            )
            if (madeRules > 0) {
                ReviewTextAction(
                    label = "$madeRules new auto-sorting ${if (madeRules == 1) "rule" else "rules"}",
                    palette = palette,
                    onClick = onOpenRules
                )
            }
        }
    }
}

@Composable
private fun ReviewSummaryRow(
    label: String,
    value: String,
    palette: HomePalette
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = palette.muted,
            fontFamily = SortedHomeFontFamily,
            fontSize = 13.sp
        )
        Text(
            text = value,
            color = palette.ink,
            fontFamily = SortedHomeFontFamily,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ReviewCategorySheet(
    transaction: TransactionUi,
    palette: HomePalette,
    onCategory: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "All categories",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            color = palette.ink,
            fontFamily = SortedHomeFontFamily,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "${transaction.merchant} · ${transaction.displayAmount()}",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
            color = palette.muted,
            fontFamily = SortedHomeFontFamily,
            fontSize = 12.sp
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 470.dp)
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            items(ReviewCategories) { category ->
                ReviewSheetRow(
                    title = category,
                    detail = if (category == transaction.reviewCategoryGuess()) "Sorted's guess" else null,
                    palette = palette,
                    onClick = { onCategory(category) }
                )
            }
        }
    }
}

@Composable
private fun ReviewSheetRow(
    title: String,
    detail: String?,
    palette: HomePalette,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    palette.faintRule,
                    Offset(0f, size.height),
                    Offset(size.width, size.height),
                    1.dp.toPx()
                )
            }
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = palette.ink,
                fontFamily = SortedHomeFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            if (!detail.isNullOrBlank()) {
                Text(
                    text = detail,
                    modifier = Modifier.padding(top = 2.dp),
                    color = palette.muted,
                    fontFamily = SortedHomeFontFamily,
                    fontSize = 11.5.sp
                )
            }
        }
        Text("›", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 14.sp)
    }
}

@Composable
private fun ReviewRuleOfferSheet(
    choice: ReviewCorrectionChoice,
    palette: HomePalette,
    onJustThisOne: () -> Unit,
    onMakeRule: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = "Always sort ${choice.transaction.merchant} as ${choice.category}?",
            color = palette.ink,
            fontFamily = SortedHomeFontFamily,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Use this for future ${choice.transaction.merchant} payments. You can change rules in Settings.",
            modifier = Modifier.padding(top = 4.dp),
            color = palette.muted,
            fontFamily = SortedHomeFontFamily,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
        Row(
            modifier = Modifier.padding(top = 16.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                ReviewChoiceButton(
                    label = "Just this one",
                    note = null,
                    primary = false,
                    palette = palette,
                    onClick = onJustThisOne
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                ReviewChoiceButton(
                    label = "Make a rule",
                    note = null,
                    primary = true,
                    palette = palette,
                    onClick = onMakeRule
                )
            }
        }
    }
}

private fun TransactionUi.reviewPromptTitle(): String {
    val merchantLower = merchant.lowercase(Locale.US)
    return when {
        transactionType == TransactionType.INVESTMENT ||
            merchantLower.contains("zerodha") ||
            merchantLower.contains("groww") ||
            merchantLower.contains("mutual fund") -> "Looks like an investment"
        transactionType == TransactionType.TRANSFER ||
            merchantLower.startsWith("to ") ||
            merchantLower.contains("own account") -> "Looks like a transfer"
        !countsInInrTotals() -> "Check this amount"
        source == "Gmail" && (inrAmountValue ?: 0.0) >= 10_000.0 -> "Check this payment"
        else -> "Which category?"
    }
}

private fun TransactionUi.reviewPromptHint(): String {
    return when (reviewPromptTitle()) {
        "Looks like an investment" -> "Investment payments stay in your monthly total"
        "Looks like a transfer" -> "Transfers stay in your monthly total"
        "Check this amount" -> "Sorted needs you to confirm the converted amount"
        "Check this payment" -> "A larger payment imported from Gmail"
        else -> "A new place — which category should it use?"
    }
}

private fun TransactionUi.reviewCategoryGuess(): String {
    if (category.isNotBlank() && category != "Other") return category
    val value = merchant.lowercase(Locale.US)
    return when {
        listOf("swiggy", "zomato", "restaurant", "cafe").any(value::contains) -> "Food"
        listOf("amazon", "flipkart", "myntra", "store").any(value::contains) -> "Shopping"
        listOf("electric", "airtel", "jio", "bill", "bescom").any(value::contains) -> "Bills"
        listOf("uber", "ola", "metro", "irctc", "flight").any(value::contains) -> "Travel"
        listOf("hospital", "pharmacy", "medical", "clinic").any(value::contains) -> "Health"
        else -> "Shopping"
    }
}

private fun TransactionUi.reviewSecondaryCategory(primary: String): String {
    return when (primary) {
        "Food" -> "Shopping"
        "Shopping" -> "Food"
        "Bills" -> "Home"
        "Travel" -> "Shopping"
        "Health" -> "Home"
        "Home" -> "Bills"
        else -> "Shopping"
    }
}

private fun TransactionUi.reviewSuggestedOutgoingType(): TransactionType {
    val value = merchant.lowercase(Locale.US)
    return if (
        transactionType == TransactionType.INVESTMENT ||
        listOf("zerodha", "groww", "mutual fund", "investment").any(value::contains)
    ) {
        TransactionType.INVESTMENT
    } else {
        TransactionType.TRANSFER
    }
}

private fun TransactionUi.reviewTypeForCategory(category: String): TransactionType = when (category) {
    "Investment" -> TransactionType.INVESTMENT
    "Transfer" -> TransactionType.TRANSFER
    else -> if (transactionType == TransactionType.SUBSCRIPTION) TransactionType.SUBSCRIPTION else TransactionType.EXPENSE
}

private fun TransactionUi.canCreateReviewRule(): Boolean {
    return id != null && merchant.isNotBlank() && merchant != "Unknown" && !merchant.looksLikeRawPaymentHandle()
}

private fun TransactionUi.reviewMeta(): String {
    return listOfNotNull(
        transactionDate?.takeIf(String::isNotBlank),
        paymentMode.takeIf(String::isNotBlank)
    ).joinToString(" · ").ifBlank { source }
}

private fun TransactionUi.reviewAlertCopy(): String {
    val amountText = if (currency == "INR") {
        amountValue.formatHomeRupee()
    } else {
        amount
    }
    return "$amountText at $merchant. $detail. Imported from $source."
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
    palette: TapePalette,
    onTransactionClick: (TransactionUi) -> Unit,
    onOpenInbox: () -> Unit
) {
    val previewRows = transactions.take(6)
    val total = transactions.sumOf { it.inrAmountValue ?: 0.0 }

    TapeLedgerBlock(
        heading = "Need review",
        meta = "${transactions.size} payments",
        palette = palette
    ) {
        TapeSummationRow("NEEDS REVIEW", total.formatRupee(), palette, palette.query)
        if (previewRows.isEmpty()) {
            IndexEmptyLine("NO REVIEW ITEMS", palette)
        } else {
            previewRows.forEach { transaction ->
                TapeInlineLine(
                    transaction = transaction,
                    palette = palette,
                    reason = transaction.reviewReason(),
                    onClick = { onTransactionClick(transaction) }
                )
            }
        }
        if (transactions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TapeActionText(
                    label = "Open Sort Inbox",
                    palette = palette,
                    color = palette.query,
                    onClick = onOpenInbox
                )
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
                text = breakdown.monthKey?.monthMovementLabel() ?: "Spent this month",
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
                    label = "Average payment",
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
    onSave: (ManualTransactionDraft) -> Unit,
    onUndo: (String) -> Unit,
    onClose: () -> Unit,
    onResetSaved: () -> Unit
) {
    AddPaymentScreen(
        feedState = feedState,
        saveState = saveState,
        modifier = modifier,
        onSave = onSave,
        onUndo = onUndo,
        onClose = onClose,
        onResetSaved = onResetSaved
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPaymentScreen(
    feedState: FeedState,
    saveState: ManualSaveState,
    modifier: Modifier = Modifier,
    onSave: (ManualTransactionDraft) -> Unit,
    onUndo: (String) -> Unit,
    onClose: () -> Unit,
    onResetSaved: () -> Unit
) {
    val palette = homePalette()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val amountFocusRequester = remember { FocusRequester() }
    var amount by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var paymentMode by remember { mutableStateOf(PaymentMode.CASH) }
    var sheet by remember { mutableStateOf<String?>(null) }
    var showPlaceEditor by remember { mutableStateOf(false) }
    var newPlace by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var savedDraft by remember { mutableStateOf<ManualTransactionDraft?>(null) }
    var savedHash by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(saveState.sourceHash, saveState.draft) {
        if (saveState.sourceHash != null && saveState.draft != null) {
            savedHash = saveState.sourceHash
            savedDraft = saveState.draft
        } else if (saveState.sourceHash == null && !saveState.isSaving) {
            savedHash = null
            savedDraft = null
        }
    }
    LaunchedEffect(savedDraft) {
        if (savedDraft == null) {
            delay(180)
            amountFocusRequester.requestFocus()
        }
    }

    val categories = listOf(
        "Food", "Groceries", "Shopping", "Subscriptions", "Transport",
        "Utilities", "Health", "Home", "Entertainment", "Other"
    )
    val recentPlaces = remember(feedState.transactions) {
        feedState.transactions
            .filter { it.merchant.isNotBlank() && it.merchant != "Unknown" }
            .groupBy { it.merchant.trim() }
            .map { (name, transactions) ->
                val latest = transactions.maxByOrNull { it.transactionDate.orEmpty() }
                Triple(name, latest?.category ?: "Other", transactions.size)
            }
            .sortedByDescending { it.third }
            .take(8)
    }
    val parsedAmount = amount.toDoubleOrNull()
    val canSave = parsedAmount != null && parsedAmount > 0.0 && !saveState.isSaving

    fun resetForm() {
        amount = ""
        merchant = ""
        category = null
        date = LocalDate.now()
        paymentMode = PaymentMode.CASH
        validationError = null
        savedDraft = null
        savedHash = null
        onResetSaved()
    }

    val amountTone = when {
        validationError != null -> palette.review
        canSave -> palette.ink
        else -> palette.muted
    }
    val currentDateLabel = when (date) {
        LocalDate.now() -> "Today"
        LocalDate.now().minusDays(1) -> "Yesterday"
        else -> date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
    }
    val savedPayment = savedDraft
    val monthTotal = remember(feedState.transactions, savedPayment) {
        val monthKey = (savedPayment?.date ?: LocalDate.now().toString()).take(7)
        feedState.transactions
            .filter { it.transactionDate?.take(7) == monthKey && it.countsTowardSpentTotal() }
            .sumOf { it.inrAmountValue ?: 0.0 }
    }

    Column(
        modifier = modifier.fillMaxSize().imePadding().background(palette.background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(palette.header)
                .padding(start = 16.dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) { Text("×", color = palette.ink, fontSize = 28.sp, lineHeight = 30.sp) }
            Text(
                "Add a payment", Modifier.weight(1f), color = palette.ink,
                fontFamily = SortedHomeFontFamily, fontSize = 17.sp, fontWeight = FontWeight.SemiBold
            )
            Text(
                date.format(java.time.format.DateTimeFormatter.ofPattern("MMMM", Locale.getDefault())),
                color = palette.muted, fontFamily = SortedHomeFontFamily,
                fontSize = 12.5.sp, fontWeight = FontWeight.Medium
            )
        }

        if (savedPayment == null) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Amount", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 12.5.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("₹", modifier = Modifier.padding(top = 8.dp), color = amountTone, fontFamily = SortedHomeFontFamily, fontSize = 26.sp, fontWeight = FontWeight.Medium)
                        BasicTextField(
                            value = amount,
                            onValueChange = { entered ->
                                val clean = entered.filter { it.isDigit() || it == '.' }
                                val decimalAt = clean.indexOf('.')
                                amount = if (decimalAt < 0) {
                                    clean.take(9)
                                } else {
                                    clean.substring(0, decimalAt).take(9) + "." +
                                        clean.substring(decimalAt + 1).replace(".", "").take(2)
                                }
                                validationError = null
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = TextStyle(
                                color = amountTone,
                                fontFamily = SortedHomeFontFamily,
                                fontSize = 56.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 58.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            modifier = Modifier.width((amount.length * 34 + 20).coerceIn(96, 300).dp).focusRequester(amountFocusRequester),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (amount.isEmpty()) {
                                        Text("0", color = amountTone, fontFamily = SortedHomeFontFamily, fontSize = 56.sp, fontWeight = FontWeight.SemiBold, lineHeight = 58.sp)
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                    if (validationError != null) {
                        Text(validationError.orEmpty(), color = palette.review, fontFamily = SortedHomeFontFamily, fontSize = 12.5.sp)
                    }
                    if (saveState.error != null) {
                        Text(saveState.error.orEmpty(), color = palette.review, fontFamily = SortedHomeFontFamily, fontSize = 12.5.sp)
                    }
                }

                Column(modifier = Modifier.fillMaxWidth().background(palette.band).padding(horizontal = 20.dp)) {
                    AddPaymentDetailRow("Where", merchant.ifBlank { "Add a place" }, merchant.isBlank(), palette) { sheet = "place" }
                    AddPaymentDetailRow("Category", category ?: "Pick one", category == null, palette) { sheet = "category" }
                    AddPaymentDetailRow("When", currentDateLabel, false, palette) { sheet = "date" }
                    AddPaymentDetailRow("Paid with", paymentMode.displayName(), false, palette, last = true) { sheet = "paid" }
                }

                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)
                        .heightIn(min = 54.dp).clip(RoundedCornerShape(10.dp))
                        .background(if (canSave) palette.ink else palette.softFill)
                        .clickable {
                            if (!canSave) {
                                validationError = "Enter how much you paid"
                            } else {
                                validationError = null
                                keyboardController?.hide()
                                onSave(
                                    ManualTransactionDraft(
                                        merchant = merchant.trim().ifBlank { "Cash payment" },
                                        amount = parsedAmount,
                                        date = date.toString(),
                                        category = category ?: "Other",
                                        miscCategory = "Manual",
                                        paymentMode = paymentMode,
                                        transactionType = TransactionType.EXPENSE,
                                        direction = Direction.DEBIT
                                    )
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (saveState.isSaving) "Saving…" else "Save payment",
                        color = if (canSave) palette.background else palette.muted,
                        fontFamily = SortedHomeFontFamily, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold
                    )
                }

            }
        } else {
            AddPaymentSaved(
                draft = savedPayment,
                monthTotal = monthTotal,
                palette = palette,
                onAddAnother = { resetForm() },
                onUndo = {
                    val hash = savedHash
                    if (hash != null) onUndo(hash)
                    savedDraft = null
                    savedHash = null
                },
                onClose = onClose,
                undoing = saveState.isSaving,
                error = saveState.error
            )
        }
    }

    if (sheet != null) {
        ModalBottomSheet(
            onDismissRequest = { sheet = null },
            containerColor = palette.background,
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 18.dp)) {
                val title = when (sheet) {
                    "place" -> "Where did you pay?"
                    "category" -> "Category"
                    "date" -> "When did you pay?"
                    else -> "Paid with"
                }
                Text(title, color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                when (sheet) {
                    "place" -> {
                        TextButton(onClick = { sheet = null; showPlaceEditor = true }) { Text("Add a place", color = palette.ink) }
                        recentPlaces.forEach { (name, suggestedCategory, count) ->
                            AddPaymentSheetRow(name, "$suggestedCategory · $count ${if (count == 1) "payment" else "payments"}", palette) {
                                merchant = name
                                if (category == null) category = suggestedCategory
                                sheet = null
                            }
                        }
                    }
                    "category" -> categories.forEach { choice ->
                        AddPaymentSheetRow(choice, if (choice == category) "Selected" else "", palette) {
                            category = choice
                            sheet = null
                        }
                    }
                    "date" -> {
                        AddPaymentSheetRow("Today", LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())), palette) {
                            date = LocalDate.now(); sheet = null
                        }
                        AddPaymentSheetRow("Yesterday", LocalDate.now().minusDays(1).format(java.time.format.DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())), palette) {
                            date = LocalDate.now().minusDays(1); sheet = null
                        }
                        AddPaymentSheetRow("Choose a date", "Open calendar", palette) {
                            sheet = null
                            DatePickerDialog(
                                context,
                                { _, year, month, day -> date = LocalDate.of(year, month + 1, day) },
                                date.year,
                                date.monthValue - 1,
                                date.dayOfMonth
                            ).show()
                        }
                    }
                    "paid" -> listOf(PaymentMode.CASH, PaymentMode.UPI, PaymentMode.CARD).forEach { mode ->
                        AddPaymentSheetRow(mode.displayName(), if (mode == paymentMode) "Selected" else "", palette) {
                            paymentMode = mode
                            sheet = null
                        }
                    }
                }
            }
        }
    }

    if (showPlaceEditor) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPlaceEditor = false },
            containerColor = palette.background,
            title = { Text("Add a place", color = palette.ink, fontFamily = SortedHomeFontFamily) },
            text = {
                OutlinedTextField(
                    value = newPlace,
                    onValueChange = { newPlace = it.take(48) },
                    singleLine = true,
                    placeholder = { Text("Place or person") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    merchant = newPlace.trim()
                    newPlace = ""
                    showPlaceEditor = false
                }, enabled = newPlace.isNotBlank()) { Text("Add", color = palette.ink) }
            },
            dismissButton = { TextButton(onClick = { showPlaceEditor = false }) { Text("Cancel", color = palette.muted) } }
        )
    }
}

@Composable
private fun AddPaymentDetailRow(
    label: String,
    value: String,
    mutedValue: Boolean,
    palette: HomePalette,
    last: Boolean = false,
    onClick: () -> Unit
) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, Modifier.width(88.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 13.sp)
            Text(value, Modifier.weight(1f), color = if (mutedValue) palette.muted else palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 15.5.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("›", Modifier.padding(start = 8.dp), color = palette.muted, fontSize = 21.sp)
        }
        if (!last) Spacer(Modifier.fillMaxWidth().height(1.dp).background(palette.faintRule))
    }
}

@Composable
private fun AddPaymentSheetRow(label: String, detail: String, palette: HomePalette, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (detail.isNotBlank()) Text(detail, color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.5.sp)
        }
        Text("›", color = palette.muted, fontSize = 20.sp)
    }
    Spacer(Modifier.fillMaxWidth().height(1.dp).background(palette.faintRule))
}

@Composable
private fun AddPaymentSaved(
    draft: ManualTransactionDraft,
    monthTotal: Double,
    palette: HomePalette,
    onAddAnother: () -> Unit,
    onUndo: () -> Unit,
    onClose: () -> Unit,
    undoing: Boolean,
    error: String?
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Added", color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Text("You can change or delete it any time.", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 13.5.sp, textAlign = TextAlign.Center)
        }
        Column(Modifier.fillMaxWidth().background(palette.band).padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(palette.softFill), contentAlignment = Alignment.Center) {
                    Text(draft.merchant.firstOrNull()?.uppercase() ?: "C", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(Modifier.weight(1f)) {
                    Text(draft.merchant, color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 15.5.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${draft.category} · ${manualDateLabel(draft.date)} · ${draft.paymentMode.displayName()}", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 12.sp)
                }
                Text(draft.amount.formatHomeRupee(), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, style = TextStyle(fontFeatureSettings = "tnum"))
            }
            Spacer(Modifier.fillMaxWidth().padding(top = 12.dp).height(1.dp).background(palette.faintRule))
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Spent this month", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 13.sp)
                Text(monthTotal.formatHomeRupee(), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, style = TextStyle(fontFeatureSettings = "tnum"))
            }
            Text("Added by you", Modifier.padding(top = 8.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.5.sp)
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.fillMaxWidth().heightIn(min = 50.dp).clip(RoundedCornerShape(8.dp)).background(palette.ink).clickable(onClick = onAddAnother), contentAlignment = Alignment.Center) {
                Text("Add another", color = palette.background, fontFamily = SortedHomeFontFamily, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = onUndo, enabled = !undoing) {
                Text(if (undoing) "Undoing…" else "Undo this payment", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 14.sp)
            }
            if (error != null) Text(error, color = palette.review, fontFamily = SortedHomeFontFamily, fontSize = 12.5.sp)
        }
        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp).heightIn(min = 50.dp).clip(RoundedCornerShape(8.dp)).background(palette.softFill).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
            Text("Back to home", color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 15.5.sp, fontWeight = FontWeight.Medium)
        }
    }
}

private fun formatManualAmount(raw: String): String {
    if (raw.isBlank()) return "0"
    val parts = raw.split('.', limit = 2)
    val whole = parts.first().ifBlank { "0" }
    val grouped = buildString {
        whole.forEachIndexed { index, digit ->
            val remaining = whole.length - index
            if (index > 0 && (remaining == 3 || remaining > 3 && (remaining - 3) % 2 == 0)) append(',')
            append(digit)
        }
    }
    return if (parts.size == 2) "$grouped.${parts[1]}" else grouped
}

private fun manualDateLabel(raw: String): String {
    val date = raw.toLocalDateOrNull() ?: return raw
    return when (date) {
        LocalDate.now() -> "Today"
        LocalDate.now().minusDays(1) -> "Yesterday"
        else -> date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
    }
}

@Composable
private fun ManualAddCard(
    feedState: FeedState,
    saveState: ManualSaveState,
    onSave: (ManualTransactionDraft) -> Unit,
    palette: TapePalette
) {
    var merchant by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var category by remember { mutableStateOf("Other") }
    var miscCategory by remember { mutableStateOf("Manual") }
    var paymentMode by remember { mutableStateOf(PaymentMode.UPI) }
    var transactionType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var direction by remember { mutableStateOf(Direction.DEBIT) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showPreview by remember { mutableStateOf(false) }
    var printedAck by remember { mutableStateOf(false) }

    LaunchedEffect(saveState.message) {
        if (saveState.message != null) {
            printedAck = true
            showPreview = false
        }
    }

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
    val parsedAmount = amount.toDoubleOrNull()
    val isMoneyIn = direction == Direction.CREDIT
    val spendDelta = if (
        parsedAmount != null &&
        parsedAmount > 0.0 &&
        direction == Direction.DEBIT
    ) {
        parsedAmount
    } else {
        0.0
    }
    val breakdown = remember(feedState.transactions) { feedState.transactions.monthBreakdown() }
    val afterSpend = breakdown.spends + spendDelta
    val afterLines = breakdown.spendCount + if (spendDelta > 0.0) 1 else 0
    val selectedTypeLabel = when (transactionType) {
        TransactionType.EXPENSE -> "SPEND"
        TransactionType.SUBSCRIPTION -> "SPEND"
        TransactionType.TRANSFER -> "MOVED"
        TransactionType.INVESTMENT -> "INVESTED"
        TransactionType.INCOME -> "INCOME"
        TransactionType.REFUND -> "REFUND"
        TransactionType.REWARD -> "REWARD"
        TransactionType.UNKNOWN -> "SPEND"
    }
    val canPreview = parsedAmount != null && parsedAmount > 0.0 && !saveState.isSaving

    fun applyType(label: String) {
        when (label) {
            "SPEND" -> {
                transactionType = TransactionType.EXPENSE
                direction = Direction.DEBIT
                if (category in listOf("Investment", "Transfer", "Income", "Refund", "Reward")) category = "Other"
            }
            "MOVED" -> {
                transactionType = TransactionType.TRANSFER
                direction = Direction.DEBIT
                category = "Transfer"
            }
            "INVESTED" -> {
                transactionType = TransactionType.INVESTMENT
                direction = Direction.DEBIT
                category = "Investment"
            }
            "REFUND" -> {
                transactionType = TransactionType.REFUND
                direction = Direction.CREDIT
                category = "Refund"
            }
            "INCOME" -> {
                transactionType = TransactionType.INCOME
                direction = Direction.CREDIT
                category = "Income"
            }
            "REWARD" -> {
                transactionType = TransactionType.REWARD
                direction = Direction.CREDIT
                category = "Reward"
            }
        }
    }

    fun resetLine() {
        amount = ""
        merchant = ""
        date = LocalDate.now().toString()
        category = "Other"
        miscCategory = "Manual"
        paymentMode = PaymentMode.UPI
        transactionType = TransactionType.EXPENSE
        direction = Direction.DEBIT
        validationError = null
        showPreview = false
        printedAck = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = if (printedAck) "PRINTED" else if (showPreview) "THIS WILL PRINT AS" else "PRINT A LINE",
            color = palette.inkSoft,
            fontFamily = SortedTapeFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.4.sp,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(12.dp))
        AddAmountBlock(
            amount = amount,
            palette = palette,
            onAmountChange = {
                amount = it.filter { char -> char.isDigit() || char == '.' }.take(12)
                printedAck = false
            }
        )
        TapeDoubleRule(palette = palette)
        AddInputLine(
            label = "MERCHANT",
            value = merchant,
            placeholder = "---",
            palette = palette,
            onValueChange = {
                merchant = it.take(48)
                printedAck = false
            }
        )
        if (recentMerchants.isNotEmpty()) {
            AddStampRail(
                stamps = recentMerchants.take(4).map { it.merchant.uppercase(Locale.US).take(14) },
                selected = merchant.uppercase(Locale.US),
                palette = palette,
                selectedColor = palette.amber,
                onSelected = { selected ->
                    val template = recentMerchants.firstOrNull {
                        it.merchant.equals(selected, ignoreCase = true) ||
                            it.merchant.uppercase(Locale.US).take(14) == selected
                    }
                    if (template != null) {
                        merchant = template.merchant
                        category = template.category
                        miscCategory = template.miscCategory
                        paymentMode = PaymentMode.entries.firstOrNull {
                            it.displayName() == template.paymentMode
                        } ?: paymentMode
                        transactionType = template.transactionType
                        direction = if (template.direction == DirectionUi.Credit) Direction.CREDIT else Direction.DEBIT
                    }
                    printedAck = false
                }
            )
        }
        AddInputLine(
            label = "DATE · MODE",
            value = date,
            placeholder = LocalDate.now().toString(),
            palette = palette,
            suffix = paymentMode.displayName().uppercase(Locale.US),
            keyboardType = KeyboardType.Number,
            onValueChange = {
                date = it.take(10)
                printedAck = false
            }
        )
        AddSectionLabel("TYPE", palette)
        AddStampRail(
            stamps = listOf("SPEND", "MOVED", "INVESTED", "REFUND", "INCOME", "REWARD"),
            selected = selectedTypeLabel,
            palette = palette,
            selectedColor = if (isMoneyIn) palette.credit else palette.amber,
            onSelected = {
                applyType(it)
                printedAck = false
            }
        )
        if (isMoneyIn) {
            AddMoneyInNotice(
                title = "MONEY IN",
                body = "Shown separately. It does not reduce this month's outgoing total.",
                palette = palette
            )
        } else {
            AddSectionLabel("CATEGORY", palette)
            AddStampRail(
                stamps = categories
                    .filterNot { it in listOf("Income", "Refund", "Reward") }
                    .map { it.uppercase(Locale.US) },
                selected = category.uppercase(Locale.US),
                palette = palette,
                selectedColor = palette.amber,
                onSelected = {
                    category = it.lowercase(Locale.US).replaceFirstChar { char -> char.titlecase(Locale.US) }
                    printedAck = false
                }
            )
        }
        AddSectionLabel("SOURCE", palette)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AddStamp("MANUAL", selected = true, palette = palette, color = palette.inkSoft, modifier = Modifier.weight(1f))
            AddStamp(paymentMode.displayName().uppercase(Locale.US), selected = false, palette = palette, color = palette.inkSoft, modifier = Modifier.weight(1f))
            AddStamp(if (direction == Direction.CREDIT) "CREDIT" else "DEBIT", selected = false, palette = palette, color = palette.inkSoft, modifier = Modifier.weight(1f))
        }
        val statusText = validationError ?: saveState.error ?: saveState.message
        if (statusText != null) {
            AddNotice(
                text = statusText.uppercase(Locale.US),
                palette = palette,
                color = if (validationError == null && saveState.error == null) palette.amber else palette.query
            )
        }
        if (showPreview || printedAck) {
            AddPrintPreview(
                merchant = merchant.ifBlank { "---" },
                amount = parsedAmount ?: 0.0,
                category = category,
                mode = paymentMode.displayName(),
                date = date,
                afterSpend = afterSpend,
                beforeSpend = breakdown.spends,
                afterLines = afterLines,
                isMoneyIn = isMoneyIn,
                printed = printedAck,
                palette = palette
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AddActionButton(
                label = if (printedAck) "ADD ANOTHER" else "CLEAR",
                enabled = true,
                palette = palette,
                color = palette.inkFaint,
                modifier = Modifier.weight(1f),
                onClick = { resetLine() }
            )
            AddActionButton(
                label = when {
                    saveState.isSaving -> "PRINTING"
                    printedAck -> "VIEW ON TAPE"
                    showPreview -> "PRINT"
                    canPreview -> "PREVIEW LINE"
                    else -> "ENTER AN AMOUNT"
                },
                enabled = canPreview && !saveState.isSaving || printedAck,
                palette = palette,
                color = palette.amber,
                modifier = Modifier.weight(1.45f),
                onClick = {
                    validationError = when {
                        parsedAmount == null || parsedAmount <= 0.0 -> "Enter an amount."
                        date.toLocalDateOrNull() == null -> "Use date as YYYY-MM-DD."
                        else -> null
                    }
                    if (validationError != null) return@AddActionButton
                    if (!showPreview && !printedAck) {
                        showPreview = true
                        return@AddActionButton
                    }
                    if (printedAck) {
                        resetLine()
                        return@AddActionButton
                    }
                    onSave(
                        ManualTransactionDraft(
                            merchant = merchant.trim().ifBlank { "Manual line" },
                            amount = parsedAmount ?: 0.0,
                            date = date,
                            category = category,
                            miscCategory = miscCategory.ifBlank { "Manual" },
                            paymentMode = paymentMode,
                            transactionType = transactionType,
                            direction = direction
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun AddDeskBar(
    palette: TapePalette,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.desk)
            .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "ADD",
            color = palette.inkSoft,
            fontFamily = SortedTapeFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 4.sp,
            maxLines = 1
        )
        Text(
            text = "  -  MANUAL LINE PRINTER",
            modifier = Modifier.weight(1f),
            color = palette.inkFaint,
            fontFamily = SortedTapeFontFamily,
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
private fun AddAmountBlock(
    amount: String,
    palette: TapePalette,
    onAmountChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "AMOUNT",
            color = palette.inkFaint,
            fontFamily = SortedTapeFontFamily,
            fontSize = 9.sp,
            letterSpacing = 1.7.sp,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "₹",
                color = if (amount.isBlank()) palette.inkFaint else palette.ink,
                fontFamily = SortedTapeFontFamily,
                fontSize = 36.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            BasicTextField(
                value = amount,
                onValueChange = onAmountChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = palette.ink,
                    fontFamily = SortedTapeFontFamily,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp
                ),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (amount.isBlank()) {
                            Text(
                                text = "0",
                                color = palette.inkFaint,
                                fontFamily = SortedTapeFontFamily,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
        Text(
            text = if (amount.isBlank()) "TAP AMOUNT" else "RUPEES · MANUAL",
            color = palette.inkFaint,
            fontFamily = SortedTapeFontFamily,
            fontSize = 8.sp,
            letterSpacing = 1.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun AddInputLine(
    label: String,
    value: String,
    placeholder: String,
    palette: TapePalette,
    onValueChange: (String) -> Unit,
    suffix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val y = size.height - 1.dp.toPx()
                drawLine(
                    color = palette.ruleFaint,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.width(104.dp),
            color = palette.inkFaint,
            fontFamily = SortedTapeFontFamily,
            fontSize = 8.sp,
            letterSpacing = 1.2.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = palette.ink,
                fontFamily = SortedTapeFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                textAlign = TextAlign.End
            ),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterEnd) {
                    if (value.isBlank()) {
                        Text(
                            text = placeholder,
                            color = palette.inkFaint,
                            fontFamily = SortedTapeFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp,
                            maxLines = 1
                        )
                    }
                    innerTextField()
                }
            }
        )
        if (suffix != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = suffix,
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
private fun AddSectionLabel(
    text: String,
    palette: TapePalette
) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 11.dp, bottom = 7.dp),
        color = palette.inkFaint,
        fontFamily = SortedTapeFontFamily,
        fontSize = 8.sp,
        letterSpacing = 1.8.sp,
        maxLines = 1
    )
}

@Composable
private fun AddStampRail(
    stamps: List<String>,
    selected: String,
    palette: TapePalette,
    selectedColor: Color,
    onSelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(stamps) { stamp ->
            AddStamp(
                text = stamp,
                selected = stamp.equals(selected, ignoreCase = true),
                palette = palette,
                color = selectedColor,
                onClick = { onSelected(stamp) }
            )
        }
    }
}

@Composable
private fun AddStamp(
    text: String,
    selected: Boolean,
    palette: TapePalette,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val borderColor = if (selected) color else palette.rule
    val background = if (selected) color.copy(alpha = 0.18f) else Color.Transparent
    Text(
        text = text,
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(2.dp))
            .background(background)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        color = if (selected) color else palette.inkSoft,
        fontFamily = SortedTapeFontFamily,
        fontSize = 9.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.9.sp,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun AddMoneyInNotice(
    title: String,
    body: String,
    palette: TapePalette
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .border(1.dp, palette.credit)
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Text(
            text = title,
            color = palette.credit,
            fontFamily = SortedTapeFontFamily,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.4.sp
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = body,
            color = palette.inkSoft,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun AddNotice(
    text: String,
    palette: TapePalette,
    color: Color
) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .border(1.dp, color)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        color = color,
        fontFamily = SortedTapeFontFamily,
        fontSize = 8.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun AddPrintPreview(
    merchant: String,
    amount: Double,
    category: String,
    mode: String,
    date: String,
    afterSpend: Double,
    beforeSpend: Double,
    afterLines: Int,
    isMoneyIn: Boolean,
    printed: Boolean,
    palette: TapePalette
) {
    Column(modifier = Modifier.padding(top = 14.dp)) {
        AddPreviewLine(
            merchant = merchant,
            amount = amount,
            category = category,
            mode = mode,
            date = date,
            printed = printed,
            isMoneyIn = isMoneyIn,
            palette = palette
        )
        TapeDoubleRule(palette = palette)
        Text(
            text = if (printed) "AFTER PRINTING" else "AFTER PRINTING",
            color = palette.inkFaint,
            fontFamily = SortedTapeFontFamily,
            fontSize = 8.sp,
            letterSpacing = 1.6.sp
        )
        TapeSummationRow(
            label = "MONTH SPEND",
            value = afterSpend.formatRupee(),
            palette = palette
        )
        if (!isMoneyIn) {
            TapeSummationRow(
                label = "WAS",
                value = beforeSpend.formatRupee(),
                palette = palette
            )
            TapeSummationRow(
                label = "PAYMENTS",
                value = afterLines.toString(),
                palette = palette
            )
        } else {
            TapeSummationRow(
                label = "MONEY IN",
                value = amount.formatRupee(),
                palette = palette,
                color = palette.credit
            )
        }
        if (printed) {
            Text(
                text = "ADDED TO TAPE",
                modifier = Modifier
                    .padding(top = 8.dp)
                    .border(1.dp, palette.amber)
                    .padding(horizontal = 11.dp, vertical = 8.dp),
                color = palette.amber,
                fontFamily = SortedTapeFontFamily,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.6.sp
            )
        }
    }
}

@Composable
private fun AddPreviewLine(
    merchant: String,
    amount: Double,
    category: String,
    mode: String,
    date: String,
    printed: Boolean,
    isMoneyIn: Boolean,
    palette: TapePalette
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (printed) palette.amber else palette.rule)
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = merchant.uppercase(Locale.US),
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
                text = (if (isMoneyIn) "+" else "") + amount.formatRupee(),
                color = if (isMoneyIn) palette.credit else palette.ink,
                fontFamily = SortedTapeFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AddStamp(category.uppercase(Locale.US), selected = true, palette = palette, color = if (isMoneyIn) palette.credit else palette.amber)
            Spacer(modifier = Modifier.width(6.dp))
            AddStamp("MANUAL", selected = false, palette = palette, color = palette.inkSoft)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$date · ${mode.uppercase(Locale.US)}${if (printed) " · JUST NOW" else ""}",
                modifier = Modifier.weight(1f),
                color = palette.inkFaint,
                fontFamily = SortedTapeFontFamily,
                fontSize = 8.sp,
                letterSpacing = 0.8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AddActionButton(
    label: String,
    enabled: Boolean,
    palette: TapePalette,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Text(
        text = label,
        modifier = modifier
            .border(1.dp, if (enabled) color else palette.rule)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 11.dp),
        color = if (enabled) color else palette.inkFaint,
        fontFamily = SortedTapeFontFamily,
        fontSize = 9.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
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
                gmailState = gmailState,
                palette = tapePalette()
            )
        }
        if (feedState.needsSmsPermission) {
            item {
                PermissionPrompt(
                    palette = tapePalette(),
                    onRequestPermission = onRequestSmsPermission
                )
            }
        }
        item {
            GmailImportCard(
                state = gmailState,
                setupInfo = gmailSetupInfo,
                palette = tapePalette(),
                onImport = onImportGmail
            )
        }
        item {
            Spacer(modifier = Modifier.height(104.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    themeMode: AppThemeMode,
    feedState: FeedState,
    gmailState: GmailUiState,
    modifier: Modifier = Modifier,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onBack: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    onOpenSmsSettings: () -> Unit,
    onImportGmail: () -> Unit,
    onExport: () -> Unit,
    onDeleteLocalData: () -> Unit,
    onOpenRuleCenter: () -> Unit
) {
    val palette = homePalette()
    val appContext = LocalContext.current.applicationContext
    var rules by remember { mutableStateOf(emptyList<CategoryRuleEntity>()) }
    var privacyOpen by remember { mutableStateOf(false) }
    var deleteConfirmOpen by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        rules = withContext(Dispatchers.IO) { TransactionRepository(appContext).listCategoryRules(limit = 300) }
    }
    val smsAllowed = !feedState.needsSmsPermission
    val gmailConnected = !gmailState.label.contains("not connected", ignoreCase = true) &&
        (gmailState.label.contains("connected", ignoreCase = true) || gmailState.label.contains("synced", ignoreCase = true))
    val gmailNeedsAction = gmailState.error != null || gmailState.label.contains("permission", ignoreCase = true)
    val gmailStatus = when {
        gmailState.isImporting -> "Checking for payment emails"
        gmailState.error?.contains("OAuth", ignoreCase = true) == true ||
            gmailState.error?.contains("UNREGISTERED", ignoreCase = true) == true ||
            gmailState.error?.contains("test-user", ignoreCase = true) == true -> "Google setup needs attention"
        gmailState.error?.contains("cancel", ignoreCase = true) == true -> "Google access was cancelled"
        gmailState.error?.contains("permission", ignoreCase = true) == true -> "Gmail read access wasn’t granted"
        gmailState.error != null -> gmailState.error.take(88)
        gmailConnected -> "Connected on this phone"
        else -> "Not connected"
    }
    val settingsHeader = if (isDarkModeActive()) Color(0xFF0F2C28) else palette.header

    Column(modifier.fillMaxSize().background(palette.background)) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).background(settingsHeader)
                .padding(start = 10.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.ink)
            }
            Text("Settings", Modifier.padding(start = 4.dp), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(palette.rule))
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            item {
                Column(Modifier.fillMaxWidth().padding(top = 20.dp)) {
                    SettingsSectionHeading("Imports", if (feedState.transactions.isEmpty()) "No payments yet" else "${feedState.transactions.size} payments", palette)
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        SettingsActionRow(
                            title = "SMS",
                            detail = if (smsAllowed) "Reading payment alerts on this phone" else "Allow access to find payment alerts",
                            action = if (smsAllowed) "Manage" else "Allow",
                            actionColor = if (smsAllowed) palette.ink else palette.review,
                            palette = palette,
                            onClick = if (smsAllowed) onOpenSmsSettings else onRequestSmsPermission
                        )
                        SettingsActionRow(
                            title = "Gmail",
                            detail = gmailStatus,
                            action = when {
                                gmailState.isImporting -> "Working"
                                gmailNeedsAction -> "Retry"
                                gmailConnected -> "Import"
                                else -> "Connect"
                            },
                            actionColor = if (gmailNeedsAction) palette.review else palette.ink,
                            palette = palette,
                            onClick = onImportGmail
                        )
                    }
                }
            }
            item {
                Column(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 1.dp).background(palette.band).padding(horizontal = 20.dp, vertical = 18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SettingsLockGlyph(palette.ink)
                        Text("Stays on this phone", Modifier.padding(start = 9.dp), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text("Your transaction data stays on this phone and isn’t uploaded.", Modifier.padding(top = 8.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 13.sp, lineHeight = 20.sp)
                    Spacer(Modifier.fillMaxWidth().padding(top = 12.dp).height(1.dp).background(palette.faintRule))
                    Row(Modifier.fillMaxWidth().clickable { privacyOpen = true }.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("What Sorted reads", Modifier.weight(1f), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
                        Text("›", color = palette.muted, fontSize = 22.sp)
                    }
                }
            }
            item {
                Column(Modifier.fillMaxWidth().padding(top = 20.dp)) {
                    SettingsSectionHeading("Auto-sorting rules", "${rules.size} saved", palette)
                    Text("Made from your corrections. Used on new payments.", Modifier.padding(horizontal = 20.dp, vertical = 8.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 12.5.sp, lineHeight = 18.sp)
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        rules.take(2).forEach { rule ->
                            val target = rule.departmentCategory ?: rule.miscCategory ?: rule.transactionType.displayName()
                            SettingsActionRow(
                                title = "${rule.merchantNormalized ?: rule.pattern} → $target",
                                detail = if (rule.enabled) "On" else "Off",
                                action = if (rule.enabled) "Edit" else "Off",
                                actionColor = palette.muted,
                                palette = palette,
                                onClick = onOpenRuleCenter
                            )
                        }
                        Row(Modifier.fillMaxWidth().clickable(onClick = onOpenRuleCenter).padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (rules.isEmpty()) "Manage auto-sorting rules" else "See all rules", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                            Text("  ›", color = palette.muted, fontSize = 16.sp)
                        }
                    }
                }
            }
            item {
                Column(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 1.dp).background(palette.band).padding(horizontal = 20.dp, vertical = 18.dp)) {
                    SettingsSectionHeading("Appearance", "", palette, horizontalPadding = 0.dp)
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        listOf(AppThemeMode.Light, AppThemeMode.Dark, AppThemeMode.System).forEach { mode ->
                            val selected = themeMode == mode
                            Box(Modifier.weight(1f).background(if (selected) palette.ink else palette.softFill, RoundedCornerShape(6.dp)).clickable { onThemeModeChange(mode) }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text(mode.label, color = if (selected) palette.background else palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 12.5.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
                            }
                        }
                    }
                    Text(if (themeMode == AppThemeMode.System) "Follows your phone’s setting." else "Using ${themeMode.label.lowercase(Locale.getDefault())} appearance.", Modifier.padding(top = 10.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 12.sp)
                }
            }
            item {
                Column(Modifier.fillMaxWidth().padding(top = 20.dp)) {
                    SettingsSectionHeading("Your data", "", palette)
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        SettingsActionRow("Export transactions", "CSV file · choose where to save it", "Export", palette.ink, palette, onExport)
                        SettingsActionRow("Delete local data", "Removes saved payments and rules from this phone.", "Delete", palette.review, palette, { deleteConfirmOpen = true }, last = true)
                    }
                }
            }
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)) {
                    Text("Sorted · ${feedState.transactions.size} payments", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.sp)
                    Text("Made to work without an account", Modifier.padding(top = 4.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.sp)
                }
            }
        }
    }

    if (privacyOpen) {
        ModalBottomSheet(onDismissRequest = { privacyOpen = false }, containerColor = palette.background, shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text("What Sorted reads", color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text("SMS payment alerts and, if you connect Gmail, the messages you allow Sorted to access. Sorted uses them to find transactions and keeps your transaction data on this phone.", Modifier.padding(top = 10.dp, bottom = 24.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 14.sp, lineHeight = 21.sp)
            }
        }
    }
    if (deleteConfirmOpen) {
        ModalBottomSheet(onDismissRequest = { deleteConfirmOpen = false }, containerColor = palette.background, shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                Text("Delete local data?", color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text("This removes saved payments, corrections, and auto-sorting rules from this phone. Import permissions stay on. This can’t be undone.", Modifier.padding(top = 8.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 14.sp, lineHeight = 21.sp)
                Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = { deleteConfirmOpen = false }, modifier = Modifier.weight(1f)) { Text("Keep my data", color = palette.ink) }
                    TextButton(onClick = { deleteConfirmOpen = false; onDeleteLocalData() }, modifier = Modifier.weight(1f)) { Text("Delete local data", color = palette.review) }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeading(title: String, meta: String, palette: HomePalette, horizontalPadding: androidx.compose.ui.unit.Dp = 20.dp) {
    Row(Modifier.fillMaxWidth().padding(horizontal = horizontalPadding), verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(Locale.US), Modifier.weight(1f), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.76.sp)
        if (meta.isNotBlank()) Text(meta, color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    detail: String,
    action: String,
    actionColor: Color,
    palette: HomePalette,
    onClick: () -> Unit,
    last: Boolean = false
) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 10.dp)) {
                Text(title, color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 15.5.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(detail, Modifier.padding(top = 3.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(action, color = actionColor, fontFamily = SortedHomeFontFamily, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            Text("  ›", color = palette.muted, fontSize = 16.sp)
        }
        if (!last) Spacer(Modifier.fillMaxWidth().height(1.dp).background(palette.faintRule))
    }
}

@Composable
private fun SettingsLockGlyph(color: Color) {
    Canvas(Modifier.size(18.dp)) {
        val stroke = 1.5.dp.toPx()
        drawRoundRect(color, topLeft = Offset(size.width * .2f, size.height * .43f), size = Size(size.width * .6f, size.height * .5f), cornerRadius = CornerRadius(size.width * .12f), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        drawArc(color, 180f, 180f, false, topLeft = Offset(size.width * .32f, size.height * .07f), size = Size(size.width * .36f, size.height * .54f), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
    }
}

@Composable
private fun ThemeSettingsCard(
    selected: AppThemeMode,
    palette: TapePalette,
    onSelected: (AppThemeMode) -> Unit
) {
    TapeLedgerBlock(
        heading = "Appearance",
        meta = selected.label,
        palette = palette
    ) {
        Text(
            text = "SCREEN INK MODE IS STORED ON THIS DEVICE.",
            color = palette.inkFaint,
            fontFamily = SortedTapeFontFamily,
            fontSize = 8.sp,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(9.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppThemeMode.entries.forEach { mode ->
                ThemeModeTile(
                    mode = mode,
                    selected = mode == selected,
                    palette = palette,
                    onClick = { onSelected(mode) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ThemeModeTile(
    mode: AppThemeMode,
    selected: Boolean,
    palette: TapePalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) palette.amber else palette.rule,
        label = "theme_tile_container"
    )
    Column(
        modifier = modifier
            .height(76.dp)
            .border(1.dp, borderColor, RoundedCornerShape(2.dp))
            .background(if (selected) palette.amber.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(9.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(4.dp)
                .background(mode.swatchColor())
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = mode.label.uppercase(Locale.US),
            color = if (selected) palette.amber else palette.ink,
            fontFamily = SortedTapeFontFamily,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 1.sp
        )
        Text(
            text = mode.description.uppercase(Locale.US),
            color = palette.inkFaint,
            fontFamily = SortedTapeFontFamily,
            fontSize = 7.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.7.sp
        )
    }
}

private fun AppThemeMode.swatchColor(): Color {
    return when (this) {
        AppThemeMode.System -> Color(0xFF9AA79A)
        AppThemeMode.Dark -> Color(0xFF000000)
        AppThemeMode.Light -> Color(0xFFF2C14E)
    }
}

@Composable
private fun LocalDataSettingsCard(feedState: FeedState, palette: TapePalette) {
    val sourceCounts = feedState.transactions.groupingBy { it.source }.eachCount().toSortedMap()
    val monthBreakdown = feedState.transactions.monthBreakdown()
    val reviewCount = feedState.transactions.reviewCandidates().size
    val sourceSummary = if (sourceCounts.isEmpty()) {
        "None"
    } else {
        sourceCounts.entries.joinToString(" / ") { (source, count) -> "$source $count" }
    }

    TapeLedgerBlock(
        heading = "Local tape",
        meta = "on device",
        palette = palette
    ) {
        IndexEntryRow("TRANSACTIONS", "ALL SOURCES", feedState.transactions.size.toString(), palette)
        IndexEntryRow("CURRENT MONTH", "ACTIVE TAPE", monthBreakdown.monthKey ?: "UNKNOWN", palette)
        IndexEntryRow("SPENT THIS MONTH", "${monthBreakdown.spendCount} PAYMENTS", monthBreakdown.spends.formatRupee(), palette)
        IndexEntryRow("NEED REVIEW", "PAYMENTS", reviewCount.toString(), palette, query = reviewCount > 0)
        IndexBlockFoot("SOURCES", sourceSummary.uppercase(Locale.US), palette)
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
    gmailState: GmailUiState,
    palette: TapePalette
) {
    val gmailLabel = if (gmailState.error != null) "Needs attention" else gmailState.label
    val autoSyncLabel = gmailState.autoSyncLabel
        ?.removePrefix("Auto sync: ")
        ?.replaceFirstChar { it.titlecase(Locale.getDefault()) }
        ?: "Manual"

    TapeLedgerBlock(
        heading = "Sources",
        meta = "permissions",
        palette = palette
    ) {
        SettingsInfoRow("SMS", if (feedState.needsSmsPermission) "PERMISSION NEEDED" else "ENABLED", palette)
        SettingsInfoRow("GMAIL", gmailLabel.uppercase(Locale.US), palette)
        SettingsInfoRow("AUTO SYNC", autoSyncLabel.uppercase(Locale.US), palette)
        SettingsInfoRow("STORAGE", "LOCAL ONLY", palette)
    }
}

@Composable
private fun RuleCenterEntryCard(onOpenRuleCenter: () -> Unit, palette: TapePalette) {
    TapeLedgerBlock(
        heading = "Rules",
        meta = "saved stamps",
        palette = palette,
        modifier = Modifier.clickable(onClick = onOpenRuleCenter)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "OPEN RULE LEDGER",
                    color = palette.ink,
                    fontFamily = SortedTapeFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "LEARNED MERCHANT CORRECTIONS AND CATEGORY OVERRIDES",
                    color = palette.inkFaint,
                    fontFamily = SortedTapeFontFamily,
                    fontSize = 8.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 0.8.sp
                )
            }
            Text(
                text = ">",
                color = palette.amber,
                fontFamily = SortedTapeFontFamily,
                fontSize = 18.sp,
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
    val palette = homePalette()
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

    Column(modifier.fillMaxSize().background(palette.background)) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).background(palette.header)
                .padding(start = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.ink)
                }
            } else {
                IconButton(onClick = onSettings, modifier = Modifier.size(40.dp)) {
                    HomeSettingsSlidersGlyph(color = palette.ink, modifier = Modifier.size(19.dp))
                }
            }
            Text(
                "Auto-sorting rules",
                Modifier.weight(1f).padding(start = 4.dp),
                color = palette.ink,
                fontFamily = SortedHomeFontFamily,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(palette.rule))
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                "Rules from your corrections sort new payments.",
                color = palette.muted,
                fontFamily = SortedHomeFontFamily,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            if (!isLoading) {
                Text(
                    "${rules.size} ${if (rules.size == 1) "rule" else "rules"} · ${rules.count { it.enabled }} on",
                    Modifier.padding(top = 7.dp),
                    color = palette.ink,
                    fontFamily = SortedHomeFontFamily,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        when {
            isLoading -> Text(
                "Loading rules…",
                Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                color = palette.muted,
                fontFamily = SortedHomeFontFamily,
                fontSize = 14.sp
            )
            rules.isEmpty() -> Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Text("No rules yet", color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Save a correction as a rule and Sorted will use it for new payments.",
                    Modifier.padding(top = 6.dp),
                    color = palette.muted,
                    fontFamily = SortedHomeFontFamily,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
            else -> LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                items(rules, key = { it.id }) { rule ->
                    RuleRow(
                        rule = rule,
                        palette = palette,
                        onToggle = { enabled ->
                            scope.launch {
                                val updated = withContext(Dispatchers.IO) {
                                    TransactionRepository(appContext)
                                        .setCategoryRuleEnabled(rule.id, enabled = enabled)
                                }
                                message = if (updated) {
                                    if (enabled) "Rule turned on" else "Rule paused"
                                } else {
                                    "Rule could not be updated"
                                }
                                reloadRules()
                            }
                        }
                    )
                }
            }
        }
        message?.let { status ->
            Text(
                status,
                Modifier.fillMaxWidth().background(palette.band).padding(horizontal = 20.dp, vertical = 10.dp),
                color = if (status.contains("could not", ignoreCase = true)) palette.review else palette.muted,
                fontFamily = SortedHomeFontFamily,
                fontSize = 12.5.sp
            )
        }
    }
}

@Composable
private fun RuleRow(
    rule: CategoryRuleEntity,
    palette: HomePalette,
    onToggle: (Boolean) -> Unit
) {
    val title = rule.merchantNormalized?.takeIf(String::isNotBlank) ?: rule.pattern
    val matchDetail = when (rule.matchType.lowercase(Locale.US)) {
        "exact" -> "Exact payment name"
        "contains" -> "Payment name contains \"${rule.pattern}\""
        else -> "Matches \"${rule.pattern}\""
    }
    val sortedAs = listOfNotNull(
        rule.departmentCategory?.takeIf(String::isNotBlank),
        rule.miscCategory?.takeIf(String::isNotBlank),
        rule.transactionType.displayName()
    ).distinct().joinToString(" · ")

    Column(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(title, color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(matchDetail, Modifier.padding(top = 3.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("Sorts as $sortedAs", Modifier.padding(top = 4.dp), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 12.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Switch(
                checked = rule.enabled,
                onCheckedChange = onToggle,
                modifier = Modifier.semantics {
                    contentDescription = "${if (rule.enabled) "Pause" else "Turn on"} rule for $title"
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = palette.background,
                    checkedTrackColor = palette.categoryTwo,
                    uncheckedThumbColor = palette.muted,
                    uncheckedTrackColor = palette.softFill,
                    uncheckedBorderColor = palette.rule
                )
            )
        }
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(palette.faintRule))
    }
}

@Composable
private fun SourceHealthMiniCard(sourceRows: List<SourceHealthRow>, palette: TapePalette) {
    TapeLedgerBlock(
        heading = "Source coverage",
        meta = "on device",
        palette = palette
    ) {
        if (sourceRows.isEmpty()) {
            IndexEmptyLine("NO PAYMENTS IMPORTED YET", palette)
        } else {
            sourceRows.forEach { row ->
                SourceHealthInlineRow(row = row, palette = palette)
            }
            IndexBlockFoot("${sourceRows.sumOf { it.totalCount }} MESSAGES READ", "0 UPLOADED", palette)
        }
    }
}

@Composable
private fun SettingsInfoRow(
    label: String,
    value: String,
    palette: TapePalette
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val y = size.height - 1.dp.toPx()
                drawLine(
                    color = palette.ruleFaint,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label.uppercase(Locale.US),
            modifier = Modifier.width(96.dp),
            color = palette.inkFaint,
            fontFamily = SortedTapeFontFamily,
            fontSize = 8.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 1.sp
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            color = palette.ink,
            fontFamily = SortedTapeFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Start,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun SourceHealthCard(
    feedState: FeedState,
    gmailState: GmailUiState,
    palette: TapePalette
) {
    val sourceRows = feedState.transactions.sourceHealthRows()
    val reviewCount = feedState.transactions.reviewCandidates().size
    val gmailRows = feedState.transactions.latestMonthTransactions().count { it.source == "Gmail" }
    val fxRows = feedState.transactions.latestMonthTransactions().count {
        it.direction == DirectionUi.Debit && it.amountValue > 0.0 && it.inrAmountValue == null
    }
    val gmailLabel = if (gmailState.error != null) "Needs attention" else gmailState.label

    TapeLedgerBlock(
        heading = "Source health",
        meta = "receipt",
        palette = palette
    ) {
        TapeSummationRow("NEED REVIEW", reviewCount.toString(), palette, if (reviewCount > 0) palette.query else palette.ink)
        TapeSummationRow("GMAIL PAYMENTS", gmailRows.toString(), palette)
        TapeSummationRow("FX NEEDS CONVERSION", fxRows.toString(), palette)
        SettingsInfoRow("SMS", if (feedState.needsSmsPermission) "PERMISSION NEEDED" else "ENABLED", palette)
        SettingsInfoRow("GMAIL", gmailLabel.uppercase(Locale.US), palette)
        SettingsInfoRow("STORAGE", "LOCAL ONLY", palette)
        if (sourceRows.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            sourceRows.forEach { row ->
                SourceHealthInlineRow(row = row, palette = palette)
            }
        }
    }
}

@Composable
private fun SourceHealthInlineRow(row: SourceHealthRow, palette: TapePalette) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val y = size.height - 1.dp.toPx()
                drawLine(
                    color = palette.ruleFaint,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#",
            modifier = Modifier.width(15.dp),
            color = palette.inkFaint,
            fontFamily = SortedTapeFontFamily,
            fontSize = 9.sp,
            textAlign = TextAlign.Center
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.source.uppercase(Locale.US),
                color = palette.ink,
                fontFamily = SortedTapeFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 1.sp
            )
            Text(
                text = "${row.spendCount} SPEND - ${row.reviewCount} REVIEW - ${row.fxCount} FX",
                color = palette.inkFaint,
                fontFamily = SortedTapeFontFamily,
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.8.sp
            )
        }
        Text(
            text = "${row.totalCount}",
            color = if (row.reviewCount > 0) palette.query else palette.amber,
            fontFamily = SortedTapeFontFamily,
            fontSize = 10.sp,
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
    palette: TapePalette,
    onImport: () -> Unit
) {
    TapeLedgerBlock(
        heading = "Gmail import",
        meta = if (state.isImporting) "reading" else "manual",
        palette = palette,
        modifier = Modifier
            .clickable(enabled = !state.isImporting, onClick = onImport)
    ) {
        SettingsInfoRow("STATUS", (state.error ?: state.label).uppercase(Locale.US), palette)
        state.autoSyncLabel?.let { label ->
            SettingsInfoRow("AUTO SYNC", label.uppercase(Locale.US), palette)
        }
        if (state.error != null) {
            Text(
                text = "OAUTH SETUP: PACKAGE ${setupInfo.packageName}, SHA-1 ${setupInfo.signingSha1 ?: "UNAVAILABLE"}",
                modifier = Modifier.padding(top = 7.dp),
                color = palette.query,
                fontFamily = SortedTapeFontFamily,
                fontSize = 8.sp,
                lineHeight = 12.sp,
                letterSpacing = 0.8.sp
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        TapeActionText(
            label = if (state.isImporting) "Reading source" else "Import Gmail",
            palette = palette,
            enabled = !state.isImporting,
            onClick = onImport
        )
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
    hasReview: Boolean,
    highlightSelection: Boolean = true,
    reviewSelected: Boolean = false,
    onTabSelected: (SortedTab) -> Unit,
    onOpenSync: () -> Unit,
    onOpenReview: () -> Unit
) {
    val palette = homePalette()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = palette.nav,
        tonalElevation = 0.dp
    ) {
        ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(fontFamily = SortedHomeDesignFontFamily)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HomeNavPerforation(palette = palette)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HomeNavTextCell(
                        label = "Home",
                        selected = highlightSelection && selectedTab == SortedTab.Home,
                        palette = palette,
                        onClick = { onTabSelected(SortedTab.Home) }
                    )
                    HomeNavTextCell(
                        label = "Insights",
                        selected = highlightSelection && selectedTab == SortedTab.Insights,
                        palette = palette,
                        onClick = { onTabSelected(SortedTab.Insights) }
                    )
                    HomeNavSyncCell(palette = palette, onClick = onOpenSync)
                    HomeNavTextCell(
                        label = "Review",
                        selected = reviewSelected,
                        palette = palette,
                        showDot = hasReview && !reviewSelected,
                        onClick = onOpenReview
                    )
                    HomeNavTextCell(
                        label = "Add",
                        selected = highlightSelection && selectedTab == SortedTab.Capture,
                        palette = palette,
                        onClick = { onTabSelected(SortedTab.Capture) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeNavPerforation(palette: HomePalette) {
    val dotAlpha = if (isDarkModeActive()) 0.18f else 0.16f
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(7.dp)
            .background(palette.nav)
    ) {
        val radius = 1.5.dp.toPx()
        val step = 10.dp.toPx()
        var x = 5.dp.toPx()
        while (x < size.width) {
            drawCircle(
                color = palette.ink.copy(alpha = dotAlpha),
                radius = radius,
                center = Offset(x, 4.dp.toPx())
            )
            x += step
        }
    }
}

@Composable
private fun RowScope.HomeNavTextCell(
    label: String,
    selected: Boolean,
    palette: HomePalette,
    showDot: Boolean = false,
    onClick: () -> Unit
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) palette.ink else palette.muted,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "home_nav_$label"
    )
    Column(
        modifier = Modifier
            .weight(1f)
            .height(62.dp)
            .drawBehind {
                drawLine(
                    color = palette.faintRule,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .clickable(onClick = onClick)
            .padding(top = 10.dp, bottom = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.height(9.dp), contentAlignment = Alignment.TopCenter) {
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (selected) palette.ink else Color.Transparent)
            )
            if (showDot) {
                Box(
                    modifier = Modifier
                        .offset(x = 20.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(palette.review)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 11.5.sp,
            fontWeight = SortedHomeWeight,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RowScope.HomeNavSyncCell(
    palette: HomePalette,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .height(62.dp)
            .drawBehind {
                drawLine(
                    color = palette.faintRule,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .clickable(onClick = onClick)
            .padding(top = 7.dp, bottom = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(palette.softFill)
                .border(1.dp, palette.rule, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            SortedNavGlyph(
                icon = SortedNavIcon.Sync,
                color = palette.ink,
                active = false,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = "Sync",
            color = palette.muted,
            fontSize = 11.5.sp,
            fontWeight = SortedHomeWeight,
            maxLines = 1
        )
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
        val palette = homePalette()
        val shape = RoundedCornerShape(4.dp)
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .clip(shape)
                .background(palette.background, shape)
                .border(1.dp, palette.rule, shape)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text("Imports", color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("Choose where to look for payments", Modifier.padding(top = 2.dp, bottom = 6.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 12.sp)
            SyncSourceRow("SMS", "Read payment alerts", palette) { onSync(SyncSource.Sms) }
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(palette.faintRule))
            SyncSourceRow("Gmail", "Import payment emails", palette) { onSync(SyncSource.Gmail) }
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
        val palette = homePalette()
        val status = message.orEmpty()
        val needsAction = listOf("failed", "needed", "missing", "cancelled", "paused")
            .any { status.contains(it, ignoreCase = true) }
        Row(
            Modifier.widthIn(max = 360.dp).clip(RoundedCornerShape(4.dp)).background(palette.band)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(if (needsAction) palette.review else palette.categoryTwo))
            Text(
                text = status,
                color = if (needsAction) palette.review else palette.ink,
                fontFamily = SortedHomeFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SyncSourceRow(
    label: String,
    detail: String,
    palette: HomePalette,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(vertical = 7.dp)) {
            Text(label, color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
            Text(detail, Modifier.padding(top = 2.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.5.sp)
        }
        Text("›", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 20.sp)
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
                        text = breakdown.monthKey?.monthSpendLabel() ?: "Spent this month",
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
                        text = "${breakdown.spendCount} payments",
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
                    label = "Investments included",
                    value = breakdown.investments.formatInr(),
                    accent = categoryColor("Food"),
                    modifier = Modifier.weight(1f)
                )
                HomeMetricTile(
                    label = "Transfers included",
                    value = breakdown.transfers.formatInr(),
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
                    label = "Average payment",
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
    val debitTransactions = monthTransactions.filter { it.countsTowardSpentTotal() }
    val spendTransactions = debitTransactions
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
    val creditTransactions = monthTransactions.filter { it.countsTowardMoneyIn() }
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
        creditCount = creditTransactions.size,
        totalCredits = creditTransactions.sumOf { it.inrAmountValue ?: 0.0 },
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
        it.countsTowardSpentTotal() &&
            it.isInSelectedMonth(monthKey)
    }
}

private fun List<TransactionUi>.latestMonthMoneyInTransactions(monthKey: String? = selectedMonthKey()): List<TransactionUi> {
    return filter { it.countsTowardMoneyIn() && it.isInSelectedMonth(monthKey) }
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

private fun List<TransactionUi>.monthMerchantGroups(): List<SummaryGroup> {
    return latestMonthSpendTransactions()
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
    return latestMonthSpendTransactions()
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
        .filter {
            it.status in setOf(TransactionStatus.COMPLETED, TransactionStatus.PENDING, TransactionStatus.UNKNOWN) &&
                (it.inrAmountValue != null || (it.direction == DirectionUi.Debit && it.amountValue > 0.0))
        }
        .filter(TransactionUi::needsReview)
        .sortedByDescending { it.inrAmountValue ?: it.amountValue }
}

private fun TransactionUi.needsReview(): Boolean {
    if (status != TransactionStatus.COMPLETED) return true
    if (direction == DirectionUi.Debit && amountValue > 0.0 && inrAmountValue == null) return true
    if (categorySource == CategorySource.USER_RULE) return false

    val amount = inrAmountValue ?: 0.0
    return category == "Other" ||
        miscCategory == "Uncategorized" ||
        categorySource == CategorySource.FALLBACK ||
        confidence < 0.70 ||
        merchant.looksLikeRawPaymentHandle() ||
        (source == "Gmail" && amount >= 10_000.0)
}

private fun TransactionUi.reviewReason(): String {
    val amount = inrAmountValue ?: 0.0
    return when {
        status != TransactionStatus.COMPLETED -> "Payment status needs checking"
        direction == DirectionUi.Debit && amountValue > 0.0 && inrAmountValue == null -> "Currency conversion needed"
        category == "Other" -> "Category needs sorting"
        miscCategory == "Uncategorized" -> "Merchant tag missing"
        categorySource == CategorySource.FALLBACK -> "Fallback categorization"
        confidence < 0.70 -> "Low parser confidence"
        merchant.looksLikeRawPaymentHandle() -> "Merchant needs cleanup"
        source == "Gmail" && amount >= 10_000.0 -> "High-value Gmail payment"
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
        it.countsTowardSpentTotal() &&
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
                spendCount = rows.count(TransactionUi::countsTowardSpentTotal),
                reviewCount = rows.count(TransactionUi::needsReview),
                fxCount = rows.count { it.direction == DirectionUi.Debit && it.amountValue > 0.0 && it.inrAmountValue == null },
                totalAmount = rows
                    .filter(TransactionUi::countsTowardSpentTotal)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MerchantCategoryDetailScreen(
    state: DrilldownState,
    allTransactions: List<TransactionUi>,
    onBack: () -> Unit,
    onTransactionClick: (TransactionUi) -> Unit
) {
    val palette = homePalette()
    val groupTransactions = remember(state, allTransactions) {
        allTransactions.filter { transaction ->
            transaction.inrAmountValue != null && when (state.kind) {
                DrilldownKind.Merchant -> transaction.merchant == state.group.label
                DrilldownKind.Category -> transaction.category == state.group.label
            }
        }
    }
    val availableMonths = remember(groupTransactions, state.monthKeys) {
        groupTransactions.mapNotNull { it.transactionDate?.take(7) }
            .filter { state.monthKeys == null || it in state.monthKeys }
            .distinct()
            .sortedDescending()
    }
    var selectedMonth by remember(state, availableMonths) {
        mutableStateOf(state.monthKey?.takeIf { it in availableMonths } ?: availableMonths.firstOrNull())
    }
    var monthSheetOpen by remember { mutableStateOf(false) }
    var paymentSheetTitle by remember { mutableStateOf<String?>(null) }
    var paymentSheetSubtitle by remember { mutableStateOf<String?>(null) }
    var paymentSheetRows by remember { mutableStateOf(emptyList<TransactionUi>()) }
    val monthLabel = selectedMonth?.monthNameLabel() ?: "This month"
    val breakdownTitle = if (state.kind == DrilldownKind.Merchant) "Categories" else "Top merchants"

    Scaffold(containerColor = palette.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(palette.background)) {
            Row(
                Modifier.fillMaxWidth()
                    .background(if (isDarkModeActive()) Color(0xFF0F2C28) else palette.header)
                    .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.ink)
                }
                Column(Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(state.title, color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (state.kind == DrilldownKind.Merchant) "Merchant" else "Category", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.5.sp)
                }
                Surface(
                    modifier = Modifier.clickable { monthSheetOpen = true },
                    color = if (isDarkModeActive()) Color(0x217FB3A4) else Color(0x214E8471),
                    shape = RoundedCornerShape(50)
                ) {
                    Row(Modifier.padding(horizontal = 11.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(monthLabel, color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        Text("⌄", Modifier.padding(start = 4.dp), color = palette.muted, fontSize = 14.sp)
                    }
                }
            }
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(palette.rule))

            Crossfade(
                targetState = selectedMonth,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                modifier = Modifier.fillMaxWidth().weight(1f),
                label = "merchant_category_month"
            ) { shownMonth ->
                val rows = remember(groupTransactions, shownMonth) {
                    groupTransactions.filter { it.transactionDate?.take(7) == shownMonth }
                        .sortedWith(compareByDescending<TransactionUi> { it.transactionDate.orEmpty() }.thenByDescending { it.inrAmountValue ?: 0.0 })
                }
                val spend = remember(rows) { rows.filter { it.countsTowardSpentTotal() } }
                val notCounted = remember(rows) { rows.filter { it.countsTowardMoneyIn() } }
                val total = spend.sumOf { it.inrAmountValue ?: 0.0 }
                val reviews = spend.filter(TransactionUi::needsReview)
                val notCountedTotal = notCounted.sumOf { it.inrAmountValue ?: 0.0 }
                val groups = remember(spend, state.kind) {
                    spend.groupBy { row ->
                        if (state.kind == DrilldownKind.Merchant) row.category.ifBlank { "Other" }
                        else row.merchant.ifBlank { "Unknown" }
                    }.map { (name, matching) ->
                        DrilldownBreakdown(name, matching, matching.sumOf { it.inrAmountValue ?: 0.0 })
                    }.sortedByDescending { it.amount }
                }
                val methods = remember(spend) {
                    spend.groupBy { it.paymentMode.ifBlank { "Not available" } }
                        .map { (name, matching) -> DrilldownBreakdown(name, matching, matching.sumOf { it.inrAmountValue ?: 0.0 }) }
                        .sortedByDescending { it.amount }
                }
                val days = remember(spend) {
                    spend.groupBy { it.transactionDate?.recentDateLabel()?.takeIf(String::isNotBlank) ?: "Date unavailable" }
                }
                val dayTotals = remember(days) { days.mapValues { (_, matching) -> matching.sumOf { it.inrAmountValue ?: 0.0 } } }
                val shownMonthLabel = shownMonth?.monthNameLabel() ?: "This month"

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
                ) {
                    item(key = "detail-hero") {
                        Column(
                            Modifier.fillMaxWidth().clickable(enabled = spend.isNotEmpty()) {
                                paymentSheetTitle = "Payments"
                                paymentSheetSubtitle = "${spend.size} payments · ${total.formatHomeRupee()}"
                                paymentSheetRows = spend
                            }.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 0.dp)
                        ) {
                            Text("Spent in $shownMonthLabel", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Row(Modifier.padding(top = 5.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(total.formatHomeRupee(), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 48.sp, lineHeight = 48.sp, fontWeight = FontWeight.SemiBold)
                                Row(
                                    Modifier.padding(bottom = 7.dp).clickable(enabled = spend.isNotEmpty()) {
                                        paymentSheetTitle = "Payments"
                                        paymentSheetSubtitle = "${spend.size} payments · ${total.formatHomeRupee()}"
                                        paymentSheetRows = spend
                                    },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text("${spend.size} payments", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text("›", color = palette.muted, fontSize = 16.sp)
                                }
                            }
                            Text(
                                "All outgoing payments, grouped by category.",
                                Modifier.padding(top = 9.dp),
                                color = palette.muted,
                                fontFamily = SortedHomeFontFamily,
                                fontSize = 12.5.sp
                            )
                        }
                    }

                    if (reviews.isNotEmpty()) {
                        item(key = "detail-review") {
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(tween(180)) + expandVertically(tween(220, easing = FastOutSlowInEasing)),
                                exit = fadeOut(tween(160)) + shrinkVertically(tween(180, easing = FastOutSlowInEasing))
                            ) {
                                Text(
                                    "${reviews.size} NEED REVIEW",
                                    Modifier.padding(start = 20.dp, top = 16.dp).graphicsLayer(rotationZ = -1.5f)
                                        .dottedOutline(palette.review)
                                        .clickable {
                                            paymentSheetTitle = "Need review"
                                            paymentSheetSubtitle = "${reviews.size} payments · ${reviews.sumOf { it.inrAmountValue ?: 0.0 }.formatHomeRupee()}"
                                            paymentSheetRows = reviews
                                        }
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                    color = palette.review,
                                    fontFamily = SortedHomeFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.1.sp
                                )
                            }
                        }
                    }

                    if (groups.isNotEmpty()) {
                        item(key = "detail-breakdown") {
                            Column(Modifier.fillMaxWidth().padding(top = 20.dp).background(palette.band).drawBehind {
                                drawLine(palette.rule, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
                                drawLine(palette.rule, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                            }.padding(horizontal = 20.dp, vertical = 16.dp)) {
                                DrilldownSectionHeading(breakdownTitle, "", palette, topPadding = 0.dp)
                                groups.forEach { group ->
                                    DrilldownShareRow(
                                        group = group,
                                        total = total,
                                        palette = palette,
                                        onClick = {
                                            paymentSheetTitle = group.name
                                            paymentSheetSubtitle = "${group.rows.size} payments · ${group.amount.formatHomeRupee()}"
                                            paymentSheetRows = group.rows
                                        }
                                    )
                                }
                                if (state.kind == DrilldownKind.Merchant && methods.isNotEmpty()) {
                                    Spacer(Modifier.fillMaxWidth().padding(top = 2.dp).height(1.dp).background(palette.rule))
                                    DrilldownSectionHeading("Paid with", "", palette, topPadding = 14.dp)
                                    methods.forEach { method ->
                                        DrilldownSecondaryRow(method.name, method.amount, method.rows.size, palette) {
                                            paymentSheetTitle = method.name
                                            paymentSheetSubtitle = "${method.rows.size} payments · ${method.amount.formatHomeRupee()}"
                                            paymentSheetRows = method.rows
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (spend.isNotEmpty()) {
                        item(key = "detail-payments-heading") {
                            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("PAYMENTS", Modifier.weight(1f), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.76.sp)
                                    Text("All outgoing", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        days.forEach { (label, dayRows) ->
                            item(key = "detail-day-$label") {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 20.dp).drawBehind {
                                        drawLine(palette.rule, Offset(0f, size.height - 1.dp.toPx()), Offset(size.width, size.height - 1.dp.toPx()), 1.dp.toPx())
                                    }.clickable {
                                        paymentSheetTitle = label
                                        paymentSheetSubtitle = "${dayRows.size} payments · ${dayTotals[label]?.formatHomeRupee().orEmpty()}"
                                        paymentSheetRows = dayRows
                                    }.padding(top = 10.dp, bottom = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(label, Modifier.weight(1f), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(dayTotals[label]?.formatHomeRupee().orEmpty(), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                            items(dayRows, key = { "detail-${it.sourceHash}" }) { transaction ->
                                DrilldownTransactionRow(transaction, palette) { onTransactionClick(transaction) }
                            }
                        }
                    } else {
                        item(key = "detail-empty") {
                            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 34.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    if (state.kind == DrilldownKind.Merchant) "No spending here this month" else "No spending in this category",
                                    color = palette.ink,
                                    fontFamily = SortedHomeFontFamily,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    if (state.kind == DrilldownKind.Merchant) "Payments from this merchant will show here." else "Payments in this category will show here.",
                                    Modifier.padding(top = 6.dp),
                                    color = palette.muted,
                                    fontFamily = SortedHomeFontFamily,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                if (availableMonths.any { it != shownMonth }) {
                                    TextButton(onClick = { monthSheetOpen = true }, modifier = Modifier.padding(top = 8.dp)) {
                                        Text("Look at another month", color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }

                    if (notCounted.isNotEmpty()) {
                        item(key = "detail-not-counted") {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)
                                    .drawBehind { drawLine(palette.faintRule, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
                                    .clickable {
                                        paymentSheetTitle = "Money in"
                                        paymentSheetSubtitle = "${notCounted.size} payments · ${notCounted.sumOf { it.inrAmountValue ?: 0.0 }.formatHomeRupee()}"
                                        paymentSheetRows = notCounted
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("Money in", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text("Refunds, rewards and income", Modifier.padding(top = 2.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.5.sp)
                                }
                                Text(notCountedTotal.formatHomeRupee(), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("›", Modifier.padding(start = 8.dp), color = palette.muted, fontSize = 18.sp)
                            }
                        }
                    }

                    item(key = "detail-footer") {
                        Text("Stays on this phone", Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.5.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }

    if (monthSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { monthSheetOpen = false },
            containerColor = palette.background,
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Choose a month", color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Text(state.title, Modifier.padding(top = 3.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 12.5.sp)
                    }
                    TextButton(onClick = { monthSheetOpen = false }) { Text("Close", color = palette.muted) }
                }
                availableMonths.forEach { month ->
                    Row(
                        Modifier.fillMaxWidth().clickable { selectedMonth = month; monthSheetOpen = false }
                            .padding(vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(month.monthNameLabel(), Modifier.weight(1f), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 15.sp, fontWeight = if (month == selectedMonth) FontWeight.SemiBold else FontWeight.Medium)
                        Text("${groupTransactions.count { it.transactionDate?.take(7) == month }} payments", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 12.sp)
                        if (month == selectedMonth) Text("  ✓", color = palette.credit, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (month != availableMonths.last()) Spacer(Modifier.fillMaxWidth().height(1.dp).background(palette.faintRule))
                }
            }
        }
    } else if (paymentSheetTitle != null) {
        ModalBottomSheet(
            onDismissRequest = { paymentSheetTitle = null },
            containerColor = palette.background,
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(paymentSheetTitle.orEmpty(), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Text(paymentSheetSubtitle.orEmpty(), Modifier.padding(top = 3.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 12.5.sp)
                    }
                    TextButton(onClick = { paymentSheetTitle = null }) { Text("Close", color = palette.muted) }
                }
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 540.dp).padding(top = 8.dp)) {
                    items(paymentSheetRows, key = { "sheet-${it.sourceHash}" }) { transaction ->
                        DrilldownTransactionRow(transaction, palette) { onTransactionClick(transaction) }
                    }
                }
            }
        }
    }
}

private data class DrilldownBreakdown(
    val name: String,
    val rows: List<TransactionUi>,
    val amount: Double
)

@Composable
private fun DrilldownSectionHeading(title: String, meta: String, palette: HomePalette, topPadding: androidx.compose.ui.unit.Dp = 16.dp) {
    Row(Modifier.fillMaxWidth().padding(top = topPadding, bottom = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(Locale.US), Modifier.weight(1f), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.76.sp)
        if (meta.isNotBlank()) Text(meta, color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DrilldownShareRow(group: DrilldownBreakdown, total: Double, palette: HomePalette, onClick: () -> Unit) {
    val share = if (total > 0.0) (group.amount / total).toFloat().coerceIn(0f, 1f) else 0f
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 11.dp).animateContentSize(tween(220))) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(group.name, Modifier.weight(1f), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 14.5.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(group.amount.formatHomeRupee(), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
            Text("${(share * 100).toInt()}%", Modifier.width(36.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
        }
        Row(Modifier.padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(2.dp)).background(palette.softFill)) {
                Box(Modifier.fillMaxWidth(share).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(palette.ink))
            }
            Text("${group.rows.size}", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun DrilldownSecondaryRow(name: String, amount: Double, count: Int, palette: HomePalette, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, Modifier.weight(1f), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 14.5.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(amount.formatHomeRupee(), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
        Text("$count", Modifier.width(60.dp).padding(start = 8.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.5.sp, textAlign = TextAlign.End)
        Text("›", Modifier.padding(start = 8.dp), color = palette.muted, fontSize = 16.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrilldownScreen(
    state: DrilldownState,
    allTransactions: List<TransactionUi>,
    onBack: () -> Unit,
    onTransactionClick: (TransactionUi) -> Unit,
    onOpenGroup: (DrilldownKind, SummaryGroup) -> Unit
) {
    val palette = homePalette()
    val baseTransactions = remember(state, allTransactions) {
        allTransactions.filter { transaction ->
            transaction.inrAmountValue != null &&
                (if (state.spendOnly) {
                    transaction.countsTowardSpentTotal()
                } else {
                    transaction.direction == DirectionUi.Debit
                }) &&
                when (state.kind) {
                    DrilldownKind.Merchant -> transaction.merchant == state.group.label
                    DrilldownKind.Category -> transaction.category == state.group.label
                } &&
                (state.monthKeys == null || transaction.transactionDate?.take(7) in state.monthKeys)
        }.sortedWith(
            compareByDescending<TransactionUi> { it.transactionDate.orEmpty() }
                .thenByDescending { it.inrAmountValue ?: 0.0 }
        )
    }
    val availableMonths = remember(baseTransactions) {
        baseTransactions.mapNotNull { it.transactionDate?.take(7) }.distinct().sortedDescending()
    }
    var selectedMonth by remember(state) {
        mutableStateOf(state.monthKey?.takeIf { it in availableMonths } ?: availableMonths.firstOrNull())
    }
    var showAllBreakdown by remember(state) { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<Pair<String, List<TransactionUi>>?>(null) }
    val monthIsRange = state.monthKeys != null && state.monthKeys.size > 1
    val monthLabel = when {
        monthIsRange -> "This year"
        selectedMonth != null -> selectedMonth!!.monthNameLabel()
        else -> "This month"
    }
    val transactions = remember(baseTransactions, selectedMonth, monthIsRange) {
        if (monthIsRange || selectedMonth == null) baseTransactions
        else baseTransactions.filter { it.transactionDate?.take(7) == selectedMonth }
    }
    val total = remember(transactions) { transactions.sumOf { it.inrAmountValue ?: 0.0 } }
    val reviewRows = remember(transactions) { transactions.filter(TransactionUi::needsReview) }
    val breakdownGroups = remember(transactions, state.kind) {
        transactions.groupBy { transaction ->
            if (state.kind == DrilldownKind.Merchant) transaction.category.ifBlank { "Other" }
            else transaction.merchant.ifBlank { "Unknown" }
        }.map { (label, rows) ->
            SummaryGroup(
                label = label,
                count = rows.size,
                total = rows.sumOf { it.inrAmountValue ?: 0.0 },
                currency = "INR",
                category = if (state.kind == DrilldownKind.Merchant) label else rows.firstOrNull()?.category ?: "Other"
            )
        }.sortedByDescending { it.total }
    }
    val paymentModes = remember(transactions) {
        transactions.groupBy { it.paymentMode.ifBlank { "Not available" } }
            .map { (label, rows) ->
                label to rows.sortedByDescending { it.transactionDate.orEmpty() }
            }
            .sortedByDescending { it.second.size }
    }

    Scaffold(containerColor = palette.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(palette.background)) {
            Row(
                Modifier.fillMaxWidth().background(palette.header).padding(start = 8.dp, end = 20.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.ink)
                }
                Column(Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(
                        if (state.kind == DrilldownKind.Merchant) "Merchant" else "Category",
                        color = palette.muted,
                        fontFamily = SortedHomeFontFamily,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        state.title,
                        color = palette.ink,
                        fontFamily = SortedHomeFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(palette.rule))

            if (!monthIsRange && availableMonths.size > 1) {
                val monthIndex = availableMonths.indexOf(selectedMonth).coerceAtLeast(0)
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = { selectedMonth = availableMonths.getOrNull(monthIndex + 1) },
                        enabled = monthIndex < availableMonths.lastIndex,
                        modifier = Modifier.size(40.dp)
                    ) { MonthArrowGlyph(-1, if (monthIndex < availableMonths.lastIndex) palette.ink else palette.faintRule, Modifier.size(18.dp)) }
                    Text(monthLabel, Modifier.animateContentSize().padding(horizontal = 8.dp), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    IconButton(
                        onClick = { selectedMonth = availableMonths.getOrNull(monthIndex - 1) },
                        enabled = monthIndex > 0,
                        modifier = Modifier.size(40.dp)
                    ) { MonthArrowGlyph(1, if (monthIndex > 0) palette.ink else palette.faintRule, Modifier.size(18.dp)) }
                }
            } else {
                Text(
                    monthLabel,
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    color = palette.muted,
                    fontFamily = SortedHomeFontFamily,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Crossfade(
                targetState = selectedMonth ?: "all",
                animationSpec = tween(220, easing = FastOutSlowInEasing),
                label = "drilldown_month_content"
            ) { monthTransition ->
                val visibleRows = remember(baseTransactions, monthTransition, monthIsRange) {
                    if (monthIsRange || monthTransition == "all") baseTransactions
                    else baseTransactions.filter { it.transactionDate?.take(7) == monthTransition }
                }
                val visibleTotal = visibleRows.sumOf { it.inrAmountValue ?: 0.0 }
                val visibleReviewRows = visibleRows.filter(TransactionUi::needsReview)
                val visibleGroups = remember(visibleRows, state.kind) {
                    visibleRows.groupBy { row ->
                        if (state.kind == DrilldownKind.Merchant) row.category.ifBlank { "Other" }
                        else row.merchant.ifBlank { "Unknown" }
                    }.map { (label, rows) ->
                        SummaryGroup(label, rows.size, rows.sumOf { it.inrAmountValue ?: 0.0 }, "INR", if (state.kind == DrilldownKind.Merchant) label else rows.firstOrNull()?.category ?: "Other")
                    }.sortedByDescending { it.total }
                }
                val visiblePaymentModes = remember(visibleRows) {
                    visibleRows.groupBy { it.paymentMode.ifBlank { "Not available" } }
                        .map { (label, rows) -> label to rows.sortedByDescending { it.transactionDate.orEmpty() } }
                        .sortedByDescending { it.second.size }
                }
                val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
                ) {
                    item(key = "hero") {
                        Column(
                            Modifier.fillMaxWidth().clickable(enabled = visibleRows.isNotEmpty()) {
                                sheet = "${visibleRows.size} payments" to visibleRows
                            }.padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            Text(
                                if (state.spendOnly) {
                                    if (state.kind == DrilldownKind.Merchant) "Spent here" else "Spent in this category"
                                } else "Payments",
                                color = palette.muted,
                                fontFamily = SortedHomeFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Crossfade(visibleTotal, animationSpec = tween(180), label = "drilldown_total") { amount ->
                                Text(
                                    amount.formatHomeRupee(),
                                    Modifier.padding(top = 4.dp),
                                    color = palette.ink,
                                    fontFamily = SortedHomeFontFamily,
                                    fontSize = 44.sp,
                                    lineHeight = 48.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                "${visibleRows.size} ${if (visibleRows.size == 1) "payment" else "payments"}",
                                color = palette.muted,
                                fontFamily = SortedHomeFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (visibleReviewRows.isNotEmpty()) {
                        item(key = "review") {
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(tween(220)) + expandVertically(tween(240, easing = FastOutSlowInEasing))
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
                                        .clickable { sheet = "Need review" to visibleReviewRows }
                                        .dottedOutline(palette.review)
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.size(7.dp).background(palette.reviewDot, CircleShape))
                                    Text("${visibleReviewRows.size} need review", Modifier.weight(1f).padding(start = 9.dp), color = palette.review, fontFamily = SortedHomeFontFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(visibleReviewRows.sumOf { it.inrAmountValue ?: 0.0 }.formatHomeRupee(), color = palette.review, fontFamily = SortedHomeFontFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    if (visibleGroups.isNotEmpty()) {
                        item(key = "breakdown_title") {
                            DrilldownSectionHeading(
                                if (state.kind == DrilldownKind.Merchant) "Categories" else "Top merchants",
                                "${visibleGroups.size} ${if (state.kind == DrilldownKind.Merchant) "categories" else "places"}",
                                palette
                            )
                        }
                        val shownGroups = if (showAllBreakdown) visibleGroups else visibleGroups.take(4)
                        items(shownGroups, key = { "mix-${it.label}" }) { group ->
                            DrilldownMixRow(
                                group = group,
                                total = visibleTotal,
                                color = drilldownMixColor(visibleGroups.indexOf(group), palette),
                                palette = palette,
                                onClick = {
                                    val nextKind = if (state.kind == DrilldownKind.Merchant) DrilldownKind.Category else DrilldownKind.Merchant
                                    onOpenGroup(nextKind, group)
                                }
                            )
                        }
                        if (visibleGroups.size > 4) {
                            item(key = "show_all") {
                                TextButton(onClick = { showAllBreakdown = !showAllBreakdown }) {
                                    Text(if (showAllBreakdown) "Show less" else "Show all ${visibleGroups.size}", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    if (state.kind == DrilldownKind.Merchant && visiblePaymentModes.isNotEmpty()) {
                        item(key = "paid_with_heading") { DrilldownSectionHeading("Paid with", "${visiblePaymentModes.size} methods", palette) }
                        items(visiblePaymentModes, key = { "mode-${it.first}" }) { (mode, rows) ->
                            DrilldownPaymentModeRow(mode, rows, visibleTotal, palette) {
                                sheet = "$mode · ${rows.size} payments" to rows
                            }
                        }
                    }

                    item(key = "payments_heading") {
                        DrilldownSectionHeading("Payments", "${visibleRows.size}", palette)
                    }
                    if (visibleRows.isEmpty()) {
                        item(key = "empty") {
                            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)) {
                                Text("No payments in this period", color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                Text("Choose another month to see payments here.", Modifier.padding(top = 5.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 13.sp)
                            }
                        }
                    } else {
                        val dateGroups = visibleRows.groupBy { row ->
                            row.transactionDate?.recentDateLabel()?.takeIf(String::isNotBlank) ?: "Date unavailable"
                        }
                        dateGroups.forEach { (dateLabel, rows) ->
                            item(key = "date-$dateLabel") {
                                Row(
                                    Modifier.fillMaxWidth().background(palette.band).padding(horizontal = 20.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(dateLabel, Modifier.weight(1f), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                    Text(rows.sumOf { it.inrAmountValue ?: 0.0 }.formatHomeRupee(), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            items(rows, key = { "payment-${it.sourceHash}" }) { transaction ->
                                DrilldownTransactionRow(transaction, palette) { onTransactionClick(transaction) }
                            }
                        }
                    }
                    item(key = "privacy") {
                        Text("Stays on this phone", Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.5.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }

    sheet?.let { (title, rows) ->
        ModalBottomSheet(
            onDismissRequest = { sheet = null },
            containerColor = palette.background,
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Text(title, color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 540.dp).padding(top = 8.dp)) {
                    items(rows, key = { "sheet-${it.sourceHash}" }) { transaction ->
                        DrilldownTransactionRow(transaction, palette) { onTransactionClick(transaction) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrilldownSectionHeading(title: String, meta: String, palette: HomePalette) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, Modifier.weight(1f), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(meta, color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DrilldownMixRow(
    group: SummaryGroup,
    total: Double,
    color: Color,
    palette: HomePalette,
    onClick: () -> Unit
) {
    val share = if (total > 0.0) (group.total / total).toFloat().coerceIn(0f, 1f) else 0f
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp).animateContentSize(tween(220))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(group.label, Modifier.weight(1f), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Column(horizontalAlignment = Alignment.End) {
                Text(group.total.formatHomeRupee(), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("${group.count} · ${(share * 100).toInt()}%", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            Text("›", Modifier.padding(start = 10.dp), color = palette.muted, fontSize = 18.sp)
        }
        Box(Modifier.fillMaxWidth().padding(top = 8.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(palette.softFill)) {
            Box(Modifier.fillMaxWidth(share).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(color))
        }
    }
}

@Composable
private fun DrilldownPaymentModeRow(
    mode: String,
    rows: List<TransactionUi>,
    total: Double,
    palette: HomePalette,
    onClick: () -> Unit
) {
    val amount = rows.sumOf { it.inrAmountValue ?: 0.0 }
    val share = if (total > 0.0) amount / total * 100 else 0.0
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(mode, color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("${rows.size} ${if (rows.size == 1) "payment" else "payments"} · ${share.toInt()}%", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.5.sp)
        }
        Text(amount.formatHomeRupee(), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text("›", Modifier.padding(start = 10.dp), color = palette.muted, fontSize = 18.sp)
    }
}

@Composable
private fun DrilldownTransactionRow(transaction: TransactionUi, palette: HomePalette, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).drawBehind {
            drawLine(palette.faintRule, Offset(20.dp.toPx(), size.height - 1.dp.toPx()), Offset(size.width - 20.dp.toPx(), size.height - 1.dp.toPx()), 1.dp.toPx())
        }.padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(palette.softFill), contentAlignment = Alignment.Center) {
            Text(transaction.merchant.firstOrNull()?.uppercaseChar()?.toString() ?: "•", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.weight(1f).padding(start = 11.dp, end = 10.dp)) {
            Text(transaction.merchant.ifBlank { "Payment" }, color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 14.5.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOf(transaction.category, transaction.transactionDate.recentDateLabel(), transaction.paymentMode.takeIf { it.isNotBlank() }).filterNotNull().filter { it.isNotBlank() }.joinToString(" · "),
                Modifier.padding(top = 2.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text((transaction.inrAmountValue ?: 0.0).formatHomeRupee(), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
            if (transaction.needsReview()) {
                Text("REVIEW", Modifier.padding(top = 3.dp).dottedOutline(palette.review).padding(horizontal = 4.dp, vertical = 1.dp), color = palette.review, fontFamily = SortedHomeFontFamily, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
            }
        }
    }
}

@Composable
private fun drilldownMixColor(index: Int, palette: HomePalette): Color = when (index % 3) {
    0 -> palette.categoryOne
    1 -> palette.categoryTwo
    else -> if (isDarkModeActive()) Color(0xFFA8CFC2) else Color(0xFF6F9A86)
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
            text = "${transactions.size} PAYMENTS",
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
            label = "$lineCount PAYMENTS",
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
            label = "$reviewCount NEED REVIEW",
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
        meta = "${rows.size} PAYMENTS",
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
    val sourceTransactions = monthKeys?.let { keys ->
        allTransactions.filter { it.transactionDate?.take(7) in keys }
            .filter { !spendOnly || it.countsTowardSpentTotal() }
    } ?: if (spendOnly) allTransactions.latestMonthSpendTransactions(monthKey) else allTransactions.latestMonthDebitTransactions(monthKey)

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
private fun PermissionPrompt(
    palette: TapePalette,
    onRequestPermission: () -> Unit
) {
    TapeLedgerBlock(
        heading = "Missing source",
        meta = "sms permission",
        palette = palette,
        modifier = Modifier.clickable(onClick = onRequestPermission)
    ) {
        SettingsInfoRow("READ SMS", "LOCAL SCAN ONLY", palette)
        Text(
            text = "SORTED SCANS MESSAGES LOCALLY AND KEEPS TRANSACTIONS ON THIS PHONE.",
            modifier = Modifier.padding(top = 8.dp, bottom = 10.dp),
            color = palette.inkFaint,
            fontFamily = SortedTapeFontFamily,
            fontSize = 8.sp,
            lineHeight = 12.sp,
            letterSpacing = 0.8.sp
        )
        TapeActionText(
            label = "Allow SMS source",
            palette = palette,
            onClick = onRequestPermission
        )
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
    return LocalSortedDarkMode.current
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
@OptIn(ExperimentalMaterial3Api::class)
private fun TransactionDetail(
    transaction: TransactionUi,
    saveState: CorrectionSaveState,
    onCorrect: (TransactionCorrectionDraft) -> Unit,
    onIgnore: () -> Unit,
    categoryShare: Int,
    onClose: () -> Unit,
    onOpenCategory: () -> Unit
) {
    val palette = homePalette()
    val context = LocalContext.current
    var sheet by remember(transaction.id, transaction.sourceHash) { mutableStateOf<String?>(null) }
    var amount by remember(transaction.id, transaction.amountValue) { mutableStateOf(transaction.amountValue.toString()) }
    var place by remember(transaction.id, transaction.merchant) { mutableStateOf(transaction.merchant) }
    var date by remember(transaction.id, transaction.transactionDate) {
        mutableStateOf(transaction.transactionDate.orEmpty())
    }
    var note by remember(transaction.id, transaction.note) { mutableStateOf(transaction.note.orEmpty()) }
    var category by remember(transaction.id, transaction.category) { mutableStateOf(transaction.category) }
    var type by remember(transaction.id, transaction.transactionType) { mutableStateOf(transaction.transactionType) }
    var validation by remember { mutableStateOf<String?>(null) }
    var undoDraft by remember(transaction.id) { mutableStateOf<TransactionCorrectionDraft?>(null) }

    LaunchedEffect(undoDraft) {
        if (undoDraft != null) {
            kotlinx.coroutines.delay(6000)
            undoDraft = null
        }
    }

    val categoryOptions = listOf(
        "Food", "Groceries", "Shopping", "Subscriptions", "Transport", "Utilities",
        "Health", "Entertainment", "Home", "Investment", "Transfer", "Income", "Refund", "Reward", "Other"
    )
    val countOptions = listOf(
        TransactionType.EXPENSE, TransactionType.SUBSCRIPTION, TransactionType.TRANSFER, TransactionType.INVESTMENT,
        TransactionType.REFUND, TransactionType.REWARD, TransactionType.INCOME
    )
    val amountValue = amount.toDoubleOrNull()
    val shownAmount = amountValue?.formatMoney(transaction.currency) ?: transaction.amount
    val isMoneyBack = type == TransactionType.REFUND || type == TransactionType.REWARD || type == TransactionType.INCOME
    val countsLabel = when {
        transaction.countsTowardSpentTotal() -> "Included in monthly total"
        transaction.countsTowardMoneyIn() -> "Money in · shown separately"
        else -> "Not in monthly total"
    }
    val currentPeriod = date.takeIf { it.length >= 7 }?.take(7)?.monthNameLabel() ?: "Payment"
    val statusMessage = saveState.error ?: saveState.message
    val canEdit = transaction.id != null
    val initialDraft = TransactionCorrectionDraft(
        transaction = transaction,
        merchant = transaction.merchant,
        miscCategory = transaction.miscCategory,
        category = transaction.category,
        transactionType = transaction.transactionType,
        amount = transaction.amountValue,
        transactionDate = transaction.transactionDate,
        note = transaction.note.orEmpty(),
        rememberRule = false
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 780.dp)
            .background(palette.background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(palette.header).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.ink)
            }
            Text("Payment", modifier = Modifier.weight(1f), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(currentPeriod, color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(8.dp))
        }

        if (undoDraft != null && statusMessage != null) {
            Row(
                modifier = Modifier.fillMaxWidth().background(palette.softFill).padding(horizontal = 20.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(statusMessage, modifier = Modifier.weight(1f), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = {
                    undoDraft?.let(onCorrect)
                    undoDraft = null
                }) { Text("Undo", color = palette.muted) }
            }
        } else if (saveState.error != null) {
            Text(saveState.error.orEmpty(), modifier = Modifier.fillMaxWidth().background(palette.softFill).padding(horizontal = 20.dp, vertical = 10.dp), color = palette.review, fontFamily = SortedHomeFontFamily, fontSize = 12.5.sp)
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 18.dp)) {
                Text(place.ifBlank { transaction.merchant }, color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    listOfNotNull(date.takeIf(String::isNotBlank)?.let(::manualDateLabel) ?: "Date unknown", transaction.transactionTime?.takeIf(String::isNotBlank), transaction.paymentMode.takeIf(String::isNotBlank)).joinToString(" · "),
                    modifier = Modifier.padding(top = 5.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 12.5.sp
                )
                Text(
                    text = (if (isMoneyBack) "+" else "") + shownAmount,
                    modifier = Modifier.padding(top = 14.dp),
                    color = if (isMoneyBack) palette.credit else palette.ink,
                    fontFamily = SortedHomeFontFamily,
                    fontSize = 52.sp,
                    lineHeight = 56.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    DetailStatusPill(countsLabel, palette, filled = transaction.countsTowardSpentTotal())
                    if (transaction.categorySource == CategorySource.USER_RULE) DetailStatusPill("Edited by you", palette, filled = false)
                    else if (transaction.source == "Manual") DetailStatusPill("Added by you", palette, filled = false)
                }
            }

            Column(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp).background(palette.band).padding(horizontal = 20.dp)) {
                DetailInfoRow("Category", category, palette, clickable = canEdit) { sheet = "category" }
                DetailInfoRow("Paid with", transaction.paymentMode, palette)
                DetailInfoRow("Account", transaction.accountHint?.let { "Ending ${it.takeLast(4)}" } ?: "Not available", palette)
                DetailInfoRow("Monthly total", countsLabel, palette)
                DetailInfoRow("Payment type", type.detailCountsLabel(), palette, clickable = canEdit) { sheet = "counts" }
                DetailInfoRow("Reference", "Not available", palette, last = true)
            }

            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                if (canEdit) {
                    DetailPrimaryAction("Edit transaction", palette, enabled = !saveState.isSaving) { sheet = "edit" }
                }
                if (canEdit) {
                    TextButton(
                        onClick = { sheet = "rule" },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                    ) {
                        Text("Always sort ${place.ifBlank { transaction.merchant }} as $category", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                } else {
                    Text("Sync your messages to edit saved payments.", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 12.sp)
                }
            }

            Column(Modifier.fillMaxWidth().background(palette.band).padding(horizontal = 20.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("PAYMENT DETAILS", modifier = Modifier.weight(1f), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                    Text(transaction.source, color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 11.5.sp)
                }
                Text(
                    "Original alert text is not stored on this phone. The details above are the information Sorted saved for this payment.",
                    modifier = Modifier.padding(top = 8.dp),
                    color = palette.ink,
                    fontFamily = SortedHomeFontFamily,
                    fontSize = 12.5.sp,
                    lineHeight = 21.sp
                )
            }

            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = canEdit, onClick = onOpenCategory).padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("See $category payments in $currentPeriod", modifier = Modifier.weight(1f), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    if (transaction.countsTowardSpentTotal()) Text("$categoryShare%", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("  ›", color = palette.muted, fontSize = 18.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = canEdit) { sheet = "delete" }.padding(vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Delete this payment", modifier = Modifier.weight(1f), color = palette.review, fontFamily = SortedHomeFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("›", color = palette.review, fontSize = 18.sp)
                }
                Text(
                    text = buildString {
                        append(if (transaction.categorySource == CategorySource.USER_RULE) "Edited by you · " else "")
                        append("Imported from ${transaction.source}")
                        transaction.sourceReceivedDate?.let { append(" · read ${manualDateLabel(it)}") }
                    },
                    modifier = Modifier.padding(bottom = 20.dp),
                    color = palette.muted,
                    fontFamily = SortedHomeFontFamily,
                    fontSize = 11.5.sp
                )
            }
        }
    }

    if (sheet != null) {
        ModalBottomSheet(
            onDismissRequest = { sheet = null },
            containerColor = palette.background,
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
        ) {
            when (sheet) {
                "edit" -> Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).imePadding().padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Edit transaction", color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text("${transaction.merchant} · ${manualDateLabel(date)}", color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 12.5.sp)
                    OutlinedTextField(value = amount, onValueChange = { raw -> amount = raw.filter { it.isDigit() || it == '.' }.let { value -> value.substringBefore('.') + if ('.' in value) ".${value.substringAfter('.').replace(".", "").take(2)}" else "" } }, label = { Text("Amount") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = place, onValueChange = { place = it.take(64) }, label = { Text("Place") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Box(Modifier.fillMaxWidth().clickable {
                        val parsed = date.toLocalDateOrNull() ?: LocalDate.now()
                        DatePickerDialog(context, { _, year, month, day -> date = LocalDate.of(year, month + 1, day).toString() }, parsed.year, parsed.monthValue - 1, parsed.dayOfMonth).show()
                    }) {
                        OutlinedTextField(value = date.toLocalDateOrNull()?.toString()?.let(::manualDateLabel) ?: "Choose date", onValueChange = {}, label = { Text("Date") }, enabled = false, readOnly = true, modifier = Modifier.fillMaxWidth())
                    }
                    DetailInfoRow("Category", category, palette, clickable = canEdit) { sheet = "category" }
                    DetailInfoRow("Payment type", type.detailCountsLabel(), palette, clickable = canEdit) { sheet = "counts" }
                    OutlinedTextField(value = note, onValueChange = { note = it.take(180) }, label = { Text("Note") }, minLines = 2, maxLines = 3, modifier = Modifier.fillMaxWidth())
                    if (validation != null || saveState.error != null) Text(validation ?: saveState.error.orEmpty(), color = palette.review, fontFamily = SortedHomeFontFamily, fontSize = 12.sp)
                    DetailPrimaryAction(if (saveState.isSaving) "Saving…" else "Save changes", palette, enabled = !saveState.isSaving) {
                        val parsedAmount = amount.toDoubleOrNull()
                        validation = when {
                            parsedAmount == null || parsedAmount <= 0.0 -> "Enter an amount greater than zero."
                            place.isBlank() -> "Enter a place."
                            date.toLocalDateOrNull() == null -> "Choose a valid date."
                            else -> null
                        }
                        if (validation == null) {
                            undoDraft = initialDraft
                            onCorrect(TransactionCorrectionDraft(transaction, place.trim(), transaction.miscCategory, category, type, parsedAmount!!, date, note.trim(), false))
                            sheet = null
                        }
                    }
                }
                "category" -> Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                    Text("Category", color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text("$place · $shownAmount", modifier = Modifier.padding(top = 4.dp, bottom = 12.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 12.5.sp)
                    categoryOptions.forEach { option ->
                        DetailChoiceRow(option, selected = option == category, palette = palette) {
                            val selectedType = when (option) {
                                "Investment" -> TransactionType.INVESTMENT
                                "Transfer" -> TransactionType.TRANSFER
                                "Income" -> TransactionType.INCOME
                                "Refund" -> TransactionType.REFUND
                                "Reward" -> TransactionType.REWARD
                                else -> type
                            }
                            category = option
                            type = selectedType
                            undoDraft = initialDraft
                            onCorrect(TransactionCorrectionDraft(transaction, place, transaction.miscCategory, option, selectedType, amountValue ?: transaction.amountValue, date, note, false))
                            sheet = null
                        }
                    }
                }
                "counts" -> Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                    Text("Payment type", color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text("This label describes the payment. Every outgoing payment stays in the monthly total.", modifier = Modifier.padding(top = 4.dp, bottom = 12.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 12.5.sp, lineHeight = 18.sp)
                    countOptions.forEach { option ->
                        DetailChoiceRow(option.detailCountsLabel(), selected = option == type, palette = palette) {
                            val selectedCategory = when (option) {
                                TransactionType.TRANSFER -> "Transfer"
                                TransactionType.INVESTMENT -> "Investment"
                                TransactionType.INCOME -> "Income"
                                TransactionType.REFUND -> "Refund"
                                TransactionType.REWARD -> "Reward"
                                else -> if (category in setOf("Transfer", "Investment", "Income", "Refund", "Reward")) "Other" else category
                            }
                            category = selectedCategory
                            type = option
                            undoDraft = initialDraft
                            onCorrect(TransactionCorrectionDraft(transaction, place, transaction.miscCategory, selectedCategory, option, amountValue ?: transaction.amountValue, date, note, false))
                            sheet = null
                        }
                    }
                }
                "rule" -> Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                    Text("Always sort $place as $category?", color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text("Sorted will use this for future payments from this place. You can change rules in Settings.", modifier = Modifier.padding(top = 6.dp, bottom = 16.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 13.sp, lineHeight = 19.sp)
                    DetailChoiceRow("This payment only", selected = false, palette = palette) { sheet = null }
                    DetailChoiceRow("Make a rule", selected = false, palette = palette) {
                        undoDraft = initialDraft
                        onCorrect(TransactionCorrectionDraft(transaction, place.trim(), transaction.miscCategory, category, type, amountValue ?: transaction.amountValue, date, note, true))
                        sheet = null
                    }
                }
                "delete" -> Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                    Text("Delete this payment?", color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text("It will be removed from this phone. Sorted will not add it again on the next import.", modifier = Modifier.padding(top = 8.dp, bottom = 16.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 13.sp, lineHeight = 19.sp)
                    DetailSecondaryAction("Keep this payment", palette) { sheet = null }
                    Spacer(Modifier.height(8.dp))
                    DetailPrimaryAction("Delete payment", palette, enabled = !saveState.isSaving, danger = true) { onIgnore() }
                }
            }
        }
    }
}

@Composable
private fun DetailStatusPill(text: String, palette: HomePalette, filled: Boolean) {
    Surface(color = if (filled) palette.ink else palette.softFill, shape = RoundedCornerShape(50)) {
        Text(text, Modifier.padding(horizontal = 11.dp, vertical = 6.dp), color = if (filled) palette.background else palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DetailInfoRow(label: String, value: String, palette: HomePalette, clickable: Boolean = false, last: Boolean = false, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier).drawBehind {
            if (!last) drawLine(palette.faintRule, Offset(0f, size.height - 1.dp.toPx()), Offset(size.width, size.height - 1.dp.toPx()), 1.dp.toPx())
        }.padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(label, Modifier.width(96.dp), color = palette.muted, fontFamily = SortedHomeFontFamily, fontSize = 13.sp)
        Text(value, Modifier.weight(1f), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (clickable) Text("›", color = palette.muted, fontSize = 18.sp)
    }
}

@Composable
private fun DetailPrimaryAction(label: String, palette: HomePalette, enabled: Boolean = true, danger: Boolean = false, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (danger) palette.review else palette.ink,
        contentColor = palette.background
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, Modifier.padding(horizontal = 16.dp, vertical = 14.dp), color = palette.background, fontFamily = SortedHomeFontFamily, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DetailSecondaryAction(label: String, palette: HomePalette, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(onClick = onClick), shape = RoundedCornerShape(8.dp), color = palette.softFill) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, Modifier.padding(horizontal = 16.dp, vertical = 14.dp), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 15.5.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun DetailChoiceRow(label: String, selected: Boolean, palette: HomePalette, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = palette.ink, fontFamily = SortedHomeFontFamily, fontSize = 15.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
        if (selected) Text("✓", color = palette.credit, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun TransactionType.detailCountsLabel(): String = when (this) {
    TransactionType.EXPENSE -> "Spending"
    TransactionType.SUBSCRIPTION -> "Subscription"
    TransactionType.TRANSFER -> "Transfer"
    TransactionType.INVESTMENT -> "Investment"
    TransactionType.REFUND -> "Refund"
    TransactionType.REWARD -> "Reward"
    TransactionType.INCOME -> "Income"
    TransactionType.UNKNOWN -> "Other"
}

@Composable
private fun TransactionDetailLegacy(
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
    val palette = tapePalette()
    val accent = transaction.tapeLineStyle(palette).accent

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .background(palette.tape)
            .padding(start = 18.dp, end = 18.dp, bottom = 34.dp)
    ) {
        TapeDoubleRule(palette = palette)
        Text(
            text = "PAYMENT",
            color = palette.inkFaint,
            fontFamily = SortedHomeFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.4.sp,
            maxLines = 1
        )
        Text(
            text = transaction.merchant,
            modifier = Modifier.padding(top = 8.dp),
            color = palette.ink,
            fontFamily = SortedHomeFontFamily,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = transaction.amount,
            color = accent,
            fontFamily = SortedHomeFontFamily,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp
        )
        Spacer(modifier = Modifier.height(14.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                listOf(
                    transaction.category,
                    transaction.countingLabel(),
                    transaction.source,
                    if (transaction.direction == DirectionUi.Credit) "Money in" else "Money out"
                )
            ) { label ->
                AddStamp(
                    text = label.uppercase(Locale.US),
                    selected = true,
                    palette = palette,
                    color = accent
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TapeLedgerBlock(
            heading = "Payment details",
            meta = transaction.source,
            palette = palette,
            modifier = Modifier.padding(horizontal = 0.dp)
        ) {
            Text(
                text = listOfNotNull(
                    transaction.paymentMode.takeIf { it.isNotBlank() },
                    transaction.accountHint?.let { "Account · $it" },
                    transaction.transactionDate
                ).joinToString("  ·  ").ifBlank { transaction.detail },
                color = palette.inkFaint,
                fontFamily = SortedHomeFontFamily,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                letterSpacing = 0.sp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TapeActionText(
                label = if (editing) "Close edit" else "Edit transaction",
                palette = palette,
                modifier = Modifier.weight(1f),
                enabled = !saveState.isSaving,
                onClick = {
                    editing = !editing
                    confirmIgnore = false
                    validationError = null
                }
            )
            TapeActionText(
                label = if (confirmIgnore) "Cancel" else "Delete payment",
                palette = palette,
                color = palette.query,
                modifier = Modifier.weight(1f),
                enabled = !saveState.isSaving,
                onClick = {
                    confirmIgnore = !confirmIgnore
                    editing = false
                }
            )
        }
        AnimatedVisibility(visible = confirmIgnore) {
            Column(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .dottedOutline(palette.query)
                    .padding(12.dp)
            ) {
                Text(
                    text = "DELETE THIS PAYMENT?",
                    color = palette.query,
                    fontFamily = SortedTapeFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "It will be removed from your payment list and will not return on the next import.",
                    color = palette.inkFaint,
                    fontFamily = SortedHomeFontFamily,
                    fontSize = 13.sp,
                    letterSpacing = 0.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                TapeActionText(
                    label = if (saveState.isSaving) "Deleting" else "Delete payment",
                    palette = palette,
                    color = palette.query,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !saveState.isSaving,
                    onClick = onIgnore
                )
            }
        }
        AnimatedVisibility(visible = editing) {
            Column(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .border(1.dp, palette.rule)
                    .padding(12.dp)
            ) {
                Text(
                    text = "EDIT TRANSACTION",
                    color = palette.inkFaint,
                    fontFamily = SortedHomeFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                AddInputLine(
                    label = "PLACE",
                    value = merchant,
                    onValueChange = { merchant = it.take(48) },
                    placeholder = "NAME",
                    palette = palette
                )
                AddInputLine(
                    label = "DETAIL",
                    value = miscCategory,
                    onValueChange = { miscCategory = it.take(36) },
                    placeholder = "MERCHANT TAG",
                    palette = palette
                )
                Spacer(modifier = Modifier.height(12.dp))
                AddSectionLabel("CATEGORY", palette)
                AddStampRail(
                    stamps = categories.map { it.uppercase(Locale.US) },
                    selected = category,
                    palette = palette,
                    selectedColor = palette.amber,
                    onSelected = { selected ->
                        category = selected.lowercase(Locale.US).replaceFirstChar { it.titlecase(Locale.US) }
                        if (category == "Investment") transactionType = TransactionType.INVESTMENT
                        if (category == "Transfer") transactionType = TransactionType.TRANSFER
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                AddSectionLabel("COUNTS AS", palette)
                AddStampRail(
                    stamps = transactionTypes.map { it.displayName().uppercase(Locale.US) },
                    selected = transactionType.displayName(),
                    palette = palette,
                    selectedColor = palette.credit,
                    onSelected = { selected ->
                        transactionType = transactionTypes.first { it.displayName().equals(selected, ignoreCase = true) }
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
                    AddStamp(
                        text = "REMEMBER FOR THIS PLACE",
                        selected = rememberRule,
                        palette = palette,
                        color = palette.amber,
                        modifier = Modifier.weight(1f),
                        onClick = { rememberRule = true }
                    )
                    AddStamp(
                        text = "THIS PAYMENT ONLY",
                        selected = !rememberRule,
                        palette = palette,
                        color = palette.amber,
                        modifier = Modifier.weight(1f),
                        onClick = { rememberRule = false }
                    )
                }
                if (statusText != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = statusText,
                        color = if (validationError == null && saveState.error == null) {
                            palette.amber
                        } else {
                            palette.query
                        },
                        fontFamily = SortedHomeFontFamily,
                        fontSize = 13.sp,
                        letterSpacing = 0.sp
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                TapeActionText(
                    label = if (saveState.isSaving) "Saving" else "Save changes",
                    palette = palette,
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
                                    amount = transaction.amountValue,
                                    transactionDate = transaction.transactionDate ?: LocalDate.now().toString(),
                                    note = transaction.note.orEmpty(),
                                    rememberRule = rememberRule
                                )
                            )
                        }
                    }
                )
            }
        }
        if (!editing && !confirmIgnore && statusText != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = statusText,
                color = if (saveState.error == null) palette.amber else palette.query,
                fontFamily = SortedHomeFontFamily,
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

private fun Double.formatHomeRupee(): String {
    val rounded = roundToInt().coerceAtLeast(0).toString()
    if (rounded.length <= 3) return "₹$rounded"
    val last3 = rounded.takeLast(3)
    val rest = rounded.dropLast(3)
        .reversed()
        .chunked(2)
        .joinToString(",")
        .reversed()
    return "₹$rest,$last3"
}

private fun String.toCsvCell(): String = "\"${replace("\"", "\"\"")}\""

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

private fun TransactionUi.countsTowardSpentTotal(): Boolean {
    val direction = when (direction) {
        DirectionUi.Debit -> Direction.DEBIT
        DirectionUi.Credit -> Direction.CREDIT
        DirectionUi.Unknown -> Direction.UNKNOWN
    }
    return OutflowPolicy.countsTowardSpent(
        status = status,
        direction = direction,
        amount = inrAmountValue
    )
}

private fun TransactionUi.countsTowardMoneyIn(): Boolean =
    status == TransactionStatus.COMPLETED &&
        direction == DirectionUi.Credit &&
        amountValue > 0.0 &&
        inrAmountValue != null

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

private fun TransactionUi.countingLabel(): String {
    return when {
        countsTowardSpentTotal() -> "Included in monthly total"
        countsTowardMoneyIn() -> "Money in · shown separately"
        else -> "Not in monthly total"
    }
}

private fun TransactionUi.displayAmount(): String =
    inrAmountValue?.formatHomeRupee() ?: amount

private fun String.monthSpendLabel(): String {
    return "Spent in ${monthNameLabel()}"
}

private fun String.monthMovementLabel(): String {
    return "Spent in ${monthNameLabel()}"
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

private fun Int.monthLetter(): String {
    return when (this) {
        1 -> "J"
        2 -> "F"
        3 -> "M"
        4 -> "A"
        5 -> "M"
        6 -> "J"
        7 -> "J"
        8 -> "A"
        9 -> "S"
        10 -> "O"
        11 -> "N"
        12 -> "D"
        else -> ""
    }
}
