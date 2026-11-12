package es.ulpgc.pamn.dominio.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import es.ulpgc.pamn.dominio.ui.screens.ScreenHome
import es.ulpgc.pamn.dominio.ui.screens.ScreenLogin
import es.ulpgc.pamn.dominio.ui.screens.ScreenRegister
import es.ulpgc.pamn.dominio.ui.screens.ScreenWelcome
import es.ulpgc.pamn.dominio.ui.screens.login.LoginViewModel

// Único sitio que sabe a dónde lleva cada botón: las pantallas solo avisan con lambdas.
@Composable
fun GradusNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = Route.Welcome, modifier = modifier) {
        composable<Route.Welcome> {
            ScreenWelcome(
                onCreateAccount = { navController.navigate(Route.Register) },
                onLogin = { navController.navigate(Route.Login) },
                onExploreCourses = { /* el catálogo aún no existe (fuera de alcance) */ },
            )
        }
        composable<Route.Register> {
            ScreenRegister(
                onBack = { navController.popBackStack() },
                onRegistered = { navController.enterHome() },
            )
        }
        composable<Route.Login> {
            val loginViewModel: LoginViewModel = viewModel()
            ScreenLogin(
                form = loginViewModel.form,
                onEmailChange = loginViewModel::onEmailChange,
                onPasswordChange = loginViewModel::onPasswordChange,
                onSubmit = { if (loginViewModel.onSubmit()) navController.enterHome() },
                onBack = { navController.popBackStack() },
                // Cambia Login por Registro: atrás desde el registro vuelve a Bienvenida.
                onCreateAccount = { navController.navigate(Route.Register) { popUpTo<Route.Welcome>() } },
            )
        }
        composable<Route.Home> {
            ScreenHome()
        }
    }
}

// Va a Inicio y borra Bienvenida, Registro e Inicio de sesión del historial:
// así el botón atrás desde Inicio cierra la app en vez de volver al registro.
private fun NavController.enterHome() {
    navigate(Route.Home) {
        popUpTo<Route.Welcome> { inclusive = true }
    }
}
