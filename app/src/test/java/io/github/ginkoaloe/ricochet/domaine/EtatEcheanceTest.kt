package io.github.ginkoaloe.ricochet.domaine

import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

class EtatEcheanceTest {

    private val lundiMidi: Instant = Instant.parse("2026-11-02T12:00:00Z")

    private fun tache(nom: String, periode: Duration, fait: Instant? = null) =
        Tache(nom, periode, historique = listOfNotNull(fait), creee = lundiMidi)

    // ----------------------------------------------------------------- les etats

    @Test
    fun `avant l'echeance, l'etat porte le temps restant`() {
        val t = tache("Arroser", 3.days, fait = lundiMidi)

        val etat = t.etatEcheance(lundiMidi.plus(1.days.toJavaDuration()))

        assertEquals(EtatEcheance.AVenir(2.days), etat)
    }

    @Test
    fun `apres l'echeance, l'etat porte le retard`() {
        val t = tache("Arroser", 3.days, fait = lundiMidi)

        val etat = t.etatEcheance(lundiMidi.plus(5.days.toJavaDuration()))

        assertEquals(EtatEcheance.EnRetard(2.days), etat)
    }

    /** A l'instant pile de l'echeance, la tache est due : en retard de zero. */
    @Test
    fun `a l'echeance exacte, le retard est nul`() {
        val t = tache("Arroser", 3.days, fait = lundiMidi)

        val etat = t.etatEcheance(lundiMidi.plus(3.days.toJavaDuration()))

        assertEquals(EtatEcheance.EnRetard(Duration.ZERO), etat)
    }

    /** Une tache neuve n'est pas un cas a part : son decompte part de sa creation. */
    @Test
    fun `une tache jamais accomplie compte depuis sa creation`() {
        val t = tache("Appeler maman", 7.days)

        assertEquals(EtatEcheance.AVenir(7.days), t.etatEcheance(lundiMidi))
        assertEquals(
            EtatEcheance.EnRetard(1.days),
            t.etatEcheance(lundiMidi.plus(8.days.toJavaDuration())),
        )
    }

    // -------------------------------------------------------------------- le tri

    @Test
    fun `l'ordre va du plus urgent au moins urgent`() {
        val maintenant = lundiMidi.plus(10.days.toJavaDuration())
        val enRetardDe1j = tache("Retard 1j", 3.days, fait = maintenant.minus(4.days.toJavaDuration()))
        val enRetardDe5j = tache("Retard 5j", 3.days, fait = maintenant.minus(8.days.toJavaDuration()))
        val dans2j = tache("Dans 2j", 3.days, fait = maintenant.minus(1.days.toJavaDuration()))

        val ordre = listOf(dans2j, enRetardDe1j, enRetardDe5j)
            .parUrgence(maintenant)
            .map { it.nom }

        assertEquals(listOf("Retard 5j", "Retard 1j", "Dans 2j"), ordre)
    }

    @Test
    fun `une tache due a l'instant passe avant une tache a venir`() {
        val maintenant = lundiMidi.plus(10.days.toJavaDuration())
        val duePile = tache("Due", 3.days, fait = maintenant.minus(3.days.toJavaDuration()))
        val dansUneHeure = tache("Bientot", 3.days, fait = maintenant.minus(71.hours.toJavaDuration()))

        val ordre = listOf(dansUneHeure, duePile).parUrgence(maintenant).map { it.nom }

        assertEquals(listOf("Due", "Bientot"), ordre)
    }

    @Test
    fun `le tri d'une liste vide ne casse pas`() {
        assertEquals(emptyList(), emptyList<Tache>().parUrgence(lundiMidi))
    }
}
