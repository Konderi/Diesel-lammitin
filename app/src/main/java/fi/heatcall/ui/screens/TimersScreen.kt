package fi.heatcall.ui.screens

import fi.heatcall.i18n.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fi.heatcall.data.Device
import fi.heatcall.data.TimerDraft
import fi.heatcall.sms.Commands
import fi.heatcall.ui.components.BodyText
import fi.heatcall.ui.components.PrimaryButton
import fi.heatcall.ui.components.SecondaryButton
import fi.heatcall.ui.components.Section
import fi.heatcall.ui.components.SubScreen
import kotlin.math.roundToInt

/**
 * Ajastimet T1–T3. Laite kytkee oletuksena lämmityksen POIS asetettuna aikana
 * (lähtöaika), eli lämmitys alkaa käyntiajan verran ennen.
 */
@Composable
fun TimersScreen(
    device: Device,
    onSaveDraft: (Int, TimerDraft) -> Unit,
    onSend: (command: String, label: String) -> Unit,
    onBack: () -> Unit,
) {
    SubScreen(title = tr("Ajastimet – ${device.displayName}", "Timers – ${device.displayName}"), onBack = onBack) {
        Section {
            BodyText(
                tr("Aika on lähtöaika: lämmitin käynnistyy käyntiajan verran ennen ja sammuu asetettuna aikana. ", "The time is the departure time: the heater starts the run time earlier and switches off at the set time. ") +
                    tr("Laitteen kellon pitää olla oikeassa ajassa (Info ja asetukset → Aseta laitteen kello).", "The device clock must be correct (gear menu → Device clock)."),
            )
            Spacer(Modifier.height(10.dp))
            PrimaryButton(
                tr("Kysy kaikkien ajastinten tila", "Query all timers"),
                onClick = { onSend(Commands.allTimersStatus(device.pin), tr("Ajastinten tila", "Timer status")) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        device.timers.forEachIndexed { i, draft ->
            val n = i + 1
            TimerCard(
                n = n,
                draft = draft,
                onChange = { onSaveDraft(n, it) },
                onProgram = {
                    onSend(
                        Commands.timerProgram(device.pin, n, draft.day, draft.hour, draft.minute, draft.runMinutes),
                        tr("Ajastin $n ohjelmointi", "Timer $n programming"),
                    )
                },
                onOff = { onSend(Commands.timerOff(device.pin, n), tr("Ajastin $n pois", "Timer $n off")) },
                onQuery = { onSend(Commands.timerStatus(device.pin, n), tr("Ajastin $n tila", "Timer $n status")) },
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimerCard(
    n: Int,
    draft: TimerDraft,
    onChange: (TimerDraft) -> Unit,
    onProgram: () -> Unit,
    onOff: () -> Unit,
    onQuery: () -> Unit,
) {
    var dayMenu by remember { mutableStateOf(false) }
    var timeDialog by remember { mutableStateOf(false) }

    Section(tr("Ajastin $n", "Timer $n")) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box {
                SecondaryButton(draft.day.label, onClick = { dayMenu = true })
                DropdownMenu(expanded = dayMenu, onDismissRequest = { dayMenu = false }) {
                    Commands.Day.entries.forEach { d ->
                        DropdownMenuItem(
                            text = { Text(d.label) },
                            onClick = {
                                dayMenu = false
                                onChange(draft.copy(day = d))
                            },
                        )
                    }
                }
            }
            SecondaryButton(tr("klo %02d.%02d", "%02d:%02d").format(draft.hour, draft.minute), onClick = { timeDialog = true })
        }
        Spacer(Modifier.height(8.dp))
        Text(tr("Käyntiaika ${draft.runMinutes} min", "Run time ${draft.runMinutes} min"), color = Color.White, fontSize = 14.sp)
        Slider(
            value = draft.runMinutes.toFloat(),
            onValueChange = { v -> onChange(draft.copy(runMinutes = ((v / 5).roundToInt() * 5).coerceIn(10, 120))) },
            valueRange = Commands.RUNTIME_MIN.toFloat()..Commands.RUNTIME_MAX.toFloat(),
            steps = (Commands.RUNTIME_MAX - Commands.RUNTIME_MIN) / 5 - 1,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PrimaryButton(tr("Ohjelmoi ja päälle", "Program and enable"), onClick = onProgram, modifier = Modifier.weight(1.4f))
            SecondaryButton(tr("Pois", "Off"), onClick = onOff, modifier = Modifier.weight(0.8f))
            SecondaryButton(tr("Tila", "Status"), onClick = onQuery, modifier = Modifier.weight(0.8f))
        }
    }

    if (timeDialog) {
        val state = rememberTimePickerState(initialHour = draft.hour, initialMinute = draft.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { timeDialog = false },
            title = { Text(tr("Lähtöaika", "Departure time")) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    timeDialog = false
                    onChange(draft.copy(hour = state.hour, minute = state.minute))
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { timeDialog = false }) { Text(tr("Peruuta", "Cancel")) } },
        )
    }
    Spacer(Modifier.width(0.dp))
}
