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
        """(\d{1,2}[-/]\d{1,2}[-/]\d{2,4})|(\d{1,2}\s+[A-Za-z]{3,9}\s+\d{2,4})"""
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

    fun parseCsvLine(headers: List<String>, values: List<String>): ParsedTransaction? {
        val map = headers.map { it.trim().lowercase() }.zip(values.map { it.trim() }).toMap()
        val amount = listOf("amount", "txn amount", "transaction amount", "debit", "credit", "value")
            .firstNotNullOfOrNull { key -> map[key]?.let { extractAmount(it) } }
            ?: values.firstNotNullOfOrNull { extractAmount(it) }
            ?: return null
        val desc = map["description"] ?: map["narration"] ?: map["particulars"]
            ?: map["merchant"] ?: map["details"] ?: values.getOrNull(1).orEmpty()
        val dateRaw = map["date"] ?: map["txn date"] ?: map["transaction date"] ?: values.firstOrNull().orEmpty()
        val date = extractDate(dateRaw) ?: Dates.now()
        val debit = map["debit"]?.let { extractAmount(it) }
        val credit = map["credit"]?.let { extractAmount(it) }
        val type = when {
            credit != null && credit > 0 && (debit == null || debit == 0.0) -> ParsedType.INCOME
            classify("", desc) == ParsedType.INVESTMENT -> ParsedType.INVESTMENT
            else -> ParsedType.EXPENSE
        }
        val usedAmount = when (type) {
            ParsedType.INCOME -> credit ?: amount
            else -> debit ?: amount
        }
        return ParsedTransaction(
            type = type,
            amount = usedAmount,
            party = desc.ifBlank { "Imported" },
            date = date,
            categoryHint = type.name,
            paymentMode = inferPaymentMode(desc),
            incomeCategory = inferIncomeCategory(desc),
            expenseCategory = inferExpenseCategory(desc, desc),
            notes = "Imported from statement",
            rawLine = values.joinToString(",")
        )
    }

    fun parseGenericTextLines(lines: List<String>): List<ParsedTransaction> {
        return lines.mapNotNull { line ->
            val amount = extractAmount(line) ?: return@mapNotNull null
            val type = classify("", line)
            ParsedTransaction(
                type = type,
                amount = amount,
                party = extractParty(line) ?: line.take(40),
                date = extractDate(line) ?: Dates.now(),
                categoryHint = type.name,
                paymentMode = inferPaymentMode(line),
                incomeCategory = inferIncomeCategory(line),
                expenseCategory = inferExpenseCategory(line, line),
                notes = "Imported from statement",
                rawLine = line
            )
        }
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
        val patterns = listOf("dd-MM-yyyy", "dd/MM/yyyy", "dd-MM-yy", "dd/MM/yy", "dd MMM yyyy", "d MMM yyyy")
        for (p in patterns) {
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
