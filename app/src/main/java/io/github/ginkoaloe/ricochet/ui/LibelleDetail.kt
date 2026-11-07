package io.github.ginkoaloe.ricochet.ui

import io.github.ginkoaloe.ricochet.R
import io.github.ginkoaloe.ricochet.domaine.Tache
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toKotlinDuration

/**
 * Les mots de l'ecran de detail et de l'historique. Comme [libelle], en Kotlin pur
 * pour se tester sans appareil.
 */

/** « Tous les 3 jours », « Toutes les 8 heures », « Chaque semaine ». */
fun libellePeriode(t: Textes, periode: Duration): String {
    val (nombre, unite) = periode.decomposer()
    val id = when (unite) {
        UnitePeriode.HEURES -> R.plurals.periode_heures
        UnitePeriode.JOURS -> R.plurals.periode_jours
        UnitePeriode.SEMAINES -> R.plurals.periode_semaines
    }
    return t.pluriel(id, nombre.toInt(), nombre.toInt())
}

/** « Faite il y a 4 jours », ou « Jamais faite ». */
fun Tache.libelleDernierAccomplissement(t: Textes, maintenant: Instant): String {
    val dernier = dernierAccomplissement ?: return t.texte(R.string.jamais_faite)
    return t.texte(R.string.faite_il_y_a, duree(t, java.time.Duration.between(dernier, maintenant).toKotlinDuration()))
}

/**
 * L'ecart d'un accomplissement a son echeance. En deca de la minute, on ne
 * chicane pas : c'etait a l'heure.
 */
fun libelleEcart(t: Textes, ecart: Duration): String = when {
    ecart.absoluteValue < 1.minutes -> t.texte(R.string.a_l_heure)
    ecart.isPositive() -> t.texte(R.string.en_retard_de, duree(t, ecart))
    else -> t.texte(R.string.en_avance_de, duree(t, -ecart))
}

/** « Lundi 2 novembre 2026 à 13:00 », a l'heure locale. */
fun libelleDate(t: Textes, instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ofPattern(t.texte(R.string.motif_date_historique), t.locale)
        .format(instant.atZone(zone))
        .replaceFirstChar { it.titlecase(t.locale) }
