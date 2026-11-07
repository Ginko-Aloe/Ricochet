package io.github.ginkoaloe.ricochet.ui

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ImageTacheTest {

    @Test
    fun `une image deja assez petite n'est pas reduite`() {
        assertEquals(1, echantillonnage(largeurSource = 800, largeurCible = 1080))
        assertEquals(1, echantillonnage(largeurSource = 1080, largeurCible = 1080))
    }

    @Test
    fun `le facteur est la plus grande puissance de deux qui garde la cible`() {
        // 4000 / 4 = 1000, encore au-dessus de 1000 ; 4000 / 8 = 500, trop petit.
        assertEquals(4, echantillonnage(largeurSource = 4000, largeurCible = 1000))
        assertEquals(2, echantillonnage(largeurSource = 2160, largeurCible = 1080))
        assertEquals(8, echantillonnage(largeurSource = 9000, largeurCible = 1000))
    }

    @Test
    fun `le facteur reste une puissance de deux`() {
        listOf(3000, 4032, 5000, 12000).forEach { largeur ->
            val facteur = echantillonnage(largeur, 1080)
            assertEquals(
                0,
                facteur and (facteur - 1),
                "$facteur n'est pas une puissance de deux pour une source de $largeur",
            )
        }
    }

    @Test
    fun `la reduction laisse toujours au moins la largeur cible`() {
        listOf(3000, 4032, 5000, 12000, 1081).forEach { largeur ->
            val reduite = largeur / echantillonnage(largeur, 1080)
            assertEquals(
                true,
                reduite >= 1080,
                "une source de $largeur tombe a $reduite, sous la cible",
            )
        }
    }

    @Test
    fun `des mesures absurdes ne font pas diviser par zero`() {
        assertEquals(1, echantillonnage(largeurSource = 0, largeurCible = 1080))
        assertEquals(1, echantillonnage(largeurSource = -1, largeurCible = 1080))
        assertEquals(1, echantillonnage(largeurSource = 4000, largeurCible = 0))
    }

    // ------------------------------------------------------------ l'orientation EXIF
    // Valeurs de la norme EXIF : 1 normale, 2 a 8 les sept autres sens.

    @Test
    fun `une orientation normale ou inconnue ne change rien`() {
        assertNull(redressement(1))
        assertNull(redressement(0))
    }

    /** Le cas du Pixel : portrait pris capteur couche, balise 6. */
    @Test
    fun `l'orientation 6 se redresse d'un quart de tour horaire`() {
        assertEquals(Redressement(90f, miroir = false), redressement(6))
    }

    @Test
    fun `les rotations simples`() {
        assertEquals(Redressement(180f, miroir = false), redressement(3))
        assertEquals(Redressement(270f, miroir = false), redressement(8))
    }

    @Test
    fun `les orientations en miroir`() {
        assertEquals(Redressement(0f, miroir = true), redressement(2))
        assertEquals(Redressement(180f, miroir = true), redressement(4))
        assertEquals(Redressement(90f, miroir = true), redressement(5))
        assertEquals(Redressement(270f, miroir = true), redressement(7))
    }
}
