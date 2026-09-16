package com.myfinancemanager.app.di

import android.content.Context
import com.myfinancemanager.app.data.local.AppDatabase
import com.myfinancemanager.app.data.local.StatementStore
import com.myfinancemanager.app.data.parser.StatementImporter
import com.myfinancemanager.app.data.prefs.UserPreferences
import com.myfinancemanager.app.data.remote.ApiClient
import com.myfinancemanager.app.data.remote.FinanceApi
import com.myfinancemanager.app.data.remote.NeonAuthClient
import com.myfinancemanager.app.data.remote.NeonAuthApi
import com.myfinancemanager.app.data.repository.AuthRepository
import com.myfinancemanager.app.data.repository.FinanceRepository
import com.myfinancemanager.app.data.session.NeonAuthCookieJar
import com.myfinancemanager.app.data.session.SessionStore
import com.myfinancemanager.app.data.sync.SyncEngine

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val db = AppDatabase.get(appContext)

    val sessionStore = SessionStore(appContext)
    val userPreferences = UserPreferences(appContext)
    val statementImporter = StatementImporter(appContext)

    /** Private copies of imported statements, kept so their upload can be retried. */
    val statementStore = StatementStore(appContext)

    /**
     * Persists Neon Auth's session cookie, which is what allows a new JWT to be minted without
     * asking the user to sign in again.
     */
    private val neonAuthCookieJar = NeonAuthCookieJar(appContext)

    /**
     * The credential authority. Sign-up, sign-in, JWT minting and sign-out all happen here, on a
     * client with no authenticator of its own so a credential failure can never be retried as an
     * expiry.
     */
    private val neonAuthApi: NeonAuthApi = NeonAuthClient.create(neonAuthCookieJar)

    /**
     * API for our backend, used by [syncEngine] and [authRepository]. The bearer token is the
     * Neon Auth JWT; a 401 triggers TokenAuthenticator, which single-flight mints a fresh JWT
     * from Neon Auth and retries once, clearing the session only if the session itself is gone.
     */
    val financeApi: FinanceApi = ApiClient.create(
        { sessionStore.currentSync()?.accessToken },
        sessionStore,
        neonAuthApi
    )

    val authRepository = AuthRepository(
        userDao = db.userDao(),
        sessionStore = sessionStore,
        neonAuth = neonAuthApi,
        cookieJar = neonAuthCookieJar,
        financeApi = financeApi
    )

    /**
     * Reconciles Room with the backend: records, the SMS review queue, statement imports, budgets,
     * settings and insights.
     */
    val syncEngine = SyncEngine(financeApi, db, userPreferences)

    val financeRepository = FinanceRepository(
        incomeDao = db.incomeDao(),
        expenseDao = db.expenseDao(),
        investmentDao = db.investmentDao(),
        autoCaptureDao = db.autoCaptureDao(),
        importBatchDao = db.importBatchDao(),
        insightDao = db.insightDao(),
        budgetDao = db.budgetDao(),
        senderRuleDao = db.senderRuleDao(),
        syncDao = db.syncDao(),
        preferences = userPreferences
    )
}
