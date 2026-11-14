package fi.heatcall

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fi.heatcall.data.AppState
import fi.heatcall.data.Device
import fi.heatcall.data.Mode
import fi.heatcall.sms.Commands
import fi.heatcall.sms.ReplyBus
import fi.heatcall.sms.SmsReceiver
import fi.heatcall.sms.SmsSender
import fi.heatcall.ui.components.ChromeColors
import fi.heatcall.sms.DeviceClock
import fi.heatcall.ui.screens.BottomItem
import fi.heatcall.ui.screens.ClockDialog
import fi.heatcall.ui.screens.DeviceAction
import fi.heatcall.ui.screens.DeviceEditScreen
import fi.heatcall.ui.screens.HelpScreen
import fi.heatcall.ui.screens.MainScreen
import fi.heatcall.ui.screens.NewsScreen
import fi.heatcall.ui.screens.PermissionHelpDialog
import fi.heatcall.ui.screens.rememberPermissionStatus
import fi.heatcall.ui.screens.ReplyDialog
import fi.heatcall.ui.screens.STATUS_LABEL
import fi.heatcall.ui.screens.SettingsScreen
import fi.heatcall.ui.screens.TimersScreen
import fi.heatcall.ui.theme.HeatCallTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val app get() = application as HeatCallApp

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        // Evätty tai "rajoitettu asetus" (tiedostosta asennettu sovellus) → näytetään ohje.
        if (result[Manifest.permission.SEND_SMS] == false || result[Manifest.permission.RECEIVE_SMS] == false) {
            showPermissionHelp = true
        }
    }

    /** Ohje rajoitettujen asetusten sallimiseen (PermissionHelpDialog). */
    private var showPermissionHelp by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleReplyIntent(intent)
        setContent {
            HeatCallTheme {
                val state by app.repository.state.collectAsStateWithLifecycle(initialValue = null)
                // Odotetaan tallennetut tiedot ennen kuin päätetään, näytetäänkö tervetulonäkymä.
                val loaded = state
                if (loaded != null) AppRoot(loaded) else Box(Modifier.fillMaxSize().background(ChromeColors.Deep))
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleReplyIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        ReplyBus.isForeground = true
        fi.heatcall.sms.MarkReadListener.rebindIfNeeded(this)
    }

    override fun onStop() {
        ReplyBus.isForeground = false
        super.onStop()
    }

    /** Ilmoituksesta avattu vastaus näytetään ikkunassa, vaikka prosessi olisi välillä suljettu. */
    private fun handleReplyIntent(intent: Intent?) {
        val text = intent?.getStringExtra(EXTRA_REPLY_TEXT) ?: return
        ReplyBus.showReply(
            ReplyBus.IncomingReply(
                deviceName = intent.getStringExtra(EXTRA_REPLY_DEVICE) ?: "Lämmitin",
                text = text,
                timestamp = intent.getLongExtra(EXTRA_REPLY_TIME, System.currentTimeMillis()),
                deviceId = intent.getStringExtra(EXTRA_REPLY_DEVICE_ID),
            ),
        )
        intent.removeExtra(EXTRA_REPLY_TEXT)
    }

    private fun requiredPermissions(): Array<String> = buildList {
        add(Manifest.permission.SEND_SMS)
        add(Manifest.permission.RECEIVE_SMS)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()

    private fun missingPermissions(): List<String> = requiredPermissions().filter {
        ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissionsIfNeeded() {
        val missing = missingPermissions()
        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
    }

    private fun send(device: Device, command: String, label: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionsIfNeeded()
            return
        }
        SmsSender.send(this, device, command, label)
        // Kellon asetus: muistetaan aika, jotta kesä-/talviaikamuistutus poistuu.
        if (command.contains("devicetime:set", ignoreCase = true)) {
            launchIo { app.repository.update(device.id) { it.copy(clockSetAt = System.currentTimeMillis()) } }
        }
    }

    private fun launchIo(block: suspend () -> Unit) {
        app.appScope.launch { block() }
    }

    private sealed interface Screen {
        data object Main : Screen
        data object Add : Screen
        data class Edit(val id: String) : Screen
        data class Timers(val id: String) : Screen
        data object News : Screen
        data object Settings : Screen
        data object Help : Screen
    }

    @Composable
    private fun AppRoot(state: AppState) {
        var screen by remember { mutableStateOf<Screen>(Screen.Main) }
        var confirmDelete by remember { mutableStateOf<Device?>(null) }
        var clockDeviceId by remember { mutableStateOf<String?>(null) }
        var permissionsAsked by rememberSaveable { mutableStateOf(false) }
        val reply by ReplyBus.reply.collectAsStateWithLifecycle()
        val sendState by ReplyBus.send.collectAsStateWithLifecycle()
        val log by app.repository.log.collectAsStateWithLifecycle(initialValue = emptyList())

        // Jos lämmitin ei vastaa (esim. ei kenttää tai kuittaus pois), ei jäädä odottamaan loputtomiin.
        LaunchedEffect(sendState) {
            val waiting = sendState as? ReplyBus.SendState.WaitingReply ?: return@LaunchedEffect
            kotlinx.coroutines.delay(REPLY_TIMEOUT_MS)
            if (ReplyBus.send.value == waiting) {
                ReplyBus.setSendState(
                    ReplyBus.SendState.Failed("Lämmitin ei vastannut 3 minuutissa. Tarkista, että autossa on kenttää."),
                )
            }
        }

        val hasDevices = state.devices.isNotEmpty()
        LaunchedEffect(hasDevices) {
            if (hasDevices && !permissionsAsked) {
                permissionsAsked = true
                requestPermissionsIfNeeded()
            }
        }

        val back = { screen = Screen.Main }
        BackHandler(enabled = screen != Screen.Main) { back() }

        // Ensimmäinen käynnistys: kysytään numero ja PIN.
        if (state.devices.isEmpty()) {
            DeviceEditScreen(
                initial = null,
                firstRun = true,
                onSave = { d ->
                    launchIo { app.repository.upsert(d) }
                    requestPermissionsIfNeeded()
                    permissionsAsked = true
                },
                onBack = null,
            )
            return
        }

        val permissions = rememberPermissionStatus()

        when (val s = screen) {
            Screen.Main -> MainScreen(
                smsMissing = permissions.smsMissing,
                onFixPermissions = { showPermissionHelp = true },
                devices = state.devices,
                selectedId = state.selected?.id,
                sendState = sendState,
                onSelect = { d -> launchIo { app.repository.select(d.id) } },
                onRunChanged = { d, run ->
                    if (d.run != run) launchIo { app.repository.update(d.id) { it.copy(run = run) } }
                },
                onModeChanged = { d, mode ->
                    if (d.mode != mode) launchIo { app.repository.update(d.id) { it.copy(mode = mode) } }
                },
                onStart = { d -> startOrStop(d) },
                onDeviceAction = { d, action ->
                    when (action) {
                        DeviceAction.STATUS -> {
                            launchIo { app.repository.select(d.id) }
                            send(d, Commands.heaterStatus(d.pin), STATUS_LABEL)
                        }
                        DeviceAction.TEMPERATURE -> send(d, Commands.temperature(d.pin), "Sisälämpötila")
                        DeviceAction.TIMERS -> screen = Screen.Timers(d.id)
                        DeviceAction.CLOCK -> clockDeviceId = d.id
                        DeviceAction.EDIT -> screen = Screen.Edit(d.id)
                        DeviceAction.DELETE -> confirmDelete = d
                    }
                },
                onBottom = { item ->
                    screen = when (item) {
                        BottomItem.SETTINGS -> Screen.Settings
                        BottomItem.ADD -> Screen.Add
                        BottomItem.NEWS -> Screen.News
                        BottomItem.HELP -> Screen.Help
                    }
                },
            )

            Screen.Add -> DeviceEditScreen(
                initial = null,
                firstRun = false,
                onSave = { d ->
                    launchIo {
                        app.repository.upsert(d)
                        app.repository.select(d.id)
                    }
                    back()
                },
                onBack = back,
            )

            is Screen.Edit -> {
                val d = state.devices.firstOrNull { it.id == s.id }
                if (d == null) {
                    LaunchedEffect(Unit) { back() }
                } else {
                    DeviceEditScreen(
                        initial = d,
                        firstRun = false,
                        onSave = { updated ->
                            launchIo { app.repository.upsert(updated) }
                            back()
                        },
                        onBack = back,
                    )
                }
            }

            is Screen.Timers -> {
                val d = state.devices.firstOrNull { it.id == s.id }
                if (d == null) {
                    LaunchedEffect(Unit) { back() }
                } else {
                    TimersScreen(
                        device = d,
                        onSaveDraft = { n, draft ->
                            launchIo {
                                app.repository.update(d.id) { dev ->
                                    dev.copy(timers = dev.timers.mapIndexed { i, t -> if (i == n - 1) draft else t })
                                }
                            }
                        },
                        onSend = { command, label -> send(d, command, label) },
                        onBack = back,
                    )
                }
            }

            Screen.News -> NewsScreen(
                log = log,
                onClear = { launchIo { app.repository.clearLog() } },
                onBack = back,
            )

            Screen.Settings -> SettingsScreen(
                devices = state.devices,
                markRepliesRead = state.markRepliesRead,
                onMarkReadChanged = { v -> launchIo { app.repository.setMarkRepliesRead(v) } },
                onEditDevice = { d -> screen = Screen.Edit(d.id) },
                onSend = { d, command, label -> send(d, command, label) },
                onBack = back,
                onRequestPermissions = { requestPermissionsIfNeeded() },
                onShowPermissionHelp = { showPermissionHelp = true },
            )

            Screen.Help -> HelpScreen(onBack = back)
        }

        if (showPermissionHelp) {
            PermissionHelpDialog(onDismiss = { showPermissionHelp = false })
        }

        reply?.let { r ->
            val replyDevice = state.devices.firstOrNull { it.id == r.deviceId }
                ?: state.devices.firstOrNull { it.displayName == r.deviceName }
            ReplyDialog(
                r,
                onDismiss = {
                    ReplyBus.dismissReply()
                    NotificationManagerCompat.from(this).cancel(SmsReceiver.NOTIFICATION_ID)
                },
                onFixClock = replyDevice?.let { d -> { send(d, DeviceClock.setFromPhone(d.pin), "Kellon asetus") } },
            )
        }

        state.devices.firstOrNull { it.id == clockDeviceId }?.let { d ->
            ClockDialog(
                device = d,
                onSetFromPhone = { send(d, DeviceClock.setFromPhone(d.pin), "Kellon asetus") },
                onQuery = { send(d, Commands.deviceTimeQuery(d.pin), "Kellon kysely") },
                onSetManual = { day, h, m -> send(d, Commands.deviceTimeSet(d.pin, day, h, m), "Kellon asetus") },
                onDismiss = { clockDeviceId = null },
            )
        }

        confirmDelete?.let { d ->
            AlertDialog(
                onDismissRequest = { confirmDelete = null },
                title = { Text("Poistetaanko ${d.displayName}?") },
                text = { Text("Laitteen tiedot ja ajastinluonnokset poistetaan tästä sovelluksesta. Itse lämmittimeen ei tehdä muutoksia.") },
                confirmButton = {
                    TextButton(onClick = {
                        confirmDelete = null
                        launchIo { app.repository.delete(d.id) }
                    }) { Text("Poista") }
                },
                dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Peruuta") } },
            )
        }
    }

    private fun startOrStop(d: Device) {
        launchIo { app.repository.select(d.id) }
        when (d.run) {
            Device.RUN_OFF -> send(d, Commands.heaterOff(d.pin), "Sammutus")
            Device.RUN_PERM -> send(
                d,
                Commands.heaterOn(d.pin, Commands.RUNTIME_DEFAULT, fan = d.mode == Mode.FAN, permanent = true),
                "Käynnistys (kestokäyttö)",
            )
            else -> send(
                d,
                Commands.heaterOn(d.pin, d.run, fan = d.mode == Mode.FAN),
                if (d.mode == Mode.FAN) "Tuuletus ${d.run} min" else "Lämmitys ${d.run} min",
            )
        }
    }

    companion object {
        const val EXTRA_REPLY_DEVICE = "reply_device"
        const val EXTRA_REPLY_TEXT = "reply_text"
        const val EXTRA_REPLY_TIME = "reply_time"
        const val EXTRA_REPLY_DEVICE_ID = "reply_device_id"
        private const val REPLY_TIMEOUT_MS = 3 * 60 * 1000L
    }
}
