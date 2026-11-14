package fi.heatcall.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import fi.heatcall.i18n.tr
import fi.heatcall.platform.DeviceSettings
import fi.heatcall.platform.Vendor
import fi.heatcall.ui.components.BodyText
import fi.heatcall.ui.components.CarbonBackground
import fi.heatcall.ui.components.PrimaryButton
import fi.heatcall.ui.components.SecondaryButton
import fi.heatcall.ui.components.glassPanel

/**
 * Taustatoiminnan opastus puhelimen valmistajan mukaan (Samsung, Pixel, OPPO/OnePlus/realme,
 * Xiaomi). Jos tunnistus osuu väärin (esim. muunneltu järjestelmä), merkin voi vaihtaa itse.
 */
@Composable
fun BackgroundGuideDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val detected = remember { Vendor.current }
    var vendor by remember { mutableStateOf(detected) }
    val guide = DeviceSettings.backgroundGuide(vendor)
    val status = rememberPermissionStatus()

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
                Text(tr("Taustatoiminta", "Background activity"), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                BodyText(
                    tr(
                        "Jotta lämmittimen vastaus tulee perille, merkitään luetuksi ja päivittää widgetin myös silloin, kun HeatCall on kiinni, puhelin ei saa rajoittaa sen taustatoimintaa.",
                        "So that the heater's reply arrives, is marked as read and updates the widget even when HeatCall is closed, the phone must not restrict its background activity.",
                    ),
                )
                Spacer(Modifier.height(10.dp))
                Text(tr("Tunnistettu puhelin", "Detected phone"), color = Color(0xFFB8C4D3), fontSize = 12.sp)
                Text(DeviceSettings.deviceDescription, color = Color.White, fontSize = 15.sp)
                BodyText(
                    if (status.battery) {
                        tr("Akun optimointi: pois päältä ✓", "Battery optimisation: off ✓")
                    } else {
                        tr("Akun optimointi: päällä", "Battery optimisation: on")
                    },
                    color = if (status.battery) Color(0xFFA5D6A7) else Color(0xFFFFCC80),
                )

                Spacer(Modifier.height(10.dp))
                Text(tr("Ohje merkille", "Instructions for"), color = Color(0xFFB8C4D3), fontSize = 12.sp)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(Vendor.SAMSUNG, Vendor.GOOGLE, Vendor.OPPO, Vendor.XIAOMI, Vendor.OTHER).forEach { v ->
                        FilterChip(
                            selected = vendor == v,
                            onClick = { vendor = v },
                            label = { Text(if (v == Vendor.OTHER) tr("Muu", "Other") else v.displayName.substringBefore(" /")) },
                            colors = FilterChipDefaults.filterChipColors(labelColor = Color.White),
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                guide.steps.forEachIndexed { i, step -> GuideStep(i + 1, step) }

                if (vendor == Vendor.OPPO || vendor == Vendor.XIAOMI) {
                    Spacer(Modifier.height(6.dp))
                    BodyText(
                        tr(
                            "Automaattisen käynnistyksen tilaa ei voi tarkistaa sovelluksesta, joten varmista se itse.",
                            "The auto launch setting cannot be checked from the app, so please verify it yourself.",
                        ),
                        color = Color(0xFFB8C4D3),
                    )
                }

                Spacer(Modifier.height(14.dp))
                guide.actions.forEachIndexed { i, action ->
                    val onClick = {
                        val exact = DeviceSettings.open(context, action.target)
                        if (!exact) {
                            Toast.makeText(
                                context,
                                tr(
                                    "Valmistajan näkymää ei löytynyt tästä mallista – avattiin sovelluksen tiedot.",
                                    "The manufacturer's screen was not found on this model – opened app info instead.",
                                ),
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    }
                    if (i == 0) {
                        PrimaryButton(action.label, onClick = onClick, modifier = Modifier.fillMaxWidth())
                    } else {
                        SecondaryButton(action.label, onClick = onClick, modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(6.dp))
                }
                SecondaryButton(tr("Sulje", "Close"), onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun GuideStep(n: Int, text: String) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
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
