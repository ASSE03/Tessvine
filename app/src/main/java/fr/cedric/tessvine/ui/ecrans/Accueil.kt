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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.cedric.tessvine.data.Mouvement
import fr.cedric.tessvine.data.Session
import fr.cedric.tessvine.data.euros
import fr.cedric.tessvine.data.titreVin
import fr.cedric.tessvine.ui.Nav
import fr.cedric.tessvine.ui.composants.Carte
import fr.cedric.tessvine.ui.composants.Chiffre
import fr.cedric.tessvine.ui.composants.Contenu
import fr.cedric.tessvine.ui.composants.LigneVin
import fr.cedric.tessvine.ui.composants.SurTitre
import fr.cedric.tessvine.ui.composants.charger
import fr.cedric.tessvine.ui.composants.rememberSortieParScan
import fr.cedric.tessvine.ui.theme.Or

@Composable
fun EcranAccueil(nav: Nav) {
    val etat = charger { Session.api.stats() }
    val scanner = rememberSortieParScan(nav)

    Contenu(etat) { s ->
        PullToRefreshBox(isRefreshing = etat.chargement, onRefresh = etat.recharger) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
                item {
                    // bandeau : nombre de bouteilles et chiffres clés
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(22.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF4A1A2A), Color(0xFF1E1B22))))
                            .padding(22.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SurTitre(Session.referentiel.nom_cave.ifBlank { "Tessvine" }, Modifier.weight(1f))
                            Icon(Icons.Filled.Settings, "Réglages", tint = Color(0xFFB9B0A6),
                                modifier = Modifier.clickable { nav.ecran("reglages") })
                        }
                        Text("${s.bouteilles}", fontSize = 76.sp, fontFamily = FontFamily.Serif, color = Color(0xFFF3E9DA))
                        Text(
                            if (s.bouteilles > 1) "bouteilles dans votre cave" else "bouteille dans votre cave",
                            fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 20.sp, color = Color(0xFFB9B0A6),
                        )
                        Spacer(Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            Chiffre("${s.references}", "références", couleur = Color(0xFFF3E9DA))
                            Chiffre(euros(s.valeur), "valeur", couleur = Color(0xFFF3E9DA))
                            Chiffre("${s.bues_annee}", "bues en ${s.annee}", couleur = Color(0xFFF3E9DA))
                        }
                        if (s.sans_emplacement > 0) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "${s.sans_emplacement} à ranger ›", color = Or,
                                modifier = Modifier.clickable { nav.plan() },
                            )
                        }
                    }
                }
                item {
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { nav.ajout() }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Ajouter")
                        }
                        FilledTonalButton(onClick = scanner, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.QrCodeScanner, null); Spacer(Modifier.width(6.dp)); Text("Sortir (scan)")
                        }
                    }
                }
                s.place_libre?.let { p ->
                    val v = p.suggestion
                    item {
                        Carte(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AcUnit, null, tint = Color(0xFF8FB4E0))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (p.libres > 1) "${p.libres} places libres dans ${p.casier_nom}" else "Une place libre dans ${p.casier_nom}",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            if (v != null) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "${if (v.a_deplacer) "À y mettre" else "Suggestion"} : ${titreVin(v.nom, v.millesime)}" +
                                        (if (v.appellation.isNotBlank()) " · ${v.appellation}" else "") +
                                        (v.valeur?.let { " · ${euros(it)}" } ?: ""),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(onClick = { nav.plan(p.cave_id, placer = v.bouteille_id) }) { Text("Ranger celle-ci") }
                                    OutlinedButton(onClick = { nav.plan(p.cave_id) }) { Text("Voir le plan") }
                                }
                            }
                        }
                    }
                }
                if (s.urgents.isNotEmpty()) {
                    item { Titre("À boire vite") }
                    items(s.urgents.size) { i -> LigneVin(s.urgents[i], { nav.vin(s.urgents[i].id) }) }
                }
                if (s.occasions.isNotEmpty()) {
                    item { Titre("Occasions notées") }
                    items(s.occasions.size) { i ->
                        val v = s.occasions[i]
                        LigneVin(v, { nav.vin(v.id) }) {
                            Text(v.note_perso, style = MaterialTheme.typography.bodySmall, maxLines = 2,
                                modifier = Modifier.width(120.dp), color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
                if (s.a_boire.isNotEmpty()) {
                    item { Titre("Prêts à boire") }
                    val liste = s.a_boire.take(8)
                    items(liste.size) { i -> LigneVin(liste[i], { nav.vin(liste[i].id) }) }
                }
                if (s.derniers_mouvements.isNotEmpty()) {
                    item { Titre("Derniers mouvements") }
                    val liste = s.derniers_mouvements.take(6)
                    items(liste.size) { i -> LigneMouvement(liste[i]) { liste[i].vin_id?.let { nav.vin(it) } } }
                    item {
                        Text("Tout l'historique ›", color = Or, modifier = Modifier.padding(16.dp).clickable { nav.ecran("historique") })
                    }
                }
            }
        }
    }
}

@Composable
fun Titre(texte: String) {
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 4.dp)) {
        SurTitre(texte)
        Spacer(Modifier.height(6.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun LigneMouvement(m: Mouvement, onClick: () -> Unit) {
    val sortie = m.type == "sortie"
    val raisons = if (sortie) Session.referentiel.raisons_sortie else Session.referentiel.raisons_entree
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.width(36.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (sortie) "−${m.quantite}" else "+${m.quantite}",
                color = if (sortie) MaterialTheme.colorScheme.primary else Color(0xFF5BAA6A),
                style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(titreVin(m.vin_nom, m.millesime), style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            val details = listOfNotNull(
                raisons[m.raison] ?: m.raison,
                m.note?.let { "★".repeat(it.toInt()) },
                m.commentaire.ifBlank { null },
            ).joinToString(" · ")
            Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
        Text(dateCourte(m.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** 2026-10-27 → 27/10/26 */
fun dateCourte(iso: String): String {
    val p = iso.take(10).split("-")
    return if (p.size == 3) "${p[2]}/${p[1]}/${p[0].takeLast(2)}" else iso
}
