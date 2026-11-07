package io.github.ginkoaloe.ricochet.ui

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class UnitePeriodeTest {

    @Test
    fun `la plus grande unite qui tombe juste l'emporte`() {
        assertEquals(2L to UnitePeriode.SEMAINES, 14.days.decomposer())
        assertEquals(1L to UnitePeriode.SEMAINES, 7.days.decomposer())
        assertEquals(3L to UnitePeriode.JOURS, 3.days.decomposer())
        assertEquals(1L to UnitePeriode.JOURS, 24.hours.decomposer())
        assertEquals(8L to UnitePeriode.HEURES, 8.hours.decomposer())
    }

    @Test
    fun `une duree qui ne tombe juste dans aucune unite reste en heures`() {
        assertEquals(36L to UnitePeriode.HEURES, 36.hours.decomposer())
        assertEquals(50L to UnitePeriode.HEURES, 50.hours.decomposer())
    }

    /** Dix jours tombent juste en jours, mais pas en semaines : ce sont des jours. */
    @Test
    fun `faute de tomber sur des semaines, on retombe sur les jours`() {
        assertEquals(10L to UnitePeriode.JOURS, 10.days.decomposer())
    }

    @Test
    fun `une periode plus courte qu'une heure est ramenee a une heure`() {
        assertEquals(1L to UnitePeriode.HEURES, 30.minutes.decomposer())
    }

    @Test
    fun `decomposer puis recomposer rend la periode d'origine`() {
        listOf(8.hours, 3.days, 14.days, 36.hours, 1.days, 10.days).forEach { origine ->
            val (nombre, unite) = origine.decomposer()
            assertEquals(origine, unite.vers(nombre), "aller-retour sur $origine")
        }
    }

    @Test
    fun `chaque unite convertit correctement`() {
        assertEquals(5.hours, UnitePeriode.HEURES.vers(5))
        assertEquals(5.days, UnitePeriode.JOURS.vers(5))
        assertEquals(21.days, UnitePeriode.SEMAINES.vers(3))
    }
}
