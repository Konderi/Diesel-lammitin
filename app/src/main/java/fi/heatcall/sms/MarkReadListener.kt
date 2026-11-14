package fi.heatcall.sms

import android.app.Notification
import android.app.Person
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
import fi.heatcall.data.AppState
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Merkitsee lämmittimen vastausviestit luetuksi käyttäjän viestisovelluksessa.
 *
 * Android sallii viestien merkitsemisen luetuksi suoraan vain oletus-SMS-sovellukselle.
 * Siksi HeatCall painaa viestisovelluksen ilmoituksen "Merkitse luetuksi" -toimintoa.
 *
 * Lämmittimen viesti tunnistetaan ilmoituksesta usealla tavalla, koska valmistajien
 * viestisovellukset (Google, Samsung, Oppo/ColorOS, Xiaomi …) rakentavat ilmoituksen eri tavoin:
 *  1. teksti vastaa juuri vastaanotettua viestiä (SmsReceiver → [enqueue])
 *  2. teksti on EasyStart Callin protokollaa (Heater:…, T1:…, ErD:… ym.)
 *  3. otsikko tai MessagingStyle-lähettäjän tel:-URI on lämmittimen numero
 * Jokainen päätös kirjataan [MarkReadDiagnostics]-lokiin.
 */
class MarkReadListener : NotificationListenerService() {

    private data class Pending(val body: String, val receivedAt: Long)

    override fun onListenerConnected() {
        connected = this
        MarkReadDiagnostics.log(this, "Kuuntelija yhdistetty")
        scanActive()
    }

    override fun onListenerDisconnected() {
        if (connected === this) connected = null
        MarkReadDiagnostics.log(this, "Kuuntelija katkesi – pyydetään uudelleenyhdistystä")
        // Jotkin valmistajat (esim. ColorOS) katkaisevat kuuntelijan taustalla.
        runCatching { NotificationListenerService.requestRebind(ComponentName(this, MarkReadListener::class.java)) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        handle(sbn)
    }

    private fun scanActive() {
        runCatching { activeNotifications }.getOrNull()?.forEach { handle(it) }
    }

    /** Muistissa oleva tila, tai levyltä luettu, jos prosessi käynnistyi juuri. */
    private fun currentState(): AppState {
        val cached = HeatCallApp.instance.cachedState
        if (cached.devices.isNotEmpty()) return cached
        return runBlocking { withTimeoutOrNull(1_000) { HeatCallApp.instance.repository.current() } } ?: cached
    }

    private fun handle(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        val defaultSms = Telephony.Sms.getDefaultSmsPackage(this)
        if (pkg != defaultSms && pkg !in KNOWN_SMS_PACKAGES) return
        // Ryhmän yhteenvetoilmoituksessa ei ole toimintoja; yksittäinen ilmoitus käsitellään erikseen.
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0 && sbn.notification.actions.isNullOrEmpty()) return

        val state = currentState()
        if (!state.markRepliesRead || state.devices.isEmpty()) return

        prunePending()
        val n = sbn.notification
        val text = collectText(n.extras)
        val title = n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: n.extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()

        val matchesPending = synchronized(pending) {
            pending.any { p -> text.contains(fingerprint(p.body), ignoreCase = true) }
        }
        val isProtocol = PROTOCOL.containsMatchIn(text)
        val fromHeaterTitle = state.deviceForSender(title) != null
        val fromHeaterUri = senderUris(n.extras).any { state.deviceForSender(it) != null }
        val reason = when {
            matchesPending -> "vastaanotettu viesti"
            isProtocol -> "lämmittimen viestimuoto"
            fromHeaterTitle -> "lähettäjän numero"
            fromHeaterUri -> "lähettäjän tel-osoite"
            else -> null
        }
        if (reason == null) return

        val actions = n.actions.orEmpty()
        val labels = actions.joinToString(", ") { "\"${it.title}\"" + if (it.semanticAction != 0) "#${it.semanticAction}" else "" }
        val action = findMarkReadAction(actions)
        if (action == null) {
            MarkReadDiagnostics.log(this, "$pkg: tunnistettu ($reason), mutta Merkitse luetuksi -toimintoa ei löytynyt. Toiminnot: [$labels]")
            Log.i(TAG, "Ei Merkitse luetuksi -toimintoa ($pkg): $labels")
            return
        }
        try {
            action.actionIntent.send()
            synchronized(pending) {
                pending.removeAll { p -> text.contains(fingerprint(p.body), ignoreCase = true) }
            }
            MarkReadDiagnostics.log(this, "$pkg: merkitty luetuksi ($reason) toiminnolla \"${action.title}\"")
            Log.i(TAG, "Lämmittimen viesti merkitty luetuksi ($pkg)")
        } catch (e: Exception) {
            MarkReadDiagnostics.log(this, "$pkg: toiminnon \"${action.title}\" suoritus epäonnistui: ${e.javaClass.simpleName}")
            Log.w(TAG, "Luetuksi merkintä epäonnistui", e)
        }
    }

    private fun findMarkReadAction(actions: Array<out Notification.Action>): Notification.Action? {
        actions.firstOrNull { it.semanticAction == Notification.Action.SEMANTIC_ACTION_MARK_AS_READ }
            ?.let { return it }
        // Varalla: tunnista toiminto otsikon perusteella. Vastaa-toiminnot (RemoteInput) ohitetaan.
        return actions.firstOrNull { a ->
            if (!a.remoteInputs.isNullOrEmpty()) return@firstOrNull false
            val t = a.title?.toString()?.lowercase() ?: return@firstOrNull false
            MARK_READ_LABELS.any { t.contains(it) } && EXCLUDE_LABELS.none { t.contains(it) }
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

    /** MessagingStyle-lähettäjien URI:t, esim. "tel:+358401234567". */
    private fun senderUris(extras: Bundle): List<String> {
        val uris = mutableListOf<String>()
        @Suppress("DEPRECATION")
        extras.getParcelableArrayList<Person>(Notification.EXTRA_PEOPLE_LIST)?.forEach { p ->
            p.uri?.let { uris += it }
        }
        @Suppress("DEPRECATION")
        extras.getParcelableArray(Notification.EXTRA_MESSAGES)?.forEach { m ->
            @Suppress("DEPRECATION")
            ((m as? Bundle)?.getParcelable<Person>("sender_person"))?.uri?.let { uris += it }
        }
        return uris.filter { it.startsWith("tel:") }
    }

    companion object {
        private const val TAG = "HeatCallMarkRead"
        private const val PENDING_TTL_MS = 10 * 60 * 1000L

        /** EasyStart Callin vastaukset alkavat näillä avaimilla (rivin alussa). */
        private val PROTOCOL = Regex(
            "(?im)^\\s*(heater|t[1-3]|temp|devicetime|mainphone|er[a-z]):",
        )

        /** Merkitse luetuksi -toiminnon nimiä eri kielillä ja valmistajilla. */
        private val MARK_READ_LABELS = listOf(
            "luetuksi", "luettu", "mark as read", "mark read", "as read", "läst", "gelesen", "已读",
        )

        /** Toimintoja, joita ei koskaan painettaisi (poisto, esto). */
        private val EXCLUDE_LABELS = listOf("poista", "delete", "estä", "block", "roskaposti", "spam")

        /** Viestisovelluksia, joita ei aina raportoida oletukseksi (esim. kaksois-SIM). */
        private val KNOWN_SMS_PACKAGES = setOf(
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.android.mms",
            "com.oplus.message",
            "com.coloros.mms",
            "com.oneplus.mms",
            "com.android.messaging",
            "com.miui.mms",
        )

        @Volatile
        private var connected: MarkReadListener? = null
        private val pending = mutableListOf<Pending>()

        /** Lisää vastaanotetun viestin jonoon ja yrittää merkitä sen heti. */
        fun enqueue(body: String) {
            synchronized(pending) { pending += Pending(body, System.currentTimeMillis()) }
            connected?.scanActive()
        }

        fun isConnected(): Boolean = connected != null

        private fun prunePending() {
            val limit = System.currentTimeMillis() - PENDING_TTL_MS
            synchronized(pending) { pending.removeAll { it.receivedAt < limit } }
        }

        /** Ilmoitus voi lyhentää tai muotoilla viestiä, joten verrataan alkuosaa. */
        private fun fingerprint(body: String): String = body.trim().take(24)

        fun isAccessGranted(context: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

        /** Pyytää järjestelmää yhdistämään kuuntelijan uudelleen (esim. sovelluksen käynnistyessä). */
        fun rebindIfNeeded(context: Context) {
            if (connected == null && isAccessGranted(context)) {
                runCatching { NotificationListenerService.requestRebind(ComponentName(context, MarkReadListener::class.java)) }
            }
        }

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
