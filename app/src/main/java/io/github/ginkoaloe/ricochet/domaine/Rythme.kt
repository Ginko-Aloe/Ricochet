package io.github.ginkoaloe.ricochet.domaine

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.time.Duration

/**
 * Ce qui decide quand une tache redevient due.
 */
sealed interface Rythme {

    /**
     * L'echeance **glisse** : elle repart du dernier accomplissement, une [periode]
     * plus tard. C'est l'idee meme de l'application.
     */
    data class Ricochet(val periode: Duration) : Rythme {
        init {
            require(periode.isPositive()) {
                "La periode doit etre strictement positive, recu : $periode"
            }
        }
    }

    /**
     * L'echeance suit le **calendrier** : « le 1er du mois a 9 h ». Le prochain 1er
     * arrive que la tache ait ete faite ou non.
     *
     * @property zone le fuseau ou se lit le calendrier : « 9 h » reste 9 h apres un
     *   changement d'heure.
     * @property reference un jour de la periode qui compte quand [Calendrier.tous]
     *   vaut plus de 1 : « toutes les 2 semaines » a partir de quelle semaine. Fixee a
     *   la creation, pour que les corrections d'historique ne la deplacent pas ; a
     *   defaut, la periode de la premiere occurrence.
     */
    data class RendezVous(
        val calendrier: Calendrier,
        val heure: LocalTime = LocalTime.of(9, 0),
        val tenirLeCompteDesOublis: Boolean = false,
        val zone: ZoneId = ZoneId.systemDefault(),
        val reference: LocalDate? = null,
    ) : Rythme
}

/**
 * Le meme rendez-vous, sa [Rythme.RendezVous.reference] fixee a sa premiere occurrence
 * apres [apres]. A appeler a la creation, et a chaque changement de calendrier.
 */
fun Rythme.RendezVous.ancre(apres: Instant): Rythme.RendezVous =
    copy(reference = copy(reference = null).premiereOccurrence(apres).atZone(zone).toLocalDate())

/** La premiere occurrence a partir de [apres] : l'apercu de l'editeur. */
fun Rythme.RendezVous.premiereOccurrence(apres: Instant): Instant = occurrences(apres).first()

/** Toutes les occurrences a partir de [depuis] inclus, dans l'ordre. Suite infinie. */
internal fun Rythme.RendezVous.occurrences(depuis: Instant): Sequence<Instant> {
    fun instants(periode: Long) = calendrier.joursDe(periode)
        .map { LocalDateTime.of(it, heure).atZone(zone).toInstant() }

    val premiere = calendrier.periodeDe(depuis.atZone(zone).toLocalDate())
    val ancre = reference?.let(calendrier::periodeDe)
        ?: generateSequence(premiere) { it + 1 }.first { p -> instants(p).any { !it.isBefore(depuis) } }
    val tous = calendrier.tous.toLong()

    return generateSequence(premiere) { it + 1 }
        .filter { Math.floorMod(it - ancre, tous) == 0L }
        .flatMap { instants(it) }
        .filter { !it.isBefore(depuis) }
}
