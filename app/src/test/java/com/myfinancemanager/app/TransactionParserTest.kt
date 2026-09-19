package com.myfinancemanager.app

import com.myfinancemanager.app.data.local.entity.ExpenseCategory
import com.myfinancemanager.app.data.local.entity.IncomeCategory
import com.myfinancemanager.app.data.local.entity.ParsedType
import com.myfinancemanager.app.data.local.entity.PaymentMode
import com.myfinancemanager.app.data.local.entity.budgetIdFor
import com.myfinancemanager.app.data.parser.TransactionParser
import com.myfinancemanager.app.util.Ids
import com.myfinancemanager.app.util.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
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
    fun budgetIdIsDerivedFromUserAndCategory() {
        // The account keys budgets on (user, category), so the local row id has to be derivable
        // from the same pair for a push and a pull to meet on one row.
        assertEquals(budgetIdFor("u1", ExpenseCategory.FOOD), budgetIdFor("u1", ExpenseCategory.FOOD))
        assertNotEquals(budgetIdFor("u1", ExpenseCategory.FOOD), budgetIdFor("u2", ExpenseCategory.FOOD))
        assertNotEquals(budgetIdFor("u1", ExpenseCategory.FOOD), budgetIdFor("u1", ExpenseCategory.TRAVEL))
    }
}
