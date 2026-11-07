package io.github.ginkoaloe.ricochet.ui

import io.github.ginkoaloe.ricochet.R
import org.w3c.dom.Element
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Des [Textes] pour les tests sur JVM, lus dans les **vrais** fichiers de ressources
 * de l'application. Un test qui passe en anglais et en francais verifie donc les
 * traductions telles qu'elles seront livrees.
 *
 * Le francais complete l'anglais comme le fait Android : une chaine absente de
 * `values-fr` se lit dans `values`.
 */
class TextesDeTest private constructor(
    override val locale: Locale,
    private val chaines: Map<String, String>,
    private val pluriels: Map<String, Map<String, String>>,
) : Textes {

    override fun texte(id: Int, vararg arguments: Any): String {
        val nom = nomDe(R.string::class.java, id)
        val motif = chaines[nom] ?: error("Chaine absente : $nom")
        return if (arguments.isEmpty()) motif else String.format(locale, motif, *arguments)
    }

    override fun pluriel(id: Int, nombre: Int, vararg arguments: Any): String {
        val nom = nomDe(R.plurals::class.java, id)
        val formes = pluriels[nom] ?: error("Pluriel absent : $nom")
        val motif = formes[categorie(nombre)] ?: formes.getValue("other")
        return if (arguments.isEmpty()) motif else String.format(locale, motif, *arguments)
    }

    /** Les regles CLDR utiles ici : en francais, 0 et 1 sont au singulier. */
    private fun categorie(n: Int): String = when (locale.language) {
        "fr" -> if (n == 0 || n == 1) "one" else "other"
        else -> if (n == 1) "one" else "other"
    }

    companion object {
        private val RES = File("src/main/res")

        val anglais: TextesDeTest by lazy { charger(Locale.ENGLISH, "values") }
        val francais: TextesDeTest by lazy { charger(Locale.FRENCH, "values", "values-fr") }

        private fun charger(locale: Locale, vararg dossiers: String): TextesDeTest {
            val chaines = mutableMapOf<String, String>()
            val pluriels = mutableMapOf<String, Map<String, String>>()
            dossiers.forEach { dossier ->
                val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(File(RES, "$dossier/strings.xml"))
                val racine = doc.documentElement
                racine.enfants("string").forEach { chaines[it.getAttribute("name")] = desechapper(it.textContent) }
                racine.enfants("plurals").forEach { p ->
                    pluriels[p.getAttribute("name")] =
                        p.enfants("item").associate { it.getAttribute("quantity") to desechapper(it.textContent) }
                }
            }
            return TextesDeTest(locale, chaines, pluriels)
        }

        private fun Element.enfants(balise: String): List<Element> {
            val noeuds = getElementsByTagName(balise)
            return (0 until noeuds.length).map { noeuds.item(it) as Element }.filter { it.parentNode == this }
        }

        /** Ce qu'Android fait des echappements d'une ressource : \' devient ', etc. */
        private fun desechapper(brut: String): String =
            brut.replace("\\'", "'").replace("\\\"", "\"").replace("\\n", "\n")

        private fun nomDe(classe: Class<*>, id: Int): String =
            classe.fields.first { it.getInt(null) == id }.name
    }
}
