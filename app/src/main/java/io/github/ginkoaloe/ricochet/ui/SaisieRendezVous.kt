package io.github.ginkoaloe.ricochet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import io.github.ginkoaloe.ricochet.R
import androidx.compose.ui.unit.dp
import io.github.ginkoaloe.ricochet.domaine.Calendrier
import io.github.ginkoaloe.ricochet.domaine.Position
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.MonthDay
import java.time.ZoneOffset
import java.time.format.TextStyle

/**
 * La saisie d'un Rendez-vous, en phrase a completer : « Tous les [N] [mois] », puis
 * les jours selon l'unite, puis « a [9 h] ».
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SaisieRendezVous(
    brouillon: BrouillonRendezVous,
    onChange: (BrouillonRendezVous) -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = rememberTextes()
    var choixHeure by remember { mutableStateOf(false) }
    var choixDate by remember { mutableStateOf(false) }
    var tousTexte by remember { mutableStateOf(brouillon.tous.toString()) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.tous_les), style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(
                value = tousTexte,
                onValueChange = { saisi ->
                    tousTexte = saisi.filter { it.isDigit() }.take(2)
                    // Vide ou 0 rend la saisie invalide, plutot que de garder en douce
                    // l'ancienne valeur : ce qui s'affiche est ce qui s'enregistre.
                    onChange(brouillon.copy(tous = tousTexte.toIntOrNull() ?: 0))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(72.dp),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UniteRendezVous.entries.forEach { unite ->
                val libelle = pluralStringResource(unite.libelle, brouillon.tous.coerceAtLeast(1))
                val choisir = { onChange(brouillon.copy(unite = unite)) }
                if (unite == brouillon.unite) Button(onClick = choisir, modifier = Modifier.weight(1f)) { Text(libelle, maxLines = 1) }
                else OutlinedButton(onClick = choisir, modifier = Modifier.weight(1f)) { Text(libelle, maxLines = 1) }
            }
        }

        when (brouillon.unite) {
            UniteRendezVous.SEMAINE -> JetonsJours(brouillon.joursSemaine) { onChange(brouillon.copy(joursSemaine = it)) }

            UniteRendezVous.MOIS -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !brouillon.parRang,
                        onClick = { onChange(brouillon.copy(parRang = false)) },
                        label = { Text(stringResource(R.string.certains_jours)) },
                    )
                    FilterChip(
                        selected = brouillon.parRang,
                        onClick = { onChange(brouillon.copy(parRang = true)) },
                        label = { Text(stringResource(R.string.jour_de_semaine)) },
                    )
                }
                if (!brouillon.parRang) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..Calendrier.Mensuel.DERNIER_JOUR).forEach { jour ->
                            FilterChip(
                                selected = jour in brouillon.joursMois,
                                onClick = { onChange(brouillon.copy(joursMois = brouillon.joursMois.basculer(jour))) },
                                label = { Text(if (jour == Calendrier.Mensuel.DERNIER_JOUR) stringResource(R.string.dernier) else "$jour") },
                            )
                        }
                    }
                } else {
                    Text(stringResource(R.string.le_rang), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 2, 3, 4, Position.DERNIER).forEach { rang ->
                            FilterChip(
                                selected = rang in brouillon.rangs,
                                onClick = { onChange(brouillon.copy(rangs = brouillon.rangs.basculer(rang))) },
                                label = { Text(libelleRang(t, rang)) },
                            )
                        }
                    }
                    JetonsJours(brouillon.joursRang) { onChange(brouillon.copy(joursRang = it)) }
                }
            }

            UniteRendezVous.AN -> OutlinedButton(onClick = { choixDate = true }) {
                Text(
                    stringResource(
                        R.string.date_annuelle_bouton,
                        numeroDuJour(t, brouillon.date.dayOfMonth),
                        nomDuMois(t, brouillon.date.month),
                    )
                )
            }
        }

        OutlinedButton(onClick = { choixHeure = true }) { Text(stringResource(R.string.a_heure, libelleHeure(t, brouillon.heure))) }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = brouillon.tenirLeCompteDesOublis,
                onCheckedChange = { onChange(brouillon.copy(tenirLeCompteDesOublis = it)) },
            )
            Column {
                Text(stringResource(R.string.tenir_le_compte), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(R.string.tenir_le_compte_explication),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (choixHeure) ChoixHeure(brouillon.heure, onChoisi = { onChange(brouillon.copy(heure = it)); choixHeure = false }, onAbandon = { choixHeure = false })
    if (choixDate) ChoixDateAnnuelle(brouillon.date, onChoisi = { onChange(brouillon.copy(date = it)); choixDate = false }, onAbandon = { choixDate = false })
}

/** Les sept jours de la semaine, a cocher. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JetonsJours(choisis: Set<DayOfWeek>, onChange: (Set<DayOfWeek>) -> Unit) {
    val t = rememberTextes()
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        DayOfWeek.entries.forEach { jour ->
            FilterChip(
                selected = jour in choisis,
                onClick = { onChange(choisis.basculer(jour)) },
                label = { Text(jour.getDisplayName(TextStyle.SHORT, t.locale)) },
            )
        }
    }
}

private fun <T> Set<T>.basculer(element: T): Set<T> = if (element in this) this - element else this + element

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoixHeure(initiale: LocalTime, onChoisi: (LocalTime) -> Unit, onAbandon: () -> Unit) {
    val etat = rememberTimePickerState(initialHour = initiale.hour, initialMinute = initiale.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onAbandon,
        title = { Text(stringResource(R.string.a_quelle_heure)) },
        text = { TimePicker(state = etat) },
        confirmButton = { TextButton(onClick = { onChoisi(LocalTime.of(etat.hour, etat.minute)) }) { Text(stringResource(R.string.valider)) } },
        dismissButton = { TextButton(onClick = onAbandon) { Text(stringResource(R.string.annuler)) } },
    )
}

/**
 * Le jour et le mois d'une date annuelle. Le `DatePicker` demande une annee : on prend
 * une annee bissextile, pour que le 29 fevrier reste choisissable, et on l'ignore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoixDateAnnuelle(initiale: MonthDay, onChoisi: (MonthDay) -> Unit, onAbandon: () -> Unit) {
    val etat = rememberDatePickerState(
        initialSelectedDateMillis = initiale.atYear(ANNEE_BISSEXTILE).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        yearRange = ANNEE_BISSEXTILE..ANNEE_BISSEXTILE,
    )
    DatePickerDialog(
        onDismissRequest = onAbandon,
        confirmButton = {
            TextButton(onClick = {
                etat.selectedDateMillis?.let {
                    val jour = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    onChoisi(MonthDay.from(jour))
                }
            }) { Text(stringResource(R.string.valider)) }
        },
        dismissButton = { TextButton(onClick = onAbandon) { Text(stringResource(R.string.annuler)) } },
    ) {
        DatePicker(state = etat, showModeToggle = false)
    }
}

private const val ANNEE_BISSEXTILE = 2028
