package io.github.ginkoaloe.ricochet.donnees

import android.content.SharedPreferences
import java.time.LocalTime

/**
 * Les reglages de l'application.
 *
 * Dans les preferences, hors de `taches.json` : ce sont des choix propres a ce
 * telephone, pas des donnees. Ils n'entrent donc pas dans l'archive d'export.
 */
class Reglages(private val preferences: SharedPreferences) {

    /** Le rappel du matin, qui annonce les taches de la journee. Coupe par defaut. */
    var rappelDuMatin: Boolean
        get() = preferences.getBoolean(RAPPEL_DU_MATIN, false)
        set(actif) = preferences.edit().putBoolean(RAPPEL_DU_MATIN, actif).apply()

    /**
     * Son heure, 8 h par defaut. Gardee meme rappel coupe : on la retrouve en le
     * reactivant. Conservee en minutes depuis minuit.
     */
    var heureDuMatin: LocalTime
        get() = preferences.getInt(HEURE_DU_MATIN, 8 * 60).let { LocalTime.of(it / 60, it % 60) }
        set(heure) = preferences.edit().putInt(HEURE_DU_MATIN, heure.hour * 60 + heure.minute).apply()

    private companion object {
        const val RAPPEL_DU_MATIN = "rappel_du_matin"
        const val HEURE_DU_MATIN = "heure_du_matin"
    }
}
