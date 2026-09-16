package com.myfinancemanager.app.data.session

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * Persists Neon Auth's session cookie across app restarts.
 *
 * Neon Auth is cookie-based. The JWT that this backend verifies comes from `GET /token`, and
 * that call is authorised by the `__Secure-neon-auth.session_token` cookie rather than by a
 * bearer token. A JWT lasts only 15 minutes, while the cookie lasts 7 days, so the cookie is
 * the durable credential — the thing that lets the app silently obtain a new JWT instead of
 * sending the user back to the login screen.
 *
 * OkHttp discards cookies unless a [CookieJar] stores them, so this class is what makes token
 * refresh work at all.
 *
 * SharedPreferences is used rather than DataStore because [CookieJar] is a synchronous
 * interface called from OkHttp's own threads.
 */
class NeonAuthCookieJar(context: Context) : CookieJar {

    private val prefs = context.applicationContext
        .getSharedPreferences("neon_auth_cookies", Context.MODE_PRIVATE)
    private val lock = Any()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        synchronized(lock) {
            val editor = prefs.edit()
            for (cookie in cookies) {
                // An already-expired cookie is the server signing us out; record the deletion.
                if (cookie.expiresAt < System.currentTimeMillis()) {
                    editor.remove(keyOf(cookie))
                } else {
                    editor.putString(keyOf(cookie), cookie.toString())
                }
            }
            editor.apply()
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val usable = mutableListOf<Cookie>()
            val expired = mutableListOf<String>()
            for ((storedKey, value) in prefs.all) {
                val text = value as? String ?: continue
                val cookie = Cookie.parse(url, text)
                if (cookie == null || cookie.expiresAt < now) expired += storedKey else usable += cookie
            }
            if (expired.isNotEmpty()) {
                val editor = prefs.edit()
                for (storedKey in expired) editor.remove(storedKey)
                editor.apply()
            }
            return usable
        }
    }

    /** Called when the user signs out or deletes the account. */
    fun clear() {
        synchronized(lock) { prefs.edit().clear().apply() }
    }

    private fun keyOf(cookie: Cookie) = "${cookie.name}@${cookie.domain}"
}
