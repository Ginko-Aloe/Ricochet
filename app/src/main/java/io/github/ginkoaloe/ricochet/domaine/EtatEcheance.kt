package io.github.ginkoaloe.ricochet.domaine

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
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

    /**
     * Pas encore due. [dans] est strictement positif.
     *
     * [jours] compte les **jours du calendrier** jusqu'a l'echeance : 0 si elle tombe
     * aujourd'hui, 1 si c'est demain, meme a 23 h pour 1 h du matin. C'est ce qu'on
     * affiche des le lendemain — compte en heures, « dans 3 jours » devenait « dans
     * 2 jours » entre deux ouvertures de l'application le meme jour. Par defaut, a
     * defaut de calendrier, les jours pleins de [dans].
     */
    data class AVenir(val dans: Duration, val jours: Int = dans.inWholeDays.toInt()) : EtatEcheance

    /** Due ou depassee. [depuis] vaut zero a l'instant precis de l'echeance. */
    data class EnRetard(val depuis: Duration) : EtatEcheance
}

/**
 * L'etat de cette tache a l'instant [maintenant]. Les jours se comptent dans [zone],
 * celle du telephone : « demain » est celui de l'utilisateur, pas celui du calendrier
 * d'un Rendez-vous.
 */
fun Tache.etatEcheance(maintenant: Instant, zone: ZoneId = ZoneId.systemDefault()): EtatEcheance {
    val restant = tempsRestant(maintenant)
    if (!restant.isPositive()) return EtatEcheance.EnRetard(-restant)
    val jours = ChronoUnit.DAYS.between(maintenant.atZone(zone).toLocalDate(), echeance.atZone(zone).toLocalDate())
    return EtatEcheance.AVenir(restant, jours.toInt())
}

/**
 * Les taches de la plus urgente a la moins urgente : les retards du plus ancien au
 * plus recent, puis les echeances a venir de la plus proche a la plus lointaine.
 *
 * Le tri est stable, donc deux taches a egalite gardent leur ordre d'origine.
 */
fun List<Tache>.parUrgence(maintenant: Instant): List<Tache> =
    sortedBy { it.tempsRestant(maintenant).inWholeSeconds }
