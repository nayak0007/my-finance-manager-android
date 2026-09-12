package com.myfinancemanager.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.myfinancemanager.app.data.local.dao.AutoCaptureDao
import com.myfinancemanager.app.data.local.dao.BudgetDao
import com.myfinancemanager.app.data.local.dao.ExpenseDao
import com.myfinancemanager.app.data.local.dao.ImportBatchDao
import com.myfinancemanager.app.data.local.dao.IncomeDao
import com.myfinancemanager.app.data.local.dao.InsightDao
import com.myfinancemanager.app.data.local.dao.InvestmentDao
import com.myfinancemanager.app.data.local.dao.SenderRuleDao
import com.myfinancemanager.app.data.local.dao.UserDao
import com.myfinancemanager.app.data.local.entity.AutoCaptureEntity
import com.myfinancemanager.app.data.local.entity.BudgetEntity
import com.myfinancemanager.app.data.local.entity.ExpenseEntity
import com.myfinancemanager.app.data.local.entity.ImportBatchEntity
import com.myfinancemanager.app.data.local.entity.IncomeEntity
import com.myfinancemanager.app.data.local.entity.InsightEntity
import com.myfinancemanager.app.data.local.entity.InvestmentEntity
import com.myfinancemanager.app.data.local.entity.SenderRuleEntity
import com.myfinancemanager.app.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        IncomeEntity::class,
        ExpenseEntity::class,
        InvestmentEntity::class,
        AutoCaptureEntity::class,
        ImportBatchEntity::class,
        InsightEntity::class,
        BudgetEntity::class,
        SenderRuleEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun incomeDao(): IncomeDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun investmentDao(): InvestmentDao
    abstract fun autoCaptureDao(): AutoCaptureDao
    abstract fun importBatchDao(): ImportBatchDao
    abstract fun insightDao(): InsightDao
    abstract fun budgetDao(): BudgetDao
    abstract fun senderRuleDao(): SenderRuleDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "my_finance_manager.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
        }
    }
}
