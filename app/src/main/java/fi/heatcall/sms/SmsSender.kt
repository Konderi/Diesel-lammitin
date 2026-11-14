package fi.heatcall.sms

import fi.heatcall.i18n.tr
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import fi.heatcall.HeatCallApp
import fi.heatcall.data.Device
import fi.heatcall.data.LogEntry
import fi.heatcall.widget.StatusWidget
import kotlinx.coroutines.launch

/** Lähettää komennon lämmittimelle taustalla, ilman viestisovellusta. */
object SmsSender {

    const val ACTION_SENT = "fi.heatcall.SMS_SENT"
    private const val EXTRA_LABEL = "label"

    /**
     * @param command valmis komento, esim. Commands.heaterOn(device.pin, 30)
     * @param label käyttäjälle näytettävä kuvaus, esim. "Käynnistys"
     */
    fun send(context: Context, device: Device, command: String, label: String) {
        if (device.phone.isBlank() || !Commands.isValidPin(device.pin)) {
            ReplyBus.setSendState(ReplyBus.SendState.Failed(tr("Laitteen numero tai PIN puuttuu", "Device number or PIN is missing")))
            return
        }
        ReplyBus.setSendState(ReplyBus.SendState.Sending(label, device.displayName))

        val sentIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_SENT,
            Intent(context, SentReceiver::class.java)
                .setAction(ACTION_SENT)
                .putExtra(EXTRA_LABEL, label),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        try {
            val sms: SmsManager = if (Build.VERSION.SDK_INT >= 31) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            sms.sendTextMessage(device.phone, null, command, sentIntent, null)
        } catch (e: Exception) {
            ReplyBus.setSendState(ReplyBus.SendState.Failed(e.message ?: tr("Lähetys epäonnistui", "Sending failed")))
            return
        }

        val app = HeatCallApp.instance
        app.appScope.launch {
            app.repository.appendLog(
                LogEntry(System.currentTimeMillis(), outgoing = true, deviceName = device.displayName, text = maskPin(command)),
            )
        }
    }

    /** Lokiin ei tallenneta PIN-koodia näkyviin. */
    fun maskPin(command: String): String =
        command.replace(Regex("^pin:\\d{4}", RegexOption.IGNORE_CASE), "pin:****")

    private const val REQUEST_SENT = 42

    /** Saa operaattorilta tiedon, lähtikö viesti. */
    class SentReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val label = intent.getStringExtra(EXTRA_LABEL) ?: ""
            if (resultCode == Activity.RESULT_OK) {
                ReplyBus.setSendState(ReplyBus.SendState.WaitingReply(label, System.currentTimeMillis()))
            } else {
                val reason = when (resultCode) {
                    SmsManager.RESULT_ERROR_NO_SERVICE -> tr("Ei verkkoyhteyttä", "No network")
                    SmsManager.RESULT_ERROR_RADIO_OFF -> tr("Lentokonetila on päällä", "Airplane mode is on")
                    else -> tr("Viestin lähetys epäonnistui (koodi $resultCode)", "Sending the message failed (code $resultCode)")
                }
                ReplyBus.setSendState(ReplyBus.SendState.Failed(reason))
                StatusWidget.markFailed(context, reason)
            }
        }
    }
}
