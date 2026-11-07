package io.github.ginkoaloe.ricochet.rappels

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.github.ginkoaloe.ricochet.domaine.Tache
import java.time.Instant

/**
 * Pose une alarme exacte par tache, a l'instant de son echeance.
 *
 * Une alarme exacte plutot qu'un sondage periodique : WaterMe se reveille toutes les
 * douze heures et previent quand ca tombe, ce qui interdit de choisir l'heure et
 * laisse le Doze repousser l'alerte. Ici l'echeance est connue a la seconde, autant
 * la demander telle quelle.
 *
 * **Seules les echeances a venir sont armees.** Une echeance deja passee a deja
 * declenche son alarme, et la liste la montre en retard ; la reposer a chaque
 * ouverture de l'application reviendrait a harceler. Le rattrapage apres une
 * extinction du telephone est le travail de [RecepteurDemarrage].
 *
 * Un Rendez-vous en retard, lui, sonne encore a sa prochaine occurrence : le 1er du
 * mois revient. C'est [Tache.prochainRappel] qui le dit.
 */
class Planificateur(private val contexte: Context) {

    private val alarmes = contexte.getSystemService(AlarmManager::class.java)

    /** Reprend toutes les alarmes : celles qui n'ont plus lieu d'etre, et les autres. */
    fun replanifier(taches: List<Tache>, maintenant: Instant = Instant.now()) {
        taches.forEach { tache ->
            annuler(tache.id)
            tache.prochainRappel(maintenant)?.let { armer(tache.id, it) }
        }
    }

    fun armer(idTache: String, quand: Instant) {
        alarmes.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            quand.toEpochMilli(),
            intentionPour(idTache, PendingIntent.FLAG_UPDATE_CURRENT),
        )
    }

    fun annuler(idTache: String) {
        alarmes.cancel(intentionPour(idTache, PendingIntent.FLAG_UPDATE_CURRENT))
    }

    private fun intentionPour(idTache: String, drapeaux: Int): PendingIntent {
        val intention = Intent(contexte, RecepteurRappel::class.java).apply {
            action = RecepteurRappel.ACTION_ECHEANCE
            putExtra(RecepteurRappel.EXTRA_ID, idTache)
        }
        return PendingIntent.getBroadcast(
            contexte,
            // Un code par tache : c'est lui, et non les extras, qui distingue
            // deux alarmes aux yeux du systeme.
            idTache.hashCode(),
            intention,
            drapeaux or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
