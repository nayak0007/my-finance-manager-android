package com.myfinancemanager.app.data.insights

import com.myfinancemanager.app.data.local.entity.ExpenseCategory
import com.myfinancemanager.app.data.local.entity.ExpenseEntity
import com.myfinancemanager.app.data.local.entity.IncomeEntity
import com.myfinancemanager.app.data.local.entity.InsightEntity
import com.myfinancemanager.app.data.local.entity.InvestmentEntity
import com.myfinancemanager.app.data.local.entity.InvestmentType
import com.myfinancemanager.app.util.Dates
import com.myfinancemanager.app.util.Ids
import com.myfinancemanager.app.util.Money
import java.time.YearMonth

object InsightEngine {
    fun generate(
        userId: String,
        incomes: List<IncomeEntity>,
        expenses: List<ExpenseEntity>,
        investments: List<InvestmentEntity>,
        currency: String
    ): List<InsightEntity> {
        val now = Dates.now()
        val current = YearMonth.now()
        val previous = current.minusMonths(1)
        val insights = mutableListOf<InsightEntity>()

        val currentIncome = sumIncome(incomes, current)
        val currentExpense = sumExpense(expenses, current)
        val prevExpense = sumExpense(expenses, previous)
        val savings = currentIncome - currentExpense
        val savingsRate = if (currentIncome > 0) (savings / currentIncome) * 100 else 0.0

        if (currentIncome > 0) {
            insights += insight(
                userId,
                "savings",
                "Your savings rate this month is ${"%.1f".format(savingsRate)}%. A common starting target is 20% of take-home pay.",
                now
            )
        }

        if (prevExpense > 0 && currentExpense > prevExpense * 1.15) {
            val delta = currentExpense - prevExpense
            insights += insight(
                userId,
                "spend_anomaly",
                "Spending is up ${Money.format(delta, currency)} versus ${Dates.formatMonth(previous)}. Review discretionary categories before month end.",
                now
            )
        }

        val byCategory = expenses.filter { inMonth(it.date, current) }.groupBy { it.category }
        val top = byCategory.maxByOrNull { it.value.sumOf { e -> e.amount } }
        if (top != null) {
            val total = top.value.sumOf { it.amount }
            insights += insight(
                userId,
                "budget",
                "${label(top.key)} is your largest spend this month at ${Money.format(total, currency)}. Setting a category budget can keep it in check.",
                now
            )
        }

        val food = byCategory[ExpenseCategory.FOOD].orEmpty().sumOf { it.amount }
        val entertainment = byCategory[ExpenseCategory.ENTERTAINMENT].orEmpty().sumOf { it.amount }
        if (food + entertainment > 0 && currentIncome > 0 && (food + entertainment) / currentIncome > 0.3) {
            insights += insight(
                userId,
                "budget",
                "Food and entertainment are over 30% of this month's income. Cooking more meals at home is a simple way to lift savings.",
                now
            )
        }

        val invested = investments.sumOf { it.amountInvested }
        val currentValue = investments.sumOf { it.currentValue }
        if (invested > 0) {
            val gainPct = Money.percent(currentValue - invested, invested)
            insights += insight(
                userId,
                "investing",
                "Portfolio is ${"%.1f".format(gainPct)}% versus cost. This is a snapshot, not a recommendation to buy or sell.",
                now
            )
            val types = investments.groupBy { it.type }.mapValues { it.value.sumOf { i -> i.currentValue } }
            if (types.size == 1 && investments.size >= 2) {
                insights += insight(
                    userId,
                    "diversification",
                    "Holdings are concentrated in ${labelType(types.keys.first())}. Spreading across uncorrelated asset types can reduce single-asset risk.",
                    now
                )
            }
        } else if (savings > 0) {
            insights += insight(
                userId,
                "investing",
                "You have positive monthly savings and no tracked investments yet. Even a small recurring amount can start a long-term habit.",
                now
            )
        }

        if (insights.isEmpty()) {
            insights += insight(
                userId,
                "general",
                "Add a few income and expense records to unlock personalized spending and savings observations.",
                now
            )
        }

        return insights
    }

    private fun insight(userId: String, category: String, text: String, now: Long): InsightEntity {
        return InsightEntity(
            id = Ids.new(),
            userId = userId,
            insightText = text,
            category = category,
            generatedAt = now
        )
    }

    private fun sumIncome(list: List<IncomeEntity>, month: YearMonth): Double {
        return list.filter { inMonth(it.date, month) }.sumOf { it.amount }
    }

    private fun sumExpense(list: List<ExpenseEntity>, month: YearMonth): Double {
        return list.filter { inMonth(it.date, month) }.sumOf { it.amount }
    }

    private fun inMonth(epoch: Long, month: YearMonth): Boolean {
        val d = Dates.toLocalDate(epoch)
        return d.year == month.year && d.month == month.month
    }

    private fun label(category: ExpenseCategory): String {
        return category.name.lowercase().replaceFirstChar { it.titlecase() }.replace('_', ' ')
    }

    private fun labelType(type: InvestmentType): String {
        return type.name.lowercase().replaceFirstChar { it.titlecase() }.replace('_', ' ')
    }
}
