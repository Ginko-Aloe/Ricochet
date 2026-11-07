package io.github.ginkoaloe.ricochet.donnees

import io.github.ginkoaloe.ricochet.domaine.Tache
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Le stockage des taches, dans un unique fichier JSON.
 *
 * Un fichier plutot qu'une base : les donnees se comptent en dizaines de lignes,
 * le format reste relisible a l'oeil, et la sauvegarde consiste a copier un fichier.
 *
 * Deux precautions, parce qu'un rappel perdu est un rappel manque :
 *
 * - **l'ecriture est atomique** — on ecrit a cote puis on deplace, de sorte qu'une
 *   interruption ne laisse jamais un fichier a moitie ecrit ;
 * - **un fichier illisible est mis de cote, pas efface**, et le chargement rend une
 *   liste vide au lieu de faire planter l'application au demarrage.
 */
class DepotTaches(private val fichier: File) {

    /**
     * Les taches enregistrees, ou une liste vide s'il n'y en a pas encore.
     *
     * Si le fichier est illisible ou incoherent, il est renomme en `.corrompu` et
     * une liste vide est rendue : mieux vaut repartir de zero que ne plus demarrer.
     */
    fun charger(): List<Tache> {
        if (!fichier.exists()) return emptyList()
        return try {
            val enregistrees = json.decodeFromString<List<TacheEnregistree>>(fichier.readText())
            val taches = enregistrees.map { it.versDomaine() }

            // Une date de creation reconstituee doit etre fixee sur-le-champ. Sans
            // cela elle serait reinventee a chaque lecture, et l'echeance d'une
            // tache jamais accomplie reculerait d'une periode a chaque ouverture.
            if (enregistrees.any { it.creee == null }) enregistrer(taches)

            taches
        } catch (erreur: Exception) {
            ecarterFichierIllisible()
            emptyList()
        }
    }

    /** Remplace l'integralite du contenu enregistre. */
    fun enregistrer(taches: List<Tache>) {
        fichier.parentFile?.mkdirs()
        val temporaire = File(fichier.parentFile, "${fichier.name}.tmp")
        temporaire.writeText(json.encodeToString(taches.map { it.enregistrable() }))
        Files.move(
            temporaire.toPath(),
            fichier.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
        )
    }

    private fun ecarterFichierIllisible() {
        val ecarte = File(fichier.parentFile, "${fichier.name}.corrompu")
        runCatching {
            Files.move(fichier.toPath(), ecarte.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private companion object {
        /** `prettyPrint` parce que ce fichier est aussi le format d'export. */
        val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }
    }
}
