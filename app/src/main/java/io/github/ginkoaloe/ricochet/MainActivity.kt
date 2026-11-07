package io.github.ginkoaloe.ricochet

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.ginkoaloe.ricochet.domaine.Tache
import io.github.ginkoaloe.ricochet.donnees.historiqueEnCsv
import io.github.ginkoaloe.ricochet.rappels.Rappels
import io.github.ginkoaloe.ricochet.ui.EcranCalendrier
import io.github.ginkoaloe.ricochet.ui.EcranDetail
import io.github.ginkoaloe.ricochet.ui.EcranEdition
import io.github.ginkoaloe.ricochet.ui.EcranHistorique
import io.github.ginkoaloe.ricochet.ui.EcranListe
import io.github.ginkoaloe.ricochet.ui.EtatTaches
import io.github.ginkoaloe.ricochet.ui.theme.RicochetTheme
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Rappels.creerCanal(this)

        setContent {
            RicochetTheme {
                DemanderLesNotifications()
                Application(remember { EtatTaches(applicationContext) })
            }
        }
    }
}

/**
 * Sans cette permission, les rappels seraient silencieux — ce qui viderait
 * l'application de son objet. On la demande donc au premier lancement.
 */
@Composable
private fun DemanderLesNotifications() {
    val contexte = LocalContext.current
    val demande = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* un refus se respecte : tout le reste continue de fonctionner */ }

    LaunchedEffect(Unit) {
        val accordee = ContextCompat.checkSelfPermission(
            contexte,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!accordee) demande.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

/** Vrai une fois que la liste a montre comment l'on accomplit par balayage. */
private const val INDICE_BALAYAGE_VU = "indice_balayage_vu"

// Les ecrans de l'application. Ils ne portent que l'identifiant d'une tache, jamais
// la tache elle-meme : c'est l'etat qui fait foi, et il change sous leurs pieds a
// chaque accomplissement.

@Serializable private data object VersListe

@Serializable private data class VersDetail(val id: String)

/** [id] vaut `null` pour une creation. */
@Serializable private data class VersEdition(val id: String? = null)

@Serializable private data class VersHistorique(val id: String)

@Serializable private data object VersCalendrier

/**
 * La navigation : liste → detail → edition, detail → historique, et liste →
 * calendrier → detail. Navigation Compose fournit la pile, le retour systeme et le
 * retour predictif.
 */
@Composable
private fun Application(etat: EtatTaches) {
    val navigation = rememberNavController()
    val contexte = LocalContext.current

    // Un etat d'interface, pas une donnee : il vit dans les preferences, hors de
    // taches.json, et n'entre donc pas dans l'archive d'export.
    val preferences = remember { contexte.getSharedPreferences("interface", Context.MODE_PRIVATE) }
    var indiceBalayageAMontrer by remember {
        mutableStateOf(!preferences.getBoolean(INDICE_BALAYAGE_VU, false))
    }

    // L'utilisateur choisit lui-meme ou poser son archive, et laquelle relire :
    // aucune permission de stockage n'est alors necessaire, et la sauvegarde ne
    // disparait pas avec l'application comme celle de WaterMe.
    val exporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { destination ->
        if (destination == null) return@rememberLauncherForActivityResult
        val resultat = runCatching {
            contexte.contentResolver.openOutputStream(destination)?.use { etat.exporter(it) }
        }
        contexte.annoncer(resultat, R.string.sauvegarde_enregistree, R.string.echec_sauvegarde)
    }

    val importer = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { source ->
        if (source == null) return@rememberLauncherForActivityResult
        val resultat = runCatching {
            contexte.contentResolver.openInputStream(source)?.use { etat.importer(it) }
        }
        contexte.annoncer(resultat, R.string.sauvegarde_restauree, R.string.archive_illisible)
    }

    // L'historique en CSV, pour le lire ailleurs. L'archive reste la sauvegarde.
    val exporterCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { destination ->
        if (destination == null) return@rememberLauncherForActivityResult
        val resultat = runCatching {
            contexte.contentResolver.openOutputStream(destination)?.use {
                it.write(historiqueEnCsv(etat.taches).toByteArray(Charsets.UTF_8))
            }
        }
        contexte.annoncer(resultat, R.string.historique_exporte, R.string.echec_export)
    }

    NavHost(navController = navigation, startDestination = VersListe) {
        composable<VersListe> {
            EcranListe(
                taches = etat.taches,
                maintenant = Instant.now(),
                onAccomplir = etat::accomplir,
                onAnnuler = etat::annulerDernier,
                onOuvrir = { navigation.navigate(VersDetail(it.id)) },
                onNouvelle = { navigation.navigate(VersEdition()) },
                onExporter = { exporter.launch("ricochet-${LocalDate.now()}.zip") },
                onImporter = { importer.launch(arrayOf("application/zip", "*/*")) },
                onExporterCsv = { exporterCsv.launch("ricochet-historique-${LocalDate.now()}.csv") },
                onCalendrier = { navigation.navigate(VersCalendrier) },
                dernierImport = etat.dernierImport,
                onAnnulerImport = {
                    contexte.annoncer(
                        runCatching { etat.annulerDernierImport() },
                        R.string.import_annule,
                        R.string.echec_annulation,
                    )
                },
                montrerIndiceBalayage = indiceBalayageAMontrer,
                onIndiceBalayageMontre = {
                    preferences.edit().putBoolean(INDICE_BALAYAGE_VU, true).apply()
                    indiceBalayageAMontrer = false
                },
            )
        }

        composable<VersDetail> { entree ->
            AvecTache(etat, entree.toRoute<VersDetail>().id, navigation) { tache ->
                EcranDetail(
                    tache = tache,
                    maintenant = Instant.now(),
                    onAccomplir = etat::accomplir,
                    onAnnuler = etat::annulerDernier,
                    onEditer = { navigation.navigate(VersEdition(tache.id)) },
                    onHistorique = { navigation.navigate(VersHistorique(tache.id)) },
                    onRetour = { navigation.popBackStack() },
                )
            }
        }

        composable<VersEdition> { entree ->
            val id = entree.toRoute<VersEdition>().id
            val tache = id?.let { cherche -> etat.taches.firstOrNull { it.id == cherche } }
            EcranEdition(
                tache = tache,
                // Une creation revient a la liste, une modification au detail :
                // dans les deux cas, d'ou l'on venait.
                onEnregistrer = { etat.enregistrer(it); navigation.popBackStack() },
                // Le detail d'une tache supprimee n'a plus de sens : on saute a la liste.
                onSupprimer = {
                    etat.supprimer(it)
                    navigation.popBackStack(VersListe, inclusive = false)
                },
                onRetour = { navigation.popBackStack() },
            )
        }

        composable<VersHistorique> { entree ->
            AvecTache(etat, entree.toRoute<VersHistorique>().id, navigation) { tache ->
                EcranHistorique(
                    tache = tache,
                    maintenant = Instant.now(),
                    onCorriger = etat::corriger,
                    onRetour = { navigation.popBackStack() },
                )
            }
        }

        composable<VersCalendrier> {
            EcranCalendrier(
                taches = etat.taches,
                maintenant = Instant.now(),
                onOuvrir = { navigation.navigate(VersDetail(it.id)) },
                onRetour = { navigation.popBackStack() },
            )
        }
    }
}

/**
 * Affiche [contenu] pour la tache [id], ou revient a la liste si elle n'existe plus —
 * ce qui arrive apres un import qui l'a fait disparaitre.
 */
@Composable
private fun AvecTache(
    etat: EtatTaches,
    id: String,
    navigation: NavController,
    contenu: @Composable (Tache) -> Unit,
) {
    val tache = etat.taches.firstOrNull { it.id == id }
    if (tache == null) {
        LaunchedEffect(id) { navigation.popBackStack(VersListe, inclusive = false) }
    } else {
        contenu(tache)
    }
}

/** Dit ce qui s'est passe, succes comme echec — un import muet serait inquietant. */
private fun Context.annoncer(resultat: Result<*>, @StringRes succes: Int, @StringRes echec: Int) {
    val message = resultat.fold(
        onSuccess = { getString(succes) },
        onFailure = { getString(R.string.echec_motif, getString(echec), it.message ?: it::class.simpleName) },
    )
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
