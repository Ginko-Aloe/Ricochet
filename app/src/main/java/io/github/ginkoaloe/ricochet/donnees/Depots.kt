package io.github.ginkoaloe.ricochet.donnees

import android.content.Context
import java.io.File

/**
 * Ou vivent les donnees, pour tout le monde — l'interface comme les recepteurs de
 * notification, qui tournent sans activite et doivent pouvoir lire et ecrire seuls.
 */
fun depotTaches(contexte: Context) = DepotTaches(File(contexte.filesDir, "taches.json"))

fun depotImages(contexte: Context) = DepotImages(File(contexte.filesDir, "images"))

fun reglages(contexte: Context) = Reglages(contexte.getSharedPreferences("reglages", Context.MODE_PRIVATE))
