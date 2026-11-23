# Buscar cursos (con filtros)

Pestaña **Buscar** del Figma (pantallas «3 · Buscar» y «4 · Buscar con filtros»): lista de cursos
del catálogo del servidor, búsqueda por texto y filtros en una hoja inferior. Al tocar un curso se
abre su camino (`Route.Course(courseId)`, ya hecho en `docs/tareas/cursos-backend-app.md`, tarea 7).

Contrato de la API: `backend/docs/api-cursos.md`
(`GET /api/courses?q=&tag=&language=&author=&published=&page=&size=`).
Especificación: `docs/especificacion.md`, «Con sesión → Buscar».
Diseño: captura del Figma en `docs/tareas/resources/search-courses.png` (las dos pantallas; no se sube a
los commits). Es una foto del 2026-11-21: si no coincide con el Figma, manda el Figma.

**Histórico:** [`buscar-cursos-historico.md`](buscar-cursos-historico.md) tiene el detalle de la fase 1
(tareas 1–9, con las decisiones con letra de cada tarea que citan los comentarios del código) y el de la
fase 2 hasta el paso 11c. Solo hace falta abrirlo para entender una decisión antigua.

Estado: fase 1 terminada (2026-11-22). Fase 2: tarea 10 (backend) hecha; tarea 11 (app) en curso.

## Relevo

Lo que necesita quien coja la tarea (orquestador o revisor). **Se reescribe** al terminar cada sesión;
no se acumula.

- **Dónde estamos (2026-11-23):** tarea 11, pasos 11a, 11b, 11c y 11d-1 hechos, revisados y con commit
  (último: `64c5dbf`, 11d-1). Toca el **11d-2** (ViewModel y conexión), con **Opus**, y después el
  **11e** (cierre).
- **Al empezar:** leer `AGENTS.md`, este Relevo y la tarea 11 (más abajo). El resto del documento y el
  histórico, solo si hacen falta.
- **Abierto:**
  - El 11d-2 debe añadir el test que el 11a-2 no pudo hacer: con creador y fecha **aplicados**, quitar
    su chip los quita de la consulta (ya está en la lista del 11d-2).
  - 11e: decidir **antes** con qué cuenta se prueba en el emulador. Para llegar a Buscar hay que iniciar
    sesión, y el chat del 11d-1 creó y borró una cuenta en la base de desarrollo sin permiso.
  - Fuera de la tarea: «Explorar cursos sin cuenta» (bienvenida) no hace nada. Solo apuntado.
  - «Pendiente» (abajo): los tests de `sharedLogic` no compilan para iOS. Tarea aparte, sin fecha.
- **Qué mirar al revisar el 11d-2:** que `onAuthorChange` y `onPublishedChange` pasen por `editDraft`
  (pausa de 300 ms, cancelar el recuento anterior, `cancelPreview`); que teclear varias letras cuente
  una sola vez; los tests de la lista, incluido el pendiente del 11a-2; que `MainNavGraph` no cambie.
  Riesgo de la decisión H ya comprobado en el 11d-1 (el teclado no tapa el campo).
- **Mensajes a los chats que implementan** (plantilla; se adapta en cada caso):
  1. Rol y paso: «Eres quien implementa el paso X de la búsqueda de cursos de Gradus».
  2. Qué leer: `AGENTS.md`, este Relevo y el paso X; **la lista de ficheros que se tocan** y, si hace
     falta, uno de ejemplo. Nada más salvo que lo necesite.
  3. **Primero, proponer:** ficheros, forma del código y lista de tests; **esperar el OK** antes de
     escribir código.
  4. Tras el OK: tests primero (rojo → verde); los tests nuevos, en un **fichero nuevo y temático** si
     el que hay es grande; en el emulador, **solo las clases de test del paso**.
  5. Al terminar: los comandos del paso; «Progreso» con lo necesario y «pendiente de revisión»; sin
     marcar la casilla; fragmentos clave y decisiones fuera del plan; sin commits.
- **Acuerdos de proceso:**
  - Que un paso esté detallado en este documento **no** significa que esté aprobado para implementar:
    quien implementa propone y espera el OK (`AGENTS.md`).
  - La batería completa del emulador se lanza al cierre de la tarea (11e), no en cada paso.
  - Sonnet con esfuerzo medio para lógica pura y componentes; esfuerzo alto para corrutinas,
    concurrencia o cambios de contrato.
  - Sin permiso, nada se escribe en la base de desarrollo (cuentas, cursos).
  - La casilla (✅) de cada paso la pone quien revisa; quien implementa deja «pendiente de revisión».
  - Nombres de test sin comas ni paréntesis (no compilan para iOS).
  - Tests de emulador con TalkBack apagado, sin el *Layout Inspector* y sin tocar el emulador.
  - Los commits los hacen los usuarios, salvo que pidan lo contrario.
  - Un orquestador nuevo por tarea (o cuando el chat se alargue), que reescribe este Relevo al terminar.
  - Cuánta revisión hace falta, cómo son los mensajes a los chats y si se juntan pasos triviales se
    decide en cada momento, sin optimizar de forma agresiva. Modelo y esfuerzo: el mínimo que mantiene
    la calidad.
  - En «Progreso», lo que alguien necesita para continuar o entender una decisión; lo que ya cuentan el
    diff, los tests o el commit no se repite.

## Objetivo

1. Ver la lista de cursos publicados nada más entrar en Buscar (los más recientes primero).
2. Buscar por texto mientras se escribe, con el número de resultados.
3. Filtrar por etiquetas, creador y fecha de publicación en una hoja inferior; los filtros activos se
   ven como chips que se quitan.
4. Abrir un curso desde su tarjeta.
5. Dejar la lógica en `sharedLogic` para que una app de iOS solo tenga que rehacer las vistas.

## Figma frente a la API

| En el Figma | En la API | Estado |
|---|---|---|
| Caja de búsqueda | `q` (título o descripción) | Fase 1 |
| «8 resultados» | `total` de la página | Fase 1 |
| Tarjeta: título, «por Ana Ruiz», etiquetas | metadatos (`title`, `author.name`, `tags`) | Fase 1 |
| «20 conceptos» | `moduleCount` | Fase 1, como **«20 módulos»** |
| «oct 2026» | `publishedAt` | Fase 1 |
| Etiquetas (varias) | `tag` repetible; el curso debe tenerlas **todas** | Fase 1 |
| «Ver todas» (lista de etiquetas) | no hay ruta de etiquetas | Decisión 3 |
| Creador | `author` (subcadena del nombre) | Fase 2: backend hecho (tarea 10), app en la 11 |
| Fecha de publicación (cualquiera, último mes, último año) | `published=month\|year` | Fase 2: backend hecho (tarea 10), app en la 11 |
| Ordenar: más recientes | orden fijo: más recientes | Sin control mientras sea la única opción |
| Ordenar: mejor valorados · ★ 4,7 (32) | **no hay valoraciones** | Fuera de alcance |
| «Relevancia» (hoja y encima de la lista) | — | **Errata del Figma** (decisión 10) |
| «Inscrito» | **no hay inscripción** todavía | Fuera de alcance |
| «Mostrar 8 cursos» | `total` de una búsqueda con el borrador | Decisión 6 |
| — | `language` | Fuera de alcance |

## Idea de diseño (código)

**Lógica pura en `sharedLogic`** (se prueba con `kotlin.test` y sirve tal cual en iOS) y, en `app`, un
ViewModel fino (corrutinas) y pantallas sin estado. La interfaz va en **`:app`**, no en `:androidApp`.

```
sharedLogic/commonMain/.../course/
├── CourseGateway                 search(query: CourseQuery, page)  (+ extensión search(text, page))
├── http/HttpCourseGateway        CourseQuery → ?q=…&tag=…&author=…&published=…&page=…&size=…
├── FakeCourseGateway             filtra por texto, etiquetas y autor; ignora la fecha (sin reloj)
├── PublishedMonth                "2026-10-03T08:35:00Z" → año 2026, mes 10 (sin java.time)
└── search/
    ├── CourseQuery               texto, etiquetas, creador y fecha. Inmutable.
    ├── PublishedWithin           ANY, LAST_MONTH, LAST_YEAR
    ├── ActiveFilter              los chips quitables (Tag, Author, Published) y el número de «Filtros n»
    ├── CourseSearchState         máquina de estados: resultados, páginas, cargando, problema
    ├── TagSuggestions            qué etiquetas ofrecer en la hoja y en qué orden
    ├── FilterSheetState          la hoja: borrador, sugerencias, «Ver todas», qué aplicar y el recuento
    └── DraftCount                lo que se sabe del recuento del borrador («Mostrar N cursos»)
app/.../ui/
├── screens/ScreenSearch.kt
├── screens/search/SearchViewModel.kt       espera al teclear, cancela la búsqueda anterior, recuento
├── screens/search/SearchActions.kt         eventos de la pantalla (como LoginActions)
├── screens/search/SearchResultsContent.kt  lista, cargando, vacío, problema, «Cargar más»
├── screens/search/SearchTexts.kt           «8 resultados», vacío y problemas (funciones puras)
├── screens/search/FilterBar.kt             fila bajo la caja: «Filtros n» y chips quitables
├── screens/search/FilterSheet.kt           hoja inferior (ModalBottomSheet)
├── screens/search/FilterSheetContent.kt    cabecera, secciones y «Mostrar N cursos»
├── screens/search/FilterSheetSections.kt   (11d-1) Etiquetas, Creador y Fecha de publicación
├── screens/search/CreatorField.kt          campo «Creador»
├── screens/search/FilterTexts.kt           textos de los filtros (funciones puras)
└── components/
    ├── SearchField.kt            caja redondeada con lupa y ✕ (ClearButton, compartido)
    ├── CourseCard.kt / CourseTexts.kt / TagLabel.kt / ProblemMessage.kt
    ├── FilterChips.kt            «Filtros n», chip quitable y chip seleccionable
    └── SingleChoiceRow.kt        opciones en fila, una elegida (fecha; más adelante «Ordenar por»)
app/.../data/DemoSearchResults.kt           los 3 cursos del Figma, para las previews
```

**Patrones:** objeto inmutable + funciones puras (`CourseQuery`, `FilterSheetState`); máquina de
estados / reductor (`CourseSearchState`); puerto/adaptador y *Decorator* (`CachingCourseGateway`);
*Mapper* (`CourseQuery` → parámetros de la URL); *factory method* (`SearchViewModel.factory`,
`CourseQuery.EMPTY`, `FilterSheetState.open`); interfaz sellada (`ActiveFilter`: cada `when` debe
cubrir todos los casos).

**En iOS haría falta:** las vistas (SwiftUI) y un equivalente del ViewModel (la espera al teclear y
lanzar las peticiones). Modelo, reglas, estados y red se reutilizan de `sharedLogic`.

## Alcance

- Lista inicial (sin texto ni filtros): todos los cursos, los más recientes primero.
- Búsqueda al escribir, «N resultados», tarjetas, «Cargar más».
- Estados: cargando (sin borrar los resultados anteriores), sin resultados, sin conexión y fallo del
  servidor, con «Reintentar».
- Fila de filtros: «Filtros» con su número y un chip quitable por filtro.
- Hoja de filtros: **Etiquetas** (varias, «Ver todas»), **Creador**, **Fecha de publicación**,
  «Restablecer», ✕ y «Mostrar N cursos».
- Tocar una tarjeta abre el curso; «atrás» vuelve a Buscar tal como estaba.
- Previews en claro y oscuro, TalkBack y zonas táctiles de 48 dp.

## Fuera de alcance

- **Valoraciones** (★ y «Mejor valorados») y, por tanto, **«Ordenar por»**.
- **«Inscrito»**: llegará con Mis cursos / Room.
- Filtro por idioma, historial de búsquedas y búsqueda sin conexión.

## Decisiones (aprobadas el 2026-11-21)

1. **Filtros de creador y fecha: fase 2.** Los datos ya estaban en el backend; faltaba exponerlos como
   parámetros de `GET /api/courses` (tarea 10, hecha). Filtrarlos en el móvil no sirve: rompería la
   paginación y el número de resultados.
2. **Tarjeta sin valoración ni «Inscrito»:** la línea de abajo queda «20 módulos · oct 2026».
3. **Las etiquetas de la hoja salen de los resultados ya cargados** de la búsqueda aplicada: primero las
   elegidas, luego las más repetidas y, a igualdad, por orden alfabético. Se ven 7 y «Ver todas»
   despliega el resto. Si se queda corto, se añadirá `GET /api/courses/tags`.
4. **Se busca al escribir, tras 300 ms sin teclear**; cada búsqueda nueva cancela la anterior (gana la
   última). La tecla «Buscar» del teclado busca al momento y lo cierra.
5. **Paginación**: botón **«Cargar más»** al final de la lista.
6. **«Mostrar N cursos»**: al cambiar el borrador se busca con él (300 ms de espera) y se enseña su
   `total`; al pulsar, se usan esos resultados sin volver a pedirlos. Mientras tanto: «Mostrar cursos».
7. **La hoja trabaja con un borrador.** ✕, deslizar hacia abajo o «atrás» lo descartan; «Restablecer»
   vacía los filtros del borrador (no el texto); «Mostrar N cursos» lo aplica. Los chips de fuera de la
   hoja se quitan al momento.
8. **Este documento va en `docs/tareas/`**, como dice `AGENTS.md`.
9. **Botón provisional de Inicio**: quitado en la tarea 9.
10. **«Relevancia» es una errata del Figma.** Sin valoraciones, el único orden es «Más recientes», que ya
    es el del backend: **no se muestra «Ordenar por»** y no hace falta `sort` en la API.

## Tareas

### Fase 1 (hechas; detalle en el histórico)

1. ✅ `CourseQuery` y `ActiveFilter`.
2. ✅ El puerto recibe un `CourseQuery` (`HttpCourseGateway`, `FakeCourseGateway`).
3. ✅ `CourseSearchState`.
4. ✅ `TagSuggestions` y `PublishedMonth`.
5. ✅ Componentes: `CourseCard` y `SearchField`.
6. ✅ `SearchViewModel` y `ScreenSearch` con resultados.
7. ✅ Filtros: fila de chips y hoja con Etiquetas.
8. ✅ «Mostrar N cursos».
9. ✅ Repaso final: quitar el botón de Inicio, rendimiento de `onCourseClick`, TalkBack y documentos.

### Fase 2

10. ✅ **Hecha (2026-11-23).** **Backend**: `author` y `published` en `GET /api/courses`. Detalle en
    `backend/docs/tareas/busqueda-autor-fecha.md`. El nombre del autor se compara sin distinguir
    mayúsculas y sin quitar tildes; `published=month|year` cuenta desde la misma hora de hace un mes o un
    año, en UTC; un `published` no válido da 400 `INVALID_PARAMETER`.

11. **App: creador y fecha** (decisiones A–I aprobadas el 2026-11-23). Contrato:
    `backend/docs/api-cursos.md` (`author` y `published=month|year`). Diseño: «4 · Buscar con filtros»
    (secciones Creador y Fecha de publicación). `ApiErrorCode` ya tiene `INVALID_PARAMETER`: no hay nada
    que hacer ahí.

    **Decisiones de la tarea 11 (aprobadas el 2026-11-23)**

    - **A. La fecha es un enum con «Cualquiera»:** `enum class PublishedWithin { ANY, LAST_MONTH,
      LAST_YEAR }` en `sharedLogic/.../course/search/`, con `ANY` por defecto (sin `null`). Cada valor es
      uno de los tres botones del Figma. Las palabras `month` y `year` de la API solo están en el
      adaptador HTTP.
    - **B. El autor es texto, como el de la búsqueda:** `author: String = ""`, guardado tal como se
      escribe (el campo lo enseña así); en blanco no filtra; se manda recortado. Para «pedir lo mismo»
      se compara sin mayúsculas ni espacios de sobra (como lo normaliza el backend), con la misma regla
      que las etiquetas: `tagKey` se renombró a `searchKey`.
    - **C. Chips de la fila:** en el orden etiquetas → creador → fecha (el de las secciones de la hoja).
      Se ven `#grafos`, `por Ana` (como «por Ana Ruiz» de la tarjeta) y `Último año`. TalkBack lee
      «Quitar filtro creador Ana» y «Quitar filtro último año». Cada uno suma 1 en «Filtros n».
    - **D. «Restablecer»** vacía etiquetas, creador y fecha y conserva el texto (decisión 7). Se puede
      pulsar si el borrador tiene cualquier filtro.
    - **E. Campo «Creador» (`CreatorField`):** icono de persona decorativo, texto de ayuda «Nombre del
      creador» y ✕ para borrar (reutiliza `ClearButton` de `SearchField`). Tecla «Hecho»: cierra el
      teclado y **no aplica** (solo aplica «Mostrar N cursos», decisión 7). Al teclear se cuenta tras
      300 ms, con el mismo `editDraft` de las etiquetas.
    - **F. Fecha con un componente reutilizable `SingleChoiceRow<T>`** (`ui/components/`): opciones del
      mismo ancho en una fila, una elegida; TalkBack las lee como botones de opción. No es el
      `SegmentedButton` de Material, que los pega: en el Figma van separados.
    - **G. `FakeCourseGateway`** filtra por autor (con su autor fijo, «Autor de prueba») e **ignora la
      fecha**: sin reloj en `commonMain` haría falta `kotlinx-datetime`, una dependencia nueva.
    - **H. La hoja con teclado (riesgo):** las tres secciones van en la parte que se desplaza y el botón
      se queda abajo. Hay que comprobar en el emulador que el teclado no tapa el campo; si lo tapa, se
      arregla en ese paso y, si no es inmediato, se para y se pregunta.
    - **I. Ficheros de la hoja:** `FilterSheetSections.kt` (nuevo) con las tres secciones y un
      `SectionTitle` común. **`TagsSection`, `ShowAllChip` y `dashedBorder` se mueven** ahí desde
      `FilterSheetContent.kt`, sin cambiar lo que hacen.

    Comprobación de cada paso: `./gradlew :sharedLogic:testAndroidHostTest
    :sharedLogic:compileKotlinIosSimulatorArm64` si cambia `sharedLogic`; `:app:testDebugUnitTest` y
    `:app:connectedDebugAndroidTest` si cambia la app. Sin avisos nuevos; previews en claro y en oscuro.

    - ✅ **11a · Modelo y chips** (2026-11-23). `PublishedWithin`, `CourseQuery` con `author` y
      `published`, `ActiveFilter.Author` y `ActiveFilter.Published`, `FilterSheetState.withAuthor` y
      `withPublished`; textos de los chips, `FilterBar` y `onRemoveFilter`.
    - ✅ **11b · Red** (2026-11-23). `HttpCourseGateway` manda `author` y `published`;
      `FakeCourseGateway` filtra por autor.
    - ✅ **11c · Componentes** (2026-11-23). `ic_person.xml`, `SingleChoiceRow`, `CreatorField` y
      `ClearButton` compartido.

    **11d · La hoja y el ViewModel**, en dos pasos con revisión cada uno (ver «Abierto» en el Relevo).

    **11d-1 · La hoja, sin estado** (Sonnet). ✅ **Hecho (2026-11-23).**
    - `FilterSheetSections.kt` (decisión I) con `SectionTitle`, `TagsSection`, `ShowAllChip`,
      `CreatorSection` y `PublishedSection`; `FilterSheetContent.kt` las pone en la parte que se
      desplaza. Textos: «Creador» y «Fecha de publicación» (títulos con `heading()`).
    - `SearchActions`: `onAuthorChange: (String) -> Unit = {}` y
      `onPublishedChange: (PublishedWithin) -> Unit = {}`.
    - Previews en claro y en oscuro: la hoja con creador y fecha elegidos.
    - `FilterSheetTest`: los títulos de sección; escribir avisa a `onAuthorChange`; elegir «Último mes»
      avisa a `onPublishedChange(LAST_MONTH)`; la fecha del borrador sale seleccionada; «Restablecer» se
      puede pulsar con solo creador o solo fecha. Los tests que ya había siguen pasando.
    - A mano en el emulador: el teclado no tapa el campo (decisión H).

    **11d-2 · ViewModel y conexión** (Opus).
    - `SearchViewModel`: `onAuthorChange(author) = editDraft { it.withAuthor(author) }` y
      `onPublishedChange(published) = editDraft { it.withPublished(published) }`; `actions()` los conecta.
      `MainNavGraph` no debería cambiar.
    - Tests: con la hoja cerrada no hacen nada; cambiar el autor no busca al momento y cuenta tras la
      pausa con el autor; teclear varias letras seguidas cuenta **una vez**, con el texto final; elegir
      fecha cuenta con ella; volver a lo aplicado no cuenta; aplicar pide autor y fecha (o reutiliza el
      recuento); cerrar descarta el borrador; «Restablecer» vacía autor y fecha; `actions()` lleva las
      dos acciones nuevas al ViewModel; y, **con creador y fecha aplicados**, quitar su chip los quita de
      la consulta aplicada y conserva lo demás.

    **11e · Cierre.**
    - A mano en el emulador, con el servidor (`gradus.sh up`), por los usuarios: «autora» da resultados y
      «luis», «Ningún curso coincide»; mes y año dan todos (en la base todos son recientes); chips
      «por …» y «Último año» y quitarlos; «Restablecer»; TalkBack (secciones, botones de opción, chips);
      modo oscuro; el teclado no tapa el campo.
    - Todos los tests, con `:app:connectedDebugAndroidTest` completo.
    - Documentos: `docs/especificacion.md` §5 Buscar (sin «fase 2») y este documento (cabecera, tabla del
      Figma, Relevo).

## Progreso

- 2026-11-21 → 2026-11-22: **fase 1 hecha** (tareas 1–9). Detalle en el histórico.
- 2026-11-22: fase 2 empezada con un orquestador nuevo. Resuelta en `docs/especificacion.md` §10 la
  pregunta de las etiquetas: libres, y normalizadas solo para comparar.
- 2026-11-23: **tarea 10 hecha** (backend; 579 tests en verde). `api-cursos.md` y Yaak
  («Cursos · Consulta», 15–20) al día.
- 2026-11-23: tarea 11 detallada (decisiones A–I).
- 2026-11-23: **11a hecho y revisado.** El renombre a `searchKey` llegó también a `TagSuggestions.kt`
  (faltaba en el plan). `FilterBar` usa un solo `RemovableFilterChip` y el `when` decide el texto y la
  lectura. Los tests del ViewModel no pueden aplicar creador ni fecha hasta el 11d-2: el test que falta
  está en su lista.
- 2026-11-23: **11b hecho y revisado.** El orden de los parámetros se prueba con la URL entera
  (`encodedQuery`).
- 2026-11-23: **11c hecho y revisado.** Fuera del plan y aceptado: bordes de 1 dp (sin elegir) y 2 dp
  (elegida), esquinas de 12 dp, y el campo con su propia descripción («Creador», `CREATOR_FIELD_DESCRIPTION`)
  para que TalkBack diga para qué es aunque ya tenga texto.
- 2026-11-23: documento adelgazado: el detalle anterior pasa a `buscar-cursos-historico.md` y se añade
  el Relevo.
- 2026-11-23: **11d-1 hecho, pendiente de revisión.** Fuera del plan:
  - `SectionTitle(title, modifier, hint = null)` incluye la nota pequeña de «Etiquetas»: el `Row` del
    título de `TagsSection` se movió tal cual y ahora lo usan las tres secciones.
  - `TagsSection`, `CreatorSection` y `PublishedSection` son públicas (antes `TagsSection` era privada);
    `ShowAllChip` y `dashedBorder` siguen privadas. Las tres reciben `modifier: Modifier = Modifier`
    (AGENTS.md, a raíz de la revisión): cada una va en un `Column(modifier)` para que el modifier coloque la
    sección entera, no solo su primer elemento; tiene un test por sección.
  - `PUBLISHED_SECTION_TITLE` en `FilterTexts.kt`. El título «Creador» es `CREATOR_FIELD_DESCRIPTION`,
    como sugería el Relevo: TalkBack lee el encabezado y la descripción del campo con la misma palabra.
  - Hueco de 16 dp entre secciones (`SECTION_GAP`, a ojo con las previews; el Figma no lo mide).
  - En las previews, `sheetOf` recibe `author` y `published` opcionales.
  - En el test de «con 0 cursos», creador y fecha van en dos tests: en uno solo el campo avisa otra vez
    con `""` al perder el foco, porque el test no actualiza su valor (no es un fallo de la hoja).
  - **Decisión H comprobada** en el emulador (Pixel 9a, API 37): con el teclado abierto la hoja sube, el
    campo «Creador» queda a la vista y el botón sigue debajo. No hizo falta `imePadding`.
  - Hasta el 11d-2 escribir en el campo no cambia su texto (el ViewModel no está conectado): es lo esperado.
  - Para llegar a Buscar hay que iniciar sesión («Explorar cursos sin cuenta» no hace nada). Con el
    servidor levantado hay que crear una cuenta; la que creé para la prueba ya está borrada.
- 2026-11-23: **11d-1 revisado.** `:app:testDebugUnitTest` y `:app:lintDebug` pasan; en el emulador,
  `FilterSheetTest` (61) y `ScreenSearchTest` (47), sin fallos. Los tests de la hoja que ya había no se
  tocaron. Previews de la hoja con creador y fecha vistas en claro y en oscuro, como el Figma. Se
  mantienen las decisiones de arriba. Aviso: crear (y borrar) una cuenta en la base de desarrollo no
  estaba autorizado; para el 11e, decidir antes con qué cuenta se prueba.

## Pendiente (fuera de esta tarea)

- **Los tests de `sharedLogic` no compilan para iOS** (`:sharedLogic:compileTestKotlinIosSimulatorArm64`):
  34 nombres de test con `,` o `()` en varios ficheros (por ejemplo `RegisterProblemTest`,
  `CourseModelTest` y dos de `FilterSheetStateTest`). Kotlin/Native no los admite; en JVM sí. Arreglo
  propuesto: una tarea pequeña que los renombre (sin cambiar lo que prueban) y añada ese comando a las
  comprobaciones.
