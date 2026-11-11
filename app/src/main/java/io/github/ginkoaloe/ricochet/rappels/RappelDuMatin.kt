package io.github.ginkoaloe.ricochet.rappels

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.ginkoaloe.ricochet.MainActivity
import io.github.ginkoaloe.ricochet.R
import io.github.ginkoaloe.ricochet.domaine.duJour
import io.github.ginkoaloe.ricochet.domaine.prochaineFois
import io.github.ginkoaloe.ricochet.donnees.depotTaches
import io.github.ginkoaloe.ricochet.donnees.reglages
import io.github.ginkoaloe.ricochet.ui.TextesAndroid
import java.time.Instant
import java.time.ZoneId

/**
 * Le rappel du matin : une notification qui annonce les taches de la journee.
 *
 * Il **s'ajoute** aux rappels de chaque tache, qui sonnent toujours a leur heure avec
 * *Fait* et le report. Lui ne porte pas d'action : il resume, et ouvre la liste.
 *
 * Une alarme exacte par jour, reposee a chaque reveil pour le lendemain, plutot
 * qu'une alarme repetitive : celles-ci sont inexactes depuis Android 4.4, et l'heure
 * choisie doit etre tenue.
 */
object RappelDuMatin {

    /**
     * Un canal a part, d'importance ordinaire : on peut le faire taire, ou le rendre
     * silencieux, sans toucher aux echeances.
     */
    const val CANAL = "matin"
    private const val ID_NOTIFICATION = 2
    private const val CODE_ALARME = 2

    /** Pose l'alarme du prochain matin si le rappel est actif, la retire sinon. */
    fun planifier(contexte: Context, maintenant: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()) {
        val alarmes = contexte.getSystemService(AlarmManager::class.java)
        val intention = intention(contexte)
        alarmes.cancel(intention)

        val reglages = reglages(contexte)
        if (!reglages.rappelDuMatin) return
        alarmes.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            prochaineFois(reglages.heureDuMatin, maintenant, zone).toEpochMilli(),
            intention,
        )
    }

    /** Annonce les taches du jour, s'il y en a. Une journee libre ne merite pas de notification. */
    fun notifier(contexte: Context, maintenant: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()) {
        val taches = depotTaches(contexte).charger().duJour(maintenant, zone)
        if (taches.isEmpty()) return

        val permise = ContextCompat.checkSelfPermission(contexte, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!permise) return

        creerCanal(contexte)
        val t = TextesAndroid(contexte.resources)
        val lignes = taches.map { ligneDuJour(t, it, maintenant, zone) }
        val style = NotificationCompat.InboxStyle().also { boite -> lignes.forEach { boite.addLine(it) } }

        val notification = NotificationCompat.Builder(contexte, CANAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titreDuJour(t, taches.size))
            // Replie, la notification montre les noms ; depliee, une ligne par tache.
            .setContentText(taches.joinToString(", ") { it.nom })
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(ouvrirApplication(contexte))
            .build()

        NotificationManagerCompat.from(contexte).notify(ID_NOTIFICATION, notification)
    }

    fun creerCanal(contexte: Context) {
        val canal = NotificationChannel(
            CANAL,
            contexte.getString(R.string.canal_matin_nom),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = contexte.getString(R.string.canal_matin_description) }
        contexte.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    private fun intention(contexte: Context): PendingIntent =
        PendingIntent.getBroadcast(
            contexte,
            CODE_ALARME,
            Intent(contexte, RecepteurRappel::class.java).setAction(RecepteurRappel.ACTION_MATIN),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun ouvrirApplication(contexte: Context): PendingIntent =
        PendingIntent.getActivity(
            contexte,
            CODE_ALARME,
            Intent(contexte, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
