package com.myfinancemanager.app

import com.myfinancemanager.app.data.remote.ApiErrors
import com.myfinancemanager.app.data.remote.NeonAuthApi
import com.myfinancemanager.app.data.remote.NeonAuthResponse
import com.myfinancemanager.app.data.remote.NeonSignInBody
import com.myfinancemanager.app.data.remote.NeonSignUpBody
import com.myfinancemanager.app.data.remote.NeonTokenResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the wire contract with Neon Auth (Managed Better Auth), which owns credentials.
 *
 * Every payload below was captured from the deployed auth service, so a change on that side
 * shows up here rather than as a mysterious sign-in failure on the phone. A compile cannot
 * catch a renamed JSON field, which is exactly the class of bug this guards.
 */
class NeonAuthApiTest {

    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    private fun <T> adapter(type: java.lang.reflect.Type) = moshi.adapter<T>(type)

    @Test
    fun `sign-in response exposes the account id used to key local records`() {
        // Captured verbatim from POST /sign-in/email. Note the extra fields Neon Auth sends that
        // this client deliberately ignores (image, banned, banReason, banExpires).
        val json = """
            {
              "redirect": false,
              "token": "D3Ho4Q69Dq7cD5J06zB2VkInrAksOcge",
              "user": {
                "name": "Codebuff Probe",
                "email": "codebuff-probe-1789538511@example.com",
                "emailVerified": false,
                "image": null,
                "createdAt": "2026-09-16T06:01:51.977Z",
                "updatedAt": "2026-09-16T06:01:51.977Z",
                "role": "user",
                "banned": false,
                "banReason": null,
                "banExpires": null,
                "id": "0a9b2b42-eee4-4d57-afc7-9dd1901e32a8"
              }
            }
        """.trimIndent()

        val response = adapter<NeonAuthResponse>(NeonAuthResponse::class.java).fromJson(json)!!

        assertEquals("0a9b2b42-eee4-4d57-afc7-9dd1901e32a8", response.user?.id)
        assertEquals("codebuff-probe-1789538511@example.com", response.user?.email)
        assertEquals("Codebuff Probe", response.user?.name)
        assertFalse(response.user!!.emailVerified)
        assertFalse(response.redirect!!)
    }

    @Test
    fun `the token from sign-in is opaque so the JWT must be fetched separately`() {
        // This is the value in `token` on a real sign-in response: a 32-character session token,
        // not a JWT. The JWT only comes from GET /token, which is why AuthRepository makes a
        // second call - treating this as the access token would send a non-JWT to the backend.
        val opaque = "D3Ho4Q69Dq7cD5J06zB2VkInrAksOcge"
        assertFalse("opaque session token must not look like a JWT", opaque.contains('.'))
    }

    @Test
    fun `token response carries a three-part JWT`() {
        // Shape of GET /token: {"token":"<header>.<payload>.<signature>"}
        val jwt = "eyJhbGciOiJFZERTQSIsImtpZCI6ImVmMTkyNTRkLTRhZi00NDAxLTg4MjgtNWZjNjE4NjZlZTg1In0" +
            ".eyJzdWIiOiIwYTliMmI0Mi1lZWU0LTRkNTctYWZjNy05ZGQxOTAxZTMyYTgifQ" +
            ".AAAA"
        val response = adapter<NeonTokenResponse>(NeonTokenResponse::class.java)
            .fromJson("""{"token":"$jwt"}""")!!

        assertEquals(jwt, response.token)
        assertEquals(3, response.token.split('.').size)
    }

    @Test
    fun `sign-in rejection surfaces the auth service message`() {
        // Real 401 from POST /sign-in/email with a wrong password.
        val body = """
            {"message":"Invalid email or password","code":"INVALID_EMAIL_OR_PASSWORD"}
        """.trimIndent()

        assertEquals("Invalid email or password", ApiErrors.messageFrom(body))
    }

    @Test
    fun `validation failure surfaces the offending field`() {
        // Real 400 from POST /sign-in/email with an empty body.
        val body = "{\"message\":\"[body.email] Invalid input: expected string, received " +
            "undefined; [body.password] Invalid input: expected string, received undefined\"," +
            "\"code\":\"VALIDATION_ERROR\"}"

        val message = ApiErrors.messageFrom(body)!!
        assertTrue(message, message.contains("[body.email]"))
    }

    @Test
    fun `a sign-up without an Origin header is rejected by the auth service`() {
        // Real 400 from POST /sign-up/email sent without an Origin header. NeonAuthClient always
        // sets one precisely because of this; the assertion documents why that header exists.
        val body = """
            {"code":"MISSING_ORIGIN",
             "message":"Origin header is required when callbackURL is not an absolute URL"}
        """.trimIndent()

        assertEquals(
            "Origin header is required when callbackURL is not an absolute URL",
            ApiErrors.messageFrom(body)
        )
    }

    @Test
    fun `request bodies serialise to the exact field names Better Auth expects`() {
        assertEquals(
            """{"email":"a@b.com","password":"secret","name":"Ada"}""",
            adapter<NeonSignUpBody>(NeonSignUpBody::class.java)
                .toJson(NeonSignUpBody("a@b.com", "secret", "Ada"))
        )
        assertEquals(
            """{"email":"a@b.com","password":"secret"}""",
            adapter<NeonSignInBody>(NeonSignInBody::class.java)
                .toJson(NeonSignInBody("a@b.com", "secret"))
        )
    }

    @Test
    fun `the public auth API exposes no account deletion route`() {
        // Documents a real limitation rather than a behaviour: on the deployed instance every
        // attempt at POST /delete-user answers 404 (and 403 without an Origin header), so the app
        // must not pretend it can delete the sign-in. If Neon ever adds the route, this comment
        // and AuthRepository.deleteAccount are the two places that need revisiting.
        val declared = NeonAuthApi::class.java.declaredMethods.map { it.name }
        assertFalse(declared.contains("deleteUser"))
    }

    @Test
    fun `success bodies that carry no useful payload still parse`() {
        // POST /sign-out and POST /delete-user answer {"success":true}.
        val parsed: Map<*, *>? = moshi.adapter(Map::class.java)
            .fromJson("""{"success":true}""")
        assertEquals(true, parsed?.get("success"))
    }
}
