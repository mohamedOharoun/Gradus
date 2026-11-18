package es.ulpgc.pamn.dominio.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import es.ulpgc.pamn.dominio.account.AccountGateway
import es.ulpgc.pamn.dominio.account.SessionStore
import es.ulpgc.pamn.dominio.model.RegisterStep
import es.ulpgc.pamn.dominio.model.Submission
import es.ulpgc.pamn.dominio.ui.screens.ScreenLogin
import es.ulpgc.pamn.dominio.ui.screens.ScreenRegister
import es.ulpgc.pamn.dominio.ui.screens.ScreenWelcome
import es.ulpgc.pamn.dominio.ui.screens.login.LoginActions
import es.ulpgc.pamn.dominio.ui.screens.login.LoginViewModel
import es.ulpgc.pamn.dominio.ui.screens.register.RegisterActions
import es.ulpgc.pamn.dominio.ui.screens.register.RegisterViewModel

// Único sitio que sabe a dónde lleva cada botón: las pantallas solo avisan con lambdas.
// dropUnlessResumed ignora toques mientras la pantalla sale o entra (evita doble navegación).
@Composable
fun GradusNavHost(
    accountGateway: AccountGateway,
    sessionStore: SessionStore,
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
            val registerViewModel: RegisterViewModel = viewModel(factory = RegisterViewModel.factory(accountGateway))
            // Atrás del sistema: mientras no estemos en el primer paso, retrocede un paso.
            BackHandler(enabled = registerViewModel.form.step != RegisterStep.NAME) {
                registerViewModel.onBack()
            }
            // Cuenta creada: a Inicio de sesión (el registro no da sesión). Se cambia Registro por
            // Login, así que atrás desde Login vuelve a Bienvenida y no al registro ya enviado.
            if (registerViewModel.form.submission == Submission.Succeeded) {
                LaunchedEffect(Unit) {
                    navController.navigate(Route.Login) { popUpTo<Route.Welcome>() }
                }
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
                    onRegister = dropUnlessResumed { registerViewModel.onRegister() },
                    onEdit = registerViewModel::onEdit,
                    // Retrocede un paso; desde el primero, sale del registro.
                    onBack = dropUnlessResumed { if (!registerViewModel.onBack()) navController.popBackStack() },
                    // Cambia Registro por Login: atrás desde Login vuelve a Bienvenida.
                    onLogin = dropUnlessResumed { navController.navigate(Route.Login) { popUpTo<Route.Welcome>() } },
                ),
            )
        }
        composable<Route.Login> {
            val loginViewModel: LoginViewModel = viewModel(factory = LoginViewModel.factory(accountGateway, sessionStore))
            // Sesión iniciada (y ya guardada): a Inicio, sin poder volver atrás al formulario.
            if (loginViewModel.form.submission == Submission.Succeeded) {
                LaunchedEffect(Unit) { navController.enterHome() }
            }
            ScreenLogin(
                form = loginViewModel.form,
                actions = LoginActions(
                    onEmailChange = loginViewModel::onEmailChange,
                    onPasswordChange = loginViewModel::onPasswordChange,
                    onSubmit = dropUnlessResumed { loginViewModel.onSubmit() },
                    onBack = dropUnlessResumed { navController.popBackStack() },
                    // Cambia Login por Registro: atrás desde el registro vuelve a Bienvenida.
                    onCreateAccount = dropUnlessResumed { navController.navigate(Route.Register) { popUpTo<Route.Welcome>() } },
                ),
            )
        }
        // Inicio, Buscar, Mis cursos y Ajustes, con la barra de navegación (MainNavGraph.kt).
        mainGraph(navController)
    }
}

// Va a Inicio (con la barra de navegación) y borra Bienvenida, Registro e Inicio de sesión del
// historial: así el botón atrás desde Inicio cierra la app en vez de volver al registro.
private fun NavController.enterHome() {
    navigate(Route.Main) {
        popUpTo<Route.Welcome> { inclusive = true }
    }
}
