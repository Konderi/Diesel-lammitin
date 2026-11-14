package fi.heatcall.platform

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import fi.heatcall.i18n.tr

/**
 * Puhelimen valmistajan tunnistus ja valmistajakohtaiset asetuspolut.
 *
 * Android-valmistajat rajoittavat taustalla toimivia sovelluksia eri tavoin, ja asetukset ovat eri
 * paikoissa. HeatCall tarvitsee taustatoimintaa, jotta lämmittimen vastaus tulee perille, merkitään
 * luetuksi ja päivittää widgetin, vaikka sovellus olisi kiinni.
 *
 * Syvälinkit ovat valmistajien sisäisiä näkymiä, jotka voivat puuttua tietyistä malleista tai
 * versioista. Siksi jokaiselle kohteelle on lista vaihtoehtoja, jotka kokeillaan järjestyksessä,
 * ja viimeisenä varapolkuna on aina sovelluksen tietosivu.
 */
enum class Vendor(val displayName: String) {
    SAMSUNG("Samsung"),
    GOOGLE("Google Pixel"),
    OPPO("OPPO / OnePlus / realme"),
    XIAOMI("Xiaomi / Redmi / POCO"),
    OTHER("Android"),
    ;

    companion object {
        /** Puhdas funktio, jotta tunnistus voidaan yksikkötestata. */
        fun detect(manufacturer: String?, brand: String?): Vendor {
            val m = "${manufacturer.orEmpty()} ${brand.orEmpty()}".lowercase()
            return when {
                "samsung" in m -> SAMSUNG
                "google" in m -> GOOGLE
                listOf("oppo", "oneplus", "realme", "oplus").any { it in m } -> OPPO
                listOf("xiaomi", "redmi", "poco").any { it in m } -> XIAOMI
                else -> OTHER
            }
        }

        val current: Vendor get() = detect(Build.MANUFACTURER, Build.BRAND)
    }
}

/** Valmistajakohtainen ohje: vaiheet ja painikkeet. */
data class VendorGuide(
    val vendor: Vendor,
    val steps: List<String>,
    val actions: List<GuideAction>,
)

data class GuideAction(val label: String, val target: SettingsTarget)

enum class SettingsTarget { APP_DETAILS, BATTERY, AUTOSTART, BATTERY_OPTIMIZATION_LIST }

object DeviceSettings {

    val deviceDescription: String
        get() = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}"

    /** Taustatoiminnan ohje juuri tämän puhelimen valmistajalle. */
    fun backgroundGuide(vendor: Vendor = Vendor.current): VendorGuide = when (vendor) {
        Vendor.SAMSUNG -> VendorGuide(
            vendor,
            listOf(
                tr("Avaa Sovelluksen tiedot alla olevasta painikkeesta.", "Open App info with the button below."),
                tr("Valitse Akku → Rajoittamaton.", "Choose Battery → Unrestricted."),
                tr(
                    "Lisäksi: Laitteen ylläpito → Akku → Taustakäytön rajoitukset → Ei koskaan nukkuvat sovellukset → lisää HeatCall.",
                    "Also: Device care → Battery → Background usage limits → Never sleeping apps → add HeatCall.",
                ),
            ),
            listOf(
                GuideAction(tr("Avaa sovelluksen tiedot", "Open app info"), SettingsTarget.APP_DETAILS),
                GuideAction(tr("Avaa akun asetukset", "Open battery settings"), SettingsTarget.BATTERY),
            ),
        )

        Vendor.GOOGLE -> VendorGuide(
            vendor,
            listOf(
                tr("Avaa Sovelluksen tiedot alla olevasta painikkeesta.", "Open App info with the button below."),
                tr("Valitse Sovelluksen akunkäyttö → Rajoittamaton.", "Choose App battery usage → Unrestricted."),
            ),
            listOf(
                GuideAction(tr("Avaa sovelluksen tiedot", "Open app info"), SettingsTarget.APP_DETAILS),
            ),
        )

        Vendor.OPPO -> VendorGuide(
            vendor,
            listOf(
                tr("Avaa Sovelluksen tiedot alla olevasta painikkeesta.", "Open App info with the button below."),
                tr(
                    "Valitse Akun käyttö ja kytke päälle Salli taustatoiminta, Salli automaattinen käynnistys ja tarvittaessa Salli käynnistys muista sovelluksista.",
                    "Choose Battery usage and turn on Allow background activity, Allow auto launch and, if shown, Allow launch by other apps.",
                ),
                tr(
                    "Jos automaattisen käynnistyksen kohtaa ei ole, avaa Automaattinen käynnistys -lista alta ja salli HeatCall.",
                    "If there is no auto launch option, open the Auto launch list below and allow HeatCall.",
                ),
            ),
            listOf(
                GuideAction(tr("Avaa sovelluksen tiedot", "Open app info"), SettingsTarget.APP_DETAILS),
                GuideAction(tr("Avaa automaattinen käynnistys", "Open auto launch"), SettingsTarget.AUTOSTART),
            ),
        )

        Vendor.XIAOMI -> VendorGuide(
            vendor,
            listOf(
                tr("Avaa Automaattinen käynnistys ja salli HeatCall.", "Open Autostart and allow HeatCall."),
                tr("Avaa akun asetukset ja valitse Ei rajoituksia.", "Open battery settings and choose No restrictions."),
            ),
            listOf(
                GuideAction(tr("Avaa automaattinen käynnistys", "Open autostart"), SettingsTarget.AUTOSTART),
                GuideAction(tr("Avaa akun asetukset", "Open battery settings"), SettingsTarget.BATTERY),
                GuideAction(tr("Avaa sovelluksen tiedot", "Open app info"), SettingsTarget.APP_DETAILS),
            ),
        )

        Vendor.OTHER -> VendorGuide(
            vendor,
            listOf(
                tr("Avaa Sovelluksen tiedot alla olevasta painikkeesta.", "Open App info with the button below."),
                tr(
                    "Etsi akkua koskeva kohta (esim. Akku tai Akunkäyttö) ja valitse Rajoittamaton tai salli taustatoiminta.",
                    "Find the battery section (e.g. Battery or Battery usage) and choose Unrestricted or allow background activity.",
                ),
            ),
            listOf(
                GuideAction(tr("Avaa sovelluksen tiedot", "Open app info"), SettingsTarget.APP_DETAILS),
                GuideAction(tr("Akun optimointilista", "Battery optimisation list"), SettingsTarget.BATTERY_OPTIMIZATION_LIST),
            ),
        )
    }

    /** Ehdokasnäkymät järjestyksessä; ensimmäinen toimiva avataan. */
    fun intentsFor(context: Context, target: SettingsTarget, vendor: Vendor = Vendor.current): List<Intent> {
        val pkg = context.packageName
        val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pkg, null))
        fun component(p: String, c: String) = Intent().setComponent(ComponentName(p, c))

        val candidates: List<Intent> = when (target) {
            SettingsTarget.APP_DETAILS -> emptyList()
            SettingsTarget.BATTERY_OPTIMIZATION_LIST -> listOf(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            SettingsTarget.BATTERY -> when (vendor) {
                Vendor.SAMSUNG -> listOf(
                    component("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"),
                    component("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"),
                    component("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity"),
                )
                Vendor.XIAOMI -> listOf(
                    component("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity")
                        .putExtra("package_name", pkg)
                        .putExtra("package_label", "HeatCall"),
                )
                Vendor.OPPO -> listOf(
                    component("com.oplus.battery", "com.oplus.powermanager.fuelgaue.PowerUsageModelActivity"),
                    component("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity"),
                )
                else -> emptyList()
            }
            SettingsTarget.AUTOSTART -> when (vendor) {
                Vendor.OPPO -> listOf(
                    component("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
                    component("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                    component("com.oplus.safecenter", "com.oplus.safecenter.startupapp.StartupAppListActivity"),
                    component("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
                )
                Vendor.XIAOMI -> listOf(
                    component("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
                )
                else -> emptyList()
            }
        }
        // Varapolku: sovelluksen tietosivu löytyy kaikista puhelimista.
        return candidates + appDetails
    }

    /**
     * Avaa ensimmäisen toimivan näkymän. Palauttaa false, jos jouduttiin käyttämään
     * varapolkua (sovelluksen tiedot), jotta käyttöliittymä voi kertoa siitä.
     */
    fun open(context: Context, target: SettingsTarget): Boolean {
        val intents = intentsFor(context, target)
        intents.forEachIndexed { i, intent ->
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return i < intents.lastIndex || target == SettingsTarget.APP_DETAILS
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }
        return false
    }
}
