package fi.heatcall.ui.screens

import fi.heatcall.i18n.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import fi.heatcall.data.Device
import fi.heatcall.sms.Commands
import fi.heatcall.sms.DeviceClock
import fi.heatcall.ui.components.BodyText
import fi.heatcall.ui.components.CarbonBackground
import fi.heatcall.ui.components.PrimaryButton
import fi.heatcall.ui.components.SecondaryButton
import fi.heatcall.ui.components.glassPanel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date
import java.util.Locale

/**
 * Laitteen kello: aseta puhelimen ajasta (sisältää kesä-/talviajan), kysy laitteen
 * kello tai aseta käsin. EasyStart Call ei vaihda kesä- ja talviaikaa itse.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockDialog(
    device: Device,
    onSetFromPhone: () -> Unit,
    onQuery: () -> Unit,
    onSetManual: (Commands.Day, Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val fi = remember { Locale.forLanguageTag("fi-FI") }
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5_000)
            now = ZonedDateTime.now()
        }
    }
    var manual by remember { mutableStateOf(false) }
    var day by remember { mutableStateOf(DeviceClock.dayOf(now.dayOfWeek)) }
    var hour by remember { mutableStateOf(now.hour) }
    var minute by remember { mutableStateOf(now.minute) }
    var dayMenu by remember { mutableStateOf(false) }
    var timeDialog by remember { mutableStateOf(false) }

    val zone = ZoneId.systemDefault()
    val dst = if (zone.rules.isDaylightSavings(now.toInstant())) tr("kesäaika", "summer time") else tr("talviaika", "winter time")
    val today = DeviceClock.dayOf(now.dayOfWeek)
    val phoneTime = tr("${today.inSentence} klo %02d.%02d", "${today.label} %02d:%02d").format(now.hour, now.minute)

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
                Text(tr("Laitteen kello", "Device clock"), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(device.displayName, color = Color(0xCCFFFFFF), fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Text(tr("Puhelimen aika: $phoneTime ($dst)", "Phone time: $phoneTime ($dst)"), color = Color.White, fontSize = 15.sp)
                BodyText(
                    if (device.clockSetAt > 0) {
                        tr("Asetettu HeatCallista: ", "Set from HeatCall: ") + SimpleDateFormat("d.M.yyyy HH.mm", fi).format(Date(device.clockSetAt))
                    } else {
                        tr("Kelloa ei ole asetettu HeatCallista.", "The clock has not been set from HeatCall.")
                    },
                    color = Color(0xFFB8C4D3),
                )
                if (DeviceClock.needsDstReminder(device.clockSetAt)) {
                    Spacer(Modifier.height(6.dp))
                    BodyText(
                        if (DeviceClock.lastTransitionWasToWinter() == true) {
                            tr("Talviaika alkoi äskettäin. Lämmittimen kello on todennäköisesti tunnin edellä.", "Winter time began recently. The heater clock is probably one hour ahead.")
                        } else {
                            tr("Kesäaika alkoi äskettäin. Lämmittimen kello on todennäköisesti tunnin jäljessä.", "Summer time began recently. The heater clock is probably one hour behind.")
                        },
                        color = Color(0xFFFFCC80),
                    )
                }
                Spacer(Modifier.height(10.dp))
                BodyText(
                    tr("EasyStart Call ei vaihda kesä- ja talviaikaa itse. Aseta kello aikasiirtymän ja jokaisen ", "EasyStart Call does not switch between summer and winter time by itself. Set the clock after each time change and every ") +
                        tr("virtakatkon jälkeen, muuten ajastimet käynnistävät lämmittimen väärään aikaan.", "power interruption, otherwise the timers will start the heater at the wrong time."),
                )
                Spacer(Modifier.height(14.dp))
                PrimaryButton(
                    tr("Aseta puhelimen ajasta", "Set from phone time"),
                    onClick = {
                        onSetFromPhone()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                SecondaryButton(
                    tr("Kysy laitteen kello", "Query device clock"),
                    onClick = {
                        onQuery()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                if (!manual) {
                    SecondaryButton(tr("Aseta käsin…", "Set manually…"), onClick = { manual = true }, modifier = Modifier.fillMaxWidth())
                } else {
                    Spacer(Modifier.height(4.dp))
                    Text(tr("Aseta käsin", "Set manually"), color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box {
                            SecondaryButton(day.label, onClick = { dayMenu = true })
                            DropdownMenu(expanded = dayMenu, onDismissRequest = { dayMenu = false }) {
                                Commands.Day.entries.take(7).forEach { d ->
                                    DropdownMenuItem(text = { Text(d.label) }, onClick = {
                                        day = d
                                        dayMenu = false
                                    })
                                }
                            }
                        }
                        SecondaryButton(tr("klo %02d.%02d", "%02d:%02d").format(hour, minute), onClick = { timeDialog = true })
                    }
                    Spacer(Modifier.height(6.dp))
                    PrimaryButton(
                        tr("Lähetä kellonaika", "Send time"),
                        onClick = {
                            onSetManual(day, hour, minute)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(6.dp))
                SecondaryButton(tr("Sulje", "Close"), onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (timeDialog) {
        val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { timeDialog = false },
            title = { Text(tr("Kellonaika", "Time")) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    hour = state.hour
                    minute = state.minute
                    timeDialog = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { timeDialog = false }) { Text(tr("Peruuta", "Cancel")) } },
        )
    }
}
