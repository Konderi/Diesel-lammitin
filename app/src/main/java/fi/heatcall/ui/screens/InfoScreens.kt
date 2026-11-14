package fi.heatcall.ui.screens

import fi.heatcall.i18n.tr
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
import fi.heatcall.ui.components.LanguagePicker
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
    SubScreen(tr("Ajankohtaista", "Latest"), onBack) {
        if (log.isEmpty()) {
            Section { BodyText(tr("Ei vielä viestejä. Lähetetyt komennot ja lämmittimen vastaukset näkyvät tässä.", "No messages yet. Sent commands and heater replies are shown here.")) }
        }
        log.forEach { e ->
            Section {
                Text(
                    (if (e.outgoing) tr("→ Lähetetty", "→ Sent") else tr("← Vastaus", "← Reply")) + " · ${e.deviceName} · ${fmt.format(Date(e.timestamp))}",
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
            SecondaryButton(tr("Tyhjennä historia", "Clear history"), onClick = onClear, modifier = Modifier.fillMaxWidth())
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
    var showBackground by remember { mutableStateOf(false) }
    if (showBackground) BackgroundGuideDialog(onDismiss = { showBackground = false })
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var accessGranted by remember { mutableStateOf(MarkReadListener.isAccessGranted(context)) }

    // Päivitä käyttöoikeuden tila, kun käyttäjä palaa järjestelmän asetuksista.
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            accessGranted = MarkReadListener.isAccessGranted(context)
        }
    }

    SubScreen(tr("Info ja asetukset", "Info and settings"), onBack) {
        Section(tr("Kieli", "Language")) { LanguagePicker() }

        Section(tr("Käyttöoikeudet", "Permissions")) {
            PermissionChecklist(
                permissions,
                onRequest = onRequestPermissions,
                onHelp = onShowPermissionHelp,
                onBackground = { showBackground = true },
            )
        }

        Section(tr("Laitteet", "Devices")) {
            devices.forEach { d ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(d.displayName, color = Color.White, fontWeight = FontWeight.Bold)
                        BodyText("${d.phone} · ${d.type.label}")
                    }
                    SecondaryButton(tr("Muokkaa", "Edit"), onClick = { onEditDevice(d) })
                }
            }
            BodyText(tr("Puhelinnumeroa ja PIN-koodia voi muuttaa kohdasta Muokkaa.", "The phone number and PIN can be changed with Edit."))
        }

        Section(tr("Vastausviestit", "Reply messages")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Merkitse vastaukset luetuiksi", "Mark replies as read"), color = Color.White)
                    BodyText(tr("Lämmittimen vastaus näytetään HeatCallissa ja merkitään luetuksi viestisovelluksessa.", "The heater's reply is shown in HeatCall and marked as read in the messaging app."))
                }
                Switch(checked = markRepliesRead, onCheckedChange = onMarkReadChanged)
            }
            Spacer(Modifier.height(8.dp))
            val smsApp = remember { defaultSmsAppName(context) }
            BodyText(tr("Oletusviestisovellus: $smsApp", "Default messaging app: $smsApp"))
            BodyText(
                if (accessGranted) tr("Ilmoitusten käyttöoikeus: annettu ✓", "Notification access: granted ✓") else tr("Ilmoitusten käyttöoikeus: puuttuu", "Notification access: missing"),
                color = if (accessGranted) Color(0xFFA5D6A7) else Color(0xFFFFCC80),
            )
            if (!accessGranted) {
                Spacer(Modifier.height(6.dp))
                BodyText(
                    tr("Luetuksi merkitseminen tarvitsee ilmoitusten käyttöoikeuden. Jos Android ilmoittaa ", "Marking as read needs notification access. If Android reports ") +
                        tr("\"rajoitettu asetus\", avaa Sovellustiedot → ⋮ → Salli rajoitetut asetukset.", "\"restricted setting\", open App info → ⋮ → Allow restricted settings."),
                )
                Spacer(Modifier.height(6.dp))
                PrimaryButton(tr("Anna käyttöoikeus", "Grant access"), onClick = { openListenerSettings(context) })
            }
        }

        devices.forEach { d ->
            Section(tr("Laitetoiminnot – ${d.displayName}", "Device functions – ${d.displayName}")) {
                DeviceFunctionButton(tr("Aseta laitteen kello puhelimen ajasta", "Set device clock from phone time")) {
                    onSend(d, DeviceClock.setFromPhone(d.pin), tr("Kellon asetus", "Clock setting"))
                }
                DeviceFunctionButton(tr("Kysy laitteen kello", "Query device clock")) {
                    onSend(d, Commands.deviceTimeQuery(d.pin), tr("Kellon kysely", "Clock query"))
                }
                DeviceFunctionButton(tr("Valtuuta tämä puhelin", "Authorise this phone")) {
                    onSend(d, Commands.mainPhoneSetMine(d.pin), tr("Numeron valtuutus", "Number authorisation"))
                }
                DeviceFunctionButton(tr("Kysy valtuutettu numero", "Query authorised number")) {
                    onSend(d, Commands.mainPhoneQuery(d.pin), tr("Valtuutettu numero", "Authorised number"))
                }
                BodyText(tr("Laitteen kello pitää asettaa uudelleen jokaisen virtakatkon jälkeen ja kesä-/talviajan vaihtuessa.", "The device clock must be set again after every power interruption and when summer/winter time changes."))
            }
        }

        Section(tr("Vianmääritys: luetuksi merkintä", "Troubleshooting: mark as read")) {
            var diag by remember { mutableStateOf(MarkReadDiagnostics.read(context)) }
            LaunchedEffect(lifecycle) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { diag = MarkReadDiagnostics.read(context) }
            }
            BodyText(
                if (MarkReadListener.isConnected()) tr("Kuuntelija: yhdistetty ✓", "Listener: connected ✓") else tr("Kuuntelija: ei yhdistetty", "Listener: not connected"),
                color = if (MarkReadListener.isConnected()) Color(0xFFA5D6A7) else Color(0xFFFFCC80),
            )
            Spacer(Modifier.height(6.dp))
            BodyText(
                tr("Jos vastauksia ei merkitä luetuiksi, lähetä tilakysely ja katso alta, mitä tapahtui. ", "If replies are not marked as read, send a status query and see below what happened. ") +
                    tr("Lokiin tallennetaan vain lähettäjä ja ilmoituksen toimintojen nimet, ei viestien sisältöä.", "Only the sender and the names of notification actions are logged, not message contents."),
            )
            Spacer(Modifier.height(8.dp))
            if (diag.isEmpty()) {
                BodyText(tr("Ei tapahtumia.", "No events."), color = Color(0x99FFFFFF))
            } else {
                diag.take(15).forEach {
                    Text(it, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 14.sp,
                        modifier = Modifier.padding(vertical = 2.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(tr("Päivitä", "Refresh"), onClick = { diag = MarkReadDiagnostics.read(context) })
                SecondaryButton(tr("Tyhjennä", "Clear"), onClick = {
                    MarkReadDiagnostics.clear(context)
                    diag = emptyList()
                })
                SecondaryButton(tr("Jaa", "Share"), onClick = {
                    val send = android.content.Intent(android.content.Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(android.content.Intent.EXTRA_TEXT, tr("HeatCall-vianmääritys\n\n", "HeatCall troubleshooting\n\n") + diag.joinToString("\n"))
                    context.startActivity(android.content.Intent.createChooser(send, tr("Jaa vianmääritysloki", "Share troubleshooting log")))
                })
            }
        }

        Section(tr("Kotinäytön widget", "Home screen widget")) {
            BodyText(tr("Widget näyttää auton lämmittimen tilan kotinäytöllä, ja tilan voi tarkistaa avaamatta sovellusta.", "The widget shows the heater status on the home screen, and you can check the status without opening the app."))
            Spacer(Modifier.height(8.dp))
            PrimaryButton(tr("Lisää widget kotinäytölle", "Add widget to home screen"), onClick = {
                if (!StatusWidget.requestPin(context)) {
                    android.widget.Toast.makeText(
                        context,
                        tr("Lisää widget pitkällä painalluksella kotinäytöltä: Widgetit → HeatCall", "Add the widget by long-pressing the home screen: Widgets → HeatCall"),
                        android.widget.Toast.LENGTH_LONG,
                    ).show()
                }
            })
        }

        Section(tr("Tietoja", "About")) {
            val version = remember {
                runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
            }
            BodyText(tr("HeatCall $version – Eberspächer EasyStart Call -yksikön tekstiviestiohjaus omaan käyttöön.", "HeatCall $version – text message control for the Eberspächer EasyStart Call unit."))
            BodyText(tr("Jokainen komento on tekstiviesti, josta liittymäsi voi veloittaa.", "Every command is a text message that your operator may charge for."))
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DeviceFunctionButton(text: String, onClick: () -> Unit) {
    SecondaryButton(text, onClick = onClick, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp))
}

private fun defaultSmsAppName(context: Context): String {
    val pkg = Telephony.Sms.getDefaultSmsPackage(context) ?: return tr("ei tiedossa", "unknown")
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
    SubScreen(tr("Ohjeet", "Help"), onBack) {
        Section(tr("Käyttö", "Usage")) {
            BodyText(
                tr("1. Valitse auto koskettamalla sen korttia (jos laitteita on useampi).\n", "1. Select a car by tapping its card (if you have several).\n") +
                    tr("2. Pyöritä Minuutit-rullaa: käyntiaika 10–120 min, ∞ = kestokäyttö (vain ilmalämmitin) ", "2. Turn the Minutes wheel: run time 10–120 min, ∞ = continuous (air heater only) ") +
                    tr("tai Pois = sammutus.\n", "or Off = switch off.\n") +
                    tr("3. Valitse Tila: Lämmitys tai Tuuletus (tuuletusta eivät tue kaikki lämmittimet).\n", "3. Choose Mode: Heating or Ventilation (not all heaters support ventilation).\n") +
                    tr("4. Paina KÄYNNISTÄ tai SAMMUTA. Komento lähtee tekstiviestinä taustalla.\n", "4. Press START or STOP. The command is sent as a text message in the background.\n") +
                    tr("Tarkista tila -painike kortissa (ja kotinäytön widgetissä) kysyy lämmittimen tilan.\n", "The Check status button on the card (and on the home screen widget) asks the heater for its status.\n") +
                    tr("5. Lämmittimen vastaus avautuu omaan ikkunaansa, ja viesti merkitään luetuksi viestisovelluksessa.", "5. The heater's reply opens in its own window, and the message is marked as read in the messaging app."),
            )
        }
        Section(tr("Rataskuvakkeen valikko", "Gear menu")) {
            BodyText(
                tr("Kysy tila – lämmitin vastaa aina, vaikka kuittausilmoitus olisi pois päältä.\n", "Check status – the heater always replies, even if confirmation messages are off.\n") +
                    tr("Sisälämpötila – vaatii lämpötila-anturin.\n", "Interior temperature – requires a temperature sensor.\n") +
                    tr("Ajastimet – kolme viikkoajastinta (lähtöaika).\n", "Timers – three weekly timers (departure time).\n") +
                    tr("Laitteen kello – aseta puhelimen ajasta kesä-/talviajan vaihtuessa ja virtakatkon jälkeen.\n", "Device clock – set from phone time when summer/winter time changes and after a power interruption.\n") +
                    tr("Muokkaa laitetta – numero, PIN, tyyppi ja kuva.", "Edit device – number, PIN, type and photo."),
            )
        }
        Section(tr("Kuittausilmoitus", "Confirmation messages")) {
            BodyText(
                tr("Jos lämmitin ei vastaa käynnistys- ja sammutuskomentoihin, sen kuittausilmoitus on pois päältä ", "If the heater does not reply to start and stop commands, its confirmation messages are switched off ") +
                    tr("(tehdasasetus). Asennusliike voi kytkeä sen päälle. Tilakysely vastaa aina.", "(factory default). Your installer can switch them on. A status query always gets a reply."),
            )
        }
        Section(tr("Virheilmoitukset", "Error messages")) {
            listOf(
                "ErD" to tr("Yhteys lämmittimeen katkesi tai lämmitintä ei tunnistettu", "Connection to the heater lost or heater not detected"),
                "ErH" to tr("Lämmittimessä on häiriö tai se ei tue toimintoa", "Heater fault or function not supported"),
                "ErC" to tr("Lämpötila-anturi puuttuu tai laitteen kelloa ei ole asetettu", "Temperature sensor missing or device clock not set"),
                "ErR" to tr("Ajastinta ei voi käyttää, koska laitteen kelloa ei ole asetettu", "The timer cannot be used because the device clock is not set"),
                "ErL" to tr("Johdotusvika", "Wiring fault"),
            ).forEach { (code, text) ->
                Row(Modifier.padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(code, color = Color(0xFFFFCC80), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    BodyText(text)
                }
            }
        }
        Section(tr("Tekstiviestikomennot", "Text message commands")) {
            BodyText(tr("HeatCall lähettää nämä puolestasi. Ne toimivat myös käsin lähetettyinä:", "HeatCall sends these for you. They also work when sent manually:"))
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
            BodyText(tr("Päivät: MO TU WE TH FR SA SU tai alueet MO-FR, SA-SU, MO-SU.", "Days: MO TU WE TH FR SA SU or ranges MO-FR, SA-SU, MO-SU."))
        }
        Spacer(Modifier.height(24.dp))
    }
}
