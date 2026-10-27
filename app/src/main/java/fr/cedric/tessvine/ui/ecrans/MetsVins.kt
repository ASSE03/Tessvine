@file:OptIn(ExperimentalLayoutApi::class)

package fr.cedric.tessvine.ui.ecrans

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.cedric.tessvine.data.ConseilPlat
import fr.cedric.tessvine.data.Session
import fr.cedric.tessvine.data.Suggestion
import fr.cedric.tessvine.data.Vin
import fr.cedric.tessvine.data.sousTitre
import fr.cedric.tessvine.data.titreVin
import fr.cedric.tessvine.ui.Nav
import fr.cedric.tessvine.ui.composants.Carte
import fr.cedric.tessvine.ui.composants.DialogueSortie
import fr.cedric.tessvine.ui.composants.LocalSnackbar
import fr.cedric.tessvine.ui.composants.Pastille
import fr.cedric.tessvine.ui.composants.SurTitre
import fr.cedric.tessvine.ui.composants.charger
import fr.cedric.tessvine.ui.theme.Or
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun EcranMetsVins(nav: Nav) {
    val ref = Session.referentiel
    var plat by rememberSaveable { mutableStateOf("") }
    var couleur by rememberSaveable { mutableStateOf("") }
    var texte by rememberSaveable { mutableStateOf("") }
    var conseil by remember { mutableStateOf<ConseilPlat?>(null) }
    var reflexion by remember { mutableStateOf(false) }
    var sortir by remember { mutableStateOf<Vin?>(null) }
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbar.current
    val focus = LocalFocusManager.current
    val tuiles = charger(plat, couleur) { if (conseil == null) Session.api.suggestion(plat, couleur) else emptyList() }

    fun demander() {
        val q = texte.trim()
        if (q.isEmpty()) { conseil = null; tuiles.recharger(); return }
        focus.clearFocus()
        scope.launch {
            reflexion = true
            try {
                val r = Session.api.conseil(q, couleur)
                conseil = r
                plat = if (r.source == "regles") r.plat ?: "" else ""
            } catch (e: CancellationException) { throw e } catch (e: Exception) {
                snackbar.showSnackbar(
                    if ((e as? fr.cedric.tessvine.data.ErreurApi)?.code == 404)
                        "La recherche libre demande le patch « recherche de plat » sur Tessvine"
                    else e.message ?: "Erreur", withDismissAction = true,
                )
            } finally { reflexion = false }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 30.dp)) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
            SurTitre("Accords")
            Text("Mets & Vins", style = MaterialTheme.typography.headlineMedium)
            Text("Que mangez-vous ? La cave classe ses bouteilles de la mieux accordée à la moins accordée, et à accord égal, celles à boire en priorité.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedTextField(
            texte, { texte = it },
            placeholder = { Text("Décrivez votre plat : magret aux cerises, raclette…") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { demander() }),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        )
        Button(onClick = { demander() }, enabled = !reflexion, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Icon(Icons.Filled.WineBar, null); Spacer(Modifier.width(8.dp)); Text("Conseiller")
        }
        if (reflexion) {
            Text("Le sommelier réfléchit à « ${texte.trim()} »…", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        }
        conseil?.let { c ->
            if (c.conseil.isNotBlank()) Carte(Modifier.padding(16.dp)) {
                SurTitre(if (c.source == "ia") "Conseil du sommelier (IA)" else "Conseil de la cave")
                Spacer(Modifier.height(6.dp))
                Text(c.conseil)
            }
        }
        FlowRow(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ref.plats.forEach { (k, l) ->
                val actif = plat == k
                Column(
                    Modifier.width(104.dp).clip(RoundedCornerShape(16.dp))
                        .background(if (actif) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.dp, if (actif) Or else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .clickable { conseil = null; texte = ""; plat = if (actif) "" else k }
                        .padding(vertical = 12.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(ref.plats_icones[k] ?: "🍽️", fontSize = 30.sp)
                    Text(l, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 2)
                }
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(couleur == "", { couleur = ""; if (conseil != null) demander() }, { Text("Toutes couleurs") })
            ref.couleurs.forEach { (k, l) ->
                FilterChip(couleur == k, { couleur = k; if (conseil != null) demander() }, { Text(l) }, leadingIcon = { Pastille(k, 12) })
            }
        }
        val resultats: List<Suggestion> = conseil?.suggestions ?: tuiles.donnees ?: emptyList()
        if (resultats.isEmpty() && (conseil != null || plat.isNotBlank()) && !reflexion) {
            Carte(Modifier.padding(16.dp)) {
                SurTitre("Aucun accord")
                Text("Aucune bouteille prête à boire ne s'accorde avec ce plat. Essayez une autre couleur ou un autre plat.")
            }
        }
        resultats.forEachIndexed { i, s ->
            CarteSuggestion(s, meilleure = i == 0 && (plat.isNotBlank() || conseil != null), ouvrir = { nav.vin(s.vin.id) }, deguster = { sortir = s.vin })
        }
    }
    sortir?.let { v -> DialogueSortie(v, fermer = { sortir = null }, apres = { if (conseil != null) demander() }) }
}

private fun niveauAccord(n: Int) = when {
    n >= 75 -> "Accord parfait"
    n >= 55 -> "Très bon accord"
    n >= 35 -> "Bon accord"
    else -> "Accord possible"
}

@Composable
private fun CarteSuggestion(s: Suggestion, meilleure: Boolean, ouvrir: () -> Unit, deguster: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(if (meilleure) 1.5.dp else 1.dp, if (meilleure) Or else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable(onClick = ouvrir).padding(14.dp)
    ) {
        s.accord?.let { a ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(progress = { a / 100f }, modifier = Modifier.width(70.dp), color = Or)
                Spacer(Modifier.width(8.dp))
                Text(niveauAccord(a), style = MaterialTheme.typography.labelMedium, color = Or)
            }
            Spacer(Modifier.height(6.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pastille(s.vin.couleur, 18)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(titreVin(s.vin.nom, s.vin.millesime), style = MaterialTheme.typography.titleMedium)
                Text(sousTitre(s.vin.appellation, s.vin.domaine, s.vin.region), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("×${s.vin.stock}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (s.raisons.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(s.raisons.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = deguster, modifier = Modifier.align(Alignment.End)) {
            Icon(Icons.Filled.WineBar, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Déguster")
        }
    }
}
