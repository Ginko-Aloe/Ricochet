package io.github.ginkoaloe.ricochet.donnees

import java.io.File
import java.io.InputStream
import java.util.UUID

/**
 * Les illustrations des taches, dans un sous-dossier prive de l'application.
 *
 * **L'image est recopiee, jamais referencee.** Une photo choisie dans la galerie
 * n'est accessible que le temps d'une permission temporaire, et l'utilisateur peut
 * la supprimer ensuite ; garder son URI reviendrait a afficher des cadres vides au
 * bout de quelques semaines.
 *
 * Les noms sont tires au sort plutot que derives du nom de la tache : renommer une
 * tache ne doit pas egarer son image, et deux taches homonymes doivent coexister.
 */
class DepotImages(val dossier: File) {

    /** Un fichier neuf, vide, ou la camera pourra ecrire. */
    fun fichierNeuf(): File {
        dossier.mkdirs()
        return File(dossier, "${UUID.randomUUID()}.jpg")
    }

    /**
     * Recopie [flux] dans un fichier neuf et rend son chemin absolu.
     *
     * **Le flux n'est pas ferme** : il appartient a l'appelant. C'est indispensable
     * pour lire une entree d'archive sans fermer l'archive entiere.
     */
    fun enregistrer(flux: InputStream): String {
        val destination = fichierNeuf()
        destination.outputStream().use { sortie -> flux.copyTo(sortie) }
        return destination.absolutePath
    }

    /**
     * Supprime une image devenue inutile. Sans effet si [chemin] est `null`, si le
     * fichier n'existe pas, ou **s'il ne se trouve pas dans notre dossier** : cette
     * derniere garde evite qu'une donnee corrompue nous fasse effacer autre chose.
     */
    fun supprimer(chemin: String?) {
        val fichier = File(chemin ?: return)
        if (fichier.parentFile?.canonicalFile != dossier.canonicalFile) return
        fichier.delete()
    }

    /**
     * Efface toutes les images du dossier sauf [gardees], et rend le nombre de
     * fichiers effaces.
     *
     * **A n'appeler qu'apres un import**, quand aucune edition n'est en cours : au
     * demarrage, Android peut avoir tue l'application pendant une prise de vue, et la
     * photo que la camera remplit paraitrait orpheline.
     */
    fun garderSeulement(gardees: Collection<String>): Int {
        val aGarder = gardees.map { File(it).canonicalFile }.toSet()
        val orphelines = dossier.listFiles { f -> f.isFile && f.canonicalFile !in aGarder }.orEmpty()
        return orphelines.count { it.delete() }
    }
}
