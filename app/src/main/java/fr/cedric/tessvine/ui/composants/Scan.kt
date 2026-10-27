package fr.cedric.tessvine.ui.composants

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import fr.cedric.tessvine.data.Session
import fr.cedric.tessvine.data.Vin
import fr.cedric.tessvine.ui.Nav

/**
 * « Sortir par code-barres » : scanne la bouteille, retrouve le vin dans la cave et ouvre le dialogue de sortie.
 * Si le code est inconnu, propose d'ajouter le vin avec ce code.
 */
@Composable
fun rememberSortieParScan(nav: Nav): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbar.current
    var vin by remember { mutableStateOf<Vin?>(null) }

    vin?.let { v -> DialogueSortie(v, fermer = { vin = null }) }

    return {
        scope.action(snackbar) {
            val code = scannerCodeBarres(context) ?: return@action
            val r = Session.api.scan(code)
            val local = r.local
            if (local != null) {
                if (local.stock > 0) vin = local
                else snackbar.showSnackbar("${local.nom} : plus de bouteille en cave")
            } else {
                val choix = snackbar.showSnackbar(
                    "Aucun vin de la cave ne porte le code $code",
                    actionLabel = "Ajouter", duration = SnackbarDuration.Long,
                )
                if (choix == SnackbarResult.ActionPerformed) nav.ajout(code)
            }
        }
    }
}
