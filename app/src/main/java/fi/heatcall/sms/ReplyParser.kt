package fi.heatcall.sms

import fi.heatcall.i18n.tr
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

    /** Getter, jotta teksti vaihtuu kielen mukana. */
    private val knownErrors: Map<String, String>
        get() = mapOf(
            "erdheatercomlost" to tr("Yhteys lämmittimeen katkesi", "Connection to the heater was lost"),
            "erdheatercomno_heater_found" to tr(
                "Lämmitintä ei tunnistettu – kokeile sulakkeen irrotusta ja uutta käynnistystä",
                "Heater not detected – try removing the fuse and starting again",
            ),
            "erldatcomlost" to tr("Johdotusvika – ota yhteys huoltoon", "Wiring fault – contact a service partner"),
            "erhheatermode_not_supported" to tr("Lämmitin ei tue pyydettyä toimintoa", "The heater does not support this function"),
            "erhheatererror" to tr("Lämmittimessä on häiriö – yritä käynnistää uudelleen", "Heater fault – try starting again"),
            "ercheatererror" to tr("Lämmittimessä on häiriö", "Heater fault"),
            "erctempmissing" to tr("Lämpötila-anturia ei ole asennettu", "No temperature sensor installed"),
            "erctempshort_circuit" to tr("Lämpötila-anturi on viallinen", "Temperature sensor is faulty"),
            "ercdevicetimetime_not_set" to tr("Laitteen kelloa ei ole asetettu", "Device clock is not set"),
            "errdevicetimetime_not_set" to tr(
                "Laitteen kelloa ei ole asetettu – ajastin ei toimi",
                "Device clock is not set – the timer will not work",
            ),
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
                    lines += if (heaterOn) tr("Lämmitin PÄÄLLÄ", "Heater ON") else tr("Lämmitin POIS", "Heater OFF")
                }

                timerKey.matches(key) -> {
                    context = Context.TIMER
                    val n = timerKey.find(key)!!.groupValues[1]
                    lines += tr("Ajastin $n: ", "Timer $n: ") + if (value.equals("on", true)) tr("päällä", "on") else tr("pois", "off")
                }

                k == "mode" -> lines += indent(context) +
                    if (value.equals("fan", true)) tr("Tila: tuuletus", "Mode: ventilation") else tr("Tila: lämmitys", "Mode: heating")

                k == "run" -> lines += indent(context) +
                    if (value.equals("perm", true)) tr("Kestokäyttö", "Continuous operation") else tr("Käyntiaika $value min", "Run time $value min")

                k == "sp" -> lines += indent(context) + tr("Asetuslämpötila $value °C", "Set temperature $value °C")

                k == "time" -> lines += when (context) {
                    Context.TIMER -> indent(context) + tr("Lähtöaika: ", "Departure time: ") + describeTime(value)
                    else -> tr("Laitteen kello: ", "Device clock: ") + describeTime(value)
                }

                k == "temp" -> context = Context.TEMP
                k == "value" -> lines += tr("Sisälämpötila $value °C", "Interior temperature $value °C")

                k == "devicetime" -> context = Context.DEVICE_TIME
                k == "mainphone" -> context = Context.MAIN_PHONE
                k == "number" -> lines += tr("Valtuutettu numero: $value", "Authorised number: $value")

                // "ok" ja muut tunnistamattomat näytetään sellaisenaan, ettei tietoa katoa.
                else -> if (!value.equals("ok", true)) lines += token
            }
            if (firstContext == null && context != Context.NONE) firstContext = context
        }

        val title = when {
            errors.isNotEmpty() -> tr("Virhe", "Error")
            firstContext == Context.HEATER -> if (heaterOn == true) tr("Lämmitin päällä", "Heater on") else tr("Lämmitin pois", "Heater off")
            firstContext == Context.TIMER -> tr("Ajastimet", "Timers")
            firstContext == Context.TEMP -> tr("Sisälämpötila", "Interior temperature")
            firstContext == Context.DEVICE_TIME -> tr("Laitteen kello", "Device clock")
            firstContext == Context.MAIN_PHONE -> tr("Valtuutettu numero", "Authorised number")
            else -> tr("Vastaus lämmittimeltä", "Reply from the heater")
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
        return if (text != null) "$text ($code)" else tr("Virheilmoitus: $token", "Error message: $token")
    }

    /** "TU.06.30" → "tiistai klo 06.30", "MO-FR.16.30" → "ma–pe klo 16.30". */
    fun describeTime(value: String): String {
        val parts = value.split('.', ':')
        if (parts.size < 3) return value
        val day = Commands.Day.fromCode(parts[0])?.inSentence ?: parts[0]
        val hh = parts[1].padStart(2, '0')
        val mm = parts[2].padStart(2, '0')
        return tr("$day klo $hh.$mm", "$day at $hh:$mm")
    }

    private fun indent(context: Context) = if (context == Context.TIMER) "   " else ""

    private enum class Context { NONE, HEATER, TIMER, TEMP, DEVICE_TIME, MAIN_PHONE }
}
