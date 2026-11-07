package io.github.ginkoaloe.ricochet.ui

import io.github.ginkoaloe.ricochet.domaine.Accomplissement
import io.github.ginkoaloe.ricochet.domaine.Calendrier
import io.github.ginkoaloe.ricochet.domaine.EtatEcheance
import io.github.ginkoaloe.ricochet.domaine.Position
import io.github.ginkoaloe.ricochet.domaine.Rythme
import io.github.ginkoaloe.ricochet.domaine.Tache
import io.github.ginkoaloe.ricochet.rappels.libelleDeReport
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
import kotlin.time.Duration.Companion.seconds

/**
 * La version anglaise, langue par defaut, lue dans values/strings.xml. Les memes
 * situations que les tests francais : ce qui compte est que chaque langue dise juste,
 * pas qu'elles disent pareil.
 */
class LibelleAnglaisTest {

    private val en = TextesDeTest.anglais
    private val paris: ZoneId = ZoneId.of("Europe/Paris")

    private fun le(texte: String): Instant = LocalDateTime.parse(texte).atZone(paris).toInstant()

    private fun rdv(calendrier: Calendrier, heure: LocalTime = LocalTime.of(9, 0), compte: Boolean = false) =
        Rythme.RendezVous(calendrier, heure, compte, paris)

    @Test
    fun `les echeances`() {
        assertEquals("In 3 days", EtatEcheance.AVenir(3.days).libelle(en))
        assertEquals("In 1 day", EtatEcheance.AVenir(1.days).libelle(en))
        assertEquals("2 days overdue", EtatEcheance.EnRetard(2.days).libelle(en))
        assertEquals("Due now", EtatEcheance.EnRetard(30.seconds).libelle(en))
    }

    @Test
    fun `les periodes et les ecarts`() {
        assertEquals("Every 3 days", libellePeriode(en, 3.days))
        assertEquals("Every day", libellePeriode(en, 1.days))
        assertEquals("Every 8 hours", libellePeriode(en, 8.hours))
        assertEquals("4 hours early", libelleEcart(en, (-4).hours))
        assertEquals("On time", libelleEcart(en, 30.seconds))
    }

    @Test
    fun `la date de l'historique`() {
        assertEquals("Monday, November 2, 2026 at 13:00", libelleDate(en, le("2026-11-02T13:00"), paris))
    }

    @Test
    fun `les rendez-vous en mots`() {
        assertEquals(
            "Every month on day 1, day 15 and the last day at 9:00",
            libelleRythme(en, rdv(Calendrier.Mensuel(setOf(31, 1, 15)))),
        )
        assertEquals(
            "Every 2 weeks on Monday and Thursday at 18:30",
            libelleRythme(en, rdv(Calendrier.Hebdomadaire(setOf(DayOfWeek.THURSDAY, DayOfWeek.MONDAY), tous = 2), LocalTime.of(18, 30))),
        )
        assertEquals(
            "Every month on the first Monday and the last Friday at 9:00",
            libelleRythme(
                en,
                rdv(Calendrier.MensuelParRang(setOf(Position(Position.DERNIER, DayOfWeek.FRIDAY), Position(1, DayOfWeek.MONDAY)))),
            ),
        )
        assertEquals("Every year on March 15 at 9:00", libelleRythme(en, rdv(Calendrier.Annuel(setOf(MonthDay.of(3, 15))))))
        assertEquals("Every day at 9:00", libelleRythme(en, rdv(Calendrier.Hebdomadaire(DayOfWeek.entries.toSet()))))
    }

    private val loyer = rdv(Calendrier.Mensuel(setOf(1)))

    @Test
    fun `l'echeance d'un rendez-vous dans la liste`() {
        assertEquals(
            "In 25 days · Tuesday, Dec 1",
            Tache("Rent", loyer, creee = le("2026-11-05T12:00")).libelleEcheance(en, le("2026-11-06T09:00")),
        )
        assertEquals(
            "4 days overdue · since Nov 1",
            Tache("Rent", loyer, creee = le("2026-10-10T12:00")).libelleEcheance(en, le("2026-11-05T12:00")),
        )
        assertEquals(
            "2 missed · since Oct 1",
            Tache("Rent", rdv(Calendrier.Mensuel(setOf(1)), compte = true), creee = le("2026-09-10T12:00"))
                .libelleEcheance(en, le("2026-11-05T12:00")),
        )
    }

    @Test
    fun `une ligne d'historique et le report`() {
        val accompli = Accomplissement(le("2026-10-28T18:00"), -(3.days + 15.hours), pour = le("2026-11-01T09:00"))
        assertEquals("For Nov 1 · 3 days early", libelleAccomplissement(en, accompli, paris))
        assertEquals("In 1 hour", libelleDeReport(en, 8.hours))
    }
}
