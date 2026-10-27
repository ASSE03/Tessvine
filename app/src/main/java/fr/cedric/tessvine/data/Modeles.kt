package fr.cedric.tessvine.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// Modèles de l'API Tessvine. Les noms suivent exactement le JSON du serveur (snake_case) ;
// tout est facultatif avec une valeur par défaut pour rester compatible avec les futures versions.

@Serializable
data class Vin(
    val id: Int = 0,
    val nom: String = "",
    val domaine: String = "",
    val appellation: String = "",
    val region: String = "",
    val pays: String = "",
    val couleur: String = "",
    val millesime: Int? = null,
    val cepages: String = "",
    val format: String = "",
    val degre: Double? = null,
    val prix_achat: Double? = null,
    val valeur: Double? = null,
    val fournisseur: String = "",
    val garde_debut: Int? = null,
    val garde_apogee: Int? = null,
    val garde_fin: Int? = null,
    val accords: String = "",
    val notes: String = "",
    val favori: Boolean = false,
    val code_barre: String = "",
    val garde_source: String = "",
    val garde_ia_note: String = "",
    val garde_ia_date: String? = null,
    val note_perso: String = "",
    val note_date: String = "",
    val stock: Int = 0,
    val statut: String = "",
    val statut_label: String = "",
    val couleur_label: String = "",
    val valeur_unitaire: Double? = null,
    val a_photo: Boolean = false,
    val updated_at: String = "",
    val bouteilles: List<Bouteille> = emptyList(),
    val mouvements: List<Mouvement> = emptyList(),
    val note_moyenne: Double? = null,
    val mouvement_id: Int? = null,
)

@Serializable
data class Bouteille(
    val id: Int = 0,
    val vin_id: Int = 0,
    val cave_id: Int? = null,
    val rangee: Int? = null,
    val colonne: Int? = null,
    val date_entree: String = "",
    val cave_nom: String? = null,
    val cave_racine: Int? = null,
    val nom: String = "",
    val millesime: Int? = null,
    val couleur: String = "",
    val domaine: String = "",
    val appellation: String = "",
    val statut: String = "",
    val a_photo: Boolean = false,
    val updated_at: String = "",
)

@Serializable
data class Mouvement(
    val id: Int = 0,
    val vin_id: Int? = null,
    val vin_nom: String = "",
    val millesime: Int? = null,
    val type: String = "",
    val quantite: Int = 1,
    val raison: String = "",
    val note: Double? = null,
    val commentaire: String = "",
    val date: String = "",
    val cree_le: String? = null,
    val annulation: String? = null,
)

@Serializable
data class Profondeur(
    val largeur: Int = 0,
    val hauteur: Int = 0,
    val empilement: String = "aligne",
    val sens: String = "avant",
    val sans: List<List<Int>> = emptyList(),
)

/** Une cave telle que la liste des caves la renvoie (compteurs). */
@Serializable
data class CaveResume(
    val id: Int = 0,
    val nom: String = "",
    val nom_complet: String = "",
    val parent_id: Int? = null,
    val refrigere: Int = 0,
    val bouteilles: Int = 0,
    val capacite: Int = 0,
)

/** Une cave avec son plan et ses bouteilles (et ses étagères pour une cave principale). */
@Serializable
data class Cave(
    val id: Int = 0,
    val nom: String = "",
    val parent_id: Int? = null,
    val position: String = "",
    val ref_id: Int? = null,
    val nom_casier: String = "",
    val refrigere: Int = 0,
    val rangees: Int = 0,
    val colonnes: Int = 0,
    val profondeurs: List<Profondeur> = emptyList(),
    val capacite: Int = 0,
    val bouteilles: List<Bouteille> = emptyList(),
    val etageres: List<Cave> = emptyList(),
)

@Serializable
data class Groupe(
    val cle: JsonElement? = null,
    val label: String = "",
    val bouteilles: Int = 0,
)

@Serializable
data class GroupeCave(
    val id: Int? = null,
    val nom: String = "",
    val bouteilles: Int = 0,
    val capacite: Int = 0,
)

@Serializable
data class PlaceLibre(
    val cave_id: Int = 0,
    val casier_id: Int = 0,
    val casier_nom: String = "",
    val libres: Int = 0,
    val suggestion: Candidat? = null,
)

@Serializable
data class Candidat(
    val bouteille_id: Int = 0,
    val a_deplacer: Boolean = false,
    val depuis: String = "",
    val vin_id: Int = 0,
    val nom: String = "",
    val millesime: Int? = null,
    val appellation: String = "",
    val couleur: String = "",
    val valeur: Double? = null,
    val raisons: List<String> = emptyList(),
)

@Serializable
data class Stats(
    val annee: Int = 0,
    val bouteilles: Int = 0,
    val references: Int = 0,
    val sans_emplacement: Int = 0,
    val valeur: Double = 0.0,
    val valeur_payee: Double = 0.0,
    val bues_annee: Int = 0,
    val entrees_mois: Int = 0,
    val sorties_mois: Int = 0,
    val place_libre: PlaceLibre? = null,
    val par_couleur: List<Groupe> = emptyList(),
    val par_region: List<Groupe> = emptyList(),
    val par_millesime: List<Groupe> = emptyList(),
    val par_statut: List<Groupe> = emptyList(),
    val par_cave: List<GroupeCave> = emptyList(),
    val urgents: List<Vin> = emptyList(),
    val occasions: List<Vin> = emptyList(),
    val a_boire: List<Vin> = emptyList(),
    val derniers_mouvements: List<Mouvement> = emptyList(),
)

@Serializable
data class Suggestion(
    val score: Double = 0.0,
    val accord: Int? = null,
    val raisons: List<String> = emptyList(),
    val vin: Vin = Vin(),
)

@Serializable
data class ConseilPlat(
    val source: String = "",
    val plat: String? = null,
    val conseil: String = "",
    val suggestions: List<Suggestion> = emptyList(),
)

@Serializable
data class Referentiel(
    val couleurs: Map<String, String> = emptyMap(),
    val statuts: Map<String, String> = emptyMap(),
    val plats: Map<String, String> = emptyMap(),
    val plats_icones: Map<String, String> = emptyMap(),
    val raisons_entree: Map<String, String> = emptyMap(),
    val raisons_sortie: Map<String, String> = emptyMap(),
    val autocompletion: Map<String, List<String>> = emptyMap(),
    val version: String = "",
    val nom_cave: String = "Tessvine",
)

@Serializable
data class Envie(
    val id: Int = 0,
    val nom: String = "",
    val details: String = "",
    val fait: Boolean = false,
)

@Serializable
data class EtatIa(
    val possible: Boolean = false,
    val raison: String? = null,
    val entite: String? = null,
)

@Serializable
data class EtatGardes(
    val en_cours: Boolean = false,
    val fait: Int = 0,
    val total: Int = 0,
    val erreurs: List<String> = emptyList(),
    val derniere: String? = null,
    val manquants: Int? = null,
    val ia: EtatIa = EtatIa(),
)

@Serializable
data class IdLibelle(val id: String = "", val label: String = "")

@Serializable
data class Capacites(
    val detourage: Boolean = false,
    val fonds: List<IdLibelle> = emptyList(),
    val fond_defaut: String = "",
    val cadrages: List<IdLibelle> = emptyList(),
    val cadrage_defaut: String = "etiquette",
    val ia: EtatIa = EtatIa(),
    val ia_texte: EtatIa = EtatIa(),
)

@Serializable
data class ResultatScan(
    val code: String = "",
    val local: Vin? = null,
    val externe: JsonObject? = null,
    val erreur: String? = null,
)

@Serializable
data class Brouillon(val id: String = "")

@Serializable
data class LectureEtiquette(val champs: JsonObject = JsonObject(emptyMap()))

@Serializable
data class Sante(val status: String = "", val version: String = "")
