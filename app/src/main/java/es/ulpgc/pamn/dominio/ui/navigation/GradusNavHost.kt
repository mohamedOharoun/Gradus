package es.ulpgc.pamn.dominio.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import es.ulpgc.pamn.dominio.model.RegisterStep
import es.ulpgc.pamn.dominio.ui.screens.ScreenHome
import es.ulpgc.pamn.dominio.ui.screens.ScreenLogin
import es.ulpgc.pamn.dominio.ui.screens.ScreenRegister
import es.ulpgc.pamn.dominio.ui.screens.ScreenWelcome
import es.ulpgc.pamn.dominio.ui.screens.login.LoginViewModel
import es.ulpgc.pamn.dominio.ui.screens.register.RegisterActions
import es.ulpgc.pamn.dominio.ui.screens.register.RegisterViewModel

// Único sitio que sabe a dónde lleva cada botón: las pantallas solo avisan con lambdas.
// dropUnlessResumed ignora toques mientras la pantalla sale o entra (evita doble navegación).
@Composable
fun GradusNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = Route.Welcome, modifier = modifier) {
        composable<Route.Welcome> {
            ScreenWelcome(
                onCreateAccount = dropUnlessResumed { navController.navigate(Route.Register) },
                onLogin = dropUnlessResumed { navController.navigate(Route.Login) },
                onExploreCourses = { /* el catálogo aún no existe (fuera de alcance) */ },
            )
        }
        composable<Route.Register> {
            val registerViewModel: RegisterViewModel = viewModel()
            // Atrás del sistema: mientras no estemos en el primer paso, retrocede un paso.
            BackHandler(enabled = registerViewModel.form.step != RegisterStep.NAME) {
                registerViewModel.onBack()
            }
            ScreenRegister(
                form = registerViewModel.form,
                actions = RegisterActions(
                    onNameChange = registerViewModel::onNameChange,
                    onEmailChange = registerViewModel::onEmailChange,
                    onPasswordChange = registerViewModel::onPasswordChange,
                    onConfirmationChange = registerViewModel::onConfirmationChange,
                    onTermsChange = registerViewModel::onTermsChange,
                    onNext = registerViewModel::onNext,
                    onRegister = dropUnlessResumed {
                        if (registerViewModel.form.canRegister) navController.enterHome()
                    },
                    onEdit = registerViewModel::onEdit,
                    // Retrocede un paso; desde el primero, sale del registro.
                    onBack = dropUnlessResumed { if (!registerViewModel.onBack()) navController.popBackStack() },
                    // Cambia Registro por Login: atrás desde Login vuelve a Bienvenida.
                    onLogin = dropUnlessResumed { navController.navigate(Route.Login) { popUpTo<Route.Welcome>() } },
                ),
            )
        }
        composable<Route.Login> {
            val loginViewModel: LoginViewModel = viewModel()
            ScreenLogin(
                form = loginViewModel.form,
                onEmailChange = loginViewModel::onEmailChange,
                onPasswordChange = loginViewModel::onPasswordChange,
                onSubmit = dropUnlessResumed { if (loginViewModel.onSubmit()) navController.enterHome() },
                onBack = dropUnlessResumed { navController.popBackStack() },
                // Cambia Login por Registro: atrás desde el registro vuelve a Bienvenida.
                onCreateAccount = dropUnlessResumed { navController.navigate(Route.Register) { popUpTo<Route.Welcome>() } },
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
