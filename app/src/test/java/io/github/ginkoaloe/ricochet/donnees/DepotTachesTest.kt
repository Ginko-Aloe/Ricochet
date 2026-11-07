package io.github.ginkoaloe.ricochet.donnees

import io.github.ginkoaloe.ricochet.domaine.Calendrier
import io.github.ginkoaloe.ricochet.domaine.Position
import io.github.ginkoaloe.ricochet.domaine.Rythme
import io.github.ginkoaloe.ricochet.domaine.Tache
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.MonthDay
import java.time.ZoneId
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

class DepotTachesTest {

    private lateinit var dossier: File
    private lateinit var fichier: File
    private lateinit var depot: DepotTaches

    private val lundiMidi: Instant = Instant.parse("2026-11-02T12:00:00Z")

    @Before
    fun preparer() {
        dossier = Files.createTempDirectory("ricochet").toFile()
        fichier = File(dossier, "taches.json")
        depot = DepotTaches(fichier)
    }

    @After
    fun nettoyer() {
        dossier.deleteRecursively()
    }

    // ------------------------------------------------------------- aller-retour

    @Test
    fun `sans fichier, le depot est vide`() {
        assertFalse(fichier.exists())
        assertEquals(emptyList(), depot.charger())
    }

    @Test
    fun `une tache enregistree se recharge a l'identique`() {
        val origine = Tache(
            nom = "Arroser le ficus",
            periode = 3.days,
            historique = listOf(lundiMidi, lundiMidi.plusSeconds(3600)),
            image = "/photos/ficus.jpg",
        )

        depot.enregistrer(listOf(origine))
        val relue = depot.charger().single()

        assertEquals(origine.id, relue.id)
        assertEquals(origine.nom, relue.nom)
        assertEquals(origine.rythme, relue.rythme)
        assertEquals(origine.historique, relue.historique)
        assertEquals(origine.image, relue.image)
        // Ce qui compte vraiment : l'echeance survit au stockage.
        assertEquals(origine.echeance, relue.echeance)
    }

    /** Les quatre calendriers, avec chacun ses reglages, font l'aller-retour. */
    @Test
    fun `un rendez-vous se recharge a l'identique`() {
        val paris = ZoneId.of("Europe/Paris")
        val rythmes = listOf(
            Rythme.RendezVous(
                Calendrier.Hebdomadaire(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), tous = 2),
                LocalTime.of(18, 30), tenirLeCompteDesOublis = true, zone = paris,
                reference = LocalDate.of(2026, 11, 9),
            ),
            Rythme.RendezVous(Calendrier.Mensuel(setOf(1, 15, 31)), zone = paris),
            Rythme.RendezVous(
                Calendrier.MensuelParRang(setOf(Position(1, DayOfWeek.MONDAY), Position(Position.DERNIER, DayOfWeek.FRIDAY))),
                zone = paris,
            ),
            Rythme.RendezVous(Calendrier.Annuel(setOf(MonthDay.of(3, 15)), tous = 3), zone = paris),
        )
        val origines = rythmes.map { Tache("Rendez-vous", it, listOf(lundiMidi), creee = lundiMidi.minusSeconds(86400)) }

        depot.enregistrer(origines)

        assertEquals(origines, depot.charger())
    }

    @Test
    fun `une periode infra-journaliere survit au stockage`() {
        depot.enregistrer(listOf(Tache("Pilule", 8.hours, listOf(lundiMidi))))

        assertEquals(Rythme.Ricochet(8.hours), depot.charger().single().rythme)
    }

    @Test
    fun `une tache jamais accomplie se recharge sans historique`() {
        depot.enregistrer(listOf(Tache("Appeler maman", 7.days)))

        val relue = depot.charger().single()

        assertEquals(emptyList(), relue.historique)
        assertTrue(relue.jamaisAccomplie)
    }

    @Test
    fun `enregistrer remplace, et n'ajoute pas`() {
        depot.enregistrer(listOf(Tache("Premiere", 1.days), Tache("Deuxieme", 2.days)))
        depot.enregistrer(listOf(Tache("Seule", 3.days)))

        assertEquals(listOf("Seule"), depot.charger().map { it.nom })
    }

    /** Le fichier est aussi le format d'export : il doit rester lisible. */
    @Test
    fun `le fichier ecrit est du json lisible`() {
        depot.enregistrer(listOf(Tache("Arroser le ficus", 3.days)))

        val texte = fichier.readText()

        assertContains(texte, "Arroser le ficus")
        assertContains(texte, "periodeSecondes")
        assertTrue(texte.contains("\n"), "le json doit etre indente, pas sur une ligne")
    }

    @Test
    fun `l'ecriture ne laisse pas de fichier temporaire`() {
        depot.enregistrer(listOf(Tache("Arroser", 3.days)))

        val restes = dossier.listFiles()!!.map { it.name }
        assertEquals(listOf("taches.json"), restes)
    }

    // ----------------------------------------------------------- la robustesse

    @Test
    fun `un fichier illisible est ecarte plutot que de faire planter`() {
        fichier.writeText("ceci n'est pas du json {{{")

        assertEquals(emptyList(), depot.charger())

        assertFalse(fichier.exists(), "le fichier fautif doit avoir ete deplace")
        assertTrue(File(dossier, "taches.json.corrompu").exists(), "il doit etre conserve")
    }

    /** Du JSON valide, mais qui viole les regles du domaine : meme traitement. */
    @Test
    fun `une tache a la fois ricochet et rendez-vous est ecartee`() {
        fichier.writeText(
            """[{"id":"x","nom":"Ambigu","periodeSecondes":3600,""" +
                """"rendezVous":{"calendrier":"mois","jours":[1],"zone":"Europe/Paris"},"historique":[]}]"""
        )

        assertEquals(emptyList(), depot.charger())
    }

    @Test
    fun `un enregistrement incoherent est ecarte`() {
        fichier.writeText(
            """[{"id":"x","nom":"Impossible","periodeSecondes":0,"historique":[]}]"""
        )

        assertEquals(emptyList(), depot.charger())
        assertTrue(File(dossier, "taches.json.corrompu").exists())
    }

    @Test
    fun `un champ inconnu n'empeche pas la relecture`() {
        fichier.writeText(
            """[{"id":"x","nom":"Arroser","periodeSecondes":259200,"historique":[],"venuDuFutur":42}]"""
        )

        assertEquals(listOf("Arroser"), depot.charger().map { it.nom })
    }

    // ------------------------------------------------- la date de creation

    @Test
    fun `la date de creation survit au stockage`() {
        val creee = lundiMidi.minusSeconds(9999)
        depot.enregistrer(listOf(Tache("Appeler maman", 7.days, creee = creee)))

        val relue = depot.charger().single()

        assertEquals(creee, relue.creee)
        assertEquals(creee.plus(7.days.toJavaDuration()), relue.echeance)
    }

    /**
     * Les fichiers ecrits avant l'introduction de ce champ doivent rester lisibles.
     * Une tache deja accomplie a forcement ete creee au plus tard a ce moment-la.
     */
    @Test
    fun `un fichier sans date de creation reprend le premier accomplissement`() {
        fichier.writeText(
            """[{"id":"x","nom":"Arroser","periodeSecondes":259200,
               "historique":["2026-11-02T12:00:00Z","2026-11-05T12:00:00Z"]}]"""
        )

        assertEquals(lundiMidi, depot.charger().single().creee)
    }

    /**
     * Sans historique ni date de creation, on repart de maintenant : cela donne a la
     * tache une periode de repit, plutot qu'un retard fictif qui la ferait notifier
     * aussitot.
     */
    @Test
    fun `un fichier sans date ni historique ne devient pas en retard`() {
        fichier.writeText("""[{"id":"x","nom":"Arroser","periodeSecondes":259200}]""")

        val relue = depot.charger().single()

        assertTrue(relue.jamaisAccomplie)
        assertFalse(relue.estDue(Instant.now()), "une tache migree ne doit pas etre due d'emblee")
    }

    /**
     * Le defaut repere sur l'appareil : une date de creation reconstituee doit etre
     * **ecrite**, sans quoi elle est reinventee a chaque lecture et l'echeance d'une
     * tache jamais accomplie recule d'une periode a chaque ouverture.
     */
    @Test
    fun `une date de creation reconstituee est fixee des la premiere lecture`() {
        fichier.writeText("""[{"id":"x","nom":"Arroser","periodeSecondes":259200}]""")

        val premiere = depot.charger().single()
        assertContains(fichier.readText(), "creee")

        val seconde = depot.charger().single()
        assertEquals(premiere.creee, seconde.creee)
        assertEquals(premiere.echeance, seconde.echeance)
    }

    @Test
    fun `un fichier deja complet n'est pas reecrit a la lecture`() {
        depot.enregistrer(listOf(Tache("Arroser", 3.days, creee = lundiMidi)))
        val avant = fichier.readText()

        depot.charger()

        assertEquals(avant, fichier.readText())
    }
}
