package io.github.ginkoaloe.ricochet.donnees

import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * La sauvegarde et la restauration, sous forme d'une archive zip.
 *
 * Un zip plutot que le seul fichier JSON, parce qu'une sauvegarde sans les
 * illustrations n'en est pas une : on retrouverait ses taches et des cadres vides.
 *
 * **Les chemins d'images sont relatifs dans l'archive** et reecrits a l'import.
 * Un chemin absolu comme `/data/user/0/<paquet>/files/images/x.jpg` ne vaut que
 * pour une installation donnee ; le conserver tel quel rendrait l'archive
 * inutilisable apres une reinstallation.
 */
class Archive(
    private val taches: DepotTaches,
    private val images: DepotImages,
) {

    fun exporter(sortie: OutputStream) {
        val liste = taches.charger()
        ZipOutputStream(sortie).use { zip ->
            val pourArchive = liste.map { tache ->
                tache.enregistrable().copy(image = tache.image?.let { nomDansArchive(it) })
            }
            zip.putNextEntry(ZipEntry(ENTREE_TACHES))
            zip.write(json.encodeToString(pourArchive).toByteArray())
            zip.closeEntry()

            liste.mapNotNull { it.image }
                .map(::File)
                .filter { it.exists() }
                .forEach { fichier ->
                    zip.putNextEntry(ZipEntry(nomDansArchive(fichier.path)))
                    fichier.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
        }
    }

    /**
     * Remplace les taches par celles de l'archive, et y recopie ses images.
     *
     * Les images qu'aucune tache ne reference plus sont ensuite **effacees**. Les
     * garder ne protegeait rien : chaque image importee est recopiee sous un nom neuf,
     * aucune tache ne peut donc plus designer les anciennes. Le filet, c'est la copie
     * de l'etat d'avant que l'appelant depose avant d'importer.
     *
     * @throws IllegalArgumentException si l'archive ne contient pas de liste de taches.
     */
    fun importer(entree: InputStream) {
        var lues: List<TacheEnregistree>? = null
        val recopiees = mutableMapOf<String, String>()

        ZipInputStream(entree).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val nom = entry.name
                when {
                    nom == ENTREE_TACHES ->
                        lues = json.decodeFromString(zip.readBytes().decodeToString())

                    nom.startsWith("$DOSSIER_IMAGES/") && !entry.isDirectory ->
                        // `enregistrer` ne ferme pas le flux : l'archive reste lisible.
                        recopiees[nom] = images.enregistrer(zip)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val enregistrees = lues ?: run {
            // Les images deja recopiees d'une archive refusee ne serviraient a rien.
            recopiees.values.forEach(images::supprimer)
            throw IllegalArgumentException("Archive invalide : elle ne contient pas de $ENTREE_TACHES")
        }
        val importees = enregistrees.map { t -> t.copy(image = t.image?.let { recopiees[it] }).versDomaine() }
        taches.enregistrer(importees)
        images.garderSeulement(importees.mapNotNull { it.image })
    }

    private fun nomDansArchive(chemin: String) = "$DOSSIER_IMAGES/${File(chemin).name}"

    private companion object {
        const val ENTREE_TACHES = "taches.json"
        const val DOSSIER_IMAGES = "images"
        val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    }
}
