package io.github.ginkoaloe.ricochet.rappels

import io.github.ginkoaloe.ricochet.R
import io.github.ginkoaloe.ricochet.ui.Textes
import io.github.ginkoaloe.ricochet.ui.duree
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * De combien « Plus tard » repousse un rappel.
 *
 * Proportionnel a la periode, parce qu'un report d'une heure n'a pas le meme sens
 * pour une pilule toutes les huit heures et pour un appel tous les quinze jours.
 * Un huitieme de la periode, borne des deux cotes : jamais si court que le rappel
 * revienne aussitot, jamais si long qu'on l'oublie.
 *
 * Le report **ne touche pas a l'historique** : la tache reste en retard, seule
 * l'alarme est redonnee plus tard. Seul « Fait » fait ricocher l'echeance.
 */
fun dureeDeReport(periode: Duration): Duration =
    (periode / 8).coerceIn(15.minutes, 12.hours)

/** Le libelle de l'action, pour que le bouton annonce ce qu'il fait : « Dans 1 heure ». */
fun libelleDeReport(t: Textes, periode: Duration): String = t.texte(R.string.dans, duree(t, dureeDeReport(periode)))
