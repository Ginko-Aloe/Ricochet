package io.github.ginkoaloe.ricochet.ui

import androidx.annotation.PluralsRes
import io.github.ginkoaloe.ricochet.R
import io.github.ginkoaloe.ricochet.domaine.Calendrier
import io.github.ginkoaloe.ricochet.domaine.Position
import io.github.ginkoaloe.ricochet.domaine.Rythme
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.MonthDay
import java.time.ZoneId

/** L'unite de « Tous les N … » pour un Rendez-vous. */
enum class UniteRendezVous(@PluralsRes val libelle: Int) {
    SEMAINE(R.plurals.unite_semaine),
    MOIS(R.plurals.unite_mois),
    AN(R.plurals.unite_an),
}

/**
 * Ce que l'editeur est en train de saisir pour un Rendez-vous, en Kotlin pur pour se
 * tester sans appareil.
 *
 * Les reglages de chaque unite sont gardes ensemble : passer de « mois » a
 * « semaines » et revenir ne perd rien. Seule l'unite choisie compte.
 *
 * Le rang se saisit en croisant des rangs et des jours : « 1er et 3e mardi ». Un
 * melange comme « 1er lundi et dernier vendredi » demande deux taches ; le stockage,
 * lui, sait le dire.
 */
data class BrouillonRendezVous(
    val unite: UniteRendezVous = UniteRendezVous.MOIS,
    val tous: Int = 1,
    val joursSemaine: Set<DayOfWeek> = setOf(DayOfWeek.SUNDAY),
    val parRang: Boolean = false,
    val joursMois: Set<Int> = setOf(1),
    val rangs: Set<Int> = setOf(1),
    val joursRang: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY),
    val date: MonthDay = MonthDay.of(1, 1),
    val heure: LocalTime = LocalTime.of(9, 0),
    val tenirLeCompteDesOublis: Boolean = false,
) {
    /** Le calendrier saisi, ou `null` s'il est incomplet — aucun jour coche, par exemple. */
    fun calendrier(): Calendrier? = runCatching {
        when (unite) {
            UniteRendezVous.SEMAINE -> Calendrier.Hebdomadaire(joursSemaine, tous)
            UniteRendezVous.MOIS ->
                if (parRang) Calendrier.MensuelParRang(
                    rangs.flatMap { r -> joursRang.map { Position(r, it) } }.toSet(), tous,
                )
                else Calendrier.Mensuel(joursMois, tous)
            UniteRendezVous.AN -> Calendrier.Annuel(setOf(date), tous)
        }
    }.getOrNull()

    fun rythme(zone: ZoneId): Rythme.RendezVous? =
        calendrier()?.let { Rythme.RendezVous(it, heure, tenirLeCompteDesOublis, zone) }

    companion object {
        /** Le brouillon qui decrit [rendezVous], pour le modifier. */
        fun depuis(rendezVous: Rythme.RendezVous): BrouillonRendezVous {
            val base = BrouillonRendezVous(
                tous = rendezVous.calendrier.tous,
                heure = rendezVous.heure,
                tenirLeCompteDesOublis = rendezVous.tenirLeCompteDesOublis,
            )
            return when (val c = rendezVous.calendrier) {
                is Calendrier.Hebdomadaire -> base.copy(unite = UniteRendezVous.SEMAINE, joursSemaine = c.jours)
                is Calendrier.Mensuel -> base.copy(unite = UniteRendezVous.MOIS, joursMois = c.jours)
                is Calendrier.MensuelParRang -> base.copy(
                    unite = UniteRendezVous.MOIS,
                    parRang = true,
                    rangs = c.positions.map { it.rang }.toSet(),
                    joursRang = c.positions.map { it.jour }.toSet(),
                )
                is Calendrier.Annuel -> base.copy(unite = UniteRendezVous.AN, date = c.dates.min())
            }
        }
    }
}
