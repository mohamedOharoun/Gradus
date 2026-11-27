package es.ulpgc.pamn.dominio.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.account.FakeAccountGateway
import es.ulpgc.pamn.dominio.account.InMemorySessionStore
import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.account.SessionStore
import es.ulpgc.pamn.dominio.data.progress.ProgressSyncRequester
import es.ulpgc.pamn.dominio.ui.testing.InSpanish
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// El arranque y salida de Main en la interfaz: qué se ve con cada StartState, cómo se sale de Main
// según el motivo (ToLogin vs ToWelcome) y que la pila se restaura aunque el NavHost se componga
// después de Loading. Con fakes y sin AppContainer.sessionStore: nunca toca la sesión real de la app.
// Las pantallas que se abren (Inicio y la guía provisional CourseGuide) no usan AppContainer.
@RunWith(AndroidJUnit4::class)
class GradusNavHostStartTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val tester = StateRestorationTester(rule)
    private var start by mutableStateOf<StartState>(StartState.Loading)
    private var resetToLoadingOnDispose = false
    private lateinit var navController: NavHostController

    // Como en la app: al guardar la sesión, el estado pasa a SignedIn (con signInOnSave = false se
    // prueba el caso en el que el Flow de StartViewModel llega un fotograma después).
    private inner class SessionStoreThatSignsIn(
        private val signInOnSave: Boolean,
        private val delegate: SessionStore = InMemorySessionStore(),
    ) : SessionStore by delegate {
        override suspend fun save(session: Session) {
            delegate.save(session)
            if (signInOnSave) start = StartState.SignedIn
        }
    }

    private fun launch(signInOnSave: Boolean = true) {
        val sessions = SessionStoreThatSignsIn(signInOnSave)
        tester.setContent {
            // Al emular la muerte del proceso, el estado se guarda con la pantalla actual y al morir
            // la Activity el StartViewModel nuevo empieza en Loading.
            DisposableEffect(Unit) {
                onDispose {
                    if (resetToLoadingOnDispose) {
                        resetToLoadingOnDispose = false
                        start = StartState.Loading
                    }
                }
            }
            InSpanish {
                GradusTheme {
                    navController = rememberNavController()
                    GradusNavHost(
                        start = start,
                        accountGateway = FakeAccountGateway(),
                        sessionStore = sessions,
                        progressSync = ProgressSyncRequester { },
                        navController = navController,
                    )
                }
            }
        }
    }

    private fun emitStart(state: StartState) {
        rule.runOnIdle { start = state }
        rule.waitForIdle()
    }

    private fun label(destination: NavDestination): String = when {
        destination.hasRoute<Route.Welcome>() -> "Welcome"
        destination.hasRoute<Route.Register>() -> "Register"
        destination.hasRoute<Route.Login>() -> "Login"
        destination.hasRoute<Route.Home>() -> "Home"
        destination.hasRoute<Route.Search>() -> "Search"
        destination.hasRoute<Route.MyCourses>() -> "MyCourses"
        destination.hasRoute<Route.CourseGuide>() -> "CourseGuide"
        destination.hasRoute<Route.Settings>() -> "Settings"
        else -> destination.route.orEmpty()
    }

    // La pila de pantallas, de la más antigua a la actual (sin los grafos: Main es un grafo).
    private fun stack(): List<String> = rule.runOnIdle {
        navController.currentBackStack.value.map { it.destination }.filter { it !is NavGraph }.map(::label)
    }

    // Pantalla provisional dentro de Main que no toca AppContainer (a diferencia de MyCoursesRoute o SettingsRoute).
    private fun goToCourseGuide() {
        rule.runOnIdle { navController.navigate(Route.CourseGuide) }
        rule.waitForIdle()
    }

    private fun pressSystemBack() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    // Guarda el estado con la pantalla actual montada (no en Loading), desmonta y restaura en Loading.
    private fun recreateWhileLoading() {
        resetToLoadingOnDispose = true
        tester.emulateSavedInstanceStateRestore()
        rule.waitForIdle()
    }

    // Rellena el formulario de Login (los dos campos editables: correo y contraseña) y pulsa «Entrar».
    private fun fillAndSubmitLogin() {
        val fields = rule.onAllNodes(hasSetTextAction())
        fields[0].performTextInput("ana@example.com")
        fields[1].performTextInput("secreta")
        rule.onNodeWithText("Entrar").performClick()
        rule.waitForIdle()
    }

    private val welcomeText = "Aprende paso a paso, sin saltarte la base."

    @Test
    fun while_loading_nothing_is_shown_and_there_is_no_destination() {
        launch()
        rule.onNodeWithText(welcomeText).assertDoesNotExist()
        assertEquals(emptyList<String>(), stack())
    }

    @Test
    fun without_session_it_starts_in_welcome() {
        launch()
        emitStart(StartState.SignedOut())
        rule.onNodeWithText(welcomeText).assertIsDisplayed()
        assertEquals(listOf("Welcome"), stack())
    }

    @Test
    fun with_session_it_starts_in_home() {
        launch()
        emitStart(StartState.SignedIn)
        rule.onNodeWithText("¡Hola!").assertIsDisplayed()
        assertEquals(listOf("Home"), stack())
    }

    @Test
    fun session_deleted_unexpectedly_inside_main_goes_to_login_and_the_stack_is_welcome_login() {
        launch()
        emitStart(StartState.SignedIn)
        goToCourseGuide()
        emitStart(StartState.SignedOut(MainExit.ToLogin))
        rule.onNodeWithText("Hola de nuevo").assertIsDisplayed()
        assertEquals(listOf("Welcome", "Login"), stack())
    }

    @Test
    fun voluntary_sign_out_inside_main_goes_directly_to_welcome_with_clean_stack() {
        launch()
        emitStart(StartState.SignedIn)
        goToCourseGuide()
        emitStart(StartState.SignedOut(MainExit.ToWelcome))
        rule.onNodeWithText(welcomeText).assertIsDisplayed()
        assertEquals(listOf("Welcome"), stack())
    }

    @Test
    fun after_voluntary_sign_out_the_user_can_log_in_again_and_enter_home() {
        launch()
        emitStart(StartState.SignedIn)
        goToCourseGuide()
        emitStart(StartState.SignedOut(MainExit.ToWelcome))
        rule.onNodeWithText("Iniciar sesión").performClick()
        fillAndSubmitLogin()
        rule.onNodeWithText("¡Hola!").assertIsDisplayed()
        assertEquals(listOf("Home"), stack())
    }

    @Test
    fun after_leaving_main_to_login_back_goes_to_welcome_and_never_to_main() {
        launch()
        emitStart(StartState.SignedIn)
        goToCourseGuide()
        emitStart(StartState.SignedOut(MainExit.ToLogin))
        pressSystemBack()
        rule.onNodeWithText(welcomeText).assertIsDisplayed()
        assertEquals(listOf("Welcome"), stack())
    }

    @Test
    fun after_leaving_main_the_user_can_log_in_again_and_back_does_not_return_to_login() {
        launch()
        emitStart(StartState.SignedIn)
        emitStart(StartState.SignedOut(MainExit.ToLogin))
        fillAndSubmitLogin()
        rule.onNodeWithText("¡Hola!").assertIsDisplayed()
        assertEquals(listOf("Home"), stack())
    }

    // Si LoginViewModel marca Succeeded un fotograma antes de que StartViewModel entregue SignedIn, no
    // entra en Main todavía (lo echaría de vuelta a Login): espera a SignedIn y entra limpio.
    @Test
    fun login_waits_for_signed_in_before_entering_main_so_it_is_not_kicked_back() {
        launch(signInOnSave = false)
        emitStart(StartState.SignedOut())
        rule.onNodeWithText("Iniciar sesión").performClick()
        fillAndSubmitLogin()
        assertEquals(listOf("Welcome", "Login"), stack())
        emitStart(StartState.SignedIn)
        rule.onNodeWithText("¡Hola!").assertIsDisplayed()
        assertEquals(listOf("Home"), stack())
    }

    // El caso de 11c: Android restaura la app dentro de Main y la sesión ya no está.
    @Test
    fun restored_inside_main_without_session_goes_to_login_with_welcome_below() {
        launch()
        emitStart(StartState.SignedIn)
        goToCourseGuide()
        recreateWhileLoading()
        assertEquals(StartState.Loading, start)
        assertEquals(emptyList<String>(), stack()) // mientras se lee, solo el fondo
        emitStart(StartState.SignedOut(MainExit.ToLogin))
        rule.onNodeWithText("Hola de nuevo").assertIsDisplayed()
        assertEquals(listOf("Welcome", "Login"), stack())
    }

    // La restauración funciona aunque el NavHost se componga después de Loading.
    @Test
    fun restored_inside_main_with_session_keeps_the_stack() {
        launch()
        emitStart(StartState.SignedIn)
        goToCourseGuide()
        recreateWhileLoading()
        assertEquals(StartState.Loading, start)
        emitStart(StartState.SignedIn)
        assertEquals(listOf("Home", "CourseGuide"), stack())
    }

    // Empezar sin sesión, iniciar sesión (entrar en Main y pasar a SignedIn), recrear y seguir en Main.
    @Test
    fun started_signed_out_logged_in_and_recreated_stays_in_main_with_the_stack_intact() {
        launch()
        emitStart(StartState.SignedOut())
        rule.onNodeWithText("Iniciar sesión").performClick()
        fillAndSubmitLogin()
        rule.onNodeWithText("¡Hola!").assertIsDisplayed()
        assertEquals(listOf("Home"), stack())
        goToCourseGuide()
        recreateWhileLoading()
        assertEquals(StartState.Loading, start)
        emitStart(StartState.SignedIn)
        assertEquals(listOf("Home", "CourseGuide"), stack())
    }
}
