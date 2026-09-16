package com.myfinancemanager.app.data.remote

import com.myfinancemanager.app.BuildConfig
import com.myfinancemanager.app.data.session.SessionStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * The signed-in user's profile, matching the backend's UserResponse.
 *
 * Credentials are not handled here. Sign-up, sign-in and JWT minting all happen against Neon
 * Auth (see [NeonAuthApi]); this backend only ever sees the resulting JWT.
 */
data class AuthUser(
    val id: String,
    val email: String,
    val fullName: String? = null,
    val currency: String? = null,
    /** Account-level notification switch; the source of truth for the Settings toggle. */
    val notificationsEnabled: Boolean? = null,
    /** Free-form account preferences, currently `insightFrequencyDays`. */
    val preferences: Map<String, Any?>? = null
)

/**
 * Error payload returned by the backend for every non-2xx response
 * (com.myfinancemanager.common.dto.ApiError).
 */
data class ApiErrorDto(
    val status: Int = 0,
    val error: String? = null,
    val message: String? = null,
    val path: String? = null,
    val fieldErrors: Map<String, String>? = null
)

/**
 * Extracts the backend's human-readable error text from a Retrofit error body so the UI can
 * show "An account with this email already exists" instead of "HTTP 409 Conflict".
 */
object ApiErrors {
    private val adapter = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
        .adapter(ApiErrorDto::class.java)

    fun messageFrom(body: String?): String? {
        if (body.isNullOrBlank()) return null
        val parsed = runCatching { adapter.fromJson(body) }.getOrNull() ?: return null
        val field = parsed.fieldErrors?.entries?.firstOrNull()?.let { (key, value) ->
            "${key.replaceFirstChar { it.uppercase() }}: $value"
        }
        val base = parsed.message?.takeIf { it.isNotBlank() }
        return when {
            field != null && base != null -> "$base ($field)"
            field != null -> field
            else -> base
        }
    }
}

/**
 * Sync payloads matching the backend's feature DTOs. The server serialises with
 * `default-property-inclusion: non_null`, so optional properties are omitted entirely
 * rather than sent as null. Every field therefore carries a default or is nullable, so
 * Moshi's reflective adapter never fails on a missing key.
 */
data class PageDto<T>(
    val content: List<T> = emptyList(),
    val page: Int = 0,
    val size: Int = 0,
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val last: Boolean = true
)

data class RemoteIncome(
    val id: String,
    val amount: Double = 0.0,
    val source: String? = null,
    val category: String? = null,
    val transactionDate: String? = null,
    val origin: String? = null,
    val recurring: Boolean = false,
    val notes: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class IncomeBody(
    val amount: Double,
    val source: String? = null,
    val category: String? = null,
    val transactionDate: String,
    val origin: String? = null,
    val recurring: Boolean = false,
    val notes: String? = null
)

data class RemoteExpense(
    val id: String,
    val amount: Double = 0.0,
    val merchant: String? = null,
    val category: String? = null,
    val paymentMode: String? = null,
    val transactionDate: String? = null,
    val origin: String? = null,
    val recurring: Boolean = false,
    val notes: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class ExpenseBody(
    val amount: Double,
    val merchant: String? = null,
    val category: String? = null,
    val paymentMode: String? = null,
    val transactionDate: String,
    val origin: String? = null,
    val recurring: Boolean = false,
    val notes: String? = null
)

data class RemoteInvestment(
    val id: String,
    val instrumentName: String? = null,
    val investmentType: String? = null,
    val amountInvested: Double = 0.0,
    val currentValue: Double? = null,
    val gainLoss: Double? = null,
    val gainLossPercent: Double? = null,
    val transactionDate: String? = null,
    val broker: String? = null,
    val origin: String? = null,
    val notes: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class InvestmentBody(
    val instrumentName: String,
    val investmentType: String? = null,
    val amountInvested: Double,
    val currentValue: Double? = null,
    val transactionDate: String,
    val broker: String? = null,
    val origin: String? = null,
    val notes: String? = null
)

/**
 * Auto-capture review queue (backend: com.myfinancemanager.dto.autocapture.*).
 *
 * The backend owns the outcome of a review: `POST /{id}/confirm` is the call that writes the
 * income/expense/investment record, and `parsedData` is a free-form map the server mines for
 * amount/date/merchant/category when no explicit review body is supplied.
 */
data class RemoteAutoCapture(
    val id: String,
    val sourceType: String? = null,
    val sender: String? = null,
    val rawText: String? = null,
    val parsedType: String? = null,
    val parsedData: Map<String, Any?>? = null,
    val status: String? = null,
    val confidence: Double? = null,
    val committedRecordId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class AutoCaptureBody(
    val sourceType: String,
    val sender: String? = null,
    val rawText: String? = null,
    val parsedType: String? = null,
    val parsedData: Map<String, Any?> = emptyMap(),
    val confidence: Double? = null
)

/** The fields a confirmed capture contributes to the record the backend writes. */
data class AutoCaptureReviewBody(
    val transactionType: String? = null,
    val transactionDate: String? = null,
    val description: String? = null,
    val merchant: String? = null,
    val category: String? = null,
    val paymentMode: String? = null,
    val source: String? = null,
    val amount: Double? = null
)

/**
 * Statement import batch (backend: com.myfinancemanager.dto.imports.ImportBatchResponse).
 * `status` is an ImportStatus name; the server re-parses the uploaded file in the background,
 * hence the initial 202 with status QUEUED.
 */
data class RemoteImportBatch(
    val id: String,
    val fileName: String? = null,
    val contentType: String? = null,
    val fileSize: Long? = null,
    val status: String? = null,
    val extractionMethod: String? = null,
    val totalTransactions: Int = 0,
    val errorMessage: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class UpdateProfileBody(
    val fullName: String? = null,
    val currency: String? = null,
    val notificationsEnabled: Boolean? = null,
    /** Replaces the account's whole preferences map, so it is pushed complete. */
    val preferences: Map<String, Any?>? = null
)

/** A category budget (backend: com.myfinancemanager.dto.budget.BudgetResponse). */
data class RemoteBudget(
    val id: String,
    val category: String? = null,
    val monthlyLimit: Double = 0.0,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

/**
 * The budget path segment is the category, so this only carries the limit. Re-sending the same
 * category edits the existing budget instead of adding another one.
 */
data class BudgetUpsertBody(val monthlyLimit: Double)

/**
 * Auto-capture settings (backend: AutoCaptureSettingsResponse). One document per account holding
 * the capture switches and the sender lists, so the allow/block lists are pushed whole.
 */
data class RemoteCaptureSettings(
    val enabled: Boolean = false,
    val smsEnabled: Boolean = false,
    val emailEnabled: Boolean = false,
    val senderAllowList: List<String> = emptyList(),
    val senderBlockList: List<String> = emptyList(),
    val updatedAt: String? = null
)

data class CaptureSettingsBody(
    val enabled: Boolean,
    val smsEnabled: Boolean,
    val emailEnabled: Boolean,
    val senderAllowList: List<String>,
    val senderBlockList: List<String>
)

/**
 * A backend-generated insight (backend: AIInsightResponse). `status` is an InsightStatus name:
 * NEW, SAVED or DISMISSED.
 */
data class RemoteInsight(
    val id: String,
    val title: String? = null,
    val insightText: String? = null,
    val category: String? = null,
    val modelUsed: String? = null,
    val generatedAt: String? = null,
    val status: String? = null
)

data class GenerateInsightsBody(val months: Int? = null, val focus: String? = null)

data class InsightStatusBody(val status: String)

/**
 * `GET /api/v1/users/me/export` (backend: UserService.exportData). The server returns an untyped
 * map, so only the record collections the CSV is built from are declared here.
 */
data class RemoteExport(
    val exportedAt: String? = null,
    val incomeRecords: List<RemoteIncome> = emptyList(),
    val expenseRecords: List<RemoteExpense> = emptyList(),
    val investmentRecords: List<RemoteInvestment> = emptyList()
)

interface FinanceApi {
    /** Public health probe; /api/v1/health does not exist on the backend. */
    @GET("actuator/health")
    suspend fun health(): Map<String, String>

    // ---- Profile ---------------------------------------------------------------------

    @PUT("api/v1/users/me")
    suspend fun updateProfile(@Body body: UpdateProfileBody): AuthUser

    /**
     * Purges the profile and every record it owns. No password is needed here: the Neon Auth
     * identity is deleted separately, by the caller that holds the credentials.
     */
    @DELETE("api/v1/users/me")
    suspend fun deleteAccount(): Response<Unit>

    // ---- Income sync -----------------------------------------------------------------

    @GET("api/v1/incomes")
    suspend fun listIncomes(@Query("page") page: Int, @Query("size") size: Int): PageDto<RemoteIncome>

    @POST("api/v1/incomes")
    suspend fun createIncome(@Body body: IncomeBody): RemoteIncome

    @PUT("api/v1/incomes/{id}")
    suspend fun updateIncome(@Path("id") id: String, @Body body: IncomeBody): RemoteIncome

    @DELETE("api/v1/incomes/{id}")
    suspend fun deleteIncome(@Path("id") id: String): Response<Unit>

    // ---- Expense sync ----------------------------------------------------------------

    @GET("api/v1/expenses")
    suspend fun listExpenses(@Query("page") page: Int, @Query("size") size: Int): PageDto<RemoteExpense>

    @POST("api/v1/expenses")
    suspend fun createExpense(@Body body: ExpenseBody): RemoteExpense

    @PUT("api/v1/expenses/{id}")
    suspend fun updateExpense(@Path("id") id: String, @Body body: ExpenseBody): RemoteExpense

    @DELETE("api/v1/expenses/{id}")
    suspend fun deleteExpense(@Path("id") id: String): Response<Unit>

    // ---- Investment sync -------------------------------------------------------------

    @GET("api/v1/investments")
    suspend fun listInvestments(@Query("page") page: Int, @Query("size") size: Int): PageDto<RemoteInvestment>

    @POST("api/v1/investments")
    suspend fun createInvestment(@Body body: InvestmentBody): RemoteInvestment

    @PUT("api/v1/investments/{id}")
    suspend fun updateInvestment(@Path("id") id: String, @Body body: InvestmentBody): RemoteInvestment

    @DELETE("api/v1/investments/{id}")
    suspend fun deleteInvestment(@Path("id") id: String): Response<Unit>

    // ---- Auto-capture review queue ---------------------------------------------------

    @GET("api/v1/auto-capture")
    suspend fun listAutoCapture(@Query("page") page: Int, @Query("size") size: Int): PageDto<RemoteAutoCapture>

    @POST("api/v1/auto-capture")
    suspend fun submitAutoCapture(@Body body: AutoCaptureBody): RemoteAutoCapture

    /** Confirms a queued capture; the backend writes the transaction and returns its id. */
    @POST("api/v1/auto-capture/{id}/confirm")
    suspend fun confirmAutoCapture(
        @Path("id") id: String,
        @Body body: AutoCaptureReviewBody
    ): RemoteAutoCapture

    @POST("api/v1/auto-capture/{id}/reject")
    suspend fun rejectAutoCapture(@Path("id") id: String): RemoteAutoCapture

    // ---- Statement imports -----------------------------------------------------------

    /** Uploads a statement (multipart field `file`) for the backend to store and parse. */
    @Multipart
    @POST("api/v1/imports")
    suspend fun uploadImport(@Part file: MultipartBody.Part): RemoteImportBatch

    @GET("api/v1/imports")
    suspend fun listImports(@Query("page") page: Int, @Query("size") size: Int): PageDto<RemoteImportBatch>

    // ---- Budgets ---------------------------------------------------------------------

    @GET("api/v1/budgets")
    suspend fun listBudgets(): List<RemoteBudget>

    @PUT("api/v1/budgets/{category}")
    suspend fun upsertBudget(@Path("category") category: String, @Body body: BudgetUpsertBody): RemoteBudget

    @DELETE("api/v1/budgets/{category}")
    suspend fun deleteBudget(@Path("category") category: String): Response<Unit>

    // ---- Capture settings ------------------------------------------------------------

    @GET("api/v1/auto-capture/settings")
    suspend fun captureSettings(): RemoteCaptureSettings

    @PUT("api/v1/auto-capture/settings")
    suspend fun updateCaptureSettings(@Body body: CaptureSettingsBody): RemoteCaptureSettings

    // ---- Insights --------------------------------------------------------------------

    @GET("api/v1/insights")
    suspend fun listInsights(@Query("page") page: Int, @Query("size") size: Int): PageDto<RemoteInsight>

    /** Generates a fresh batch; fails with 503 when the server has no AI provider configured. */
    @POST("api/v1/insights/generate")
    suspend fun generateInsights(@Body body: GenerateInsightsBody): List<RemoteInsight>

    @PATCH("api/v1/insights/{id}/status")
    suspend fun updateInsightStatus(@Path("id") id: String, @Body body: InsightStatusBody): RemoteInsight

    @DELETE("api/v1/insights/{id}")
    suspend fun deleteInsight(@Path("id") id: String): Response<Unit>

    // ---- Account ---------------------------------------------------------------------

    @GET("api/v1/users/me")
    suspend fun profile(): AuthUser

    @GET("api/v1/users/me/export")
    suspend fun exportData(): RemoteExport
}

object ApiConfig {
    const val BASE_URL = BuildConfig.API_BASE_URL
}

class TokenInterceptor(private val tokenProvider: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val token = tokenProvider()
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }
}

object ApiClient {
    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val logging: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        }
    }

    /**
     * Base client for backend calls. Deliberately carries no token interceptor and no
     * authenticator; [create] adds those. Token refresh happens against Neon Auth on a separate
     * client, so a failing refresh can never recurse back through this one.
     */
    /**
     * Timeouts are deliberately generous. The deployed backend runs on a free Render plan,
     * which spins down when idle and can take 50s+ to answer the first request after that;
     * a 20s read timeout made the very first login of a session fail every time.
     */
    private val baseClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    /**
     * Authenticated API: injects the Neon Auth JWT and, on 401, [TokenAuthenticator] mints a
     * fresh one from Neon Auth and retries the request transparently.
     */
    fun create(
        tokenProvider: () -> String?,
        sessionStore: SessionStore,
        neonAuth: NeonAuthApi
    ): FinanceApi {
        val client = baseClient.newBuilder()
            .addInterceptor(TokenInterceptor(tokenProvider))
            .authenticator(TokenAuthenticator(sessionStore, neonAuth))
            .build()
        return Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(FinanceApi::class.java)
    }
}
