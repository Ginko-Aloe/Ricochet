package io.github.ginkoaloe.ricochet.domaine

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

/** Ce qu'une marque du calendrier dit d'une tache, un jour donne. */
enum class SorteDeMarque {
    /** Une echeance sure : la prochaine d'un Ricochet, une occurrence d'un Rendez-vous. */
    ECHEANCE,

    /** Une echeance supposee : un Ricochet fait a l'heure tomberait la. */
    PROJECTION,

    /** Une tache due, montree **aujourd'hui** — la ou elle doit etre traitee. */
    RETARD,

    /** Un accomplissement passe. */
    FAIT,
}

/**
 * Une marque du calendrier.
 *
 * @property quand l'instant marque ; pour un retard, la date due, a rappeler dans le libelle.
 * @property oublis pour un retard, combien d'occurrences attendent (au moins 1).
 */
data class Marque(val tache: Tache, val sorte: SorteDeMarque, val quand: Instant, val oublis: Int = 0)

/**
 * Ce qui tombe chaque jour de [du] a [au] inclus, vu a [maintenant]. Les jours vides
 * sont absents ; les marques d'un jour sont triees par heure, les retards d'abord.
 *
 * - Les **Faits** passes, a leur jour.
 * - Les **retards** — et les oublis comptes — **aujourd'hui**, pas a la date manquee.
 * - Les **Rendez-vous** : leurs occurrences a venir non soldees.
 * - Les **Ricochets** : la prochaine echeance si elle est a venir, puis des
 *   **projections** une periode apres l'autre, en supposant la tache faite a l'heure
 *   — a partir de maintenant si elle est en retard. Sous un jour, on ne projette pas :
 *   une pilule toutes les 8 h inonderait la grille.
 */
fun List<Tache>.agenda(du: LocalDate, au: LocalDate, maintenant: Instant, zone: ZoneId): Map<LocalDate, List<Marque>> {
    val fin = au.plusDays(1).atStartOfDay(zone).toInstant()
    fun jourDe(instant: Instant): LocalDate = instant.atZone(zone).toLocalDate()
    fun dansLaPeriode(instant: Instant) = jourDe(instant).let { !it.isBefore(du) && !it.isAfter(au) }

    val marques = flatMap { tache ->
        buildList {
            tache.historique.filter(::dansLaPeriode).forEach { add(Marque(tache, SorteDeMarque.FAIT, it)) }

            val due = tache.dueDepuis(maintenant)
            if (due != null && dansLaPeriode(maintenant)) {
                add(Marque(tache, SorteDeMarque.RETARD, due, tache.oublis(maintenant)))
            }

            when (val rythme = tache.rythme) {
                is Rythme.RendezVous -> {
                    val suivi = tache.suivi!!
                    generateSequence(suivi.prochaine) { it + 1 }.map(suivi::occurrence)
                        .filter { it.isAfter(maintenant) }
                        .takeWhile { it.isBefore(fin) }
                        .filter(::dansLaPeriode)
                        .forEach { add(Marque(tache, SorteDeMarque.ECHEANCE, it)) }
                }
                is Rythme.Ricochet -> {
                    val pas = rythme.periode.toJavaDuration()
                    val depart = if (due == null) tache.echeance.also {
                        if (dansLaPeriode(it)) add(Marque(tache, SorteDeMarque.ECHEANCE, it))
                    } else maintenant
                    if (rythme.periode >= 1.days) {
                        generateSequence(depart.plus(pas)) { it.plus(pas) }
                            .takeWhile { it.isBefore(fin) }
                            .filter(::dansLaPeriode)
                            .forEach { add(Marque(tache, SorteDeMarque.PROJECTION, it)) }
                    }
                }
            }
        }
    }

    return marques
        .groupBy { if (it.sorte == SorteDeMarque.RETARD) jourDe(maintenant) else jourDe(it.quand) }
        .mapValues { (_, duJour) ->
            duJour.sortedWith(compareBy<Marque>({ it.sorte != SorteDeMarque.RETARD }, { it.quand }))
        }
}
