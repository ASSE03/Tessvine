@file:OptIn(ExperimentalMaterial3Api::class)

package fr.cedric.tessvine.ui.ecrans

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.cedric.tessvine.data.Session
import fr.cedric.tessvine.ui.Nav
import fr.cedric.tessvine.ui.composants.Contenu
import fr.cedric.tessvine.ui.composants.LigneVin
import fr.cedric.tessvine.ui.composants.Pastille
import fr.cedric.tessvine.ui.composants.SurTitre
import fr.cedric.tessvine.ui.composants.charger
import fr.cedric.tessvine.ui.composants.rememberSortieParScan
import kotlinx.coroutines.delay

@Composable
fun EcranVins(nav: Nav) {
    var recherche by rememberSaveable { mutableStateOf("") }
    var couleur by rememberSaveable { mutableStateOf("") }
    var statut by rememberSaveable { mutableStateOf("") }
    var stock by rememberSaveable { mutableStateOf("1") }
    val etat = charger(recherche, couleur, statut, stock) {
        if (recherche.isNotBlank()) delay(300)  // on attend la fin de la frappe
        Session.api.vins(recherche, couleur, statut, stock)
    }
    val scanner = rememberSortieParScan(nav)
    val ref = Session.referentiel

    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    SurTitre("Collection")
                    Text("Mes vins", style = MaterialTheme.typography.headlineMedium)
                }
                IconButton(onClick = scanner) { Icon(Icons.Filled.QrCodeScanner, "Sortir par code-barres") }
            }
            OutlinedTextField(
                recherche, { recherche = it },
                placeholder = { Text("Nom, domaine, appellation, millésime…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = { if (recherche.isNotEmpty()) IconButton(onClick = { recherche = "" }) { Icon(Icons.Filled.Close, "Effacer") } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(stock == "1", { stock = if (stock == "1") "all" else "1" }, { Text(if (stock == "1") "En cave" else "Tous") })
                ref.couleurs.forEach { (k, l) ->
                    FilterChip(couleur == k, { couleur = if (couleur == k) "" else k }, { Text(l) }, leadingIcon = { Pastille(k, 12) })
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ref.statuts.forEach { (k, l) -> FilterChip(statut == k, { statut = if (statut == k) "" else k }, { Text(l) }) }
            }
            Contenu(etat) { vins ->
                PullToRefreshBox(isRefreshing = etat.chargement, onRefresh = etat.recharger) {
                    if (vins.isEmpty()) {
                        Text("Aucun vin ne correspond.", Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 88.dp)) {
                        item {
                            val bouteilles = vins.sumOf { it.stock }
                            Text(
                                "${vins.size} vin${if (vins.size > 1) "s" else ""} · $bouteilles bouteille${if (bouteilles > 1) "s" else ""}",
                                Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        items(vins, key = { it.id }) { v ->
                            LigneVin(v, { nav.vin(v.id) })
                            HorizontalDivider(Modifier.padding(start = 48.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { nav.ajout() },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
        ) { Icon(Icons.Filled.Add, "Ajouter un vin") }
    }
}
