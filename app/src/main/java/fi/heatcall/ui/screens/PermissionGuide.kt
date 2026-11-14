package fi.heatcall.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import fi.heatcall.sms.MarkReadListener
import fi.heatcall.ui.components.BodyText
import fi.heatcall.ui.components.CarbonBackground
import fi.heatcall.ui.components.PrimaryButton
import fi.heatcall.ui.components.SecondaryButton
import fi.heatcall.ui.components.glassPanel

/**
 * Lupien tila ja opastus.
 *
 * Tiedostosta (selain, Drive, GitHub) asennettu sovellus ei Android 13+:ssa saa
 * tekstiviesti- eikä ilmoituskuuntelijalupaa ennen kuin käyttäjä sallii
 * "rajoitetut asetukset" sovelluksen tietosivulta. Järjestelmän lupa-ikkunassa
 * Salli on silloin harmaana. Tämä tiedosto kertoo käyttäjälle, miten se korjataan.
 */
data class PermissionStatus(
    val sms: Boolean,
    val notifications: Boolean,
    val markRead: Boolean,
    val battery: Boolean,
) {
    val smsMissing: Boolean get() = !sms
}

fun readPermissionStatus(context: Context): PermissionStatus {
    fun granted(p: String) = ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
    val pm = context.getSystemService(PowerManager::class.java)
    return PermissionStatus(
        sms = granted(Manifest.permission.SEND_SMS) && granted(Manifest.permission.RECEIVE_SMS),
        notifications = Build.VERSION.SDK_INT < 33 || granted(Manifest.permission.POST_NOTIFICATIONS),
        markRead = MarkReadListener.isAccessGranted(context),
        battery = pm.isIgnoringBatteryOptimizations(context.packageName),
    )
}

/** Lupien tila, joka päivittyy aina kun käyttäjä palaa järjestelmän asetuksista. */
@Composable
fun rememberPermissionStatus(): PermissionStatus {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var status by remember { mutableStateOf(readPermissionStatus(context)) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            status = readPermissionStatus(context)
        }
    }
    return status
}

/** Avaa HeatCallin sovellustiedot: sieltä löytyvät ⋮ → Salli rajoitetut asetukset, luvat ja akku. */
fun openAppDetails(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

fun openBatterySettings(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (_: ActivityNotFoundException) {
        openAppDetails(context)
    }
}

/** Päänäkymän varoituspalkki, kun tekstiviestilupa puuttuu. */
@Composable
fun PermissionBanner(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xCC5D2A12))
            .border(1.dp, Color(0xFFFF7043), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFFFB74D))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Tekstiviestilupa puuttuu", color = Color.White, fontWeight = FontWeight.Bold)
            Text("HeatCall ei voi ohjata lämmitintä. Napauta ohjeet.", color = Color(0xFFFFE0B2), fontSize = 13.sp)
        }
    }
}

/** Luettelo luvista Info ja asetukset -näkymään. */
@Composable
fun PermissionChecklist(status: PermissionStatus, onRequest: () -> Unit, onHelp: () -> Unit) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusRow("Tekstiviestit", "Komentojen lähetys ja vastausten vastaanotto", status.sms)
        StatusRow("Ilmoitukset", "Vastaus ilmoituksena, kun sovellus on taustalla", status.notifications)
        StatusRow("Ilmoitusten käyttöoikeus", "Vastausten merkitseminen luetuiksi", status.markRead)
        StatusRow("Ei akun optimointia", "Vastaukset tulevat perille myös sovelluksen ollessa kiinni", status.battery)
        Spacer(Modifier.height(4.dp))
        if (!status.sms || !status.notifications) {
            PrimaryButton("Pyydä luvat", onClick = onRequest, modifier = Modifier.fillMaxWidth())
        }
        if (!status.battery) {
            SecondaryButton("Akun asetukset", onClick = { openBatterySettings(context) }, modifier = Modifier.fillMaxWidth())
        }
        SecondaryButton("Luvat estetty? Näytä ohje", onClick = onHelp, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun StatusRow(title: String, desc: String, ok: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(if (ok) Color(0xFF66BB6A) else Color(0xFFFF7043)),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp)
            Text(desc, color = Color(0xFFB8C4D3), fontSize = 12.sp)
        }
        Text(if (ok) "OK" else "Puuttuu", color = if (ok) Color(0xFFA5D6A7) else Color(0xFFFFCC80), fontSize = 13.sp)
    }
}

/**
 * Vaiheittainen ohje rajoitettujen asetusten sallimiseen.
 * Näytetään automaattisesti, jos tekstiviestilupa evätään, ja Info ja asetukset -näkymästä.
 */
@Composable
fun PermissionHelpDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Box(Modifier.clip(RoundedCornerShape(14.dp))) {
            CarbonBackground(Modifier.matchParentSize()) {}
            Column(
                Modifier
                    .fillMaxWidth()
                    .glassPanel()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
            ) {
                Text("Lupien salliminen", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                BodyText(
                    "Kun HeatCall on asennettu tiedostosta (selain, Drive, GitHub), Android estää aluksi " +
                        "tekstiviestiluvan ja ilmoitusten käyttöoikeuden. Lupaikkunassa Salli näkyy silloin harmaana " +
                        "tai tulee ilmoitus \"Sovelluksen lupa kielletty\".",
                )
                Spacer(Modifier.height(12.dp))
                Step(1, "Paina alta Avaa sovelluksen tiedot.")
                Step(2, "Paina oikean yläkulman ⋮ (kolme pistettä).")
                Step(3, "Valitse Salli rajoitetut asetukset ja vahvista PIN-koodilla tai sormenjäljellä.")
                Step(4, "Avaa Käyttöoikeudet → Tekstiviestit → Salli.")
                Step(5, "Oppo/OnePlus/Xiaomi: Akun käyttö → salli taustatoiminta ja automaattinen käynnistys.")
                Step(6, "Palaa HeatCalliin ja anna ilmoitusten käyttöoikeus kohdasta Info ja asetukset.")
                Spacer(Modifier.height(8.dp))
                BodyText(
                    "Jos ⋮-valikossa ei ole valintaa, yritä ensin antaa tekstiviestilupa kerran (se estetään), " +
                        "ja avaa sovelluksen tiedot vasta sen jälkeen. Play Protectia ei tarvitse kytkeä pois.",
                    color = Color(0xFFB8C4D3),
                )
                Spacer(Modifier.height(16.dp))
                PrimaryButton(
                    "Avaa sovelluksen tiedot",
                    onClick = { openAppDetails(context) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                SecondaryButton("Sulje", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun Step(n: Int, text: String) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Color(0xFF2C77A8)),
            contentAlignment = Alignment.Center,
        ) {
            Text("$n", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Text(text, color = Color.White, fontSize = 14.sp, lineHeight = 19.sp, modifier = Modifier.weight(1f))
    }
}
