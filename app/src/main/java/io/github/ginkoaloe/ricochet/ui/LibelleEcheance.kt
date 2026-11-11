package io.github.ginkoaloe.ricochet.ui

import io.github.ginkoaloe.ricochet.R
import io.github.ginkoaloe.ricochet.domaine.EtatEcheance
import io.github.ginkoaloe.ricochet.domaine.Rythme
import io.github.ginkoaloe.ricochet.domaine.Tache
import io.github.ginkoaloe.ricochet.domaine.etatEcheance
import java.time.Instant
import kotlin.time.Duration

/**
 * Met un [EtatEcheance] en mots, dans la langue de [t].
 *
 * En Kotlin pur, a travers [Textes] : une fonction se teste sans emulateur, et les
 * tests verifient les vrais fichiers de traduction.
 */
fun EtatEcheance.libelle(t: Textes): String = when (this) {
    // Des le lendemain, en jours du calendrier : le libelle ne bouge plus de la journee.
    is EtatEcheance.AVenir -> when {
        jours == 1 -> t.texte(R.string.demain)
        jours > 1 -> t.texte(R.string.dans, t.pluriel(R.plurals.duree_jours, jours, jours))
        else -> t.texte(R.string.dans, duree(t, dans))
    }
    is EtatEcheance.EnRetard ->
        // En deca de la minute, annoncer un retard serait pedant : c'est maintenant.
        if (depuis.inWholeMinutes < 1) t.texte(R.string.a_faire_maintenant)
        else t.texte(R.string.en_retard_de, duree(t, depuis))
}

/**
 * Le libelle complet d'une tache dans la liste : son echeance, et la mention
 * « jamais faite » tant qu'elle n'a pas ete accomplie une premiere fois — une
 * information que l'echeance seule ne porte plus.
 *
 * Un Rendez-vous dit plutot sa date, qui compte davantage : voir
 * [libelleEcheanceRendezVous].
 */
fun Tache.libelleEcheance(t: Textes, maintenant: Instant): String {
    (rythme as? Rythme.RendezVous)?.let { return libelleEcheanceRendezVous(t, it, maintenant) }
    val echeance = etatEcheance(maintenant).libelle(t)
    return if (jamaisAccomplie) t.texte(R.string.echeance_jamais_faite, echeance) else echeance
}

/** `true` si l'echeance appelle une mise en evidence visuelle. */
val EtatEcheance.estPressant: Boolean
    get() = this is EtatEcheance.EnRetard

/**
 * Une duree dans son unite la plus parlante : on ne dit pas « dans 72 heures »
 * quand on peut dire « dans 3 jours ». L'arrondi se fait vers le bas, par troncature.
 */
internal fun duree(t: Textes, d: Duration): String = when {
    d.inWholeDays >= 1 -> d.inWholeDays.toInt().let { t.pluriel(R.plurals.duree_jours, it, it) }
    d.inWholeHours >= 1 -> d.inWholeHours.toInt().let { t.pluriel(R.plurals.duree_heures, it, it) }
    d.inWholeMinutes >= 1 -> d.inWholeMinutes.toInt().let { t.pluriel(R.plurals.duree_minutes, it, it) }
    else -> t.texte(R.string.moins_d_une_minute)
}
