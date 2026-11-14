package fi.heatcall.platform

import org.junit.Assert.assertEquals
import org.junit.Test

class VendorTest {

    @Test
    fun detectsMainBrands() {
        assertEquals(Vendor.SAMSUNG, Vendor.detect("samsung", "samsung"))
        assertEquals(Vendor.GOOGLE, Vendor.detect("Google", "google"))
        assertEquals(Vendor.OPPO, Vendor.detect("OPPO", "OPPO"))
        assertEquals(Vendor.OPPO, Vendor.detect("OnePlus", "OnePlus"))
        assertEquals(Vendor.OPPO, Vendor.detect("realme", "realme"))
        assertEquals(Vendor.XIAOMI, Vendor.detect("Xiaomi", "Redmi"))
        assertEquals(Vendor.XIAOMI, Vendor.detect("Xiaomi", "POCO"))
        assertEquals(Vendor.OTHER, Vendor.detect("motorola", "motorola"))
        assertEquals(Vendor.OTHER, Vendor.detect(null, null))
    }

    @Test
    fun everyVendorHasStepsAndAppInfoFallback() {
        Vendor.entries.forEach { v ->
            val g = DeviceSettings.backgroundGuide(v)
            assert(g.steps.isNotEmpty()) { "$v: ei vaiheita" }
            assert(g.actions.isNotEmpty()) { "$v: ei painikkeita" }
        }
    }
}
