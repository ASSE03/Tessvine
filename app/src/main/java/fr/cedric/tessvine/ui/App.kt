package fr.cedric.tessvine.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fr.cedric.tessvine.data.Session
import fr.cedric.tessvine.ui.composants.LocalOuvrirConnexion
import fr.cedric.tessvine.ui.composants.LocalPortee
import fr.cedric.tessvine.ui.composants.LocalSnackbar
import fr.cedric.tessvine.ui.ecrans.EcranAccueil
import fr.cedric.tessvine.ui.ecrans.EcranAjout
import fr.cedric.tessvine.ui.ecrans.EcranConnexion
import fr.cedric.tessvine.ui.ecrans.EcranEdition
import fr.cedric.tessvine.ui.ecrans.EcranEnvies
import fr.cedric.tessvine.ui.ecrans.EcranFiche
import fr.cedric.tessvine.ui.ecrans.EcranHistorique
import fr.cedric.tessvine.ui.ecrans.EcranMetsVins
import fr.cedric.tessvine.ui.ecrans.EcranPlan
import fr.cedric.tessvine.ui.ecrans.EcranPlus
import fr.cedric.tessvine.ui.ecrans.EcranReglages
import fr.cedric.tessvine.ui.ecrans.EcranStats
import fr.cedric.tessvine.ui.ecrans.EcranVins

private data class Onglet(val route: String, val libelle: String, val icone: ImageVector)

private val ONGLETS = listOf(
    Onglet("accueil", "Accueil", Icons.Filled.Home),
    Onglet("vins", "Mes vins", Icons.Filled.Inventory2),
    Onglet("plan", "Plan", Icons.Filled.GridView),
    Onglet("mets", "Mets & Vins", Icons.Filled.Restaurant),
    Onglet("plus", "Plus", Icons.Filled.MoreHoriz),
)

/** Navigation de l'application (les écrans reçoivent ce contrôleur pour ouvrir les autres). */
class Nav(private val c: NavHostController) {
    fun retour() { c.popBackStack() }
    fun vin(id: Int) = c.navigate("vin/$id")
    fun edition(id: Int) = c.navigate("edition/$id")
    fun ajout(code: String = "") = c.navigate("ajout?code=$code")
    fun plan(cave: Int = 0, vin: Int = 0, placer: Int = 0) = c.navigate("plan?cave=$cave&vin=$vin&placer=$placer") {
        launchSingleTop = true
    }
    fun ecran(route: String) = c.navigate(route)
    fun accueil() = c.navigate("accueil") { popUpTo(0) { inclusive = true } }
}

@Composable
fun App() {
    val controleur = rememberNavController()
    val nav = remember(controleur) { Nav(controleur) }
    val snackbar = remember { SnackbarHostState() }
    val portee = rememberCoroutineScope()
    val entree by controleur.currentBackStackEntryAsState()
    val routeActuelle = entree?.destination?.route?.substringBefore('?')

    LaunchedEffect(Unit) { Session.chargerReferentiel() }

    CompositionLocalProvider(
        LocalSnackbar provides snackbar, LocalPortee provides portee,
        LocalOuvrirConnexion provides { nav.ecran("serveur") },
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (routeActuelle in ONGLETS.map { it.route }) {
                    NavigationBar {
                        ONGLETS.forEach { o ->
                            NavigationBarItem(
                                selected = routeActuelle == o.route,
                                onClick = {
                                    controleur.navigate(o.route) {
                                        popUpTo(controleur.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(o.icone, null) },
                                label = { Text(o.libelle, maxLines = 1) },
                            )
                        }
                    }
                }
            },
        ) { marges ->
            NavHost(
                controleur,
                startDestination = if (Session.prefs.premierLancement) "connexion" else "accueil",
                modifier = Modifier.padding(marges),
            ) {
                composable("connexion") { EcranConnexion(nav, premierLancement = true) }
                composable("accueil") { EcranAccueil(nav) }
                composable("vins") { EcranVins(nav) }
                composable(
                    "plan?cave={cave}&vin={vin}&placer={placer}",
                    arguments = listOf(
                        navArgument("cave") { type = NavType.IntType; defaultValue = 0 },
                        navArgument("vin") { type = NavType.IntType; defaultValue = 0 },
                        navArgument("placer") { type = NavType.IntType; defaultValue = 0 },
                    ),
                ) { e ->
                    val a = e.arguments
                    EcranPlan(nav, a?.getInt("cave") ?: 0, a?.getInt("vin") ?: 0, a?.getInt("placer") ?: 0)
                }
                composable("mets") { EcranMetsVins(nav) }
                composable("plus") { EcranPlus(nav) }
                composable("vin/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { e ->
                    EcranFiche(nav, e.arguments?.getInt("id") ?: 0)
                }
                composable("edition/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { e ->
                    EcranEdition(nav, e.arguments?.getInt("id") ?: 0)
                }
                composable(
                    "ajout?code={code}",
                    arguments = listOf(navArgument("code") { type = NavType.StringType; defaultValue = "" }),
                ) { e -> EcranAjout(nav, e.arguments?.getString("code") ?: "") }
                composable("envies") { EcranEnvies(nav) }
                composable("stats") { EcranStats(nav) }
                composable("historique") { EcranHistorique(nav) }
                composable("reglages") { EcranReglages(nav) }
                composable("serveur") { EcranConnexion(nav, premierLancement = false) }
            }
        }
    }
}
