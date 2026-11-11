package io.github.ginkoaloe.ricochet.rappels

import io.github.ginkoaloe.ricochet.domaine.Tache
import io.github.ginkoaloe.ricochet.ui.TextesDeTest
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class RecapitulatifTest {

    private val fr = TextesDeTest.francais
    private val en = TextesDeTest.anglais

    private val paris: ZoneId = ZoneId.of("Europe/Paris")

    private fun le(texte: String): Instant = LocalDateTime.parse(texte).atZone(paris).toInstant()

    private val huitHeures = le("2026-11-10T08:00")

    @Test
    fun `le titre compte les taches`() {
        assertEquals("1 tâche aujourd'hui", titreDuJour(fr, 1))
        assertEquals("3 tâches aujourd'hui", titreDuJour(fr, 3))
        assertEquals("3 tasks today", titreDuJour(en, 3))
    }

    /** Pour ce qui vient, l'heure : le matin, « dans 11 heures » obligerait a compter. */
    @Test
    fun `une tache a venir donne son heure`() {
        val pilule = Tache("Pilule", 12.hours, historique = listOf(le("2026-11-10T07:00")), creee = le("2026-11-01T12:00"))

        assertEquals("Pilule · à 19 h", ligneDuJour(fr, pilule, huitHeures, paris))
        assertEquals("Pilule · at 19:00", ligneDuJour(en, pilule, huitHeures, paris))
    }

    @Test
    fun `les fois suivantes du jour se comptent sans heure`() {
        val pilule = Tache("Pilule", 2.hours, historique = listOf(le("2026-11-10T12:00")), creee = le("2026-11-01T12:00"))
        val tisane = Tache("Tisane", 8.hours, historique = listOf(le("2026-11-10T06:00")), creee = le("2026-11-01T12:00"))

        assertEquals("Pilule · à 14 h, puis encore 4 fois", ligneDuJour(fr, pilule, huitHeures, paris))
        assertEquals("Tisane · à 14 h, puis encore une fois", ligneDuJour(fr, tisane, huitHeures, paris))
        assertEquals("Pill · at 14:00, then 4 more times", ligneDuJour(en, pilule.copy(nom = "Pill"), huitHeures, paris))
    }

    @Test
    fun `une tache en retard donne son retard`() {
        val arroser = Tache("Arroser", 3.days, historique = listOf(le("2026-11-06T18:00")), creee = le("2026-11-01T12:00"))

        assertEquals("Arroser · en retard de 14 heures", ligneDuJour(fr, arroser, huitHeures, paris))
    }
}
