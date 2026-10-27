@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package fr.cedric.tessvine.ui.composants

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.cedric.tessvine.data.Session
import fr.cedric.tessvine.data.Vin
import fr.cedric.tessvine.data.obj
import fr.cedric.tessvine.data.sousTitre
import fr.cedric.tessvine.data.titreVin
import fr.cedric.tessvine.ui.theme.Surtitre
import fr.cedric.tessvine.ui.theme.couleurStatut
import fr.cedric.tessvine.ui.theme.couleurVin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

val LocalSnackbar = compositionLocalOf { SnackbarHostState() }

/** Portée de coroutines de l'application entière : survit à la fermeture d'un dialogue (pour « Annuler »). */
val LocalPortee = compositionLocalOf<CoroutineScope> { error("Portée non fournie") }

// ---------------------------------------------------------------------- chargement des données

class Etat<T>(val donnees: T?, val erreur: String?, val chargement: Boolean, val recharger: () -> Unit)

/** Charge des données (et les recharge quand une clé change ou après une modification dans l'appli). */
@Composable
fun <T> charger(vararg cles: Any?, bloc: suspend () -> T): Etat<T> {
    var essai by remember { mutableIntStateOf(0) }
    var donnees by remember { mutableStateOf<T?>(null) }
    var erreur by remember { mutableStateOf<String?>(null) }
    var chargement by remember { mutableStateOf(true) }
    val blocActuel by rememberUpdatedState(bloc)
    LaunchedEffect(*cles, essai, Session.version) {
        chargement = true
        try {
            donnees = blocActuel()
            erreur = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            erreur = e.message ?: "Erreur inconnue"
        }
        chargement = false
    }
    return Etat(donnees, erreur, chargement) { essai++ }
}

/** Affiche le contenu une fois chargé, sinon un indicateur ou l'erreur avec « Réessayer ». */
@Composable
fun <T> Contenu(etat: Etat<T>, contenu: @Composable (T) -> Unit) {
    val d = etat.donnees
    when {
        d != null -> contenu(d)
        etat.erreur != null -> MessageErreur(etat.erreur, etat.recharger)
        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    }
}

@Composable
fun MessageErreur(message: String, reessayer: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Oups", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (reessayer != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = reessayer) {
                Icon(Icons.Filled.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Réessayer")
            }
        }
    }
}

/** Lance une action réseau et affiche son erreur éventuelle dans la barre du bas. */
fun CoroutineScope.action(snackbar: SnackbarHostState, bloc: suspend () -> Unit) = launch {
    try {
        bloc()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        snackbar.showSnackbar(e.message ?: "Erreur", withDismissAction = true)
    }
}

// ---------------------------------------------------------------------- éléments visuels

@Composable
fun Pastille(couleur: String, taille: Int = 14) {
    val c = couleurVin(couleur)
    Box(
        Modifier.size(taille.dp).clip(CircleShape)
            .background(Brush.radialGradient(listOf(c.copy(alpha = 0.75f), c)))
            .border(1.dp, Color.Black.copy(alpha = 0.25f), CircleShape)
    )
}

@Composable
fun BadgeStatut(statut: String, label: String) {
    if (label.isBlank()) return
    val c = couleurStatut(statut)
    Text(
        label,
        fontSize = 12.sp,
        color = c,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(c.copy(alpha = 0.14f)).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
fun SurTitre(texte: String, modifier: Modifier = Modifier) {
    Text(texte.uppercase(), style = Surtitre, modifier = modifier)
}

@Composable
fun Carte(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, contenu: @Composable () -> Unit) {
    val couleurs = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    val bord = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), colors = couleurs, border = bord, shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp)) { contenu() }
        }
    } else {
        Card(modifier = modifier.fillMaxWidth(), colors = couleurs, border = bord, shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp)) { contenu() }
        }
    }
}

/** Une ligne de vin dans une liste. */
@Composable
fun LigneVin(vin: Vin, onClick: () -> Unit, droite: @Composable (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Pastille(vin.couleur, 18)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(titreVin(vin.nom, vin.millesime), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                sousTitre(vin.domaine, vin.appellation),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (vin.statut_label.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                BadgeStatut(vin.statut, vin.statut_label)
            }
        }
        if (droite != null) droite() else if (vin.stock > 0) {
            Text("×${vin.stock}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun Etoiles(note: Int, changer: ((Int) -> Unit)? = null, taille: Int = 30) {
    Row {
        for (i in 1..5) {
            val pleine = i <= note
            val mod = if (changer != null) Modifier.clickable { changer(if (note == i) 0 else i) } else Modifier
            Icon(
                if (pleine) Icons.Filled.Star else Icons.Filled.StarBorder, null,
                tint = if (pleine) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                modifier = mod.size(taille.dp),
            )
        }
    }
}

@Composable
fun ChoixPuces(options: Map<String, String>, choisi: String, choisir: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (k, l) -> FilterChip(selected = k == choisi, onClick = { choisir(k) }, label = { Text(l) }) }
    }
}

@Composable
fun Chiffre(valeur: String, libelle: String, modifier: Modifier = Modifier, couleur: Color = MaterialTheme.colorScheme.onSurface) {
    Column(modifier) {
        Text(valeur, style = MaterialTheme.typography.headlineSmall, color = couleur, fontWeight = FontWeight.Normal)
        Text(libelle.uppercase(), fontSize = 11.sp, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------------- dégustation / sortie

/**
 * Dialogue « Déguster / sortir » : quantité, raison, note et commentaire.
 * Après la sortie, la barre du bas propose « Annuler » pendant quelques secondes.
 */
@Composable
fun DialogueSortie(vin: Vin, bouteilleId: Int? = null, fermer: () -> Unit, apres: () -> Unit = {}) {
    val snackbar = LocalSnackbar.current
    val scope = LocalPortee.current
    var quantite by remember { mutableIntStateOf(1) }
    var raison by remember { mutableStateOf("bue") }
    var note by remember { mutableIntStateOf(0) }
    var commentaire by remember { mutableStateOf("") }
    var envoi by remember { mutableStateOf(false) }
    val raisons = Session.referentiel.raisons_sortie.ifEmpty {
        mapOf("bue" to "Bue", "offerte" to "Offerte", "cassee" to "Cassée / bouchonnée", "vendue" to "Vendue", "autre" to "Autre")
    }
    AlertDialog(
        onDismissRequest = fermer,
        title = { Text(titreVin(vin.nom, vin.millesime)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (bouteilleId == null && vin.stock > 1) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Bouteilles", Modifier.weight(1f))
                        OutlinedButton(onClick = { if (quantite > 1) quantite-- }) { Text("−") }
                        Text("$quantite", Modifier.padding(horizontal = 14.dp), style = MaterialTheme.typography.titleLarge)
                        OutlinedButton(onClick = { if (quantite < vin.stock) quantite++ }) { Text("+") }
                    }
                }
                ChoixPuces(raisons, raison) { raison = it }
                if (raison == "bue") {
                    Text("Votre note", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Etoiles(note, { note = it })
                }
                OutlinedTextField(commentaire, { commentaire = it }, label = { Text("Commentaire") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = {
            Button(enabled = !envoi, onClick = {
                envoi = true
                scope.action(snackbar) {
                    try {
                        val r = Session.api.post<Vin>(
                            "vins/${vin.id}/sortie",
                            obj(
                                "quantite" to quantite, "raison" to raison,
                                "note" to (if (raison == "bue" && note > 0) note else null),
                                "commentaire" to commentaire, "bouteille_id" to bouteilleId,
                            ),
                        )
                        fermer()
                        Session.changement()
                        apres()
                        val mid = r.mouvement_id
                        val res = snackbar.showSnackbar(
                            if (quantite > 1) "$quantite bouteilles sorties" else "Bouteille sortie",
                            actionLabel = if (mid != null) "Annuler" else null,
                            duration = SnackbarDuration.Long,
                        )
                        if (res == SnackbarResult.ActionPerformed && mid != null) {
                            Session.api.brut("POST", "mouvements/$mid/annuler")
                            Session.changement()
                            snackbar.showSnackbar("Sortie annulée : la bouteille a retrouvé sa place")
                        }
                    } finally {
                        envoi = false
                    }
                }
            }) { Text(if (raison == "bue") "Déguster" else "Sortir") }
        },
        dismissButton = { TextButton(onClick = fermer) { Text("Annuler") } },
    )
}

@Composable
fun BoutonIcone(icone: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(icone, description) }
}
