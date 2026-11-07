package io.github.ginkoaloe.ricochet.ui.theme

import androidx.compose.ui.graphics.Color

// La palette vient de l'icone : l'ardoise pour la structure, l'ocre du galet pour
// l'urgence, et rien d'autre. L'ocre occupe l'emplacement « tertiary » de Material,
// qu'aucun composant n'emploie de lui-meme : il n'apparait donc que la ou on le
// demande, c'est-a-dire sur ce qui est du.

// Teintes de l'icone, reprises telles quelles (ic_launcher_*.xml).
val Ardoise = Color(0xFF35414B)
val OndeLoin = Color(0xFF5D6E7B)
val OndePres = Color(0xFF93A5B2)
val Ocre = Color(0xFFC98A3C)
val OcreClair = Color(0xFFE6B677)

// Mode clair. L'ocre de l'icone est trop pale pour du texte sur fond clair :
// on l'assombrit jusqu'a un contraste lisible.
val ArdoiseClair = Color(0xFF3F4D58)
val ArdoiseConteneurClair = Color(0xFFD3DDE5)
val SurArdoiseConteneurClair = Color(0xFF17222B)
val SecondaireConteneurClair = Color(0xFFDCE3E8)
val SurSecondaireConteneurClair = Color(0xFF1A252D)
val OcreFonce = Color(0xFF8F5A14)
val OcreConteneurClair = Color(0xFFFFDDB5)
val SurOcreConteneurClair = Color(0xFF2E1A00)
val FondClair = Color(0xFFF6F8F9)
val EncreClair = Color(0xFF191C1E)
val SurfaceVariantClair = Color(0xFFDDE3E8)
val SurSurfaceVariantClair = Color(0xFF424A51)
val ContourClair = Color(0xFF727A81)
val ContourDiscretClair = Color(0xFFC2C9CF)

// Mode sombre : l'ardoise de l'icone devient conteneur, l'ocre clair devient l'accent.
val ArdoisePale = Color(0xFFB7C6D2)
val SurArdoisePale = Color(0xFF21303B)
val SecondaireConteneurSombre = Color(0xFF3B4852)
val SurOcreClair = Color(0xFF472A00)
val OcreConteneurSombre = Color(0xFF6B4510)
val FondSombre = Color(0xFF111518)
val EncreSombre = Color(0xFFE1E5E8)
val SurfaceVariantSombre = Color(0xFF3F474E)
val ContourSombre = Color(0xFF8C949B)
val ContourDiscretSombre = Color(0xFF42494F)
