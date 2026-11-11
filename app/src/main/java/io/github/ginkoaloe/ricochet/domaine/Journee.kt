package io.github.ginkoaloe.ricochet.domaine

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.time.toJavaDuration

/**
 * Ce que le rappel du matin annonce : les taches **en retard**, puis celles qui
 * tombent **d'ici minuit**, de la plus urgente a la moins urgente.
 *
 * Minuit s'entend dans [zone], celle du telephone : la journee est celle de
 * l'utilisateur.
 */
fun List<Tache>.duJour(maintenant: Instant, zone: ZoneId): List<Tache> {
    val minuit = minuit(maintenant, zone)
    return filter { it.echeance.isBefore(minuit) }.parUrgence(maintenant)
}

/**
 * Combien de fois cette tache tombe d'ici minuit, la prochaine comprise : une pilule
 * toutes les 2 heures, due a 14 h, tombe a 14 h, 16 h, 18 h, 20 h et 22 h — 5 fois.
 *
 * Au-dela de la prochaine, ce sont des **projections**, comme au calendrier : la tache
 * supposee faite a l'heure, ou des maintenant si elle est en retard. Un Rendez-vous
 * compte ses occurrences du jour.
 */
fun Tache.foisAujourdhui(maintenant: Instant, zone: ZoneId): Int {
    val minuit = minuit(maintenant, zone)
    if (!echeance.isBefore(minuit)) return 0
    val due = estDue(maintenant)
    val suivantes = when (val r = rythme) {
        is Rythme.Ricochet -> {
            val pas = r.periode.toJavaDuration()
            generateSequence((if (due) maintenant else echeance).plus(pas)) { it.plus(pas) }
                .takeWhile { it.isBefore(minuit) }.count()
        }
        // Celles d'apres maintenant ; la prochaine en est, si la tache n'est pas due.
        is Rythme.RendezVous -> suivi!!.let { s ->
            generateSequence(s.prochaine) { it + 1 }.map(s::occurrence)
                .dropWhile { !it.isAfter(maintenant) }
                .takeWhile { it.isBefore(minuit) }.count()
        }.let { if (due) it else it - 1 }
    }
    return 1 + suivantes
}

private fun minuit(maintenant: Instant, zone: ZoneId): Instant =
    maintenant.atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant()

/**
 * Le prochain instant ou il est [heure] dans [zone], strictement apres [maintenant] :
 * aujourd'hui si l'heure n'est pas encore passee, demain sinon.
 *
 * Un jour de changement d'heure, une heure qui n'existe pas (2 h 30 au printemps)
 * glisse a la suivante, comme le fait `java.time`.
 */
fun prochaineFois(heure: LocalTime, maintenant: Instant, zone: ZoneId): Instant {
    val aujourdhui = maintenant.atZone(zone).toLocalDate()
    val ceJour = aujourdhui.atTime(heure).atZone(zone).toInstant()
    return if (ceJour.isAfter(maintenant)) ceJour
    else aujourdhui.plusDays(1).atTime(heure).atZone(zone).toInstant()
}
