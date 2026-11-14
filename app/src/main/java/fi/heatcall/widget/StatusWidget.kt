package fi.heatcall.widget

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import fi.heatcall.HeatCallApp
import fi.heatcall.MainActivity
import fi.heatcall.R
import fi.heatcall.data.AppState
import fi.heatcall.sms.Commands
import fi.heatcall.sms.SmsSender
import fi.heatcall.ui.screens.STATUS_LABEL
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Kotinäytön widget: näyttää valitun auton lämmittimen viimeisimmän tilan ja
 * lähettää tilakyselyn (heater?) Tarkista tila -painikkeesta avaamatta sovellusta.
 *
 * Widget päivittyy, kun lämmittimen vastaus tallentuu (HeatCallApp kuuntelee tilaa).
 */
class StatusWidget : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_CHECK -> runAsync { check(context) }
            ACTION_REFRESH -> runAsync { renderAll(context, HeatCallApp.instance.repository.current()) }
            else -> super.onReceive(context, intent)
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        runAsync { renderAll(context, HeatCallApp.instance.repository.current()) }
    }

    private fun runAsync(block: suspend () -> Unit) {
        val pending = goAsync()
        HeatCallApp.instance.appScope.launch {
            try {
                block()
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun check(context: Context) {
        val state = HeatCallApp.instance.repository.current()
        val device = state.selected
        val prefs = prefs(context)
        when {
            device == null -> prefs.edit { putString(KEY_ERROR, "Lisää laite sovelluksessa") }
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED ->
                prefs.edit { putString(KEY_ERROR, "Avaa sovellus ja salli tekstiviestit") }
            else -> {
                prefs.edit {
                    putLong(KEY_CHECKING_SINCE, System.currentTimeMillis())
                    remove(KEY_ERROR)
                }
                SmsSender.send(context, device, Commands.heaterStatus(device.pin), STATUS_LABEL)
                scheduleRefresh(context)
            }
        }
        renderAll(context, state)
    }

    /** Herättää widgetin aikakatkaisun jälkeen, ettei "Tarkistetaan…" jää näkyviin. */
    private fun scheduleRefresh(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.set(
            AlarmManager.RTC,
            System.currentTimeMillis() + TIMEOUT_MS + 5_000,
            broadcast(context, ACTION_REFRESH, 2),
        )
    }

    companion object {
        const val ACTION_CHECK = "fi.heatcall.widget.CHECK"
        const val ACTION_REFRESH = "fi.heatcall.widget.REFRESH"
        private const val TIMEOUT_MS = 3 * 60 * 1000L
        private const val PREFS = "widget"
        private const val KEY_CHECKING_SINCE = "checking_since"
        private const val KEY_ERROR = "error"

        private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        /** Lähetys epäonnistui (esim. ei kenttää): lopetetaan odotus ja näytetään syy. */
        fun markFailed(context: Context, reason: String) {
            val p = prefs(context)
            if (p.getLong(KEY_CHECKING_SINCE, 0L) == 0L) return
            p.edit {
                remove(KEY_CHECKING_SINCE)
                putString(KEY_ERROR, reason)
            }
            context.sendBroadcast(Intent(context, StatusWidget::class.java).setAction(ACTION_REFRESH))
        }

        /** Pyytää käynnistintä lisäämään widgetin kotinäytölle (käyttäjä vahvistaa). */
        fun requestPin(context: Context): Boolean {
            val manager = AppWidgetManager.getInstance(context)
            if (!manager.isRequestPinAppWidgetSupported) return false
            return manager.requestPinAppWidget(ComponentName(context, StatusWidget::class.java), null, null)
        }

        fun hasWidgets(context: Context): Boolean =
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, StatusWidget::class.java))
                .isNotEmpty()

        fun renderAll(context: Context, state: AppState) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, StatusWidget::class.java))
            if (ids.isEmpty()) return
            val views = build(context, state)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        private fun broadcast(context: Context, action: String, requestCode: Int): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, StatusWidget::class.java).setAction(action),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        private fun build(context: Context, state: AppState): RemoteViews {
            val v = RemoteViews(context.packageName, R.layout.widget_status)
            val openApp = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            v.setOnClickPendingIntent(R.id.widget_root, openApp)
            v.setOnClickPendingIntent(R.id.widget_check, broadcast(context, ACTION_CHECK, 1))

            val device = state.selected
            val p = prefs(context)
            val now = System.currentTimeMillis()
            val since = p.getLong(KEY_CHECKING_SINCE, 0L)
            val error = p.getString(KEY_ERROR, null)

            if (device == null) {
                v.setTextViewText(R.id.widget_title, "HeatCall")
                v.setTextViewText(R.id.widget_status, "Ei laitteita")
                v.setTextViewText(R.id.widget_sub, "Lisää lämmitin sovelluksessa")
                v.setInt(R.id.widget_dot, "setColorFilter", COLOR_UNKNOWN)
                setChecking(v, false)
                return v
            }

            // Vastaus tuli kyselyn jälkeen → odotus päättyy.
            val answered = since > 0 && device.lastStatusAt >= since
            if (answered) p.edit { remove(KEY_CHECKING_SINCE) }
            val checking = since > 0 && !answered && now - since < TIMEOUT_MS
            val timedOut = since > 0 && !answered && now - since >= TIMEOUT_MS

            v.setTextViewText(R.id.widget_title, device.displayName)
            val (text, color) = when (device.lastKnownOn) {
                true -> "Lämmitin päällä" to COLOR_ON
                false -> "Lämmitin pois" to COLOR_OFF
                null -> "Tila ei tiedossa" to COLOR_UNKNOWN
            }
            v.setTextViewText(R.id.widget_status, text)
            v.setInt(R.id.widget_dot, "setColorFilter", color)
            v.setTextViewText(
                R.id.widget_sub,
                when {
                    checking -> "Tarkistetaan…"
                    timedOut -> "Ei vastausta – yritä uudelleen"
                    error != null -> error
                    device.lastStatusAt > 0 -> "Päivitetty " + formatTime(device.lastStatusAt)
                    else -> "Paina Tarkista tila"
                },
            )
            setChecking(v, checking)
            return v
        }

        private fun setChecking(v: RemoteViews, checking: Boolean) {
            v.setViewVisibility(R.id.widget_progress, if (checking) View.VISIBLE else View.GONE)
            v.setViewVisibility(R.id.widget_refresh_icon, if (checking) View.GONE else View.VISIBLE)
            v.setTextViewText(R.id.widget_check_text, if (checking) "Odotetaan vastausta" else "Tarkista tila")
        }

        /** "tänään klo 15.09" tai "14.11. klo 15.09". */
        private fun formatTime(at: Long): String {
            val fi = Locale.forLanguageTag("fi-FI")
            val then = Calendar.getInstance().apply { timeInMillis = at }
            val today = Calendar.getInstance()
            val sameDay = then.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                then.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
            val time = SimpleDateFormat("HH.mm", fi).format(Date(at))
            return if (sameDay) "tänään klo $time" else SimpleDateFormat("d.M.", fi).format(Date(at)) + " klo $time"
        }

        private const val COLOR_ON = 0xFFFF7043.toInt()
        private const val COLOR_OFF = 0xFF78909C.toInt()
        private const val COLOR_UNKNOWN = 0x88FFFFFF.toInt()
    }
}
