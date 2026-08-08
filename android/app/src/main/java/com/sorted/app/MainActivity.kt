package com.sorted.app

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.result.IntentSenderRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.sorted.app.engine.Direction
import com.sorted.app.engine.ParsedTransaction
import com.sorted.app.engine.PaymentMode
import com.sorted.app.engine.SmsParser
import com.sorted.app.engine.TransactionType
import com.sorted.app.data.ImportRecord
import com.sorted.app.data.ImportSource
import com.sorted.app.data.FxRateEntity
import com.sorted.app.data.FxRateKey
import com.sorted.app.data.FxRateRepository
import com.sorted.app.data.TransactionEntity
import com.sorted.app.data.TransactionRepository
import com.sorted.app.data.stableHash
import com.sorted.app.gmail.GmailImportPlan
import com.sorted.app.gmail.GmailImportSummary
import com.sorted.app.gmail.GmailImporter
import com.sorted.app.gmail.GmailSyncPreferences
import com.sorted.app.gmail.GmailSyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SortedTheme {
                SortedHome()
            }
        }
    }
}

private data class TransactionUi(
    val merchant: String,
    val detail: String,
    val amount: String,
    val amountValue: Double,
    val currency: String,
    val inrAmountValue: Double?,
    val category: String,
    val direction: DirectionUi,
    val transactionType: TransactionType,
    val transactionDate: String?,
    val source: String
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
    val totalDebits: Double,
    val spends: Double,
    val transfers: Double,
    val investments: Double,
    val fxConverted: Double
)

private data class SummaryGroup(
    val label: String,
    val count: Int,
    val total: Double,
    val currency: String,
    val category: String
)

private data class DrilldownState(
    val title: String,
    val kind: DrilldownKind,
    val group: SummaryGroup
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
    Capture("Capture", SortedNavIcon.Capture),
    Sources("Sources", SortedNavIcon.Sources)
}

private enum class SortedNavIcon {
    Home,
    Insights,
    Capture,
    Sources,
    Settings
}

private enum class DirectionUi {
    Debit,
    Credit
}

private fun parsedSampleTransactions(): List<TransactionUi> {
    return SampleSmsSource.messages
        .map(SmsParser::parse)
        .filter { it.isTransaction }
        .map { it.toTransactionUi() }
}

private fun loadRealSmsTransactions(context: Context): List<TransactionUi> {
    val repository = TransactionRepository(context)
    val messages = readRecentSmsMessages(context, limit = 3000)
    val records = messages.map { message ->
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

private fun ParsedTransaction.toTransactionUi(
    source: String = "Parsed SMS",
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
        merchant = merchantNormalized ?: merchantRaw ?: "Unknown",
        detail = detail,
        amount = amountNumber.formatMoney(currencyCode),
        amountValue = amountNumber,
        currency = currencyCode,
        inrAmountValue = inrEquivalent,
        category = category,
        direction = directionUi,
        transactionType = transactionType,
        transactionDate = transactionDate,
        source = source
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
        merchant = merchantNormalized ?: merchantRaw ?: "Unknown",
        detail = detail,
        amount = amountNumber.formatMoney(currencyCode),
        amountValue = amountNumber,
        currency = currencyCode,
        inrAmountValue = inrEquivalent,
        category = category,
        direction = directionUi,
        transactionType = transactionType,
        transactionDate = transactionDate,
        source = source.displayLabel()
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

@Composable
private fun SortedTheme(content: @Composable () -> Unit) {
    val darkMode = isSystemInDarkTheme()
    val appFontFamily = if (darkMode) {
        FontFamily(
            Font(R.font.balsamiq_sans_regular, FontWeight.Normal),
            Font(R.font.balsamiq_sans_bold, FontWeight.Medium),
            Font(R.font.balsamiq_sans_bold, FontWeight.SemiBold),
            Font(R.font.balsamiq_sans_bold, FontWeight.Bold)
        )
    } else {
        FontFamily(
            Font(R.font.comic_neue_regular, FontWeight.Normal),
            Font(R.font.comic_neue_regular, FontWeight.Medium),
            Font(R.font.comic_neue_bold, FontWeight.SemiBold),
            Font(R.font.comic_neue_bold, FontWeight.Bold)
        )
    }
    val colors = if (darkMode) {
        darkColorScheme(
            background = Color(0xFF000000),
            surface = Color(0xFF080806),
            surfaceVariant = Color(0xFF151108),
            primary = Color(0xFFFFC857),
            secondary = Color(0xFFFFE08A),
            tertiary = Color(0xFFD99A2B),
            onBackground = Color(0xFFFFF8E7),
            onSurface = Color(0xFFFFF8E7),
            onSurfaceVariant = Color(0xFFC9B889),
            onPrimary = Color(0xFF171000)
        )
    } else {
        lightColorScheme(
            background = Color(0xFFFFFBF1),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFFFF0B8),
            primary = Color(0xFFFF4D8D),
            secondary = Color(0xFF00B8FF),
            tertiary = Color(0xFF7C4DFF),
            onBackground = Color(0xFF171018),
            onSurface = Color(0xFF171018),
            onSurfaceVariant = Color(0xFF64555F),
            onPrimary = Color(0xFFFFFFFF)
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortedHome() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val gmailSetupInfo = remember { gmailSetupInfo(appContext) }
    val gmailSyncPreferences = remember { GmailSyncPreferences(appContext) }
    var selected by remember { mutableStateOf<TransactionUi?>(null) }
    var drilldown by remember { mutableStateOf<DrilldownState?>(null) }
    var selectedTab by remember { mutableStateOf(SortedTab.Home) }
    var settingsOpen by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(hasReadSmsPermission(context)) }
    var gmailState by remember {
        mutableStateOf(GmailUiState(autoSyncLabel = gmailSyncPreferences.statusLabel()))
    }
    var feedState by remember {
        mutableStateOf(
            FeedState(
                transactions = parsedSampleTransactions(),
                label = "sample SMS",
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

    fun runGmailImport(accessToken: String?, startLabel: String = "Reading Gmail") {
        if (accessToken.isNullOrBlank()) {
            gmailState = gmailStateWithAutoSync(
                label = "Gmail import failed",
                error = "Google did not return an access token."
            )
            return
        }

        gmailState = gmailStateWithAutoSync(label = startLabel, isImporting = true)
        scope.launch {
            try {
                val (summary, transactions) = withContext(Dispatchers.IO) {
                    val summary = GmailImporter(appContext).importLatest(accessToken)
                    GmailSyncScheduler.schedule(appContext)
                    gmailSyncPreferences.markSyncSuccess(summary)
                    val transactions = loadPersistedTransactions(appContext)
                    summary to transactions
                }
                feedState = FeedState(
                    transactions = transactions,
                    label = transactions.feedSourceLabel(),
                    needsSmsPermission = !hasPermission
                )
                gmailState = gmailStateWithAutoSync(label = summary.displayLabel())
            } catch (error: Throwable) {
                gmailSyncPreferences.markSyncError(error.message ?: error.javaClass.simpleName)
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
                gmailState = gmailStateWithAutoSync(
                    label = "Gmail permission missing",
                    error = "Gmail read permission was not granted."
                )
                return@rememberLauncherForActivityResult
            }
            runGmailImport(authorizationResult.accessToken)
        } catch (error: ApiException) {
            Log.e(LogTag, "Gmail authorization failed", error)
            gmailState = gmailStateWithAutoSync(
                label = "Gmail import failed",
                error = gmailAuthErrorMessage(error, gmailSetupInfo)
            )
        } catch (error: Throwable) {
            Log.e(LogTag, "Gmail authorization result failed", error)
            gmailState = gmailStateWithAutoSync(
                label = "Gmail import failed",
                error = error.message ?: error.javaClass.simpleName
            )
        }
    }

    fun requestGmailImport() {
        val activity = context.findComponentActivity()
        if (activity == null) {
            gmailState = gmailStateWithAutoSync(
                label = "Gmail import failed",
                error = "Unable to open Google authorization from this screen."
            )
            return
        }

        gmailState = gmailStateWithAutoSync(label = "Opening Google consent", isImporting = true)
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(GmailImportPlan.RequiredScope)))
            .build()

        Identity.getAuthorizationClient(activity)
            .authorize(request)
            .addOnSuccessListener { authorizationResult ->
                if (authorizationResult.hasResolution()) {
                    val pendingIntent = authorizationResult.pendingIntent
                    if (pendingIntent == null) {
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
        feedState = if (hasPermission) {
            val realTransactions = loadRealSmsTransactions(context)
            FeedState(
                transactions = realTransactions,
                label = realTransactions.feedSourceLabel(),
                needsSmsPermission = false
            )
        } else {
            FeedState(
                transactions = parsedSampleTransactions(),
                label = "sample SMS",
                needsSmsPermission = true
            )
        }
    }

    val activeDrilldown = drilldown
    BackHandler(
        enabled = activeDrilldown != null || settingsOpen || selectedTab != SortedTab.Home
    ) {
        when {
            activeDrilldown != null -> drilldown = null
            settingsOpen -> settingsOpen = false
            selectedTab != SortedTab.Home -> selectedTab = SortedTab.Home
        }
    }

    when {
        activeDrilldown != null -> {
            DrilldownScreen(
                state = activeDrilldown,
                allTransactions = feedState.transactions,
                onBack = { drilldown = null },
                onTransactionClick = { selected = it }
            )
        }

        settingsOpen -> {
            SettingsScreen(onBack = { settingsOpen = false })
        }

        else -> {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    SortedBottomBar(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                }
            ) { padding ->
                when (selectedTab) {
                    SortedTab.Home -> HomeTabContent(
                        feedState = feedState,
                        modifier = Modifier.padding(padding),
                        onSettings = { settingsOpen = true },
                        onMerchantClick = { group ->
                            drilldown = DrilldownState(
                                title = group.label,
                                kind = DrilldownKind.Merchant,
                                group = group
                            )
                        },
                        onCategoryClick = { group ->
                            drilldown = DrilldownState(
                                title = group.label,
                                kind = DrilldownKind.Category,
                                group = group
                            )
                        }
                    )

                    SortedTab.Insights -> InsightsTabContent(
                        feedState = feedState,
                        modifier = Modifier.padding(padding),
                        onSettings = { settingsOpen = true },
                        onTransactionClick = { selected = it }
                    )

                    SortedTab.Capture -> CaptureTabContent(
                        modifier = Modifier.padding(padding),
                        onSettings = { settingsOpen = true }
                    )

                    SortedTab.Sources -> SourcesTabContent(
                        feedState = feedState,
                        gmailState = gmailState,
                        gmailSetupInfo = gmailSetupInfo,
                        modifier = Modifier.padding(padding),
                        onSettings = { settingsOpen = true },
                        onRequestSmsPermission = {
                            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                        },
                        onImportGmail = { requestGmailImport() }
                    )
                }
            }
        }
    }

    selected?.let { transaction ->
        ModalBottomSheet(
            onDismissRequest = { selected = null },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
        ) {
            TransactionDetail(transaction = transaction)
        }
    }
}

@Composable
private fun HomeTabContent(
    feedState: FeedState,
    modifier: Modifier,
    onSettings: () -> Unit,
    onMerchantClick: (SummaryGroup) -> Unit,
    onCategoryClick: (SummaryGroup) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Header(title = "Sorted", onSettings = onSettings)
        }
        item {
            MonthSummary(feedState)
        }
        item {
            SummaryRail(
                title = "By merchant",
                groups = feedState.transactions.monthMerchantGroups(),
                onGroupClick = onMerchantClick
            )
        }
        item {
            SummaryRail(
                title = "By category",
                groups = feedState.transactions.monthCategoryGroups(),
                onGroupClick = onCategoryClick
            )
        }
        item {
            Spacer(modifier = Modifier.height(104.dp))
        }
    }
}

@Composable
private fun InsightsTabContent(
    feedState: FeedState,
    modifier: Modifier,
    onSettings: () -> Unit,
    onTransactionClick: (TransactionUi) -> Unit
) {
    val recentTransactions = remember(feedState.transactions) {
        feedState.transactions.sortedByDescending { transaction ->
            transaction.transactionDate.orEmpty()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Header(title = "Insights", onSettings = onSettings)
        }
        item {
            SectionIntroCard(
                title = "Recent transactions",
                body = "${recentTransactions.size} transactions from ${feedState.label}",
                accent = MaterialTheme.colorScheme.primary
            )
        }
        items(recentTransactions) { transaction ->
            TransactionRow(
                transaction = transaction,
                onClick = { onTransactionClick(transaction) }
            )
        }
        item {
            Spacer(modifier = Modifier.height(104.dp))
        }
    }
}

@Composable
private fun CaptureTabContent(
    modifier: Modifier,
    onSettings: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Header(title = "Capture", onSettings = onSettings)
        }
        item {
            SectionIntroCard(
                title = "Manual add",
                body = "Fast transaction entry will live here. This tab replaces the floating add button.",
                accent = MaterialTheme.colorScheme.primary
            )
        }
        item {
            FeaturePlaceholder(
                title = "Next build",
                body = "Amount, merchant, category, payment mode, date, and note in one low-friction sheet."
            )
        }
        item {
            Spacer(modifier = Modifier.height(104.dp))
        }
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
            SourceStatusCard(feedState = feedState)
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
private fun SettingsScreen(onBack: () -> Unit) {
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
                SectionIntroCard(
                    title = "Settings and privacy",
                    body = "Theme, local data controls, exports, parser diagnostics, and source permissions will be grouped here.",
                    accent = MaterialTheme.colorScheme.primary
                )
            }
            item {
                FeaturePlaceholder(
                    title = "Local-first controls",
                    body = "No backend. The important settings are import sources, database controls, export/delete, and parser debugging."
                )
            }
            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
private fun SectionIntroCard(
    title: String,
    body: String,
    accent: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    letterSpacing = 0.sp
                )
            }
        }
    }
}

@Composable
private fun FeaturePlaceholder(
    title: String,
    body: String
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
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 18.sp,
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
            IconButton(onClick = {}) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
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
private fun SortedBottomBar(
    selectedTab: SortedTab,
    onTabSelected: (SortedTab) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SortedTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                val contentColor = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                val itemBackground = if (selected) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    Color.Transparent
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(itemBackground)
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SortedNavGlyph(
                        icon = tab.icon,
                        color = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = tab.label,
                        color = contentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        letterSpacing = 0.sp
                    )
                }
            }
        }
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
private fun SortedNavGlyph(
    icon: SortedNavIcon,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.1.dp.toPx()
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        val w = size.width
        val h = size.height

        when (icon) {
            SortedNavIcon.Home -> {
                drawLine(color, Offset(w * 0.18f, h * 0.48f), Offset(w * 0.50f, h * 0.22f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.50f, h * 0.22f), Offset(w * 0.82f, h * 0.48f), strokeWidth, StrokeCap.Round)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.28f, h * 0.48f),
                    size = Size(w * 0.44f, h * 0.34f),
                    cornerRadius = CornerRadius(w * 0.07f, w * 0.07f)
                )
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(w * 0.42f, h * 0.62f),
                    size = Size(w * 0.16f, h * 0.20f),
                    cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
                )
            }

            SortedNavIcon.Insights -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.18f, h * 0.55f),
                    size = Size(w * 0.14f, h * 0.30f),
                    cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.43f, h * 0.36f),
                    size = Size(w * 0.14f, h * 0.49f),
                    cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.68f, h * 0.20f),
                    size = Size(w * 0.14f, h * 0.65f),
                    cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
                )
            }

            SortedNavIcon.Capture -> {
                drawCircle(
                    color = color,
                    radius = w * 0.36f,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = stroke
                )
                drawLine(color, Offset(w * 0.34f, h * 0.5f), Offset(w * 0.66f, h * 0.5f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.5f, h * 0.34f), Offset(w * 0.5f, h * 0.66f), strokeWidth, StrokeCap.Round)
            }

            SortedNavIcon.Sources -> {
                drawCircle(color, radius = w * 0.10f, center = Offset(w * 0.24f, h * 0.33f))
                drawCircle(color, radius = w * 0.10f, center = Offset(w * 0.24f, h * 0.68f))
                drawLine(color, Offset(w * 0.42f, h * 0.33f), Offset(w * 0.78f, h * 0.33f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.42f, h * 0.68f), Offset(w * 0.78f, h * 0.68f), strokeWidth, StrokeCap.Round)
            }

            SortedNavIcon.Settings -> {
                drawCircle(
                    color = color,
                    radius = w * 0.32f,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = stroke
                )
                drawCircle(
                    color = color,
                    radius = w * 0.10f,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = stroke
                )
                drawLine(color, Offset(w * 0.5f, h * 0.04f), Offset(w * 0.5f, h * 0.18f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.5f, h * 0.82f), Offset(w * 0.5f, h * 0.96f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.04f, h * 0.5f), Offset(w * 0.18f, h * 0.5f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.82f, h * 0.5f), Offset(w * 0.96f, h * 0.5f), strokeWidth, StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun MonthSummary(feedState: FeedState) {
    val breakdown = feedState.transactions.monthBreakdown()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = breakdown.monthKey?.monthOutflowLabel() ?: "Tracked INR outflow",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        letterSpacing = 0.sp
                    )
                    Text(
                        text = breakdown.totalDebits.formatInr(),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${breakdown.debitCount} debits",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.sp
                    )
                    Text(
                        text = feedState.label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        letterSpacing = 0.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            SummaryBreakdownRow("Spends", breakdown.spends)
            SummaryBreakdownRow("Transfers", breakdown.transfers)
            SummaryBreakdownRow("Investments", breakdown.investments)
            if (breakdown.fxConverted > 0.0) {
                SummaryBreakdownRow("FX converted", breakdown.fxConverted)
            }
        }
    }
}

private fun List<TransactionUi>.monthBreakdown(): MonthBreakdown {
    val monthKey = mapNotNull { it.transactionDate?.take(7) }.maxOrNull()
    val allMonthTransactions = filter {
        monthKey == null || it.transactionDate?.startsWith(monthKey) == true
    }
    val monthTransactions = allMonthTransactions.filter {
        it.inrAmountValue != null &&
            (monthKey == null || it.transactionDate?.startsWith(monthKey) == true)
    }
    val debitTransactions = monthTransactions.filter { it.direction == DirectionUi.Debit }
    val fxConverted = debitTransactions
        .filter { !it.countsInInrTotals() }
        .sumOf { it.inrAmountValue ?: 0.0 }
    val spends = debitTransactions
        .filter { it.transactionType.countsAsSpend() }
        .sumOf { it.inrAmountValue ?: 0.0 }
    val transfers = debitTransactions
        .filter { it.transactionType == TransactionType.TRANSFER }
        .sumOf { it.inrAmountValue ?: 0.0 }
    val investments = debitTransactions
        .filter { it.transactionType == TransactionType.INVESTMENT }
        .sumOf { it.inrAmountValue ?: 0.0 }

    return MonthBreakdown(
        monthKey = monthKey,
        debitCount = debitTransactions.size,
        totalDebits = debitTransactions.sumOf { it.inrAmountValue ?: 0.0 },
        spends = spends,
        transfers = transfers,
        investments = investments,
        fxConverted = fxConverted
    )
}

private fun List<TransactionUi>.latestMonthDebitTransactions(): List<TransactionUi> {
    val monthKey = mapNotNull { it.transactionDate?.take(7) }.maxOrNull()
    return filter {
        it.direction == DirectionUi.Debit &&
            (monthKey == null || it.transactionDate?.startsWith(monthKey) == true)
    }
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
        .take(12)
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

private fun List<TransactionUi>.feedSourceLabel(): String {
    val sourceSet = map { it.source }.toSet()
    return when {
        "SMS" in sourceSet && "Gmail" in sourceSet -> "SMS + Gmail"
        "Gmail" in sourceSet -> "Gmail"
        "SMS" in sourceSet -> "device SMS"
        else -> "sample SMS"
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
    val transactions = remember(state, allTransactions) {
        state.filteredTransactions(allTransactions)
    }

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
                DrilldownHeader(
                    state = state,
                    transactions = transactions,
                    onBack = onBack
                )
            }
            item {
                DrilldownBreakdown(transactions = transactions, kind = state.kind)
            }
            item {
                SectionLabel("Transactions")
            }
            items(transactions) { transaction ->
                TransactionRow(
                    transaction = transaction,
                    onClick = { onTransactionClick(transaction) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun DrilldownHeader(
    state: DrilldownState,
    transactions: List<TransactionUi>,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = 0.sp
                    )
                    Text(
                        text = state.kind.label(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        letterSpacing = 0.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = state.group.total.formatMoney(state.group.currency),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${transactions.size} transaction${if (transactions.size == 1) "" else "s"} this month",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                letterSpacing = 0.sp
            )
        }
    }
}

@Composable
private fun DrilldownBreakdown(
    transactions: List<TransactionUi>,
    kind: DrilldownKind
) {
    val sourceGroups = transactions
        .groupBy { it.source }
        .map { (source, rows) -> source to rows.size }
        .sortedByDescending { it.second }
    val secondaryGroups = when (kind) {
        DrilldownKind.Merchant -> transactions
            .groupBy { it.category }
            .map { (category, rows) -> category to rows.size }
            .sortedByDescending { it.second }

        DrilldownKind.Category -> transactions
            .groupBy { it.merchant }
            .map { (merchant, rows) -> merchant to rows.size }
            .sortedByDescending { it.second }
            .take(4)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = if (kind == DrilldownKind.Merchant) "Category split" else "Merchant split",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            secondaryGroups.forEach { (label, count) ->
                DrilldownBreakdownRow(label = label, count = count)
            }
            if (sourceGroups.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sourceGroups.forEach { (source, count) ->
                        DetailChip("$source $count")
                    }
                }
            }
        }
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
    return allTransactions
        .latestMonthDebitTransactions()
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
        SectionLabel(title)
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
    Surface(
        onClick = onClick,
        modifier = Modifier.width(176.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryMiniDot(group.category)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = group.displayLabel(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
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
                textAlign = TextAlign.Center,
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
                textAlign = TextAlign.Center,
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
private fun SummaryBreakdownRow(label: String, amount: Double, currency: String = "INR") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            letterSpacing = 0.sp
        )
        Text(
            text = amount.formatMoney(currency),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp
        )
    }
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
    val darkMode = isSystemInDarkTheme()
    return if (darkMode) {
        when (category) {
            "Food" -> Color(0xFFFFC857)
            "Groceries" -> Color(0xFFFFD76D)
            "Investment" -> Color(0xFFE9B949)
            "Refund" -> Color(0xFFFFE6A3)
            "Subscriptions" -> Color(0xFFD7A928)
            "Reward" -> Color(0xFFFFF0B8)
            "Transfer" -> Color(0xFFC9951F)
            "Shopping" -> Color(0xFFF3C969)
            "Health" -> Color(0xFFFFDFA0)
            "Entertainment" -> Color(0xFFE2B33B)
            "Transport" -> Color(0xFFBC8926)
            "Utilities" -> Color(0xFFFFC045)
            "Fuel" -> Color(0xFFFFB020)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
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
private fun TransactionDetail(transaction: TransactionUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailChip(transaction.category)
            DetailChip(transaction.source)
            DetailChip(if (transaction.direction == DirectionUi.Credit) "Credit" else "Debit")
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

private fun String.monthOutflowLabel(): String {
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
    return "$monthName INR outflow"
}
