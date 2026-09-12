package com.myfinancemanager.app.sms

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import com.myfinancemanager.app.data.parser.TransactionParser
import com.myfinancemanager.app.data.parser.ParsedTransaction

data class ScannedSms(
    val sender: String,
    val body: String,
    val parsed: ParsedTransaction
)

object InboxScanner {
    fun scan(context: Context, limit: Int = 80): List<ScannedSms> {
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)
        val result = mutableListOf<ScannedSms>()
        context.contentResolver.query(uri, projection, null, null, "${Telephony.Sms.DATE} DESC")?.use { cursor ->
            val addrIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
            val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)
            var count = 0
            while (cursor.moveToNext() && count < limit) {
                val sender = cursor.getString(addrIdx).orEmpty()
                val body = cursor.getString(bodyIdx).orEmpty()
                val date = cursor.getLong(dateIdx)
                val parsed = TransactionParser.parseSms(sender, body, date) ?: continue
                result += ScannedSms(sender, body, parsed)
                count++
            }
        }
        return result
    }
}
