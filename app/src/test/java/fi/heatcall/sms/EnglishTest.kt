package fi.heatcall.sms

import fi.heatcall.i18n.AppLanguage
import fi.heatcall.i18n.I18n
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Englanninkieliset tekstit; komennot pysyvät samoina kielestä riippumatta. */
class EnglishTest {

    @Before fun english() { I18n.language = AppLanguage.EN }
    @After fun finnish() { I18n.language = AppLanguage.FI }

    @Test
    fun repliesInEnglish() {
        val r = ReplyParser.parse("Heater:on,mode:heater,run:45")
        assertEquals("Heater on", r.title)
        assertTrue(r.lines.contains("Run time 45 min"))
        assertEquals("Connection to the heater was lost (ErD)", ReplyParser.parse("ErD:HeaterCom:lost,Heater:off").lines.first())
        assertEquals("Device clock: Friday at 06:30", ReplyParser.parse("DeviceTime:ok,time:FR.06.30").lines.single())
        assertTrue(ReplyParser.parse("T2:on,time:MO-FR.16.30,run:40").lines.any { it.contains("Mon–Fri at 16:30") })
    }

    @Test
    fun commandsAreLanguageIndependent() {
        assertEquals("pin:1234 devicetime:set,time:FR.06.30", Commands.deviceTimeSet("1234", Commands.Day.FR, 6, 30))
        assertEquals("pin:1234 heater:on,run:30", Commands.heaterOn("1234", 30))
        assertEquals("1 h ahead", DeviceClock.describeOffset(60))
    }
}
