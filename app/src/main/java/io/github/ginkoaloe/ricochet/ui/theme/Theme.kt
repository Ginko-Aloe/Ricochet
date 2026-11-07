package io.github.ginkoaloe.ricochet.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PaletteClaire = lightColorScheme(
    primary = ArdoiseClair,
    onPrimary = Color.White,
    primaryContainer = ArdoiseConteneurClair,
    onPrimaryContainer = SurArdoiseConteneurClair,
    secondary = OndeLoin,
    onSecondary = Color.White,
    secondaryContainer = SecondaireConteneurClair,
    onSecondaryContainer = SurSecondaireConteneurClair,
    tertiary = OcreFonce,
    onTertiary = Color.White,
    tertiaryContainer = OcreConteneurClair,
    onTertiaryContainer = SurOcreConteneurClair,
    background = FondClair,
    onBackground = EncreClair,
    surface = FondClair,
    onSurface = EncreClair,
    surfaceVariant = SurfaceVariantClair,
    onSurfaceVariant = SurSurfaceVariantClair,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF0F3F5),
    surfaceContainer = Color(0xFFEAEEF1),
    surfaceContainerHigh = Color(0xFFE4E9EC),
    surfaceContainerHighest = Color(0xFFDEE3E7),
    outline = ContourClair,
    outlineVariant = ContourDiscretClair,
)

private val PaletteSombre = darkColorScheme(
    primary = ArdoisePale,
    onPrimary = SurArdoisePale,
    primaryContainer = Ardoise,
    onPrimaryContainer = ArdoiseConteneurClair,
    secondary = OndePres,
    onSecondary = SurArdoisePale,
    secondaryContainer = SecondaireConteneurSombre,
    onSecondaryContainer = SecondaireConteneurClair,
    tertiary = OcreClair,
    onTertiary = SurOcreClair,
    tertiaryContainer = OcreConteneurSombre,
    onTertiaryContainer = OcreConteneurClair,
    background = FondSombre,
    onBackground = EncreSombre,
    surface = FondSombre,
    onSurface = EncreSombre,
    surfaceVariant = SurfaceVariantSombre,
    onSurfaceVariant = ContourDiscretClair,
    surfaceContainerLowest = Color(0xFF0C1013),
    surfaceContainerLow = Color(0xFF191D21),
    surfaceContainer = Color(0xFF1D2226),
    surfaceContainerHigh = Color(0xFF272C31),
    surfaceContainerHighest = Color(0xFF32373C),
    outline = ContourSombre,
    outlineVariant = ContourDiscretSombre,
)

/**
 * Le theme de Ricochet : sa propre palette, tiree de l'icone, en clair et en sombre
 * selon le systeme. Pas de couleurs dynamiques : l'accent doit rester l'ocre, qui ne
 * dit qu'une chose — c'est du —, quel que soit le fond d'ecran.
 */
@Composable
fun RicochetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) PaletteSombre else PaletteClaire,
        typography = Typography,
        content = content
    )
}
