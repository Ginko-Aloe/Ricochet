package io.github.ginkoaloe.ricochet.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.ginkoaloe.ricochet.domaine.Tache
import io.github.ginkoaloe.ricochet.donnees.Archive
import io.github.ginkoaloe.ricochet.donnees.depotImages
import io.github.ginkoaloe.ricochet.donnees.depotTaches
import io.github.ginkoaloe.ricochet.rappels.Planificateur
import io.github.ginkoaloe.ricochet.rappels.Rappels
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant

/**
 * Les taches de l'application, et les gestes qui les modifient.
 *
 * **Chaque modification est ecrite sur le disque immediatement**, et les alarmes
 * sont reposees dans la foulee. Les donnees sont minuscules et l'ecriture est
 * atomique ; rien ne justifie de differer, alors qu'un rappel perdu parce que
 * l'application a ete tuee serait une faute.
 *
 * Le disque fait foi : si l'activite est recreee, l'etat se reconstruit en relisant
 * le depot.
 */
class EtatTaches(private val contexte: Context) {

    private val depot = depotTaches(contexte)
    private val planificateur = Planificateur(contexte)
    private val archive = Archive(depot, depotImages(contexte))

    var taches: List<Tache> by mutableStateOf(depot.charger())
        private set

    init {
        planificateur.replanifier(taches)
    }

    /** Marque la tache comme faite maintenant, ce qui fait ricocher son echeance. */
    fun accomplir(tache: Tache) {
        // Si une notification attendait ce geste, elle n'a plus lieu d'etre.
        Rappels.retirer(contexte, tache.id)
        remplacer(tache.accomplir(Instant.now()))
    }

    /**
     * Revient sur le dernier accomplissement, et donc sur l'echeance.
     *
     * On repart de la tache **telle qu'elle est maintenant**, et non de
     * l'exemplaire recu : celui que detient l'appelant date d'avant
     * l'accomplissement qu'il veut defaire, et annuler dessus retirerait le
     * precedent.
     */
    fun annulerDernier(tache: Tache) {
        val courante = taches.firstOrNull { it.id == tache.id } ?: return
        remplacer(courante.annulerDernier())
    }

    /** Cree la tache si elle est nouvelle, la remplace sinon. */
    fun enregistrer(tache: Tache) {
        val connue = taches.any { it.id == tache.id }
        appliquer(if (connue) taches.map { if (it.id == tache.id) tache else it } else taches + tache)
    }

    /**
     * Remplace une tache dont l'historique a ete corrige. Si la correction l'a rendue
     * non due, sa notification n'a plus lieu d'etre. Si elle l'est encore, on la
     * laisse : aucune alarme n'est reposee pour une echeance passee, et la retirer
     * ferait taire le rappel pour de bon.
     */
    fun corriger(tache: Tache) {
        if (!tache.estDue(Instant.now())) Rappels.retirer(contexte, tache.id)
        remplacer(tache)
    }

    fun supprimer(tache: Tache) {
        Rappels.retirer(contexte, tache.id)
        planificateur.annuler(tache.id)
        appliquer(taches.filterNot { it.id == tache.id })
    }

    // ------------------------------------------- sauvegarde et restauration

    fun exporter(sortie: OutputStream) = archive.exporter(sortie)

    private val copieAvantImport = File(contexte.filesDir, "avant-import.ricochet")

    /** Quand a eu lieu l'import qu'on peut encore annuler, ou `null` s'il n'y en a pas. */
    var dernierImport: Instant? by mutableStateOf(copieAvantImport.dateSiPresente())
        private set

    /**
     * Remplace les taches par celles d'une archive.
     *
     * Une copie de l'etat courant est deposee dans l'espace prive avant toute
     * chose : un import decevant ne doit pas etre sans retour. Elle ne remplace la
     * copie precedente qu'une fois l'import reussi — une archive refusee n'a rien
     * change, il n'y a rien a annuler.
     */
    fun importer(entree: InputStream) {
        val provisoire = File(contexte.filesDir, "avant-import.ricochet.tmp")
        provisoire.outputStream().use { archive.exporter(it) }
        try {
            archive.importer(entree)
        } catch (e: Exception) {
            provisoire.delete()
            throw e
        }
        provisoire.renameTo(copieAvantImport)
        dernierImport = copieAvantImport.dateSiPresente()
        recharger()
    }

    /**
     * Revient a l'etat d'avant le dernier import, images comprises. Ce qui a ete
     * fait depuis est perdu : l'appelant le fait confirmer. La copie est consommee,
     * on ne peut pas annuler deux fois.
     */
    fun annulerDernierImport() {
        if (!copieAvantImport.exists()) return
        copieAvantImport.inputStream().use { archive.importer(it) }
        copieAvantImport.delete()
        dernierImport = null
        recharger()
    }

    private fun recharger() {
        taches = depot.charger()
        planificateur.replanifier(taches)
    }

    private fun remplacer(modifiee: Tache) =
        appliquer(taches.map { if (it.id == modifiee.id) modifiee else it })

    private fun appliquer(nouvelles: List<Tache>) {
        taches = nouvelles
        depot.enregistrer(nouvelles)
        planificateur.replanifier(nouvelles)
    }
}

private fun File.dateSiPresente(): Instant? =
    if (exists()) Instant.ofEpochMilli(lastModified()) else null
