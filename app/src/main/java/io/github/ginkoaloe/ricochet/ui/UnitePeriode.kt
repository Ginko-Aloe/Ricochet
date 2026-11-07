package io.github.ginkoaloe.ricochet.ui

import androidx.annotation.StringRes
import io.github.ginkoaloe.ricochet.R
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/** Les unites proposees a la saisie d'une periode. */
enum class UnitePeriode(@StringRes val libelle: Int) {
    HEURES(R.string.unite_heures),
    JOURS(R.string.unite_jours),
    SEMAINES(R.string.unite_semaines);

    fun vers(nombre: Long): Duration = when (this) {
        HEURES -> nombre.hours
        JOURS -> nombre.days
        SEMAINES -> (nombre * 7).days
    }
}

/**
 * Decompose une periode dans l'unite ou elle se lit le mieux : on edite « 2 semaines »
 * plutot que « 336 heures ». La plus grande unite qui tombe juste l'emporte.
 *
 * En deca de l'heure, la saisie ne sait rien exprimer : une telle periode est ramenee
 * a une heure. L'editeur ne permet pas d'en creer, c'est donc sans consequence.
 */
fun Duration.decomposer(): Pair<Long, UnitePeriode> {
    val heures = inWholeHours
    val heuresParSemaine = 24L * 7
    return when {
        heures >= heuresParSemaine && heures % heuresParSemaine == 0L ->
            heures / heuresParSemaine to UnitePeriode.SEMAINES

        heures >= 24 && heures % 24 == 0L ->
            heures / 24 to UnitePeriode.JOURS

        else -> heures.coerceAtLeast(1) to UnitePeriode.HEURES
    }
}
