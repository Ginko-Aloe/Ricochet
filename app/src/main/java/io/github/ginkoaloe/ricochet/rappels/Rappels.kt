package io.github.ginkoaloe.ricochet.rappels

import android.Manifest
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
import io.github.ginkoaloe.ricochet.domaine.Tache
import io.github.ginkoaloe.ricochet.ui.TextesAndroid
import io.github.ginkoaloe.ricochet.ui.libelleEcheance
import java.time.Instant

/**
 * Les notifications d'echeance.
 *
 * Une notification par tache, et non un compteur global comme WaterMe (« you have
 * 3 plants to water ») : seule une notification nommee peut porter un bouton qui
 * agit. Elles sont regroupees sous un resume, pour que trois retards simultanes
 * n'occupent pas tout le volet.
 */
object Rappels {

    const val CANAL = "echeances"
    private const val GROUPE = "io.github.ginkoaloe.ricochet.ECHEANCES"
    private const val ID_RESUME = 1

    /** L'accent de l'icone, reserve a l'urgence : une notification n'arrive que pour une tache due. */
    private const val OCRE = 0xFFC98A3C.toInt()

    fun creerCanal(contexte: Context) {
        val canal = NotificationChannel(
            CANAL,
            contexte.getString(R.string.canal_nom),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = contexte.getString(R.string.canal_description) }
        contexte.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    fun notifier(contexte: Context, tache: Tache, maintenant: Instant = Instant.now()) {
        creerCanal(contexte)
        val gestionnaire = NotificationManagerCompat.from(contexte)
        val t = TextesAndroid(contexte.resources)

        val notification = NotificationCompat.Builder(contexte, CANAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(OCRE)
            .setContentTitle(tache.nom)
            .setContentText(tache.libelleEcheance(t, maintenant))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setGroup(GROUPE)
            .setContentIntent(ouvrirApplication(contexte))
            .addAction(0, t.texte(R.string.fait), action(contexte, RecepteurRappel.ACTION_FAIT, tache.id))
            .addAction(
                0,
                libelleDeReport(t, tache.intervalle),
                action(contexte, RecepteurRappel.ACTION_REPORT, tache.id),
            )
            .build()

        val resume = NotificationCompat.Builder(contexte, CANAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(OCRE)
            .setContentTitle(t.texte(R.string.app_name))
            .setContentText(t.texte(R.string.resume_texte))
            .setGroup(GROUPE)
            .setGroupSummary(true)
            .setContentIntent(ouvrirApplication(contexte))
            .build()

        // Sans la permission, on n'affiche rien, sans tomber pour autant : l'alarme et
        // l'etat des taches restent corrects.
        val permise = ContextCompat.checkSelfPermission(contexte, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!permise) return
        gestionnaire.notify(tache.id.hashCode(), notification)
        gestionnaire.notify(ID_RESUME, resume)
    }

    /** Retire la notification d'une tache, et le resume s'il ne couvre plus rien. */
    fun retirer(contexte: Context, idTache: String) {
        val gestionnaire = NotificationManagerCompat.from(contexte)
        val t = TextesAndroid(contexte.resources)
        gestionnaire.cancel(idTache.hashCode())

        val restantes = runCatching {
            contexte.getSystemService(NotificationManager::class.java)
                .activeNotifications
                .count { it.notification.group == GROUPE && it.id != ID_RESUME }
        }.getOrDefault(0)

        if (restantes == 0) gestionnaire.cancel(ID_RESUME)
    }

    private fun ouvrirApplication(contexte: Context): PendingIntent =
        PendingIntent.getActivity(
            contexte,
            0,
            Intent(contexte, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun action(contexte: Context, quoi: String, idTache: String): PendingIntent {
        val intention = Intent(contexte, RecepteurRappel::class.java).apply {
            action = quoi
            putExtra(RecepteurRappel.EXTRA_ID, idTache)
        }
        return PendingIntent.getBroadcast(
            contexte,
            // Un code distinct par couple (tache, action), sans quoi « Fait » et
            // « Plus tard » partageraient la meme intention.
            (quoi + idTache).hashCode(),
            intention,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
