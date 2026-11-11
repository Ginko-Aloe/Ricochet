package io.github.ginkoaloe.ricochet.domaine

import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class JourneeTest {

    private val paris: ZoneId = ZoneId.of("Europe/Paris")

    private fun le(texte: String): Instant = LocalDateTime.parse(texte).atZone(paris).toInstant()

    private val huitHeures = le("2026-11-10T08:00")

    private fun faite(nom: String, periode: kotlin.time.Duration, quand: String) =
        Tache(nom, periode, historique = listOf(le(quand)), creee = le("2026-10-01T12:00"))

    // ------------------------------------------------------------- les taches du jour

    @Test
    fun `le jour compte les retards et ce qui tombe avant minuit`() {
        val enRetard = faite("Arroser", 3.days, "2026-11-06T18:00")    // due le 9 a 18 h
        val ceSoir = faite("Pilule", 12.hours, "2026-11-10T07:30")     // due a 19 h 30
        val justeAvantMinuit = faite("Chat", 1.days, "2026-11-09T23:59")
        val demain = faite("Linge", 1.days, "2026-11-10T07:00")        // due demain a 7 h

        val jour = listOf(demain, ceSoir, justeAvantMinuit, enRetard).duJour(huitHeures, paris)

        assertEquals(listOf("Arroser", "Pilule", "Chat"), jour.map { it.nom })
    }

    @Test
    fun `minuit pile appartient au lendemain`() {
        val aMinuit = faite("Chat", 1.days, "2026-11-10T00:00")        // due le 11 a 0 h

        assertEquals(emptyList(), listOf(aMinuit).duJour(huitHeures, paris))
    }

    @Test
    fun `un rendez-vous du jour y figure`() {
        val poubelles = Tache(
            "Poubelles",
            Rythme.RendezVous(Calendrier.Hebdomadaire(setOf(java.time.DayOfWeek.TUESDAY)), LocalTime.of(19, 0), zone = paris),
            creee = le("2026-11-09T12:00"),
        )

        // Creee la veille : sa premiere occurrence est ce mardi soir, pas un retard.
        assertEquals(le("2026-11-10T19:00"), poubelles.echeance)
        assertEquals(listOf("Poubelles"), listOf(poubelles).duJour(huitHeures, paris).map { it.nom })
    }

    // ------------------------------------------------------- combien de fois

    /** Due a 14 h, toutes les 2 heures : 14, 16, 18, 20 et 22 h. Minuit est demain. */
    @Test
    fun `un ricochet court tombe plusieurs fois dans la journee`() {
        val pilule = faite("Pilule", 2.hours, "2026-11-10T12:00")

        assertEquals(5, pilule.foisAujourdhui(huitHeures, paris))
    }

    /** En retard, la suite se compte depuis maintenant, comme si on la faisait la. */
    @Test
    fun `un ricochet en retard se projette depuis maintenant`() {
        val pilule = faite("Pilule", 5.hours, "2026-11-10T01:00")     // due a 6 h

        // Faite a 8 h : 13 h, 18 h, 23 h.
        assertEquals(4, pilule.foisAujourdhui(huitHeures, paris))
    }

    @Test
    fun `une tache quotidienne ne tombe qu'une fois`() {
        assertEquals(1, faite("Chat", 1.days, "2026-11-09T19:00").foisAujourdhui(huitHeures, paris))
        assertEquals(0, faite("Linge", 1.days, "2026-11-10T07:00").foisAujourdhui(huitHeures, paris))
    }

    /** Manque hier soir, le rendez-vous quotidien revient encore ce soir. */
    @Test
    fun `un rendez-vous en retard compte aussi celui du soir`() {
        val tous = java.time.DayOfWeek.entries.toSet()
        val soir = Tache(
            "Volets",
            Rythme.RendezVous(Calendrier.Hebdomadaire(tous), LocalTime.of(21, 0), zone = paris),
            creee = le("2026-11-09T12:00"),
        )

        assertEquals(2, soir.foisAujourdhui(huitHeures, paris))
        assertEquals(1, soir.foisAujourdhui(le("2026-11-10T21:30"), paris))
    }

    // ------------------------------------------------------------ le prochain matin

    @Test
    fun `avant l'heure, c'est aujourd'hui`() {
        assertEquals(le("2026-11-10T08:00"), prochaineFois(LocalTime.of(8, 0), le("2026-11-10T07:59"), paris))
    }

    /** L'alarme qui sonne a 8 h se repose pour le lendemain, pas pour l'instant meme. */
    @Test
    fun `a l'heure pile ou apres, c'est demain`() {
        assertEquals(le("2026-11-11T08:00"), prochaineFois(LocalTime.of(8, 0), le("2026-11-10T08:00"), paris))
        assertEquals(le("2026-11-11T08:00"), prochaineFois(LocalTime.of(8, 0), le("2026-11-10T21:00"), paris))
    }

    @Test
    fun `l'heure locale tient au changement d'heure`() {
        // Le 25 octobre 2026, Paris repasse a l'heure d'hiver.
        assertEquals(le("2026-10-25T08:00"), prochaineFois(LocalTime.of(8, 0), le("2026-10-24T09:00"), paris))
    }
}
