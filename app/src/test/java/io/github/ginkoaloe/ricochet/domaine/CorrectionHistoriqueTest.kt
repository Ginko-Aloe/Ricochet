package io.github.ginkoaloe.ricochet.domaine

import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

class CorrectionHistoriqueTest {

    private val creation: Instant = Instant.parse("2026-11-02T12:00:00Z")

    private fun apres(d: Duration): Instant = creation.plus(d.toJavaDuration())

    private val arroser = Tache("Arroser", 3.days, listOf(apres(3.days)), creee = creation)

    // ---------------------------------------------------------------- ajouter

    @Test
    fun `un oubli rattrape s'insere dans l'historique`() {
        val corrigee = arroser.ajouterAccomplissement(apres(1.days))
        assertEquals(listOf(apres(1.days), apres(3.days)), corrigee.historique)
    }

    /** Ajouter plus recent que le dernier, c'est faire ricocher l'echeance. */
    @Test
    fun `un ajout plus recent que le dernier deplace l'echeance`() {
        val corrigee = arroser.ajouterAccomplissement(apres(4.days))
        assertEquals(apres(7.days), corrigee.echeance)
    }

    @Test
    fun `un ajout anterieur a la creation fait reculer la creation`() {
        val corrigee = arroser.ajouterAccomplissement(creation.minus(10.days.toJavaDuration()))
        assertEquals(creation.minus(10.days.toJavaDuration()), corrigee.creee)
    }

    @Test
    fun `un ajout posterieur a la creation ne la touche pas`() {
        assertEquals(creation, arroser.ajouterAccomplissement(apres(1.days)).creee)
    }

    // ---------------------------------------------------------------- retirer

    @Test
    fun `retirer le dernier restaure l'echeance precedente`() {
        val deuxFois = arroser.ajouterAccomplissement(apres(5.days))
        assertEquals(apres(6.days), deuxFois.retirerAccomplissement(apres(5.days)).echeance)
    }

    @Test
    fun `retirer un accomplissement ancien laisse l'echeance en place`() {
        val deuxFois = arroser.ajouterAccomplissement(apres(5.days))
        assertEquals(deuxFois.echeance, deuxFois.retirerAccomplissement(apres(3.days)).echeance)
    }

    /** Deux « Fait » a la meme seconde : on n'en retire qu'un. */
    @Test
    fun `retirer un doublon n'en retire qu'un`() {
        val doublon = arroser.ajouterAccomplissement(apres(3.days))
        assertEquals(listOf(apres(3.days)), doublon.retirerAccomplissement(apres(3.days)).historique)
    }

    @Test
    fun `retirer un accomplissement inconnu ne change rien`() {
        assertSame(arroser, arroser.retirerAccomplissement(apres(9.days)))
    }

    @Test
    fun `la creation ne revient pas en avant quand on retire ce qui l'avait reculee`() {
        val avant = creation.minus(10.days.toJavaDuration())
        val corrigee = arroser.ajouterAccomplissement(avant).retirerAccomplissement(avant)
        assertEquals(avant, corrigee.creee)
    }

    // ---------------------------------------------------------------- deplacer

    @Test
    fun `deplacer corrige la date et l'echeance qui en depend`() {
        val corrigee = arroser.deplacerAccomplissement(apres(3.days), apres(3.days - 5.hours))
        assertEquals(listOf(apres(3.days - 5.hours)), corrigee.historique)
        assertEquals(apres(6.days - 5.hours), corrigee.echeance)
    }

    @Test
    fun `deplacer un accomplissement inconnu ne change rien`() {
        assertSame(arroser, arroser.deplacerAccomplissement(apres(9.days), apres(1.days)))
    }
}
