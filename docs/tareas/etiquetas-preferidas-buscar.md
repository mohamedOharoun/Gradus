# Etiquetas preferidas en Buscar (nota para el compañero)

Estado: **propuesta para ti** (2026-11-25). Nada de esto está hecho en Buscar: Buscar es tuyo y no lo he
tocado. Viene de `ajustes.md`, fase 3, decisión N.

## Qué hay ya

En Ajustes, el alumno elige hasta 10 **etiquetas preferidas** («Ajustes → Etiquetas preferidas»). Se
guardan en el móvil y la frase de Ajustes promete: «*Pronto* verás antes los cursos con estas etiquetas
en Inicio y en Buscar». La idea es que Buscar ponga primero los cursos que tengan alguna de ellas.

Todo lo que necesitas está en `sharedLogic`, con tests:

| Qué | Dónde | Para qué |
|---|---|---|
| `AppSettings.preferredTags: PreferredTags` | `settings/AppSettings.kt` | Las etiquetas elegidas (`values: List<String>`, ya normalizadas con tu `searchKey()`, sin «#»). |
| `SettingsStore.settings: StateFlow<AppSettings>` | `settings/SettingsStore.kt` | Las preferencias actuales y cada cambio. En la app: `AppContainer.settingsStore`. En tests: `InMemorySettingsStore`. |
| `List<CourseSummary>.prioritizing(PreferredTags)` | `settings/PreferredTags.kt` | Pone primero los cursos con alguna etiqueta preferida **sin cambiar el orden del resto** (es estable: dentro de cada grupo se respeta el orden por fecha). Sin preferidas, devuelve la lista tal cual. Compara con `searchKey()`. Tests: `PrioritizingTest`. |

## Lo que tienes que decidir: «Cargar más»

Tus resultados llegan por páginas y se van juntando en `CourseSearchState.results`. Hay tres formas de
ordenar, y cambian lo que ve el alumno:

1. **Ordenar cada página al llegar** (propuesta). Lo nuevo siempre se añade al final y nada de lo que
   ya se ve se mueve. Las preferidas van primero *dentro de cada página*.
2. **Ordenar toda la lista cada vez.** Es lo más simple (`search.results.prioritizing(tags)`), pero al
   pulsar «Cargar más» un curso preferido de la página 2 «salta» por encima de los que el alumno estaba
   mirando. También se mueve todo si cambia las etiquetas con Buscar abierto.
3. **Que lo haga el servidor** (un parámetro `preferred=` en `GET /api/courses`). Las preferidas irían
   primero en todo el catálogo, no solo en lo cargado; es un cambio de backend (tuyo).

## Cómo conectarlo (ejemplo para la opción 2, la más corta)

En `SearchViewModel`, recibir las etiquetas y exponer la lista ya ordenada (la pantalla no cambia de
lógica, solo pinta otra lista):

```kotlin
class SearchViewModel(
    private val gateway: CourseGateway,
    private val settings: SettingsStore,          // nuevo
) : ViewModel() {

    // Las etiquetas preferidas actuales (y cada cambio que se haga en Ajustes).
    private val preferred = settings.settings
        .map { it.preferredTags }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PreferredTags())

    // Lo que pinta la lista: los resultados con las preferidas primero.
    val shownResults: List<CourseSummary>
        get() = search.results.prioritizing(preferred.value)
}
```

y en `MainNavGraph.kt`: `SearchViewModel.factory(AppContainer.courseGateway, AppContainer.settingsStore)`.

Ojo: `preferred.value` leído así no avisa a Compose si cambia; si quieres que la lista se repinte al
cambiar las etiquetas con Buscar abierto, guárdalo en un `mutableStateOf` (actualizado con
`collect`) o expón la lista como `StateFlow` combinando las dos cosas. Para la opción 1 bastaría con
aplicar `page.items.prioritizing(tags)` dentro de `CourseSearchState.accept(...)` (pasándole las
etiquetas), sin tocar la pantalla.

Tests sugeridos (en `SearchViewModelTest`, con `FakeCourseGateway` e `InMemorySettingsStore`): un curso
con una etiqueta preferida sale primero; sin preferidas, el orden del servidor; cambiar las preferidas
en el store cambia el orden; con la opción 1, «Cargar más» no mueve lo que ya se veía.

## Cuando esté

Avísame y quito el «Pronto» de la frase de Ajustes (`screens/settings/SettingsContent.kt`, es mío):
«Verás antes los cursos con estas etiquetas en Buscar». Inicio todavía es provisional, así que de
momento solo Buscar.
