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
