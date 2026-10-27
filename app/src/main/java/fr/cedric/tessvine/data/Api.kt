package fr.cedric.tessvine.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class ErreurApi(message: String, val code: Int = 0) : Exception(message)

/** Réglages de connexion, gardés sur le téléphone. */
class Preferences(context: Context) {
    private val p = context.getSharedPreferences("tessvine", Context.MODE_PRIVATE)
    var urlMaison: String
        get() = p.getString("url_maison", "http://192.168.1.37:8099") ?: ""
        set(v) = p.edit().putString("url_maison", v.trim().trimEnd('/')).apply()
    var urlExterieur: String
        get() = p.getString("url_exterieur", "") ?: ""
        set(v) = p.edit().putString("url_exterieur", v.trim().trimEnd('/')).apply()
    var cle: String
        get() = p.getString("cle", "") ?: ""
        set(v) = p.edit().putString("cle", v.trim()).apply()
    val configure: Boolean get() = urlMaison.isNotBlank() || urlExterieur.isNotBlank()
    /** Vrai tant que l'écran de connexion n'a jamais été validé. */
    val premierLancement: Boolean get() = !p.contains("url_maison")
}

val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
    explicitNulls = false
    encodeDefaults = true
}

/**
 * Accès à l'API de Tessvine. L'adresse utilisée est la première qui répond parmi
 * l'adresse « maison » (réseau local ou VPN) et l'adresse « extérieur » (https).
 */
class Api(private val prefs: Preferences) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(150, TimeUnit.SECONDS)   // la lecture d'étiquette par l'IA peut être longue
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    private val sonde = client.newBuilder()
        .connectTimeout(2500, TimeUnit.MILLISECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()
    private val typeJson = "application/json; charset=utf-8".toMediaType()

    @Volatile
    var base: String? = null
        private set

    val cle: String get() = prefs.cle

    fun oublierAdresse() { base = null }

    private fun candidates(): List<String> =
        listOf(prefs.urlMaison, prefs.urlExterieur).map { it.trim().trimEnd('/') }.filter { it.isNotBlank() }.distinct()

    /** Teste une adresse ; renvoie un message d'erreur, ou null si Tessvine répond. */
    suspend fun tester(url: String, cle: String): String? = withContext(Dispatchers.IO) {
        val u = url.trim().trimEnd('/')
        if (u.toHttpUrlOrNull() == null) return@withContext "Adresse invalide"
        try {
            val req = Request.Builder().url("$u/api/health").header("X-API-Key", cle).build()
            sonde.newCall(req).execute().use { r ->
                when {
                    r.code == 401 -> "Clé d'API refusée"
                    !r.isSuccessful -> "Erreur ${r.code}"
                    !(r.body?.string() ?: "").contains("\"ok\"") -> "Ce n'est pas un serveur Tessvine"
                    else -> null
                }
            }
        } catch (e: IOException) {
            "Injoignable (${e.message ?: e.javaClass.simpleName})"
        }
    }

    /** Trouve l'adresse qui répond (maison d'abord). */
    suspend fun resoudre(): String {
        base?.let { return it }
        val essais = candidates()
        if (essais.isEmpty()) throw ErreurApi("Aucune adresse de Tessvine n'est configurée")
        val erreurs = mutableListOf<String>()
        for (u in essais) {
            val e = tester(u, prefs.cle)
            if (e == null) { base = u; return u }
            if (e == "Clé d'API refusée") throw ErreurApi("Clé d'API refusée par Tessvine : vérifiez-la dans les réglages", 401)
            erreurs += "$u : $e"
        }
        val conseils = buildList {
            if (essais.any { it.contains("nabu.casa") })
                add("• L'adresse Nabu Casa ne fonctionne pas avec l'appli : retirez-la du champ « extérieur ».")
            if (essais.any { Regex("""//(192\.168|10\.|172\.(1[6-9]|2\d|3[01]))""").containsMatchIn(it) })
                add("• L'adresse maison ne marche que sur votre Wi-Fi : connectez-vous au Wi-Fi, ou utilisez un VPN (Tailscale) hors de chez vous.")
        }
        throw ErreurApi(
            "Tessvine injoignable.\n\n" + (if (conseils.isEmpty()) "" else conseils.joinToString("\n") + "\n\n") +
                "Détail :\n" + erreurs.joinToString("\n")
        )
    }

    /** Adresse complète d'une ressource (images), avec la clé en paramètre. */
    fun url(chemin: String): String {
        val b = base ?: candidates().firstOrNull() ?: ""
        val sep = if (chemin.contains('?')) '&' else '?'
        return if (cle.isBlank()) "$b/api/$chemin" else "$b/api/$chemin${sep}cle=${java.net.URLEncoder.encode(cle, "UTF-8")}"
    }

    suspend fun brut(methode: String, chemin: String, corps: JsonElement? = null): String = withContext(Dispatchers.IO) {
        val b = resoudre()
        val req = Request.Builder()
            .url("$b/api/$chemin")
            .header("X-API-Key", prefs.cle)
            .method(methode, when {
                corps != null -> corps.toString().toRequestBody(typeJson)
                methode == "GET" || methode == "DELETE" -> null
                else -> "{}".toRequestBody(typeJson)
            })
            .build()
        try {
            executer(client.newCall(req)).use { r ->
                val texte = r.body?.string() ?: ""
                if (!r.isSuccessful) {
                    val message = runCatching {
                        json.parseToJsonElement(texte).jsonObject["erreur"]?.jsonPrimitive?.contentOrNull
                    }.getOrNull()
                    throw ErreurApi(message ?: "Erreur ${r.code}", r.code)
                }
                texte
            }
        } catch (e: IOException) {
            base = null  // l'adresse a peut-être changé (Wi-Fi ↔ 4G) : on la recherchera
            throw ErreurApi("Connexion perdue avec Tessvine (${e.message ?: "réseau"})")
        }
    }

    /** Exécute un appel HTTP qu'on peut annuler (délai dépassé, bouton « Annuler », écran quitté). */
    private suspend fun executer(appel: okhttp3.Call): okhttp3.Response =
        kotlinx.coroutines.suspendCancellableCoroutine { suite ->
            suite.invokeOnCancellation { appel.cancel() }
            appel.enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    if (suite.isActive) suite.resumeWith(Result.failure(e))
                }
                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    if (suite.isActive) suite.resumeWith(Result.success(response)) else response.close()
                }
            })
        }

    suspend inline fun <reified T> get(chemin: String): T = json.decodeFromString(brut("GET", chemin))
    suspend inline fun <reified T> post(chemin: String, corps: JsonElement? = null): T = json.decodeFromString(brut("POST", chemin, corps))
    suspend inline fun <reified T> put(chemin: String, corps: JsonElement): T = json.decodeFromString(brut("PUT", chemin, corps))
    suspend fun delete(chemin: String) { brut("DELETE", chemin) }

    // ------------------------------------------------------------------ raccourcis

    suspend fun referentiel(): Referentiel = get("referentiel")
    suspend fun stats(): Stats = get("stats")
    suspend fun vins(q: String = "", couleur: String = "", statut: String = "", stock: String = "1"): List<Vin> =
        get("vins?" + params("q" to q, "couleur" to couleur, "statut" to statut, "stock" to stock))
    suspend fun vin(id: Int): Vin = get("vins/$id")
    suspend fun caves(): List<CaveResume> = get("caves")
    suspend fun cave(id: Int): Cave = get("caves/$id")
    suspend fun nonRangees(): List<Bouteille> = get("bouteilles/non-rangees")
    suspend fun mouvements(limite: Int = 200): List<Mouvement> = get("mouvements?limite=$limite")
    suspend fun envies(): List<Envie> = get("envies")
    suspend fun suggestion(plat: String, couleur: String): List<Suggestion> =
        get("suggestion?" + params("plat" to plat, "couleur" to couleur, "limite" to "10"))
    suspend fun conseil(plat: String, couleur: String): ConseilPlat =
        post("suggestion/texte", obj("plat" to plat, "couleur" to couleur.ifBlank { null }))
    suspend fun gardes(): EtatGardes = get("ia/gardes")
    suspend fun capacites(): Capacites = get("capacites")
    suspend fun scan(code: String): ResultatScan = get("scan/${code.filter { it.isDigit() }}")

    companion object {
        fun params(vararg p: Pair<String, String>): String =
            p.filter { it.second.isNotBlank() }
                .joinToString("&") { "${it.first}=${java.net.URLEncoder.encode(it.second, "UTF-8")}" }
    }
}

/** Petit constructeur d'objet JSON : obj("a" to 1, "b" to "x", "c" to null). */
fun obj(vararg paires: Pair<String, Any?>): JsonObject = JsonObject(paires.associate { (k, v) -> k to versJson(v) })

fun versJson(v: Any?): JsonElement = when (v) {
    null -> kotlinx.serialization.json.JsonNull
    is JsonElement -> v
    is String -> kotlinx.serialization.json.JsonPrimitive(v)
    is Number -> kotlinx.serialization.json.JsonPrimitive(v)
    is Boolean -> kotlinx.serialization.json.JsonPrimitive(v)
    is Map<*, *> -> JsonObject(v.entries.associate { (k, x) -> k.toString() to versJson(x) })
    is Iterable<*> -> kotlinx.serialization.json.JsonArray(v.map { versJson(it) })
    else -> kotlinx.serialization.json.JsonPrimitive(v.toString())
}
