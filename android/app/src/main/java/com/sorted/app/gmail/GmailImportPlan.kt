package com.sorted.app.gmail

object GmailImportPlan {
    const val RequiredScope = "https://www.googleapis.com/auth/gmail.readonly"
    const val CandidateQuery = "newer_than:365d (INR OR Rs OR USD OR LRS OR remittance OR Vested OR debited OR credited OR spent OR deducted OR refund OR payment OR transaction OR invoice OR receipt OR UPI OR card)"
    const val MaxResults = 80
    const val MinImportConfidence = 0.65
}
