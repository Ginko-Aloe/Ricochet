package io.github.ginkoaloe.ricochet.donnees

import io.github.ginkoaloe.ricochet.domaine.Rythme
import io.github.ginkoaloe.ricochet.domaine.Tache
import io.github.ginkoaloe.ricochet.domaine.accomplissements
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.DurationUnit

/**
 * L'historique de toutes les taches en CSV, pour l'analyser ailleurs. L'archive zip
 * reste la sauvegarde ; ceci n'est qu'une lecture.
 *
 * CSV standard (RFC 4180) : virgule, point decimal, fins de ligne CRLF, dates ISO
 * 8601 en UTC. Un tableur francais le relira par son assistant d'import ; un script
 * ou Google Sheets, directement.
 *
 * Une ligne par accomplissement, triees par tache puis par date. `ecart_heures` est
 * positif en cas de retard ; pour un Ricochet il est calcule avec la periode
 * **actuelle**, pour un Rendez-vous il se mesure a l'occurrence soldee, que donne
 * `echeance_soldee`. Un Rendez-vous n'a pas de `periode_heures`.
 *
 * Les colonnes ajoutees avec les Rendez-vous viennent **a la fin**, pour qu'un
 * tableur deja branche sur les cinq premieres continue de fonctionner.
 */
fun historiqueEnCsv(taches: List<Tache>): String = buildString {
    append("id,tache,date,ecart_heures,periode_heures,type,echeance_soldee\r\n")
    for (tache in taches.sortedBy { it.nom.lowercase(Locale.ROOT) }) {
        val periode = (tache.rythme as? Rythme.Ricochet)?.periode?.let(::heures).orEmpty()
        val type = if (tache.rythme is Rythme.Ricochet) "ricochet" else "rendez-vous"
        for (accompli in tache.accomplissements().asReversed()) {
            append(champ(tache.id)).append(',')
            append(champ(tache.nom)).append(',')
            append(accompli.quand).append(',')
            append(accompli.ecart?.let(::heures).orEmpty()).append(',')
            append(periode).append(',')
            append(type).append(',')
            append(accompli.pour?.toString().orEmpty()).append("\r\n")
        }
    }
}

private fun heures(d: Duration): String = String.format(Locale.ROOT, "%.2f", d.toDouble(DurationUnit.HOURS))

/** Entoure de guillemets ce qui romprait la ligne, en doublant ceux qu'il contient. */
private fun champ(texte: String): String =
    if (texte.any { it == ',' || it == '"' || it == '\r' || it == '\n' }) "\"${texte.replace("\"", "\"\"")}\""
    else texte
