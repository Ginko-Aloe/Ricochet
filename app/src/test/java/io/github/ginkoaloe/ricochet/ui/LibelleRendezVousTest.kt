package io.github.ginkoaloe.ricochet.ui

import io.github.ginkoaloe.ricochet.domaine.Accomplissement
import io.github.ginkoaloe.ricochet.domaine.Calendrier
import io.github.ginkoaloe.ricochet.domaine.Position
import io.github.ginkoaloe.ricochet.domaine.Rythme
import io.github.ginkoaloe.ricochet.domaine.Tache
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.MonthDay
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class LibelleRendezVousTest {

    /** Les vrais textes francais, lus dans values-fr/strings.xml. */
    private val fr = TextesDeTest.francais

    private val paris: ZoneId = ZoneId.of("Europe/Paris")

    private fun le(texte: String): Instant = LocalDateTime.parse(texte).atZone(paris).toInstant()

    private fun rdv(calendrier: Calendrier, heure: LocalTime = LocalTime.of(9, 0), compte: Boolean = false) =
        Rythme.RendezVous(calendrier, heure, compte, paris)

    // ------------------------------------------------------------ le rythme en mots

    @Test
    fun `un ricochet se dit comme avant`() {
        assertEquals("Tous les 3 jours", libelleRythme(fr, Rythme.Ricochet(3.days)))
    }

    @Test
    fun `les jours de la semaine`() {
        assertEquals(
            "Chaque semaine, le dimanche à 19 h",
            libelleRythme(fr, rdv(Calendrier.Hebdomadaire(setOf(DayOfWeek.SUNDAY)), LocalTime.of(19, 0))),
        )
        assertEquals(
            "Toutes les 2 semaines, le lundi et le jeudi à 18 h 30",
            libelleRythme(fr, 
                rdv(Calendrier.Hebdomadaire(setOf(DayOfWeek.THURSDAY, DayOfWeek.MONDAY), tous = 2), LocalTime.of(18, 30))
            ),
        )
        assertEquals("Chaque jour à 9 h", libelleRythme(fr, rdv(Calendrier.Hebdomadaire(DayOfWeek.entries.toSet()))))
    }

    @Test
    fun `les jours du mois, le premier et le dernier compris`() {
        assertEquals(
            "Chaque mois, le 1er, le 15 et le dernier jour à 9 h",
            libelleRythme(fr, rdv(Calendrier.Mensuel(setOf(31, 1, 15)))),
        )
        assertEquals("Tous les 3 mois, le 1er à 9 h", libelleRythme(fr, rdv(Calendrier.Mensuel(setOf(1), tous = 3))))
    }

    @Test
    fun `les rangs dans le mois`() {
        assertEquals(
            "Chaque mois, le 1er lundi et le dernier vendredi à 9 h",
            libelleRythme(fr, 
                rdv(Calendrier.MensuelParRang(setOf(Position(Position.DERNIER, DayOfWeek.FRIDAY), Position(1, DayOfWeek.MONDAY))))
            ),
        )
        assertEquals(
            "Chaque mois, le 3e mardi à 9 h",
            libelleRythme(fr, rdv(Calendrier.MensuelParRang(setOf(Position(3, DayOfWeek.TUESDAY))))),
        )
    }

    @Test
    fun `les dates de l'annee`() {
        assertEquals("Chaque année, le 15 mars à 9 h", libelleRythme(fr, rdv(Calendrier.Annuel(setOf(MonthDay.of(3, 15))))))
        assertEquals(
            "Tous les 2 ans, le 1er janvier à 9 h",
            libelleRythme(fr, rdv(Calendrier.Annuel(setOf(MonthDay.of(1, 1)), tous = 2))),
        )
    }

    // ------------------------------------------------------------ l'echeance dans la liste

    private val loyer = rdv(Calendrier.Mensuel(setOf(1)))

    @Test
    fun `a venir, le delai puis la date`() {
        val tache = Tache("Loyer", loyer, creee = le("2026-11-05T12:00"))

        assertEquals("Dans 25 jours · mardi 1er déc.", tache.libelleEcheance(fr, le("2026-11-06T09:00")))
    }

    @Test
    fun `en retard, depuis quelle occurrence`() {
        val tache = Tache("Loyer", loyer, creee = le("2026-10-10T12:00"))

        assertEquals("En retard de 4 jours · depuis le 1er nov.", tache.libelleEcheance(fr, le("2026-11-05T12:00")))
    }

    @Test
    fun `en tenant le compte, le nombre d'oublis`() {
        val tache = Tache("Loyer", rdv(Calendrier.Mensuel(setOf(1)), compte = true), creee = le("2026-09-10T12:00"))

        assertEquals("2 oublis · depuis le 1er oct.", tache.libelleEcheance(fr, le("2026-11-05T12:00")))
    }

    // ------------------------------------------------------------ l'historique

    @Test
    fun `une ligne d'historique dit pour quelle occurrence`() {
        val accompli = Accomplissement(le("2026-10-28T18:00"), -(3.days + 15.hours), pour = le("2026-11-01T09:00"))

        assertEquals("Pour le 1er nov. · en avance de 3 jours", libelleAccomplissement(fr, accompli, paris))
    }

    @Test
    fun `une ligne de ricochet garde l'ecart seul`() {
        assertEquals("En retard de 2 jours", libelleAccomplissement(fr, Accomplissement(le("2026-11-05T12:00"), 2.days), paris))
    }
}
