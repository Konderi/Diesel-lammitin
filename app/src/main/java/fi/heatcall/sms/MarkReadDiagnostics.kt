package fi.heatcall.sms

import android.content.Context
import androidx.core.content.edit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Luetuksi merkinnän vianmääritysloki (30 viimeisintä tapahtumaa).
 *
 * Eri valmistajien viestisovellukset (Google, Samsung, Oppo/ColorOS, Xiaomi …) rakentavat
 * ilmoituksensa eri tavoin. Loki näyttää, näkikö HeatCall ilmoituksen, tunnistiko se
 * lämmittimen viestin ja löytyikö "Merkitse luetuksi" -toiminto. Viestien sisältöä ei
 * tallenneta, vain otsikko (lähettäjä) ja toimintojen nimet.
 */
object MarkReadDiagnostics {

    private const val PREFS = "mark_read_diag"
    private const val KEY = "log"
    private const val MAX = 30
    private const val SEP = "\u001F"

    fun log(context: Context, line: String) {
        val time = SimpleDateFormat("d.M. HH.mm.ss", Locale.forLanguageTag("fi-FI")).format(Date())
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        synchronized(this) {
            val old = p.getString(KEY, "").orEmpty().split(SEP).filter { it.isNotBlank() }
            val updated = (listOf("$time  $line") + old).take(MAX)
            p.edit { putString(KEY, updated.joinToString(SEP)) }
        }
    }

    fun read(context: Context): List<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "").orEmpty().split(SEP).filter { it.isNotBlank() }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { remove(KEY) }
    }
}
