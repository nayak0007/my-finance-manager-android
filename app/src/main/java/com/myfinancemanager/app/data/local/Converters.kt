package com.myfinancemanager.app.data.local

import androidx.room.TypeConverter
import com.myfinancemanager.app.data.local.entity.AutoCaptureStatus
import com.myfinancemanager.app.data.local.entity.ExpenseCategory
import com.myfinancemanager.app.data.local.entity.IncomeCategory
import com.myfinancemanager.app.data.local.entity.InvestmentType
import com.myfinancemanager.app.data.local.entity.ParsedType
import com.myfinancemanager.app.data.local.entity.PaymentMode
import com.myfinancemanager.app.data.local.entity.RecordOrigin
import com.myfinancemanager.app.data.local.entity.RecordStatus

class Converters {
    @TypeConverter fun fromOrigin(value: RecordOrigin): String = value.name
    @TypeConverter fun toOrigin(value: String): RecordOrigin = RecordOrigin.valueOf(value)
    @TypeConverter fun fromStatus(value: RecordStatus): String = value.name
    @TypeConverter fun toStatus(value: String): RecordStatus = RecordStatus.valueOf(value)
    @TypeConverter fun fromIncomeCat(value: IncomeCategory): String = value.name
    @TypeConverter fun toIncomeCat(value: String): IncomeCategory = IncomeCategory.valueOf(value)
    @TypeConverter fun fromExpenseCat(value: ExpenseCategory): String = value.name
    @TypeConverter fun toExpenseCat(value: String): ExpenseCategory = ExpenseCategory.valueOf(value)
    @TypeConverter fun fromPayment(value: PaymentMode): String = value.name
    @TypeConverter fun toPayment(value: String): PaymentMode = PaymentMode.valueOf(value)
    @TypeConverter fun fromInvType(value: InvestmentType): String = value.name
    @TypeConverter fun toInvType(value: String): InvestmentType = InvestmentType.valueOf(value)
    @TypeConverter fun fromAutoStatus(value: AutoCaptureStatus): String = value.name
    @TypeConverter fun toAutoStatus(value: String): AutoCaptureStatus = AutoCaptureStatus.valueOf(value)
    @TypeConverter fun fromParsed(value: ParsedType): String = value.name
    @TypeConverter fun toParsed(value: String): ParsedType = ParsedType.valueOf(value)
}
