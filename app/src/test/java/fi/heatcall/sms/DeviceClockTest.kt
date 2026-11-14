package fi.heatcall.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class DeviceClockTest {

    private val helsinki = ZoneId.of("Europe/Helsinki")

    // Lauantai 14.11.2026 klo 15.20
    private val now = ZonedDateTime.of(2026, 11, 14, 15, 20, 0, 0, helsinki)

    @Test
    fun setFromPhoneUsesWeekdayAndLocalTime() {
        assertEquals("pin:1234 devicetime:set,time:SA.15.20", DeviceClock.setFromPhone("1234", now))
    }

    @Test
    fun detectsSummerTimeOffset() {
        // Laite jäi kesäaikaan: tunnin edellä.
        assertEquals(60, DeviceClock.offsetMinutes("DeviceTime:ok,time:SA.16.20", now))
        assertEquals("1 h edellä", DeviceClock.describeOffset(60))
        assertTrue(DeviceClock.isWrong(60))
    }

    @Test
    fun handlesWeekWrapAndSmallDelay() {
        val sundayNight = ZonedDateTime.of(2026, 11, 15, 23, 50, 0, 0, helsinki)
        assertEquals(30, DeviceClock.offsetMinutes("DeviceTime:ok,time:MO.00.20", sundayNight))
        assertEquals(-2, DeviceClock.offsetMinutes("DeviceTime:ok,time:SA.15.18", now))
        assertFalse(DeviceClock.isWrong(-2))
        assertNull(DeviceClock.offsetMinutes("Heater:on,run:30", now))
    }

    @Test
    fun dstReminderAfterOctoberTransition() {
        // Talviaika alkoi 25.10.2026. Kolme päivää myöhemmin muistutus näkyy, jos kelloa ei ole asetettu.
        val oct28 = ZonedDateTime.of(2026, 10, 28, 12, 0, 0, 0, helsinki).toInstant()
        assertTrue(DeviceClock.needsDstReminder(0L, helsinki, oct28))
        val setAfter = ZonedDateTime.of(2026, 10, 26, 8, 0, 0, 0, helsinki).toInstant().toEpochMilli()
        assertFalse(DeviceClock.needsDstReminder(setAfter, helsinki, oct28))
        // Kuukauden päästä muistutusta ei enää näytetä.
        val nov30 = ZonedDateTime.of(2026, 11, 30, 12, 0, 0, 0, helsinki).toInstant()
        assertFalse(DeviceClock.needsDstReminder(0L, helsinki, nov30))
        assertEquals(true, DeviceClock.lastTransitionWasToWinter(helsinki, oct28))
    }
}
