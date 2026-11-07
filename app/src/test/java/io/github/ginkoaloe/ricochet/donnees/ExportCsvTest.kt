package io.github.ginkoaloe.ricochet.donnees

import io.github.ginkoaloe.ricochet.domaine.Calendrier
import io.github.ginkoaloe.ricochet.domaine.Rythme
import io.github.ginkoaloe.ricochet.domaine.Tache
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

class ExportCsvTest {

    private val creation: Instant = Instant.parse("2026-11-02T12:00:00Z")

    private fun apres(d: Duration): Instant = creation.plus(d.toJavaDuration())

    private fun lignes(csv: String) = csv.removeSuffix("\r\n").split("\r\n")

    @Test
    fun `sans historique, il ne reste que l'entete`() {
        assertEquals(
            "id,tache,date,ecart_heures,periode_heures,type,echeance_soldee\r\n",
            historiqueEnCsv(listOf(Tache("Arroser", 3.days, creee = creation, id = "a"))),
        )
    }

    @Test
    fun `une ligne par accomplissement, du plus ancien au plus recent`() {
        val tache = Tache("Arroser", 3.days, listOf(apres(5.days), apres(7.days)), creee = creation, id = "a")
        assertEquals(
            listOf(
                "id,tache,date,ecart_heures,periode_heures,type,echeance_soldee",
                "a,Arroser,2026-11-07T12:00:00Z,48.00,72.00,ricochet,",
                "a,Arroser,2026-11-09T12:00:00Z,-24.00,72.00,ricochet,",
            ),
            lignes(historiqueEnCsv(listOf(tache))),
        )
    }

    @Test
    fun `les taches sont triees par nom`() {
        val pilule = Tache("pilule", 8.hours, listOf(apres(8.hours)), creee = creation, id = "p")
        val arroser = Tache("Arroser", 3.days, listOf(apres(3.days)), creee = creation, id = "a")
        assertEquals(listOf("a", "p"), lignes(historiqueEnCsv(listOf(pilule, arroser))).drop(1).map { it.substringBefore(',') })
    }

    @Test
    fun `un nom avec virgule ou guillemet est protege`() {
        val tache = Tache("Pain, \"bio\"", 1.days, listOf(apres(1.days)), creee = creation, id = "b")
        assertEquals(
            "b,\"Pain, \"\"bio\"\"\",2026-11-03T12:00:00Z,0.00,24.00,ricochet,",
            lignes(historiqueEnCsv(listOf(tache)))[1],
        )
    }

    /** Paye le 28 octobre a 18 h (Paris) pour le 1er novembre a 9 h. */
    @Test
    fun `un rendez-vous dit l'occurrence soldee, et n'a pas de periode`() {
        val loyer = Rythme.RendezVous(Calendrier.Mensuel(setOf(1)), zone = ZoneId.of("Europe/Paris"))
        val tache = Tache(
            "Loyer", loyer, listOf(Instant.parse("2026-10-28T17:00:00Z")),
            creee = Instant.parse("2026-10-10T10:00:00Z"), id = "l",
        )
        assertEquals(
            "l,Loyer,2026-10-28T17:00:00Z,-87.00,,rendez-vous,2026-11-01T08:00:00Z",
            lignes(historiqueEnCsv(listOf(tache)))[1],
        )
    }
}
