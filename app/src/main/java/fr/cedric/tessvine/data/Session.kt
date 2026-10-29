package fr.cedric.tessvine.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.NumberFormat
import java.util.Locale

/** État partagé de l'application : connexion, référentiel, et un compteur pour rafraîchir les écrans. */
object Session {
    lateinit var prefs: Preferences
        private set
    lateinit var api: Api
        private set

    var referentiel by mutableStateOf(Referentiel())
    /** Incrémenté après chaque modification (sortie, ajout, déplacement) : les écrans se rechargent. */
    var version by mutableIntStateOf(0)
        private set

    fun init(context: Context) {
        if (::api.isInitialized) return
        prefs = Preferences(context.applicationContext)
        api = Api(prefs)
    }

    fun changement() { version++ }

    suspend fun chargerReferentiel() {
        runCatching { api.referentiel() }.onSuccess { referentiel = it }
    }

    fun couleurLabel(cle: String) = referentiel.couleurs[cle] ?: cle.replaceFirstChar { it.uppercase() }
}

private val formatEuros = NumberFormat.getCurrencyInstance(Locale.FRANCE)
fun euros(v: Double?): String = if (v == null) "" else formatEuros.format(v)

/** Rangée d'un casier comme sur le plan de Tessvine : A, B, C… */
fun lettreRang(etage: Int): String {
    val i = (etage - 1) % 26
    val tour = if (etage > 26) ((etage + 25) / 26).toString() else ""
    return ('A' + i).toString() + tour
}

/** Nom d'une place à partir de l'adresse du serveur (rangée = (profondeur - 1) × 100 + étage). */
fun nomPlace(rangee: Int?, colonne: Int?): String {
    if (rangee == null || colonne == null) return "non rangée"
    val profondeur = (rangee - 1) / 100
    val etage = (rangee - 1) % 100 + 1
    val base = "${lettreRang(etage)}$colonne"
    return if (profondeur == 0) base else "$base (rang ${profondeur + 1})"
}

fun titreVin(nom: String, millesime: Int?) = if (millesime != null) "$nom $millesime" else nom

fun sousTitre(vararg morceaux: String?) = morceaux.filter { !it.isNullOrBlank() }.joinToString(" · ")


/** Contenance d'un format en cl (« 75cl », « 1,5 L », « Magnum »…), ou null si inconnue. */
fun contenanceCl(format: String): Double? {
    val t = format.trim().lowercase()
    if (t.isEmpty()) return null
    Regex("""(\d+(?:[.,]\d+)?)\s*(ml|cl|l)\b""").find(t)?.let { m ->
        val n = m.groupValues[1].replace(',', '.').toDouble()
        return when (m.groupValues[2]) { "ml" -> n / 10; "cl" -> n; else -> n * 100 }
    }
    val noms = mapOf("demi" to 37.5, "magnum" to 150.0, "jeroboam" to 300.0, "jéroboam" to 300.0, "cubi" to 300.0,
        "mathusalem" to 600.0, "salmanazar" to 900.0, "balthazar" to 1200.0, "nabuchodonosor" to 1500.0)
    noms.forEach { (nom, cl) -> if (nom in t) return cl }
    return t.replace(',', '.').toDoubleOrNull()
}

/** Bouteille trop grande pour les casiers de la cave (plus de 75 cl). */
fun grandFormat(format: String): Boolean = (contenanceCl(format) ?: 0.0) > 75.0
