package io.github.ginkoaloe.ricochet.rappels

import io.github.ginkoaloe.ricochet.ui.TextesDeTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class ReportTest {

    /** Les vrais textes francais, lus dans values-fr/strings.xml. */
    private val fr = TextesDeTest.francais

    @Test
    fun `le report vaut un huitieme de la periode`() {
        assertEquals(1.hours, dureeDeReport(8.hours))
        assertEquals(3.hours, dureeDeReport(24.hours))
    }

    @Test
    fun `une periode courte ne donne pas un report derisoire`() {
        // Un huitieme d'une heure ferait sept minutes : le rappel reviendrait aussitot.
        assertEquals(15.minutes, dureeDeReport(1.hours))
        assertEquals(15.minutes, dureeDeReport(2.hours))
    }

    @Test
    fun `une periode longue ne donne pas un report interminable`() {
        // Un huitieme de quinze jours ferait pres de deux jours : on aurait oublie.
        assertEquals(12.hours, dureeDeReport(15.days))
        assertEquals(12.hours, dureeDeReport(365.days))
    }

    @Test
    fun `le report reste toujours dans ses bornes`() {
        listOf(1.hours, 8.hours, 3.days, 7.days, 30.days).forEach { periode ->
            val report = dureeDeReport(periode)
            assertTrue(report >= 15.minutes, "report trop court pour $periode : $report")
            assertTrue(report <= 12.hours, "report trop long pour $periode : $report")
        }
    }

    @Test
    fun `le libelle annonce la duree reelle`() {
        assertEquals("Dans 1 heure", libelleDeReport(fr, 8.hours))
        assertEquals("Dans 3 heures", libelleDeReport(fr, 24.hours))
        assertEquals("Dans 15 minutes", libelleDeReport(fr, 1.hours))
        assertEquals("Dans 12 heures", libelleDeReport(fr, 30.days))
    }
}
