@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package fr.cedric.tessvine.ui.ecrans

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import fr.cedric.tessvine.data.Capacites
import fr.cedric.tessvine.data.Session
import fr.cedric.tessvine.data.Vin
import fr.cedric.tessvine.data.euros
import fr.cedric.tessvine.data.nomPlace
import fr.cedric.tessvine.data.obj
import fr.cedric.tessvine.data.sousTitre
import fr.cedric.tessvine.ui.Nav
import fr.cedric.tessvine.ui.composants.BadgeStatut
import fr.cedric.tessvine.ui.composants.Carte
import fr.cedric.tessvine.ui.composants.ChoixPuces
import fr.cedric.tessvine.ui.composants.Contenu
import fr.cedric.tessvine.ui.composants.DialogueSortie
import fr.cedric.tessvine.ui.composants.LocalSnackbar
import fr.cedric.tessvine.ui.composants.SurTitre
import fr.cedric.tessvine.ui.composants.action
import fr.cedric.tessvine.ui.composants.charger
import fr.cedric.tessvine.ui.composants.nouveauFichierPhoto
import fr.cedric.tessvine.ui.composants.photoEnDataUrl
import fr.cedric.tessvine.ui.theme.couleurVin

@Composable
fun EcranFiche(nav: Nav, id: Int) {
    val etat = charger(id) { Session.api.vin(id) }
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    var sortie by remember { mutableStateOf<Int?>(null) }   // 0 = n'importe laquelle, sinon id de bouteille
    var entree by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var supprimer by remember { mutableStateOf(false) }
    var gardeIa by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(etat.donnees?.nom ?: "", maxLines = 1) },
            navigationIcon = { IconButton(onClick = nav::retour) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") } },
            actions = {
                val v = etat.donnees
                if (v != null) {
                    IconButton(onClick = {
                        scope.action(snackbar) { Session.api.put<Vin>("vins/${v.id}", obj("favori" to !v.favori)); Session.changement() }
                    }) { Icon(if (v.favori) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Favori") }
                    IconButton(onClick = { nav.edition(v.id) }) { Icon(Icons.Filled.Edit, "Modifier") }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Plus") }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(text = { Text("Supprimer ce vin") }, leadingIcon = { Icon(Icons.Filled.Delete, null) },
                                onClick = { menu = false; supprimer = true })
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )
        Contenu(etat) { v ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
                EnTeteVin(v)
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { sortie = 0 }, enabled = v.stock > 0, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.WineBar, null); Spacer(Modifier.width(6.dp)); Text("Déguster")
                    }
                    FilledTonalButton(onClick = { entree = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Ajouter")
                    }
                }
                val rangee = v.bouteilles.firstOrNull { it.cave_id != null }
                if (rangee != null) {
                    TextButton(onClick = { nav.plan(0, vin = v.id) }, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Icon(Icons.Filled.MyLocation, null); Spacer(Modifier.width(6.dp)); Text("Localiser dans la cave")
                    }
                }
                CarteGarde(v, gardeIa) {
                    gardeIa = true
                    scope.action(snackbar) {
                        try {
                            Session.api.post<Vin>("vins/${v.id}/garde-ia", obj("appliquer" to true))
                            Session.changement()
                            snackbar.showSnackbar("Garde estimée par l'IA")
                        } finally { gardeIa = false }
                    }
                }
                CartePhoto(v)
                CarteInfos(v)
                if (v.bouteilles.isNotEmpty()) {
                    Titre("Bouteilles en cave")
                    v.bouteilles.forEach { b ->
                        Row(
                            Modifier.fillMaxWidth().clickable { if (b.cave_id != null) nav.plan(0, vin = v.id) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(b.cave_nom ?: "Non rangée", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    if (b.cave_id == null) "sans emplacement" else nomPlace(b.rangee, b.colonne),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (b.rangee == null) {
                                TextButton(onClick = { nav.plan(b.cave_id ?: 0, placer = b.id) }) { Text("Ranger") }
                            }
                            TextButton(onClick = { sortie = b.id }) { Text("Sortir") }
                        }
                    }
                }
                if (v.mouvements.isNotEmpty()) {
                    Titre("Historique" + (v.note_moyenne?.let { " · note moyenne $it/5" } ?: ""))
                    v.mouvements.forEach { m -> LigneMouvement(m) {} }
                }
            }
            sortie?.let { b ->
                DialogueSortie(v, bouteilleId = if (b == 0) null else b, fermer = { sortie = null })
            }
            if (entree) DialogueEntree(v) { entree = false }
            if (supprimer) AlertDialog(
                onDismissRequest = { supprimer = false },
                title = { Text("Supprimer ${v.nom} ?") },
                text = { Text("Le vin, ses ${v.stock} bouteille(s) et son historique seront effacés. C'est définitif.") },
                confirmButton = {
                    Button(onClick = {
                        supprimer = false
                        scope.action(snackbar) { Session.api.delete("vins/${v.id}"); Session.changement(); nav.retour() }
                    }) { Text("Supprimer") }
                },
                dismissButton = { TextButton(onClick = { supprimer = false }) { Text("Annuler") } },
            )
        }
    }
}

@Composable
private fun EnTeteVin(v: Vin) {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        if (v.a_photo) {
            AsyncImage(
                model = Session.api.url("vins/${v.id}/photo?v=${v.updated_at}"),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(width = 92.dp, height = 140.dp).clip(RoundedCornerShape(12.dp)),
            )
        } else {
            val c = couleurVin(v.couleur)
            Box(
                Modifier.size(width = 56.dp, height = 140.dp).clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 10.dp, bottomEnd = 10.dp))
                    .background(Brush.verticalGradient(listOf(c.copy(alpha = 0.55f), c)))
            )
        }
        Spacer(Modifier.width(16.dp))
        Column {
            SurTitre(v.couleur_label.ifBlank { Session.couleurLabel(v.couleur) })
            Text(v.nom, style = MaterialTheme.typography.headlineSmall)
            v.millesime?.let { Text("$it", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary) }
            Text(sousTitre(v.domaine, v.appellation, v.region, v.pays), color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BadgeStatut(v.statut, v.statut_label)
                Spacer(Modifier.width(10.dp))
                Text(if (v.stock > 0) "×${v.stock} en cave" else "plus en cave", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun CarteGarde(v: Vin, enCours: Boolean, estimer: () -> Unit) {
    Carte(Modifier.padding(16.dp)) {
        SurTitre("Garde")
        Spacer(Modifier.height(8.dp))
        if (v.garde_debut == null && v.garde_fin == null) {
            Text("Fenêtre de garde non renseignée.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Annee("À partir de", v.garde_debut)
                Annee("Apogée", v.garde_apogee)
                Annee("À boire avant", v.garde_fin)
            }
        }
        val source = when (v.garde_source) {
            "ia" -> "Estimée par l'IA"
            "manuel" -> "Saisie à la main"
            "estimation" -> "Estimation automatique"
            else -> ""
        }
        if (source.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(source, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        }
        if (v.garde_ia_note.isNotBlank()) {
            Text(v.garde_ia_note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(10.dp))
        if (enCours) LinearProgressIndicator(Modifier.fillMaxWidth())
        else OutlinedButton(onClick = estimer) { Icon(Icons.Filled.AutoAwesome, null); Spacer(Modifier.width(6.dp)); Text("Garde par l'IA") }
    }
}

@Composable
private fun Annee(libelle: String, annee: Int?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(annee?.toString() ?: "—", style = MaterialTheme.typography.titleLarge)
        Text(libelle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CartePhoto(v: Vin) {
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val capacites = charger("capacites") { Session.api.capacites() }.donnees ?: Capacites()
    var fichier by remember { mutableStateOf<Uri?>(null) }
    var envoi by remember { mutableStateOf(false) }
    var choixFond by remember { mutableStateOf(false) }
    val appareil = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = fichier
        if (ok && uri != null) {
            envoi = true
            scope.action(snackbar) {
                try {
                    val image = photoEnDataUrl(context, uri)
                    val r = Session.api.brut("PUT", "vins/${v.id}/photo", obj("image" to image, "fond" to capacites.fond_defaut.ifBlank { null }))
                    Session.changement()
                    if (r.contains("avertissement\": \"")) snackbar.showSnackbar("Photo enregistrée sans détourage")
                } finally { envoi = false }
            }
        }
    }
    Carte(Modifier.padding(horizontal = 16.dp)) {
        SurTitre("Photo")
        Spacer(Modifier.height(8.dp))
        if (envoi) {
            Text("Envoi et détourage de la photo…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
        } else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                val uri = nouveauFichierPhoto(context); fichier = uri; appareil.launch(uri)
            }) { Icon(Icons.Filled.PhotoCamera, null); Spacer(Modifier.width(6.dp)); Text(if (v.a_photo) "Reprendre" else "Photographier") }
            if (v.a_photo && capacites.detourage) OutlinedButton(onClick = { choixFond = true }) { Text("Fond") }
            if (v.a_photo) TextButton(onClick = {
                scope.action(snackbar) { Session.api.delete("vins/${v.id}/photo"); Session.changement() }
            }) { Text("Retirer") }
        }
    }
    if (choixFond) AlertDialog(
        onDismissRequest = { choixFond = false },
        title = { Text("Fond de la photo") },
        text = {
            Column {
                (listOf(fr.cedric.tessvine.data.IdLibelle("", "Photo d'origine")) + capacites.fonds).forEach { f ->
                    Text(f.label, Modifier.fillMaxWidth().clickable {
                        choixFond = false
                        envoi = true
                        scope.action(snackbar) {
                            try { Session.api.brut("POST", "vins/${v.id}/photo/fond", obj("fond" to f.id)); Session.changement() }
                            finally { envoi = false }
                        }
                    }.padding(vertical = 12.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = { choixFond = false }) { Text("Fermer") } },
    )
}

@Composable
private fun CarteInfos(v: Vin) {
    val lignes = listOf(
        "Cépages" to v.cepages,
        "Format" to v.format,
        "Degré" to (v.degre?.let { "$it % vol." } ?: ""),
        "Prix d'achat" to euros(v.prix_achat),
        "Valeur estimée" to euros(v.valeur),
        "Fournisseur" to v.fournisseur,
        "Code-barres" to v.code_barre,
    ).filter { it.second.isNotBlank() }
    Carte(Modifier.padding(16.dp)) {
        SurTitre("Fiche")
        Spacer(Modifier.height(8.dp))
        lignes.forEach { (l, x) ->
            Row(Modifier.padding(vertical = 3.dp)) {
                Text(l, Modifier.width(120.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(x)
            }
        }
        if (v.accords.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text("Accords", fontWeight = FontWeight.SemiBold)
            Text(v.accords, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (v.note_perso.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text("Occasion" + (if (v.note_date.isNotBlank()) " · ${dateCourte(v.note_date)}" else ""), fontWeight = FontWeight.SemiBold)
            Text(v.note_perso, color = MaterialTheme.colorScheme.secondary)
        }
        if (v.notes.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text("Notes", fontWeight = FontWeight.SemiBold)
            Text(v.notes, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Ajouter des bouteilles d'un vin déjà connu : quantité, raison et cave (rangées dans les places libres). */
@Composable
fun DialogueEntree(v: Vin, fermer: () -> Unit) {
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val caves = charger("caves") { Session.api.caves() }.donnees ?: emptyList()
    var quantite by remember { mutableIntStateOf(1) }
    var raison by remember { mutableStateOf("achat") }
    var cave by remember { mutableStateOf<Int?>(null) }
    val raisons = Session.referentiel.raisons_entree.ifEmpty { mapOf("achat" to "Achat", "cadeau" to "Cadeau") }
    AlertDialog(
        onDismissRequest = fermer,
        title = { Text("Ajouter des bouteilles") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bouteilles", Modifier.weight(1f))
                    OutlinedButton(onClick = { if (quantite > 1) quantite-- }) { Text("−") }
                    Text("$quantite", Modifier.padding(horizontal = 14.dp), style = MaterialTheme.typography.titleLarge)
                    OutlinedButton(onClick = { quantite++ }) { Text("+") }
                }
                ChoixPuces(raisons, raison) { raison = it }
                Text("Où les ranger ?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(cave == null, { cave = null }, { Text("Non rangées") })
                    caves.forEach { c ->
                        FilterChip(cave == c.id, { cave = c.id }, { Text("${c.nom_complet.ifBlank { c.nom }} (${c.capacite - c.bouteilles} libres)") })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                scope.action(snackbar) {
                    Session.api.post<Vin>("vins/${v.id}/entree", obj("quantite" to quantite, "raison" to raison, "cave_id" to cave))
                    fermer()
                    Session.changement()
                    snackbar.showSnackbar("$quantite bouteille(s) ajoutée(s)")
                }
            }) { Text("Ajouter") }
        },
        dismissButton = { TextButton(onClick = fermer) { Text("Annuler") } },
    )
}
