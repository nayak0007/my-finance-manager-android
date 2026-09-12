package com.myfinancemanager.app.di

import android.content.Context
import com.myfinancemanager.app.data.local.AppDatabase
import com.myfinancemanager.app.data.parser.StatementImporter
import com.myfinancemanager.app.data.prefs.UserPreferences
import com.myfinancemanager.app.data.repository.AuthRepository
import com.myfinancemanager.app.data.repository.FinanceRepository
import com.myfinancemanager.app.data.session.SessionStore

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val db = AppDatabase.get(appContext)

    val sessionStore = SessionStore(appContext)
    val userPreferences = UserPreferences(appContext)
    val statementImporter = StatementImporter(appContext)

    val authRepository = AuthRepository(
        userDao = db.userDao(),
        sessionStore = sessionStore
    )

    val financeRepository = FinanceRepository(
        incomeDao = db.incomeDao(),
        expenseDao = db.expenseDao(),
        investmentDao = db.investmentDao(),
        autoCaptureDao = db.autoCaptureDao(),
        importBatchDao = db.importBatchDao(),
        insightDao = db.insightDao(),
        budgetDao = db.budgetDao(),
        senderRuleDao = db.senderRuleDao()
    )
}
