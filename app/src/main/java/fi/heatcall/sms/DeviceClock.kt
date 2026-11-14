package fi.heatcall.sms

import fi.heatcall.i18n.tr
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs

/**
 * EasyStart Callin kello: laite tietää vain viikonpäivän ja kellonajan, eikä se
 * vaihda kesä- ja talviaikaa itse. Kello pitää asettaa uudelleen jokaisen
 * virtakatkon ja aikasiirtymän jälkeen. Muuten ajastimet laukeavat tunnin väärään aikaan.
 */
object DeviceClock {

    private const val WEEK_MIN = 7 * 24 * 60

    /** Kesä-/talviaikamuistutus näytetään näin monta päivää siirtymän jälkeen. */
    private const val DST_REMINDER_DAYS = 21L

    private val replyTime = Regex("(?i)devicetime:ok,time:([a-z]{2})[.:](\\d{1,2})[.:](\\d{1,2})")

    fun dayOf(dow: DayOfWeek): Commands.Day = when (dow) {
        DayOfWeek.MONDAY -> Commands.Day.MO
        DayOfWeek.TUESDAY -> Commands.Day.TU
        DayOfWeek.WEDNESDAY -> Commands.Day.WE
        DayOfWeek.THURSDAY -> Commands.Day.TH
        DayOfWeek.FRIDAY -> Commands.Day.FR
        DayOfWeek.SATURDAY -> Commands.Day.SA
        DayOfWeek.SUNDAY -> Commands.Day.SU
    }

    /**
     * Komento, joka asettaa laitteen kellon puhelimen ajasta (sisältää kesä-/talviajan).
     *
     * Käyttöohje s. 27: `pin:1234␣devicetime:set,time:FR.06.30`. Laite tuntee vain minuutit,
     * joten aika pyöristetään lähimpään minuuttiin (klo 16.09.50 → 16.10). Muuten laite
     * jäisi jopa minuutin jälkeen.
     */
    fun setFromPhone(pin: String, now: ZonedDateTime = ZonedDateTime.now()): String {
        val t = now.plusSeconds(30)
        return Commands.deviceTimeSet(pin, dayOf(t.dayOfWeek), t.hour, t.minute)
    }

    /**
     * Laitteen kellon poikkeama puhelimen ajasta minuutteina (+ = laite edellä).
     * Palauttaa null, jos vastaus ei sisällä kellonaikaa.
     */
    fun offsetMinutes(reply: String, now: ZonedDateTime = ZonedDateTime.now()): Int? {
        val m = replyTime.find(reply) ?: return null
        val day = Commands.Day.fromCode(m.groupValues[1]) ?: return null
        if (day.ordinal > Commands.Day.SU.ordinal) return null
        val h = m.groupValues[2].toInt()
        val min = m.groupValues[3].toInt()
        val device = day.ordinal * 24 * 60 + h * 60 + min
        val phone = (now.dayOfWeek.value - 1) * 24 * 60 + now.hour * 60 + now.minute
        var diff = (device - phone) % WEEK_MIN
        if (diff > WEEK_MIN / 2) diff -= WEEK_MIN
        if (diff < -WEEK_MIN / 2) diff += WEEK_MIN
        return diff
    }

    /** Pieni ero johtuu tekstiviestin viiveestä; 10 min tai enemmän on virhe. */
    fun isWrong(offsetMinutes: Int): Boolean = abs(offsetMinutes) >= 10

    /** "1 h edellä", "58 min jäljessä", "1 h 5 min edellä". */
    fun describeOffset(offsetMinutes: Int): String {
        val a = abs(offsetMinutes)
        val h = a / 60
        val m = a % 60
        val amount = when {
            h == 0 -> tr("$m min", "$m min")
            m == 0 -> "$h h"
            else -> tr("$h h $m min", "$h h $m min")
        }
        return amount + if (offsetMinutes > 0) tr(" edellä", " ahead") else tr(" jäljessä", " behind")
    }

    /** Viimeisin kesä-/talviaikasiirtymä puhelimen aikavyöhykkeellä. */
    fun lastDstTransition(zone: ZoneId = ZoneId.systemDefault(), now: Instant = Instant.now()): Instant? =
        zone.rules.previousTransition(now)?.instant

    /**
     * Näytetäänkö muistutus: aikasiirtymä on ollut viimeisen kolmen viikon aikana,
     * eikä laitteen kelloa ole asetettu HeatCallista sen jälkeen.
     */
    fun needsDstReminder(
        clockSetAt: Long,
        zone: ZoneId = ZoneId.systemDefault(),
        now: Instant = Instant.now(),
    ): Boolean {
        val t = lastDstTransition(zone, now) ?: return false
        val recent = Duration.between(t, now) < Duration.ofDays(DST_REMINDER_DAYS)
        return recent && clockSetAt < t.toEpochMilli()
    }

    /** Siirtyikö kello viimeksi talviaikaan (true) vai kesäaikaan (false). */
    fun lastTransitionWasToWinter(zone: ZoneId = ZoneId.systemDefault(), now: Instant = Instant.now()): Boolean? {
        val t = zone.rules.previousTransition(now) ?: return null
        return t.isOverlap
    }
}
