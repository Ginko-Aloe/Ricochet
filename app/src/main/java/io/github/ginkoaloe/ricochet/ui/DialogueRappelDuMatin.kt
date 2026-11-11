package io.github.ginkoaloe.ricochet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.ginkoaloe.ricochet.R
import java.time.LocalTime

/**
 * Regler le rappel du matin : l'activer, et choisir son heure.
 *
 * Rien n'est enregistre avant *Valider* : on peut essayer une heure, puis renoncer.
 */
@Composable
fun DialogueRappelDuMatin(
    actif: Boolean,
    heure: LocalTime,
    onValider: (actif: Boolean, heure: LocalTime) -> Unit,
    onAbandon: () -> Unit,
) {
    val t = rememberTextes()
    var actifChoisi by remember { mutableStateOf(actif) }
    var heureChoisie by remember { mutableStateOf(heure) }
    var choixHeure by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onAbandon,
        title = { Text(stringResource(R.string.rappel_du_matin)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.rappel_du_matin_explication),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = actifChoisi, role = Role.Switch, onValueChange = { actifChoisi = it }),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(R.string.activer), style = MaterialTheme.typography.bodyLarge)
                    // La rangee entiere bascule : l'interrupteur ne fait que montrer l'etat.
                    Switch(checked = actifChoisi, onCheckedChange = null)
                }
                OutlinedButton(onClick = { choixHeure = true }, enabled = actifChoisi) {
                    Text(t.texte(R.string.a_heure, libelleHeure(t, heureChoisie)))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onValider(actifChoisi, heureChoisie) }) { Text(stringResource(R.string.valider)) }
        },
        dismissButton = { TextButton(onClick = onAbandon) { Text(stringResource(R.string.annuler)) } },
    )

    if (choixHeure) {
        ChoixHeure(
            initiale = heureChoisie,
            onChoisi = { heureChoisie = it; choixHeure = false },
            onAbandon = { choixHeure = false },
        )
    }
}
