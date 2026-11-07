package io.github.ginkoaloe.ricochet.ui

import io.github.ginkoaloe.ricochet.R
import io.github.ginkoaloe.ricochet.domaine.Accomplissement
import io.github.ginkoaloe.ricochet.domaine.Calendrier
import io.github.ginkoaloe.ricochet.domaine.Position
import io.github.ginkoaloe.ricochet.domaine.Rythme
import io.github.ginkoaloe.ricochet.domaine.Tache
import io.github.ginkoaloe.ricochet.domaine.etatEcheance
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.Month
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

/**
 * Les mots des Rendez-vous. Comme [libelle], en Kotlin pur pour se tester sans
 * appareil.
 */

/**
 * Le rythme d'une tache en une phrase : « Tous les 3 jours », « Chaque mois, le 1er et
 * le 15 à 9 h », « Toutes les 2 semaines, le dimanche à 19 h ».
 */
fun libelleRythme(t: Textes, rythme: Rythme): String = when (rythme) {
    is Rythme.Ricochet -> libellePeriode(t, rythme.periode)
    is Rythme.RendezVous -> {
        val c = rythme.calendrier
        val chaqueJour = c is Calendrier.Hebdomadaire && c.tous == 1 && c.jours.size == 7
        val heure = libelleHeure(t, rythme.heure)
        if (chaqueJour) t.texte(R.string.chaque_jour_a, heure)
        else t.texte(R.string.rythme_rendez_vous, cadence(t, c), enumerer(t, jours(t, c)), heure)
    }
}

private fun cadence(t: Textes, c: Calendrier): String {
    val id = when (c) {
        is Calendrier.Hebdomadaire -> R.plurals.cadence_semaines
        is Calendrier.Mensuel, is Calendrier.MensuelParRang -> R.plurals.cadence_mois
        is Calendrier.Annuel -> R.plurals.cadence_ans
    }
    return t.pluriel(id, c.tous, c.tous)
}

private fun jours(t: Textes, c: Calendrier): List<String> = when (c) {
    is Calendrier.Hebdomadaire -> c.jours.sorted().map { t.texte(R.string.jour_de_la_semaine, nomDuJour(t, it)) }
    is Calendrier.Mensuel -> c.jours.sorted().map {
        if (it == Calendrier.Mensuel.DERNIER_JOUR) t.texte(R.string.le_dernier_jour)
        else t.pluriel(R.plurals.jour_du_mois, it, it)
    }
    is Calendrier.MensuelParRang ->
        c.positions.sortedWith(compareBy<Position>({ if (it.rang == Position.DERNIER) 5 else it.rang }, { it.jour }))
            .map { t.texte(R.string.position, libelleRang(t, it.rang), nomDuJour(t, it.jour)) }
    is Calendrier.Annuel -> c.dates.sorted().map {
        t.texte(R.string.date_annuelle, numeroDuJour(t, it.dayOfMonth), nomDuMois(t, it.month))
    }
}

/** « 1er », « 2e », « dernier » ; « first », « second », « last ». */
fun libelleRang(t: Textes, rang: Int): String = t.texte(
    when (rang) {
        Position.DERNIER -> R.string.rang_dernier
        1 -> R.string.rang_1
        2 -> R.string.rang_2
        3 -> R.string.rang_3
        else -> R.string.rang_4
    }
)

/** « 9 h », « 18 h 30 » ; « 9:00 », « 18:30 ». */
fun libelleHeure(t: Textes, heure: LocalTime): String =
    if (heure.minute == 0) t.texte(R.string.heure_pile, heure.hour)
    else t.texte(R.string.heure_minutes, heure.hour, heure.minute)

fun nomDuJour(t: Textes, jour: DayOfWeek): String = jour.getDisplayName(TextStyle.FULL, t.locale)

internal fun nomDuMois(t: Textes, mois: Month): String = mois.getDisplayName(TextStyle.FULL, t.locale)

/** « 1er », « 15 » ; en anglais, le nombre seul. */
internal fun numeroDuJour(t: Textes, jour: Int): String = t.pluriel(R.plurals.numero_jour, jour, jour)

/** « le 1er », « le 1er et le 15 », « le 1er, le 15 et le dernier jour ». */
private fun enumerer(t: Textes, elements: List<String>): String =
    if (elements.size <= 1) elements.joinToString()
    else t.texte(R.string.liste_et, elements.dropLast(1).joinToString(", "), elements.last())

/** « 1er nov. », ou « mardi 1er déc. » avec le jour de la semaine ; « Nov 1 », « Tuesday, Dec 1 ». */
fun dateCourte(t: Textes, instant: Instant, zone: ZoneId, avecJourDeSemaine: Boolean = false): String {
    val date = instant.atZone(zone)
    val numero = numeroDuJour(t, date.dayOfMonth)
    val mois = DateTimeFormatter.ofPattern("MMM", t.locale).format(date)
    return if (avecJourDeSemaine) t.texte(R.string.date_courte_jour, nomDuJour(t, date.dayOfWeek), numero, mois)
    else t.texte(R.string.date_courte, numero, mois)
}

/**
 * L'echeance d'un Rendez-vous dans la liste : le delai, puis la date. « Dans 24 jours
 * · mardi 1er déc. », « En retard de 4 jours · depuis le 1er nov. », et en tenant le
 * compte des oublis, « 2 oublis · depuis le 1er oct. ».
 */
internal fun Tache.libelleEcheanceRendezVous(t: Textes, rdv: Rythme.RendezVous, maintenant: Instant): String {
    val due = dueDepuis(maintenant) ?: return t.texte(
        R.string.echeance_et_date,
        etatEcheance(maintenant).libelle(t),
        dateCourte(t, echeance, rdv.zone, avecJourDeSemaine = true),
    )
    val oublis = oublis(maintenant)
    val tete = if (oublis > 1) t.pluriel(R.plurals.oublis, oublis, oublis) else etatEcheance(maintenant).libelle(t)
    return t.texte(R.string.echeance_depuis, tete, dateCourte(t, due, rdv.zone))
}

/**
 * Une ligne d'historique : l'ecart seul pour un Ricochet, l'occurrence soldee en tete
 * pour un Rendez-vous — « Pour le 1er nov. · en avance de 3 jours » —, et « Avant ce
 * calendrier » pour un *Fait* anterieur au Rendez-vous, qui ne soldait rien.
 */
fun libelleAccomplissement(t: Textes, accompli: Accomplissement, zone: ZoneId = ZoneId.systemDefault()): String {
    val ecart = libelleEcart(t, accompli.ecart ?: return t.texte(R.string.avant_ce_calendrier))
    val pour = accompli.pour ?: return ecart
    return t.texte(R.string.pour_le, dateCourte(t, pour, zone), ecart.replaceFirstChar { it.lowercase(t.locale) })
}
