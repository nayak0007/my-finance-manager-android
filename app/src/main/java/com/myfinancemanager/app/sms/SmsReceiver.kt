package com.myfinancemanager.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.myfinancemanager.app.MyFinanceApp
import com.myfinancemanager.app.data.parser.TransactionParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val pending = goAsync()
        val app = context.applicationContext as? MyFinanceApp ?: run {
            pending.finish()
            return
        }
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = app.container.userPreferences.prefs.first()
                if (!prefs.smsCaptureEnabled) return@launch
                val session = app.container.sessionStore.session.first() ?: return@launch
                messages.forEach { sms ->
                    val sender = sms.displayOriginatingAddress.orEmpty()
                    val body = sms.displayMessageBody.orEmpty()
                    val parsed = TransactionParser.parseSms(sender, body, sms.timestampMillis) ?: return@forEach
                    app.container.financeRepository.enqueueAutoCapture(
                        userId = session.userId,
                        sender = sender,
                        rawText = body,
                        parsed = parsed
                    )
                }
            } finally {
                pending.finish()
            }
        }
    }
}
