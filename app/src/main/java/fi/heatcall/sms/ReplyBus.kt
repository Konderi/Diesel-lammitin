package fi.heatcall.sms

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Sovelluksen sisäinen tila: lähetyksen eteneminen ja näytettävä vastausikkuna. */
object ReplyBus {

    data class IncomingReply(
        val deviceName: String,
        val text: String,
        val timestamp: Long,
        val deviceId: String? = null,
    )

    sealed interface SendState {
        data object Idle : SendState
        data class Sending(val label: String, val deviceName: String) : SendState
        data class WaitingReply(val label: String, val sentAt: Long) : SendState
        data class Failed(val reason: String) : SendState
    }

    private val _reply = MutableStateFlow<IncomingReply?>(null)
    val reply: StateFlow<IncomingReply?> = _reply.asStateFlow()

    private val _send = MutableStateFlow<SendState>(SendState.Idle)
    val send: StateFlow<SendState> = _send.asStateFlow()

    /** Onko sovelluksen ikkuna näkyvissä (MainActivity onStart/onStop). */
    @Volatile
    var isForeground: Boolean = false

    fun showReply(reply: IncomingReply) {
        _reply.value = reply
        _send.value = SendState.Idle
    }

    fun dismissReply() {
        _reply.value = null
    }

    fun setSendState(state: SendState) {
        _send.value = state
    }
}
