@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package fr.cedric.tessvine.ui.ecrans

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.cedric.tessvine.data.Bouteille
import fr.cedric.tessvine.data.Cave
import fr.cedric.tessvine.data.Profondeur
import fr.cedric.tessvine.data.Session
import fr.cedric.tessvine.data.Vin
import fr.cedric.tessvine.data.lettreRang
import fr.cedric.tessvine.data.nomPlace
import fr.cedric.tessvine.data.obj
import fr.cedric.tessvine.data.sousTitre
import fr.cedric.tessvine.data.titreVin
import fr.cedric.tessvine.ui.Nav
import fr.cedric.tessvine.ui.composants.BadgeStatut
import fr.cedric.tessvine.ui.composants.Contenu
import fr.cedric.tessvine.ui.composants.DialogueSortie
import fr.cedric.tessvine.ui.composants.LocalSnackbar
import fr.cedric.tessvine.ui.composants.Pastille
import fr.cedric.tessvine.ui.composants.SurTitre
import fr.cedric.tessvine.ui.composants.action
import fr.cedric.tessvine.ui.composants.charger
import fr.cedric.tessvine.ui.theme.Or
import fr.cedric.tessvine.ui.theme.couleurStatut
import fr.cedric.tessvine.ui.theme.couleurVin

private const val PAS_PROFONDEUR = 100

/** Une place du plan, en coordonnées de cases (x, y), avec la bouteille qui l'occupe. */
private data class Place(val etage: Int, val colonne: Int, val rangee: Int, val x: Float, val y: Float, val bouteille: Bouteille?)

private fun profondeursDe(c: Cave): List<Profondeur> =
    c.profondeurs.ifEmpty { if (c.rangees > 0 && c.colonnes > 0) listOf(Profondeur(c.colonnes, c.rangees)) else emptyList() }

/** Places d'un rang (profondeur p, 0 = devant) : quinconce d'une demi-bouteille un étage sur deux si décalé. */
private fun places(c: Cave, p: Int): List<Place> {
    val prof = profondeursDe(c).getOrNull(p) ?: return emptyList()
    val occupees = c.bouteilles.filter { it.rangee != null && it.colonne != null }.associateBy { it.rangee!! to it.colonne!! }
    val pas = if (prof.empilement != "aligne" && prof.hauteur > 1) 0.87f else 1f
    val sans = prof.sans.map { it.getOrNull(0) to it.getOrNull(1) }.toSet()
    val liste = mutableListOf<Place>()
    for (etage in 1..prof.hauteur) {
        val niveau = prof.hauteur - etage
        val decal = if (niveau % 2 == 1) when (prof.empilement) { "droite" -> 0.5f; "gauche" -> -0.5f; else -> 0f } else 0f
        for (col in 1..prof.largeur) {
            if ((etage to col) in sans) continue
            val rangee = p * PAS_PROFONDEUR + etage
            liste += Place(etage, col, rangee, (col - 1) + decal, (etage - 1) * pas, occupees[rangee to col])
        }
    }
    val x0 = liste.minOfOrNull { it.x } ?: 0f
    return liste.map { it.copy(x = it.x - x0) }
}

private enum class Couleurs { VIN, GARDE }

@Composable
fun EcranPlan(nav: Nav, caveDemandee: Int, vinSurligne: Int, placerDemande: Int) {
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val caves = charger { Session.api.caves() }
    val vinCible = charger(vinSurligne) { if (vinSurligne > 0) Session.api.vin(vinSurligne) else null }
    var caveId by remember { mutableIntStateOf(caveDemandee) }
    LaunchedEffect(caves.donnees, vinCible.donnees) {
        if (caveId == 0) {
            caveId = vinCible.donnees?.bouteilles?.firstNotNullOfOrNull { it.cave_racine }
                ?: caves.donnees?.firstOrNull { it.parent_id == null }?.id ?: 0
        }
    }
    val cave = charger(caveId) { if (caveId > 0) Session.api.cave(caveId) else null }
    val aRanger = charger("non-rangees") { Session.api.nonRangees() }

    var profondeur by remember { mutableIntStateOf(0) }
    var recherche by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(Couleurs.VIN) }
    var aPlacer by remember { mutableStateOf<Bouteille?>(null) }   // bouteille à poser sur une place libre
    var choisie by remember { mutableStateOf<Pair<Cave, Place>?>(null) }
    var placeLibre by remember { mutableStateOf<Pair<Cave, Place>?>(null) }
    var sortir by remember { mutableStateOf<Pair<Vin, Int>?>(null) }

    // « Ranger celle-ci » depuis l'accueil ou la fiche : la bouteille à placer est connue
    LaunchedEffect(placerDemande, aRanger.donnees) {
        if (placerDemande > 0 && aPlacer == null) aPlacer = aRanger.donnees?.firstOrNull { it.id == placerDemande }
    }

    fun poser(b: Bouteille, casier: Cave, place: Place) = scope.action(snackbar) {
        Session.api.put<Bouteille>("bouteilles/${b.id}", obj("cave_id" to casier.id, "rangee" to place.rangee, "colonne" to place.colonne))
        aPlacer = null
        Session.changement()
        snackbar.showSnackbar("${titreVin(b.nom, b.millesime)} rangé en ${nomPlace(place.rangee, place.colonne)}")
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
            SurTitre("Cave")
            Text("Plan de cave", style = MaterialTheme.typography.headlineMedium)
        }
        val racines = caves.donnees?.filter { it.parent_id == null } ?: emptyList()
        if (racines.size > 1) Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            racines.forEach { c -> FilterChip(caveId == c.id, { caveId = c.id; profondeur = 0 }, { Text("${c.nom} (${c.bouteilles}/${c.capacite})") }) }
        }
        Contenu(cave) { c0 ->
            val c = c0 ?: run {
                Text("Aucune cave n'est créée. Créez-la dans Tessvine (Plan de cave → Nouvelle cave).", Modifier.padding(16.dp))
                return@Contenu
            }
            val casiers = listOf(c) + c.etageres
            val nbProf = casiers.maxOf { profondeursDe(it).size }
            val q = recherche.trim().lowercase()
            val surligne: (Bouteille) -> Boolean = { b ->
                when {
                    vinSurligne > 0 && q.isEmpty() -> b.vin_id == vinSurligne
                    q.isEmpty() -> false
                    else -> b.nom.lowercase().contains(q) || b.millesime?.toString() == q || b.domaine.lowercase().contains(q) ||
                        nomPlace(b.rangee, b.colonne).lowercase() == q
                }
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    recherche, { recherche = it },
                    placeholder = { Text("Chercher : nom, millésime ou place (B4)") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = { if (recherche.isNotEmpty()) IconButton(onClick = { recherche = "" }) { Icon(Icons.Filled.Close, null) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                )
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(mode == Couleurs.VIN, { mode = Couleurs.VIN }, { Text("Couleur du vin") })
                    FilterChip(mode == Couleurs.GARDE, { mode = Couleurs.GARDE }, { Text("Garde") })
                    if (nbProf > 1) for (p in 0 until nbProf) {
                        FilterChip(profondeur == p, { profondeur = p }, { Text(if (p == 0) "Devant" else if (nbProf == 2) "Derrière" else "Rang ${p + 1}") })
                    }
                }
                aPlacer?.let { b ->
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Pastille(b.couleur, 16)
                        Spacer(Modifier.width(10.dp))
                        Text("Touchez une place libre pour ranger ${titreVin(b.nom, b.millesime)}", Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                        TextButton(onClick = { aPlacer = null }) { Text("Annuler") }
                    }
                }
                casiers.forEach { casier ->
                    if (profondeursDe(casier).size > profondeur) {
                        VueCasier(
                            casier, if (casier.id == c.id) (c.nom_casier.ifBlank { c.nom }) else nomEtagere(casier),
                            profondeur, mode, surligne, aPlacer != null,
                        ) { place ->
                            val b = place.bouteille
                            val enAttente = aPlacer
                            when {
                                b != null -> choisie = casier to place
                                enAttente != null -> poser(enAttente, casier, place)
                                else -> placeLibre = casier to place
                            }
                        }
                    }
                }
                Legende(mode)
                val liste = aRanger.donnees ?: emptyList()
                if (liste.isNotEmpty()) {
                    Titre("À ranger (${liste.size})")
                    liste.forEach { b ->
                        Row(
                            Modifier.fillMaxWidth().clickable { aPlacer = b }.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Pastille(b.couleur, 16)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(titreVin(b.nom, b.millesime))
                                Text(sousTitre(b.domaine, b.appellation), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(if (aPlacer?.id == b.id) "choisie" else "Ranger ›", color = Or)
                        }
                    }
                }
                Spacer(Modifier.height(30.dp))
            }
        }
    }

    // fenêtre d'une bouteille du plan
    choisie?.let { (casier, place) ->
        val b = place.bouteille!!
        ModalBottomSheet(onDismissRequest = { choisie = null }) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pastille(b.couleur, 22)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(titreVin(b.nom, b.millesime), style = MaterialTheme.typography.titleLarge)
                        Text(sousTitre(b.domaine, b.appellation), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text("${nomEtagereOuCave(casier)} · ${nomPlace(place.rangee, place.colonne)}", color = MaterialTheme.colorScheme.secondary)
                BadgeStatut(b.statut, Session.referentiel.statuts[b.statut] ?: "")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        choisie = null
                        scope.action(snackbar) { sortir = Session.api.vin(b.vin_id) to b.id }
                    }) { Text("Déguster / sortir") }
                    FilledTonalButton(onClick = { choisie = null; aPlacer = b }) { Text("Déplacer") }
                    OutlinedButton(onClick = { choisie = null; nav.vin(b.vin_id) }) { Text("Voir la fiche") }
                }
            }
        }
    }

    // place libre touchée hors mode « ranger » : choisir une bouteille à y mettre
    placeLibre?.let { (casier, place) ->
        ModalBottomSheet(onDismissRequest = { placeLibre = null }) {
            Column(Modifier.padding(bottom = 28.dp)) {
                Text("Ranger en ${nomPlace(place.rangee, place.colonne)}", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp))
                val liste = aRanger.donnees ?: emptyList()
                if (liste.isEmpty()) Text("Aucune bouteille à ranger. Pour déplacer une bouteille, touchez-la puis « Déplacer ».",
                    Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                liste.forEach { b ->
                    Row(
                        Modifier.fillMaxWidth().clickable { placeLibre = null; poser(b, casier, place) }.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Pastille(b.couleur, 16); Spacer(Modifier.width(12.dp)); Text(titreVin(b.nom, b.millesime))
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }

    sortir?.let { (vin, bouteille) -> DialogueSortie(vin, bouteilleId = bouteille, fermer = { sortir = null }) }
}

private fun nomEtagere(c: Cave): String = c.nom.ifBlank {
    when (c.position) { "haut" -> "Étagère du haut"; "gauche" -> "Étagère de gauche"; "droite" -> "Étagère de droite"; else -> "Étagère du bas" }
}

private fun nomEtagereOuCave(c: Cave) = if (c.parent_id == null) c.nom else nomEtagere(c)

@Composable
private fun VueCasier(
    casier: Cave, titre: String, profondeur: Int, mode: Couleurs,
    surligne: (Bouteille) -> Boolean, rangement: Boolean, toucher: (Place) -> Unit,
) {
    val liste = places(casier, profondeur)
    if (liste.isEmpty()) return
    val largeur = liste.maxOf { it.x } + 1f
    val hauteur = liste.maxOf { it.y } + 1f
    val prof = profondeursDe(casier)[profondeur]
    val occupees = liste.count { it.bouteille != null }
    val unSurligne = liste.any { p -> p.bouteille?.let(surligne) == true }
    val clignote = rememberInfiniteTransition(label = "clignote")
    val pulsation by clignote.animateFloat(0.55f, 1f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "pulsation")

    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(titre, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("$occupees / ${liste.size}" + if (casier.refrigere == 1) "  ❄" else "",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(8.dp))
        BoxWithConstraints(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF221E25), Color(0xFF141216))))
                .padding(10.dp)
        ) {
            val etiquette = 20f   // marge des lettres et des numéros
            val case = ((maxWidth.value - etiquette) / largeur).coerceIn(22f, 64f)
            Box(Modifier.size((etiquette + largeur * case).dp, (etiquette + hauteur * case).dp)) {
                // numéros de colonne (sur la première rangée) et lettres de rangée
                for (col in 1..prof.largeur) {
                    Text("$col", fontSize = 11.sp, color = Color(0xFF8D8790), textAlign = TextAlign.Center,
                        modifier = Modifier.offset((etiquette + (col - 1) * case).dp, 0.dp).width(case.dp))
                }
                liste.map { it.etage to it.y }.distinct().forEach { (etage, y) ->
                    Text(lettreRang(etage), fontSize = 11.sp, color = Color(0xFF8D8790),
                        modifier = Modifier.offset(0.dp, (etiquette + y * case + case / 2 - 8).dp))
                }
                liste.forEach { p ->
                    val b = p.bouteille
                    val estSurligne = b != null && surligne(b)
                    val teinte = when {
                        b == null -> Color.Transparent
                        mode == Couleurs.GARDE -> couleurStatut(b.statut)
                        else -> couleurVin(b.couleur)
                    }
                    Box(
                        Modifier.offset((etiquette + p.x * case).dp, (etiquette + p.y * case).dp).size(case.dp).padding(2.dp)
                            .then(if (estSurligne) Modifier.scale(1.05f + 0.08f * pulsation) else Modifier)
                            .alpha(if (unSurligne && !estSurligne) 0.35f else 1f)
                            .clip(CircleShape)
                            .background(
                                if (b == null) Brush.radialGradient(listOf(Color(0xFF2B2730), Color(0xFF19171C)))
                                else Brush.radialGradient(listOf(teinte.copy(alpha = 0.6f), teinte, teinte.copy(red = teinte.red * 0.6f, green = teinte.green * 0.6f, blue = teinte.blue * 0.6f)))
                            )
                            .border(
                                when {
                                    estSurligne -> 2.5.dp
                                    b == null && rangement -> 1.5.dp
                                    else -> 1.dp
                                },
                                when {
                                    estSurligne -> Or.copy(alpha = pulsation)
                                    b == null && rangement -> Color(0xFF5BAA6A)
                                    b == null -> Color(0xFF3A3540)
                                    else -> Color.Black.copy(alpha = 0.35f)
                                },
                                CircleShape,
                            )
                            .clickable { toucher(p) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (b != null) {
                            // goulot (capsule) au centre, comme une bouteille couchée vue de face
                            Box(Modifier.size((case * 0.28f).dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)))
                            if (b.statut == "urgent" || b.statut == "depasse") {
                                Box(Modifier.align(Alignment.TopEnd).size((case * 0.24f).dp).clip(CircleShape).background(Color(0xFFD9534F)))
                            }
                        } else {
                            Text(nomPlace(p.rangee, p.colonne).substringBefore(' '), fontSize = (case * 0.22f).sp, color = Color(0xFF5A5360))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Legende(mode: Couleurs) {
    val ref = Session.referentiel
    FlowRow(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        if (mode == Couleurs.VIN) ref.couleurs.forEach { (k, l) ->
            Row(verticalAlignment = Alignment.CenterVertically) { Pastille(k, 12); Spacer(Modifier.width(5.dp)); Text(l, fontSize = 12.sp) }
        } else ref.statuts.forEach { (k, l) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(couleurStatut(k))); Spacer(Modifier.width(5.dp)); Text(l, fontSize = 12.sp)
            }
        }
    }
}
