package fr.cedric.tessvine.ui.ecrans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import fr.cedric.tessvine.data.Session
import fr.cedric.tessvine.ui.Nav
import fr.cedric.tessvine.ui.composants.SurTitre
import kotlinx.coroutines.launch

/** Adresses de Tessvine (maison et extérieur) et clé d'API. */
@Composable
fun EcranConnexion(nav: Nav, premierLancement: Boolean) {
    val prefs = Session.prefs
    val scope = rememberCoroutineScope()
    var maison by remember { mutableStateOf(prefs.urlMaison) }
    var exterieur by remember { mutableStateOf(prefs.urlExterieur) }
    var cle by remember { mutableStateOf(prefs.cle) }
    var resultat by remember { mutableStateOf<String?>(null) }
    var test by remember { mutableStateOf(false) }

    fun tester(apresSucces: Boolean) = scope.launch {
        test = true
        resultat = null
        val api = Session.api
        val lignes = mutableListOf<String>()
        for ((nom, url) in listOf("Maison" to maison, "Extérieur" to exterieur)) {
            if (url.isBlank()) continue
            val e = api.tester(url, cle)
            lignes += "$nom : ${e ?: "✓ Tessvine répond"}"
        }
        resultat = if (lignes.isEmpty()) "Indiquez au moins une adresse" else lignes.joinToString("\n")
        test = false
        // « Enregistrer » sauvegarde même si rien ne répond (téléphone hors de la maison, serveur éteint…)
        if (apresSucces && lignes.isNotEmpty()) {
            prefs.urlMaison = maison
            prefs.urlExterieur = exterieur
            prefs.cle = cle
            api.oublierAdresse()
            Session.chargerReferentiel()
            Session.changement()
            if (premierLancement) nav.accueil() else nav.retour()
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SurTitre("Connexion")
        Text(if (premierLancement) "Bienvenue dans Tessvine" else "Serveur Tessvine", style = MaterialTheme.typography.headlineMedium)
        Text(
            "L'appli essaie d'abord l'adresse « maison » (Wi-Fi ou VPN), puis l'adresse « extérieur » si la première ne répond pas.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            maison, { maison = it }, label = { Text("Adresse à la maison") },
            placeholder = { Text("http://192.168.1.37:8099") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            exterieur, { exterieur = it }, label = { Text("Adresse depuis l'extérieur (facultatif)") },
            placeholder = { Text("https://cave.mondomaine.fr") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            cle, { cle = it }, label = { Text("Clé d'API (CAVE_API_KEY)") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { tester(false) }, enabled = !test) { Text("Tester") }
            Spacer(Modifier.width(12.dp))
            Button(onClick = { tester(true) }, enabled = !test) { Text("Enregistrer") }
            if (test) {
                Spacer(Modifier.width(16.dp))
                CircularProgressIndicator(Modifier.width(24.dp).height(24.dp), strokeWidth = 2.dp)
            }
        }
        resultat?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Spacer(Modifier.height(8.dp))
        Text(
            "Depuis l'extérieur, Nabu Casa ne laisse pas passer l'appli : utilisez un VPN (Tailscale, WireGuard) avec l'adresse maison, " +
                "ou exposez Tessvine en https avec Nginx Proxy Manager et mettez cette adresse dans « extérieur ».",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
