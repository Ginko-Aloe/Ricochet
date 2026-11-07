package io.github.ginkoaloe.ricochet.ui

import io.github.ginkoaloe.ricochet.domaine.Calendrier
import io.github.ginkoaloe.ricochet.domaine.Position
import io.github.ginkoaloe.ricochet.domaine.Rythme
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.MonthDay
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BrouillonRendezVousTest {

    private val paris: ZoneId = ZoneId.of("Europe/Paris")

    @Test
    fun `la saisie devient le rythme qu'elle decrit`() {
        val brouillon = BrouillonRendezVous(
            unite = UniteRendezVous.SEMAINE,
            tous = 2,
            joursSemaine = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
            heure = LocalTime.of(18, 30),
            tenirLeCompteDesOublis = true,
        )

        assertEquals(
            Rythme.RendezVous(
                Calendrier.Hebdomadaire(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), tous = 2),
                LocalTime.of(18, 30), tenirLeCompteDesOublis = true, zone = paris,
            ),
            brouillon.rythme(paris),
        )
    }

    /** Les reglages des autres unites restent en memoire, sans compter. */
    @Test
    fun `seule l'unite choisie compte`() {
        val brouillon = BrouillonRendezVous(unite = UniteRendezVous.MOIS, joursMois = setOf(1, 15))

        assertEquals(Calendrier.Mensuel(setOf(1, 15)), brouillon.rythme(paris)?.calendrier)
    }

    @Test
    fun `les rangs se croisent avec les jours`() {
        val brouillon = BrouillonRendezVous(
            unite = UniteRendezVous.MOIS,
            parRang = true,
            rangs = setOf(1, 3),
            joursRang = setOf(DayOfWeek.TUESDAY),
        )

        assertEquals(
            Calendrier.MensuelParRang(setOf(Position(1, DayOfWeek.TUESDAY), Position(3, DayOfWeek.TUESDAY))),
            brouillon.rythme(paris)?.calendrier,
        )
    }

    /** Un champ « Tous les » vide ou a 0 ne doit pas garder l'ancienne valeur en douce. */
    @Test
    fun `tous les 0 n'est pas un rythme`() {
        assertNull(BrouillonRendezVous(tous = 0).rythme(paris))
    }

    @Test
    fun `une saisie sans jour n'est pas un rythme`() {
        assertNull(BrouillonRendezVous(unite = UniteRendezVous.SEMAINE, joursSemaine = emptySet()).rythme(paris))
    }

    @Test
    fun `un rendez-vous existant se relit dans le brouillon`() {
        val annuel = Rythme.RendezVous(
            Calendrier.Annuel(setOf(MonthDay.of(3, 15)), tous = 2), LocalTime.of(8, 0), zone = paris,
        )

        assertEquals(annuel, BrouillonRendezVous.depuis(annuel).rythme(paris))
    }
}
