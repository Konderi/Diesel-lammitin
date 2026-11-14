package fi.heatcall.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import fi.heatcall.sms.Commands
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "heatcall")

enum class HeaterType(val label: String) {
    WATER("Vesilämmitin"),
    AIR("Ilmalämmitin"),
}

enum class Mode(val label: String) {
    HEAT("Lämmitys"),
    FAN("Tuuletus"),
}

/** Ajastimen luonnos, jotta lomake muistaa viimeksi lähetetyt arvot. */
data class TimerDraft(
    val day: Commands.Day = Commands.Day.MO_FR,
    val hour: Int = 7,
    val minute: Int = 0,
    val runMinutes: Int = Commands.RUNTIME_DEFAULT,
)

/** Yksi lämmitin eli yksi EasyStart Call -yksikkö (yksi auto). */
data class Device(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val phone: String = "",
    val pin: String = "1234",
    val type: HeaterType = HeaterType.WATER,
    /** Minuutit-rullan valinta: [RUN_OFF], [RUN_PERM] tai 10–120. */
    val run: Int = Commands.RUNTIME_DEFAULT,
    val mode: Mode = Mode.HEAT,
    val photoPath: String? = null,
    val timers: List<TimerDraft> = List(3) { TimerDraft() },
    /** Viimeisin lämmittimen ilmoittama tila: true = päällä, false = pois, null = ei tiedossa. */
    val lastKnownOn: Boolean? = null,
    val lastStatusAt: Long = 0L,
    /** Milloin laitteen kello asetettiin viimeksi HeatCallista (0 = ei tiedossa). */
    val clockSetAt: Long = 0L,
) {
    val displayName: String get() = name.ifBlank { "Lämmitin" }

    companion object {
        const val RUN_OFF = 0
        const val RUN_PERM = -1
    }
}

data class LogEntry(
    val timestamp: Long,
    val outgoing: Boolean,
    val deviceName: String,
    val text: String,
)

data class AppState(
    val devices: List<Device> = emptyList(),
    val selectedId: String? = null,
    val markRepliesRead: Boolean = true,
) {
    val selected: Device? get() = devices.firstOrNull { it.id == selectedId } ?: devices.firstOrNull()

    fun deviceForSender(sender: String?): Device? = devices.firstOrNull { Commands.samePhone(sender, it.phone) }
}

class DeviceRepository(private val context: Context) {

    private object Keys {
        val DEVICES = stringPreferencesKey("devices")
        val SELECTED = stringPreferencesKey("selected")
        val MARK_READ = booleanPreferencesKey("mark_read")
        val LOG = stringPreferencesKey("log")
    }

    val state: Flow<AppState> = context.dataStore.data.map { p ->
        AppState(
            devices = decodeDevices(p[Keys.DEVICES]),
            selectedId = p[Keys.SELECTED],
            markRepliesRead = p[Keys.MARK_READ] ?: true,
        )
    }

    suspend fun current(): AppState = state.first()

    suspend fun upsert(device: Device) {
        context.dataStore.edit { p ->
            val list = decodeDevices(p[Keys.DEVICES]).toMutableList()
            val i = list.indexOfFirst { it.id == device.id }
            if (i >= 0) list[i] = device else list += device
            p[Keys.DEVICES] = encodeDevices(list)
            if (p[Keys.SELECTED] == null) p[Keys.SELECTED] = device.id
        }
    }

    suspend fun update(id: String, change: (Device) -> Device) {
        context.dataStore.edit { p ->
            val list = decodeDevices(p[Keys.DEVICES]).map { if (it.id == id) change(it) else it }
            p[Keys.DEVICES] = encodeDevices(list)
        }
    }

    suspend fun delete(id: String) {
        context.dataStore.edit { p ->
            val list = decodeDevices(p[Keys.DEVICES])
            list.firstOrNull { it.id == id }?.photoPath?.let { File(it).delete() }
            val rest = list.filterNot { it.id == id }
            p[Keys.DEVICES] = encodeDevices(rest)
            if (p[Keys.SELECTED] == id) {
                rest.firstOrNull()?.let { p[Keys.SELECTED] = it.id } ?: p.remove(Keys.SELECTED)
            }
        }
    }

    suspend fun select(id: String) {
        context.dataStore.edit { it[Keys.SELECTED] = id }
    }

    suspend fun setMarkRepliesRead(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MARK_READ] = enabled }
    }

    // --- Viestiloki ------------------------------------------------------------

    val log: Flow<List<LogEntry>> = context.dataStore.data.map { p -> decodeLog(p[Keys.LOG]) }

    suspend fun appendLog(entry: LogEntry) {
        context.dataStore.edit { p ->
            p[Keys.LOG] = encodeLog((listOf(entry) + decodeLog(p[Keys.LOG])).take(MAX_LOG))
        }
    }

    suspend fun clearLog() {
        context.dataStore.edit { it.remove(Keys.LOG) }
    }

    // --- JSON ------------------------------------------------------------------

    private fun encodeDevices(list: List<Device>): String {
        val arr = JSONArray()
        list.forEach { d ->
            val timers = JSONArray()
            d.timers.forEach { t ->
                timers.put(JSONObject().put("day", t.day.name).put("h", t.hour).put("m", t.minute).put("run", t.runMinutes))
            }
            arr.put(
                JSONObject()
                    .put("id", d.id).put("name", d.name).put("phone", d.phone).put("pin", d.pin)
                    .put("type", d.type.name).put("run", d.run).put("mode", d.mode.name)
                    .put("photo", d.photoPath ?: "")
                    .put("timers", timers)
                    .put("on", d.lastKnownOn?.toString() ?: "")
                    .put("statusAt", d.lastStatusAt)
                    .put("clockSetAt", d.clockSetAt),
            )
        }
        return arr.toString()
    }

    private fun decodeDevices(s: String?): List<Device> {
        if (s.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(s)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val ta = o.optJSONArray("timers")
                val timers = (0 until 3).map { n ->
                    val t = ta?.optJSONObject(n)
                    if (t == null) {
                        TimerDraft()
                    } else {
                        TimerDraft(
                            day = runCatching { Commands.Day.valueOf(t.getString("day")) }.getOrDefault(Commands.Day.MO_FR),
                            hour = t.optInt("h", 7),
                            minute = t.optInt("m", 0),
                            runMinutes = t.optInt("run", Commands.RUNTIME_DEFAULT),
                        )
                    }
                }
                Device(
                    id = o.getString("id"),
                    name = o.optString("name"),
                    phone = o.optString("phone"),
                    pin = o.optString("pin", "1234"),
                    type = runCatching { HeaterType.valueOf(o.getString("type")) }.getOrDefault(HeaterType.WATER),
                    run = o.optInt("run", Commands.RUNTIME_DEFAULT),
                    mode = runCatching { Mode.valueOf(o.getString("mode")) }.getOrDefault(Mode.HEAT),
                    photoPath = o.optString("photo").ifBlank { null },
                    timers = timers,
                    lastKnownOn = o.optString("on").ifBlank { null }?.toBooleanStrictOrNull(),
                    lastStatusAt = o.optLong("statusAt", 0L),
                    clockSetAt = o.optLong("clockSetAt", 0L),
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeLog(list: List<LogEntry>): String {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("t", it.timestamp).put("o", it.outgoing).put("d", it.deviceName).put("x", it.text))
        }
        return arr.toString()
    }

    private fun decodeLog(s: String?): List<LogEntry> {
        if (s.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(s)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                LogEntry(o.getLong("t"), o.getBoolean("o"), o.optString("d"), o.getString("x"))
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val MAX_LOG = 150
    }
}
