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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import com.sorted.app.data.TransactionEntity
import com.sorted.app.data.TransactionRepository
import com.sorted.app.data.stableHash
import com.sorted.app.gmail.GmailImportPlan
import com.sorted.app.gmail.GmailImportSummary
import com.sorted.app.gmail.GmailImporter
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
    val error: String? = null
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
    val investments: Double
)

private data class SummaryGroup(
    val label: String,
    val count: Int,
    val total: Double,
    val category: String
)

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
    return repository.listTransactions().map { it.toTransactionUi() }
}

private fun loadPersistedTransactions(context: Context): List<TransactionUi> {
    return TransactionRepository(context).listTransactions().map { it.toTransactionUi() }
}

private fun ParsedTransaction.toTransactionUi(source: String = "Parsed SMS"): TransactionUi {
    val amountNumber = amount ?: 0.0
    val payment = paymentMode.displayName()
    val misc = miscCategory ?: "Uncategorized"
    val category = departmentCategory ?: "Other"
    val date = transactionDate ?: "Date unknown"
    val directionUi = if (direction == Direction.CREDIT) DirectionUi.Credit else DirectionUi.Debit

    return TransactionUi(
        merchant = merchantNormalized ?: merchantRaw ?: "Unknown",
        detail = "$payment • $misc • $date",
        amount = amountNumber.formatInr(),
        amountValue = amountNumber,
        category = category,
        direction = directionUi,
        transactionType = transactionType,
        transactionDate = transactionDate,
        source = source
    )
}

private fun TransactionEntity.toTransactionUi(): TransactionUi {
    val amountNumber = amount ?: 0.0
    val payment = paymentMode.displayName()
    val misc = miscCategory ?: "Uncategorized"
    val category = departmentCategory ?: "Other"
    val date = transactionDate ?: "Date unknown"
    val directionUi = if (direction == Direction.CREDIT) DirectionUi.Credit else DirectionUi.Debit

    return TransactionUi(
        merchant = merchantNormalized ?: merchantRaw ?: "Unknown",
        detail = "$payment • $misc • $date",
        amount = amountNumber.formatInr(),
        amountValue = amountNumber,
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
    val colors = darkColorScheme(
        background = Color(0xFF101412),
        surface = Color(0xFF171C19),
        surfaceVariant = Color(0xFF202721),
        primary = Color(0xFF9BE3B4),
        secondary = Color(0xFFFFCB77),
        tertiary = Color(0xFF8EC9FF),
        onBackground = Color(0xFFF3F5F1),
        onSurface = Color(0xFFF3F5F1),
        onSurfaceVariant = Color(0xFFC7D0C7),
        onPrimary = Color(0xFF102016)
    )

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortedHome() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val gmailSetupInfo = remember { gmailSetupInfo(appContext) }
    var selected by remember { mutableStateOf<TransactionUi?>(null) }
    var hasPermission by remember { mutableStateOf(hasReadSmsPermission(context)) }
    var gmailState by remember { mutableStateOf(GmailUiState()) }
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
    fun runGmailImport(accessToken: String?) {
        if (accessToken.isNullOrBlank()) {
            gmailState = GmailUiState(
                label = "Gmail import failed",
                error = "Google did not return an access token."
            )
            return
        }

        gmailState = GmailUiState(label = "Reading Gmail", isImporting = true)
        scope.launch {
            try {
                val (summary, transactions) = withContext(Dispatchers.IO) {
                    val summary = GmailImporter(appContext).importLatest(accessToken)
                    val transactions = loadPersistedTransactions(appContext)
                    summary to transactions
                }
                feedState = FeedState(
                    transactions = transactions,
                    label = transactions.feedSourceLabel(),
                    needsSmsPermission = !hasPermission
                )
                gmailState = GmailUiState(label = summary.displayLabel())
            } catch (error: Throwable) {
                gmailState = GmailUiState(
                    label = "Gmail import failed",
                    error = error.message?.take(180) ?: error.javaClass.simpleName
                )
            }
        }
    }

    val gmailAuthorizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.data == null) {
            gmailState = GmailUiState(
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
                gmailState = GmailUiState(
                    label = "Gmail permission missing",
                    error = "Gmail read permission was not granted."
                )
                return@rememberLauncherForActivityResult
            }
            runGmailImport(authorizationResult.accessToken)
        } catch (error: ApiException) {
            Log.e(LogTag, "Gmail authorization failed", error)
            gmailState = GmailUiState(
                label = "Gmail import failed",
                error = gmailAuthErrorMessage(error, gmailSetupInfo)
            )
        } catch (error: Throwable) {
            Log.e(LogTag, "Gmail authorization result failed", error)
            gmailState = GmailUiState(
                label = "Gmail import failed",
                error = error.message ?: error.javaClass.simpleName
            )
        }
    }

    fun requestGmailImport() {
        val activity = context.findComponentActivity()
        if (activity == null) {
            gmailState = GmailUiState(
                label = "Gmail import failed",
                error = "Unable to open Google authorization from this screen."
            )
            return
        }

        gmailState = GmailUiState(label = "Opening Google consent", isImporting = true)
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(GmailImportPlan.RequiredScope)))
            .build()

        Identity.getAuthorizationClient(activity)
            .authorize(request)
            .addOnSuccessListener { authorizationResult ->
                if (authorizationResult.hasResolution()) {
                    val pendingIntent = authorizationResult.pendingIntent
                    if (pendingIntent == null) {
                        gmailState = GmailUiState(
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
                gmailState = GmailUiState(
                    label = "Gmail import failed",
                    error = if (apiError != null) {
                        gmailAuthErrorMessage(apiError, gmailSetupInfo)
                    } else {
                        error.localizedMessage ?: "Google authorization failed."
                    }
                )
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {},
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add transaction")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Header()
            }
            item {
                MonthSummary(feedState)
            }
            if (feedState.needsSmsPermission) {
                item {
                    PermissionPrompt(
                        onRequestPermission = {
                            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                        }
                    )
                }
            }
            item {
                GmailImportCard(
                    state = gmailState,
                    setupInfo = gmailSetupInfo,
                    onImport = { requestGmailImport() }
                )
            }
            item {
                SummaryRail(
                    title = "By merchant",
                    groups = feedState.transactions.monthMerchantGroups()
                )
            }
            item {
                SummaryRail(
                    title = "By category",
                    groups = feedState.transactions.monthCategoryGroups()
                )
            }
            item {
                SectionLabel("Recent")
            }
            items(feedState.transactions) { transaction ->
                TransactionRow(
                    transaction = transaction,
                    onClick = { selected = transaction }
                )
            }
            item {
                Spacer(modifier = Modifier.height(80.dp))
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
private fun Header() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Sorted",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )
            Text(
                text = "Transactions categorized privately",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                letterSpacing = 0.sp
            )
        }
        IconButton(onClick = {}) {
            Icon(Icons.Default.Search, contentDescription = "Search")
        }
        IconButton(onClick = {}) {
            Icon(Icons.Default.Menu, contentDescription = "Filters")
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
                        text = breakdown.monthKey?.monthOutflowLabel() ?: "Tracked outflow",
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
        }
    }
}

private fun List<TransactionUi>.monthBreakdown(): MonthBreakdown {
    val monthKey = mapNotNull { it.transactionDate?.take(7) }.maxOrNull()
    val monthTransactions = filter { monthKey == null || it.transactionDate?.startsWith(monthKey) == true }
    val debitTransactions = monthTransactions.filter { it.direction == DirectionUi.Debit }
    val spends = debitTransactions
        .filter { it.transactionType.countsAsSpend() }
        .sumOf { it.amountValue }
    val transfers = debitTransactions
        .filter { it.transactionType == TransactionType.TRANSFER }
        .sumOf { it.amountValue }
    val investments = debitTransactions
        .filter { it.transactionType == TransactionType.INVESTMENT }
        .sumOf { it.amountValue }

    return MonthBreakdown(
        monthKey = monthKey,
        debitCount = debitTransactions.size,
        totalDebits = debitTransactions.sumOf { it.amountValue },
        spends = spends,
        transfers = transfers,
        investments = investments
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
        .groupBy { it.merchant }
        .map { (merchant, transactions) ->
            SummaryGroup(
                label = merchant,
                count = transactions.size,
                total = transactions.sumOf { it.amountValue },
                category = transactions.firstOrNull()?.category ?: "Other"
            )
        }
        .sortedByDescending { it.total }
        .take(12)
}

private fun List<TransactionUi>.monthCategoryGroups(): List<SummaryGroup> {
    return latestMonthDebitTransactions()
        .groupBy { it.category }
        .map { (category, transactions) ->
            SummaryGroup(
                label = category,
                count = transactions.size,
                total = transactions.sumOf { it.amountValue },
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
    return "Imported $importedTransactions of $transactionsDetected detected. $skippedDuplicates matched SMS."
}

@Composable
private fun SummaryRail(
    title: String,
    groups: List<SummaryGroup>
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
                SummaryGroupCard(group)
            }
            item {
                Spacer(modifier = Modifier.width(10.dp))
            }
        }
    }
}

@Composable
private fun SummaryGroupCard(group: SummaryGroup) {
    Surface(
        modifier = Modifier.width(168.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryMiniDot(group.category)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = group.label,
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
                text = group.total.formatInr(),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${group.count} transaction${if (group.count == 1) "" else "s"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                letterSpacing = 0.sp
            )
        }
    }
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
private fun SummaryBreakdownRow(label: String, amount: Double) {
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
            text = amount.formatInr(),
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
    return when (category) {
        "Food" -> Color(0xFFFF9F7A)
        "Groceries" -> Color(0xFF9BE3B4)
        "Investment" -> Color(0xFF8EC9FF)
        "Refund" -> Color(0xFFFFCB77)
        "Subscriptions" -> Color(0xFFD3A4FF)
        "Reward" -> Color(0xFFFFE08A)
        "Transfer" -> Color(0xFFB8C2FF)
        "Shopping" -> Color(0xFFFFB4D8)
        "Health" -> Color(0xFFFF8E8E)
        "Entertainment" -> Color(0xFFC9B7FF)
        "Transport" -> Color(0xFF7DD3FC)
        "Utilities" -> Color(0xFFFFD166)
        "Fuel" -> Color(0xFFFFB86B)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
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
            fontWeight = FontWeight.Medium,
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
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp
        )
    }
}

private fun Double.formatInr(): String {
    return "INR " + String.format(Locale.US, "%,.2f", this)
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
    return "$monthName outflow"
}
