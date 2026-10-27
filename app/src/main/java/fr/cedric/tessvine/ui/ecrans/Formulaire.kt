@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package fr.cedric.tessvine.ui.ecrans

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import fr.cedric.tessvine.data.Brouillon
import fr.cedric.tessvine.data.Capacites
import fr.cedric.tessvine.data.LectureEtiquette
import fr.cedric.tessvine.data.Session
import fr.cedric.tessvine.data.Vin
import fr.cedric.tessvine.data.obj
import fr.cedric.tessvine.data.titreVin
import fr.cedric.tessvine.ui.Nav
import fr.cedric.tessvine.ui.composants.Carte
import fr.cedric.tessvine.ui.composants.Contenu
import fr.cedric.tessvine.ui.composants.LocalSnackbar
import fr.cedric.tessvine.ui.composants.Pastille
import fr.cedric.tessvine.ui.composants.SurTitre
import fr.cedric.tessvine.ui.composants.action
import fr.cedric.tessvine.ui.composants.charger
import fr.cedric.tessvine.ui.composants.nouveauFichierPhoto
import fr.cedric.tessvine.ui.composants.photoEnDataUrl
import fr.cedric.tessvine.ui.composants.scannerCodeBarres
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

private val TEXTES = listOf(
    "nom", "domaine", "appellation", "region", "pays", "cepages", "format", "fournisseur",
    "accords", "notes", "code_barre", "note_perso", "note_date",
)
private val ENTIERS = listOf("millesime", "garde_debut", "garde_apogee", "garde_fin")
private val REELS = listOf("degre", "prix_achat", "valeur")
private val GARDE = listOf("garde_debut", "garde_apogee", "garde_fin")

/** Champs du formulaire d'un vin, en texte (convertis au moment d'enregistrer). */
class EtatFiche {
    val champs = mutableStateMapOf<String, String>()
    var couleur by mutableStateOf("rouge")
    private val initiaux = mutableMapOf<String, String>()

    operator fun get(cle: String) = champs[cle] ?: ""
    operator fun set(cle: String, v: String) { champs[cle] = v }

    fun depuisVin(v: Vin) {
        val m = mapOf(
            "nom" to v.nom, "domaine" to v.domaine, "appellation" to v.appellation, "region" to v.region, "pays" to v.pays,
            "cepages" to v.cepages, "format" to v.format, "fournisseur" to v.fournisseur, "accords" to v.accords,
            "notes" to v.notes, "code_barre" to v.code_barre, "note_perso" to v.note_perso, "note_date" to v.note_date,
            "millesime" to (v.millesime?.toString() ?: ""), "garde_debut" to (v.garde_debut?.toString() ?: ""),
            "garde_apogee" to (v.garde_apogee?.toString() ?: ""), "garde_fin" to (v.garde_fin?.toString() ?: ""),
            "degre" to nombre(v.degre), "prix_achat" to nombre(v.prix_achat), "valeur" to nombre(v.valeur),
        )
        champs.putAll(m); initiaux.putAll(m)
        couleur = v.couleur.ifBlank { "rouge" }
    }

    /** Remplit depuis la lecture d'étiquette de l'IA (sans écraser ce que l'utilisateur a déjà saisi). */
    fun depuisIa(lu: Map<String, JsonElement>) {
        lu.forEach { (k, e) ->
            val txt = (e as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
            if (txt.isBlank() || txt == "null") return@forEach
            when (k) {
                "couleur" -> couleur = txt
                "description" -> if (this["notes"].isBlank()) this["notes"] = txt
                in ENTIERS -> this[k] = txt.substringBefore('.')
                in TEXTES, in REELS -> if (this[k].isBlank()) this[k] = txt
            }
        }
    }

    fun versJson(seulementGardeModifiee: Boolean): Map<String, Any?> {
        val r = mutableMapOf<String, Any?>("couleur" to couleur)
        TEXTES.forEach { r[it] = this[it].trim() }
        ENTIERS.forEach { r[it] = this[it].trim().toIntOrNull() }
        REELS.forEach { r[it] = this[it].trim().replace(',', '.').toDoubleOrNull() }
        if (seulementGardeModifiee) GARDE.forEach { if (this[it] == (initiaux[it] ?: "")) r.remove(it) }
        return r
    }

    private fun nombre(d: Double?) = when {
        d == null -> ""
        d == Math.floor(d) -> d.toLong().toString()
        else -> d.toString()
    }
}

/** Le formulaire complet d'un vin. */
@Composable
fun FormulaireVin(f: EtatFiche) {
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val auto = Session.referentiel.autocompletion
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Champ(f, "nom", "Nom du vin *")
        Champ(f, "domaine", "Domaine / château", auto["domaines"])
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Session.referentiel.couleurs.ifEmpty { mapOf("rouge" to "Rouge", "blanc" to "Blanc", "rose" to "Rosé") }.forEach { (k, l) ->
                FilterChip(f.couleur == k, { f.couleur = k }, { Text(l) }, leadingIcon = { Pastille(k, 12) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Champ(f, "millesime", "Millésime", clavier = KeyboardType.Number, modifier = Modifier.weight(1f))
            Champ(f, "format", "Format", auto["formats"], modifier = Modifier.weight(1f))
        }
        Champ(f, "appellation", "Appellation", auto["appellations"])
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Champ(f, "region", "Région", auto["regions"], modifier = Modifier.weight(1f))
            Champ(f, "pays", "Pays", auto["pays"], modifier = Modifier.weight(1f))
        }
        Champ(f, "cepages", "Cépages")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Champ(f, "degre", "Degré", clavier = KeyboardType.Decimal, modifier = Modifier.weight(1f))
            Champ(f, "prix_achat", "Prix d'achat €", clavier = KeyboardType.Decimal, modifier = Modifier.weight(1f))
            Champ(f, "valeur", "Valeur €", clavier = KeyboardType.Decimal, modifier = Modifier.weight(1f))
        }
        Champ(f, "fournisseur", "Fournisseur", auto["fournisseurs"])

        SurTitre("Garde", Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Champ(f, "garde_debut", "À partir de", clavier = KeyboardType.Number, modifier = Modifier.weight(1f))
            Champ(f, "garde_apogee", "Apogée", clavier = KeyboardType.Number, modifier = Modifier.weight(1f))
            Champ(f, "garde_fin", "Avant", clavier = KeyboardType.Number, modifier = Modifier.weight(1f))
        }
        OutlinedButton(onClick = {
            scope.action(snackbar) {
                val q = fr.cedric.tessvine.data.Api.params(
                    "couleur" to f.couleur, "millesime" to f["millesime"], "region" to f["region"],
                    "appellation" to f["appellation"], "valeur" to f["valeur"].ifBlank { f["prix_achat"] },
                )
                val g = fr.cedric.tessvine.data.json.parseToJsonElement(Session.api.brut("GET", "garde?$q")) as kotlinx.serialization.json.JsonObject
                listOf("garde_debut", "garde_apogee", "garde_fin").forEach { k ->
                    g[k]?.jsonPrimitive?.contentOrNull?.let { f[k] = it }
                }
                snackbar.showSnackbar(g["niveau_label"]?.jsonPrimitive?.contentOrNull?.let { "Estimation : $it" } ?: "Garde estimée")
            }
        }) { Text("Estimer la garde") }

        SurTitre("Accords & notes", Modifier.padding(top = 8.dp))
        Champ(f, "accords", "Accords mets-vins", lignes = 2)
        Champ(f, "notes", "Notes", lignes = 3)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Champ(f, "code_barre", "Code-barres", clavier = KeyboardType.Number, modifier = Modifier.weight(1f))
            IconButton(onClick = {
                scope.action(snackbar) { scannerCodeBarres(context)?.let { f["code_barre"] = it } }
            }) { Icon(Icons.Filled.QrCodeScanner, "Scanner") }
        }
        SurTitre("Occasion", Modifier.padding(top = 8.dp))
        Champ(f, "note_perso", "Pour quelle occasion ? (« Anniversaire de Paul »)")
        Champ(f, "note_date", "Date (AAAA-MM-JJ) : rappel 7 jours avant")
    }
}

@Composable
private fun Champ(
    f: EtatFiche, cle: String, libelle: String, suggestions: List<String>? = null,
    clavier: KeyboardType = KeyboardType.Text, lignes: Int = 1, modifier: Modifier = Modifier.fillMaxWidth(),
) {
    Column(modifier) {
        OutlinedTextField(
            f[cle], { f[cle] = it }, label = { Text(libelle) },
            singleLine = lignes == 1, minLines = lignes,
            keyboardOptions = KeyboardOptions(keyboardType = clavier),
            modifier = Modifier.fillMaxWidth(),
        )
        // propositions issues de la cave (domaines, appellations…)
        val saisie = f[cle]
        if (!suggestions.isNullOrEmpty() && saisie.length >= 2) {
            val props = suggestions.filter { it.contains(saisie, ignoreCase = true) && !it.equals(saisie, ignoreCase = true) }.take(3)
            if (props.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                props.forEach { p -> TextButton(onClick = { f[cle] = p }) { Text(p, maxLines = 1) } }
            }
        }
    }
}

// ---------------------------------------------------------------------- modifier un vin

@Composable
fun EcranEdition(nav: Nav, id: Int) {
    val etat = charger(id) { Session.api.vin(id) }
    val f = remember { EtatFiche() }
    var pret by remember { mutableStateOf(false) }
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(etat.donnees) { etat.donnees?.let { if (!pret) { f.depuisVin(it); pret = true } } }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Modifier") },
            navigationIcon = { IconButton(onClick = nav::retour) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") } },
            actions = {
                TextButton(enabled = pret, onClick = {
                    scope.action(snackbar) {
                        if (f["nom"].isBlank()) error("Le nom est obligatoire")
                        Session.api.put<Vin>("vins/$id", obj(*f.versJson(seulementGardeModifiee = true).toList().toTypedArray()))
                        Session.changement()
                        nav.retour()
                    }
                }) { Text("Enregistrer") }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )
        Contenu(etat) {
            if (pret) Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                FormulaireVin(f)
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------- ajouter un vin

private enum class Etape { CHOIX, VERSO, LECTURE, FICHE }

@Composable
fun EcranAjout(nav: Nav, codeInitial: String) {
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val capacites = charger("capacites") { Session.api.capacites() }.donnees ?: Capacites()
    val caves = charger("caves") { Session.api.caves() }.donnees ?: emptyList()
    val f = remember { EtatFiche().also { if (codeInitial.isNotBlank()) it["code_barre"] = codeInitial } }
    var etape by remember { mutableStateOf(if (codeInitial.isNotBlank()) Etape.FICHE else Etape.CHOIX) }
    var brouillon by remember { mutableStateOf<String?>(null) }
    var fond by remember { mutableStateOf("") }
    var travail by remember { mutableStateOf<String?>(null) }   // message pendant une opération longue
    var quantite by remember { mutableIntStateOf(1) }
    var cave by remember { mutableStateOf<Int?>(null) }
    var raison by remember { mutableStateOf("achat") }
    var existant by remember { mutableStateOf<Vin?>(null) }
    var fichier by remember { mutableStateOf<Uri?>(null) }
    var photoVerso by remember { mutableStateOf(false) }

    LaunchedEffect(capacites.fond_defaut) { if (fond.isBlank()) fond = capacites.fond_defaut }
    LaunchedEffect(caves) { if (cave == null) cave = caves.firstOrNull { it.parent_id == null }?.id }

    fun lire(verso: String?) = scope.action(snackbar) {
        val id = brouillon ?: return@action
        if (!capacites.ia.possible) { etape = Etape.FICHE; return@action }
        etape = Etape.LECTURE
        travail = "L'IA lit l'étiquette…"
        try {
            val lu = Session.api.post<LectureEtiquette>("brouillons/$id/lire", obj("verso" to verso))
            f.depuisIa(lu.champs)
            // ce vin est-il déjà dans la cave ?
            if (f["nom"].isNotBlank()) {
                existant = Session.api.vins(f["nom"], stock = "all").firstOrNull {
                    it.nom.equals(f["nom"], true) && it.millesime?.toString() == f["millesime"].ifBlank { null }
                }
            }
        } catch (e: Exception) {
            snackbar.showSnackbar("Lecture de l'étiquette impossible : ${e.message}. Complétez la fiche à la main.", withDismissAction = true)
        } finally {
            travail = null
            etape = Etape.FICHE
        }
    }

    val appareil = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = fichier
        if (!ok || uri == null) return@rememberLauncherForActivityResult
        if (!photoVerso) {
            scope.action(snackbar) {
                travail = "Préparation de la photo…"
                try {
                    val image = photoEnDataUrl(context, uri)
                    brouillon = Session.api.post<Brouillon>("brouillons", obj("image" to image)).id
                    etape = Etape.VERSO
                } finally { travail = null }
            }
        } else {
            scope.action(snackbar) {
                val verso = photoEnDataUrl(context, uri, 1400)
                lire(verso)
            }
        }
    }

    fun photographier(verso: Boolean) {
        photoVerso = verso
        val uri = nouveauFichierPhoto(context)
        fichier = uri
        appareil.launch(uri)
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Ajouter un vin") },
            navigationIcon = { IconButton(onClick = nav::retour) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            travail?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            brouillon?.let { id ->
                // Le détourage se fait sur le serveur (jusqu'à une minute la première fois) : on le prépare d'abord,
                // puis on affiche l'image déjà composée.
                var affiche by remember(id) { mutableStateOf<String?>(null) }
                var preparation by remember(id) { mutableStateOf(false) }
                LaunchedEffect(id, fond, capacites.cadrage_defaut) {
                    if (fond.isBlank()) { affiche = ""; return@LaunchedEffect }
                    preparation = true
                    try {
                        Session.api.brut("POST", "brouillons/$id/rendu", obj("fond" to fond, "cadrage" to capacites.cadrage_defaut))
                        affiche = fond
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        affiche = ""
                        snackbar.showSnackbar("Détourage impossible : ${e.message}. Photo d'origine conservée.", withDismissAction = true)
                    } finally { preparation = false }
                }
                val choisi = affiche
                if (choisi != null) AsyncImage(
                    model = Session.api.url("brouillons/$id/image?fond=$choisi&cadrage=${capacites.cadrage_defaut}"),
                    contentDescription = "Photo de la bouteille",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp).clip(RoundedCornerShape(16.dp)),
                )
                if (preparation) {
                    Text("Détourage de la bouteille… (jusqu'à une minute la première fois)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                if (capacites.detourage && capacites.fonds.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(fond == "", { fond = "" }, { Text("Photo d'origine") })
                    capacites.fonds.forEach { x -> FilterChip(fond == x.id, { fond = x.id }, { Text(x.label) }) }
                }
            }
            when (etape) {
                Etape.CHOIX -> {
                    Carte {
                        SurTitre("Le plus simple")
                        Spacer(Modifier.height(6.dp))
                        Text("Photographiez l'étiquette, de face, la bouteille entière dans l'image." +
                            if (capacites.ia.possible) " L'IA remplira la fiche." else " (IA indisponible : vous compléterez la fiche à la main.)")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { photographier(false) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.PhotoCamera, null); Spacer(Modifier.width(8.dp)); Text("Photographier la bouteille")
                        }
                    }
                    OutlinedButton(onClick = {
                        scope.action(snackbar) {
                            val code = scannerCodeBarres(context) ?: return@action
                            f["code_barre"] = code
                            val r = Session.api.scan(code)
                            r.local?.let { existant = it }
                            (r.externe?.get("vin") as? kotlinx.serialization.json.JsonObject)?.let { lu ->
                                if (f["nom"].isBlank()) f.depuisIa(lu)
                            }
                            etape = Etape.FICHE
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.QrCodeScanner, null); Spacer(Modifier.width(8.dp)); Text("Scanner le code-barres")
                    }
                    OutlinedButton(onClick = { etape = Etape.FICHE }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Edit, null); Spacer(Modifier.width(8.dp)); Text("Saisir à la main")
                    }
                }
                Etape.VERSO -> Carte {
                    SurTitre("Le verso")
                    Spacer(Modifier.height(6.dp))
                    Text("La contre-étiquette complète la fiche (cépages, degré…) et donne le code-barres pour sortir la bouteille d'un scan.")
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { photographier(true) }) { Icon(Icons.Filled.PhotoCamera, null); Spacer(Modifier.width(6.dp)); Text("Photographier") }
                        OutlinedButton(onClick = { lire(null) }) { Text("Ignorer") }
                    }
                }
                Etape.LECTURE -> {}
                Etape.FICHE -> {
                    existant?.let { v ->
                        Carte {
                            SurTitre("Déjà dans votre cave")
                            Text(titreVin(v.nom, v.millesime), style = MaterialTheme.typography.titleMedium)
                            Text("${v.stock} bouteille(s) en cave", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = { nav.retour(); nav.vin(v.id) }) { Text("Ajouter à ce vin") }
                                TextButton(onClick = { existant = null }) { Text("C'est un autre vin") }
                            }
                        }
                    }
                    if (brouillon != null && capacites.ia.possible) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(6.dp))
                            Text("Fiche pré-remplie par l'IA : vérifiez-la.", color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    FormulaireVin(f)
                    SurTitre("Bouteilles", Modifier.padding(top = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Nombre", Modifier.weight(1f))
                        OutlinedButton(onClick = { if (quantite > 1) quantite-- }) { Text("−") }
                        Text("$quantite", Modifier.padding(horizontal = 14.dp), style = MaterialTheme.typography.titleLarge)
                        OutlinedButton(onClick = { quantite++ }) { Text("+") }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Session.referentiel.raisons_entree.ifEmpty { mapOf("achat" to "Achat", "cadeau" to "Cadeau") }.forEach { (k, l) ->
                            FilterChip(raison == k, { raison = k }, { Text(l) })
                        }
                    }
                    Text("Où les ranger ?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(cave == null, { cave = null }, { Text("Non rangées") })
                        caves.forEach { c ->
                            FilterChip(cave == c.id, { cave = c.id }, { Text("${c.nom_complet.ifBlank { c.nom }} (${c.capacite - c.bouteilles} libres)") })
                        }
                    }
                    Text(
                        "Les bouteilles prennent les premières places libres ; vous pourrez les déplacer sur le plan.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        enabled = travail == null,
                        onClick = {
                            scope.action(snackbar) {
                                if (f["nom"].isBlank()) error("Le nom du vin est obligatoire")
                                travail = "Enregistrement…"
                                try {
                                    val corps = f.versJson(false).toMutableMap()
                                    corps["quantite"] = quantite
                                    corps["raison"] = raison
                                    corps["cave_id"] = cave
                                    brouillon?.let { corps["photo"] = mapOf("brouillon" to it, "fond" to fond) }
                                    val v = Session.api.post<Vin>("vins", obj(*corps.toList().toTypedArray()))
                                    Session.changement()
                                    nav.retour()
                                    nav.vin(v.id)
                                    snackbar.showSnackbar("${titreVin(v.nom, v.millesime)} ajouté à la cave")
                                } finally { travail = null }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Ajouter à ma cave") }
                    Spacer(Modifier.height(30.dp))
                }
            }
        }
    }
}
