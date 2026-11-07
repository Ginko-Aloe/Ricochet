package io.github.ginkoaloe.ricochet.ui

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Les mots de l'application, dans la langue du telephone.
 *
 * Une interface plutot que des appels directs a [Resources] : les fonctions de
 * libelle restent ainsi du Kotlin pur, testable sans appareil. Les tests en donnent
 * une realisation qui relit les **vrais** fichiers `strings.xml` — ils verifient les
 * traductions, pas une copie.
 */
interface Textes {
    /** La langue des textes : elle decide aussi des noms de jours et de mois. */
    val locale: Locale

    fun texte(@StringRes id: Int, vararg arguments: Any): String

    /** Une forme plurielle, choisie selon [nombre] et les regles de la langue. */
    fun pluriel(@PluralsRes id: Int, nombre: Int, vararg arguments: Any): String
}

/** Les textes tires des ressources Android. */
class TextesAndroid(private val ressources: Resources) : Textes {
    override val locale: Locale get() = ressources.configuration.locales[0]

    override fun texte(id: Int, vararg arguments: Any): String =
        if (arguments.isEmpty()) ressources.getString(id) else ressources.getString(id, *arguments)

    override fun pluriel(id: Int, nombre: Int, vararg arguments: Any): String =
        if (arguments.isEmpty()) ressources.getQuantityString(id, nombre)
        else ressources.getQuantityString(id, nombre, *arguments)
}

/** Les textes de l'ecran courant, renouveles si la langue change. */
@Composable
fun rememberTextes(): Textes {
    val contexte = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(configuration) { TextesAndroid(contexte.resources) }
}
