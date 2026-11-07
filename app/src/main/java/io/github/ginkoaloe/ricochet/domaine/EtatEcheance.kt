package io.github.ginkoaloe.ricochet.domaine

import java.time.Instant
import kotlin.time.Duration

/**
 * Ou en est une tache vis-a-vis de son echeance.
 *
 * Volontairement depourvu de toute mise en forme : c'est la couche d'interface qui
 * decide des mots et des couleurs. Le domaine se contente de dire l'etat, ce qui
 * le laisse testable sans Android.
 *
 * Il n'y a pas de cas « jamais accomplie » : une tache neuve a une echeance comme
 * les autres, comptee depuis sa creation. Que l'historique soit vide se lit sur
 * [Tache.jamaisAccomplie], et ne regarde que l'affichage.
 */
sealed interface EtatEcheance {

    /** Pas encore due. [dans] est strictement positif. */
    data class AVenir(val dans: Duration) : EtatEcheance

    /** Due ou depassee. [depuis] vaut zero a l'instant precis de l'echeance. */
    data class EnRetard(val depuis: Duration) : EtatEcheance
}

/** L'etat de cette tache a l'instant [maintenant]. */
fun Tache.etatEcheance(maintenant: Instant): EtatEcheance {
    val restant = tempsRestant(maintenant)
    return if (restant.isPositive()) EtatEcheance.AVenir(restant)
    else EtatEcheance.EnRetard(-restant)
}

/**
 * Les taches de la plus urgente a la moins urgente : les retards du plus ancien au
 * plus recent, puis les echeances a venir de la plus proche a la plus lointaine.
 *
 * Le tri est stable, donc deux taches a egalite gardent leur ordre d'origine.
 */
fun List<Tache>.parUrgence(maintenant: Instant): List<Tache> =
    sortedBy { it.tempsRestant(maintenant).inWholeSeconds }
