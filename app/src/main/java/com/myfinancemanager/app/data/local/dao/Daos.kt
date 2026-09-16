package com.myfinancemanager.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.myfinancemanager.app.data.local.entity.AutoCaptureEntity
import com.myfinancemanager.app.data.local.entity.AutoCaptureStatus
import com.myfinancemanager.app.data.local.entity.BudgetEntity
import com.myfinancemanager.app.data.local.entity.ExpenseCategory
import com.myfinancemanager.app.data.local.entity.ExpenseEntity
import com.myfinancemanager.app.data.local.entity.ImportBatchEntity
import com.myfinancemanager.app.data.local.entity.IncomeEntity
import com.myfinancemanager.app.data.local.entity.InsightEntity
import com.myfinancemanager.app.data.local.entity.InvestmentEntity
import com.myfinancemanager.app.data.local.entity.RecordStatus
import com.myfinancemanager.app.data.local.entity.SenderRuleEntity
import com.myfinancemanager.app.data.local.entity.SyncTombstoneEntity
import com.myfinancemanager.app.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface IncomeDao {
    @Query("SELECT * FROM income_records WHERE userId = :userId AND status = :status ORDER BY date DESC")
    fun observe(userId: String, status: RecordStatus): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM income_records WHERE userId = :userId AND status = :status AND date BETWEEN :from AND :to ORDER BY date DESC")
    fun observeInRange(userId: String, from: Long, to: Long, status: RecordStatus): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM income_records WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): IncomeEntity?

    @Query("SELECT COUNT(*) FROM income_records WHERE fingerprint = :fingerprint")
    suspend fun countFingerprint(fingerprint: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: IncomeEntity)

    @Update
    suspend fun update(record: IncomeEntity)

    @Query("DELETE FROM income_records WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM income_records WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)

    // ---- Sync ------------------------------------------------------------------------

    /** Rows with local changes waiting to be pushed to the backend. */
    @Query("SELECT * FROM income_records WHERE userId = :userId AND dirty = 1")
    suspend fun pendingSync(userId: String): List<IncomeEntity>

    @Query("SELECT * FROM income_records WHERE remoteId = :remoteId LIMIT 1")
    suspend fun findByRemoteId(remoteId: String): IncomeEntity?

    @Query("UPDATE income_records SET remoteId = :remoteId, dirty = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markSynced(id: String, remoteId: String, updatedAt: Long)

    /** Drops the dirty flag without a remote id — used when the server rejects a payload forever. */
    @Query("UPDATE income_records SET dirty = 0 WHERE id = :id")
    suspend fun markClean(id: String)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expense_records WHERE userId = :userId AND status = :status ORDER BY date DESC")
    fun observe(userId: String, status: RecordStatus): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expense_records WHERE userId = :userId AND status = :status AND date BETWEEN :from AND :to ORDER BY date DESC")
    fun observeInRange(userId: String, from: Long, to: Long, status: RecordStatus): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expense_records WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ExpenseEntity?

    @Query("SELECT COUNT(*) FROM expense_records WHERE fingerprint = :fingerprint")
    suspend fun countFingerprint(fingerprint: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: ExpenseEntity)

    @Update
    suspend fun update(record: ExpenseEntity)

    @Query("DELETE FROM expense_records WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM expense_records WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)

    // ---- Sync ------------------------------------------------------------------------

    @Query("SELECT * FROM expense_records WHERE userId = :userId AND dirty = 1")
    suspend fun pendingSync(userId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expense_records WHERE remoteId = :remoteId LIMIT 1")
    suspend fun findByRemoteId(remoteId: String): ExpenseEntity?

    @Query("UPDATE expense_records SET remoteId = :remoteId, dirty = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markSynced(id: String, remoteId: String, updatedAt: Long)

    @Query("UPDATE expense_records SET dirty = 0 WHERE id = :id")
    suspend fun markClean(id: String)
}

@Dao
interface InvestmentDao {
    @Query("SELECT * FROM investment_records WHERE userId = :userId AND status = :status ORDER BY date DESC")
    fun observe(userId: String, status: RecordStatus): Flow<List<InvestmentEntity>>

    @Query("SELECT * FROM investment_records WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): InvestmentEntity?

    @Query("SELECT COUNT(*) FROM investment_records WHERE fingerprint = :fingerprint")
    suspend fun countFingerprint(fingerprint: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: InvestmentEntity)

    @Update
    suspend fun update(record: InvestmentEntity)

    @Query("DELETE FROM investment_records WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM investment_records WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)

    // ---- Sync ------------------------------------------------------------------------

    @Query("SELECT * FROM investment_records WHERE userId = :userId AND dirty = 1")
    suspend fun pendingSync(userId: String): List<InvestmentEntity>

    @Query("SELECT * FROM investment_records WHERE remoteId = :remoteId LIMIT 1")
    suspend fun findByRemoteId(remoteId: String): InvestmentEntity?

    @Query("UPDATE investment_records SET remoteId = :remoteId, dirty = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markSynced(id: String, remoteId: String, updatedAt: Long)

    @Query("UPDATE investment_records SET dirty = 0 WHERE id = :id")
    suspend fun markClean(id: String)
}

@Dao
interface AutoCaptureDao {
    @Query("SELECT * FROM auto_capture_queue WHERE userId = :userId AND status = :status ORDER BY createdAt DESC")
    fun observeByStatus(userId: String, status: AutoCaptureStatus): Flow<List<AutoCaptureEntity>>

    @Query("SELECT * FROM auto_capture_queue WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AutoCaptureEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: AutoCaptureEntity)

    @Update
    suspend fun update(item: AutoCaptureEntity)

    @Query("DELETE FROM auto_capture_queue WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)

    @Query("SELECT COUNT(*) FROM auto_capture_queue WHERE userId = :userId AND status = :status")
    fun observeCount(userId: String, status: AutoCaptureStatus): Flow<Int>

    // ---- Sync ------------------------------------------------------------------------

    /** Captures whose current state (new item, or review decision) has not been pushed yet. */
    @Query("SELECT * FROM auto_capture_queue WHERE userId = :userId AND dirty = 1")
    suspend fun pendingSync(userId: String): List<AutoCaptureEntity>

    @Query("SELECT * FROM auto_capture_queue WHERE remoteId = :remoteId LIMIT 1")
    suspend fun findByRemoteId(remoteId: String): AutoCaptureEntity?

    /**
     * Stores the server id of a freshly submitted capture while leaving the row dirty: the id is
     * needed to replay a confirm/reject, and losing it would make the next pass submit the same
     * capture twice.
     */
    @Query("UPDATE auto_capture_queue SET remoteId = :remoteId WHERE id = :id")
    suspend fun setRemoteId(id: String, remoteId: String)

    @Query("UPDATE auto_capture_queue SET remoteId = :remoteId, dirty = 0 WHERE id = :id")
    suspend fun markSynced(id: String, remoteId: String)

    /** Drops the dirty flag without a remote id — used when the server will never accept a payload. */
    @Query("UPDATE auto_capture_queue SET dirty = 0 WHERE id = :id")
    suspend fun markClean(id: String)
}

@Dao
interface ImportBatchDao {
    @Query("SELECT * FROM import_batches WHERE userId = :userId ORDER BY createdAt DESC")
    fun observe(userId: String): Flow<List<ImportBatchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(batch: ImportBatchEntity)

    @Update
    suspend fun update(batch: ImportBatchEntity)

    @Query("DELETE FROM import_batches WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)

    // ---- Sync ------------------------------------------------------------------------

    /** Batches whose statement file still has to be uploaded to the backend. */
    @Query("SELECT * FROM import_batches WHERE userId = :userId AND dirty = 1")
    suspend fun pendingSync(userId: String): List<ImportBatchEntity>

    @Query("SELECT * FROM import_batches WHERE remoteId = :remoteId LIMIT 1")
    suspend fun findByRemoteId(remoteId: String): ImportBatchEntity?

    @Query("UPDATE import_batches SET remoteId = :remoteId, dirty = 0 WHERE id = :id")
    suspend fun markSynced(id: String, remoteId: String)

    @Query("UPDATE import_batches SET dirty = 0 WHERE id = :id")
    suspend fun markClean(id: String)
}

@Dao
interface InsightDao {
    @Query("SELECT * FROM ai_insights WHERE userId = :userId AND dismissed = 0 ORDER BY generatedAt DESC")
    fun observeActive(userId: String): Flow<List<InsightEntity>>

    @Query("SELECT * FROM ai_insights WHERE userId = :userId")
    suspend fun all(userId: String): List<InsightEntity>

    @Query("SELECT * FROM ai_insights WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): InsightEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: InsightEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<InsightEntity>)

    @Update
    suspend fun update(item: InsightEntity)

    // ---- Sync ------------------------------------------------------------------------

    /** Insights with a save/dismiss decision that has not reached the account yet. */
    @Query("SELECT * FROM ai_insights WHERE userId = :userId AND dirty = 1")
    suspend fun pendingSync(userId: String): List<InsightEntity>

    @Query("UPDATE ai_insights SET dirty = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("DELETE FROM ai_insights WHERE id = :id")
    suspend fun delete(id: String)

    /**
     * Drops rows the account no longer has. Local decisions that are still queued are left alone,
     * which is why the caller must never pass an empty list (`NOT IN ()` is not valid SQL).
     */
    @Query("DELETE FROM ai_insights WHERE userId = :userId AND dirty = 0 AND id NOT IN (:keep)")
    suspend fun deleteMissing(userId: String, keep: List<String>)

    @Query("DELETE FROM ai_insights WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE userId = :userId")
    fun observe(userId: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE userId = :userId AND category = :category")
    suspend fun delete(userId: String, category: ExpenseCategory)

    @Query("DELETE FROM budgets WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)

    // ---- Sync ------------------------------------------------------------------------

    /** Budgets whose limit has not been pushed to the account. */
    @Query("SELECT * FROM budgets WHERE userId = :userId AND dirty = 1")
    suspend fun pendingSync(userId: String): List<BudgetEntity>

    @Query("UPDATE budgets SET remoteId = :remoteId, dirty = 0 WHERE id = :id")
    suspend fun markSynced(id: String, remoteId: String)

    @Query("UPDATE budgets SET dirty = 0 WHERE id = :id")
    suspend fun markClean(id: String)
}

@Dao
interface SenderRuleDao {
    @Query("SELECT * FROM sender_rules WHERE userId = :userId ORDER BY sender")
    fun observe(userId: String): Flow<List<SenderRuleEntity>>

    @Query("SELECT * FROM sender_rules WHERE userId = :userId ORDER BY sender")
    suspend fun rules(userId: String): List<SenderRuleEntity>

    @Query("SELECT * FROM sender_rules WHERE userId = :userId AND sender = :sender LIMIT 1")
    suspend fun get(userId: String, sender: String): SenderRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: SenderRuleEntity)

    @Query("DELETE FROM sender_rules WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM sender_rules WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}

@Dao
interface SyncDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(tombstone: SyncTombstoneEntity)

    @Query("SELECT * FROM sync_tombstones WHERE userId = :userId")
    suspend fun pending(userId: String): List<SyncTombstoneEntity>

    @Query("DELETE FROM sync_tombstones WHERE id = :id")
    suspend fun remove(id: String)

    @Query("DELETE FROM sync_tombstones WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}
