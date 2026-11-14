package fi.heatcall.sms

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import fi.heatcall.HeatCallApp
import fi.heatcall.MainActivity
import fi.heatcall.R
import fi.heatcall.data.LogEntry
import kotlinx.coroutines.launch

/**
 * Vastaanottaa saapuvat tekstiviestit. Vain tallennettujen lämmittimien numeroista tulevat
 * käsitellään: ne tallennetaan lokiin, näytetään vastausikkunassa ja merkitään luetuksi
 * oletusviestisovelluksessa (ks. [MarkReadListener]).
 *
 * Viesti menee silti normaalisti myös oletusviestisovellukseen; Android ei salli
 * muun kuin oletus-SMS-sovelluksen estää sitä.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val sender = messages.first().originatingAddress
        // Pitkä viesti voi tulla useana osana.
        val body = messages.joinToString("") { it.messageBody ?: "" }.trim()
        val app = HeatCallApp.instance
        val pending = goAsync()

        app.appScope.launch {
            try {
                // Luetaan tila levyltä: prosessi on voinut käynnistyä juuri tätä viestiä varten.
                val state = app.repository.current()
                val device = state.deviceForSender(sender) ?: return@launch
                val now = System.currentTimeMillis()
                val parsed = ReplyParser.parse(body)

                MarkReadDiagnostics.log(
                    context,
                    "Lämmittimen viesti vastaanotettu (${device.displayName})" +
                        if (state.markRepliesRead) "" else " – luetuksi merkintä pois päältä",
                )
                if (state.markRepliesRead) MarkReadListener.enqueue(body)

                val reply = ReplyBus.IncomingReply(device.displayName, body, now, device.id)
                ReplyBus.showReply(reply)
                if (!ReplyBus.isForeground) showNotification(context, reply, parsed)

                app.repository.appendLog(LogEntry(now, outgoing = false, deviceName = device.displayName, text = body))
                parsed.heaterOn?.let { on ->
                    app.repository.update(device.id) { it.copy(lastKnownOn = on, lastStatusAt = now) }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun showNotification(context: Context, reply: ReplyBus.IncomingReply, parsed: ReplyParser.Reply) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(MainActivity.EXTRA_REPLY_DEVICE, reply.deviceName)
                .putExtra(MainActivity.EXTRA_REPLY_TEXT, reply.text)
                .putExtra(MainActivity.EXTRA_REPLY_TIME, reply.timestamp)
                .putExtra(MainActivity.EXTRA_REPLY_DEVICE_ID, reply.deviceId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, HeatCallApp.CHANNEL_REPLIES)
            .setSmallIcon(R.drawable.ic_stat_flame)
            .setContentTitle("${reply.deviceName}: ${parsed.title}")
            .setContentText(parsed.lines.firstOrNull() ?: reply.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(parsed.lines.joinToString("\n")))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val NOTIFICATION_ID = 1001
    }
}
