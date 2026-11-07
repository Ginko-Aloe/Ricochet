package io.github.ginkoaloe.ricochet.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import io.github.ginkoaloe.ricochet.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Choisir quand une tache a ete faite : la date, puis l'heure.
 *
 * Pas de date future — on ne peut pas avoir fait demain. Une heure future le jour
 * meme est ramenee a [maintenant] plutot que refusee : c'est presque toujours une
 * approximation de « tout a l'heure ».
 *
 * Le `DatePicker` de Material raisonne en minuit UTC ; la conversion vers le jour
 * local se fait ici et nulle part ailleurs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoixDateHeure(
    initial: Instant,
    maintenant: Instant,
    onChoisi: (Instant) -> Unit,
    onAbandon: () -> Unit,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val aujourdhui = maintenant.atZone(zone).toLocalDate()
    val depart = initial.atZone(zone)
    var jourChoisi: LocalDate? by remember { mutableStateOf(null) }

    val jour = jourChoisi
    if (jour == null) {
        val etatDate = rememberDatePickerState(
            initialSelectedDateMillis = depart.toLocalDate().minuitUtc(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    !jourUtc(utcTimeMillis).isAfter(aujourdhui)

                override fun isSelectableYear(year: Int) = year <= aujourdhui.year
            },
        )
        DatePickerDialog(
            onDismissRequest = onAbandon,
            confirmButton = {
                TextButton(
                    onClick = { etatDate.selectedDateMillis?.let { jourChoisi = jourUtc(it) } },
                    enabled = etatDate.selectedDateMillis != null,
                ) { Text(stringResource(R.string.suivant)) }
            },
            dismissButton = { TextButton(onClick = onAbandon) { Text(stringResource(R.string.annuler)) } },
        ) {
            DatePicker(state = etatDate)
        }
    } else {
        val etatHeure = rememberTimePickerState(
            initialHour = depart.hour,
            initialMinute = depart.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = onAbandon,
            title = { Text(stringResource(R.string.a_quelle_heure)) },
            text = { TimePicker(state = etatHeure) },
            confirmButton = {
                TextButton(onClick = {
                    val choisi = LocalDateTime.of(jour, LocalTime.of(etatHeure.hour, etatHeure.minute))
                        .atZone(zone).toInstant()
                    onChoisi(minOf(choisi, maintenant))
                }) { Text(stringResource(R.string.valider)) }
            },
            dismissButton = { TextButton(onClick = onAbandon) { Text(stringResource(R.string.annuler)) } },
        )
    }
}

private fun LocalDate.minuitUtc(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun jourUtc(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
