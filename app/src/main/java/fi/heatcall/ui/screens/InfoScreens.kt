package fi.heatcall.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import fi.heatcall.data.Device
import fi.heatcall.data.LogEntry
import fi.heatcall.sms.Commands
import fi.heatcall.sms.DeviceClock
import fi.heatcall.sms.MarkReadDiagnostics
import fi.heatcall.sms.MarkReadListener
import fi.heatcall.sms.ReplyParser
import fi.heatcall.ui.components.BodyText
import fi.heatcall.ui.components.ChromeColors
import fi.heatcall.ui.components.PrimaryButton
import fi.heatcall.ui.components.SecondaryButton
import fi.heatcall.ui.components.Section
import fi.heatcall.ui.components.SubScreen
import fi.heatcall.widget.StatusWidget
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val fi = Locale.forLanguageTag("fi-FI")

// --- Ajankohtaista: viestihistoria ----------------------------------------------

@Composable
fun NewsScreen(log: List<LogEntry>, onClear: () -> Unit, onBack: () -> Unit) {
    val fmt = remember { SimpleDateFormat("d.M.yyyy HH.mm", fi) }
    SubScreen("Ajankohtaista", onBack) {
        if (log.isEmpty()) {
            Section { BodyText("Ei vielä viestejä. Lähetetyt komennot ja lämmittimen vastaukset näkyvät tässä.") }
        }
        log.forEach { e ->
            Section {
                Text(
                    (if (e.outgoing) "→ Lähetetty" else "← Vastaus") + " · ${e.deviceName} · ${fmt.format(Date(e.timestamp))}",
                    color = if (e.outgoing) Color(0xFFB3E5FC) else Color(0xFFFFCC80),
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(4.dp))
                if (e.outgoing) {
                    Text(e.text, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                } else {
                    val r = ReplyParser.parse(e.text)
                    Text(r.title, color = if (r.isError) Color(0xFFFF8A80) else Color.White, fontWeight = FontWeight.Bold)
                    r.lines.forEach { BodyText(it) }
                    Text(e.text, color = Color(0x99FFFFFF), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        }
        if (log.isNotEmpty()) {
            SecondaryButton("Tyhjennä historia", onClick = onClear, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(24.dp))
    }
}

// --- Info ja asetukset ---------------------------------------------------------------

@Composable
fun SettingsScreen(
    devices: List<Device>,
    markRepliesRead: Boolean,
    onMarkReadChanged: (Boolean) -> Unit,
    onEditDevice: (Device) -> Unit,
    onSend: (Device, String, String) -> Unit,
    onBack: () -> Unit,
    onRequestPermissions: () -> Unit = {},
    onShowPermissionHelp: () -> Unit = {},
) {
    val context = LocalContext.current
    val permissions = rememberPermissionStatus()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var accessGranted by remember { mutableStateOf(MarkReadListener.isAccessGranted(context)) }

    // Päivitä käyttöoikeuden tila, kun käyttäjä palaa järjestelmän asetuksista.
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            accessGranted = MarkReadListener.isAccessGranted(context)
        }
    }

    SubScreen("Info ja asetukset", onBack) {
        Section("Käyttöoikeudet") {
            PermissionChecklist(permissions, onRequest = onRequestPermissions, onHelp = onShowPermissionHelp)
        }

        Section("Laitteet") {
            devices.forEach { d ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(d.displayName, color = Color.White, fontWeight = FontWeight.Bold)
                        BodyText("${d.phone} · ${d.type.label}")
                    }
                    SecondaryButton("Muokkaa", onClick = { onEditDevice(d) })
                }
            }
            BodyText("Puhelinnumeroa ja PIN-koodia voi muuttaa kohdasta Muokkaa.")
        }

        Section("Vastausviestit") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Merkitse vastaukset luetuiksi", color = Color.White)
                    BodyText("Lämmittimen vastaus näytetään HeatCallissa ja merkitään luetuksi viestisovelluksessa.")
                }
                Switch(checked = markRepliesRead, onCheckedChange = onMarkReadChanged)
            }
            Spacer(Modifier.height(8.dp))
            val smsApp = remember { defaultSmsAppName(context) }
            BodyText("Oletusviestisovellus: $smsApp")
            BodyText(
                if (accessGranted) "Ilmoitusten käyttöoikeus: annettu ✓" else "Ilmoitusten käyttöoikeus: puuttuu",
                color = if (accessGranted) Color(0xFFA5D6A7) else Color(0xFFFFCC80),
            )
            if (!accessGranted) {
                Spacer(Modifier.height(6.dp))
                BodyText(
                    "Luetuksi merkitseminen tarvitsee ilmoitusten käyttöoikeuden. Jos Android ilmoittaa " +
                        "\"rajoitettu asetus\", avaa Sovellustiedot → ⋮ → Salli rajoitetut asetukset.",
                )
                Spacer(Modifier.height(6.dp))
                PrimaryButton("Anna käyttöoikeus", onClick = { openListenerSettings(context) })
            }
        }

        devices.forEach { d ->
            Section("Laitetoiminnot – ${d.displayName}") {
                DeviceFunctionButton("Aseta laitteen kello puhelimen ajasta") {
                    onSend(d, DeviceClock.setFromPhone(d.pin), "Kellon asetus")
                }
                DeviceFunctionButton("Kysy laitteen kello") {
                    onSend(d, Commands.deviceTimeQuery(d.pin), "Kellon kysely")
                }
                DeviceFunctionButton("Valtuuta tämä puhelin") {
                    onSend(d, Commands.mainPhoneSetMine(d.pin), "Numeron valtuutus")
                }
                DeviceFunctionButton("Kysy valtuutettu numero") {
                    onSend(d, Commands.mainPhoneQuery(d.pin), "Valtuutettu numero")
                }
                BodyText("Laitteen kello pitää asettaa uudelleen jokaisen virtakatkon jälkeen ja kesä-/talviajan vaihtuessa.")
            }
        }

        Section("Vianmääritys: luetuksi merkintä") {
            var diag by remember { mutableStateOf(MarkReadDiagnostics.read(context)) }
            LaunchedEffect(lifecycle) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { diag = MarkReadDiagnostics.read(context) }
            }
            BodyText(
                if (MarkReadListener.isConnected()) "Kuuntelija: yhdistetty ✓" else "Kuuntelija: ei yhdistetty",
                color = if (MarkReadListener.isConnected()) Color(0xFFA5D6A7) else Color(0xFFFFCC80),
            )
            Spacer(Modifier.height(6.dp))
            BodyText(
                "Jos vastauksia ei merkitä luetuiksi, lähetä tilakysely ja katso alta, mitä tapahtui. " +
                    "Lokiin tallennetaan vain lähettäjä ja ilmoituksen toimintojen nimet, ei viestien sisältöä.",
            )
            Spacer(Modifier.height(8.dp))
            if (diag.isEmpty()) {
                BodyText("Ei tapahtumia.", color = Color(0x99FFFFFF))
            } else {
                diag.take(15).forEach {
                    Text(it, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 14.sp,
                        modifier = Modifier.padding(vertical = 2.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("Päivitä", onClick = { diag = MarkReadDiagnostics.read(context) })
                SecondaryButton("Tyhjennä", onClick = {
                    MarkReadDiagnostics.clear(context)
                    diag = emptyList()
                })
                SecondaryButton("Jaa", onClick = {
                    val send = android.content.Intent(android.content.Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(android.content.Intent.EXTRA_TEXT, "HeatCall-vianmääritys\n\n" + diag.joinToString("\n"))
                    context.startActivity(android.content.Intent.createChooser(send, "Jaa vianmääritysloki"))
                })
            }
        }

        Section("Kotinäytön widget") {
            BodyText("Widget näyttää auton lämmittimen tilan kotinäytöllä, ja tilan voi tarkistaa avaamatta sovellusta.")
            Spacer(Modifier.height(8.dp))
            PrimaryButton("Lisää widget kotinäytölle", onClick = {
                if (!StatusWidget.requestPin(context)) {
                    android.widget.Toast.makeText(
                        context,
                        "Lisää widget pitkällä painalluksella kotinäytöltä: Widgetit → HeatCall",
                        android.widget.Toast.LENGTH_LONG,
                    ).show()
                }
            })
        }

        Section("Tietoja") {
            val version = remember {
                runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
            }
            BodyText("HeatCall $version – Eberspächer EasyStart Call -yksikön tekstiviestiohjaus omaan käyttöön.")
            BodyText("Jokainen komento on tekstiviesti, josta liittymäsi voi veloittaa.")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DeviceFunctionButton(text: String, onClick: () -> Unit) {
    SecondaryButton(text, onClick = onClick, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp))
}

private fun defaultSmsAppName(context: Context): String {
    val pkg = Telephony.Sms.getDefaultSmsPackage(context) ?: return "ei tiedossa"
    return runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)
}

private fun openListenerSettings(context: Context) {
    try {
        context.startActivity(MarkReadListener.settingsIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        context.startActivity(MarkReadListener.fallbackSettingsIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

// --- Ohjeet ----------------------------------------------------------------------

@Composable
fun HelpScreen(onBack: () -> Unit) {
    SubScreen("Ohjeet", onBack) {
        Section("Käyttö") {
            BodyText(
                "1. Valitse auto koskettamalla sen korttia (jos laitteita on useampi).\n" +
                    "2. Pyöritä Minuutit-rullaa: käyntiaika 10–120 min, ∞ = kestokäyttö (vain ilmalämmitin) " +
                    "tai Pois = sammutus.\n" +
                    "3. Valitse Tila: Lämmitys tai Tuuletus (tuuletusta eivät tue kaikki lämmittimet).\n" +
                    "4. Paina KÄYNNISTÄ tai SAMMUTA. Komento lähtee tekstiviestinä taustalla.\n" +
                    "Tarkista tila -painike kortissa (ja kotinäytön widgetissä) kysyy lämmittimen tilan.\n" +
                    "5. Lämmittimen vastaus avautuu omaan ikkunaansa, ja viesti merkitään luetuksi viestisovelluksessa.",
            )
        }
        Section("Rataskuvakkeen valikko") {
            BodyText(
                "Kysy tila – lämmitin vastaa aina, vaikka kuittausilmoitus olisi pois päältä.\n" +
                    "Sisälämpötila – vaatii lämpötila-anturin.\n" +
                    "Ajastimet – kolme viikkoajastinta (lähtöaika).\n" +
                    "Muokkaa laitetta – numero, PIN, tyyppi ja kuva.",
            )
        }
        Section("Kuittausilmoitus") {
            BodyText(
                "Jos lämmitin ei vastaa käynnistys- ja sammutuskomentoihin, sen kuittausilmoitus on pois päältä " +
                    "(tehdasasetus). Asennusliike voi kytkeä sen päälle. Tilakysely vastaa aina.",
            )
        }
        Section("Virheilmoitukset") {
            listOf(
                "ErD" to "Yhteys lämmittimeen katkesi tai lämmitintä ei tunnistettu",
                "ErH" to "Lämmittimessä on häiriö tai se ei tue toimintoa",
                "ErC" to "Lämpötila-anturi puuttuu tai laitteen kelloa ei ole asetettu",
                "ErR" to "Ajastinta ei voi käyttää, koska laitteen kelloa ei ole asetettu",
                "ErL" to "Johdotusvika",
            ).forEach { (code, text) ->
                Row(Modifier.padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(code, color = Color(0xFFFFCC80), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    BodyText(text)
                }
            }
        }
        Section("Tekstiviestikomennot") {
            BodyText("HeatCall lähettää nämä puolestasi. Ne toimivat myös käsin lähetettyinä:")
            Spacer(Modifier.height(6.dp))
            listOf(
                "pin:1234 heater:on,run:30",
                "pin:1234 heater:off",
                "pin:1234 heater?",
                "pin:1234 temp?",
                "pin:1234 t1:on,time:MO-FR.06.30,run:30",
                "pin:1234 t1:off",
                "pin:1234 tall?",
                "pin:1234 devicetime:set,time:FR.06.30",
                "pin:1234 mainphone:set,number:mine",
            ).forEach {
                Text(it, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = ChromeColors.PanelBorder)
            BodyText("Päivät: MO TU WE TH FR SA SU tai alueet MO-FR, SA-SU, MO-SU.")
        }
        Spacer(Modifier.height(24.dp))
    }
}
