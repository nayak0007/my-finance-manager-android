package com.myfinancemanager.app.data.parser

import com.myfinancemanager.app.data.local.entity.ExpenseCategory
import com.myfinancemanager.app.data.local.entity.IncomeCategory
import com.myfinancemanager.app.data.local.entity.ParsedType
import com.myfinancemanager.app.data.local.entity.PaymentMode
import com.myfinancemanager.app.util.Dates
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ParsedTransaction(
    val type: ParsedType,
    val amount: Double,
    val party: String,
    val date: Long,
    val categoryHint: String?,
    val paymentMode: PaymentMode = PaymentMode.OTHER,
    val incomeCategory: IncomeCategory = IncomeCategory.OTHER,
    val expenseCategory: ExpenseCategory = ExpenseCategory.OTHER,
    val notes: String = "",
    val rawLine: String = ""
)

/**
 * Parses bank notification SMSes into transactions for the auto-capture review queue.
 *
 * Statement files (PDF/CSV) are deliberately NOT parsed here any more: import runs on the
 * backend (OpenRouter over the text it extracts) via [ImportManager], which also fixed the
 * mis-parses the old on-device guesser produced.
 */
object TransactionParser {
    private val amountRegex = Regex(
        """(?:INR|Rs\.?|₹|USD|\$)\s*([0-9]+(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?)|([0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )
    private val debitKeywords = listOf(
        "debited", "debit", "spent", "purchase", "paid", "withdrawn", "payment",
        "upi", "pos", "swiped", "charged"
    )
    private val creditKeywords = listOf(
        "credited", "credit", "received", "salary", "deposit", "refund", "interest"
    )
    private val investmentKeywords = listOf(
        "mutual fund", "sip", "nse", "bse", "demat", "broker", "gold", "fd booked", "equity"
    )
    private val merchantRegex = Regex(
        """(?:at|to|from|towards)\s+([A-Za-z0-9 &._-]{3,40})""",
        RegexOption.IGNORE_CASE
    )
    private val dateRegex = Regex(
        """(\d{4}[-/]\d{1,2}[-/]\d{1,2})|(\d{1,2}[-/]\d{1,2}[-/]\d{2,4})|(\d{1,2}[- ][A-Za-z]{3,9}[- ]\d{2,4})|([A-Za-z]{3,9}\s+\d{1,2},?\s+\d{2,4})"""
    )

    /**
     * Parse orders seen in real bank exports. ISO (`2026-09-12`) must come first so the
     * day-first patterns never split an ISO date into `2026` + `09` + `12`.
     */
    private val datePatterns = listOf(
        "yyyy-MM-dd", "yyyy/MM/dd", "dd-MMM-yyyy", "dd/MMM/yyyy", "dd MMM yyyy", "d MMM yyyy",
        "dd-MMM-yy", "dd/MMM/yy", "MMM dd, yyyy", "MMM d, yyyy", "MMM dd yyyy",
        "dd-MM-yyyy", "dd/MM/yyyy", "dd-MM-yy", "dd/MM/yy", "d/M/yyyy", "d-M-yyyy"
    )

    fun parseSms(sender: String, body: String, receivedAt: Long = Dates.now()): ParsedTransaction? {
        val text = body.replace('\n', ' ')
        val amount = extractAmount(text) ?: return null
        val type = classify(sender, text)
        val party = extractParty(text) ?: sender.ifBlank { "Unknown" }
        val date = extractDate(text) ?: receivedAt
        return ParsedTransaction(
            type = type,
            amount = amount,
            party = party.trim(),
            date = date,
            categoryHint = type.name,
            paymentMode = inferPaymentMode(text),
            incomeCategory = inferIncomeCategory(text),
            expenseCategory = inferExpenseCategory(text, party),
            notes = "Auto-detected from SMS ($sender)",
            rawLine = text
        )
    }

    private fun classify(sender: String, text: String): ParsedType {
        val lower = "$sender $text".lowercase()
        if (investmentKeywords.any { lower.contains(it) }) return ParsedType.INVESTMENT
        val creditHit = creditKeywords.any { lower.contains(it) }
        val debitHit = debitKeywords.any { lower.contains(it) }
        return when {
            creditHit && !debitHit -> ParsedType.INCOME
            else -> ParsedType.EXPENSE
        }
    }

    private fun extractAmount(text: String): Double? {
        val match = amountRegex.find(text) ?: return null
        val raw = (match.groupValues.getOrNull(1).orEmpty().ifBlank {
            match.groupValues.getOrNull(2).orEmpty()
        }).replace(",", "")
        return raw.toDoubleOrNull()?.takeIf { it > 0 }
    }

    private fun extractParty(text: String): String? {
        return merchantRegex.find(text)?.groupValues?.getOrNull(1)?.trim()?.trimEnd('.', '-')
    }

    private fun extractDate(text: String): Long? {
        val raw = dateRegex.find(text)?.value ?: return null
        for (p in datePatterns) {
            runCatching {
                val parsed = LocalDate.parse(raw, DateTimeFormatter.ofPattern(p, Locale.ENGLISH))
                return Dates.toEpoch(parsed)
            }
        }
        return null
    }

    private fun inferPaymentMode(text: String): PaymentMode {
        val lower = text.lowercase()
        return when {
            "upi" in lower -> PaymentMode.UPI
            "card" in lower || "visa" in lower || "mastercard" in lower -> PaymentMode.CARD
            "neft" in lower || "imps" in lower || "rtgs" in lower || "transfer" in lower -> PaymentMode.BANK_TRANSFER
            "cash" in lower -> PaymentMode.CASH
            else -> PaymentMode.OTHER
        }
    }

    private fun inferIncomeCategory(text: String): IncomeCategory {
        val lower = text.lowercase()
        return when {
            "salary" in lower -> IncomeCategory.SALARY
            "freelance" in lower || "invoice" in lower -> IncomeCategory.FREELANCE
            "interest" in lower -> IncomeCategory.INTEREST
            "rent" in lower -> IncomeCategory.RENTAL
            else -> IncomeCategory.OTHER
        }
    }

    fun inferExpenseCategory(text: String, party: String): ExpenseCategory {
        val lower = "$text $party".lowercase()
        return when {
            listOf("swiggy", "zomato", "restaurant", "cafe", "food", "grocery", "blinkit").any { it in lower } -> ExpenseCategory.FOOD
            listOf("uber", "ola", "irctc", "flight", "hotel", "travel", "fuel").any { it in lower } -> ExpenseCategory.TRAVEL
            listOf("electric", "water", "gas", "broadband", "airtel", "jio", "utility").any { it in lower } -> ExpenseCategory.UTILITIES
            listOf("amazon", "flipkart", "myntra", "store", "mall").any { it in lower } -> ExpenseCategory.SHOPPING
            listOf("netflix", "spotify", "movie", "bookmyshow").any { it in lower } -> ExpenseCategory.ENTERTAINMENT
            listOf("hospital", "pharmacy", "apollo", "health").any { it in lower } -> ExpenseCategory.HEALTH
            listOf("school", "udemy", "course", "tuition").any { it in lower } -> ExpenseCategory.EDUCATION
            listOf("rent", "housing").any { it in lower } -> ExpenseCategory.RENT
            listOf("bill", "recharge", "insurance").any { it in lower } -> ExpenseCategory.BILLS
            else -> ExpenseCategory.OTHER
        }
    }
}
