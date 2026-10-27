package fr.cedric.tessvine.ui.composants

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import fr.cedric.tessvine.data.Maj
import fr.cedric.tessvine.data.Session
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Fenêtre « Nouvelle version disponible » : télécharge l'APK puis ouvre l'installeur d'Android. */
@Composable
fun DialogueMiseAJour(version: Maj.Version, fermer: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var progres by remember { mutableFloatStateOf(-1f) }   // -1 : pas encore commencé
    var erreur by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (progres < 0f) fermer() },
        title = { Text("Nouvelle version : ${version.tag_name.removePrefix("v")}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Version installée : ${Maj.nomVersion}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (version.body.isNotBlank()) Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                    Text(version.body)
                }
                if (progres >= 0f) {
                    Text("Téléchargement… ${(progres * 100).toInt()} %")
                    LinearProgressIndicator(progress = { progres }, modifier = Modifier.fillMaxWidth())
                }
                erreur?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (!Maj.peutInstaller(context)) Text(
                    "Android va d'abord vous demander d'autoriser Tessvine à installer des applications : activez l'option puis revenez ici.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(enabled = progres < 0f, onClick = {
                if (!Maj.peutInstaller(context)) { Maj.ouvrirAutorisation(context); return@Button }
                scope.launch {
                    erreur = null
                    progres = 0f
                    try {
                        val apk = Maj.telecharger(context, Session.prefs.jetonGithub, version) { progres = it }
                        Maj.installer(context, apk)
                        fermer()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        erreur = e.message ?: "Erreur"
                    } finally {
                        progres = -1f
                    }
                }
            }) { Text("Mettre à jour") }
        },
        dismissButton = { TextButton(onClick = fermer, enabled = progres < 0f) { Text("Plus tard") } },
    )
}
