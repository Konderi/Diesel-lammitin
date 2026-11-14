package fi.heatcall.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import fi.heatcall.HeatCallApp
import fi.heatcall.i18n.AppLanguage
import fi.heatcall.i18n.I18n

/** Kielivalinta: Suomi / English. Vaihto näkyy heti koko sovelluksessa. */
@Composable
fun LanguagePicker() {
    val app = LocalContext.current.applicationContext as HeatCallApp
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppLanguage.entries.forEach { lang ->
            FilterChip(
                selected = I18n.language == lang,
                onClick = { app.setLanguage(lang) },
                label = { Text(lang.nativeName) },
                colors = FilterChipDefaults.filterChipColors(labelColor = Color.White),
            )
        }
    }
}
