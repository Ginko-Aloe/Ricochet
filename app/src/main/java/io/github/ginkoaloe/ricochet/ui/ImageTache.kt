package io.github.ginkoaloe.ricochet.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Charge l'illustration d'une tache : hors du fil principal, et reduite.
 *
 * Rend `null` tant que le chargement n'est pas fini, et si la tache n'a pas d'image
 * ou que son fichier a disparu — un cadre vide vaut mieux qu'un plantage.
 */
@Composable
fun imageDeTache(chemin: String?, largeurCible: Int = 1080): State<ImageBitmap?> =
    produceState<ImageBitmap?>(initialValue = null, chemin, largeurCible) {
        value = withContext(Dispatchers.IO) { chargerReduite(chemin, largeurCible) }
    }

private fun chargerReduite(chemin: String?, largeurCible: Int): ImageBitmap? {
    val fichier = File(chemin ?: return null)
    if (!fichier.exists()) return null
    return runCatching {
        // Premiere passe : mesurer sans decoder les pixels.
        val mesure = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(fichier.path, mesure)

        val options = BitmapFactory.Options().apply {
            inSampleSize = echantillonnage(mesure.outWidth, largeurCible)
        }
        val brute = BitmapFactory.decodeFile(fichier.path, options) ?: return null
        redresser(brute, orientationExif(fichier)).asImageBitmap()
    }.getOrNull()
}

/**
 * L'appareil photo n'enregistre pas l'image tournee : il ecrit les pixels tels que
 * le capteur les lit, et range le sens de la prise de vue dans la balise EXIF
 * Orientation. `BitmapFactory` l'ignore — une photo prise en portrait s'afficherait
 * couchee. On redresse donc au chargement, sans toucher au fichier : la correction
 * vaut aussi pour les images deja enregistrees, et rien n'est reencode.
 */
private fun orientationExif(fichier: File): Int =
    runCatching {
        ExifInterface(fichier.path)
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

private fun redresser(image: Bitmap, orientation: Int): Bitmap {
    val r = redressement(orientation) ?: return image
    val matrice = Matrix().apply {
        setRotate(r.degres)
        if (r.miroir) postScale(-1f, 1f)
    }
    return Bitmap.createBitmap(image, 0, 0, image.width, image.height, matrice, true)
}

/** Une rotation, suivie s'il le faut d'un retournement horizontal. */
internal data class Redressement(val degres: Float, val miroir: Boolean)

/**
 * Ce qu'il faut appliquer aux pixels pour une valeur d'orientation EXIF, ou `null`
 * quand ils sont deja dans le bon sens. Les huit valeurs de la norme, dans l'ordre.
 */
internal fun redressement(orientation: Int): Redressement? = when (orientation) {
    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> Redressement(0f, miroir = true)
    ExifInterface.ORIENTATION_ROTATE_180 -> Redressement(180f, miroir = false)
    ExifInterface.ORIENTATION_FLIP_VERTICAL -> Redressement(180f, miroir = true)
    ExifInterface.ORIENTATION_TRANSPOSE -> Redressement(90f, miroir = true)
    ExifInterface.ORIENTATION_ROTATE_90 -> Redressement(90f, miroir = false)
    ExifInterface.ORIENTATION_TRANSVERSE -> Redressement(270f, miroir = true)
    ExifInterface.ORIENTATION_ROTATE_270 -> Redressement(270f, miroir = false)
    else -> null
}

/**
 * Le facteur de reduction a appliquer au decodage : la plus grande puissance de deux
 * qui laisse l'image au moins aussi large que [largeurCible].
 *
 * `BitmapFactory` n'accepte que des puissances de deux, et arrondirait lui-meme a la
 * puissance inferieure ; autant la calculer explicitement.
 */
internal fun echantillonnage(largeurSource: Int, largeurCible: Int): Int {
    if (largeurSource <= 0 || largeurCible <= 0) return 1
    var facteur = 1
    while (largeurSource / (facteur * 2) >= largeurCible) facteur *= 2
    return facteur
}
