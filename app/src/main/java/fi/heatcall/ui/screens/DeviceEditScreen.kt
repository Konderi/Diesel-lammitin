package fi.heatcall.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import fi.heatcall.data.Device
import fi.heatcall.data.HeaterType
import fi.heatcall.sms.Commands
import fi.heatcall.ui.components.BodyText
import fi.heatcall.ui.components.PrimaryButton
import fi.heatcall.ui.components.SecondaryButton
import fi.heatcall.ui.components.Section
import fi.heatcall.ui.components.SubScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Laitteen lisäys ja muokkaus. Ensimmäisellä käynnistyskerralla ([firstRun]) tämä näytetään
 * tervetulonäkymänä, josta ei pääse takaisin ennen kuin numero ja PIN on annettu.
 */
@Composable
fun DeviceEditScreen(
    initial: Device?,
    firstRun: Boolean,
    onSave: (Device) -> Unit,
    onBack: (() -> Unit)?,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val base = remember { initial ?: Device(name = if (firstRun) "Oma auto" else "") }

    var name by remember { mutableStateOf(base.name) }
    var phone by remember { mutableStateOf(base.phone) }
    var pin by remember { mutableStateOf(if (initial == null) "" else base.pin) }
    var showPin by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf(base.type) }
    var photoPath by remember { mutableStateOf(base.photoPath) }
    var photoVersion by remember { mutableStateOf(0) }
    var tried by remember { mutableStateOf(false) }

    val phoneOk = Commands.isValidPhone(phone)
    val pinOk = Commands.isValidPin(pin)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val path = withContext(Dispatchers.IO) { savePhoto(context, uri, base.id) }
                if (path != null) {
                    photoPath = path
                    photoVersion++
                }
            }
        }
    }

    SubScreen(
        title = when {
            firstRun -> "Tervetuloa HeatCalliin"
            initial == null -> "Lisää laite"
            else -> "Muokkaa laitetta"
        },
        onBack = onBack,
    ) {
        if (firstRun) {
            Section {
                BodyText(
                    "HeatCall ohjaa Eberspächer-lämmitintä EasyStart Call -yksikön kautta tekstiviesteillä.\n\n" +
                        "Anna aluksi lämmittimen SIM-kortin puhelinnumero ja laitteen nelinumeroinen PIN-koodi " +
                        "(tehdasasetus 1234). Voit muuttaa näitä myöhemmin kohdasta Info ja asetukset.",
                )
            }
        }

        Section("Lämmittimen tiedot") {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Lämmittimen puhelinnumero") },
                placeholder = { Text("+358 40 123 4567") },
                singleLine = true,
                isError = tried && !phoneOk,
                supportingText = { if (tried && !phoneOk) Text("Tarkista numero, esim. +358401234567") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = pin,
                onValueChange = { v -> pin = v.filter { it.isDigit() }.take(4) },
                label = { Text("PIN-koodi (4 numeroa)") },
                singleLine = true,
                isError = tried && !pinOk,
                supportingText = { if (tried && !pinOk) Text("PIN on tasan neljä numeroa") },
                visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                trailingIcon = {
                    androidx.compose.material3.TextButton(onClick = { showPin = !showPin }) {
                        Text(if (showPin) "Piilota" else "Näytä")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(24) },
                label = { Text("Nimi (esim. auton merkki)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Section("Lämmittimen tyyppi") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaterType.entries.forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t },
                        label = { Text(t.label) },
                        colors = FilterChipDefaults.filterChipColors(labelColor = Color.White),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            BodyText(
                if (type == HeaterType.WATER) {
                    "Vesilämmitin: käyntiaika 10–120 minuuttia."
                } else {
                    "Ilmalämmitin: lisäksi kestokäyttö (∞)."
                },
            )
        }

        Section("Auton kuva") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.runtime.key(photoVersion) {
                    VehiclePhoto(photoPath, Modifier.size(width = 120.dp, height = 90.dp))
                }
                androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SecondaryButton("Valitse kuva", onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    })
                    if (photoPath != null) {
                        SecondaryButton("Poista kuva", onClick = { photoPath = null })
                    }
                }
            }
        }

        PrimaryButton(
            text = if (firstRun) "Tallenna ja aloita" else "Tallenna",
            onClick = {
                tried = true
                if (phoneOk && pinOk) {
                    onSave(
                        base.copy(
                            name = name.trim(),
                            phone = Commands.normalizePhone(phone),
                            pin = pin,
                            type = type,
                            photoPath = photoPath,
                            // Ilmalämmittimen kestokäyttö ei kelpaa vesilämmittimelle.
                            run = if (type == HeaterType.WATER && base.run == Device.RUN_PERM) Commands.RUNTIME_DEFAULT else base.run,
                        ),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
    }
}

/** Kopioi valitun kuvan sovelluksen omaan kansioon pienennettynä. */
private fun savePhoto(context: Context, uri: Uri, deviceId: String): String? = runCatching {
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        val max = 800
        val w = info.size.width
        val h = info.size.height
        if (w > max || h > max) {
            val scale = max.toFloat() / maxOf(w, h)
            decoder.setTargetSize((w * scale).toInt(), (h * scale).toInt())
        }
    }
    val dir = File(context.filesDir, "photos").apply { mkdirs() }
    // Uusi tiedostonimi joka kerta, jotta muistissa oleva vanha kuva ei jää näkyviin.
    dir.listFiles { f -> f.name.startsWith(deviceId) }?.forEach { it.delete() }
    val file = File(dir, "$deviceId-${System.currentTimeMillis()}.jpg")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
    file.absolutePath
}.getOrNull()
