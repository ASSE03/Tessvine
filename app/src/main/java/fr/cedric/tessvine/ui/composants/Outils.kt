package fr.cedric.tessvine.ui.composants

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/** Lecture d'un code-barres avec le lecteur de Google Play Services (aucune autorisation caméra nécessaire). */
suspend fun scannerCodeBarres(context: Context): String? {
    val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_CODE_128, Barcode.FORMAT_QR_CODE,
        )
        .build()
    return try {
        GmsBarcodeScanning.getClient(context, options).startScan().await()?.rawValue
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        null  // scan annulé par l'utilisateur
    }
}

/** Fichier temporaire où l'appareil photo enregistre une prise de vue. */
fun nouveauFichierPhoto(context: Context): Uri {
    val dossier = File(context.cacheDir, "photos").apply { mkdirs() }
    val fichier = File.createTempFile("photo-", ".jpg", dossier)
    return FileProvider.getUriForFile(context, "${context.packageName}.fichiers", fichier)
}

/**
 * Photo prête à envoyer à Tessvine : redressée selon l'EXIF, réduite à 1600 px de côté,
 * en JPEG, sous forme de « data URL » base64.
 */
suspend fun photoEnDataUrl(context: Context, uri: Uri, cote: Int = 1600): String = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val bornes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bornes) }
    var echelle = 1
    while (maxOf(bornes.outWidth, bornes.outHeight) / (echelle * 2) >= cote) echelle *= 2
    val bitmap = resolver.openInputStream(uri)!!.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = echelle })
    } ?: error("Photo illisible")
    val rotation = resolver.openInputStream(uri)!!.use {
        when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }
    val ratio = minOf(1f, cote.toFloat() / maxOf(bitmap.width, bitmap.height))
    val matrice = Matrix().apply { postRotate(rotation); postScale(ratio, ratio) }
    val final = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrice, true)
    val sortie = ByteArrayOutputStream()
    final.compress(Bitmap.CompressFormat.JPEG, 88, sortie)
    "data:image/jpeg;base64," + Base64.encodeToString(sortie.toByteArray(), Base64.NO_WRAP)
}
