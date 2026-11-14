package fi.heatcall.sms

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import fi.heatcall.HeatCallApp

/**
 * Merkitsee lämmittimen vastausviestit luetuksi käyttäjän oletusviestisovelluksessa.
 *
 * Android sallii viestien merkitsemisen luetuksi suoraan vain oletus-SMS-sovellukselle.
 * Siksi HeatCall painaa oletusviestisovelluksen ilmoituksen "Merkitse luetuksi"
 * -toimintoa. Google Messages ja Samsung Messages tarjoavat sen kumpikin.
 * Oletusviestisovellus tunnistetaan Telephony.Sms.getDefaultSmsPackage()-kutsulla, joten
 * vaihtaminen toiseen viestisovellukseen ei vaadi asetusmuutoksia.
 */
class MarkReadListener : NotificationListenerService() {

    private data class Pending(val body: String, val receivedAt: Long)

    override fun onListenerConnected() {
        connected = this
        scanActive()
    }

    override fun onListenerDisconnected() {
        if (connected === this) connected = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        handle(sbn)
    }

    private fun scanActive() {
        runCatching { activeNotifications }.getOrNull()?.forEach { handle(it) }
    }

    private fun handle(sbn: StatusBarNotification) {
        val state = HeatCallApp.instance.cachedState
        if (!state.markRepliesRead || state.devices.isEmpty()) return

        val smsPackage = Telephony.Sms.getDefaultSmsPackage(this) ?: return
        if (sbn.packageName != smsPackage) return

        prunePending()
        val n = sbn.notification
        val text = collectText(n.extras)
        val title = n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: n.extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()

        val matchesPending = synchronized(pending) {
            pending.any { p -> text.contains(fingerprint(p.body), ignoreCase = true) }
        }
        val fromHeater = state.deviceForSender(title) != null
        if (!matchesPending && !fromHeater) return

        val action = findMarkReadAction(n) ?: run {
            Log.i(TAG, "Ilmoituksessa ei ole Merkitse luetuksi -toimintoa ($smsPackage)")
            return
        }
        try {
            action.actionIntent.send()
            synchronized(pending) {
                pending.removeAll { p -> text.contains(fingerprint(p.body), ignoreCase = true) }
            }
            Log.i(TAG, "Lämmittimen viesti merkitty luetuksi ($smsPackage)")
        } catch (e: Exception) {
            Log.w(TAG, "Luetuksi merkintä epäonnistui", e)
        }
    }

    private fun findMarkReadAction(n: Notification): Notification.Action? {
        val actions = n.actions ?: return null
        actions.firstOrNull { it.semanticAction == Notification.Action.SEMANTIC_ACTION_MARK_AS_READ }
            ?.let { return it }
        // Varalla: tunnista toiminto otsikon perusteella (fi/en/sv/de).
        val labels = listOf("luetuksi", "mark as read", "mark read", "läst", "gelesen")
        return actions.firstOrNull { a ->
            val t = a.title?.toString()?.lowercase() ?: return@firstOrNull false
            labels.any { t.contains(it) }
        }
    }

    /** Kerää ilmoituksen kaikki tekstikentät, myös MessagingStyle-viestit. */
    private fun collectText(extras: Bundle): String {
        val parts = mutableListOf<CharSequence?>()
        parts += extras.getCharSequence(Notification.EXTRA_TEXT)
        parts += extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.let { parts += it }
        @Suppress("DEPRECATION")
        extras.getParcelableArray(Notification.EXTRA_MESSAGES)?.forEach { m ->
            (m as? Bundle)?.getCharSequence("text")?.let { parts += it }
        }
        return parts.filterNotNull().joinToString("\n")
    }

    companion object {
        private const val TAG = "HeatCallMarkRead"
        private const val PENDING_TTL_MS = 10 * 60 * 1000L

        @Volatile
        private var connected: MarkReadListener? = null
        private val pending = mutableListOf<Pending>()

        /** Lisää vastaanotetun viestin jonoon ja yrittää merkitä sen heti. */
        fun enqueue(body: String) {
            synchronized(pending) { pending += Pending(body, System.currentTimeMillis()) }
            connected?.scanActive()
        }

        private fun prunePending() {
            val limit = System.currentTimeMillis() - PENDING_TTL_MS
            synchronized(pending) { pending.removeAll { it.receivedAt < limit } }
        }

        /** Ilmoitus voi lyhentää tai muotoilla viestiä, joten verrataan alkuosaa. */
        private fun fingerprint(body: String): String = body.trim().take(24)

        fun isAccessGranted(context: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

        /** Avaa ilmoitusten käyttöoikeuden asetukset suoraan HeatCallin kohdalle (Android 11+). */
        fun settingsIntent(context: Context): android.content.Intent {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                val component = ComponentName(context, MarkReadListener::class.java).flattenToString()
                return android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                    .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component)
            }
            return fallbackSettingsIntent()
        }

        fun fallbackSettingsIntent(): android.content.Intent =
            android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    }
}
