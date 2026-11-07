package io.github.ginkoaloe.ricochet.domaine

import java.time.Instant
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration

/**
 * Une tache recurrente dont l'echeance **glisse**.
 *
 * L'echeance se calcule a partir du dernier accomplissement, jamais de l'echeance
 * precedente. Accomplir une tache en retard repousse donc la suivante d'une periode
 * pleine : une tache de periode 3 jours, faite avec 2 jours de retard, est due
 * 3 jours plus tard — pas 1. C'est ce qui distingue Ricochet d'un agenda.
 *
 * Consequence directe, et voulue : **le retard est absorbe, jamais accumule**. Une
 * tache oubliee pendant un mois ne produit pas trente echeances en souffrance, mais
 * une seule.
 *
 * Tant qu'elle n'a jamais ete accomplie, c'est **sa creation** qui sert de point de
 * depart : creer « pilule toutes les 8 heures » a midi la rend due a 20 heures. Dire
 * qu'une tache neuve est due sur-le-champ serait plus simple, mais notifierait a
 * l'instant meme ou l'on finit d'en taper le nom.
 *
 * Les instants sont des [Instant] (`java.time`, disponible sans dependance des
 * l'API 26, bien en dessous de notre plancher 34). La periode est une [Duration] de
 * Kotlin, plus lisible a l'ecriture (`3.days`, `8.hours`) et assez fine pour le cas
 * de la pilule, que WaterMe ne sait pas exprimer.
 *
 * @property historique tous les accomplissements. L'ordre n'a pas d'importance :
 *   c'est toujours le plus recent qui fait foi, ce qui rend l'import de sauvegarde
 *   et la saisie retroactive sans danger.
 */
data class Tache(
    val nom: String,
    val rythme: Rythme,
    val historique: List<Instant> = emptyList(),
    val image: String? = null,
    val creee: Instant = Instant.now(),
    val id: String = UUID.randomUUID().toString(),
) {
    /** Un Ricochet de [periode] : la forme la plus courante, et la plus ancienne. */
    constructor(
        nom: String,
        periode: Duration,
        historique: List<Instant> = emptyList(),
        image: String? = null,
        creee: Instant = Instant.now(),
        id: String = UUID.randomUUID().toString(),
    ) : this(nom, Rythme.Ricochet(periode), historique, image, creee, id)

    init {
        require(nom.isNotBlank()) { "Une tache doit porter un nom." }
    }

    /** Le plus recent accomplissement, ou `null` si la tache n'a jamais ete faite. */
    val dernierAccomplissement: Instant?
        get() = historique.maxOrNull()

    val jamaisAccomplie: Boolean
        get() = historique.isEmpty()

    /** D'ou repart le decompte : le dernier accomplissement, ou a defaut la creation. */
    val depuis: Instant
        get() = dernierAccomplissement ?: creee

    /** Ce que l'historique a solde, pour un Rendez-vous. Calcule a la demande, une fois. */
    internal val suivi: Suivi? by lazy {
        (rythme as? Rythme.RendezVous)?.let { Suivi(it, creee, historique) }
    }

    /**
     * L'instant ou la tache redevient due : une periode apres le dernier
     * accomplissement pour un Ricochet, la premiere occurrence non soldee pour un
     * Rendez-vous.
     */
    val echeance: Instant
        get() = when (rythme) {
            is Rythme.Ricochet -> depuis.plus(rythme.periode.toJavaDuration())
            is Rythme.RendezVous -> suivi!!.let { it.occurrence(it.prochaine) }
        }

    /**
     * La duree typique entre deux echeances : de quoi proportionner un report.
     */
    val intervalle: Duration
        get() = when (rythme) {
            is Rythme.Ricochet -> rythme.periode
            is Rythme.RendezVous -> suivi!!.let {
                java.time.Duration.between(it.occurrence(it.prochaine), it.occurrence(it.prochaine + 1))
                    .toKotlinDuration()
            }
        }

    fun estDue(maintenant: Instant): Boolean = !maintenant.isBefore(echeance)

    /**
     * Le temps restant avant l'echeance. **Negatif en cas de retard**, ce qui permet
     * de trier une liste par urgence sans autre calcul.
     */
    fun tempsRestant(maintenant: Instant): Duration =
        java.time.Duration.between(maintenant, dueDepuis(maintenant) ?: echeance).toKotlinDuration()

    /**
     * Depuis quand la tache est due a [maintenant], ou `null` si elle ne l'est pas.
     *
     * Pour un Rendez-vous absorbe, c'est la **plus recente** occurrence manquee : les
     * precedentes sont pardonnees. En tenant le compte des oublis, c'est la plus
     * ancienne, celle qu'on rattrape d'abord.
     */
    fun dueDepuis(maintenant: Instant): Instant? {
        if (maintenant.isBefore(echeance)) return null
        val rdv = rythme as? Rythme.RendezVous ?: return echeance
        if (rdv.tenirLeCompteDesOublis) return echeance
        return echues(maintenant).last()
    }

    /**
     * Combien d'occurrences attendent d'etre soldees a [maintenant]. Au plus une, sauf
     * pour un Rendez-vous qui tient le compte des oublis.
     */
    fun oublis(maintenant: Instant): Int = when {
        maintenant.isBefore(echeance) -> 0
        (rythme as? Rythme.RendezVous)?.tenirLeCompteDesOublis == true -> echues(maintenant).size
        else -> 1
    }

    /**
     * Quand sonner le prochain rappel, ou `null` s'il n'y en a plus a poser.
     *
     * Un Ricochet en retard ne sonne plus : sa notification attend. Un Rendez-vous
     * sonne a chaque occurrence nouvelle, meme en retard — le 1er du mois revient.
     */
    fun prochainRappel(maintenant: Instant): Instant? = when (rythme) {
        is Rythme.Ricochet -> echeance.takeIf { it.isAfter(maintenant) }
        is Rythme.RendezVous -> suivi!!.let { s ->
            generateSequence(s.prochaine) { it + 1 }.map(s::occurrence).first { it.isAfter(maintenant) }
        }
    }

    /** Les occurrences non soldees deja passees a [maintenant], dans l'ordre. */
    private fun echues(maintenant: Instant): List<Instant> = suivi!!.let { s ->
        generateSequence(s.prochaine) { it + 1 }.map(s::occurrence).takeWhile { !it.isAfter(maintenant) }.toList()
    }

    /** Marque la tache comme faite, ce qui fait ricocher l'echeance d'une periode pleine. */
    fun accomplir(maintenant: Instant): Tache =
        copy(historique = (historique + maintenant).sorted())

    /**
     * Annule le dernier accomplissement et restaure donc l'echeance precedente.
     * Sans effet sur une tache jamais accomplie.
     *
     * Indispensable : une tape malencontreuse ne se contente pas de salir
     * l'historique, elle decale l'echeance.
     */
    fun annulerDernier(): Tache {
        val dernier = dernierAccomplissement ?: return this
        val restant = historique.toMutableList().apply { removeAt(lastIndexOf(dernier)) }
        return copy(historique = restant)
    }

    // ------------------------------------------- correction de l'historique

    /**
     * Enregistre un accomplissement a posteriori : « je l'ai fait hier ».
     *
     * Une date anterieure a la creation **fait reculer la creation** jusqu'a elle :
     * une habitude commencee avant l'application se rattrape ainsi sans que le
     * premier ecart, mesure depuis la creation, ne devienne une avance absurde.
     */
    fun ajouterAccomplissement(quand: Instant): Tache =
        copy(historique = (historique + quand).sorted(), creee = minOf(creee, quand))

    /**
     * Retire un accomplissement precis. Sans effet s'il n'y figure pas.
     *
     * La creation ne revient pas en avant, meme si elle avait recule pour cet
     * accomplissement : sa date d'origine n'est plus connue.
     */
    fun retirerAccomplissement(quand: Instant): Tache {
        val rang = historique.indexOf(quand)
        if (rang < 0) return this
        return copy(historique = historique.toMutableList().apply { removeAt(rang) })
    }

    /** Corrige la date d'un accomplissement. Sans effet s'il n'y figure pas. */
    fun deplacerAccomplissement(de: Instant, vers: Instant): Tache =
        if (de !in historique) this else retirerAccomplissement(de).ajouterAccomplissement(vers)
}
