package io.github.ginkoaloe.ricochet.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.ginkoaloe.ricochet.R
import io.github.ginkoaloe.ricochet.domaine.Tache
import io.github.ginkoaloe.ricochet.domaine.etatEcheance
import io.github.ginkoaloe.ricochet.domaine.parUrgence
import io.github.ginkoaloe.ricochet.ui.theme.RicochetTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.math.roundToInt
import kotlin.time.toJavaDuration

/**
 * La liste des taches, de la plus urgente a la moins urgente.
 *
 * [maintenant] est passe plutot que lu ici : la vue reste ainsi previsible, et c'est
 * l'appelant qui decide a quel rythme le temps avance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranListe(
    taches: List<Tache>,
    maintenant: Instant,
    onAccomplir: (Tache) -> Unit,
    onAnnuler: (Tache) -> Unit,
    onOuvrir: (Tache) -> Unit,
    onNouvelle: () -> Unit,
    onExporter: () -> Unit,
    onImporter: () -> Unit,
    modifier: Modifier = Modifier,
    onExporterCsv: () -> Unit = {},
    onCalendrier: () -> Unit = {},
    onRappelDuMatin: () -> Unit = {},
    dernierImport: Instant? = null,
    onAnnulerImport: () -> Unit = {},
    montrerIndiceBalayage: Boolean = false,
    onIndiceBalayageMontre: () -> Unit = {},
) {
    val t = rememberTextes()
    val hote = remember { SnackbarHostState() }
    val portee = rememberCoroutineScope()
    var menuOuvert by remember { mutableStateOf(false) }
    var confirmerAnnulationImport by remember { mutableStateOf(false) }

    // Revenir avant l'import ecrase ce qui a ete fait depuis : on le dit, et on demande.
    if (confirmerAnnulationImport && dernierImport != null) {
        AlertDialog(
            onDismissRequest = { confirmerAnnulationImport = false },
            title = { Text(stringResource(R.string.annuler_import_titre)) },
            text = {
                Text(
                    stringResource(
                        R.string.annuler_import_texte,
                        libelleDate(t, dernierImport).replaceFirstChar { it.lowercase(t.locale) },
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmerAnnulationImport = false; onAnnulerImport() }) {
                    Text(stringResource(R.string.revenir_en_arriere))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmerAnnulationImport = false }) { Text(stringResource(R.string.garder)) }
            },
        )
    }

    /**
     * Accomplir, puis offrir de revenir en arriere. L'annulation n'est pas un
     * confort : une tape malencontreuse ne salit pas seulement l'historique, elle
     * decale l'echeance d'une periode entiere.
     */
    fun accomplirEtOffrirAnnulation(tache: Tache) {
        onAccomplir(tache)
        portee.launch {
            val reponse = hote.showSnackbar(
                message = t.texte(R.string.tache_accomplie, tache.nom),
                actionLabel = t.texte(R.string.annuler),
                duration = SnackbarDuration.Long,
            )
            if (reponse == SnackbarResult.ActionPerformed) onAnnuler(tache)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hote) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onCalendrier) {
                        Icon(painterResource(R.drawable.ic_calendrier), contentDescription = stringResource(R.string.calendrier))
                    }
                    TextButton(onClick = { menuOuvert = true }) { Text(stringResource(R.string.donnees)) }
                    DropdownMenu(
                        expanded = menuOuvert,
                        onDismissRequest = { menuOuvert = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.exporter)) },
                            onClick = { menuOuvert = false; onExporter() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.importer)) },
                            onClick = { menuOuvert = false; onImporter() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.exporter_csv)) },
                            onClick = { menuOuvert = false; onExporterCsv() },
                        )
                        // Seulement s'il reste un import a annuler.
                        if (dernierImport != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.annuler_import)) },
                                onClick = { menuOuvert = false; confirmerAnnulationImport = true },
                            )
                        }
                        // Un reglage plutot qu'une donnee, mais un ecran de reglages
                        // pour lui seul serait bien vide : il se range a la fin du menu.
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.rappel_du_matin_menu)) },
                            onClick = { menuOuvert = false; onRappelDuMatin() },
                        )
                    }
                },
            )
        },
        floatingActionButton = { BoutonGalet(onClick = onNouvelle, description = stringResource(R.string.nouvelle_tache)) },
    ) { interieur ->
        if (taches.isEmpty()) {
            ListeVide(Modifier.padding(interieur))
        } else {
            LazyColumn(
                modifier = Modifier.padding(interieur),
                // La marge basse degage la liste du bouton flottant.
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(taches.parUrgence(maintenant), key = { _, tache -> tache.id }) { rang, tache ->
                    LigneBalayable(
                        tache = tache,
                        maintenant = maintenant,
                        onAccomplir = { accomplirEtOffrirAnnulation(tache) },
                        onOuvrir = { onOuvrir(tache) },
                        montrerIndice = montrerIndiceBalayage && rang == 0,
                        onIndiceMontre = onIndiceBalayageMontre,
                    )
                }
            }
        }
    }
}

@Composable
private fun ListeVide(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.liste_vide),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * La carte, et le geste de balayage qui l'accomplit.
 *
 * Le balayage **ne fait pas disparaitre la carte** : la tache n'est pas terminee,
 * elle vient seulement de ricocher. On remet donc la carte en place aussitot le
 * geste pris en compte. C'est ce que fait WaterMe, et c'est le bon reflexe.
 */
@Composable
private fun LigneBalayable(
    tache: Tache,
    maintenant: Instant,
    onAccomplir: () -> Unit,
    onOuvrir: () -> Unit,
    modifier: Modifier = Modifier,
    montrerIndice: Boolean = false,
    onIndiceMontre: () -> Unit = {},
) {
    // Le seuil par defaut, 56 dp, se franchit en faisant defiler la liste : un pouce
    // qui remonte en biais traverse la carte sans le vouloir. Il faut ici un geste
    // franc, sur plus d'un tiers de la largeur.
    val balayage = rememberSwipeToDismissBoxState(positionalThreshold = { largeur -> largeur * SEUIL_BALAYAGE })
    // Une portee qui survit aux changements d'etat du balayage : attachee a
    // celui-ci, l'animation de remise en place serait annulee en plein vol.
    val portee = rememberCoroutineScope()
    val accomplir by rememberUpdatedState(onAccomplir)

    // Cette lambda doit garder la meme identite d'une recomposition a l'autre.
    // `SwipeToDismissBox` indexe son `LaunchedEffect` sur `onDismiss` autant que
    // sur l'etat : une lambda recreee a chaque passage relance l'effet, qui
    // rappelle `onDismiss` tant que la boite n'est pas revenue en place — et un
    // seul balayage enregistre alors des dizaines d'accomplissements.
    val surBalayage = remember(balayage, portee) {
        { _: SwipeToDismissBoxValue ->
            accomplir()
            portee.launch { balayage.reset() }
            Unit
        }
    }

    // Sans bouton « Fait » sur la carte, le balayage est le seul geste pour
    // accomplir depuis la liste, et il est invisible. On le montre donc une fois :
    // la carte s'entrouvre sur ce qu'elle cache, puis revient en place.
    val decalage = remember { Animatable(0f) }
    val indiceFini by rememberUpdatedState(onIndiceMontre)
    val ouvertureIndice = with(LocalDensity.current) { 140.dp.toPx() }
    LaunchedEffect(montrerIndice) {
        if (!montrerIndice) return@LaunchedEffect
        delay(700)
        decalage.animateTo(ouvertureIndice, tween(durationMillis = 450))
        delay(600)
        decalage.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        indiceFini()
    }

    SwipeToDismissBox(
        state = balayage,
        modifier = modifier,
        backgroundContent = { FondDeBalayage() },
        // Vers la droite seulement : c'est le sens que montrent l'indice et le fond.
        // Le pouce droit qui fait defiler la liste derive, lui, vers la gauche — un
        // balayage dans ce sens accomplissait la tache sous le doigt.
        enableDismissFromEndToStart = false,
        onDismiss = surBalayage,
    ) {
        CarteTache(
            tache = tache,
            maintenant = maintenant,
            onOuvrir = onOuvrir,
            modifier = Modifier.offset { IntOffset(decalage.value.roundToInt(), 0) },
        )
    }
}

/** La part de la largeur de la carte qu'un balayage lent doit parcourir pour accomplir. */
private const val SEUIL_BALAYAGE = 0.4f

/** Ce qu'on decouvre sous la carte pendant le geste : ce qui va se passer. */
@Composable
private fun FondDeBalayage() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = stringResource(R.string.marquer_fait),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun CarteTache(
    tache: Tache,
    maintenant: Instant,
    onOuvrir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val etat = tache.etatEcheance(maintenant)
    val image by imageDeTache(tache.image)

    Card(modifier.clickable(onClick = onOuvrir)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(96.dp)
        ) {
            image?.let { photo ->
                Image(
                    bitmap = photo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            }

            // Le voile, repris de WaterMe : discret quand il y a une photo a
            // montrer, opaque quand il n'y en a pas. La meme carte reste lisible
            // dans les deux cas, sans mise en page separee.
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant
                            .copy(alpha = if (image != null) 0.62f else 1f)
                    )
            )

            Row(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Le galet de l'icone porte l'urgence, comme la goutte de WaterMe :
                // neutre a venir, ocre quand c'est du. Seul signal de couleur de la liste.
                Icon(
                    painter = painterResource(R.drawable.ic_galet),
                    contentDescription = if (etat.estPressant) stringResource(R.string.en_retard) else null,
                    tint = if (etat.estPressant) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(14.dp))
                Box(
                    Modifier
                        .width(1.dp)
                        .height(44.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = tache.nom,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = tache.libelleEcheance(rememberTextes(), maintenant),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (etat.estPressant) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_chevron),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// --------------------------------------------------------------------- apercus

private fun tachesExemple(maintenant: Instant): List<Tache> = listOf(
    Tache("Arroser le ficus", 3.days, listOf(maintenant.minus(5.days.toJavaDuration()))),
    Tache("Pilule", 8.hours, listOf(maintenant.minus(2.hours.toJavaDuration()))),
    Tache("Appeler maman", 14.days, creee = maintenant),
)

@Preview(showBackground = true, heightDp = 640)
@Composable
private fun ApercuEcranListe() {
    val maintenant = Instant.parse("2026-11-02T12:00:00Z")
    RicochetTheme {
        EcranListe(tachesExemple(maintenant), maintenant, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ApercuListeVide() {
    RicochetTheme {
        EcranListe(emptyList(), Instant.parse("2026-11-02T12:00:00Z"), {}, {}, {}, {}, {}, {})
    }
}
