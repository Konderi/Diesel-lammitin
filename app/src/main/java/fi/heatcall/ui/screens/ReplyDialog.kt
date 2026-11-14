package fi.heatcall.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import fi.heatcall.sms.DeviceClock
import fi.heatcall.sms.ReplyBus
import fi.heatcall.sms.ReplyParser
import fi.heatcall.ui.components.BodyText
import fi.heatcall.ui.components.CarbonBackground
import fi.heatcall.ui.components.PrimaryButton
import fi.heatcall.ui.components.glassPanel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Lämmittimen vastaus omassa ikkunassaan. */
@Composable
fun ReplyDialog(reply: ReplyBus.IncomingReply, onDismiss: () -> Unit, onFixClock: (() -> Unit)? = null) {
    val parsed = remember(reply) { ReplyParser.parse(reply.text) }
    val time = remember(reply) {
        SimpleDateFormat("HH.mm", Locale.forLanguageTag("fi-FI")).format(Date(reply.timestamp))
    }
    val accent = when {
        parsed.isError -> Color(0xFFFF5252)
        parsed.heaterOn == true -> Color(0xFFFF7043)
        else -> Color(0xFF4FC3F7)
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))) {
            CarbonBackground(Modifier.matchParentSize()) {}
            Column(
                Modifier
                    .fillMaxWidth()
                    .glassPanel()
                    .padding(18.dp),
            ) {
                Text("${reply.deviceName} · vastaus klo $time", color = Color(0xCCFFFFFF), fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(14.dp).clip(CircleShape).background(accent))
                    Spacer(Modifier.width(10.dp))
                    Text(parsed.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                parsed.lines.forEach { line ->
                    BodyText(line, color = if (parsed.isError && line == parsed.lines.first()) Color(0xFFFF8A80) else Color.White)
                }
                // Kellon kysely: verrataan laitteen aikaa puhelimen aikaan vastaushetkellä.
                val offset = remember(reply) {
                    DeviceClock.offsetMinutes(
                        reply.text,
                        java.time.Instant.ofEpochMilli(reply.timestamp).atZone(java.time.ZoneId.systemDefault()),
                    )
                }
                if (offset != null) {
                    Spacer(Modifier.height(8.dp))
                    if (DeviceClock.isWrong(offset)) {
                        BodyText("Laitteen kello on ${DeviceClock.describeOffset(offset)} puhelimen ajasta.", color = Color(0xFFFFCC80))
                        if (onFixClock != null) {
                            Spacer(Modifier.height(8.dp))
                            PrimaryButton("Aseta oikea aika", onClick = {
                                onFixClock()
                                onDismiss()
                            }, modifier = Modifier.fillMaxWidth())
                        }
                    } else {
                        BodyText("Laitteen kello on oikeassa ajassa ✓", color = Color(0xFFA5D6A7))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(reply.text, color = Color(0x88FFFFFF), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                Spacer(Modifier.height(16.dp))
                PrimaryButton("OK", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
