package io.github.ginkoaloe.ricochet.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

// La police du systeme, sans rien embarquer. Seuls les titres prennent du poids :
// le nom d'une tache doit se lire d'un coup d'oeil, meme pose sur une photo.
private val Base = Typography()

val Typography = Base.copy(
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
)
