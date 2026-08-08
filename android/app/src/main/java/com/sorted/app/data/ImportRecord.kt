package com.sorted.app.data

import com.sorted.app.engine.ParsedTransaction

data class ImportRecord(
    val source: ImportSource,
    val sourceHash: String,
    val sourceReceivedDate: String?,
    val parsed: ParsedTransaction
)

