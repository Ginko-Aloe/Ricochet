package io.github.ginkoaloe.ricochet.domaine

import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

class TacheTest {

    private val lundiMidi: Instant = Instant.parse("2026-11-02T12:00:00Z")

    // ---------------------------------------------------------------- le principe

    /**
     * Le cas fondateur de Ricochet, celui qui justifie de ne pas utiliser un agenda.
     * Une tache de periode 3 jours, accomplie avec 2 jours de retard, redevient due
     * 3 jours apres cet accomplissement — et non 1 jour apres.
     */
    @Test
    fun `l'echeance repart de l'accomplissement, pas de l'echeance manquee`() {
        val arroser = Tache("Arroser le ficus", 3.days, historique = listOf(lundiMidi))
        val echeanceManquee = lundiMidi.plus(3.days.toJavaDuration())

        val deuxJoursDeRetard = echeanceManquee.plus(2.days.toJavaDuration())
        val apresCoup = arroser.accomplir(deuxJoursDeRetard)

        assertEquals(deuxJoursDeRetard.plus(3.days.toJavaDuration()), apresCoup.echeance)

        // Et surtout : pas ce que donnerait un agenda, qui aurait replanifie
        // trois jours apres l'echeance manquee, sans tenir compte du retard.
        val cequeDonneraitUnAgenda = echeanceManquee.plus(3.days.toJavaDuration())
        assertFalse(apresCoup.estDue(cequeDonneraitUnAgenda))
        assertEquals(2.days, apresCoup.tempsRestant(cequeDonneraitUnAgenda))
    }

    /** Corollaire : le retard est absorbe, jamais accumule. */
    @Test
    fun `un long retard ne cree pas de dette`() {
        val pilule = Tache("Pilule", 1.days, historique = listOf(lundiMidi))
        val unMoisPlusTard = lundiMidi.plus(30.days.toJavaDuration())

        val reprise = pilule.accomplir(unMoisPlusTard)

        assertEquals(unMoisPlusTard.plus(1.days.toJavaDuration()), reprise.echeance)
        assertFalse(reprise.estDue(unMoisPlusTard))
        assertEquals(1, reprise.historique.size - pilule.historique.size)
    }

    /** La granularite infra-journaliere, que WaterMe ne sait pas exprimer. */
    @Test
    fun `une periode peut etre plus courte qu'une journee`() {
        val pilule = Tache("Pilule", 8.hours, historique = listOf(lundiMidi))

        assertEquals(lundiMidi.plus(8.hours.toJavaDuration()), pilule.echeance)
        assertFalse(pilule.estDue(lundiMidi.plus(7.hours.toJavaDuration())))
        assertTrue(pilule.estDue(lundiMidi.plus(8.hours.toJavaDuration())))
    }

    // -------------------------------------------------- la tache jamais accomplie

    /**
     * Creer une tache demarre son rythme : elle est due une periode plus tard, et
     * non sur-le-champ — sans quoi elle notifierait a l'instant de sa creation.
     */
    @Test
    fun `une tache neuve est due une periode apres sa creation`() {
        val pilule = Tache("Pilule", 8.hours, creee = lundiMidi)

        assertTrue(pilule.jamaisAccomplie)
        assertNull(pilule.dernierAccomplissement)
        assertEquals(lundiMidi.plus(8.hours.toJavaDuration()), pilule.echeance)
        assertFalse(pilule.estDue(lundiMidi))
        assertTrue(pilule.estDue(lundiMidi.plus(8.hours.toJavaDuration())))
    }

    @Test
    fun `le premier accomplissement prend le relais de la creation`() {
        val pilule = Tache("Pilule", 8.hours, creee = lundiMidi)
        val faitePlusTard = lundiMidi.plus(20.hours.toJavaDuration())

        val apres = pilule.accomplir(faitePlusTard)

        assertFalse(apres.jamaisAccomplie)
        assertEquals(faitePlusTard.plus(8.hours.toJavaDuration()), apres.echeance)
    }

    @Test
    fun `annuler le premier accomplissement redonne la creation pour repere`() {
        val pilule = Tache("Pilule", 8.hours, creee = lundiMidi)
        val faite = pilule.accomplir(lundiMidi.plus(20.hours.toJavaDuration()))

        val repare = faite.annulerDernier()

        assertTrue(repare.jamaisAccomplie)
        assertEquals(pilule.echeance, repare.echeance)
    }

    // ------------------------------------------------------------------ l'echeance

    @Test
    fun `la tache n'est pas due avant son echeance, et l'est des qu'elle l'atteint`() {
        val serviette = Tache("Changer la serviette", 3.days, historique = listOf(lundiMidi))
        val echeance = lundiMidi.plus(3.days.toJavaDuration())

        assertFalse(serviette.estDue(echeance.minusSeconds(1)))
        assertTrue(serviette.estDue(echeance))
        assertTrue(serviette.estDue(echeance.plusSeconds(1)))
    }

    @Test
    fun `le temps restant est negatif en cas de retard`() {
        val serviette = Tache("Changer la serviette", 3.days, historique = listOf(lundiMidi))
        val deuxJoursApresEcheance = lundiMidi.plus(5.days.toJavaDuration())

        assertEquals((-2).days, serviette.tempsRestant(deuxJoursApresEcheance))
    }

    @Test
    fun `c'est le plus recent accomplissement qui fait foi, quel que soit l'ordre`() {
        val desordonne = listOf(
            lundiMidi.plus(2.days.toJavaDuration()),
            lundiMidi,
            lundiMidi.plus(1.days.toJavaDuration()),
        )
        val tache = Tache("Import de sauvegarde", 3.days, historique = desordonne)

        assertEquals(lundiMidi.plus(2.days.toJavaDuration()), tache.dernierAccomplissement)
        assertEquals(lundiMidi.plus(5.days.toJavaDuration()), tache.echeance)
    }

    // --------------------------------------------------------------- l'annulation

    @Test
    fun `annuler le dernier accomplissement restaure l'echeance precedente`() {
        val avant = Tache("Arroser le ficus", 3.days, historique = listOf(lundiMidi))
        val apresTapeMalencontreuse = avant.accomplir(lundiMidi.plus(1.days.toJavaDuration()))

        val repare = apresTapeMalencontreuse.annulerDernier()

        assertEquals(avant.echeance, repare.echeance)
        assertEquals(avant.historique, repare.historique)
    }

    @Test
    fun `annuler sur une tache jamais accomplie ne fait rien`() {
        val neuve = Tache("Appeler maman", 7.days, creee = lundiMidi)

        assertEquals(neuve, neuve.annulerDernier())
    }

    @Test
    fun `annuler ne retire qu'un seul accomplissement`() {
        val tache = Tache("Pilule", 8.hours, creee = lundiMidi)
            .accomplir(lundiMidi)
            .accomplir(lundiMidi.plus(8.hours.toJavaDuration()))
            .accomplir(lundiMidi.plus(16.hours.toJavaDuration()))

        val repare = tache.annulerDernier()

        assertEquals(2, repare.historique.size)
        assertEquals(lundiMidi.plus(8.hours.toJavaDuration()), repare.dernierAccomplissement)
    }

    // ----------------------------------------------------------------- la validite

    @Test
    fun `une periode nulle ou negative est refusee`() {
        assertFailsWith<IllegalArgumentException> { Tache("Impossible", 0.days) }
        assertFailsWith<IllegalArgumentException> { Tache("Impossible", (-1).days) }
    }

    @Test
    fun `un nom vide est refuse`() {
        assertFailsWith<IllegalArgumentException> { Tache("", 3.days) }
        assertFailsWith<IllegalArgumentException> { Tache("   ", 3.days) }
    }

    /** `copy()` passe aussi par `init`, la validation ne peut donc pas etre contournee. */
    @Test
    fun `la validation resiste a copy`() {
        val tache = Tache("Arroser le ficus", 3.days)

        assertFailsWith<IllegalArgumentException> { tache.copy(rythme = Rythme.Ricochet(0.days)) }
    }
}
