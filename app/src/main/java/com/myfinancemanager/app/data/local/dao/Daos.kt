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
}

@Dao
interface InsightDao {
    @Query("SELECT * FROM ai_insights WHERE userId = :userId AND dismissed = 0 ORDER BY generatedAt DESC")
    fun observeActive(userId: String): Flow<List<InsightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<InsightEntity>)

    @Update
    suspend fun update(item: InsightEntity)

    @Query("DELETE FROM ai_insights WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE userId = :userId")
    fun observe(userId: String): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE userId = :userId AND category = :category")
    suspend fun delete(userId: String, category: ExpenseCategory)

    @Query("DELETE FROM budgets WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}

@Dao
interface SenderRuleDao {
    @Query("SELECT * FROM sender_rules WHERE userId = :userId ORDER BY sender")
    fun observe(userId: String): Flow<List<SenderRuleEntity>>

    @Query("SELECT * FROM sender_rules WHERE userId = :userId AND sender = :sender LIMIT 1")
    suspend fun get(userId: String, sender: String): SenderRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: SenderRuleEntity)

    @Query("DELETE FROM sender_rules WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM sender_rules WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}
