package io.github.ginkoaloe.ricochet.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.ginkoaloe.ricochet.R
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import io.github.ginkoaloe.ricochet.domaine.Rythme
import io.github.ginkoaloe.ricochet.domaine.Tache
import io.github.ginkoaloe.ricochet.domaine.ancre
import io.github.ginkoaloe.ricochet.domaine.premiereOccurrence
import io.github.ginkoaloe.ricochet.donnees.DepotImages
import io.github.ginkoaloe.ricochet.ui.theme.RicochetTheme
import java.io.File
import java.time.Instant
import java.time.ZoneId
import kotlin.time.Duration.Companion.days

/**
 * Creation ou modification d'une tache.
 *
 * [tache] vaut `null` pour une creation. L'historique d'une tache existante n'est
 * jamais touche ici : on ne modifie que son nom, son rythme et son image, de sorte
 * qu'une correction de libelle ne decale pas l'echeance. Changer de sorte —
 * Ricochet ou Rendez-vous — garde l'historique, relu selon le nouveau rythme.
 *
 * Les images choisies sont recopiees aussitot dans notre dossier prive. Celles qui
 * ne sont finalement pas retenues — parce qu'on a change d'avis, ou annule — sont
 * effacees : un ecran d'edition ne doit pas laisser de dechets derriere lui.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranEdition(
    tache: Tache?,
    onEnregistrer: (Tache) -> Unit,
    onSupprimer: (Tache) -> Unit,
    onRetour: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contexte = LocalContext.current
    val t = rememberTextes()
    val depotImages = remember { DepotImages(File(contexte.filesDir, "images")) }

    val (nombreInitial, uniteInitiale) = remember(tache) {
        ((tache?.rythme as? Rythme.Ricochet)?.periode ?: 3.days).decomposer()
    }
    var nom by remember(tache) { mutableStateOf(tache?.nom.orEmpty()) }
    var nombre by remember(tache) { mutableStateOf(nombreInitial.toString()) }
    var unite by remember(tache) { mutableStateOf(uniteInitiale) }
    var image by remember(tache) { mutableStateOf(tache?.image) }
    val ancienRendezVous = tache?.rythme as? Rythme.RendezVous
    var rendezVous by remember(tache) { mutableStateOf(ancienRendezVous != null) }
    val brouillonInitial = remember(tache) { ancienRendezVous?.let(BrouillonRendezVous::depuis) }
    var brouillon by remember(tache) { mutableStateOf(brouillonInitial ?: BrouillonRendezVous()) }
    // Un rendez-vous garde son fuseau ; un neuf prend celui du telephone.
    val zone = ancienRendezVous?.zone ?: ZoneId.systemDefault()

    // Les images creees pendant cette edition, pour savoir lesquelles effacer.
    val creees = remember(tache) { mutableListOf<String>() }
    var fichierEnAttente by remember { mutableStateOf<File?>(null) }

    fun adopter(chemin: String) {
        creees += chemin
        image = chemin
    }

    val choisirDansGalerie = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            contexte.contentResolver.openInputStream(uri)?.use { flux ->
                adopter(depotImages.enregistrer(flux))
            }
        }
    }

    val prendreUnePhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { reussi ->
        val fichier = fichierEnAttente
        if (reussi && fichier != null) adopter(fichier.absolutePath) else fichier?.delete()
        fichierEnAttente = null
    }

    fun nettoyer(retenue: String?) {
        creees.filter { it != retenue }.forEach(depotImages::supprimer)
    }

    val nombreValide = nombre.toLongOrNull()?.takeIf { it > 0 }
    val rythmeSaisi: Rythme? =
        if (rendezVous) brouillon.rythme(zone)
        else nombreValide?.let { Rythme.Ricochet(unite.vers(it)) }
    val saisieValide = nom.isNotBlank() && rythmeSaisi != null

    /**
     * Le rythme a enregistrer. Un rendez-vous dont on n'a pas touche la saisie reste
     * **tel quel** : sa reference, pour que « toutes les 2 semaines » ne change pas de
     * semaine en corrigeant un nom, et son calendrier, que la saisie ne sait pas
     * toujours redire (plusieurs dates annuelles, rangs melanges). Un rendez-vous
     * neuf ou modifie prend sa reference a sa premiere occurrence.
     */
    fun rythmeFinal(saisi: Rythme): Rythme = when {
        saisi !is Rythme.RendezVous -> saisi
        ancienRendezVous != null && brouillon == brouillonInitial -> ancienRendezVous
        else -> saisi.ancre(apres = Instant.now())
    }

    val abandonner = {
        nettoyer(retenue = null)
        onRetour()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (tache == null) R.string.nouvelle_tache else R.string.modifier)) },
                navigationIcon = { TextButton(onClick = abandonner) { Text(stringResource(R.string.annuler)) } },
                actions = {
                    TextButton(
                        enabled = saisieValide,
                        onClick = {
                            nettoyer(retenue = image)
                            // L'ancienne image d'une tache modifiee n'a plus d'usage.
                            if (tache?.image != null && tache.image != image) {
                                depotImages.supprimer(tache.image)
                            }
                            val rythme = rythmeFinal(rythmeSaisi!!)
                            onEnregistrer(
                                tache?.copy(nom = nom.trim(), rythme = rythme, image = image)
                                    ?: Tache(nom = nom.trim(), rythme = rythme, image = image)
                            )
                        },
                    ) { Text(stringResource(R.string.enregistrer)) }
                },
            )
        },
    ) { interieur ->
        Column(
            modifier = Modifier
                .padding(interieur)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = nom,
                onValueChange = { nom = it },
                label = { Text(stringResource(R.string.nom_tache)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            ChoixImage(
                chemin = image,
                onGalerie = {
                    choisirDansGalerie.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onPhoto = {
                    val fichier = depotImages.fichierNeuf()
                    fichierEnAttente = fichier
                    prendreUnePhoto.launch(
                        FileProvider.getUriForFile(
                            contexte,
                            contexte.packageName + ".fichiers",
                            fichier,
                        )
                    )
                },
                onRetirer = { image = null },
            )

            ChoixSorte(rendezVous, onChoix = { rendezVous = it }, modifier = Modifier.fillMaxWidth())

            if (!rendezVous) {
                Text(stringResource(R.string.a_refaire_tous_les), style = MaterialTheme.typography.labelLarge)

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { saisi -> nombre = saisi.filter { it.isDigit() }.take(4) },
                    label = { Text(stringResource(R.string.nombre)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(120.dp),
                )

                ChoixUnite(unite, onChoix = { unite = it }, modifier = Modifier.fillMaxWidth())

                Text(
                    text = stringResource(R.string.explication_ricochet),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                SaisieRendezVous(brouillon, onChange = { brouillon = it })

                val prochain = (rythmeSaisi as? Rythme.RendezVous)?.let { r ->
                    val maintenant = Instant.now()
                    val premiere = rythmeFinal(r).let { it as Rythme.RendezVous }.premiereOccurrence(maintenant)
                    t.texte(R.string.prochain, dateCourte(t, premiere, zone, avecJourDeSemaine = true), libelleHeure(t, r.heure))
                } ?: stringResource(R.string.choisir_un_jour)
                Text(prochain, style = MaterialTheme.typography.bodyLarge)

                Text(
                    text = stringResource(R.string.explication_rendez_vous),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (tache != null) {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        nettoyer(retenue = null)
                        depotImages.supprimer(tache.image)
                        onSupprimer(tache)
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.supprimer_tache)) }
            }
        }
    }
}

/** L'illustration : son apercu, et les deux facons de la choisir. */
@Composable
private fun ChoixImage(
    chemin: String?,
    onGalerie: () -> Unit,
    onPhoto: () -> Unit,
    onRetirer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val apercu by imageDeTache(chemin, largeurCible = 1080)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            val photo = apercu
            if (photo != null) {
                Image(
                    bitmap = photo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            } else {
                Text(
                    text = stringResource(R.string.aucune_illustration),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onGalerie, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.galerie), maxLines = 1)
            }
            OutlinedButton(onClick = onPhoto, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.photo), maxLines = 1)
            }
            if (chemin != null) {
                OutlinedButton(onClick = onRetirer, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.retirer), maxLines = 1)
                }
            }
        }
    }
}

/**
 * Les deux sortes de rappel. Un Ricochet repart de chaque accomplissement ; un
 * Rendez-vous suit le calendrier.
 */
@Composable
private fun ChoixSorte(rendezVous: Boolean, onChoix: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(false to R.string.sorte_ricochet, true to R.string.sorte_rendez_vous).forEach { (sorte, id) ->
            val libelle = stringResource(id)
            if (sorte == rendezVous) {
                Button(onClick = { onChoix(sorte) }, modifier = Modifier.weight(1f)) { Text(libelle, maxLines = 1) }
            } else {
                OutlinedButton(onClick = { onChoix(sorte) }, modifier = Modifier.weight(1f)) { Text(libelle, maxLines = 1) }
            }
        }
    }
}

/**
 * Le choix d'unite, en boutons plutot qu'en menu deroulant : trois valeurs se
 * montrent, elles ne se cherchent pas.
 */
@Composable
private fun ChoixUnite(
    choisie: UnitePeriode,
    onChoix: (UnitePeriode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        UnitePeriode.entries.forEach { unite ->
            if (unite == choisie) {
                Button(onClick = { onChoix(unite) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(unite.libelle), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            } else {
                OutlinedButton(onClick = { onChoix(unite) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(unite.libelle), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 760)
@Composable
private fun ApercuCreation() {
    RicochetTheme { EcranEdition(null, {}, {}, {}) }
}

@Preview(showBackground = true, heightDp = 760)
@Composable
private fun ApercuModification() {
    RicochetTheme {
        EcranEdition(Tache("Arroser le ficus", 3.days), {}, {}, {})
    }
}
