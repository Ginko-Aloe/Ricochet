package io.github.ginkoaloe.ricochet.donnees

import io.github.ginkoaloe.ricochet.domaine.Calendrier
import io.github.ginkoaloe.ricochet.domaine.Position
import io.github.ginkoaloe.ricochet.domaine.Rythme
import io.github.ginkoaloe.ricochet.domaine.Tache
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.MonthDay
import java.time.ZoneId
import kotlin.time.Duration.Companion.seconds

/**
 * La forme d'une tache sur le disque.
 *
 * Volontairement distincte de [Tache] : le format de stockage doit pouvoir evoluer
 * sans contraindre le domaine, et l'inverse. C'est aussi ce qui evite d'ecrire des
 * serialiseurs sur mesure pour `Duration` et `Instant`.
 *
 * Les durees sont en secondes et les instants en ISO-8601, pour qu'un humain puisse
 * relire le fichier — l'export est un objectif declare.
 *
 * Un Ricochet porte [periodeSecondes], un Rendez-vous porte [rendezVous]. Les
 * fichiers ecrits avant les Rendez-vous n'ont que le premier : ils se relisent tels
 * quels.
 */
@Serializable
internal data class TacheEnregistree(
    val id: String,
    val nom: String,
    val periodeSecondes: Long? = null,
    val rendezVous: RendezVousEnregistre? = null,
    val historique: List<String> = emptyList(),
    val image: String? = null,
    /** Absent des fichiers ecrits avant l'introduction de la date de creation. */
    val creee: String? = null,
)

/**
 * Un Rendez-vous sur le disque, a plat et lisible : `"calendrier": "mois"`,
 * `"jours": [1, 15]`, `"heure": "09:00"`.
 *
 * @property jours jours de la semaine (1 = lundi, ISO) ou du mois (1 a 31).
 * @property rangs « rang:jour », ex. `"1:1"` pour le premier lundi, `"-1:5"` pour le
 *   dernier vendredi.
 * @property dates dates annuelles au format ISO `--MM-JJ`.
 */
@Serializable
internal data class RendezVousEnregistre(
    val calendrier: String,
    val tous: Int = 1,
    val jours: List<Int> = emptyList(),
    val rangs: List<String> = emptyList(),
    val dates: List<String> = emptyList(),
    val heure: String = "09:00",
    val tenirLeCompteDesOublis: Boolean = false,
    val zone: String,
    val reference: String? = null,
)

internal fun Tache.enregistrable(): TacheEnregistree = TacheEnregistree(
    id = id,
    nom = nom,
    periodeSecondes = (rythme as? Rythme.Ricochet)?.periode?.inWholeSeconds,
    rendezVous = (rythme as? Rythme.RendezVous)?.enregistrable(),
    historique = historique.map { it.toString() },
    image = image,
    creee = creee.toString(),
)

private fun Rythme.RendezVous.enregistrable(): RendezVousEnregistre {
    val base = RendezVousEnregistre(
        calendrier = "",
        tous = calendrier.tous,
        heure = heure.toString(),
        tenirLeCompteDesOublis = tenirLeCompteDesOublis,
        zone = zone.id,
        reference = reference?.toString(),
    )
    return when (val c = calendrier) {
        is Calendrier.Hebdomadaire -> base.copy(calendrier = SEMAINE, jours = c.jours.map { it.value }.sorted())
        is Calendrier.Mensuel -> base.copy(calendrier = MOIS, jours = c.jours.sorted())
        is Calendrier.MensuelParRang ->
            base.copy(calendrier = RANG, rangs = c.positions.map { "${it.rang}:${it.jour.value}" }.sorted())
        is Calendrier.Annuel -> base.copy(calendrier = AN, dates = c.dates.map { it.toString() }.sorted())
    }
}

/**
 * @throws IllegalArgumentException si l'enregistrement viole les regles du domaine
 *   (nom vide, periode nulle, calendrier vide ou inconnu). C'est voulu : [DepotTaches]
 *   s'en sert pour ecarter un fichier corrompu plutot que de laisser passer une
 *   tache absurde.
 */
internal fun TacheEnregistree.versDomaine(): Tache {
    val accomplissements = historique.map { Instant.parse(it) }
    require(periodeSecondes == null || rendezVous == null) { "A la fois periode et rendez-vous" }
    val rythme = rendezVous?.versDomaine()
        ?: Rythme.Ricochet(requireNotNull(periodeSecondes) { "Ni periode ni rendez-vous" }.seconds)
    return Tache(
        nom = nom,
        rythme = rythme,
        historique = accomplissements,
        image = image,
        // Fichier anterieur a ce champ : la tache a ete creee au plus tard lors de
        // son premier accomplissement. Faute d'historique, on repart de maintenant,
        // ce qui lui donne une periode de repit plutot qu'un retard fictif.
        creee = creee?.let(Instant::parse) ?: accomplissements.minOrNull() ?: Instant.now(),
        id = id,
    )
}

private fun RendezVousEnregistre.versDomaine(): Rythme.RendezVous {
    val calendrier = when (calendrier) {
        SEMAINE -> Calendrier.Hebdomadaire(jours.map(DayOfWeek::of).toSet(), tous)
        MOIS -> Calendrier.Mensuel(jours.toSet(), tous)
        RANG -> Calendrier.MensuelParRang(
            rangs.map { it.split(':').let { (rang, jour) -> Position(rang.toInt(), DayOfWeek.of(jour.toInt())) } }.toSet(),
            tous,
        )
        AN -> Calendrier.Annuel(dates.map(MonthDay::parse).toSet(), tous)
        else -> throw IllegalArgumentException("Calendrier inconnu : $calendrier")
    }
    return Rythme.RendezVous(
        calendrier = calendrier,
        heure = LocalTime.parse(heure),
        tenirLeCompteDesOublis = tenirLeCompteDesOublis,
        zone = ZoneId.of(zone),
        reference = reference?.let(LocalDate::parse),
    )
}

private const val SEMAINE = "semaine"
private const val MOIS = "mois"
private const val RANG = "mois-rang"
private const val AN = "an"
