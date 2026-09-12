package com.myfinancemanager.app.ui.navigation

object Routes {
    const val Splash = "splash"
    const val Login = "login"
    const val Signup = "signup"
    const val Onboarding = "onboarding"
    const val Home = "home"
    const val Income = "income"
    const val Expense = "expense"
    const val Investments = "investments"
    const val Insights = "insights"
    const val Import = "import"
    const val Queue = "queue"
    const val Settings = "settings"
    const val AddIncome = "add_income"
    const val AddExpense = "add_expense"
    const val AddInvestment = "add_investment"
    const val IncomeDetail = "income_detail/{id}"
    const val ExpenseDetail = "expense_detail/{id}"
    const val InvestmentDetail = "investment_detail/{id}"
    const val Budgets = "budgets"
    const val Senders = "senders"

    fun incomeDetail(id: String) = "income_detail/$id"
    fun expenseDetail(id: String) = "expense_detail/$id"
    fun investmentDetail(id: String) = "investment_detail/$id"
}

enum class TabDest(val route: String, val label: String) {
    Home(Routes.Home, "Home"),
    Income(Routes.Income, "Income"),
    Expense(Routes.Expense, "Spend"),
    Investments(Routes.Investments, "Invest"),
    Insights(Routes.Insights, "Insights")
}
