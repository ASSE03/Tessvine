package fr.cedric.tessvine.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import fr.cedric.tessvine.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/** Mises à jour de l'appli depuis les « Releases » du dépôt GitHub (privé : jeton en lecture seule). */
object Maj {
    const val DEPOT = "ASSE03/Tessvine"

    @Serializable
    data class Fichier(val name: String = "", val url: String = "", val size: Long = 0)

    @Serializable
    data class Version(
        val tag_name: String = "",
        val name: String = "",
        val body: String = "",
        val assets: List<Fichier> = emptyList(),
    ) {
        /** v1.0.12 → 12 (numéro de compilation, comparé à celui de l'appli installée). */
        val numero: Int get() = tag_name.substringAfterLast('.').toIntOrNull() ?: 0
        val apk: Fichier? get() = assets.firstOrNull { it.name.endsWith(".apk") }
    }

    val versionInstallee: Int get() = BuildConfig.VERSION_CODE
    val nomVersion: String get() = BuildConfig.VERSION_NAME

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private fun requete(url: String, jeton: String, accept: String) = Request.Builder()
        .url(url)
        .header("Authorization", "Bearer $jeton")
        .header("Accept", accept)
        .header("X-GitHub-Api-Version", "2022-11-28")
        .build()

    /** La dernière version publiée, ou null si l'appli est déjà à jour. */
    suspend fun verifier(jeton: String): Version? = withContext(Dispatchers.IO) {
        if (jeton.isBlank()) throw ErreurApi("Jeton GitHub non renseigné")
        client.newCall(requete("https://api.github.com/repos/$DEPOT/releases/latest", jeton, "application/vnd.github+json"))
            .execute().use { r ->
                when (r.code) {
                    401 -> throw ErreurApi("Jeton GitHub refusé (expiré ou mal copié)")
                    403, 404 -> throw ErreurApi("Le jeton n'a pas accès au dépôt $DEPOT (droit « Contents : Read-only »)")
                }
                if (!r.isSuccessful) throw ErreurApi("GitHub : erreur ${r.code}")
                val v = json.decodeFromString<Version>(r.body?.string() ?: "")
                if (v.numero > versionInstallee && v.apk != null) v else null
            }
    }

    /** Télécharge l'APK dans le cache ; [progres] reçoit une valeur de 0 à 1. */
    suspend fun telecharger(context: Context, jeton: String, v: Version, progres: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val apk = v.apk ?: throw ErreurApi("Pas d'APK dans cette version")
        val dossier = File(context.cacheDir, "maj").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
        val cible = File(dossier, "Tessvine-${v.numero}.apk")
        // l'API renvoie vers le stockage de GitHub : OkHttp suit la redirection (sans renvoyer le jeton)
        client.newCall(requete(apk.url, jeton, "application/octet-stream")).execute().use { r ->
            if (!r.isSuccessful) throw ErreurApi("Téléchargement impossible (erreur ${r.code})")
            val corps = r.body ?: throw ErreurApi("Téléchargement vide")
            val total = corps.contentLength().takeIf { it > 0 } ?: apk.size
            corps.byteStream().use { entree ->
                cible.outputStream().use { sortie ->
                    val tampon = ByteArray(64 * 1024)
                    var lu = 0L
                    while (true) {
                        val n = entree.read(tampon)
                        if (n < 0) break
                        sortie.write(tampon, 0, n)
                        lu += n
                        if (total > 0) progres((lu.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
        }
        cible
    }

    /** Android doit autoriser l'appli à installer des applications (une fois). */
    fun peutInstaller(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun ouvrirAutorisation(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** Ouvre l'installeur d'Android sur l'APK téléchargé. */
    fun installer(context: Context, fichier: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fichiers", fichier)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
