package com.myfinancemanager.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.myfinancemanager.app.util.Ids

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

/**
 * Primary key of a budget row. A user has at most one budget per category, so the key can be
 * derived from the pair — which is what lets a pulled budget land on the row it was pushed from
 * instead of creating a second one.
 */
fun budgetIdFor(userId: String, category: ExpenseCategory): String =
    Ids.fingerprint(listOf(userId, category.name))

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
    val updatedAt: Long,
    /** Server UUID once this row has been pushed; null while it only exists on this device. */
    val remoteId: String? = null,
    /** True while the row holds local changes that have not been pushed to the server. */
    @ColumnInfo(defaultValue = "1")
    val dirty: Boolean = true
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
    val updatedAt: Long,
    /** Server UUID once this row has been pushed; null while it only exists on this device. */
    val remoteId: String? = null,
    /** True while the row holds local changes that have not been pushed to the server. */
    @ColumnInfo(defaultValue = "1")
    val dirty: Boolean = true
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
    val updatedAt: Long,
    /** Server UUID once this row has been pushed; null while it only exists on this device. */
    val remoteId: String? = null,
    /** True while the row holds local changes that have not been pushed to the server. */
    @ColumnInfo(defaultValue = "1")
    val dirty: Boolean = true
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
    val createdAt: Long,
    /** Server UUID once this capture has been mirrored; null while it only exists on this device. */
    val remoteId: String? = null,
    /**
     * True while the local state has not reached the server — either the capture itself, or the
     * review decision taken on it. The backend writes the transaction when an item is confirmed,
     * so a decision has to be replayed there rather than applied locally only.
     */
    @ColumnInfo(defaultValue = "1")
    val dirty: Boolean = true
)

@Entity(tableName = "import_batches")
data class ImportBatchEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val sourceFile: String,
    val status: String,
    val totalParsed: Int,
    val committed: Int,
    val createdAt: Long,
    /** Server UUID of the mirrored batch, set once the statement file has been uploaded. */
    val remoteId: String? = null,
    /** Private on-device copy of the statement, kept so a failed upload can be retried. */
    val localPath: String? = null,
    val contentType: String? = null,
    @ColumnInfo(defaultValue = "0")
    val fileSize: Long = 0,
    /** True while the statement file still has to be uploaded to the backend. */
    @ColumnInfo(defaultValue = "1")
    val dirty: Boolean = true
)

/**
 * An insight generated by the backend from the account's own records. The primary key *is* the
 * server UUID — there is no on-device generator any more, so every row has a server identity.
 */
@Entity(tableName = "ai_insights")
data class InsightEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String? = null,
    val insightText: String,
    val category: String,
    val generatedAt: Long,
    val dismissed: Boolean = false,
    val saved: Boolean = false,
    /** True while a save/dismiss decision has not been pushed to the account. */
    @ColumnInfo(defaultValue = "0")
    val dirty: Boolean = false
)

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["userId", "category"], unique = true)]
)
data class BudgetEntity(
    /** Derived from (user, category) so a pulled budget lands on the row it was pushed from. */
    @PrimaryKey val id: String,
    val userId: String,
    val category: ExpenseCategory,
    val monthlyLimit: Double,
    val updatedAt: Long,
    val remoteId: String? = null,
    /** True while the limit has not been pushed to the account. */
    @ColumnInfo(defaultValue = "1")
    val dirty: Boolean = true
)

@Entity(tableName = "sender_rules")
data class SenderRuleEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val sender: String,
    val allowed: Boolean
)

/**
 * A record that was deleted on this device but may still exist on the server.
 *
 * Deleting locally is immediate so the UI never blocks, but the remote copy has to be
 * removed too — otherwise the next pull would resurrect the record. The tombstone is kept
 * until the remote delete succeeds (including when the server answers 404, meaning the
 * record is already gone).
 */
@Entity(tableName = "sync_tombstones")
data class SyncTombstoneEntity(
    /** Local id of the deleted record; unique per record so re-deletes collapse. */
    @PrimaryKey val id: String,
    val userId: String,
    /** One of INCOME / EXPENSE / INVESTMENT. */
    val kind: String,
    /** Null when the record was deleted before it ever reached the server. */
    val remoteId: String?
)

/** The record families that take part in two-way sync. */
enum class SyncKind {
    INCOME, EXPENSE, INVESTMENT
}
