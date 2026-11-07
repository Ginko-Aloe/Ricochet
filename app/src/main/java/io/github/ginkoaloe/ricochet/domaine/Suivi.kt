package io.github.ginkoaloe.ricochet.domaine

import java.time.Instant

/**
 * Ce que l'historique d'un Rendez-vous a solde. **Jamais stocke** : on le recalcule
 * a partir de l'historique et du calendrier, si bien qu'une correction d'historique
 * reste juste sans rien de plus.
 *
 * Chaque *Fait*, dans l'ordre, solde une occurrence :
 * - par defaut, **la plus proche** — une fenetre d'une demi-intervalle de chaque cote,
 *   d'ou le loyer paye le 28 qui compte pour le 1er ; et si elle l'est deja, **la
 *   suivante**, pour qu'on puisse toujours faire en avance. Les occurrences sautees
 *   sont **absorbees** : deux mois manques, un seul *Fait* remet a jour ;
 * - avec « Tenir le compte des oublis », **la plus ancienne non soldee** : chaque
 *   oubli est une dette, rattrapee dans l'ordre.
 *
 * Les deux regles se disent d'un trait : le *Fait* solde la prochaine occurrence non
 * soldee, sauf qu'en mode absorbe il saute en avant jusqu'a la plus proche.
 *
 * **Le calendrier commence a sa reference** — sa premiere occurrence quand il a ete
 * cree ou modifie —, pas a la creation de la tache. Sans quoi un vieux Ricochet
 * devenu Rendez-vous, ou un *Fait* antidate qui fait reculer la creation, ferait
 * surgir des dizaines d'occurrences jamais soldees. Un *Fait* anterieur a la fenetre
 * de la premiere occurrence ne solde donc rien : il date d'avant ce calendrier.
 */
internal class Suivi(
    private val rendezVous: Rythme.RendezVous,
    creee: Instant,
    historique: List<Instant>,
) {
    private val iterateur = rendezVous
        .occurrences(rendezVous.reference?.atStartOfDay(rendezVous.zone)?.toInstant() ?: creee)
        .iterator()
    private val vues = ArrayList<Instant>()

    /** La [rang]-ieme occurrence du calendrier, en partant de 0. */
    @Synchronized
    fun occurrence(rang: Int): Instant {
        while (vues.size <= rang) vues += iterateur.next()
        return vues[rang]
    }

    /**
     * Pour chaque accomplissement, dans l'ordre chronologique, le rang de l'occurrence
     * soldee — ou `null` s'il date d'avant ce calendrier.
     */
    val soldees: List<Pair<Instant, Int?>>

    /** Le rang de la premiere occurrence non soldee. */
    val prochaine: Int

    init {
        val debutFenetre = occurrence(0).minus(java.time.Duration.between(occurrence(0), occurrence(1)).dividedBy(2))
        var p = 0
        soldees = historique.sorted().map { quand ->
            if (quand.isBefore(debutFenetre)) return@map quand to null
            val proche = plusProche(quand)
            val soldee = if (!rendezVous.tenirLeCompteDesOublis && proche > p) proche else p
            p = soldee + 1
            quand to soldee
        }
        prochaine = p
    }

    /** Le rang de l'occurrence la plus proche de [instant] ; a egalite, la suivante. */
    private fun plusProche(instant: Instant): Int {
        var k = 0
        while (occurrence(k).isBefore(instant)) k++
        if (k == 0) return 0
        val avant = java.time.Duration.between(occurrence(k - 1), instant)
        val apres = java.time.Duration.between(instant, occurrence(k))
        return if (avant < apres) k - 1 else k
    }
}
