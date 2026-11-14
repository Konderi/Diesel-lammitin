package fi.heatcall.ui.screens

import fi.heatcall.i18n.tr
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fi.heatcall.data.Device
import fi.heatcall.data.HeaterType
import fi.heatcall.data.Mode
import fi.heatcall.sms.Commands
import fi.heatcall.sms.DeviceClock
import fi.heatcall.sms.ReplyBus
import fi.heatcall.ui.components.CarbonBackground
import fi.heatcall.ui.components.ChromeColors
import fi.heatcall.ui.components.ChromeStartButton
import fi.heatcall.ui.components.WheelPicker
import fi.heatcall.ui.components.glassPanel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Toiminnot, jotka laitekortin rataskuvakkeen valikko tarjoaa. */
enum class DeviceAction(private val fi: String, private val en: String) {
    STATUS("Kysy tila", "Check status"),
    TEMPERATURE("Sisälämpötila", "Interior temperature"),
    TIMERS("Ajastimet", "Timers"),
    CLOCK("Laitteen kello", "Device clock"),
    EDIT("Muokkaa laitetta", "Edit device"),
    DELETE("Poista laite", "Delete device"),
    ;

    val label: String get() = tr(fi, en)
}

enum class BottomItem(private val fi: String, private val en: String, val icon: ImageVector) {
    SETTINGS("Info ja\nasetukset", "Info and\nsettings", Icons.Filled.Settings),
    ADD("Lisää\nlaite", "Add\ndevice", Icons.Filled.AddCircle),
    NEWS("Ajan-\nkohtaista", "Latest", Icons.Filled.Notifications),
    HELP("Ohjeet", "Help", Icons.Filled.Info),
    ;

    val label: String get() = tr(fi, en)
}

@Composable
fun MainScreen(
    devices: List<Device>,
    selectedId: String?,
    sendState: ReplyBus.SendState,
    onSelect: (Device) -> Unit,
    onRunChanged: (Device, Int) -> Unit,
    onModeChanged: (Device, Mode) -> Unit,
    onStart: (Device) -> Unit,
    onDeviceAction: (Device, DeviceAction) -> Unit,
    onBottom: (BottomItem) -> Unit,
    smsMissing: Boolean = false,
    onFixPermissions: () -> Unit = {},
) {
    val selected = devices.firstOrNull { it.id == selectedId } ?: devices.firstOrNull()

    CarbonBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (smsMissing) {
                    PermissionBanner(onClick = onFixPermissions)
                    Spacer(Modifier.height(14.dp))
                }
                devices.forEach { device ->
                    DeviceCard(
                        device = device,
                        isSelected = devices.size > 1 && device.id == selected?.id,
                        onClick = { onSelect(device) },
                        onRunChanged = { onRunChanged(device, it) },
                        onModeChanged = { onModeChanged(device, it) },
                        onAction = { onDeviceAction(device, it) },
                        checking = device.id == selected?.id && isStatusCheck(sendState),
                    )
                    Spacer(Modifier.height(14.dp))
                }

                Spacer(Modifier.height(18.dp))

                if (selected != null) {
                    val label = if (selected.run == Device.RUN_OFF) tr("SAMMUTA", "STOP") else tr("KÄYNNISTÄ", "START")
                    val glow = if (selected.run == Device.RUN_OFF) Color(0xFFFF7043) else ChromeColors.Glow
                    ChromeStartButton(
                        label = label,
                        glow = glow,
                        enabled = sendState !is ReplyBus.SendState.Sending,
                        onClick = { onStart(selected) },
                    )
                    Spacer(Modifier.height(10.dp))
                    if (devices.size > 1) {
                        Text(selected.displayName, color = ChromeColors.Label, fontSize = 14.sp)
                    }
                    SendStatusText(sendState)
                }
            }
            BottomBar(onBottom)
        }
    }
}

private fun isStatusCheck(state: ReplyBus.SendState): Boolean = when (state) {
    is ReplyBus.SendState.Sending -> state.label == STATUS_LABEL
    is ReplyBus.SendState.WaitingReply -> state.label == STATUS_LABEL
    else -> false
}

/** Tilakyselyn tunniste lähetystilassa; MainActivity käyttää samaa tekstiä. */
val STATUS_LABEL: String get() = tr("Tilakysely", "Status query")

@Composable
private fun SendStatusText(state: ReplyBus.SendState) {
    val (text, color) = when (state) {
        ReplyBus.SendState.Idle -> "" to Color.Transparent
        is ReplyBus.SendState.Sending -> tr("Lähetetään: ${state.label}…", "Sending: ${state.label}…") to ChromeColors.Label
        is ReplyBus.SendState.WaitingReply -> tr("Viesti lähetetty – odotetaan vastausta lämmittimeltä", "Message sent – waiting for the heater's reply") to Color(0xFFB3E5FC)
        is ReplyBus.SendState.Failed -> state.reason to Color(0xFFFF8A80)
    }
    if (text.isNotEmpty()) {
        Text(
            text,
            color = color,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, start = 24.dp, end = 24.dp),
        )
    }
}

@Composable
fun DeviceCard(
    device: Device,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRunChanged: (Int) -> Unit,
    onModeChanged: (Mode) -> Unit,
    onAction: (DeviceAction) -> Unit,
    checking: Boolean = false,
    onCheckStatus: () -> Unit = { onAction(DeviceAction.STATUS) },
) {
    val runOptions = remember(device.type) {
        buildList {
            add(Device.RUN_OFF)
            addAll((Commands.RUNTIME_MIN..Commands.RUNTIME_MAX step 5).toList())
            if (device.type == HeaterType.AIR) add(Device.RUN_PERM)
        }
    }
    val run = if (device.run in runOptions) device.run else Commands.RUNTIME_DEFAULT

    Column(
        Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.border(2.dp, ChromeColors.Glow, RoundedCornerShape(12.dp)) else Modifier,
            )
            .glassPanel()
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        // Otsikkorivi: auto | Minuutit | Tila | ratas
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                device.displayName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.1f),
            )
            Text(tr("Minuutit", "Minutes"), color = ChromeColors.Label, fontSize = 15.sp, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
            Text(tr("Tila", "Mode"), color = ChromeColors.Label, fontSize = 15.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            GearMenu(onAction)
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.height(102.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VehiclePhoto(device.photoPath, Modifier.weight(1.1f).fillMaxHeight())
            WheelPicker(
                items = runOptions,
                selected = run,
                onSelected = onRunChanged,
                label = { v ->
                    when (v) {
                        Device.RUN_OFF -> tr("Pois", "Off")
                        Device.RUN_PERM -> "∞"
                        else -> v.toString()
                    }
                },
                modifier = Modifier.weight(0.8f),
            )
            WheelPicker(
                items = Mode.entries.toList(),
                selected = device.mode,
                onSelected = onModeChanged,
                label = { it.label },
                modifier = Modifier.weight(1.25f),
            )
        }
        StatusLine(device, checking, onCheckStatus)
        if (DeviceClock.needsDstReminder(device.clockSetAt)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x55FF7043))
                    .clickable { onAction(DeviceAction.CLOCK) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    (if (DeviceClock.lastTransitionWasToWinter() == true) tr("Talviaika alkoi", "Winter time began") else tr("Kesäaika alkoi", "Summer time began")) +
                        tr(" – päivitä lämmittimen kello", " – update the heater clock"),
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(tr("Aseta ›", "Set ›"), color = Color(0xFFFFCC80), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Tilarivi: viimeisin tiedossa oleva tila ja helppo Tarkista tila -painike. */
@Composable
private fun StatusLine(device: Device, checking: Boolean, onCheck: () -> Unit) {
    val on = device.lastKnownOn
    val (text, dot) = when (on) {
        true -> tr("Lämmitin päällä", "Heater on") to Color(0xFFFF7043)
        false -> tr("Lämmitin pois", "Heater off") to Color(0xFF78909C)
        null -> tr("Tila ei tiedossa", "Status unknown") to Color(0x55FFFFFF)
    }
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(dot),
        )
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (checking) tr("Tarkistetaan…", "Checking…") else statusAge(device.lastStatusAt),
                color = ChromeColors.Label,
                fontSize = 12.sp,
            )
        }
        Row(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x332C77A8))
                .border(1.dp, Color(0x883FA9F5), RoundedCornerShape(16.dp))
                .clickable(enabled = !checking, onClick = onCheck)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (checking) {
                CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(6.dp))
            Text(tr("Tarkista tila", "Check status"), color = Color.White, fontSize = 13.sp)
        }
    }
}

/** "juuri nyt", "12 min sitten", "3 h sitten" tai päivämäärä. */
private fun statusAge(at: Long): String {
    if (at <= 0L) return tr("Paina Tarkista tila", "Press Check status")
    val min = (System.currentTimeMillis() - at) / 60_000
    return when {
        min < 1 -> tr("Päivitetty juuri nyt", "Updated just now")
        min < 60 -> tr("Päivitetty $min min sitten", "Updated $min min ago")
        min < 24 * 60 -> tr("Päivitetty ${min / 60} h sitten", "Updated ${min / 60} h ago")
        else -> tr("Päivitetty ", "Updated ") + SimpleDateFormat(tr("d.M. HH.mm", "d/M HH:mm"), Locale.forLanguageTag("fi-FI")).format(Date(at))
    }
}

@Composable
private fun GearMenu(onAction: (DeviceAction) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.clickable { open = true }.padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Settings, contentDescription = tr("Laitteen valikko", "Device menu"), tint = Color.White, modifier = Modifier.size(26.dp))
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DeviceAction.entries.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label) },
                    onClick = {
                        open = false
                        onAction(action)
                    },
                )
            }
        }
    }
}

@Composable
fun VehiclePhoto(path: String?, modifier: Modifier = Modifier) {
    val bitmap: ImageBitmap? = remember(path) { path?.let { loadBitmap(it) } }
    Box(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFF2F4F6))
            .border(3.dp, Color.White, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(bitmap, contentDescription = tr("Auton kuva", "Car photo"), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.AddCircle, contentDescription = null, tint = Color(0xFFB0BAC4), modifier = Modifier.size(34.dp))
                Text("HeatCall", color = Color(0xFF1565C0), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

private fun loadBitmap(path: String): ImageBitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    while (bounds.outWidth / sample > 600 || bounds.outHeight / sample > 600) sample *= 2
    BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
}.getOrNull()

@Composable
private fun BottomBar(onClick: (BottomItem) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(ChromeColors.Bar)
            .navigationBarsPadding()
            .height(72.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BottomItem.entries.forEachIndexed { i, item ->
            if (i > 0) {
                Box(Modifier.width(1.dp).height(44.dp).background(Color(0x55FFFFFF)))
            }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onClick(item) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(item.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                Text(
                    item.label,
                    color = Color.White,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
    HorizontalDivider(color = Color.Black)
}
