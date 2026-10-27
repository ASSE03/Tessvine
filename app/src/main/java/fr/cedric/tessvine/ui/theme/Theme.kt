package fr.cedric.tessvine.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Or = Color(0xFFC9A45C)
val Bordeaux = Color(0xFF9B2C4B)

private val Sombre = darkColorScheme(
    primary = Color(0xFFB8456A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5E1A2E),
    onPrimaryContainer = Color(0xFFF6DCE3),
    secondary = Or,
    onSecondary = Color(0xFF2A1F08),
    secondaryContainer = Color(0xFF3A3122),
    onSecondaryContainer = Color(0xFFF1E3C4),
    background = Color(0xFF16141A),
    onBackground = Color(0xFFEDE6DC),
    surface = Color(0xFF16141A),
    onSurface = Color(0xFFEDE6DC),
    surfaceVariant = Color(0xFF2A2630),
    onSurfaceVariant = Color(0xFFB9B0A6),
    surfaceContainer = Color(0xFF221F26),
    surfaceContainerHigh = Color(0xFF2A2630),
    surfaceContainerLow = Color(0xFF1C1A20),
    outline = Color(0xFF5A5360),
    outlineVariant = Color(0xFF3A3540),
    error = Color(0xFFE5737A),
)

private val Clair = lightColorScheme(
    primary = Color(0xFF7A1F3A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3D7DF),
    onPrimaryContainer = Color(0xFF3E0C1C),
    secondary = Color(0xFF9A7A35),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1E5C8),
    onSecondaryContainer = Color(0xFF3A2C0B),
    background = Color(0xFFF6F1E8),
    onBackground = Color(0xFF221D1A),
    surface = Color(0xFFF6F1E8),
    onSurface = Color(0xFF221D1A),
    surfaceVariant = Color(0xFFEAE2D6),
    onSurfaceVariant = Color(0xFF5E564E),
    surfaceContainer = Color(0xFFFFFBF5),
    surfaceContainerHigh = Color(0xFFF0E9DE),
    surfaceContainerLow = Color(0xFFFBF7F0),
    outline = Color(0xFFB4AA9E),
    outlineVariant = Color(0xFFDDD3C6),
)

private val base = Typography()
private val Typo = base.copy(
    displaySmall = base.displaySmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal),
    headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.Serif),
    headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.Serif),
    titleLarge = base.titleLarge.copy(fontFamily = FontFamily.Serif),
    titleMedium = base.titleMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
)

/** Petit titre en capitales espacées, doré (« ACCORDS », « COLLECTION »…). */
val Surtitre = TextStyle(fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold, color = Or)

@Composable
fun TessvineTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Sombre else Clair,
        typography = Typo,
        content = content,
    )
}

/** Couleur d'un type de vin (pastilles du plan, listes). */
fun couleurVin(couleur: String): Color = when (couleur) {
    "rouge" -> Color(0xFF8E2238)
    "blanc" -> Color(0xFFD4BC5A)
    "rose" -> Color(0xFFE6A0B2)
    "effervescent" -> Color(0xFF6F93C2)
    "liquoreux" -> Color(0xFFD08E2F)
    else -> Color(0xFF8D8790)
}

/** Couleur de l'état de garde. */
fun couleurStatut(statut: String): Color = when (statut) {
    "urgent", "depasse" -> Color(0xFFD9534F)
    "declin" -> Color(0xFFE0913A)
    "apogee" -> Or
    "a_boire" -> Color(0xFF5BAA6A)
    "trop_jeune" -> Color(0xFF7D93B5)
    else -> Color(0xFF8D8790)
}
