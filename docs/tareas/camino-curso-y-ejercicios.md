# Camino del curso y ejercicios

Las pantallas para estudiar un curso: el **camino** con sus conceptos unidos por líneas y la
**práctica** con tres tipos de ejercicio. En la especificación §5 aparecen como «pendientes de
diseñar»: *detalle del curso con el grafo* y *práctica de preguntas*.

Reparto: el compañero termina el backend de cursos (aceptar y gestionar). Esta tarea es solo de
interfaz y lógica local (`app/` y `sharedLogic/`). Funciona con **datos de ejemplo**. Cuando el
backend esté listo, se conecta igual que las cuentas: un DTO, un *mapper* y un *gateway*.

Estado: **propuesta, pendiente de vuestro OK** (2026-11-19).

> **Nota (2026-11-20):** al conectar con el backend, «concepto» pasó a llamarse **«módulo»** en
> todo el código y las pantallas (`Concept` → `Module`, `ConceptState` → `ModuleState`,
> `ConceptNode` → `ModuleNode`, `conceptId` → `moduleId`…). Este documento conserva los nombres
> antiguos porque cuenta cómo se hizo. Ver `cursos-backend-app.md`, tarea 1.

## Objetivo

1. Ver un curso como un camino de arriba abajo, al estilo Duolingo. Cada concepto es un nodo y
   las **líneas** muestran los prerrequisitos. Cada nodo tiene uno de tres colores según su estado.
2. Al tocar un concepto de la frontera, practicar con sus preguntas. Hay tres tipos de ejercicio:
   - **Opción única** (test de una sola respuesta).
   - **Selección múltiple** (varias respuestas correctas).
   - **Emparejar**: unir elementos de dos columnas con flechas.

## Vocabulario: «módulo» = «concepto»

El compañero dice «módulos dados / frontera / no dados». En la especificación (§3) se llaman
**conceptos** y sus estados son **dominado / frontera / bloqueado**. Usamos esos nombres en el
código y en la interfaz para no tener dos palabras para lo mismo.

| Compañero | Especificación | Color (ya está en el tema) | Icono | ¿Se puede tocar? |
|---|---|---|---|---|
| Dado | Dominado | Morado `EstadoDominado` | ✓ | Sí (repasar) |
| Frontera | Frontera | Naranja `colorScheme.tertiary` | ▶ | Sí (practicar) |
| No dado | Bloqueado | Fondo `surface` con borde `outline` | 🔒 | No: dice qué falta |

Son los mismos colores que ya usan `UnlockGraphIllustration` y `WelcomeIllustration`, así que la
app se ve igual en todas partes. El estado no se distingue **solo** por el color: cada nodo lleva
además un icono y su `contentDescription` (por ejemplo, «Grafos, frontera»).

## Diseño propuesto

No hay diseño en Figma para estas pantallas. Propuesta: diseñarlas directamente en Compose. Las
previews en claro y en oscuro hacen de diseño, y podemos pasar capturas al compañero. Ver la
decisión 1.

### Camino del curso

Un curso es un **grafo**, no una lista: un concepto puede tener varios prerrequisitos y
desbloquear varios conceptos a la vez. Por eso el camino se organiza **por niveles**:

- **Nivel 0:** los conceptos sin prerrequisitos.
- **Nivel *n*:** el concepto que está un paso después de su prerrequisito más lejano.
- Los conceptos del mismo nivel van en la misma fila.
- Si una fila solo tiene un concepto, se desplaza a la izquierda o a la derecha de forma
  alternada: así sale el zigzag de Duolingo.

```
┌──────────────────────────────┐
│ ←  Estructuras de datos      │
│    2 de 6 dominados  ▓▓░░░░  │
│                              │
│        (✓) Conjuntos         │  nivel 0   morado
│           ╲                  │
│            (✓) Relaciones    │  nivel 1   morado
│           ╱      ╲           │
│   (▶) Grafos    (▶) Árboles  │  nivel 2   naranja
│      │   ╲         ┊         │
│   (🔒) BFS  (🔒) DFS ┘        │  nivel 3   gris
└──────────────────────────────┘
  ─── línea llena (morada): el concepto de abajo ya está desbloqueado
  ┊┊┊ línea discontinua (gris): el concepto de abajo sigue bloqueado
```

- Las líneas son curvas suaves de cada prerrequisito al concepto que depende de él, dibujadas con
  `Canvas` **detrás** de los nodos.
- Cada nodo es un círculo de 56 dp (más que los 48 dp mínimos) con el título debajo.
- Al tocar un nodo:
  - **Frontera o dominado:** abre la práctica de ese concepto.
  - **Bloqueado:** muestra un aviso con lo que falta, por ejemplo «Domina antes Grafos».
- Pantalla sin barra inferior, porque no es una pestaña: tiene una barra superior con «atrás».

### Práctica (los tres ejercicios)

Todos siguen el mismo ciclo: **elegir → «Comprobar» → ver si está bien → «Continuar»**. Arriba
hay una barra de progreso (pregunta 2 de 5). Al acabar, un resumen con los aciertos.

```
 Opción única            Selección múltiple        Emparejar
┌───────────────────┐   ┌───────────────────┐   ┌────────────────────────┐
│ ▓▓▓▓░░░░  2 / 5   │   │ Marca todas las   │   │ Une cada recorrido con │
│ ¿Qué estructura   │   │ que sean árboles: │   │ su estructura:         │
│ usa BFS?          │   │                   │   │                        │
│ ( ) Pila          │   │ [✓] Árbol binario │   │ [ BFS ]──────▶[ Pila ] │
│ (●) Cola          │   │ [ ] Grafo cíclico │   │ [ DFS ]─╲  ╱─▶[ Cola ] │
│ ( ) Montículo     │   │ [✓] Montículo     │   │          ╳             │
│                   │   │                   │   │ (toca uno de cada lado)│
│ [  Comprobar  ]   │   │ [  Comprobar  ]   │   │ [  Comprobar  ]        │
└───────────────────┘   └───────────────────┘   └────────────────────────┘
```

- **Opción única:** al tocar una opción se desmarca la anterior.
- **Selección múltiple:** se marcan y desmarcan libremente. Es correcta solo si coincide
  **exactamente** con las respuestas buenas (sin puntos parciales). El enunciado avisa de que puede
  haber varias.
- **Emparejar:** se **toca** un elemento de la izquierda y luego uno de la derecha, y aparece una
  flecha entre los dos. Volver a tocar un elemento ya unido lo desune. No hay que arrastrar: es
  más fácil de usar con TalkBack y sigue la idea de la especificación para «ordenar» (§7: *tocando,
  sin arrastrar*). La columna derecha se baraja.
- **Al comprobar:** las respuestas correctas se marcan en morado y las incorrectas en rojo
  (`colorScheme.error`). Debajo aparece un mensaje («¡Correcto!» o «La respuesta era…»), con
  `liveRegion` para que lo lea TalkBack.

## Idea de diseño (código)

```
sharedLogic/commonMain/.../course/            Kotlin puro, test primero
├── Course, Concept(id, title, requires)      modelo del curso
├── ConceptState + Course.statesFor(mastered) dominado / frontera / bloqueado (regla 2 de la §4)
├── CoursePathLayout                          niveles, columnas, zigzag y líneas (función pura)
└── exercise/
    ├── Question (sealed)                     SingleChoice · MultipleChoice · Matching
    ├── Answer + Question.isCorrect(answer)   corrección de cada tipo
    └── ExerciseState                         Choosing → Checked(correct)
app/.../ui/
├── components/ConceptNode, CoursePath, AnswerOption, MatchingBoard
├── screens/course/ScreenCourse (+ ViewModel)
└── screens/exercise/ScreenExercise (+ ViewModel)
app/.../data/DemoCourse.kt                    curso de ejemplo para previews y emulador
```

- **La lógica va en `sharedLogic`, no en la interfaz:**
  - Calcular el estado de cada concepto, colocar los nodos y corregir respuestas son funciones
    puras con tests.
  - La interfaz solo pinta lo que le dan y avisa de lo que se toca (*state hoisting*).
- **Patrones:**
  - `Question` es una *sealed interface*: cada tipo sabe corregirse. El patrón es **Strategy**, y
    el compilador obliga a cubrir los tres tipos en cada `when`.
  - `ExerciseState` es una **máquina de estados** pequeña, como `Submission` en el registro.
  - El cálculo del camino es una **función pura**: el mismo curso da siempre el mismo dibujo.
  - El backend se conectará con un **Adapter**: un `CourseGateway` + `HttpCourseGateway`, como
    `AccountGateway`.
- **Rutas nuevas:**
  - `Route.Course(courseId)` y `Route.Exercise(courseId, conceptId)`, dentro del grafo
    `Route.Main`, pero sin barra inferior.
  - Acceso provisional: un enlace «Abrir curso de ejemplo» en Inicio, hasta que exista la pantalla
    de Inicio de verdad.

## Alcance

- Modelo del curso, cálculo de estados y distribución del camino, con tests.
- Modelo de los tres tipos de pregunta, su corrección y el estado de un ejercicio, con tests.
- Componentes y pantallas del camino y de la práctica, con previews en claro y en oscuro.
- Un curso de ejemplo con unos 6 conceptos y 2 o 3 preguntas de cada tipo.
- Iconos de Material Symbols nuevos en `res/drawable`: candado y *play*. El ✓ ya existe.
- Navegación: camino → práctica → resumen → volver al camino.

## Fuera de alcance

- **Guardar el progreso** y la regla «4 de las 5 últimas» (motor de aprendizaje, §4.3). Necesita
  Room, que todavía no está. En esta tarea, lo dominado viene fijo en los datos de ejemplo y
  responder no cambia el camino.
- **Conectar con el backend de cursos**: es otra tarea, cuando el compañero lo termine.
- Los otros tipos de la §7: respuesta corta, numérica, ordenar y tarjeta autoevaluada.
- Repasos (SM-2), notas con cámara y la pantalla de Inicio real.
- Comprobar ciclos en el grafo: eso lo hace el validador del curso (§6). Aquí se supone que el
  curso ya es válido.

## Decisiones para revisar

1. **Diseño:** ¿en Compose directamente, o primero en Figma? Recomiendo **Compose**: no hay
   diseño previo, las previews sirven de diseño y en Figma ya nos topamos con el límite del plan.
2. **Emparejar no está en la especificación.** La §7 tiene opción única, selección múltiple,
   respuesta corta, numérica, ordenar y tarjeta. Propuesta: añadir **«Emparejar (unir con
   flechas tocando, sin arrastrar)»** a la §7 y dejar el resto para más adelante.
3. **Ficheros antiguos con nombres en español:**
   - `PantallaCurso.kt`, `PantallaPregunta.kt`, `TarjetaConcepto.kt` y `OpcionRespuesta.kt` están
     vacíos o a medias.
   - `Concepto.kt` (`Estado`, `ConceptoUi`) y `DatosDemo.kt` también son de antes.
   - El AGENTS.md pide nombres en inglés y no puedo borrarlos sin permiso. Propuesta: crear los
     nuevos en inglés y, al terminar, **borrar los antiguos si me dais permiso**. Antes, preguntad
     al compañero si son suyos.
4. **Acordar con el compañero los nombres del JSON** para que el *mapper* sea sencillo. Propuesta
   (los nombres siguen la §6):

   ```json
   { "formatVersion": 1, "id": "ed", "title": "Estructuras de datos",
     "concepts": [ { "id": "grafos", "title": "Grafos", "requires": ["relaciones"] } ],
     "questions": [
       { "id": "q1", "concept": "bfs", "type": "single",   "prompt": "¿Qué usa BFS?",
         "options": ["Pila", "Cola"], "correct": [1] },
       { "id": "q2", "concept": "arboles", "type": "multiple", "prompt": "…",
         "options": ["…"], "correct": [0, 2] },
       { "id": "q3", "concept": "bfs", "type": "matching", "prompt": "…",
         "pairs": [ { "left": "BFS", "right": "Cola" }, { "left": "DFS", "right": "Pila" } ] }
     ] }
   ```

   Si él ya tiene otros nombres, nos adaptamos a los suyos: solo cambia el *mapper*.

## Tareas

### Parte A · Camino del curso

1. ✅ **Hecha (2026-11-19).** **Modelo del curso y estados** (`sharedLogic`, test primero).
   - `Course`, `Concept` y `ConceptState`.
   - `statesFor(mastered)`: un concepto es frontera si no está dominado y **todos** sus
     prerrequisitos lo están. Si le falta alguno, está bloqueado. Los conceptos sin prerrequisitos
     empiezan en la frontera.
   - `missingFor(concept)` devuelve los prerrequisitos que faltan (para el aviso).
   - ✅ Criterio: tests de cada caso (sin prerrequisitos, uno, varios, todos dominados).
2. ✅ **Hecha (2026-11-19).** **Distribución del camino** (`sharedLogic`, test primero).
   - `CoursePathLayout`: el nivel de cada concepto, su columna dentro de la fila, el zigzag en las
     filas con un solo nodo y la lista de líneas (de, a, ¿desbloqueada?).
   - ✅ Criterio:
     - Tests con una cadena, un rombo (A→B, A→C, B y C→D) y el curso de ejemplo.
     - Ningún concepto queda por encima de uno de sus prerrequisitos.
3. ✅ **Hecha (2026-11-19), falta mirar las previews.** **`ConceptNode`** (`app/ui/components`).
   - Círculo de 56 dp con el color y el icono de su estado y el título debajo.
   - `contentDescription` con el nombre y el estado.
   - ✅ Criterio: previews de los 3 estados en claro y en oscuro, y zona táctil de 48 dp o más.
4. ✅ **Hecha (2026-11-19).** **`CoursePath`**, que al final se llama **`CourseMap`**.
   - Coloca los nodos según `CoursePathLayout` y dibuja detrás las líneas con `Canvas`: llenas y
     moradas si llevan a un concepto desbloqueado, discontinuas y grises si no.
   - Se desplaza en vertical.
   - ✅ Criterio:
     - Preview del curso de ejemplo en claro y en oscuro.
     - Las líneas no se cortan con los títulos.
5. ✅ **Hecha (2026-11-19).** **`ScreenCourse` y navegación**.
   - Curso de ejemplo (`DemoCourse`), barra superior con título y progreso, y ruta `Route.Course`.
   - Acceso provisional desde Inicio.
   - Al tocar un nodo bloqueado sale un aviso con lo que falta.
   - ✅ Criterio, en el emulador:
     - Se abre desde Inicio y «atrás» vuelve a Inicio.
     - El giro de pantalla mantiene el scroll.

### Parte B · Ejercicios

6. ✅ **Hecha (2026-11-19).** **Preguntas y corrección** (`sharedLogic`, test primero).
   - `Question` (`SingleChoice`, `MultipleChoice`, `Matching`), `Answer` e `isCorrect`.
   - El barajado de la columna derecha es una función con semilla, para poder probarla.
   - ✅ Criterio: tests de acierto y fallo de cada tipo, la selección múltiple sin puntos
     parciales y un emparejamiento incompleto (que nunca es correcto).
7. ✅ **Hecha (2026-11-19).** **Estado de un ejercicio** (`sharedLogic`, test primero).
   - `ExerciseState`: elegir, cambiar la elección, `canCheck` (no se comprueba sin responder),
     `check()` y, en emparejar, unir, desunir y volver a unir.
   - `PracticeSession`: la lista de preguntas de un concepto, la actual, el progreso y los
     aciertos.
   - ✅ Criterio: tests de cada transición.
8. ✅ **Hecha (2026-11-19), falta mirar las previews.** **`AnswerOption`** (opción única y selección múltiple).
   - Tarjeta tocable con radio o casilla y con sus estados: normal, elegida, correcta e incorrecta.
   - Sustituye al `OpcionRespuesta` vacío.
   - ✅ Criterio: previews de todos los estados en claro y en oscuro, y semántica de
     `RadioButton`/`Checkbox` para TalkBack.
9. ✅ **Hecha (2026-11-19), falta mirar las previews.** **`MatchingBoard`** (emparejar con flechas).
   - Dos columnas de tarjetas. Se toca una de cada lado y aparece una flecha dibujada con
     `Canvas`; la posición de cada tarjeta se mide con `onGloballyPositioned`.
   - Volver a tocar desune.
   - ✅ Criterio:
     - Previews sin unir, a medias, comprobado bien y comprobado mal.
     - Usable solo con TalkBack (cada tarjeta dice con qué está unida).
10. ✅ **Hecha (2026-11-19), falta probarla en el emulador.** **`ScreenExercise` y navegación**.
    - Barra de progreso, pregunta, botón «Comprobar»/«Continuar», mensaje de resultado y
      resumen final.
    - `ViewModel` con `PracticeSession` y ruta `Route.Exercise`.
    - Se abre al tocar un nodo de la frontera o dominado.
    - ✅ Criterio, en el emulador:
      - Se completa una práctica con los tres tipos.
      - Al acabar se vuelve al camino.
      - El giro de pantalla no pierde la respuesta.
11. ✅ **Hecha en parte (2026-11-19): falta lo del emulador.** **Repaso final**.
    - TalkBack en las dos pantallas y modo oscuro.
    - Actualizar la especificación (§5 y §7, si se aprueba la decisión 2) y este documento.
    - ✅ Criterio: la §10 del AGENTS.md cumplida.

## Progreso

- 2026-11-19: documento creado. Pendiente del OK a las decisiones 1–4.
- 2026-11-19: **decisión 3 aprobada**: se pueden borrar los ficheros antiguos en español cuando
  los nuevos los sustituyan.
- 2026-11-19: **decisión 1 resuelta**: el Figma no tiene el curso por dentro (solo Buscar, Filtros
  y Mis cursos), así que se diseña en Compose. **Decisión 2 aprobada**: «Emparejar» añadido a la §7
  de la especificación. La decisión 4 (nombres del JSON) queda para hablarla con el compañero; no
  bloquea, porque el modelo de `sharedLogic` no depende del JSON.
- 2026-11-19: **tarea 1 hecha**.
  - `course/Course.kt` (`Course`, `Concept`) y `course/ConceptState.kt` (`stateOf`, `statesFor`,
    `missingFor`, `masteredCount`).
  - 11 tests en `ConceptStateTest`; 193 en total, todos pasan.
  - Decidido: lo dominado sigue dominado aunque falte un prerrequisito (regla 5, provisional).
- 2026-11-19: **tarea 2 hecha**.
  - `course/CoursePath.kt` (`PathNode`, `PathEdge`, `CoursePath`) y `course/CoursePathLayout.kt`
    (`Course.pathLayout()`).
  - 14 tests en `CoursePathLayoutTest`; 207 en total, todos pasan.
  - Cambio respecto al plan: el camino **no** sabe lo que está dominado, solo la geometría. Si una
    línea está «desbloqueada» lo decide la interfaz con `stateOf(edge.to)`. Así el mismo dibujo
    sirve para cualquier progreso y la función queda más simple.
  - Un ciclo lanza `IllegalArgumentException` (no debería llegar: lo filtra el validador).
  - Arreglo: usaba `toSortedMap()`, que solo existe en la JVM. Los tests pasaban (se ejecutan en
    la JVM), pero `commonMain` no compilaba para iOS y el IDE lo marcaba en rojo. Cambiado por
    `keys.sorted()`. Para detectarlo: `./gradlew :sharedLogic:compileCommonMainKotlinMetadata`.
- 2026-11-19: **tarea 3 hecha**.
  - `ui/components/ConceptNode.kt` (`ConceptNode`, `ConceptNodeDefaults`).
  - Iconos `ic_lock.xml` e `ic_play.xml` (Material Icons, rellenos; el ✓ es `ic_check`).
  - Todo el nodo (círculo y título, 104 dp de ancho) es una sola zona táctil. TalkBack lo lee como
    «Grafos, frontera, botón, doble toque para practicar».
- 2026-11-19: **tarea 4 hecha**.
  - `ui/components/CourseMap.kt`. El composable se llama `CourseMap` y no `CoursePath`, porque
    `CoursePath` ya es la clase de datos de `sharedLogic`.
  - `data/DemoCourse.kt` (7 conceptos; el plan lo ponía en la tarea 5, pero hacía falta para la
    preview).
  - Las líneas son curvas en S del borde inferior de un círculo al superior del siguiente. Moradas
    y llenas si el concepto de destino está desbloqueado; grises y discontinuas si está bloqueado.
  - El título del nodo lleva fondo del color de la pantalla, así que tapa la línea que pasa por
    detrás. Los títulos admiten ahora 3 líneas («Búsqueda en profundidad (DFS)» no cabía en 2).
  - `CourseMap` no tiene scroll propio: lo pondrá la pantalla (tarea 5).
  - Comprobado con capturas en el emulador (API 37), en claro y en oscuro, con un test
    instrumentado temporal que ya está borrado.
- 2026-11-19: **mejora del camino para niveles anchos** (con 3 o más conceptos se liaba).
  - Reparto: cada concepto va en el centro de su parte del ancho (con 3: 1/6, 3/6 y 5/6). Así
    caben 3 nodos sin que choquen los títulos.
  - Como mucho 3 por fila: un nivel con más se parte en filas equilibradas (4 = 2 + 2, 5 = 3 + 2).
  - Las líneas que cruzan filas pasan por el hueco libre más cercano (`PathEdge.via`), nunca por
    detrás de un nodo. Antes, «Tablas hash → Colisiones» parecía salir de DFS.
  - 10 tests nuevos en `CoursePathLayoutTest`; 217 en total, todos pasan.
- 2026-11-19: **tarea 5 hecha**.
  - `ui/screens/course/`: `ScreenCourse` (+ `CourseActions`), `CourseViewModel` (+ `CourseUiState`
    y `CourseNotice`) y `CourseNoticeMessages`. `ui/components/CourseHeader.kt`.
  - `Route.Course(courseId)` en `Routes.kt`, el destino en `MainNavGraph.kt` (sin barra) y el
    botón provisional «Abrir curso de ejemplo» en `ScreenHome`. `BackButton` de `FormTopBar.kt`
    pasa a ser público para reutilizarlo.
  - El curso de ejemplo crece a 10 conceptos, con un nivel de 3 y otro de 4.
  - Al tocar un concepto bloqueado sale un Snackbar («Para desbloquear «DFS», domina antes
    Grafos.»). Si está en la frontera o dominado, por ahora sale «La práctica llegará pronto»; la
    tarea 10 lo cambiará por abrir la práctica.
  - Decidido al probarlo: la cabecera se desplaza con el camino (en horizontal, fija, se comía
    media pantalla) y el camino mide como mucho 480 dp, centrado.
  - Comprobado en el emulador (API 37), de principio a fin con el backend: iniciar sesión → Inicio →
    curso → avisos → girar (mantiene el scroll) → atrás vuelve a Inicio.
- 2026-11-19: **tarea 6 hecha**.
  - `course/exercise/Question.kt` (`Question` sellada: `SingleChoice`, `MultipleChoice`,
    `Matching` + `MatchPair`) y `course/exercise/Answer.kt` (`Single`, `Multiple`, `Matches`).
  - `Course` tiene ahora `questions` (por defecto vacía) y `questionsFor(conceptId)`.
  - Las respuestas usan **índices**, no textos. En emparejar se comparan los textos, así que si
    dos elementos de la derecha dicen lo mismo valen los dos.
  - `Matching.rightOrder(random)` baraja la columna derecha y nunca la deja en el mismo orden
    que la izquierda. Recibe el `Random` para fijar la semilla en los tests.
  - Una pregunta mal hecha (opción correcta que no existe, menos de 2 opciones o parejas, o
    selección múltiple sin ninguna correcta) lanza `IllegalArgumentException` al crearla.
  - 18 tests nuevos (`QuestionTest` y uno en `ConceptStateTest`); 235 en total, todos pasan.
  - Las preguntas del curso de ejemplo se añadirán cuando haga falta enseñarlas (tareas 8–10).
- 2026-11-19: **tarea 7 hecha**.
  - `course/exercise/Draft.kt`: la respuesta a medias de cada tipo (`SingleDraft`,
    `MultipleDraft`, `MatchingDraft`).
  - `course/exercise/ExerciseState.kt`: `ExerciseState` (`choose`, `tapLeft`, `tapRight`,
    `canCheck`, `check`) y el factory method `Question.startExercise(random)`.
  - `course/exercise/PracticeSession.kt`: `start`, `update`, `next`, `position`/`total`,
    `completedCount`, `correctCount` y `isFinished`.
  - Reglas de emparejar:
    - Se toca uno de cada lado, en cualquier orden.
    - Tocar el elemento que está esperando lo suelta, y tocar otro del mismo lado cambia cuál
      espera.
    - Tocar un elemento ya unido lo desune.
    - Solo se puede comprobar cuando están todos unidos.
  - Comprobada una pregunta, ya no se puede cambiar la respuesta. No se pasa a la siguiente sin
    comprobar.
  - El orden barajado de la derecha se fija al empezar y vive en el estado, así que sobrevive al
    giro de pantalla.
  - 26 tests nuevos (`ExerciseStateTest` y `PracticeSessionTest`); 261 en total, todos pasan.
- 2026-11-19: **tarea 8 hecha**.
  - `ui/components/AnswerOption.kt` (`AnswerOption`, `AnswerOptionKind`, `AnswerFeedback`).
  - Icono nuevo `ic_close.xml` (Material Icons). `OpcionRespuesta.kt`, que estaba vacío, borrado
    (decisión 3).
  - Aspectos:
    - normal: `surface`;
    - elegida: `primaryContainer` con borde `primary`;
    - correcta: morado `EstadoDominado` con ✓;
    - incorrecta: `errorContainer` con borde `error` y ✗.
  - Una opción buena que no se eligió también sale en morado, para enseñar la respuesta.
  - Accesibilidad:
    - La tarjeta entera es la zona táctil (56 dp de alto como mínimo).
    - TalkBack la lee como radio o casilla, con su estado y, tras comprobar, «correcta» o
      «incorrecta».
  - Previews de los dos tipos con todos los aspectos, en claro y en oscuro. El emulador se cerró
    por falta de memoria, así que no hay captura: falta mirarlas en Android Studio.
- 2026-11-19: **tarea 9 hecha**.
  - `ui/components/MatchingBoard.kt`.
  - `Matching.isRightFor(left, right)` en `sharedLogic`, para saber si cada pareja está bien
    (+1 test; 262 en total).
  - Cambio respecto al plan: en vez de medir las tarjetas con `onGloballyPositioned`, un `Layout`
    propio las coloca y apunta su posición; `drawBehind` dibuja las flechas justo después, en el
    mismo fotograma. Ventajas:
    - no hace falta estado extra;
    - las flechas no llegan con un fotograma de retraso;
    - funciona en la preview.
  - Las filas se alinean: cada una mide lo que la más alta de sus dos tarjetas (`minIntrinsicHeight`).
  - Colores de las flechas: moradas (`primary`) mientras se responde; al comprobar, moradas
    (`EstadoDominado`) las parejas bien y rojas (`error`) las parejas mal. Las tarjetas también se
    marcan, así que el resultado no depende solo del color de la flecha.
  - TalkBack: cada tarjeta es un botón con su estado, por ejemplo «BFS, unido con Cola» o «DFS,
    seleccionado, sin unir».
  - Previews: sin unir, a medias, comprobado bien y comprobado mal, en claro y en oscuro.
- 2026-11-19: **tarea 10 hecha** (compila, lint limpio y 266 tests; falta el emulador, que se cerró
  por falta de memoria).
  - `sharedLogic`: `course/exercise/OptionMarks.kt` con `OptionMark`,
    `ExerciseState.isSelected` y `markFor`. Las buenas salen como correctas, también las no
    elegidas; las elegidas que no eran buenas, como incorrectas. +4 tests.
    - `AnswerOption` usa ahora `OptionMark` en vez de su `AnswerFeedback`: una sola fuente.
  - `ui/screens/exercise/`:
    - `ScreenExercise` (+ `ExerciseActions`);
    - `ExerciseViewModel` (+ `ExerciseUiState`);
    - `ExerciseParts` (barra superior con cerrar y progreso, barra inferior con
      resultado y «Comprobar»/«Continuar»/«Ver resultado», y el resumen);
    - `ExerciseTexts` (pista, solución y resumen).
  - `ui/components/SpanishText.kt` (`joinedWithAnd`), sacado de `CourseNoticeMessages` para
    reutilizarlo.
  - `Route.Exercise(courseId, conceptId)` y su destino en `MainNavGraph`.
  - El camino ya no dice «llegará pronto». Un concepto de la frontera o dominado abre su
    práctica: `CourseUiState.practiceToOpen` es un evento y el NavHost navega y lo borra.
  - El curso de ejemplo tiene 11 preguntas en los 6 conceptos que se pueden practicar, de los tres
    tipos.
  - Si se falla, se enseña la solución, por ejemplo «La respuesta era «Cola»» o «Las parejas
    eran: …».
  - Un concepto sin preguntas enseña directamente «Este concepto todavía no tiene preguntas».
  - Como estaba previsto, responder no cambia el camino: el progreso se guardará con Room.
- 2026-11-19: **tarea 11, la parte sin emulador**.
  - Borrados los ficheros antiguos en español (decisión 3): `PantallaCurso.kt`,
    `PantallaPregunta.kt`, `TarjetaConcepto.kt`, `DatosDemo.kt` y `model/Concepto.kt`. Solo se
    usaban entre ellos.
  - Especificación §5: el camino del curso y la práctica, descritos; «pendientes de diseñar»
    queda solo para repasos y notas con cámara. La §7 ya tenía emparejar.
  - Accesibilidad, revisada en el código: el contador «2 / 5» se lee «Pregunta 2 de 5».
  - 266 tests y lint limpio en los ficheros de esta funcionalidad.
  - **Pendiente, en el emulador** (no se arrancó: solo 3,5 GB de memoria libre):
    1. Una práctica completa con los tres tipos.
    2. Girar la pantalla a mitad de una pregunta.
    3. Salir con atrás y con ✕.
    4. TalkBack en el camino y en la práctica.
    5. Modo oscuro en las dos pantallas.

## Siguiente paso

Las pruebas en el emulador que faltan (lista de la tarea 11). Con eso, el documento queda
terminado. Para después, fuera de este documento:
- guardar el progreso con Room (regla «4 de las 5 últimas»), para que practicar cambie el camino;
- conectar con el backend de cursos del compañero (un `CourseGateway`, como `AccountGateway`).
