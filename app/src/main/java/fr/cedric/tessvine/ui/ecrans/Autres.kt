@file:OptIn(ExperimentalMaterial3Api::class)

package fr.cedric.tessvine.ui.ecrans

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import fr.cedric.tessvine.data.EtatGardes
import fr.cedric.tessvine.data.Groupe
import fr.cedric.tessvine.data.Mouvement
import fr.cedric.tessvine.data.Session
import fr.cedric.tessvine.data.Vin
import fr.cedric.tessvine.data.euros
import fr.cedric.tessvine.data.obj
import fr.cedric.tessvine.ui.Nav
import fr.cedric.tessvine.ui.composants.Carte
import fr.cedric.tessvine.ui.composants.Chiffre
import fr.cedric.tessvine.ui.composants.Contenu
import fr.cedric.tessvine.ui.composants.LocalSnackbar
import fr.cedric.tessvine.ui.composants.SurTitre
import fr.cedric.tessvine.ui.composants.action
import fr.cedric.tessvine.ui.composants.charger
import fr.cedric.tessvine.ui.theme.Or
import fr.cedric.tessvine.ui.theme.couleurStatut
import fr.cedric.tessvine.ui.theme.couleurVin
import kotlinx.coroutines.delay
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Composable
private fun Barre(titre: String, nav: Nav) {
    TopAppBar(
        title = { Text(titre) },
        navigationIcon = { IconButton(onClick = nav::retour) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") } },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

// ---------------------------------------------------------------------- « Plus »

@Composable
fun EcranPlus(nav: Nav) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)) {
            SurTitre("Tessvine")
            Text("Plus", style = MaterialTheme.typography.headlineMedium)
        }
        Entree(Icons.Filled.Checklist, "Envies", "Vins à acheter") { nav.ecran("envies") }
        Entree(Icons.Filled.BarChart, "Statistiques", "Répartition, valeur, garde") { nav.ecran("stats") }
        Entree(Icons.Filled.History, "Historique", "Entrées et sorties, annulation") { nav.ecran("historique") }
        Entree(Icons.Filled.GridView, "Plan de cave", "Ranger et déplacer les bouteilles") { nav.plan() }
        Entree(Icons.Filled.Settings, "Réglages", "IA, garde, connexion") { nav.ecran("reglages") }
        Entree(Icons.Filled.Wifi, "Connexion au serveur", Session.api.base ?: Session.prefs.urlMaison) { nav.ecran("serveur") }
        Text(
            "Appli Tessvine · serveur ${Session.referentiel.version}",
            Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Entree(icone: ImageVector, titre: String, detail: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(titre) },
        supportingContent = { Text(detail) },
        leadingContent = { Icon(icone, null, tint = Or) },
        modifier = Modifier.clickable(onClick = onClick),
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

// ---------------------------------------------------------------------- envies

@Composable
fun EcranEnvies(nav: Nav) {
    val etat = charger { Session.api.envies() }
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    var nom by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        Barre("Envies", nav)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(nom, { nom = it }, label = { Text("Vin à acheter") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(details, { details = it }, label = { Text("Détails (millésime, caviste, prix…)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(enabled = nom.isNotBlank(), onClick = {
                scope.action(snackbar) {
                    Session.api.brut("POST", "envies", obj("nom" to nom.trim(), "details" to details.trim()))
                    nom = ""; details = ""; Session.changement()
                }
            }) { Text("Ajouter") }
        }
        Contenu(etat) { envies ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 8.dp)) {
                if (envies.isEmpty()) Text("Aucune envie pour l'instant.", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                envies.forEach { e ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(e.fait, { f ->
                            scope.action(snackbar) { Session.api.brut("PUT", "envies/${e.id}", obj("fait" to f)); Session.changement() }
                        })
                        Column(Modifier.weight(1f)) {
                            Text(e.nom, textDecoration = if (e.fait) TextDecoration.LineThrough else null)
                            if (e.details.isNotBlank()) Text(e.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { scope.action(snackbar) { Session.api.delete("envies/${e.id}"); Session.changement() } }) {
                            Icon(Icons.Filled.Delete, "Supprimer")
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------- statistiques

@Composable
fun EcranStats(nav: Nav) {
    val etat = charger { Session.api.stats() }
    Column(Modifier.fillMaxSize()) {
        Barre("Statistiques", nav)
        Contenu(etat) { s ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 30.dp)) {
                Carte(Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Chiffre("${s.bouteilles}", "bouteilles")
                        Chiffre("${s.references}", "références")
                        Chiffre(euros(s.valeur), "valeur")
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Chiffre("${s.bues_annee}", "bues en ${s.annee}")
                        Chiffre("${s.entrees_mois}", "entrées du mois")
                        if (s.valeur_payee > 0) Chiffre(euros(s.valeur_payee), "payé")
                    }
                }
                Barres("Par couleur", s.par_couleur) { g -> couleurVin(g.cle?.jsonPrimitive?.contentOrNull ?: "") }
                Barres("Par garde", s.par_statut.filter { it.bouteilles > 0 }) { g -> couleurStatut(g.cle?.jsonPrimitive?.contentOrNull ?: "") }
                Barres("Par région", s.par_region)
                Barres("Par millésime", s.par_millesime)
                Barres("Par cave", s.par_cave.map { Groupe(null, if (it.capacite > 0) "${it.nom} (${it.bouteilles}/${it.capacite})" else it.nom, it.bouteilles) })
            }
        }
    }
}

@Composable
private fun Barres(titre: String, groupes: List<Groupe>, couleur: (Groupe) -> androidx.compose.ui.graphics.Color = { Or }) {
    if (groupes.isEmpty()) return
    val max = groupes.maxOf { it.bouteilles }.coerceAtLeast(1)
    Titre(titre)
    groupes.forEach { g ->
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(g.label, Modifier.width(130.dp), maxLines = 1, style = MaterialTheme.typography.bodySmall)
            Box(Modifier.weight(1f).height(14.dp)) {
                Box(
                    Modifier.fillMaxWidth(g.bouteilles / max.toFloat()).height(14.dp).clip(RoundedCornerShape(7.dp))
                        .background(couleur(g))
                )
            }
            Text("${g.bouteilles}", Modifier.width(36.dp).padding(start = 8.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ---------------------------------------------------------------------- historique

@Composable
fun EcranHistorique(nav: Nav) {
    val etat = charger { Session.api.mouvements(300) }
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        Barre("Historique", nav)
        Contenu(etat) { liste ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                liste.forEach { m: Mouvement ->
                    LigneMouvement(m) { m.vin_id?.let { nav.vin(it) } }
                    if (m.type == "sortie" && m.annulation != null && recente(m.cree_le)) {
                        TextButton(onClick = {
                            scope.action(snackbar) {
                                Session.api.post<Vin>("mouvements/${m.id}/annuler")
                                Session.changement()
                                snackbar.showSnackbar("Sortie annulée")
                            }
                        }, modifier = Modifier.padding(start = 60.dp)) { Text("Annuler cette sortie") }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

/** Une sortie s'annule pendant 30 minutes (règle du serveur). */
private fun recente(creeLe: String?): Boolean = try {
    val t = java.time.LocalDateTime.parse(creeLe ?: return false)
    java.time.Duration.between(t, java.time.LocalDateTime.now()).toMinutes() < 30
} catch (e: Exception) { false }

// ---------------------------------------------------------------------- réglages

@Composable
fun EcranReglages(nav: Nav) {
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    var gardes by remember { mutableStateOf<EtatGardes?>(null) }
    var erreur by remember { mutableStateOf<String?>(null) }
    var tic by remember { mutableStateOf(0) }
    LaunchedEffect(tic) {
        while (true) {
            try { gardes = Session.api.gardes(); erreur = null } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; erreur = e.message }
            if (gardes?.en_cours != true) break
            delay(2000)
        }
    }
    fun lancer(manquants: Boolean) = scope.action(snackbar) {
        gardes = Session.api.post<EtatGardes>("ia/gardes", obj("manquants" to manquants))
        tic++
    }

    Column(Modifier.fillMaxSize()) {
        Barre("Réglages", nav)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Carte {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = Or)
                    Spacer(Modifier.width(8.dp))
                    Text("IA & garde", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))
                val g = gardes
                when {
                    erreur != null -> Text(erreur ?: "", color = MaterialTheme.colorScheme.error)
                    g == null -> LinearProgressIndicator(Modifier.fillMaxWidth())
                    g.en_cours -> {
                        Text("Estimation en cours : ${g.fait} / ${g.total} vins…")
                        LinearProgressIndicator(progress = { if (g.total > 0) g.fait / g.total.toFloat() else 0f }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    }
                    else -> {
                        Text(
                            if (g.ia.possible) "✓ IA de Home Assistant disponible" else "IA indisponible : ${g.ia.raison ?: "inconnue"}",
                            color = if (g.ia.possible) androidx.compose.ui.graphics.Color(0xFF5BAA6A) else MaterialTheme.colorScheme.error,
                        )
                        Text(
                            g.derniere?.let { "Dernier passage : ${dateCourte(it)}" } ?: "Pas encore de passage.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (g.erreurs.isNotEmpty()) Text(
                            "${g.erreurs.size} erreur(s) : " + g.erreurs.take(3).joinToString(" ; ") { it.take(120) },
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
                        )
                        if (g.ia.possible) {
                            Spacer(Modifier.height(8.dp))
                            val manquants = g.manquants ?: 0
                            if (manquants > 0) Button(onClick = { lancer(true) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Estimer les vins manquants ($manquants)")
                            }
                            OutlinedButton(onClick = { lancer(false) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Réestimer toute la cave")
                            }
                        }
                    }
                }
            }
            Carte(onClick = { nav.ecran("serveur") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Wifi, null, tint = Or)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Connexion au serveur", style = MaterialTheme.typography.titleMedium)
                        Text(Session.api.base ?: "non connecté", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text(
                "Les autres réglages (notifications, capteurs de température, fonds de photo, langue) se font dans Tessvine, depuis Home Assistant.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
