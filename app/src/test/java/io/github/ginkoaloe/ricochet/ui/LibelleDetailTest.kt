package io.github.ginkoaloe.ricochet.ui

import io.github.ginkoaloe.ricochet.domaine.Tache
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class LibelleDetailTest {

    /** Les vrais textes francais, lus dans values-fr/strings.xml. */
    private val fr = TextesDeTest.francais

    private val lundiMidi: Instant = Instant.parse("2026-11-02T12:00:00Z")

    @Test
    fun `la periode s'accorde a son unite`() {
        assertEquals("Tous les 3 jours", libellePeriode(fr, 3.days))
        assertEquals("Toutes les 8 heures", libellePeriode(fr, 8.hours))
        assertEquals("Toutes les 2 semaines", libellePeriode(fr, 14.days))
    }

    @Test
    fun `une periode unitaire se dit chaque`() {
        assertEquals("Chaque jour", libellePeriode(fr, 1.days))
        assertEquals("Chaque heure", libellePeriode(fr, 1.hours))
        assertEquals("Chaque semaine", libellePeriode(fr, 7.days))
    }

    @Test
    fun `le dernier accomplissement se dit au passe`() {
        val faite = Tache("Arroser", 3.days, listOf(lundiMidi.minus(4.days.toJavaDuration())))
        assertEquals("Faite il y a 4 jours", faite.libelleDernierAccomplissement(fr, lundiMidi))
    }

    @Test
    fun `une tache neuve n'a jamais ete faite`() {
        assertEquals("Jamais faite", Tache("Arroser", 3.days).libelleDernierAccomplissement(fr, lundiMidi))
    }

    @Test
    fun `l'ecart dit le retard, l'avance, ou l'heure`() {
        assertEquals("En retard de 2 jours", libelleEcart(fr, 2.days))
        assertEquals("En avance de 4 heures", libelleEcart(fr, (-4).hours))
        assertEquals("À l'heure", libelleEcart(fr, 30.seconds))
        assertEquals("À l'heure", libelleEcart(fr, (-30).seconds))
    }

    /** 12 h UTC font 13 h a Paris en novembre. */
    @Test
    fun `la date se lit en francais, a l'heure locale`() {
        assertEquals(
            "Lundi 2 novembre 2026 à 13:00",
            libelleDate(fr, lundiMidi, ZoneId.of("Europe/Paris")),
        )
    }
}
