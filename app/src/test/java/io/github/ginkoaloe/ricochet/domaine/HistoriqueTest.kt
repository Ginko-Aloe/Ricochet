package io.github.ginkoaloe.ricochet.domaine

import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

class HistoriqueTest {

    private val creation: Instant = Instant.parse("2026-11-02T12:00:00Z")

    private fun apres(d: Duration): Instant = creation.plus(d.toJavaDuration())

    @Test
    fun `une tache jamais faite n'a pas d'accomplissements`() {
        assertTrue(Tache("Pilule", 8.hours, creee = creation).accomplissements().isEmpty())
    }

    @Test
    fun `le premier accomplissement se mesure depuis la creation`() {
        val tache = Tache("Arroser", 3.days, listOf(apres(5.days)), creee = creation)
        assertEquals(2.days, tache.accomplissements().single().ecart)
    }

    /** Due a J+3, faite a J+3 moins 4 heures. */
    @Test
    fun `une avance donne un ecart negatif`() {
        val tache = Tache("Arroser", 3.days, listOf(apres(3.days - 4.hours)), creee = creation)
        assertEquals((-4).hours, tache.accomplissements().single().ecart)
    }

    /**
     * Le deuxieme se mesure depuis le premier, pas depuis l'echeance d'origine : c'est
     * la regle meme de l'application.
     */
    @Test
    fun `les suivants se mesurent depuis le precedent`() {
        val tache = Tache("Arroser", 3.days, listOf(apres(5.days), apres(8.days)), creee = creation)
        assertEquals(listOf(0.days, 2.days), tache.accomplissements().map { it.ecart })
    }

    @Test
    fun `le plus recent vient en tete, quel que soit l'ordre enregistre`() {
        val tache = Tache("Arroser", 3.days, listOf(apres(8.days), apres(5.days)), creee = creation)
        assertEquals(listOf(apres(8.days), apres(5.days)), tache.accomplissements().map { it.quand })
    }
}
