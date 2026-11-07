package io.github.ginkoaloe.ricochet.domaine

import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class AgendaTest {

    private val paris: ZoneId = ZoneId.of("Europe/Paris")

    private fun le(texte: String): Instant = LocalDateTime.parse(texte).atZone(paris).toInstant()

    private fun jour(texte: String): LocalDate = LocalDate.parse(texte)

    /** Samedi 7 novembre 2026, midi. */
    private val maintenant = le("2026-11-07T12:00")

    private fun agenda(vararg taches: Tache, du: String = "2026-11-01", au: String = "2026-11-30") =
        taches.toList().agenda(jour(du), jour(au), maintenant, paris)

    /** Les marques d'une sorte donnee, par jour. */
    private fun Map<LocalDate, List<Marque>>.jours(sorte: SorteDeMarque) =
        filterValues { marques -> marques.any { it.sorte == sorte } }.keys.sorted()

    @Test
    fun `un ricochet fait aujourd'hui est du dans une periode, puis projete`() {
        val arroser = Tache("Arroser", 3.days, listOf(maintenant), creee = le("2026-11-01T12:00"))

        val agenda = agenda(arroser, au = "2026-11-17")

        assertEquals(listOf(jour("2026-11-10")), agenda.jours(SorteDeMarque.ECHEANCE))
        assertEquals(listOf(jour("2026-11-13"), jour("2026-11-16")), agenda.jours(SorteDeMarque.PROJECTION))
    }

    @Test
    fun `une tache de moins d'un jour n'est pas projetee`() {
        val pilule = Tache("Pilule", 8.hours, listOf(maintenant), creee = le("2026-11-01T12:00"))

        val agenda = agenda(pilule)

        assertEquals(listOf(jour("2026-11-07")), agenda.jours(SorteDeMarque.ECHEANCE))
        assertTrue(agenda.jours(SorteDeMarque.PROJECTION).isEmpty())
    }

    /** Due le 3, toujours pas faite le 7 : le retard se montre aujourd'hui. */
    @Test
    fun `un retard s'affiche aujourd'hui, pas a sa date manquee`() {
        val arroser = Tache("Arroser", 2.days, creee = le("2026-11-01T12:00"))

        val agenda = agenda(arroser, au = "2026-11-12")

        assertEquals(listOf(jour("2026-11-07")), agenda.jours(SorteDeMarque.RETARD))
        assertTrue(agenda.jours(SorteDeMarque.ECHEANCE).isEmpty())
        // Les projections partent de maintenant : si on le fait aujourd'hui, le 9.
        assertEquals(listOf(jour("2026-11-09"), jour("2026-11-11")), agenda.jours(SorteDeMarque.PROJECTION))
    }

    @Test
    fun `les Faits passes s'affichent a leur jour`() {
        val arroser = Tache("Arroser", 3.days, listOf(le("2026-11-02T08:00"), le("2026-11-05T21:00")), creee = le("2026-11-01T12:00"))

        assertEquals(listOf(jour("2026-11-02"), jour("2026-11-05")), agenda(arroser).jours(SorteDeMarque.FAIT))
    }

    /** Le loyer du 1er decembre, deja paye le 28 novembre, ne s'affiche plus. */
    @Test
    fun `un rendez-vous montre ses occurrences non soldees`() {
        val loyer = Tache(
            "Loyer",
            Rythme.RendezVous(Calendrier.Mensuel(setOf(1)), zone = paris),
            historique = listOf(le("2026-11-28T18:00")),
            creee = le("2026-11-02T12:00"),
        )

        val agenda = loyer.let { listOf(it).agenda(jour("2026-11-01"), jour("2027-01-31"), le("2026-11-29T12:00"), paris) }

        assertEquals(listOf(jour("2027-01-01")), agenda.jours(SorteDeMarque.ECHEANCE))
    }

    @Test
    fun `un retard de rendez-vous en tenant le compte porte le nombre d'oublis`() {
        val compte = Tache(
            "Loyer",
            Rythme.RendezVous(Calendrier.Mensuel(setOf(1)), tenirLeCompteDesOublis = true, zone = paris),
            creee = le("2026-09-10T12:00"),
        )

        val retard = agenda(compte).getValue(jour("2026-11-07")).single { it.sorte == SorteDeMarque.RETARD }

        assertEquals(2, retard.oublis)
        assertEquals(le("2026-10-01T09:00"), retard.quand)
    }
}
