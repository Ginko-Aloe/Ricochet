package io.github.ginkoaloe.ricochet.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.ginkoaloe.ricochet.R
import io.github.ginkoaloe.ricochet.domaine.Marque
import io.github.ginkoaloe.ricochet.domaine.SorteDeMarque
import io.github.ginkoaloe.ricochet.domaine.Tache
import io.github.ginkoaloe.ricochet.domaine.agenda
import io.github.ginkoaloe.ricochet.ui.theme.RicochetTheme
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

/**
 * Le calendrier : une grille du mois, et la liste du jour choisi dessous.
 *
 * Il sert a voir venir et a se souvenir, pas a agir : toucher une tache ouvre son
 * detail, ou se trouvent *Fait*, l'edition et l'historique. Ce qui tombe chaque jour
 * est calcule par [agenda], dans le domaine.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranCalendrier(
    taches: List<Tache>,
    maintenant: Instant,
    onOuvrir: (Tache) -> Unit,
    onRetour: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val t = rememberTextes()
    val aujourdhui = maintenant.atZone(zone).toLocalDate()
    var mois by remember { mutableStateOf(YearMonth.from(aujourdhui)) }
    var choisi by remember { mutableStateOf(aujourdhui) }

    // La grille commence le lundi de la semaine du 1er et couvre six semaines.
    val debutGrille = mois.atDay(1).let { it.minusDays((it.dayOfWeek.value - 1).toLong()) }
    val finGrille = debutGrille.plusDays(41)
    val agenda = remember(taches, mois, maintenant) { taches.agenda(debutGrille, finGrille, maintenant, zone) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.calendrier)) },
                navigationIcon = {
                    IconButton(onClick = onRetour) { Icon(painterResource(R.drawable.ic_retour), stringResource(R.string.retour)) }
                },
                actions = {
                    TextButton(onClick = { mois = YearMonth.from(aujourdhui); choisi = aujourdhui }) {
                        Text(stringResource(R.string.aujourd_hui))
                    }
                },
            )
        },
    ) { interieur ->
        LazyColumn(
            modifier = Modifier.padding(interieur),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            item {
                EnteteDuMois(mois, onPrecedent = { mois = mois.minusMonths(1) }, onSuivant = { mois = mois.plusMonths(1) })
                Grille(
                    mois = mois,
                    debut = debutGrille,
                    agenda = agenda,
                    aujourdhui = aujourdhui,
                    choisi = choisi,
                    onChoisir = { choisi = it },
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = libelleJour(t, choisi, aujourdhui),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
            val duJour = agenda[choisi].orEmpty()
            if (duJour.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.rien_ce_jour),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    )
                }
            }
            items(duJour) { marque ->
                LigneDuJour(marque, zone, onClick = { onOuvrir(marque.tache) })
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun EnteteDuMois(mois: YearMonth, onPrecedent: () -> Unit, onSuivant: () -> Unit) {
    val t = rememberTextes()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onPrecedent) { Text("‹", style = MaterialTheme.typography.titleLarge) }
        Text(
            text = mois.month.getDisplayName(TextStyle.FULL_STANDALONE, t.locale)
                .replaceFirstChar { it.titlecase(t.locale) } + " " + mois.year,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onSuivant) { Text("›", style = MaterialTheme.typography.titleLarge) }
    }
}

@Composable
private fun Grille(
    mois: YearMonth,
    debut: LocalDate,
    agenda: Map<LocalDate, List<Marque>>,
    aujourdhui: LocalDate,
    choisi: LocalDate,
    onChoisir: (LocalDate) -> Unit,
) {
    val t = rememberTextes()
    Column {
        Row {
            DayOfWeek.entries.forEach { jour ->
                Text(
                    text = jour.getDisplayName(TextStyle.NARROW, t.locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        (0 until 6).forEach { semaine ->
            Row {
                (0 until 7).forEach { j ->
                    val date = debut.plusDays(semaine * 7L + j)
                    Case(
                        date = date,
                        dansLeMois = YearMonth.from(date) == mois,
                        marques = agenda[date].orEmpty(),
                        aujourdhui = date == aujourdhui,
                        choisi = date == choisi,
                        onClick = { onChoisir(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** Un jour de la grille : son numero, et jusqu'a trois marques, puis « +N ». */
@Composable
private fun Case(
    date: LocalDate,
    dansLeMois: Boolean,
    marques: List<Marque>,
    aujourdhui: Boolean,
    choisi: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fond = if (choisi) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    Column(
        modifier = modifier
            .aspectRatio(0.8f)
            .padding(2.dp)
            .background(fond, MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val couleurNumero = when {
            !dansLeMois -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            else -> MaterialTheme.colorScheme.onSurface
        }
        Box(
            modifier = Modifier
                .size(26.dp)
                .then(
                    if (aujourdhui) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${date.dayOfMonth}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (aujourdhui) FontWeight.SemiBold else FontWeight.Normal,
                color = couleurNumero,
            )
        }
        Spacer(Modifier.height(2.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            marques.take(3).forEach { MarqueMiniature(it.sorte, 9.dp) }
        }
        if (marques.size > 3) {
            Text(
                text = "+${marques.size - 3}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Le vocabulaire de l'icone : galet plein pour une echeance sure, galet en pointille
 * pour une projection, galet ocre pour un retard, petite coche pour un *Fait*.
 */
@Composable
private fun MarqueMiniature(sorte: SorteDeMarque, taille: Dp) {
    val neutre = MaterialTheme.colorScheme.onSurfaceVariant
    val ocre = MaterialTheme.colorScheme.tertiary
    when (sorte) {
        SorteDeMarque.ECHEANCE, SorteDeMarque.RETARD -> Icon(
            painter = painterResource(R.drawable.ic_galet),
            contentDescription = null,
            tint = if (sorte == SorteDeMarque.RETARD) ocre else neutre,
            modifier = Modifier.size(taille),
        )
        SorteDeMarque.PROJECTION -> Canvas(Modifier.size(taille).rotate(-14f)) {
            val trait = size.minDimension * 0.12f
            drawOval(
                color = neutre,
                topLeft = Offset(trait / 2, size.height * 0.2f),
                size = Size(size.width - trait, size.height * 0.6f),
                style = Stroke(width = trait, pathEffect = PathEffect.dashPathEffect(floatArrayOf(trait * 1.6f, trait * 1.2f))),
            )
        }
        SorteDeMarque.FAIT -> Canvas(Modifier.size(taille)) {
            val coche = Path().apply {
                moveTo(size.width * 0.15f, size.height * 0.55f)
                lineTo(size.width * 0.42f, size.height * 0.8f)
                lineTo(size.width * 0.88f, size.height * 0.22f)
            }
            drawPath(coche, color = neutre, style = Stroke(width = size.minDimension * 0.16f))
        }
    }
}

/** Une ligne de la liste du jour : la marque, le nom, et ce qu'elle dit. */
@Composable
private fun LigneDuJour(marque: Marque, zone: ZoneId, onClick: () -> Unit) {
    val t = rememberTextes()
    val heure = libelleHeure(t, marque.quand.atZone(zone).toLocalTime())
    val detail = when (marque.sorte) {
        SorteDeMarque.ECHEANCE -> t.texte(R.string.a_heure, heure)
        SorteDeMarque.PROJECTION -> t.texte(R.string.marque_projection, heure)
        SorteDeMarque.FAIT -> t.texte(R.string.marque_fait, heure)
        SorteDeMarque.RETARD ->
            if (marque.oublis > 1) t.texte(
                R.string.echeance_depuis,
                t.pluriel(R.plurals.oublis, marque.oublis, marque.oublis),
                dateCourte(t, marque.quand, zone),
            )
            else t.texte(R.string.marque_retard, dateCourte(t, marque.quand, zone), heure)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(32.dp), contentAlignment = Alignment.Center) { MarqueMiniature(marque.sorte, 18.dp) }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(marque.tache.nom, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = if (marque.sorte == SorteDeMarque.RETARD) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** « Aujourd'hui », « Demain », ou « Lundi 9 novembre ». */
private fun libelleJour(t: Textes, jour: LocalDate, aujourdhui: LocalDate): String = when (jour) {
    aujourdhui -> t.texte(R.string.aujourd_hui)
    aujourdhui.plusDays(1) -> t.texte(R.string.demain)
    aujourdhui.minusDays(1) -> t.texte(R.string.hier)
    else -> t.texte(R.string.jour_complet, nomDuJour(t, jour.dayOfWeek), numeroDuJour(t, jour.dayOfMonth), nomDuMois(t, jour.month))
}.replaceFirstChar { it.titlecase(t.locale) }

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun ApercuCalendrier() {
    val maintenant = Instant.parse("2026-11-07T11:00:00Z")
    val taches = listOf(
        Tache("Arroser le ficus", 3.days, listOf(maintenant.minus(1.days.toJavaDuration())), creee = maintenant.minus(20.days.toJavaDuration())),
        Tache("Appeler maman", 2.days, creee = maintenant.minus(5.days.toJavaDuration())),
    )
    RicochetTheme { EcranCalendrier(taches, maintenant, {}, {}) }
}
