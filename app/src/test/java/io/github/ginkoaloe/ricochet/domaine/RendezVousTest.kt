package io.github.ginkoaloe.ricochet.domaine

import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class RendezVousTest {

    private val paris: ZoneId = ZoneId.of("Europe/Paris")

    /** Un instant lu a l'heure de Paris, pour ecrire les attendus comme on les dit. */
    private fun le(texte: String): Instant = LocalDateTime.parse(texte).atZone(paris).toInstant()

    private fun rendezVous(
        calendrier: Calendrier,
        heure: LocalTime = LocalTime.of(9, 0),
        tenirLeCompteDesOublis: Boolean = false,
    ) = Rythme.RendezVous(calendrier, heure, tenirLeCompteDesOublis, paris)

    private fun tache(rythme: Rythme, creee: Instant, historique: List<Instant> = emptyList()) =
        Tache("Poubelles", rythme, historique, creee = creee)

    // ------------------------------------------------------------ les occurrences

    /** Mercredi 4 novembre 2026 : le dimanche suivant est le 8. */
    @Test
    fun `un rendez-vous neuf est du a sa premiere occurrence`() {
        val dimanche = rendezVous(Calendrier.Hebdomadaire(setOf(DayOfWeek.SUNDAY)), LocalTime.of(19, 0))
        val tache = tache(dimanche, creee = le("2026-11-04T10:00"))

        assertEquals(le("2026-11-08T19:00"), tache.echeance)
    }

    private val loyer = rendezVous(Calendrier.Mensuel(setOf(1)))

    // ------------------------------------------------------------ ce que solde un Fait

    /** Le critere de l'issue : paye le 28, rien ne doit sonner le 1er. */
    @Test
    fun `un Fait en avance solde l'occurrence qui arrive`() {
        val tache = tache(loyer, creee = le("2026-10-10T12:00"), historique = listOf(le("2026-10-28T18:00")))

        assertEquals(le("2026-12-01T09:00"), tache.echeance)
    }

    @Test
    fun `un Fait en retard solde l'occurrence manquee`() {
        val tache = tache(loyer, creee = le("2026-10-10T12:00"), historique = listOf(le("2026-11-03T18:00")))

        assertEquals(le("2026-12-01T09:00"), tache.echeance)
    }

    /** Le 1er novembre est deja solde ; le 10, on peut deja faire celui de decembre. */
    @Test
    fun `un Fait loin de toute occurrence libre solde la suivante`() {
        val tache = tache(
            loyer,
            creee = le("2026-10-10T12:00"),
            historique = listOf(le("2026-10-31T18:00"), le("2026-11-10T18:00")),
        )

        assertEquals(le("2027-01-01T09:00"), tache.echeance)
    }

    /** Ni le 1er octobre ni le 1er novembre : un seul Fait remet a jour. */
    @Test
    fun `par defaut, les oublis sont absorbes`() {
        val tache = tache(loyer, creee = le("2026-09-10T12:00"), historique = listOf(le("2026-11-02T18:00")))

        assertEquals(le("2026-12-01T09:00"), tache.echeance)
    }

    @Test
    fun `en tenant le compte des oublis, un Fait solde le plus ancien`() {
        val compte = rendezVous(Calendrier.Mensuel(setOf(1)), tenirLeCompteDesOublis = true)
        val tache = tache(compte, creee = le("2026-09-10T12:00"), historique = listOf(le("2026-11-02T18:00")))

        assertEquals(le("2026-11-01T09:00"), tache.echeance)
    }

    // ------------------------------------------------------------ l'historique

    /** Paye le 28 octobre a 18 h pour le 1er novembre a 9 h : 3 jours et 15 heures d'avance. */
    @Test
    fun `l'ecart d'un rendez-vous se mesure a l'occurrence soldee`() {
        val tache = tache(loyer, creee = le("2026-10-10T12:00"), historique = listOf(le("2026-10-28T18:00")))

        val accompli = tache.accomplissements().single()
        assertEquals(le("2026-11-01T09:00"), accompli.pour)
        assertEquals(-(3.days + 15.hours), accompli.ecart)
    }

    @Test
    fun `un ricochet ne designe pas d'occurrence`() {
        val ricochet = Tache("Arroser", 3.days, listOf(le("2026-11-04T12:00")), creee = le("2026-11-01T12:00"))

        assertNull(ricochet.accomplissements().single().pour)
    }

    // ------------------------------------------------------------ l'etat a un instant

    /** Rien fait depuis septembre, le 5 novembre a midi. */
    private val cinqNovembre = le("2026-11-05T12:00")

    @Test
    fun `absorbe, le retard court depuis la derniere occurrence manquee`() {
        val tache = tache(loyer, creee = le("2026-09-10T12:00"))

        assertEquals(le("2026-11-01T09:00"), tache.dueDepuis(cinqNovembre))
        assertEquals(1, tache.oublis(cinqNovembre))
        assertTrue(tache.etatEcheance(cinqNovembre) is EtatEcheance.EnRetard)
    }

    @Test
    fun `en tenant le compte, le retard court depuis le plus ancien oubli`() {
        val compte = rendezVous(Calendrier.Mensuel(setOf(1)), tenirLeCompteDesOublis = true)
        val tache = tache(compte, creee = le("2026-09-10T12:00"))

        assertEquals(le("2026-10-01T09:00"), tache.dueDepuis(cinqNovembre))
        assertEquals(2, tache.oublis(cinqNovembre))
    }

    @Test
    fun `a jour, il n'y a ni retard ni oubli`() {
        val tache = tache(loyer, creee = le("2026-10-10T12:00"), historique = listOf(le("2026-11-01T10:00")))

        assertNull(tache.dueDepuis(cinqNovembre))
        assertEquals(0, tache.oublis(cinqNovembre))
    }

    /** En retard depuis le 1er novembre, le rendez-vous sonnera de nouveau le 1er decembre. */
    @Test
    fun `un rendez-vous en retard sonne a sa prochaine occurrence`() {
        val tache = tache(loyer, creee = le("2026-09-10T12:00"))

        assertEquals(le("2026-12-01T09:00"), tache.prochainRappel(cinqNovembre))
    }

    @Test
    fun `un ricochet en retard ne sonne plus, a l'heure il sonne a son echeance`() {
        val ricochet = Tache("Arroser", 3.days, creee = le("2026-11-01T12:00"))

        assertEquals(le("2026-11-04T12:00"), ricochet.prochainRappel(le("2026-11-02T12:00")))
        assertNull(ricochet.prochainRappel(cinqNovembre))
    }

    // ------------------------------------------------------------ la reference

    /**
     * Une semaine sur deux a partir du dimanche 8. Rattraper un Fait du 26 octobre fait
     * reculer la creation ; les occurrences doivent rester les 8 et 22, sans glisser
     * aux 1er et 15 que donnerait une creation au 26. Ce Fait, anterieur au
     * calendrier, ne solde rien.
     */
    @Test
    fun `une correction d'historique ne decale pas « toutes les N »`() {
        val uneSurDeux = rendezVous(Calendrier.Hebdomadaire(setOf(DayOfWeek.SUNDAY), tous = 2), LocalTime.of(19, 0))
            .ancre(apres = le("2026-11-04T10:00"))
        val tache = tache(uneSurDeux, creee = le("2026-11-04T10:00"), historique = listOf(le("2026-11-08T20:00")))

        val rattrapee = tache.ajouterAccomplissement(le("2026-10-26T12:00"))

        assertEquals(listOf(le("2026-11-08T19:00"), null), rattrapee.accomplissements().map { it.pour })
        assertEquals(le("2026-11-22T19:00"), rattrapee.echeance)
    }

    /**
     * Un Ricochet cree il y a un an, fait chaque semaine, devient un Rendez-vous du
     * dimanche aujourd'hui : il ne doit pas naitre en retard, ni endette.
     */
    @Test
    fun `convertir une vieille tache ne cree pas de dettes`() {
        val creation = le("2025-11-04T10:00")
        val faits = (0 until 50).map { creation.plus(java.time.Duration.ofDays(7L * it + 3)) }
        val dimanche = rendezVous(
            Calendrier.Hebdomadaire(setOf(DayOfWeek.SUNDAY)), LocalTime.of(19, 0), tenirLeCompteDesOublis = true,
        ).ancre(apres = le("2026-11-04T10:00"))

        val convertie = tache(dimanche, creee = creation, historique = faits)

        assertEquals(le("2026-11-08T19:00"), convertie.echeance)
        assertEquals(0, convertie.oublis(le("2026-11-05T12:00")))
    }

    @Test
    fun `un Fait bien anterieur au calendrier ne solde rien`() {
        val dimanche = rendezVous(Calendrier.Hebdomadaire(setOf(DayOfWeek.SUNDAY)), LocalTime.of(19, 0))
            .ancre(apres = le("2026-11-04T10:00"))
        val tache = tache(dimanche, creee = le("2026-11-04T10:00"))

        val rattrapee = tache.ajouterAccomplissement(le("2026-08-10T12:00"))

        assertEquals(le("2026-11-08T19:00"), rattrapee.echeance)
        assertNull(rattrapee.accomplissements().single().pour)
    }

    @Test
    fun `la premiere occurrence se lit avant meme de creer la tache`() {
        assertEquals(le("2026-12-01T09:00"), loyer.premiereOccurrence(apres = le("2026-11-05T12:00")))
    }

    // ------------------------------------------------------------ les calendriers

    private fun premieresOccurrences(rythme: Rythme.RendezVous, creee: Instant, combien: Int) =
        rythme.occurrences(creee).take(combien).toList()

    @Test
    fun `le 31 tombe sur le dernier jour des mois plus courts`() {
        val finDeMois = rendezVous(Calendrier.Mensuel(setOf(Calendrier.Mensuel.DERNIER_JOUR)))

        assertEquals(
            listOf(le("2027-01-31T09:00"), le("2027-02-28T09:00"), le("2027-03-31T09:00"), le("2027-04-30T09:00")),
            premieresOccurrences(finDeMois, le("2027-01-10T12:00"), 4),
        )
    }

    @Test
    fun `plusieurs jours du mois se suivent dans l'ordre`() {
        val quinzaine = rendezVous(Calendrier.Mensuel(setOf(15, 1)))

        assertEquals(
            listOf(le("2026-11-15T09:00"), le("2026-12-01T09:00"), le("2026-12-15T09:00")),
            premieresOccurrences(quinzaine, le("2026-11-10T12:00"), 3),
        )
    }

    /** Premiere occurrence le dimanche 8 ; la suivante le 22, pas le 15. */
    @Test
    fun `toutes les 2 semaines compte depuis la premiere occurrence`() {
        val uneSurDeux = rendezVous(Calendrier.Hebdomadaire(setOf(DayOfWeek.SUNDAY), tous = 2), LocalTime.of(19, 0))

        assertEquals(
            listOf(le("2026-11-08T19:00"), le("2026-11-22T19:00"), le("2026-12-06T19:00")),
            premieresOccurrences(uneSurDeux, le("2026-11-04T10:00"), 3),
        )
    }

    @Test
    fun `le premier lundi et le dernier vendredi du mois`() {
        val rangs = rendezVous(
            Calendrier.MensuelParRang(
                setOf(Position(1, DayOfWeek.MONDAY), Position(Position.DERNIER, DayOfWeek.FRIDAY))
            )
        )

        assertEquals(
            listOf(le("2026-11-27T09:00"), le("2026-12-07T09:00"), le("2026-12-25T09:00")),
            premieresOccurrences(rangs, le("2026-11-10T12:00"), 3),
        )
    }

    @Test
    fun `un 29 fevrier tombe le 28 les annees communes`() {
        val bissextile = rendezVous(Calendrier.Annuel(setOf(java.time.MonthDay.of(2, 29))))

        assertEquals(
            listOf(le("2027-02-28T09:00"), le("2028-02-29T09:00")),
            premieresOccurrences(bissextile, le("2026-11-10T12:00"), 2),
        )
    }

    /** Passage a l'heure d'hiver le 25 octobre 2026 : 9 h reste 9 h. */
    @Test
    fun `l'heure tient malgre le changement d'heure`() {
        val quotidien = rendezVous(Calendrier.Hebdomadaire(DayOfWeek.entries.toSet()))

        assertEquals(
            listOf(le("2026-10-24T09:00"), le("2026-10-25T09:00"), le("2026-10-26T09:00")),
            premieresOccurrences(quotidien, le("2026-10-24T08:00"), 3),
        )
    }
}
