package com.myfinancemanager.app

import com.myfinancemanager.app.data.insights.InsightEngine
import com.myfinancemanager.app.data.local.entity.ExpenseCategory
import com.myfinancemanager.app.data.local.entity.ExpenseEntity
import com.myfinancemanager.app.data.local.entity.IncomeCategory
import com.myfinancemanager.app.data.local.entity.IncomeEntity
import com.myfinancemanager.app.data.local.entity.ParsedType
import com.myfinancemanager.app.data.local.entity.PaymentMode
import com.myfinancemanager.app.data.local.entity.RecordOrigin
import com.myfinancemanager.app.data.local.entity.RecordStatus
import com.myfinancemanager.app.data.parser.TransactionParser
import com.myfinancemanager.app.util.Dates
import com.myfinancemanager.app.util.Ids
import com.myfinancemanager.app.util.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionParserTest {
    @Test
    fun parsesDebitSms() {
        val parsed = TransactionParser.parseSms(
            "VM-HDFCBK",
            "INR 1,250.00 debited from A/c XX12 at SWIGGY on 12-09-2026 via UPI"
        )
        assertNotNull(parsed)
        assertEquals(ParsedType.EXPENSE, parsed!!.type)
        assertEquals(1250.00, parsed.amount, 0.01)
        assertEquals(PaymentMode.UPI, parsed.paymentMode)
        assertEquals(ExpenseCategory.FOOD, parsed.expenseCategory)
    }

    @Test
    fun parsesCreditSms() {
        val parsed = TransactionParser.parseSms(
            "VM-ICICIB",
            "Rs. 85000 credited to your account towards SALARY on 01-09-2026"
        )
        assertNotNull(parsed)
        assertEquals(ParsedType.INCOME, parsed!!.type)
        assertEquals(85000.0, parsed.amount, 0.01)
        assertEquals(IncomeCategory.SALARY, parsed.incomeCategory)
    }

    @Test
    fun parsesCsvCreditDebit() {
        val headers = listOf("Date", "Description", "Debit", "Credit")
        val expense = TransactionParser.parseCsvLine(headers, listOf("12/09/2026", "UBER INDIA", "320", ""))
        val income = TransactionParser.parseCsvLine(headers, listOf("01/09/2026", "SALARY CREDIT", "", "50000"))
        assertEquals(ParsedType.EXPENSE, expense!!.type)
        assertEquals(320.0, expense.amount, 0.01)
        assertEquals(ParsedType.INCOME, income!!.type)
        assertEquals(50000.0, income.amount, 0.01)
    }

    @Test
    fun fingerprintIsStable() {
        val a = Ids.fingerprint(listOf("u1", "expense", "100.0", "Cafe", "1"))
        val b = Ids.fingerprint(listOf("u1", "expense", "100.0", "Cafe", "1"))
        assertEquals(a, b)
    }

    @Test
    fun moneyPercentHandlesZero() {
        assertEquals(0.0, Money.percent(10.0, 0.0), 0.0)
        assertEquals(10.0, Money.percent(10.0, 100.0), 0.0)
    }

    @Test
    fun insightEngineProducesSavingsTip() {
        val now = Dates.now()
        val incomes = listOf(
            IncomeEntity("1", "u", 100000.0, "Job", IncomeCategory.SALARY, now, RecordOrigin.MANUAL, "", false, RecordStatus.CONFIRMED, "a", now, now)
        )
        val expenses = listOf(
            ExpenseEntity("2", "u", 40000.0, "Swiggy", ExpenseCategory.FOOD, PaymentMode.UPI, now, RecordOrigin.MANUAL, "", false, RecordStatus.CONFIRMED, "b", now, now)
        )
        val insights = InsightEngine.generate("u", incomes, expenses, emptyList(), "INR")
        assertTrue(insights.isNotEmpty())
        assertTrue(insights.any { it.category == "savings" })
    }
}
