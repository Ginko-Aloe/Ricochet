package io.github.ginkoaloe.ricochet.ui

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import io.github.ginkoaloe.ricochet.R
import io.github.ginkoaloe.ricochet.domaine.Rythme
import io.github.ginkoaloe.ricochet.domaine.Tache
import io.github.ginkoaloe.ricochet.domaine.etatEcheance
import io.github.ginkoaloe.ricochet.ui.theme.OcreClair
import io.github.ginkoaloe.ricochet.ui.theme.RicochetTheme
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

/**
 * Le detail d'une tache, repris du meilleur ecran de WaterMe : la photo occupe tout
 * le fond, le nom s'ecrit en tres grand par-dessus, les valeurs sont encadrees, et
 * l'action principale est un large bouton en bas.
 *
 * Apres « Fait » on **reste ici** : on voit l'echeance ricocher sous ses yeux, ce
 * qui rend visible la regle meme de l'application.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EcranDetail(
    tache: Tache,
    maintenant: Instant,
    onAccomplir: (Tache) -> Unit,
    onAnnuler: (Tache) -> Unit,
    onEditer: () -> Unit,
    onHistorique: () -> Unit,
    onRetour: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = rememberTextes()
    val image by imageDeTache(tache.image)
    val hote = remember { SnackbarHostState() }
    val portee = rememberCoroutineScope()
    val etat = tache.etatEcheance(maintenant)

    // Sur une photo, le texte est blanc sous un voile sombre, quel que soit le mode :
    // c'est la photo qui fait le fond. Sans photo, on revient aux couleurs du theme.
    val surPhoto = image != null
    val encre = if (surPhoto) Color.White else MaterialTheme.colorScheme.onSurface
    val encreDiscrete =
        if (surPhoto) Color.White.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant
    // L'ocre fonce du mode clair se perdrait sur le voile : on y prend l'ocre clair.
    val urgence = if (surPhoto) OcreClair else MaterialTheme.colorScheme.tertiary
    IconesDeBarreEnBlanc(surPhoto)

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        val photo = image
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            // Le voile adaptatif pousse au bout : leger au milieu pour montrer la
            // photo, dense en bas ou s'ecrit le texte, un peu en haut pour la barre.
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.35f),
                            0.35f to Color.Black.copy(alpha = 0.05f),
                            1f to Color.Black.copy(alpha = 0.78f),
                        )
                    )
            )
        } else {
            // Faute de photo, le galet de l'icone en filigrane.
            Icon(
                painter = painterResource(R.drawable.ic_galet),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-96).dp)
                    .size(320.dp),
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            contentColor = encre,
            snackbarHost = { SnackbarHost(hote) },
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onRetour) {
                            Icon(painterResource(R.drawable.ic_retour), stringResource(R.string.retour), tint = encre)
                        }
                    },
                    actions = {
                        IconButton(onClick = onEditer) {
                            Icon(painterResource(R.drawable.ic_crayon), stringResource(R.string.modifier), tint = encre)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            },
        ) { interieur ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(interieur)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(
                    text = tache.nom,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = encre,
                )
                Spacer(Modifier.height(16.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Cadre(libelleRythme(t, tache.rythme), encre, surPhoto)
                    Cadre(if (tache.rythme is Rythme.RendezVous) tache.libelleEcheance(t, maintenant) else etat.libelle(t), if (etat.estPressant) urgence else encre, surPhoto)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = tache.libelleDernierAccomplissement(t, maintenant),
                    style = MaterialTheme.typography.bodyLarge,
                    color = encreDiscrete,
                )
                // Fonction secondaire : un lien discret, pas un bouton.
                TextButton(onClick = onHistorique, contentPadding = PaddingValues(horizontal = 0.dp)) {
                    Text(stringResource(R.string.voir_historique), color = encreDiscrete)
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        onAccomplir(tache)
                        portee.launch {
                            val reponse = hote.showSnackbar(
                                message = t.texte(R.string.tache_accomplie, tache.nom),
                                actionLabel = t.texte(R.string.annuler),
                                duration = SnackbarDuration.Long,
                            )
                            if (reponse == SnackbarResult.ActionPerformed) onAnnuler(tache)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Text(stringResource(R.string.fait), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

/**
 * Sur une photo, les icones de la barre d'etat passent en blanc comme le texte : en
 * mode clair, elles resteraient sombres sur le voile. On rend l'etat d'avant en
 * quittant l'ecran.
 */
@Composable
private fun IconesDeBarreEnBlanc(actif: Boolean) {
    val vue = LocalView.current
    if (vue.isInEditMode) return
    DisposableEffect(actif) {
        val fenetre = (vue.context as? Activity)?.window ?: return@DisposableEffect onDispose {}
        val barres = WindowCompat.getInsetsController(fenetre, vue)
        val avant = barres.isAppearanceLightStatusBars
        if (actif) barres.isAppearanceLightStatusBars = false
        onDispose { barres.isAppearanceLightStatusBars = avant }
    }
}

/** Une valeur encadree : « Tous les 3 jours », « Dans 2 jours ». */
@Composable
private fun Cadre(texte: String, couleur: Color, surPhoto: Boolean) {
    val forme = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .border(1.dp, couleur.copy(alpha = 0.45f), forme)
            .background(
                if (surPhoto) Color.Black.copy(alpha = 0.25f)
                else MaterialTheme.colorScheme.surfaceContainerHigh,
                forme,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(texte, style = MaterialTheme.typography.titleMedium, color = couleur)
    }
}

// --------------------------------------------------------------------- apercus

@Preview(showBackground = true, heightDp = 720)
@Composable
private fun ApercuDetail() {
    val maintenant = Instant.parse("2026-11-02T12:00:00Z")
    RicochetTheme {
        EcranDetail(
            tache = Tache("Arroser le ficus", 3.days, listOf(maintenant.minus(5.days.toJavaDuration()))),
            maintenant = maintenant,
            onAccomplir = {}, onAnnuler = {}, onEditer = {}, onHistorique = {}, onRetour = {},
        )
    }
}
