package fi.heatcall.i18n

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import java.util.Locale

/** Sovelluksen kielet. */
enum class AppLanguage(val code: String, val nativeName: String) {
    FI("fi", "Suomi"),
    EN("en", "English"),
}

/**
 * Sovelluksen oma kielivalinta (suomi/englanti), riippumaton puhelimen kielestä.
 *
 * [language] on Compose-tila, joten näkymät päivittyvät heti kielen vaihtuessa.
 * Tekstit kirjoitetaan koodiin parina: `tr("Suomeksi", "In English")`.
 */
object I18n {
    private const val PREFS = "i18n"
    private const val KEY = "language"

    /** Nykyinen kieli. Oletus ennen [init]-kutsua (esim. yksikkötesteissä) on suomi. */
    var language by mutableStateOf(AppLanguage.FI)

    /** Onko käyttäjä valinnut kielen itse (muuten käytetään puhelimen kieltä). */
    var chosenByUser: Boolean = false
        private set

    val locale: Locale
        get() = if (language == AppLanguage.FI) Locale.forLanguageTag("fi-FI") else Locale.UK

    fun init(context: Context) {
        val code = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        chosenByUser = code != null
        language = AppLanguage.entries.firstOrNull { it.code == code } ?: systemDefault()
    }

    fun set(context: Context, lang: AppLanguage) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(KEY, lang.code) }
        chosenByUser = true
        language = lang
    }

    /** Suomenkielinen puhelin → suomi, muuten englanti. */
    fun systemDefault(): AppLanguage =
        if (Locale.getDefault().language == "fi") AppLanguage.FI else AppLanguage.EN
}

/** Palauttaa tekstin valitulla kielellä. */
fun tr(fi: String, en: String): String = if (I18n.language == AppLanguage.FI) fi else en
