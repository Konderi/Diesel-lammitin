package fi.heatcall.sms

/**
 * Tulkitsee EasyStart Callin vastausviestit suomenkieliseksi tekstiksi.
 *
 * Esimerkkejä vastauksista:
 *  - Heater:on,mode:heater,run:45
 *  - Heater:off
 *  - T1:on,time:TU.06.30,mode:heater,run:45,T2:off,T3:off
 *  - Temp:ok,value:-4
 *  - DeviceTime:ok,time:FR.06.30
 *  - MainPhone:ok,number:+358401234567
 *  - ErD:HeaterCom:lost,Heater:off
 */
object ReplyParser {

    data class Reply(
        val title: String,
        val lines: List<String>,
        val isError: Boolean,
        /** true = lämmitin päällä, false = pois, null = vastaus ei kertonut tilaa. */
        val heaterOn: Boolean?,
    )

    private val errorKey = Regex("^er[a-z]$", RegexOption.IGNORE_CASE)
    private val timerKey = Regex("^t([1-3])$", RegexOption.IGNORE_CASE)

    private val knownErrors: Map<String, String> = mapOf(
        "erdheatercomlost" to "Yhteys lämmittimeen katkesi",
        "erdheatercomno_heater_found" to "Lämmitintä ei tunnistettu – kokeile sulakkeen irrotusta ja uutta käynnistystä",
        "erldatcomlost" to "Johdotusvika – ota yhteys huoltoon",
        "erhheatermode_not_supported" to "Lämmitin ei tue pyydettyä toimintoa",
        "erhheatererror" to "Lämmittimessä on häiriö – yritä käynnistää uudelleen",
        "ercheatererror" to "Lämmittimessä on häiriö",
        "erctempmissing" to "Lämpötila-anturia ei ole asennettu",
        "erctempshort_circuit" to "Lämpötila-anturi on viallinen",
        "ercdevicetimetime_not_set" to "Laitteen kelloa ei ole asetettu",
        "errdevicetimetime_not_set" to "Laitteen kelloa ei ole asetettu – ajastin ei toimi",
    )

    fun parse(raw: String): Reply {
        val tokens = raw.trim().split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val lines = mutableListOf<String>()
        val errors = mutableListOf<String>()
        var heaterOn: Boolean? = null
        var context = Context.NONE
        var firstContext: Context? = null

        for (token in tokens) {
            val idx = token.indexOf(':')
            val key = if (idx >= 0) token.substring(0, idx) else token
            val value = if (idx >= 0) token.substring(idx + 1) else ""
            val k = key.lowercase()

            when {
                errorKey.matches(key) -> errors += explainError(token)

                k == "heater" -> {
                    context = Context.HEATER
                    heaterOn = value.equals("on", ignoreCase = true)
                    lines += if (heaterOn) "Lämmitin PÄÄLLÄ" else "Lämmitin POIS"
                }

                timerKey.matches(key) -> {
                    context = Context.TIMER
                    val n = timerKey.find(key)!!.groupValues[1]
                    lines += "Ajastin $n: " + if (value.equals("on", true)) "päällä" else "pois"
                }

                k == "mode" -> lines += indent(context) +
                    if (value.equals("fan", true)) "Tila: tuuletus" else "Tila: lämmitys"

                k == "run" -> lines += indent(context) +
                    if (value.equals("perm", true)) "Kestokäyttö" else "Käyntiaika $value min"

                k == "sp" -> lines += indent(context) + "Asetuslämpötila $value °C"

                k == "time" -> lines += when (context) {
                    Context.TIMER -> indent(context) + "Lähtöaika: " + describeTime(value)
                    else -> "Laitteen kello: " + describeTime(value)
                }

                k == "temp" -> context = Context.TEMP
                k == "value" -> lines += "Sisälämpötila $value °C"

                k == "devicetime" -> context = Context.DEVICE_TIME
                k == "mainphone" -> context = Context.MAIN_PHONE
                k == "number" -> lines += "Valtuutettu numero: $value"

                // "ok" ja muut tunnistamattomat näytetään sellaisenaan, ettei tietoa katoa.
                else -> if (!value.equals("ok", true)) lines += token
            }
            if (firstContext == null && context != Context.NONE) firstContext = context
        }

        val title = when {
            errors.isNotEmpty() -> "Virhe"
            firstContext == Context.HEATER -> if (heaterOn == true) "Lämmitin päällä" else "Lämmitin pois"
            firstContext == Context.TIMER -> "Ajastimet"
            firstContext == Context.TEMP -> "Sisälämpötila"
            firstContext == Context.DEVICE_TIME -> "Laitteen kello"
            firstContext == Context.MAIN_PHONE -> "Valtuutettu numero"
            else -> "Vastaus lämmittimeltä"
        }

        return Reply(
            title = title,
            lines = errors + lines,
            isError = errors.isNotEmpty(),
            heaterOn = heaterOn,
        )
    }

    fun explainError(token: String): String {
        val code = token.substringBefore(':')
        val norm = token.lowercase().replace(":", "")
        val text = knownErrors[norm] ?: knownErrors.entries.firstOrNull { norm.startsWith(it.key) }?.value
        return if (text != null) "$text ($code)" else "Virheilmoitus: $token"
    }

    /** "TU.06.30" → "tiistai klo 06.30", "MO-FR.16.30" → "ma–pe klo 16.30". */
    fun describeTime(value: String): String {
        val parts = value.split('.', ':')
        if (parts.size < 3) return value
        val day = Commands.Day.fromCode(parts[0])?.label?.lowercase() ?: parts[0]
        val hh = parts[1].padStart(2, '0')
        val mm = parts[2].padStart(2, '0')
        return "$day klo $hh.$mm"
    }

    private fun indent(context: Context) = if (context == Context.TIMER) "   " else ""

    private enum class Context { NONE, HEATER, TIMER, TEMP, DEVICE_TIME, MAIN_PHONE }
}
