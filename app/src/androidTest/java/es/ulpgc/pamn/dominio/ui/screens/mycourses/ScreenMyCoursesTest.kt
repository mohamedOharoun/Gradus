package es.ulpgc.pamn.dominio.ui.screens.mycourses

import android.content.ClipboardManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.ulpgc.pamn.dominio.course.Author
import es.ulpgc.pamn.dominio.course.CourseSummary
import es.ulpgc.pamn.dominio.course.publication.AuthorCourseFilter
import es.ulpgc.pamn.dominio.course.publication.AuthorCourseItem
import es.ulpgc.pamn.dominio.course.publication.CourseDraft
import es.ulpgc.pamn.dominio.course.publication.CourseIssue
import es.ulpgc.pamn.dominio.course.publication.CourseReport
import es.ulpgc.pamn.dominio.course.publication.DraftRejection
import es.ulpgc.pamn.dominio.course.publication.DraftStatus
import es.ulpgc.pamn.dominio.course.publication.PendingAction
import es.ulpgc.pamn.dominio.course.publication.PendingNotice
import es.ulpgc.pamn.dominio.course.publication.countByFilter
import es.ulpgc.pamn.dominio.ui.screens.ScreenMyCourses
import es.ulpgc.pamn.dominio.ui.testing.setContentInEnglish
import es.ulpgc.pamn.dominio.ui.testing.setContentInSpanish
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Tests instrumentados de la pantalla «Mis cursos», su selector, tarjetas, aviso inline, diálogo de
// descarte, hoja de informe y traducción al inglés.
@RunWith(AndroidJUnit4::class)
class ScreenMyCoursesTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val instant = Instant.parse("2026-11-27T18:00:00Z")
    private val author = Author(id = "user-1", name = "Ana Ruiz")
    private val liveRegionPolite =
        SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    private fun summary(
        id: String,
        title: String,
        moduleCount: Int = 10,
        version: Int = 1,
    ) = CourseSummary(
        id = id,
        version = version,
        author = author,
        publishedAt = "2026-11-20T10:00:00Z",
        title = title,
        description = "",
        language = "es",
        tags = emptyList(),
        moduleCount = moduleCount,
    )

    private fun draft(
        id: String,
        title: String,
        status: DraftStatus,
        moduleCount: Int? = 8,
        targetCourseId: String? = null,
    ) = CourseDraft(
        id = id,
        userId = author.id,
        targetCourseId = targetCourseId,
        filePath = "$id.json",
        title = title,
        moduleCount = moduleCount,
        status = status,
        updatedAt = instant,
    )

    private fun showInSpanish(
        state: MyCoursesUiState,
        actions: MyCoursesActions = MyCoursesActions(),
    ) = composeRule.setContentInSpanish {
        GradusTheme { ScreenMyCourses(state = state, actions = actions) }
    }

    private fun scrollToText(text: String) {
        composeRule.onNode(hasScrollToIndexAction())
            .performScrollToNode(hasText(text, substring = true))
    }

    // --- Cabecera y hero ---

    @Test
    fun header_and_hero_buttons_trigger_their_actions() {
        var uploadClicks = 0
        var guideClicks = 0
        showInSpanish(
            state = MyCoursesUiState(),
            actions = MyCoursesActions(
                onUploadCourse = { uploadClicks++ },
                onOpenGuide = { guideClicks++ },
            ),
        )

        composeRule.onNodeWithText("Mis cursos").assertIsDisplayed()
        composeRule.onNodeWithText("Los cursos que has creado").assertIsDisplayed()
        composeRule.onNodeWithText("Subir curso").assertHeightIsAtLeast(48.dp).performClick()
        composeRule.onNodeWithText("Ver la guía para crear cursos con IA")
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertEquals(1, uploadClicks)
        assertEquals(1, guideClicks)
    }

    // --- Cargando, vacío y filtro sin resultados ---

    @Test
    fun loading_initial_state_shows_progress_indicator_and_hides_empty_message() {
        showInSpanish(MyCoursesUiState(isLoading = true))

        composeRule.onNodeWithContentDescription("Cargando tus cursos").assertIsDisplayed()
        composeRule.onNodeWithText("TUS CURSOS").assertDoesNotExist()
        composeRule.onNodeWithText("Todavía no has creado ningún curso").assertDoesNotExist()
    }

    @Test
    fun empty_state_shows_empty_message_and_hides_filter_header() {
        showInSpanish(MyCoursesUiState())

        composeRule.onNodeWithText("Todavía no has creado ningún curso").assertIsDisplayed()
        composeRule.onNodeWithText("Sigue la guía de IA y sube tu primer JSON para empezar.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("TUS CURSOS").assertDoesNotExist()
    }

    @Test
    fun empty_filter_shows_filter_header_and_filter_empty_message() {
        val counts = mapOf(
            AuthorCourseFilter.ALL to 2,
            AuthorCourseFilter.PUBLISHED to 2,
            AuthorCourseFilter.ACCEPTED to 0,
            AuthorCourseFilter.PENDING to 0,
            AuthorCourseFilter.REJECTED to 0,
        )
        showInSpanish(
            MyCoursesUiState(
                items = emptyList(),
                allCount = 2,
                filter = AuthorCourseFilter.REJECTED,
                counts = counts,
            ),
        )

        composeRule.onNodeWithText("TUS CURSOS").assertIsDisplayed()
        composeRule.onNodeWithText("Rechazados · 0").assertIsDisplayed()
        composeRule.onNodeWithText("No hay cursos en este estado").assertIsDisplayed()
        composeRule.onNodeWithText("Prueba eligiendo otro filtro.").assertIsDisplayed()
    }

    // --- Lista con los 4 estados y acciones de tarjetas ---

    @Test
    fun published_card_shows_badge_hides_trash_and_triggers_upload_new_version() {
        val publishedItem = AuthorCourseItem(
            published = summary(id = "c-1", title = "Kotlin para Android", moduleCount = 18),
        )
        val uploaded = mutableListOf<AuthorCourseItem>()
        showInSpanish(
            state = MyCoursesUiState(
                items = listOf(publishedItem),
                allCount = 1,
                counts = listOf(publishedItem).countByFilter(),
            ),
            actions = MyCoursesActions(onUploadNewVersion = { uploaded += it }),
        )

        composeRule.onNodeWithText("Kotlin para Android").assertIsDisplayed()
        composeRule.onNodeWithText("Publicado").assertIsDisplayed()
        composeRule.onNodeWithText("18 módulos").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Descartar borrador de Kotlin para Android")
            .assertDoesNotExist()

        composeRule.onNodeWithText("Subir nueva versión").performClick()

        assertEquals(listOf(publishedItem), uploaded)
    }

    @Test
    fun accepted_card_with_warnings_triggers_publish_warnings_and_discard() {
        val warningsReport = CourseReport(
            warnings = listOf(
                CourseIssue("REMOVED_IDS", "/modules", "Se han eliminado módulos."),
                CourseIssue("SHORT_DESCRIPTION", "/description", "Descripción corta."),
            ),
        )
        val acceptedDraft = draft(
            id = "d-acc",
            title = "Sistemas operativos",
            status = DraftStatus.Accepted(warningsReport),
            moduleCount = 9,
            targetCourseId = "c-2",
        )
        val item = AuthorCourseItem(
            published = summary(id = "c-2", title = "Sistemas operativos", version = 2),
            draft = acceptedDraft,
        )
        val publishedIds = mutableListOf<String>()
        val openedReports = mutableListOf<AuthorCourseItem>()
        val discarded = mutableListOf<CourseDraft>()

        showInSpanish(
            state = MyCoursesUiState(
                items = listOf(item),
                allCount = 1,
                counts = listOf(item).countByFilter(),
            ),
            actions = MyCoursesActions(
                onPublish = { publishedIds += it },
                onOpenReport = { openedReports += it },
                onRequestDiscard = { discarded += it },
            ),
        )

        composeRule.onNodeWithText("Aceptado").assertIsDisplayed()
        composeRule.onNodeWithText("Publicada: versión 2").assertIsDisplayed()
        composeRule.onNodeWithText("Listo para publicar · 9 módulos").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Descartar borrador de Sistemas operativos")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.onNodeWithText("Ver avisos (2)").performClick()
        composeRule.onNodeWithText("Publicar").performClick()

        assertEquals(listOf(acceptedDraft), discarded)
        assertEquals(listOf(item), openedReports)
        assertEquals(listOf("d-acc"), publishedIds)
    }

    @Test
    fun pending_and_rejected_cards_show_badges_and_trigger_see_reasons() {
        val pendingItem = AuthorCourseItem(
            draft = draft(
                id = "d-pend",
                title = "Redes de computadores",
                status = DraftStatus.Pending(PendingAction.VALIDATING, PendingNotice.NETWORK),
                moduleCount = 12,
            ),
        )
        val rejectedItem = AuthorCourseItem(
            draft = draft(
                id = "d-rej",
                title = "Bases de datos",
                status = DraftStatus.Rejected(
                    DraftRejection.ValidationReport(
                        CourseReport(
                            errors = listOf(CourseIssue("MISSING_FIELD", "/title", "Falta título.")),
                        ),
                    ),
                ),
                moduleCount = 11,
            ),
        )
        val items = listOf(pendingItem, rejectedItem)
        val openedReports = mutableListOf<AuthorCourseItem>()

        showInSpanish(
            state = MyCoursesUiState(
                items = items,
                allCount = items.size,
                counts = items.countByFilter(),
            ),
            actions = MyCoursesActions(onOpenReport = { openedReports += it }),
        )

        scrollToText("Redes de computadores")
        composeRule.onNodeWithText("Pendiente").assertIsDisplayed()
        composeRule.onNodeWithText("Se validará cuando haya conexión.").assertIsDisplayed()

        scrollToText("Ver motivos")
        composeRule.onNodeWithText("Rechazado").assertIsDisplayed()
        composeRule.onNodeWithText("Ver motivos").performClick()

        assertEquals(listOf(rejectedItem), openedReports)
    }

    // --- Selector desplegable de filtro ---

    @Test
    fun filter_dropdown_lists_all_five_options_with_counts_and_selects_one() {
        val selected = mutableListOf<AuthorCourseFilter>()
        val counts = mapOf(
            AuthorCourseFilter.ALL to 3,
            AuthorCourseFilter.PUBLISHED to 1,
            AuthorCourseFilter.ACCEPTED to 1,
            AuthorCourseFilter.PENDING to 0,
            AuthorCourseFilter.REJECTED to 1,
        )
        val item = AuthorCourseItem(published = summary(id = "c-1", title = "Kotlin"))
        showInSpanish(
            state = MyCoursesUiState(
                items = listOf(item),
                allCount = 3,
                filter = AuthorCourseFilter.ALL,
                counts = counts,
            ),
            actions = MyCoursesActions(onSelectFilter = { selected += it }),
        )

        composeRule.onNodeWithContentDescription("Filtrar cursos: Todos, 3").performClick()

        composeRule.onNodeWithText("Publicados · 1").assertIsDisplayed()
        composeRule.onNodeWithText("Aceptados · 1").assertIsDisplayed()
        composeRule.onNodeWithText("Pendientes · 0").assertIsDisplayed()
        composeRule.onNodeWithText("Rechazados · 1").assertIsDisplayed()

        composeRule.onNodeWithText("Pendientes · 0").performClick()

        assertEquals(listOf(AuthorCourseFilter.PENDING), selected)
    }

    // --- Diálogo de descarte ---

    @Test
    fun discard_dialog_for_new_draft_shows_draft_copy_and_confirms() {
        val newDraft = draft(
            id = "d-1",
            title = "Bases de datos",
            status = DraftStatus.Accepted(),
            targetCourseId = null,
        )
        var confirmed = 0
        var dismissed = 0
        showInSpanish(
            state = MyCoursesUiState(pendingDiscard = newDraft),
            actions = MyCoursesActions(
                onConfirmDiscard = { confirmed++ },
                onDismissDiscard = { dismissed++ },
            ),
        )

        composeRule.onNodeWithText("¿Descartar borrador?").assertIsDisplayed()
        composeRule.onNodeWithText("Se eliminará el borrador «Bases de datos» de tu móvil.")
            .assertIsDisplayed()

        composeRule.onNodeWithText("Descartar").performClick()
        assertEquals(1, confirmed)
        assertEquals(0, dismissed)
    }

    @Test
    fun discard_dialog_for_new_version_shows_version_copy_and_cancels() {
        val versionDraft = draft(
            id = "d-2",
            title = "",
            status = DraftStatus.Accepted(),
            targetCourseId = "c-1",
        )
        var confirmed = 0
        var dismissed = 0
        showInSpanish(
            state = MyCoursesUiState(pendingDiscard = versionDraft),
            actions = MyCoursesActions(
                onConfirmDiscard = { confirmed++ },
                onDismissDiscard = { dismissed++ },
            ),
        )

        composeRule.onNodeWithText("¿Descartar versión nueva?").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Se eliminará el borrador de la nueva versión de «Curso sin título». " +
                "La versión publicada no cambia.",
        ).assertIsDisplayed()

        composeRule.onNodeWithText("Cancelar").performClick()
        assertEquals(0, confirmed)
        assertEquals(1, dismissed)
    }

    // --- Hoja de informe (CourseReportSheet) ---

    @Test
    fun report_sheet_displays_errors_warnings_root_path_and_copies_to_clipboard() {
        val openReport = OpenCourseReport(
            courseTitle = "Bases de datos",
            isRejection = true,
            report = CourseReport(
                errors = listOf(
                    CourseIssue("MISSING_FIELD", "/modules/0/title", "Falta el título."),
                    CourseIssue("CYCLE_DETECTED", "", "Hay un ciclo."),
                ),
                warnings = listOf(
                    CourseIssue("SHORT_DESCRIPTION", "/description", "Descripción muy breve."),
                ),
            ),
        )
        var closed = 0
        showInSpanish(
            state = MyCoursesUiState(openReport = openReport),
            actions = MyCoursesActions(onCloseReport = { closed++ }),
        )

        composeRule.onNodeWithText("Motivos del rechazo").assertIsDisplayed()
        composeRule.onNodeWithText("Bases de datos").assertIsDisplayed()
        composeRule.onNodeWithText("Errores (2)").assertIsDisplayed()
        composeRule.onNodeWithText("/modules/0/title").assertIsDisplayed()
        composeRule.onNodeWithText("[MISSING_FIELD]").assertIsDisplayed()
        composeRule.onNodeWithText("Falta el título.").assertIsDisplayed()
        composeRule.onNodeWithText("raíz").assertIsDisplayed()
        composeRule.onNodeWithText("[CYCLE_DETECTED]").assertIsDisplayed()
        composeRule.onNodeWithText("Avisos (1)").assertIsDisplayed()
        composeRule.onNodeWithText("[SHORT_DESCRIPTION]").assertIsDisplayed()

        composeRule.onNodeWithText("Copiar informe").assertHeightIsAtLeast(48.dp).performClick()

        composeRule.onNodeWithText("Informe copiado")
            .assertIsDisplayed()
            .assert(liveRegionPolite)

        val clipboard = InstrumentationRegistry.getInstrumentation().targetContext
            .getSystemService(ClipboardManager::class.java)
        val copiedText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
        assertTrue(copiedText.contains("Errores (2):"))
        assertTrue(copiedText.contains("[CYCLE_DETECTED] raíz: Hay un ciclo."))
        assertTrue(copiedText.contains("Avisos (1):"))

        composeRule.onNodeWithContentDescription("Cerrar informe")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.waitForIdle()
        assertEquals(1, closed)
    }

    @Test
    fun report_sheet_empty_report_shows_empty_message_and_warnings_title() {
        composeRule.setContentInSpanish {
            GradusTheme {
                CourseReportSheetContent(
                    openReport = OpenCourseReport(
                        courseTitle = "Curso vacío",
                        report = CourseReport.EMPTY,
                        isRejection = false,
                    ),
                    onClose = {},
                )
            }
        }

        composeRule.onNodeWithText("Avisos antes de publicar").assertIsDisplayed()
        composeRule.onNodeWithText("Sin errores ni avisos.").assertIsDisplayed()
    }

    // --- Aviso inline ---

    @Test
    fun inline_notice_unreachable_shows_retry_and_dismiss_with_live_region() {
        var retries = 0
        var dismissed = 0
        showInSpanish(
            state = MyCoursesUiState(inlineNotice = MyCoursesInlineNotice.UNREACHABLE),
            actions = MyCoursesActions(
                onRetry = { retries++ },
                onDismissNotice = { dismissed++ },
            ),
        )

        composeRule.onNode(
            liveRegionPolite and hasAnyDescendant(
                hasText("Sin conexión. Mostrando tus borradores locales."),
            ),
        ).assertExists()

        composeRule.onNodeWithText("Reintentar").assertHeightIsAtLeast(48.dp).performClick()
        composeRule.onNodeWithContentDescription("Cerrar aviso")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertEquals(1, retries)
        assertEquals(1, dismissed)
    }

    @Test
    fun inline_notice_server_failure_shows_retry_button() {
        showInSpanish(MyCoursesUiState(inlineNotice = MyCoursesInlineNotice.SERVER_FAILURE))

        composeRule.onNodeWithText("No se han podido cargar tus cursos publicados. Puedes reintentar.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Reintentar").assertIsDisplayed()
    }

    @Test
    fun inline_notice_file_too_large_hides_retry_button() {
        showInSpanish(MyCoursesUiState(inlineNotice = MyCoursesInlineNotice.FILE_TOO_LARGE))

        composeRule.onNodeWithText("El fichero supera el límite de 2 MiB y no se ha podido subir.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Reintentar").assertDoesNotExist()
    }

    // --- Verificación en inglés ---

    @Test
    fun my_courses_screen_and_selector_in_english() {
        val item = AuthorCourseItem(
            published = summary(id = "c-1", title = "Kotlin", version = 2),
            draft = draft(
                id = "d-1",
                title = "Kotlin",
                status = DraftStatus.Accepted(
                    CourseReport(
                        warnings = listOf(CourseIssue("REMOVED_IDS", "/modules", "Removed")),
                    ),
                ),
                moduleCount = 5,
                targetCourseId = "c-1",
            ),
        )
        composeRule.setContentInEnglish {
            GradusTheme {
                ScreenMyCourses(
                    state = MyCoursesUiState(
                        items = listOf(item),
                        allCount = 1,
                        counts = listOf(item).countByFilter(),
                        inlineNotice = MyCoursesInlineNotice.UNREACHABLE,
                    ),
                    actions = MyCoursesActions(),
                )
            }
        }

        composeRule.onNodeWithText("My courses").assertIsDisplayed()
        composeRule.onNodeWithText("Courses you've created").assertIsDisplayed()
        composeRule.onNodeWithText("Upload course").assertIsDisplayed()
        composeRule.onNodeWithText("See the guide to create courses with AI").assertIsDisplayed()
        composeRule.onNodeWithText("Offline. Showing your local drafts.").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed()
        composeRule.onNodeWithText("YOUR COURSES").assertIsDisplayed()
        composeRule.onNodeWithText("All · 1").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Filter courses: All, 1").performClick()
        composeRule.onNodeWithText("Published · 0").assertIsDisplayed()
        composeRule.onNodeWithText("Accepted · 1").assertIsDisplayed()
        composeRule.onNodeWithText("Pending · 0").assertIsDisplayed()
        composeRule.onNodeWithText("Rejected · 0").assertIsDisplayed()
        composeRule.onNodeWithText("Accepted · 1").performClick()

        scrollToText("Publish")
        composeRule.onNodeWithText("Accepted").assertIsDisplayed()
        composeRule.onNodeWithText("Published: version 2").assertIsDisplayed()
        composeRule.onNodeWithText("Upload new version").assertIsDisplayed()
        composeRule.onNodeWithText("See warnings (1)").assertIsDisplayed()
        composeRule.onNodeWithText("Publish").assertIsDisplayed()
    }

    @Test
    fun report_sheet_and_discard_dialog_in_english() {
        val openReport = OpenCourseReport(
            courseTitle = "Databases",
            isRejection = true,
            report = CourseReport(
                errors = listOf(CourseIssue("CYCLE", "", "Cycle detected")),
                warnings = listOf(CourseIssue("WARN", "/description", "Short")),
            ),
        )
        composeRule.setContentInEnglish {
            GradusTheme {
                CourseReportSheetContent(openReport = openReport, onClose = {})
            }
        }

        composeRule.onNodeWithText("Rejection reasons").assertIsDisplayed()
        composeRule.onNodeWithText("Errors (1)").assertIsDisplayed()
        composeRule.onNodeWithText("root").assertIsDisplayed()
        composeRule.onNodeWithText("Warnings (1)").assertIsDisplayed()
        composeRule.onNodeWithText("Copy report").performClick()
        composeRule.onNodeWithText("Report copied").assertIsDisplayed()
    }

    @Test
    fun discard_dialog_in_english() {
        val newDraft = draft(
            id = "d-en",
            title = "Databases",
            status = DraftStatus.Accepted(),
            targetCourseId = null,
        )
        composeRule.setContentInEnglish {
            GradusTheme {
                DiscardDraftDialog(draft = newDraft, onConfirm = {}, onDismiss = {})
            }
        }

        composeRule.onNodeWithText("Discard draft?").assertIsDisplayed()
        composeRule.onNodeWithText("The draft “Databases” will be removed from your phone.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Discard").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
    }
}
