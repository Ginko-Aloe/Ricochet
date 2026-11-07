package io.github.ginkoaloe.ricochet.rappels

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.ginkoaloe.ricochet.donnees.depotTaches
import java.time.Instant

/**
 * Les alarmes ne survivent pas a l'extinction du telephone : il faut les reposer.
 *
 * Et rattraper ce qui s'est passe pendant ce temps — une echeance echue alors que
 * l'appareil etait eteint n'a declenche aucune alarme, et serait perdue sans cela.
 */
class RecepteurDemarrage : BroadcastReceiver() {

    override fun onReceive(contexte: Context, intention: Intent) {
        if (intention.action != Intent.ACTION_BOOT_COMPLETED) return

        val taches = depotTaches(contexte).charger()
        val maintenant = Instant.now()

        Planificateur(contexte).replanifier(taches, maintenant)
        taches.filter { it.estDue(maintenant) }
            .forEach { Rappels.notifier(contexte, it, maintenant) }
    }
}
