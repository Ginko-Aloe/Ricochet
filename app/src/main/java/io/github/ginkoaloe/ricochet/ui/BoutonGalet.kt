package io.github.ginkoaloe.ricochet.ui

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.ginkoaloe.ricochet.R

/**
 * Le bouton flottant de creation : un + dans un galet. La forme rappelle l'icone, le
 * + porte le sens — un galet seul doublerait celui d'urgence qu'il a en tete de
 * carte. Ardoise et non ocre : l'ocre est reserve a l'urgence.
 */
@Composable
fun BoutonGalet(onClick: () -> Unit, description: String, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onClick,
        shape = FormeGalet,
        modifier = modifier.size(width = 76.dp, height = 56.dp),
    ) {
        Icon(painterResource(R.drawable.ic_plus), contentDescription = description)
    }
}

/**
 * Le galet de l'icone : un ovale incline de 14 degres. Ses proportions le gardent
 * entier dans sa boite une fois tourne, ombre comprise.
 */
private val FormeGalet = GenericShape { taille, _ ->
    val cx = taille.width / 2
    val cy = taille.height / 2
    val rx = taille.width * 0.47f
    val ry = taille.height * 0.42f
    addOval(Rect(cx - rx, cy - ry, cx + rx, cy + ry))
    transform(Matrix().apply {
        translate(cx, cy)
        rotateZ(-14f)
        translate(-cx, -cy)
    })
}
