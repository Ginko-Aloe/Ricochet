package io.github.ginkoaloe.ricochet.rappels

import io.github.ginkoaloe.ricochet.R
import io.github.ginkoaloe.ricochet.domaine.Tache
import io.github.ginkoaloe.ricochet.domaine.foisAujourdhui
import io.github.ginkoaloe.ricochet.ui.Textes
import io.github.ginkoaloe.ricochet.ui.libelleEcheance
import io.github.ginkoaloe.ricochet.ui.libelleHeure
import java.time.Instant
import java.time.ZoneId

/**
 * Les mots du rappel du matin. Comme les autres libelles, en Kotlin pur pour se
 * tester sans appareil.
 */

/** « 3 tâches aujourd'hui ». */
fun titreDuJour(t: Textes, nombre: Int): String = t.pluriel(R.plurals.taches_du_jour, nombre, nombre)

/**
 * Une ligne du recapitulatif : « Nourrir le chat · en retard de 1 heure »,
 * « Sortir les poubelles · à 19 h », « Pilule · à 14 h, puis encore 4 fois ».
 *
 * Pour une tache a venir, l'heure dit davantage que le delai : le matin, « dans
 * 11 heures » oblige a compter. Les fois suivantes ne sont que comptees, sans
 * heure : elles dependent de quand la tache sera faite.
 */
fun ligneDuJour(t: Textes, tache: Tache, maintenant: Instant, zone: ZoneId = ZoneId.systemDefault()): String {
    val quand = if (tache.estDue(maintenant)) tache.libelleEcheance(t, maintenant)
    else t.texte(R.string.a_heure, libelleHeure(t, tache.echeance.atZone(zone).toLocalTime()))
    val encore = tache.foisAujourdhui(maintenant, zone) - 1
    val tout = if (encore < 1) quand else t.pluriel(R.plurals.puis_encore, encore, quand, encore)
    return t.texte(R.string.ligne_du_jour, tache.nom, tout.replaceFirstChar { it.lowercase(t.locale) })
}
