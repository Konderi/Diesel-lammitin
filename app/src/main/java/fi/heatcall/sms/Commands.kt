package fi.heatcall.sms

import fi.heatcall.i18n.AppLanguage
import fi.heatcall.i18n.I18n
import fi.heatcall.i18n.tr

/**
 * EasyStart Call -tekstiviestikomennot (käyttöohje 08.2014, s. 17–27).
 *
 * Muoto: "pin:1234 <komento>". PIN:n ja komennon välissä on aina välilyönti.
 * Laite hyväksyy sekä isot että pienet kirjaimet.
 */
object Commands {

    const val RUNTIME_MIN = 10
    const val RUNTIME_MAX = 120
    const val RUNTIME_DEFAULT = 30

    /** Viikonpäivät ja -alueet laitteen omilla englanninkielisillä lyhenteillä. */
    enum class Day(val code: String, private val fi: String, private val en: String) {
        MO("MO", "Maanantai", "Monday"),
        TU("TU", "Tiistai", "Tuesday"),
        WE("WE", "Keskiviikko", "Wednesday"),
        TH("TH", "Torstai", "Thursday"),
        FR("FR", "Perjantai", "Friday"),
        SA("SA", "Lauantai", "Saturday"),
        SU("SU", "Sunnuntai", "Sunday"),
        MO_FR("MO-FR", "Ma–pe", "Mon–Fri"),
        SA_SU("SA-SU", "La–su", "Sat–Sun"),
        MO_SU("MO-SU", "Joka päivä", "Every day");

        /** Nimi valitulla kielellä. */
        val label: String get() = tr(fi, en)

        /** Nimi lauseen keskellä: suomeksi pienellä, englanniksi isolla. */
        val inSentence: String get() = if (I18n.language == AppLanguage.FI) label.lowercase() else label

        companion object {
            fun fromCode(code: String): Day? = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
        }
    }

    fun clampRuntime(minutes: Int): Int = minutes.coerceIn(RUNTIME_MIN, RUNTIME_MAX)

    private fun withPin(pin: String, body: String): String = "pin:$pin $body"

    // --- Lämmitys -----------------------------------------------------------

    /**
     * Lämmitys tai tuuletus päälle.
     * @param permanent kestokäyttö (run:perm) – vain ilmalämmittimet
     * @param fan tuuletus (mode:fan) – vain lämmittimet, joissa on tuuletintoiminto
     */
    fun heaterOn(pin: String, runMinutes: Int, fan: Boolean = false, permanent: Boolean = false): String {
        val run = if (permanent) "perm" else clampRuntime(runMinutes).toString()
        val mode = if (fan) "mode:fan," else ""
        return withPin(pin, "heater:on,${mode}run:$run")
    }

    fun heaterOff(pin: String): String = withPin(pin, "heater:off")

    /** Tilakysely – laite vastaa aina, vaikka kuittausilmoitus olisi pois päältä. */
    fun heaterStatus(pin: String): String = withPin(pin, "heater?")

    fun temperature(pin: String): String = withPin(pin, "temp?")

    // --- Ajastimet ----------------------------------------------------------

    fun formatTime(day: Day, hour: Int, minute: Int): String =
        "%s.%02d.%02d".format(day.code, hour, minute)

    /** Ohjelmoi ja aktivoi ajastimen 1–3. */
    fun timerProgram(pin: String, timer: Int, day: Day, hour: Int, minute: Int, runMinutes: Int): String {
        require(timer in 1..3) { "Ajastin 1–3" }
        require(hour in 0..23 && minute in 0..59) { "Virheellinen kellonaika" }
        return withPin(pin, "t$timer:on,time:${formatTime(day, hour, minute)},run:${clampRuntime(runMinutes)}")
    }

    /** Aktivoi aiemmin ohjelmoidun ajastimen sen tallennetuilla arvoilla. */
    fun timerOn(pin: String, timer: Int): String = withPin(pin, "t$timer:on")

    fun timerOff(pin: String, timer: Int): String = withPin(pin, "t$timer:off")

    fun timerStatus(pin: String, timer: Int): String = withPin(pin, "t$timer?")

    fun allTimersStatus(pin: String): String = withPin(pin, "tall?")

    // --- Laite --------------------------------------------------------------

    fun deviceTimeQuery(pin: String): String = withPin(pin, "devicetime?")

    fun deviceTimeSet(pin: String, day: Day, hour: Int, minute: Int): String {
        require(day.ordinal <= Day.SU.ordinal) { "Kellon asetukseen tarvitaan yksittäinen päivä" }
        return withPin(pin, "devicetime:set,time:${formatTime(day, hour, minute)}")
    }

    fun mainPhoneQuery(pin: String): String = withPin(pin, "mainphone?")

    /** Valtuuttaa viestin lähettäjän numeron (tämän puhelimen). */
    fun mainPhoneSetMine(pin: String): String = withPin(pin, "mainphone:set,number:mine")

    fun changePin(pin: String, newPin: String): String {
        require(isValidPin(newPin)) { "PIN on 4 numeroa" }
        return withPin(pin, "config:set,pin:$newPin")
    }

    // --- Validointi ---------------------------------------------------------

    fun isValidPin(pin: String): Boolean = pin.length == 4 && pin.all { it.isDigit() }

    /** Hyväksyy muodot +358401234567, 00358401234567 ja 0401234567. */
    fun isValidPhone(phone: String): Boolean {
        val p = phone.replace(" ", "").replace("-", "")
        val digits = if (p.startsWith("+")) p.drop(1) else p
        return digits.length in 6..20 && digits.all { it.isDigit() }
    }

    fun normalizePhone(phone: String): String = phone.replace(" ", "").replace("-", "")

    /**
     * Vertaa numeroita viimeisten 9 numeron perusteella, jolloin
     * +358401234567, 00358401234567 ja 0401234567 ovat sama numero.
     */
    fun samePhone(a: String?, b: String?): Boolean {
        if (a.isNullOrBlank() || b.isNullOrBlank()) return false
        val da = a.filter { it.isDigit() }
        val db = b.filter { it.isDigit() }
        if (da.isEmpty() || db.isEmpty()) return false
        val n = minOf(9, da.length, db.length)
        return da.takeLast(n) == db.takeLast(n)
    }
}
