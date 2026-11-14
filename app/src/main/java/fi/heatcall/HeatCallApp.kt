package fi.heatcall

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import fi.heatcall.data.AppState
import fi.heatcall.i18n.AppLanguage
import fi.heatcall.i18n.I18n
import fi.heatcall.i18n.tr
import fi.heatcall.data.DeviceRepository
import fi.heatcall.widget.StatusWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HeatCallApp : Application() {

    /** Sovelluksen elinkaaren mittainen scope taustatallennuksille. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var repository: DeviceRepository
        private set

    /**
     * Viimeisin tila muistissa. Ilmoituskuuntelija ja SMS-vastaanotin
     * tarvitsevat lämmittimien numerot synkronisesti.
     */
    @Volatile
    var cachedState: AppState = AppState()
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        I18n.init(this)
        repository = DeviceRepository(this)
        appScope.launch {
            repository.state.collect { state ->
                val before = cachedState.selected
                cachedState = state
                // Kotinäytön widget seuraa valitun auton tilaa.
                val after = state.selected
                if (before?.id != after?.id || before?.lastKnownOn != after?.lastKnownOn ||
                    before?.lastStatusAt != after?.lastStatusAt || before?.name != after?.name
                ) {
                    StatusWidget.renderAll(this@HeatCallApp, state)
                }
            }
        }
        createChannels()
    }

    /** Vaihtaa sovelluksen kielen ja päivittää ilmoituskanavan nimen ja widgetin. */
    fun setLanguage(lang: AppLanguage) {
        I18n.set(this, lang)
        createChannels()
        StatusWidget.renderAll(this, cachedState)
    }

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        // createNotificationChannel päivittää olemassa olevan kanavan nimen.
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REPLIES,
                tr("Lämmittimen vastaukset", "Heater replies"),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = tr("Ilmoitus, kun lämmitin vastaa komentoon", "Notification when the heater replies to a command") },
        )
    }

    companion object {
        const val CHANNEL_REPLIES = "replies"

        lateinit var instance: HeatCallApp
            private set
    }
}
