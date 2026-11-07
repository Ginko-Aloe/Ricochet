package io.github.ginkoaloe.ricochet.domaine

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.MonthDay
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/**
 * Les jours ou tombe un Rendez-vous, independamment de l'heure.
 *
 * Chaque calendrier decoupe le temps en **periodes** — semaines, mois ou annees —
 * et dit quels jours de chaque periode comptent. [tous] n'en retient qu'une sur N :
 * « toutes les 2 semaines ».
 */
sealed interface Calendrier {
    val tous: Int

    /** Certains jours de la semaine. */
    data class Hebdomadaire(val jours: Set<DayOfWeek>, override val tous: Int = 1) : Calendrier {
        init {
            require(jours.isNotEmpty()) { "Il faut au moins un jour." }
            require(tous >= 1) { "« Tous les » vaut au moins 1, recu : $tous" }
        }
    }

    /**
     * Certains jours du mois, de 1 a 31. Un jour qui n'existe pas dans un mois plus
     * court tombe sur son dernier jour : « le 31 » est le 30 avril et le 28 fevrier.
     * [DERNIER_JOUR] se dit donc 31.
     */
    data class Mensuel(val jours: Set<Int>, override val tous: Int = 1) : Calendrier {
        init {
            require(jours.isNotEmpty()) { "Il faut au moins un jour." }
            require(jours.all { it in 1..31 }) { "Jours du mois entre 1 et 31, recu : $jours" }
            require(tous >= 1) { "« Tous les » vaut au moins 1, recu : $tous" }
        }

        companion object {
            const val DERNIER_JOUR = 31
        }
    }

    /** Le Nieme jour de semaine du mois : « le 1er lundi », « le dernier vendredi ». */
    data class MensuelParRang(val positions: Set<Position>, override val tous: Int = 1) : Calendrier {
        init {
            require(positions.isNotEmpty()) { "Il faut au moins une position." }
            require(tous >= 1) { "« Tous les » vaut au moins 1, recu : $tous" }
        }
    }

    /** Certaines dates de l'annee. Un 29 fevrier tombe le 28 les annees communes. */
    data class Annuel(val dates: Set<MonthDay>, override val tous: Int = 1) : Calendrier {
        init {
            require(dates.isNotEmpty()) { "Il faut au moins une date." }
            require(tous >= 1) { "« Tous les » vaut au moins 1, recu : $tous" }
        }
    }
}

/**
 * Un jour de semaine dans le mois. [rang] va de 1 a 4, ou vaut [DERNIER].
 */
data class Position(val rang: Int, val jour: DayOfWeek) {
    init {
        require(rang in 1..4 || rang == DERNIER) { "Rang entre 1 et 4, ou dernier, recu : $rang" }
    }

    companion object {
        const val DERNIER = -1
    }
}

/** Le numero de la periode qui contient [date] : semaine, mois ou annee. */
internal fun Calendrier.periodeDe(date: LocalDate): Long = when (this) {
    is Calendrier.Hebdomadaire -> Math.floorDiv(date.toEpochDay() - LUNDI_EPOQUE, 7L)
    is Calendrier.Mensuel, is Calendrier.MensuelParRang -> date.year * 12L + (date.monthValue - 1)
    is Calendrier.Annuel -> date.year.toLong()
}

/** Les jours retenus dans la periode [periode], dans l'ordre. */
internal fun Calendrier.joursDe(periode: Long): List<LocalDate> = when (this) {
    is Calendrier.Hebdomadaire -> {
        val lundi = LocalDate.ofEpochDay(LUNDI_EPOQUE + periode * 7)
        jours.map { lundi.plusDays(it.value - 1L) }
    }
    is Calendrier.Mensuel -> {
        val mois = moisDe(periode)
        jours.map { mois.atDay(minOf(it, mois.lengthOfMonth())) }
    }
    is Calendrier.MensuelParRang -> {
        val premier = moisDe(periode).atDay(1)
        positions.map { p ->
            if (p.rang == Position.DERNIER) premier.with(TemporalAdjusters.lastInMonth(p.jour))
            else premier.with(TemporalAdjusters.dayOfWeekInMonth(p.rang, p.jour))
        }
    }
    is Calendrier.Annuel -> dates.map { it.atYear(periode.toInt()) }
}.distinct().sorted()

private fun moisDe(periode: Long): YearMonth =
    YearMonth.of(Math.floorDiv(periode, 12L).toInt(), Math.floorMod(periode, 12L).toInt() + 1)

/** 1970-01-05, premier lundi apres l'epoque : l'origine des semaines. */
private const val LUNDI_EPOQUE = 4L
