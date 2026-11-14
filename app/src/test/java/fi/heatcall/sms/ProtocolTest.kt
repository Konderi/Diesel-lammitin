package fi.heatcall.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolTest {

    @Test
    fun commandsMatchManual() {
        assertEquals("pin:1234 heater:on,run:30", Commands.heaterOn("1234", 30))
        assertEquals("pin:1234 heater:on,run:10", Commands.heaterOn("1234", 3))
        assertEquals("pin:1234 heater:on,mode:fan,run:45", Commands.heaterOn("1234", 45, fan = true))
        assertEquals("pin:1234 heater:on,run:perm", Commands.heaterOn("1234", 0, permanent = true))
        assertEquals("pin:1234 heater:off", Commands.heaterOff("1234"))
        assertEquals("pin:1234 heater?", Commands.heaterStatus("1234"))
        assertEquals(
            "pin:1234 t2:on,time:MO-FR.06.05,run:40",
            Commands.timerProgram("1234", 2, Commands.Day.MO_FR, 6, 5, 40),
        )
        assertEquals("pin:1234 tall?", Commands.allTimersStatus("1234"))
        assertEquals(
            "pin:1234 devicetime:set,time:FR.06.30",
            Commands.deviceTimeSet("1234", Commands.Day.FR, 6, 30),
        )
        assertEquals("pin:1234 mainphone:set,number:mine", Commands.mainPhoneSetMine("1234"))
    }

    @Test
    fun phoneHelpers() {
        assertTrue(Commands.samePhone("+358401234567", "0401234567"))
        assertTrue(Commands.samePhone("00358 40 123 4567", "+358401234567"))
        assertFalse(Commands.samePhone("+358401234567", "+358401234568"))
        assertTrue(Commands.isValidPhone("+358 40 123 4567"))
        assertFalse(Commands.isValidPhone("abc"))
        assertTrue(Commands.isValidPin("0007"))
        assertFalse(Commands.isValidPin("123"))
    }

    @Test
    fun parsesHeaterOn() {
        val r = ReplyParser.parse("Heater:on,mode:heater,run:45")
        assertEquals("Lämmitin päällä", r.title)
        assertEquals(true, r.heaterOn)
        assertTrue(r.lines.contains("Käyntiaika 45 min"))
    }

    @Test
    fun parsesError() {
        val r = ReplyParser.parse("ErD:HeaterCom:lost,Heater:off")
        assertTrue(r.isError)
        assertEquals(false, r.heaterOn)
        assertEquals("Yhteys lämmittimeen katkesi (ErD)", r.lines.first())
    }

    @Test
    fun parsesTimers() {
        val r = ReplyParser.parse("T1:on,time:TU.06.30,mode:heater,run:45,T2:off,T3:on,time:MO-FR.16.30,run:40")
        assertEquals("Ajastimet", r.title)
        assertTrue(r.lines.contains("Ajastin 2: pois"))
        assertTrue(r.lines.any { it.contains("ma–pe klo 16.30") })
    }

    @Test
    fun parsesTempAndClock() {
        assertEquals("Sisälämpötila -4 °C", ReplyParser.parse("Temp:ok,value:-4").lines.single())
        assertEquals("Laitteen kello: perjantai klo 06.30", ReplyParser.parse("DeviceTime:ok,time:FR.06.30").lines.single())
    }
}
