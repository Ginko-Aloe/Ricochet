package io.github.ginkoaloe.ricochet.ui

import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.assertEquals

/**
 * Les traductions sont completes : une chaine ajoutee en anglais sans son pendant
 * francais s'afficherait en anglais a un utilisateur francais, sans que rien ne le
 * signale.
 */
class TraductionsTest {

    private fun noms(dossier: String): Set<String> {
        val racine = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/main/res/$dossier/strings.xml")).documentElement
        return listOf("string", "plurals").flatMap { balise ->
            val noeuds = racine.getElementsByTagName(balise)
            (0 until noeuds.length).map { (noeuds.item(it) as Element).getAttribute("name") }
        }.toSet()
    }

    /** Le nom de l'application ne se traduit pas : il n'existe qu'en anglais, par defaut. */
    private val communs = setOf("app_name")

    @Test
    fun `chaque chaine anglaise a sa traduction francaise`() {
        assertEquals(emptySet(), noms("values") - communs - noms("values-fr"))
    }

    @Test
    fun `le francais n'a pas de chaine orpheline`() {
        assertEquals(emptySet(), noms("values-fr") - noms("values"))
    }
}
