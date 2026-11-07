package io.github.ginkoaloe.ricochet.domaine

import java.time.Instant
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration

/**
 * Un accomplissement passe, et son ecart a l'echeance qu'il soldait.
 *
 * @property ecart positif en cas de retard, negatif en cas d'avance ; `null` pour un
 *   accomplissement d'avant le calendrier d'un Rendez-vous, qui ne soldait rien.
 * @property pour l'occurrence soldee, pour un Rendez-vous ; `null` pour un Ricochet,
 *   dont l'echeance n'existe que par l'accomplissement precedent.
 */
data class Accomplissement(val quand: Instant, val ecart: Duration?, val pour: Instant? = null)

/**
 * L'historique relu, **du plus recent au plus ancien** : chaque accomplissement avec
 * son ecart a l'echeance d'alors.
 *
 * Pour un Ricochet, l'echeance d'alors partait du precedent — ou de la creation pour
 * le premier — et se calcule avec la periode **actuelle**, la seule que le modele
 * garde : l'ecart est faux pour les accomplissements anterieurs a un changement de
 * periode. Pour un Rendez-vous, l'ecart est **exact** : il se mesure a l'occurrence
 * soldee, que le calendrier donne.
 */
fun Tache.accomplissements(): List<Accomplissement> {
    val suivi = suivi
    val liste = if (suivi != null) {
        suivi.soldees.map { (quand, rang) ->
            if (rang == null) return@map Accomplissement(quand, ecart = null)
            val pour = suivi.occurrence(rang)
            Accomplissement(quand, java.time.Duration.between(pour, quand).toKotlinDuration(), pour)
        }
    } else {
        val tries = historique.sorted()
        tries.mapIndexed { rang, quand ->
            val depart = if (rang == 0) creee else tries[rang - 1]
            val echeance = depart.plus(intervalle.toJavaDuration())
            Accomplissement(quand, java.time.Duration.between(echeance, quand).toKotlinDuration())
        }
    }
    return liste.asReversed()
}
