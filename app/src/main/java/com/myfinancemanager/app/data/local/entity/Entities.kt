package com.myfinancemanager.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class RecordOrigin {
    MANUAL, SMS, EMAIL, IMPORT
}

enum class RecordStatus {
    DRAFT, CONFIRMED
}

enum class IncomeCategory {
    SALARY, FREELANCE, INTEREST, RENTAL, OTHER
}

enum class ExpenseCategory {
    FOOD, TRAVEL, BILLS, SHOPPING, ENTERTAINMENT, HEALTH, EDUCATION, RENT, UTILITIES, OTHER
}

enum class PaymentMode {
    CASH, CARD, UPI, BANK_TRANSFER, OTHER
}

enum class InvestmentType {
    MUTUAL_FUND, STOCK, FD, BOND, GOLD, RETIREMENT, CRYPTO, OTHER
}

enum class AutoCaptureStatus {
    PENDING, CONFIRMED, REJECTED
}

enum class ParsedType {
    INCOME, EXPENSE, INVESTMENT
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val displayName: String,
    val authProvider: String,
    val passwordHash: String?,
    val createdAt: Long,
    val currencyCode: String = "INR"
)

@Entity(
    tableName = "income_records",
    indices = [Index(value = ["userId", "date"]), Index(value = ["fingerprint"], unique = true)]
)
data class IncomeEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val amount: Double,
    val source: String,
    val category: IncomeCategory,
    val date: Long,
    val origin: RecordOrigin,
    val notes: String,
    val recurring: Boolean,
    val status: RecordStatus,
    val fingerprint: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "expense_records",
    indices = [Index(value = ["userId", "date"]), Index(value = ["fingerprint"], unique = true)]
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val amount: Double,
    val merchant: String,
    val category: ExpenseCategory,
    val paymentMode: PaymentMode,
    val date: Long,
    val origin: RecordOrigin,
    val notes: String,
    val recurring: Boolean,
    val status: RecordStatus,
    val fingerprint: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "investment_records",
    indices = [Index(value = ["userId", "date"]), Index(value = ["fingerprint"], unique = true)]
)
data class InvestmentEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val instrumentName: String,
    val type: InvestmentType,
    val amountInvested: Double,
    val currentValue: Double,
    val date: Long,
    val broker: String,
    val origin: RecordOrigin,
    val notes: String,
    val status: RecordStatus,
    val fingerprint: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "auto_capture_queue")
data class AutoCaptureEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val rawText: String,
    val sender: String,
    val parsedType: ParsedType,
    val parsedAmount: Double?,
    val parsedParty: String?,
    val parsedDate: Long?,
    val parsedCategory: String?,
    val status: AutoCaptureStatus,
    val createdAt: Long
)

@Entity(tableName = "import_batches")
data class ImportBatchEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val sourceFile: String,
    val status: String,
    val totalParsed: Int,
    val committed: Int,
    val createdAt: Long
)

@Entity(tableName = "ai_insights")
data class InsightEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val insightText: String,
    val category: String,
    val generatedAt: Long,
    val dismissed: Boolean = false,
    val saved: Boolean = false
)

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["userId", "category"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val category: ExpenseCategory,
    val monthlyLimit: Double,
    val updatedAt: Long
)

@Entity(tableName = "sender_rules")
data class SenderRuleEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val sender: String,
    val allowed: Boolean
)
