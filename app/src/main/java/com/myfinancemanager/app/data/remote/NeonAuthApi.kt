package com.myfinancemanager.app.data.remote

import com.myfinancemanager.app.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.CookieJar
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * Request/response models for Neon Auth (Managed Better Auth), which owns user credentials.
 *
 * Verified against the deployed service, not just the docs:
 *
 *   POST /sign-up/email  { email, password, name }  -> { redirect, token, user }
 *   POST /sign-in/email  { email, password }        -> { redirect, token, user }
 *   POST /sign-out                                  -> { success: true }
 *   GET  /token                                     -> { token }   (the JWT; needs the cookie)
 *
 * The `token` returned by sign-in/sign-up is an opaque session token, *not* the JWT, so the
 * JWT always comes from `GET /token`. Both calls also set the session cookie.
 *
 * There is deliberately no account-deletion call. The service exposes no such route on its
 * public API (`POST /delete-user` answers 404 on this instance whatever Origin is sent), so
 * removing an identity is a Neon Console or management-API operation instead.
 */
data class NeonSignUpBody(val email: String, val password: String, val name: String)
data class NeonSignInBody(val email: String, val password: String)

data class NeonUser(
    val id: String,
    val email: String,
    val name: String? = null,
    val emailVerified: Boolean = false,
    val role: String? = null
)

data class NeonAuthResponse(
    val redirect: Boolean? = null,
    val token: String? = null,
    val user: NeonUser? = null
)

data class NeonTokenResponse(val token: String)

/** Neon Auth's error shape: `{ "message": "...", "code": "..." }`. */
data class NeonErrorDto(val message: String? = null, val code: String? = null)

interface NeonAuthApi {
    @POST("sign-up/email")
    suspend fun signUp(@Body body: NeonSignUpBody): NeonAuthResponse

    @POST("sign-in/email")
    suspend fun signIn(@Body body: NeonSignInBody): NeonAuthResponse

    /** Revokes the session server-side and clears the session cookie. */
    @POST("sign-out")
    suspend fun signOut(): Map<String, Any?>

    /** Mints a short-lived JWT for the current session cookie. */
    @GET("token")
    suspend fun token(): NeonTokenResponse
}

object NeonAuthConfig {
    /** Neon Auth base URL, including the `/<database>/auth` suffix; must end with a slash. */
    const val BASE_URL: String = BuildConfig.NEON_AUTH_URL

    /**
     * Value sent as `Origin` on sign-up.
     *
     * Better Auth rejects a sign-up without an Origin header ("Origin header is required when
     * callbackURL is not an absolute URL"), and then checks that origin against the auth
     * instance's trusted list. A native app has no real origin, and this project's trusted list
     * is `["https://finance-tracker-api-5x4k.onrender.com"]` with `allow_localhost` enabled, so
     * localhost is the value that is accepted.
     *
     * If `allow_localhost` is ever turned off, add a domain you control to the auth instance's
     * trusted origins and change this constant to match.
     */
    const val ORIGIN: String = "http://localhost"
}

object NeonAuthClient {

    private val moshi: Moshi by lazy {
        Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    }

    private val logging: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            // BASIC, not BODY: these requests carry passwords and session cookies.
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    private val originHeader = Interceptor { chain ->
        chain.proceed(
            chain.request().newBuilder()
                .header("Origin", NeonAuthConfig.ORIGIN)
                .build()
        )
    }

    /**
     * Deliberately has no token interceptor and no authenticator: a 401 from Neon Auth is a real
     * credential failure and must surface as one, never trigger a retry loop.
     *
     * The cookie jar is what carries the session between calls, and the generous timeouts cover
     * the same cold-starting free hosting plan the rest of the app talks to.
     */
    fun create(cookieJar: CookieJar): NeonAuthApi =
        Retrofit.Builder()
            .baseUrl(NeonAuthConfig.BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .cookieJar(cookieJar)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .writeTimeout(120, TimeUnit.SECONDS)
                    .addInterceptor(originHeader)
                    .addInterceptor(logging)
                    .build()
            )
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(NeonAuthApi::class.java)
}
