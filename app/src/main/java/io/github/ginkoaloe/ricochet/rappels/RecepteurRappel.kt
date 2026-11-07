package io.github.ginkoaloe.ricochet.rappels

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.ginkoaloe.ricochet.donnees.depotTaches
import java.time.Instant
import kotlin.time.toJavaDuration

/**
 * Ce qui arrive quand une echeance tombe, et ce que font les boutons de la
 * notification.
 *
 * Les actions agissent **sans ouvrir l'application** : c'est tout l'interet par
 * rapport a WaterMe, qui oblige a entrer dans l'app pour un geste quotidien.
 *
 * Les lectures et ecritures sont synchrones : le fichier des taches pese quelques
 * lignes, et un recepteur dispose largement du temps qu'il faut pour cela.
 */
class RecepteurRappel : BroadcastReceiver() {

    override fun onReceive(contexte: Context, intention: Intent) {
        val id = intention.getStringExtra(EXTRA_ID) ?: return
        val depot = depotTaches(contexte)
        val taches = depot.charger()
        val tache = taches.firstOrNull { it.id == id } ?: return
        val planificateur = Planificateur(contexte)

        when (intention.action) {
            ACTION_ECHEANCE -> {
                Rappels.notifier(contexte, tache)
                // Un Rendez-vous sonne encore a sa prochaine occurrence, meme sans
                // reponse a celle-ci. Pour un Ricochet en retard, rien n'est repose.
                planificateur.replanifier(listOf(tache))
            }

            ACTION_FAIT -> {
                val faite = tache.accomplir(Instant.now())
                depot.enregistrer(taches.map { if (it.id == id) faite else it })
                Rappels.retirer(contexte, id)
                // L'echeance a ricoche : la prochaine alarme se pose d'ici une periode.
                planificateur.replanifier(listOf(faite))
            }

            ACTION_REPORT -> {
                // Un report ne touche pas a l'historique : la tache reste en retard,
                // seule l'alarme est redonnee plus tard.
                Rappels.retirer(contexte, id)
                planificateur.armer(
                    id,
                    Instant.now().plus(dureeDeReport(tache.intervalle).toJavaDuration()),
                )
            }
        }
    }

    companion object {
        const val ACTION_ECHEANCE = "io.github.ginkoaloe.ricochet.ECHEANCE"
        const val ACTION_FAIT = "io.github.ginkoaloe.ricochet.FAIT"
        const val ACTION_REPORT = "io.github.ginkoaloe.ricochet.REPORT"
        const val EXTRA_ID = "idTache"
    }
}
