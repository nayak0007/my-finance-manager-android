package com.myfinancemanager.app

import com.myfinancemanager.app.data.remote.ApiErrors
import com.myfinancemanager.app.data.remote.AuthUser
import com.myfinancemanager.app.data.remote.AutoCaptureBody
import com.myfinancemanager.app.data.remote.AutoCaptureReviewBody
import com.myfinancemanager.app.data.remote.BudgetUpsertBody
import com.myfinancemanager.app.data.remote.CaptureSettingsBody
import com.myfinancemanager.app.data.remote.InsightStatusBody
import com.myfinancemanager.app.data.remote.PageDto
import com.myfinancemanager.app.data.remote.RemoteAutoCapture
import com.myfinancemanager.app.data.remote.RemoteBudget
import com.myfinancemanager.app.data.remote.RemoteCaptureSettings
import com.myfinancemanager.app.data.remote.RemoteExpense
import com.myfinancemanager.app.data.remote.RemoteExport
import com.myfinancemanager.app.data.remote.RemoteImportBatch
import com.myfinancemanager.app.data.remote.RemoteIncome
import com.myfinancemanager.app.data.remote.RemoteInsight
import com.myfinancemanager.app.data.remote.RemoteInvestment
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the wire contract between this client and the Spring Boot backend.
 *
 * The payloads below mirror what the server actually emits, including two properties that
 * shape the DTOs: `default-property-inclusion: non_null` (null fields are omitted entirely
 * rather than sent as null) and `write-dates-as-timestamps: false` (dates are ISO-8601
 * strings). A compile cannot catch a mismatch here, so it is asserted directly.
 */
class RemoteApiTest {

    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    private fun <T> adapter(type: java.lang.reflect.Type) = moshi.adapter<T>(type)

    @Test
    fun `paged income list maps every field`() {
        // GET /api/v1/incomes -> PageResponse<IncomeResponse>
        val json = """
            {
              "content": [
                {
                  "id": "3f0c1c3e-2a4b-4c6d-8e2f-9a1b2c3d4e5f",
                  "amount": 50000.00,
                  "source": "Acme Payroll",
                  "category": "SALARY",
                  "transactionDate": "2026-09-01",
                  "origin": "MANUAL",
                  "recurring": true,
                  "notes": "September salary",
                  "createdAt": "2026-09-01T10:15:30.123456789Z",
                  "updatedAt": "2026-09-02T08:00:00Z"
                }
              ],
              "page": 0,
              "size": 20,
              "totalElements": 1,
              "totalPages": 1,
              "last": true
            }
        """.trimIndent()

        val type = Types.newParameterizedType(PageDto::class.java, RemoteIncome::class.java)
        val page = adapter<PageDto<RemoteIncome>>(type).fromJson(json)!!

        assertEquals(1, page.content.size)
        assertEquals(0, page.page)
        assertEquals(20, page.size)
        assertEquals(1L, page.totalElements)
        assertTrue(page.last)

        val income = page.content.first()
        assertEquals("3f0c1c3e-2a4b-4c6d-8e2f-9a1b2c3d4e5f", income.id)
        assertEquals(50000.0, income.amount, 0.0)
        assertEquals("Acme Payroll", income.source)
        assertEquals("SALARY", income.category)
        assertEquals("2026-09-01", income.transactionDate)
        assertEquals("MANUAL", income.origin)
        assertTrue(income.recurring)
        assertEquals("2026-09-02T08:00:00Z", income.updatedAt)
    }

    @Test
    fun `omitted null fields fall back to DTO defaults`() {
        // null merchant/category/paymentMode/notes are absent from the JSON entirely.
        val json = """
            {
              "content": [
                {
                  "id": "11111111-2222-3333-4444-555555555555",
                  "amount": 12.5,
                  "recurring": false,
                  "transactionDate": "2026-09-02",
                  "origin": "SMS"
                }
              ],
              "page": 0,
              "size": 20,
              "totalElements": 1,
              "totalPages": 1,
              "last": true
            }
        """.trimIndent()

        val type = Types.newParameterizedType(PageDto::class.java, RemoteExpense::class.java)
        val page = adapter<PageDto<RemoteExpense>>(type).fromJson(json)!!

        val expense = page.content.first()
        assertEquals(12.5, expense.amount, 0.0)
        assertEquals("SMS", expense.origin)
        assertNull(expense.merchant)
        assertNull(expense.category)
        assertNull(expense.paymentMode)
        assertNull(expense.notes)
    }

    @Test
    fun `investment response ignores server-only gain fields`() {
        val json = """
            {
              "content": [
                {
                  "id": "99999999-8888-7777-6666-555555555555",
                  "instrumentName": "Nifty 50 Index Fund",
                  "investmentType": "MUTUAL_FUND",
                  "amountInvested": 100000.0,
                  "currentValue": 112500.5,
                  "gainLoss": 12500.5,
                  "gainLossPercent": 12.5001,
                  "transactionDate": "2026-04-01",
                  "broker": "Groww",
                  "origin": "MANUAL",
                  "createdAt": "2026-04-01T06:00:00Z",
                  "updatedAt": "2026-09-10T06:00:00Z"
                }
              ],
              "page": 0,
              "size": 20,
              "totalElements": 1,
              "totalPages": 1,
              "last": true
            }
        """.trimIndent()

        val type = Types.newParameterizedType(PageDto::class.java, RemoteInvestment::class.java)
        val page = adapter<PageDto<RemoteInvestment>>(type).fromJson(json)!!

        val investment = page.content.first()
        assertEquals("Nifty 50 Index Fund", investment.instrumentName)
        assertEquals("MUTUAL_FUND", investment.investmentType)
        assertEquals(100000.0, investment.amountInvested, 0.0)
        assertEquals(112500.5, investment.currentValue!!, 0.0)
        assertEquals("2026-04-01", investment.transactionDate)
    }

    @Test
    fun `api error body surfaces the backend message`() {
        // The backend's ApiError shape, as returned by any conflicting write. Auth errors now
        // come from Neon Auth instead - see NeonAuthApiTest.
        val conflict = """
            {
              "timestamp": "2026-09-15T17:36:53.991095849Z",
              "status": 409,
              "error": "Conflict",
              "message": "An account with this email already exists",
              "path": "/api/v1/budgets/FOOD"
            }
        """.trimIndent()

        assertEquals(
            "An account with this email already exists",
            ApiErrors.messageFrom(conflict)
        )
    }

    @Test
    fun `api error body folds in field validation detail`() {
        // Real 400 from a backend write whose body failed bean validation.
        val invalid = """
            {
              "timestamp": "2026-09-15T17:36:55.215198360Z",
              "status": 400,
              "error": "Bad Request",
              "message": "Validation failed",
              "path": "/api/v1/budgets/travel",
              "fieldErrors": { "monthlyLimit": "must be greater than 0" }
            }
        """.trimIndent()

        assertEquals(
            "Validation failed (MonthlyLimit: must be greater than 0)",
            ApiErrors.messageFrom(invalid)
        )
    }

    @Test
    fun `unparseable error body is reported as absent`() {
        assertNull(ApiErrors.messageFrom(null))
        assertNull(ApiErrors.messageFrom(""))
        assertNull(ApiErrors.messageFrom("<html>502 Bad Gateway</html>"))
    }

    @Test
    fun `paged auto-capture queue keeps parsedData values typed`() {
        // GET /api/v1/auto-capture -> PageResponse<AutoCaptureItemResponse>. parsedData is a
        // free-form map, and the sync engine reads amount/date/merchant back out of it.
        val json = """
            {
              "content": [
                {
                  "id": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
                  "sourceType": "SMS",
                  "sender": "VM-HDFCBK",
                  "rawText": "Rs.499 debited from a/c XX1234 at SWIGGY",
                  "parsedType": "EXPENSE",
                  "parsedData": {
                    "type": "EXPENSE",
                    "amount": 499.0,
                    "date": "2026-09-14",
                    "merchant": "SWIGGY"
                  },
                  "status": "PENDING",
                  "confidence": 0.82,
                  "createdAt": "2026-09-14T12:05:00.5Z",
                  "updatedAt": "2026-09-14T12:05:00.5Z"
                }
              ],
              "page": 0,
              "size": 200,
              "totalElements": 1,
              "totalPages": 1,
              "last": true
            }
        """.trimIndent()

        val type = Types.newParameterizedType(PageDto::class.java, RemoteAutoCapture::class.java)
        val page = adapter<PageDto<RemoteAutoCapture>>(type).fromJson(json)!!

        val item = page.content.first()
        assertEquals("SMS", item.sourceType)
        assertEquals("PENDING", item.status)
        assertEquals("EXPENSE", item.parsedType)
        assertEquals("VM-HDFCBK", item.sender)
        assertEquals(0.82, item.confidence!!, 0.0001)
        // JSON numbers decode as Double, which is what the number reader expects.
        assertTrue(item.parsedData!!["amount"] is Double)
        assertEquals(499.0, (item.parsedData!!["amount"] as Double), 0.0)
        assertEquals("2026-09-14", item.parsedData!!["date"])
        assertNull(item.committedRecordId)
    }

    @Test
    fun `submitted capture carries the keys the backend mines`() {
        // POST /api/v1/auto-capture -> AutoCaptureItemRequest. `sourceType` is @NotNull, and the
        // server resolves amount/date/merchant out of parsedData by these exact key names.
        val body = AutoCaptureBody(
            sourceType = "SMS",
            sender = "VM-HDFCBK",
            rawText = "Rs.499 debited from a/c XX1234 at SWIGGY",
            parsedType = "EXPENSE",
            parsedData = mapOf(
                "type" to "EXPENSE",
                "amount" to 499.0,
                "date" to "2026-09-14",
                "merchant" to "SWIGGY",
                "description" to "SWIGGY"
            )
        )

        val json = moshi.adapter(AutoCaptureBody::class.java).toJson(body)

        assertTrue(json.contains("\"sourceType\":\"SMS\""))
        assertTrue(json.contains("\"parsedType\":\"EXPENSE\""))
        assertTrue(json.contains("\"amount\":499.0"))
        assertTrue(json.contains("\"merchant\":\"SWIGGY\""))
        assertTrue(json.contains("\"date\":\"2026-09-14\""))
        // Unset optional fields are dropped rather than sent as null, matching the server's own
        // `default-property-inclusion: non_null` serialisation.
        assertTrue(!json.contains("confidence"))
    }

    @Test
    fun `review body names every field the backend maps onto a record`() {
        // POST /api/v1/auto-capture/{id}/confirm -> AutoCaptureReviewRequest. This is the payload
        // that becomes the transaction, so the field names have to be exact.
        val body = AutoCaptureReviewBody(
            transactionType = "EXPENSE",
            transactionDate = "2026-09-14",
            description = "SWIGGY",
            merchant = "SWIGGY",
            category = "OTHER",
            paymentMode = "OTHER",
            amount = 499.0
        )

        val json = moshi.adapter(AutoCaptureReviewBody::class.java).toJson(body)

        listOf("transactionType", "transactionDate", "description", "merchant", "category", "paymentMode", "amount")
            .forEach { assertTrue("missing $it in $json", json.contains("\"$it\"")) }
        assertTrue(json.contains("\"transactionType\":\"EXPENSE\""))
        assertTrue(json.contains("\"transactionDate\":\"2026-09-14\""))
        // Nullable and unset in this payload, so it must not be sent as an empty string.
        assertTrue(!json.contains("source"))
    }

    @Test
    fun `import batch response maps the 202 upload result`() {
        // POST /api/v1/imports -> 202 Accepted with ImportBatchResponse; processing is async, so
        // the status is QUEUED and the parsed counts are still zero.
        val queued = """
            {
              "id": "12345678-abcd-4321-abcd-1234567890ab",
              "fileName": "hdfc_sept.csv",
              "contentType": "text/csv",
              "fileSize": 20481,
              "status": "QUEUED",
              "extractionMethod": "NONE",
              "totalTransactions": 0,
              "createdAt": "2026-09-15T09:30:00Z",
              "updatedAt": "2026-09-15T09:30:00Z"
            }
        """.trimIndent()

        val batch = moshi.adapter(RemoteImportBatch::class.java).fromJson(queued)!!

        assertEquals("hdfc_sept.csv", batch.fileName)
        assertEquals("text/csv", batch.contentType)
        assertEquals(20481L, batch.fileSize)
        assertEquals("QUEUED", batch.status)
        assertEquals(0, batch.totalTransactions)
        assertNull(batch.errorMessage)
    }

    @Test
    fun `budget list maps the category keyed upsert`() {
        // GET /api/v1/budgets -> List<BudgetResponse>, a bare array rather than a page.
        val json = """
            [
              {
                "id": "7c1f2f5e-1111-2222-3333-444455556666",
                "category": "FOOD",
                "monthlyLimit": 8000.00,
                "createdAt": "2026-09-01T05:00:00Z",
                "updatedAt": "2026-09-15T05:00:00Z"
              }
            ]
        """.trimIndent()

        val type = Types.newParameterizedType(List::class.java, RemoteBudget::class.java)
        val budgets = adapter<List<RemoteBudget>>(type).fromJson(json)!!

        assertEquals(1, budgets.size)
        assertEquals("FOOD", budgets.first().category)
        assertEquals(8000.0, budgets.first().monthlyLimit, 0.0)
    }

    @Test
    fun `budget upsert body carries only the limit`() {
        // PUT /api/v1/budgets/{category} -> BudgetUpsertRequest. The category is in the path, and
        // the server's unique (user, category) constraint is what makes this idempotent.
        val json = moshi.adapter(BudgetUpsertBody::class.java).toJson(BudgetUpsertBody(9500.0))
        assertEquals("{\"monthlyLimit\":9500.0}", json)
    }

    @Test
    fun `capture settings body sends both sender lists whole`() {
        // PUT /api/v1/auto-capture/settings -> AutoCaptureSettingsRequest. The server replaces the
        // two lists wholesale, so an empty list has to be sent as an empty array, not omitted.
        val body = CaptureSettingsBody(
            enabled = true,
            smsEnabled = true,
            emailEnabled = false,
            senderAllowList = emptyList(),
            senderBlockList = listOf("VM-SPAM")
        )

        val json = moshi.adapter(CaptureSettingsBody::class.java).toJson(body)

        assertTrue(json.contains("\"enabled\":true"))
        assertTrue(json.contains("\"smsEnabled\":true"))
        assertTrue(json.contains("\"emailEnabled\":false"))
        assertTrue(json.contains("\"senderAllowList\":[]"))
        assertTrue(json.contains("\"senderBlockList\":[\"VM-SPAM\"]"))
    }

    @Test
    fun `capture settings response maps the sender lists`() {
        val json = """
            {
              "enabled": true,
              "smsEnabled": true,
              "emailEnabled": false,
              "senderAllowList": ["VM-HDFCBK", "VM-ICICI"],
              "senderBlockList": ["VM-PROMO"],
              "updatedAt": "2026-09-15T10:00:00Z"
            }
        """.trimIndent()

        val settings = moshi.adapter(RemoteCaptureSettings::class.java).fromJson(json)!!

        assertTrue(settings.smsEnabled)
        assertTrue(!settings.emailEnabled)
        assertEquals(listOf("VM-HDFCBK", "VM-ICICI"), settings.senderAllowList)
        assertEquals(listOf("VM-PROMO"), settings.senderBlockList)
    }

    @Test
    fun `insight response maps every field the tab shows`() {
        // GET /api/v1/insights -> PageResponse<AIInsightResponse>.
        val json = """
            {
              "content": [
                {
                  "id": "abcdefab-1234-5678-9abc-def012345678",
                  "title": "Food spending is up",
                  "insightText": "You spent 32% more on FOOD this month than last.",
                  "category": "SPENDING",
                  "modelUsed": "openai/gpt-4o-mini",
                  "generatedAt": "2026-09-15T11:00:00Z",
                  "status": "NEW",
                  "metadata": {}
                }
              ],
              "page": 0,
              "size": 200,
              "totalElements": 1,
              "totalPages": 1,
              "last": true
            }
        """.trimIndent()

        val type = Types.newParameterizedType(PageDto::class.java, RemoteInsight::class.java)
        val page = adapter<PageDto<RemoteInsight>>(type).fromJson(json)!!

        val insight = page.content.first()
        assertEquals("Food spending is up", insight.title)
        assertEquals("SPENDING", insight.category)
        assertEquals("NEW", insight.status)
        assertEquals("2026-09-15T11:00:00Z", insight.generatedAt)
    }

    @Test
    fun `insight status body uses the server's enum names`() {
        // PATCH /api/v1/insights/{id}/status -> UpdateInsightStatusRequest.
        assertEquals(
            "{\"status\":\"DISMISSED\"}",
            moshi.adapter(InsightStatusBody::class.java).toJson(InsightStatusBody("DISMISSED"))
        )
        assertEquals(
            "{\"status\":\"SAVED\"}",
            moshi.adapter(InsightStatusBody::class.java).toJson(InsightStatusBody("SAVED"))
        )
    }

    @Test
    fun `account export is parsed from the untyped server payload`() {
        // GET /api/v1/users/me/export returns an open map, so the CSV is built from these three
        // collections and nothing else is required to be present.
        val json = """
            {
              "exportedAt": "2026-09-16T04:00:00Z",
              "user": { "id": "u-1", "email": "jane@example.com", "currency": "INR" },
              "incomeRecords": [
                { "id": "i-1", "amount": 2500.0, "source": "Salary", "category": "salary",
                  "transactionDate": "2026-09-01", "origin": "MANUAL", "recurring": false }
              ],
              "expenseRecords": [
                { "id": "e-1", "amount": 400.0, "merchant": "BigBasket", "category": "food",
                  "paymentMode": "UPI", "transactionDate": "2026-09-02", "origin": "MANUAL" }
              ],
              "investmentRecords": [
                { "id": "v-1", "instrumentName": "Nifty 50", "investmentType": "MUTUAL_FUND",
                  "amountInvested": 100000.0, "transactionDate": "2026-04-01", "origin": "MANUAL" }
              ]
            }
        """.trimIndent()

        val export = moshi.adapter(RemoteExport::class.java).fromJson(json)!!

        assertEquals(1, export.incomeRecords.size)
        assertEquals(1, export.expenseRecords.size)
        assertEquals(1, export.investmentRecords.size)
        assertEquals("Salary", export.incomeRecords.first().source)
        assertEquals("BigBasket", export.expenseRecords.first().merchant)
        assertEquals(100000.0, export.investmentRecords.first().amountInvested, 0.0)
        assertEquals("2026-09-01", export.incomeRecords.first().transactionDate)
    }

    @Test
    fun `account profile carries the notification switch and preferences`() {
        // GET /api/v1/users/me -> UserResponse. AuthUser also parses the auth payload, whose user
        // block has fewer fields, so everything beyond identity is optional.
        val json = """
            {
              "id": "11111111-1111-1111-1111-111111111111",
              "email": "jane@example.com",
              "fullName": "Jane Doe",
              "authProvider": "LOCAL",
              "currency": "USD",
              "emailVerified": false,
              "notificationsEnabled": false,
              "preferences": { "insightFrequencyDays": 14 },
              "createdAt": "2026-09-01T05:00:00Z"
            }
        """.trimIndent()

        val profile = moshi.adapter(AuthUser::class.java).fromJson(json)!!

        assertEquals("USD", profile.currency)
        assertEquals(false, profile.notificationsEnabled)
        assertEquals(14, (profile.preferences!!["insightFrequencyDays"] as Double).toInt())
    }

    @Test
    fun `import batch response tolerates the fields a finished parse adds`() {
        val ready = """
            {
              "id": "12345678-abcd-4321-abcd-1234567890ab",
              "fileName": "hdfc_sept.csv",
              "contentType": "text/csv",
              "fileSize": 20481,
              "status": "READY_FOR_REVIEW",
              "extractionMethod": "OPENROUTER_FALLBACK",
              "totalTransactions": 42,
              "errorMessage": "2 statement row(s) without a valid amount were skipped",
              "createdAt": "2026-09-15T09:30:00Z",
              "updatedAt": "2026-09-15T09:30:20Z"
            }
        """.trimIndent()

        val batch = moshi.adapter(RemoteImportBatch::class.java).fromJson(ready)!!

        assertEquals("READY_FOR_REVIEW", batch.status)
        assertEquals(42, batch.totalTransactions)
        assertEquals("OPENROUTER_FALLBACK", batch.extractionMethod)
        assertTrue(batch.errorMessage!!.contains("skipped"))
    }
}
