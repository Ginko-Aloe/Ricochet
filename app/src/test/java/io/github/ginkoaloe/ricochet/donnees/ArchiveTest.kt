package io.github.ginkoaloe.ricochet.donnees

import io.github.ginkoaloe.ricochet.domaine.Rythme
import io.github.ginkoaloe.ricochet.domaine.Tache
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.time.Instant
import java.util.zip.ZipInputStream
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class ArchiveTest {

    private lateinit var racine: File
    private lateinit var depart: Contexte
    private lateinit var arrivee: Contexte

    private val lundiMidi: Instant = Instant.parse("2026-11-02T12:00:00Z")
    private val photo = byteArrayOf(9, 8, 7, 6, 5)

    /** Deux installations distinctes, pour eprouver le transport d'une archive. */
    private class Contexte(racine: File, nom: String) {
        val dossier = File(racine, nom).apply { mkdirs() }
        val taches = DepotTaches(File(dossier, "taches.json"))
        val images = DepotImages(File(dossier, "images"))
        val archive = Archive(taches, images)
    }

    @Before
    fun preparer() {
        racine = Files.createTempDirectory("ricochet-archive").toFile()
        depart = Contexte(racine, "depart")
        arrivee = Contexte(racine, "arrivee")
    }

    @After
    fun nettoyer() {
        racine.deleteRecursively()
    }

    private fun exporterDepart(): ByteArray =
        ByteArrayOutputStream().also { depart.archive.exporter(it) }.toByteArray()

    // ------------------------------------------------------------ l'aller-retour

    @Test
    fun `les taches traversent l'archive`() {
        depart.taches.enregistrer(
            listOf(
                Tache("Arroser", 3.days, historique = listOf(lundiMidi), creee = lundiMidi),
                Tache("Pilule", 8.hours, creee = lundiMidi),
            )
        )

        arrivee.archive.importer(ByteArrayInputStream(exporterDepart()))

        val restaurees = arrivee.taches.charger().sortedBy { it.nom }
        assertEquals(listOf("Arroser", "Pilule"), restaurees.map { it.nom })
        assertEquals(Rythme.Ricochet(3.days), restaurees[0].rythme)
        assertEquals(listOf(lundiMidi), restaurees[0].historique)
        assertEquals(lundiMidi, restaurees[1].creee)
        // Ce qui compte vraiment : l'echeance survit au transport.
        assertEquals(lundiMidi.plusSeconds(3 * 86400), restaurees[0].echeance)
    }

    @Test
    fun `l'image suit, et son chemin est reecrit pour l'installation d'arrivee`() {
        val chemin = depart.images.enregistrer(photo.inputStream())
        depart.taches.enregistrer(listOf(Tache("Arroser", 3.days, image = chemin)))

        arrivee.archive.importer(ByteArrayInputStream(exporterDepart()))

        val restauree = arrivee.taches.charger().single()
        val cheminRestaure = requireNotNull(restauree.image)
        assertNotEquals(chemin, cheminRestaure, "le chemin d'origine ne vaut que pour le depart")
        assertEquals(arrivee.images.dossier.canonicalFile, File(cheminRestaure).parentFile.canonicalFile)
        assertContentEquals(photo, File(cheminRestaure).readBytes())
    }

    @Test
    fun `une tache sans image reste sans image`() {
        depart.taches.enregistrer(listOf(Tache("Appeler maman", 7.days)))

        arrivee.archive.importer(ByteArrayInputStream(exporterDepart()))

        assertNull(arrivee.taches.charger().single().image)
    }

    @Test
    fun `plusieurs images ne se melangent pas`() {
        val a = depart.images.enregistrer(byteArrayOf(1, 1, 1).inputStream())
        val b = depart.images.enregistrer(byteArrayOf(2, 2, 2).inputStream())
        depart.taches.enregistrer(
            listOf(Tache("A", 1.days, image = a), Tache("B", 1.days, image = b))
        )

        arrivee.archive.importer(ByteArrayInputStream(exporterDepart()))

        val restaurees = arrivee.taches.charger().associateBy { it.nom }
        assertContentEquals(byteArrayOf(1, 1, 1), File(restaurees.getValue("A").image!!).readBytes())
        assertContentEquals(byteArrayOf(2, 2, 2), File(restaurees.getValue("B").image!!).readBytes())
    }

    // ----------------------------------------------------------- la forme et les gardes

    @Test
    fun `l'archive ne contient que des chemins relatifs`() {
        val chemin = depart.images.enregistrer(photo.inputStream())
        depart.taches.enregistrer(listOf(Tache("Arroser", 3.days, image = chemin)))

        val entrees = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(exporterDepart())).use { zip ->
            var e = zip.nextEntry
            while (e != null) { entrees += e.name; zip.closeEntry(); e = zip.nextEntry }
        }

        assertTrue(entrees.contains("taches.json"), "entrees : $entrees")
        assertTrue(entrees.any { it.startsWith("images/") }, "entrees : $entrees")
        assertTrue(entrees.none { it.startsWith("/") }, "aucun chemin absolu : $entrees")
    }

    @Test
    fun `importer remplace les taches en place, et n'ajoute pas`() {
        arrivee.taches.enregistrer(listOf(Tache("Ancienne", 5.days)))
        depart.taches.enregistrer(listOf(Tache("Nouvelle", 3.days)))

        arrivee.archive.importer(ByteArrayInputStream(exporterDepart()))

        assertEquals(listOf("Nouvelle"), arrivee.taches.charger().map { it.nom })
    }

    @Test
    fun `une archive sans liste de taches est refusee`() {
        val vide = ByteArrayOutputStream().also { java.util.zip.ZipOutputStream(it).close() }

        assertFailsWith<IllegalArgumentException> {
            arrivee.archive.importer(ByteArrayInputStream(vide.toByteArray()))
        }
    }

    @Test
    fun `une archive vide ne detruit pas les taches en place`() {
        arrivee.taches.enregistrer(listOf(Tache("A garder", 5.days)))
        val vide = ByteArrayOutputStream().also { java.util.zip.ZipOutputStream(it).close() }

        runCatching { arrivee.archive.importer(ByteArrayInputStream(vide.toByteArray())) }

        assertEquals(listOf("A garder"), arrivee.taches.charger().map { it.nom })
    }

    // ------------------------------------------------------------ le menage

    @Test
    fun `importer efface les images des taches remplacees, et garde les importees`() {
        val ancienne = arrivee.images.enregistrer(byteArrayOf(4, 4).inputStream())
        arrivee.taches.enregistrer(listOf(Tache("Ancienne", 5.days, image = ancienne)))
        depart.taches.enregistrer(
            listOf(Tache("Nouvelle", 3.days, image = depart.images.enregistrer(photo.inputStream())))
        )

        arrivee.archive.importer(ByteArrayInputStream(exporterDepart()))

        assertTrue(!File(ancienne).exists(), "l'ancienne image n'est plus referencee")
        val importee = File(arrivee.taches.charger().single().image!!)
        assertEquals(listOf(importee.canonicalFile), arrivee.images.dossier.listFiles()!!.map { it.canonicalFile })
    }

    /** Ses images avaient deja ete recopiees avant que l'absence de taches ne se voie. */
    @Test
    fun `une archive refusee ne laisse pas d'images derriere elle`() {
        val enPlace = arrivee.images.enregistrer(photo.inputStream())
        arrivee.taches.enregistrer(listOf(Tache("A garder", 5.days, image = enPlace)))
        val sansTaches = ByteArrayOutputStream().also { sortie ->
            java.util.zip.ZipOutputStream(sortie).use { zip ->
                zip.putNextEntry(java.util.zip.ZipEntry("images/x.jpg"))
                zip.write(byteArrayOf(1, 2, 3))
                zip.closeEntry()
            }
        }

        runCatching { arrivee.archive.importer(ByteArrayInputStream(sansTaches.toByteArray())) }

        assertEquals(listOf(File(enPlace).canonicalFile), arrivee.images.dossier.listFiles()!!.map { it.canonicalFile })
    }

    @Test
    fun `exporter puis reimporter chez soi ne change rien`() {
        val chemin = depart.images.enregistrer(photo.inputStream())
        depart.taches.enregistrer(
            listOf(Tache("Arroser", 3.days, historique = listOf(lundiMidi), image = chemin))
        )
        val avant = depart.taches.charger().single()

        depart.archive.importer(ByteArrayInputStream(exporterDepart()))

        val apres = depart.taches.charger().single()
        assertEquals(avant.id, apres.id)
        assertEquals(avant.echeance, apres.echeance)
        assertContentEquals(photo, File(apres.image!!).readBytes())
    }
}
