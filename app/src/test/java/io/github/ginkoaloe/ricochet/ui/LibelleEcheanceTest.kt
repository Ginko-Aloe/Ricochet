package io.github.ginkoaloe.ricochet.ui

import io.github.ginkoaloe.ricochet.domaine.EtatEcheance
import io.github.ginkoaloe.ricochet.domaine.Tache
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class LibelleEcheanceTest {

    /** Les vrais textes francais, lus dans values-fr/strings.xml. */
    private val fr = TextesDeTest.francais

    private val lundiMidi: Instant = Instant.parse("2026-11-02T12:00:00Z")

    @Test
    fun `l'unite choisie est la plus parlante`() {
        assertEquals("Dans 3 jours", EtatEcheance.AVenir(3.days).libelle(fr))
        assertEquals("Dans 5 heures", EtatEcheance.AVenir(5.hours).libelle(fr))
        assertEquals("Dans 20 minutes", EtatEcheance.AVenir(20.minutes).libelle(fr))
    }

    /** 72 heures se disent « 3 jours ». */
    @Test
    fun `les heures deviennent des jours des qu'il y en a assez`() {
        assertEquals("Dans 3 jours", EtatEcheance.AVenir(72.hours).libelle(fr))
    }

    @Test
    fun `l'arrondi se fait vers le bas`() {
        assertEquals("Dans 2 jours", EtatEcheance.AVenir((2.days + 23.hours)).libelle(fr))
    }

    @Test
    fun `le singulier et le pluriel sont respectes`() {
        assertEquals("Dans 2 jours", EtatEcheance.AVenir(2.days).libelle(fr))
        assertEquals("Dans 1 heure", EtatEcheance.AVenir(1.hours).libelle(fr))
        assertEquals("Dans 1 minute", EtatEcheance.AVenir(1.minutes).libelle(fr))
    }

    // ------------------------------------------------- les jours du calendrier

    /** Le lendemain se dit « demain », quelle que soit l'heure. */
    @Test
    fun `le lendemain se dit demain`() {
        assertEquals("Demain", EtatEcheance.AVenir(2.hours, jours = 1).libelle(fr))
        assertEquals("Demain", EtatEcheance.AVenir(47.hours, jours = 1).libelle(fr))
    }

    /** Ce sont les jours du calendrier qui comptent, pas les heures restantes. */
    @Test
    fun `au-dela, les jours se comptent au calendrier`() {
        assertEquals("Dans 3 jours", EtatEcheance.AVenir((2.days + 1.hours), jours = 3).libelle(fr))
        assertEquals("Dans 2 jours", EtatEcheance.AVenir((2.days + 23.hours), jours = 2).libelle(fr))
    }

    /** Le jour meme, les heures et les minutes restent : elles changent vraiment. */
    @Test
    fun `le jour meme, le decompte reste en heures`() {
        assertEquals("Dans 5 heures", EtatEcheance.AVenir(5.hours, jours = 0).libelle(fr))
    }

    @Test
    fun `une echeance imminente ne se chiffre pas en secondes`() {
        assertEquals("Dans moins d'une minute", EtatEcheance.AVenir(30.seconds).libelle(fr))
    }

    @Test
    fun `le retard s'annonce avec son ampleur`() {
        assertEquals("En retard de 2 jours", EtatEcheance.EnRetard(2.days).libelle(fr))
        assertEquals("En retard de 3 heures", EtatEcheance.EnRetard(3.hours).libelle(fr))
    }

    /** Un retard d'une poignee de secondes, c'est « maintenant », pas un retard. */
    @Test
    fun `un retard infime se dit maintenant`() {
        assertEquals("À faire maintenant", EtatEcheance.EnRetard(Duration.ZERO).libelle(fr))
        assertEquals("À faire maintenant", EtatEcheance.EnRetard(30.seconds).libelle(fr))
    }

    @Test
    fun `seul le retard est pressant`() {
        assertFalse(EtatEcheance.AVenir(1.days).estPressant)
        assertTrue(EtatEcheance.EnRetard(1.days).estPressant)
    }

    // ------------------------------------------------------- le libelle complet

    @Test
    fun `une tache jamais faite le signale en plus de son echeance`() {
        val neuve = Tache("Appeler maman", 7.days, creee = lundiMidi)

        assertEquals("Dans 7 jours · jamais faite", neuve.libelleEcheance(fr, lundiMidi))
    }

    @Test
    fun `une tache deja accomplie n'affiche que son echeance`() {
        val connue = Tache("Arroser", 3.days, historique = listOf(lundiMidi))

        assertEquals("Dans 3 jours", connue.libelleEcheance(fr, lundiMidi))
    }

    @Test
    fun `la mention jamais faite survit au retard`() {
        val oubliee = Tache("Appeler maman", 7.days, creee = lundiMidi)

        assertEquals(
            "En retard de 1 jour · jamais faite",
            oubliee.libelleEcheance(fr, lundiMidi.plus(8.days.toJavaDuration())),
        )
    }
}
