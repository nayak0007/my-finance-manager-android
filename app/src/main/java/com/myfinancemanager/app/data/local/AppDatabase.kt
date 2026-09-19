package com.myfinancemanager.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.myfinancemanager.app.data.local.dao.AutoCaptureDao
import com.myfinancemanager.app.data.local.dao.BudgetDao
import com.myfinancemanager.app.data.local.dao.ExpenseDao
import com.myfinancemanager.app.data.local.dao.ImportBatchDao
import com.myfinancemanager.app.data.local.dao.IncomeDao
import com.myfinancemanager.app.data.local.dao.InsightDao
import com.myfinancemanager.app.data.local.dao.InvestmentDao
import com.myfinancemanager.app.data.local.dao.SenderRuleDao
import com.myfinancemanager.app.data.local.dao.SyncDao
import com.myfinancemanager.app.data.local.dao.UserDao
import com.myfinancemanager.app.data.local.entity.AutoCaptureEntity
import com.myfinancemanager.app.data.local.entity.BudgetEntity
import com.myfinancemanager.app.data.local.entity.ExpenseEntity
import com.myfinancemanager.app.data.local.entity.ImportBatchEntity
import com.myfinancemanager.app.data.local.entity.IncomeEntity
import com.myfinancemanager.app.data.local.entity.InsightEntity
import com.myfinancemanager.app.data.local.entity.InvestmentEntity
import com.myfinancemanager.app.data.local.entity.SenderRuleEntity
import com.myfinancemanager.app.data.local.entity.SyncTombstoneEntity
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
        SenderRuleEntity::class,
        SyncTombstoneEntity::class
    ],
    version = 5,
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
    abstract fun syncDao(): SyncDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /**
         * v1 -> v2: adds the two-way sync bookkeeping. Existing rows are stamped
         * `dirty = 1` so the first sync after upgrading pushes everything the user already
         * had on the device up to the backend instead of leaving it stranded locally.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                listOf("income_records", "expense_records", "investment_records").forEach { table ->
                    db.execSQL("ALTER TABLE $table ADD COLUMN remoteId TEXT")
                    db.execSQL("ALTER TABLE $table ADD COLUMN dirty INTEGER NOT NULL DEFAULT 1")
                }
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS sync_tombstones (" +
                        "id TEXT NOT NULL, " +
                        "userId TEXT NOT NULL, " +
                        "kind TEXT NOT NULL, " +
                        "remoteId TEXT, " +
                        "PRIMARY KEY(id))"
                )
            }
        }

        /**
         * v2 -> v3: adds the sync bookkeeping for the SMS review queue and statement imports.
         *
         * Captures that were still pending are flagged dirty so they get mirrored to the backend.
         * Captures already reviewed on this device are deliberately left clean: their transaction
         * was written locally by the old build, and replaying the review server-side would create a
         * second copy of each one. Imports made before this build have no private copy of the
         * statement file, so there is nothing to upload and they stay clean too.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // The column default has to stay "1" to match the entity's declaration; the
                // rows that predate mirroring are then cleared individually below.
                db.execSQL("ALTER TABLE auto_capture_queue ADD COLUMN remoteId TEXT")
                db.execSQL("ALTER TABLE auto_capture_queue ADD COLUMN dirty INTEGER NOT NULL DEFAULT 1")
                db.execSQL("UPDATE auto_capture_queue SET dirty = 0 WHERE status != 'PENDING'")

                db.execSQL("ALTER TABLE import_batches ADD COLUMN remoteId TEXT")
                db.execSQL("ALTER TABLE import_batches ADD COLUMN localPath TEXT")
                db.execSQL("ALTER TABLE import_batches ADD COLUMN contentType TEXT")
                db.execSQL("ALTER TABLE import_batches ADD COLUMN fileSize INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE import_batches ADD COLUMN dirty INTEGER NOT NULL DEFAULT 1")
                db.execSQL("UPDATE import_batches SET dirty = 0")
            }
        }

        /**
         * v3 -> v4: extends the mirror to budgets and insights.
         *
         * Budgets that already exist on the device are flagged dirty so the first sync pushes them
         * into the account. Insights the on-device engine produced are dropped: the backend now
         * generates them and keys them by its own UUID, so those rows have no server counterpart
         * to reconcile with and are replaced on the next refresh.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE budgets ADD COLUMN remoteId TEXT")
                db.execSQL("ALTER TABLE budgets ADD COLUMN dirty INTEGER NOT NULL DEFAULT 1")

                db.execSQL("ALTER TABLE ai_insights ADD COLUMN title TEXT")
                db.execSQL("ALTER TABLE ai_insights ADD COLUMN dirty INTEGER NOT NULL DEFAULT 0")
                db.execSQL("DELETE FROM ai_insights")
            }
        }

        /**
         * v4 -> v5: statement imports become server-driven.
         *
         * The phone no longer parses statements; the backend does (OpenRouter over text it
         * extracts itself). The row gains the parse error the server reports. Pre-existing batches
         * were committed locally by their old build and never mirrored, so they keep their
         * clean flag and history as-is.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE import_batches ADD COLUMN errorMessage TEXT")
            }
        }

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "my_finance_manager.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration().build().also { instance = it }
            }
        }
    }
}
