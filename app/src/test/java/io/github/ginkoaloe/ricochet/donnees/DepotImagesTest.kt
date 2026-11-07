package io.github.ginkoaloe.ricochet.donnees

import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DepotImagesTest {

    private lateinit var racine: File
    private lateinit var dossier: File
    private lateinit var depot: DepotImages

    @Before
    fun preparer() {
        racine = Files.createTempDirectory("ricochet-images").toFile()
        dossier = File(racine, "images")
        depot = DepotImages(dossier)
    }

    @After
    fun nettoyer() {
        racine.deleteRecursively()
    }

    @Test
    fun `une image enregistree se retrouve a l'identique`() {
        val octets = byteArrayOf(1, 2, 3, 4, 5)

        val chemin = depot.enregistrer(octets.inputStream())

        assertContentEquals(octets, File(chemin).readBytes())
    }

    @Test
    fun `le dossier est cree au besoin`() {
        assertFalse(dossier.exists())

        depot.enregistrer(byteArrayOf(1).inputStream())

        assertTrue(dossier.isDirectory)
    }

    @Test
    fun `deux enregistrements ne s'ecrasent pas`() {
        val premier = depot.enregistrer(byteArrayOf(1).inputStream())
        val second = depot.enregistrer(byteArrayOf(2).inputStream())

        assertNotEquals(premier, second)
        assertContentEquals(byteArrayOf(1), File(premier).readBytes())
        assertContentEquals(byteArrayOf(2), File(second).readBytes())
    }

    @Test
    fun `un fichier neuf est vide et porte un nom unique`() {
        val a = depot.fichierNeuf()
        val b = depot.fichierNeuf()

        assertNotEquals(a.name, b.name)
        assertEquals(dossier.canonicalFile, a.parentFile.canonicalFile)
    }

    // ------------------------------------------------------------- la suppression

    @Test
    fun `supprimer efface l'image`() {
        val chemin = depot.enregistrer(byteArrayOf(1).inputStream())

        depot.supprimer(chemin)

        assertFalse(File(chemin).exists())
    }

    @Test
    fun `supprimer tolere l'absence`() {
        depot.supprimer(null)
        depot.supprimer(File(dossier, "jamais-ecrit.jpg").absolutePath)
    }

    /**
     * La garde qui compte : une donnee corrompue ne doit pas pouvoir nous faire
     * effacer un fichier hors de notre dossier.
     */
    @Test
    fun `supprimer refuse un chemin hors du dossier`() {
        val etranger = File(racine, "precieux.txt").apply { writeText("a garder") }

        depot.supprimer(etranger.absolutePath)

        assertTrue(etranger.exists(), "un fichier hors du dossier doit etre epargne")
    }

    @Test
    fun `supprimer refuse une remontee par chemin relatif`() {
        val etranger = File(racine, "precieux.txt").apply { writeText("a garder") }
        dossier.mkdirs()

        depot.supprimer(File(dossier, "../precieux.txt").path)

        assertTrue(etranger.exists(), "la remontee par .. doit etre neutralisee")
    }

    // ------------------------------------------------------------ le menage

    @Test
    fun `le menage n'efface que les images non gardees`() {
        val gardee = depot.enregistrer(byteArrayOf(1).inputStream())
        val orpheline = depot.enregistrer(byteArrayOf(2).inputStream())

        assertEquals(1, depot.garderSeulement(listOf(gardee)))

        assertTrue(File(gardee).exists())
        assertFalse(File(orpheline).exists())
    }

    @Test
    fun `le menage ne sort pas du dossier`() {
        val etranger = File(racine, "precieux.txt").apply { writeText("a garder") }
        depot.enregistrer(byteArrayOf(1).inputStream())

        depot.garderSeulement(emptyList())

        assertTrue(etranger.exists())
    }

    @Test
    fun `le menage tolere un dossier absent`() {
        assertEquals(0, depot.garderSeulement(emptyList()))
    }
}
