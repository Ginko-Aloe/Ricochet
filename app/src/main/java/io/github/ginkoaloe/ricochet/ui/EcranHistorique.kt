package io.github.ginkoaloe.ricochet.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.ginkoaloe.ricochet.R
import io.github.ginkoaloe.ricochet.domaine.Rythme
import io.github.ginkoaloe.ricochet.domaine.Tache
import io.github.ginkoaloe.ricochet.domaine.accomplissements
import io.github.ginkoaloe.ricochet.domaine.etatEcheance
import io.github.ginkoaloe.ricochet.ui.theme.RicochetTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

/** Ce que le choix de date en cours doit produire. */
private sealed interface Saisie {
    data object Ajout : Saisie
    data class Modification(val de: Instant) : Saisie
}

/**
 * L'historique d'une tache, et sa correction : on y vient par un lien discret du
 * detail. Depuis que l'historique s'exporte, Ricochet peut servir a suivre sa
 * regularite — un historique faux n'y est plus anodin.
 *
 * Le galet + rattrape un oubli ; toucher une ligne propose de corriger sa date ou de
 * la retirer. Chaque correction s'applique aussitot, avec *Annuler* : toucher le
 * plus recent deplace l'echeance, et la `SnackBar` le dit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranHistorique(
    tache: Tache,
    maintenant: Instant,
    onCorriger: (Tache) -> Unit,
    onRetour: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = rememberTextes()
    val lignes = remember(tache) { tache.accomplissements() }
    val zone = (tache.rythme as? Rythme.RendezVous)?.zone ?: ZoneId.systemDefault()
    val hote = remember { SnackbarHostState() }
    val portee = rememberCoroutineScope()
    var saisie: Saisie? by remember { mutableStateOf(null) }
    // Par rang plutot que par date : deux accomplissements peuvent tomber a la meme seconde.
    var menuOuvertSur: Int? by remember { mutableStateOf(null) }

    fun appliquer(corrigee: Tache, @StringRes quoi: Int) {
        val avant = tache
        onCorriger(corrigee)
        val message =
            if (corrigee.echeance == avant.echeance) t.texte(quoi)
            else t.texte(
                R.string.message_et_echeance,
                t.texte(quoi),
                corrigee.etatEcheance(maintenant).libelle(t).replaceFirstChar { it.lowercase(t.locale) },
            )
        portee.launch {
            // Annuler ne vaut que pour la derniere correction : la precedente s'efface.
            hote.currentSnackbarData?.dismiss()
            val reponse = hote.showSnackbar(
                message = message,
                actionLabel = t.texte(R.string.annuler),
                duration = SnackbarDuration.Long,
            )
            if (reponse == SnackbarResult.ActionPerformed) onCorriger(avant)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hote) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.historique))
                        Text(
                            text = tache.nom,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onRetour) {
                        Icon(painterResource(R.drawable.ic_retour), stringResource(R.string.retour))
                    }
                },
            )
        },
        floatingActionButton = {
            BoutonGalet(onClick = { saisie = Saisie.Ajout }, description = stringResource(R.string.ajouter_accomplissement))
        },
    ) { interieur ->
        if (lignes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(interieur), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.jamais_faite_pour_l_instant),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(interieur),
                // La marge basse degage la liste du bouton flottant.
                contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
            ) {
                itemsIndexed(lignes) { rang, accompli ->
                    Box {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { menuOuvertSur = rang }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(libelleDate(t, accompli.quand), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = libelleAccomplissement(t, accompli, zone),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        DropdownMenu(
                            expanded = menuOuvertSur == rang,
                            onDismissRequest = { menuOuvertSur = null },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.modifier_la_date)) },
                                onClick = {
                                    menuOuvertSur = null
                                    saisie = Saisie.Modification(accompli.quand)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.supprimer)) },
                                onClick = {
                                    menuOuvertSur = null
                                    appliquer(tache.retirerAccomplissement(accompli.quand), R.string.accomplissement_retire)
                                },
                            )
                        }
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                item {
                    // Le modele ne garde que le rythme actuel : on le dit plutot que
                    // de laisser croire a des ecarts exacts apres un changement.
                    val aide =
                        if (tache.rythme is Rythme.Ricochet) R.string.aide_historique_ricochet
                        else R.string.aide_historique_rendez_vous
                    Text(
                        text = stringResource(
                            aide,
                            libelleRythme(t, tache.rythme).replaceFirstChar { it.lowercase(t.locale) },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }

    saisie?.let { enCours ->
        ChoixDateHeure(
            initial = (enCours as? Saisie.Modification)?.de ?: maintenant,
            maintenant = maintenant,
            onChoisi = { quand ->
                saisie = null
                when (enCours) {
                    Saisie.Ajout ->
                        appliquer(tache.ajouterAccomplissement(quand), R.string.accomplissement_ajoute)
                    is Saisie.Modification ->
                        appliquer(tache.deplacerAccomplissement(enCours.de, quand), R.string.date_corrigee)
                }
            },
            onAbandon = { saisie = null },
        )
    }
}

@Preview(showBackground = true, heightDp = 640)
@Composable
private fun ApercuHistorique() {
    val creation = Instant.parse("2026-10-20T08:00:00Z")
    val historique = listOf(3.days + 2.hours, 6.days, 10.days).map { creation.plus(it.toJavaDuration()) }
    RicochetTheme {
        EcranHistorique(
            tache = Tache("Arroser le ficus", 3.days, historique, creee = creation),
            maintenant = creation.plus(12.days.toJavaDuration()),
            onCorriger = {},
            onRetour = {},
        )
    }
}
