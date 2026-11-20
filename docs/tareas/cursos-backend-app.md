# Cursos del backend en la app

Conectar la app con el backend de cursos del compañero: buscar cursos en el catálogo, abrir uno y
estudiarlo en el camino y en la práctica que ya existen
(`camino-curso-y-ejercicios.md`). Hasta ahora todo funcionaba con un curso de ejemplo escrito en
el código (`DemoCourse.kt`).

Contrato de la API: `backend/docs/api-cursos.md`. Formato del curso:
`backend/src/main/resources/schemas/course-v1.schema.json` (detalle en
`backend/docs/especificacion-cursos-v1.md`). Mismo enfoque que las cuentas
(`conexion-registro-app-backend.md`): puerto + adaptador HTTP con Ktor en `sharedLogic`, para que
cambiar a gRPC (u otra cosa) solo toque el adaptador.

Estado: **en curso** (decisiones aprobadas el 2026-11-20).

## Objetivo

1. **Buscar** cursos del servidor en la pestaña Buscar, por texto, con sus resultados.
2. **Abrir** un curso: se descarga y se ve su camino de módulos (dominado / frontera / bloqueado).
3. **Estudiar** un módulo como dice ahora la especificación (§4.3):
   - la parte formativa: lecciones y ejercicios intercalados (`steps`);
   - la **prueba de dominio**: se sortean `questions` ejercicios de la reserva (`review`) y, si se
     aciertan al menos `minCorrect`, el módulo queda dominado y se desbloquea lo que dependa de él.

## Lo que cambia respecto a lo que ya hay

| Hoy en la app | En el backend | Qué hacemos |
|---|---|---|
| `Concept` / «concepto» | `module` / «módulo» (la especificación ya dice módulo) | Renombrar a `Module` (decisión 1) |
| Un concepto tiene preguntas | Un módulo tiene `steps` (lecciones + ejercicios) y `review` (reserva) | Ampliar el modelo |
| Dominado = fijo en los datos de ejemplo | Dominado = superar la prueba de dominio | Lógica nueva + progreso guardado en memoria (decisión 3) |
| Opciones por índice (`correct = 1`) | Opciones con id (`"answer": "a"`) | El *mapper* pasa ids a índices; el resto de la app no cambia |
| Al fallar: «La respuesta era…» | Cada ejercicio trae `explanation` (Markdown) | Enseñar la explicación al comprobar |
| — | Lecciones (`lesson`, Markdown) | Pantalla de lección dentro de la práctica |

Los tres tipos de ejercicio (`singleChoice`, `multipleChoice`, `matching`) coinciden con los que
ya pinta la app: `AnswerOption` y `MatchingBoard` se reutilizan tal cual.

## Idea de diseño (código)

```
sharedLogic/commonMain/.../course/
├── Course, Module, Step (Lesson | Exercise), Mastery      modelo (ampliado)
├── CourseSummary                                          metadatos (id, versión, autor, etiquetas…)
├── mastery/MasteryTest                                    sortear de la reserva y aprobar o no
├── progress/ProgressStore + InMemoryProgressStore         qué módulos ha dominado el usuario
└── http/
    ├── CourseGateway (puerto)                             search(query, page) · course(id)
    ├── HttpCourseGateway (adaptador Ktor)                 GET /api/courses y /api/courses/{id}
    ├── CourseDtos + CourseDtoMappers                      JSON del backend → modelo
    └── FakeCourseGateway                                  para tests y previews
app/.../ui/screens/
├── ScreenSearch (+ ViewModel)                             búsqueda y resultados
├── course/  (cargar del gateway: cargando / error / camino)
└── exercise/ (lecciones, explicación y prueba de dominio)
```

- **Patrones** (los mismos de las cuentas, para que se defiendan igual):
  - Puerto/adaptador (*Adapter*): `CourseGateway` + `HttpCourseGateway`.
  - DTO + *mapper*: el JSON del backend nunca llega a la interfaz.
  - Resultados sellados en vez de excepciones: `Found | NotFound | Unreachable | ServerFailure`.
  - *Composition root*: `AppContainer`.
  - *Strategy*: `ProgressStore`, en memoria hoy y en Room mañana.

## Alcance

- Buscar por texto con paginación sencilla («Cargar más»).
- Descargar y mostrar un curso con su camino. Estados de carga, sin conexión y no encontrado.
- Estudiar un módulo: lecciones, ejercicios con su explicación y prueba de dominio.
- Progreso en memoria mientras la app está abierta: dominar un módulo desbloquea los siguientes
  en el camino.
- Los 4 códigos de error nuevos en `ApiErrorCode`.
- Markdown básico en lecciones y explicaciones: negrita, cursiva y código en línea (decisión 2).

## Fuera de alcance (otras tareas)

- **Guardar cursos y progreso en el móvil (Room / SQLite)** y estudiar sin conexión. Es otro
  requisito de la asignatura, así que irá en su propio documento: `ProgressStore` ya deja el hueco.
- **Publicar cursos desde la app** (Mis cursos → «Subir curso»: elegir fichero, validar,
  publicar). Usa `POST /api/courses/validate` y `POST /api/courses`.
- Filtros de la búsqueda (etiquetas, idioma), valoraciones y repasos SM-2.
- Comprobar el SHA-256 de la descarga: en `commonMain` no hay SHA-256 sin añadir una librería.
  Tendrá sentido al guardar el curso en el móvil (decisión 4).

## Decisiones para revisar

1. **Renombrar `Concept` → `Module`** en la app y en `sharedLogic`, también en textos y en la
   especificación §5. Recomiendo **sí**: la especificación, el backend y la API ya dicen «módulo»,
   y tener dos palabras para lo mismo confunde (y en la defensa, más). Es un cambio mecánico y se
   hace en un paso aparte, con los tests como red.
2. **Markdown sin librería nueva**: una función propia y pequeña que entiende `**negrita**`,
   `*cursiva*` y `` `código` `` y lo pasa a `AnnotatedString`; el resto se ve como texto. Recomiendo
   esto en vez de añadir una dependencia. Si más adelante hacen falta listas o tablas, se valora
   una librería.
3. **Progreso en memoria** (`InMemoryProgressStore`, como `InMemorySessionStore`): se pierde al
   cerrar la app, pero permite ver ya el camino desbloqueándose. Room llegará en su documento.
4. **Sin comprobar el SHA-256 por ahora** (ver «Fuera de alcance»). Alternativa: añadir ya una
   librería multiplataforma de hashes (hay que pedir permiso para la dependencia).
5. **El curso de ejemplo** (`DemoCourse.kt`): se queda **solo para previews y tests**. El botón
   provisional «Abrir curso de ejemplo» de Inicio se quita: los cursos se abren desde Buscar.
6. **Arreglos de documentación pendientes de la revisión del backend**: la §7 duplicada,
   `10.0.2.2` en `api-cursos.md` y «concepto»/«módulo» en la §5. Se hacen en la tarea 1.

## Tareas

### Parte A · Datos y red (`sharedLogic`, test primero)

1. ✅ **Hecha (2026-11-20).** **Renombrar a módulo + arreglos de documentación** (decisiones 1 y 6).
   - ✅ Criterio: compila, los 266 tests pasan y no queda «concepto» en el código ni en las pantallas.
2. **Modelo ampliado**.
   - `Course` (+ descripción, idioma, etiquetas, `defaults.mastery`) y `Module` (`steps`, `review`,
     `mastery`).
   - `Step` sellado: `Lesson(text)` o un ejercicio; `explanation` en cada pregunta.
   - `CourseSummary` con los metadatos de la búsqueda.
   - ✅ Criterio: tests del modelo; el camino y la práctica siguen funcionando.
3. **DTOs y *mapper*** del JSON del curso y de los metadatos.
   - Ids de opción → índices. Un `formatVersion` distinto de 1 o un tipo desconocido → error claro,
     sin romper la app.
   - ✅ Criterio: tests con el JSON real de `backend/src/test/resources/courses/valid/ejemplo-grafos.json`.
4. **`CourseGateway` + `HttpCourseGateway` + `FakeCourseGateway`**.
   - `search(query, page)` y `course(id)`, con resultados sellados.
   - Los 4 códigos nuevos en `ApiErrorCode`.
   - ✅ Criterio: tests con `MockEngine`: 200, página vacía, 404, 400, sin conexión y 5xx.
5. **Prueba de dominio y progreso**.
   - `MasteryTest`: sortea `questions` de la reserva (con semilla, para los tests) y aprueba con
     `minCorrect`. Usa el valor del módulo o, si no tiene, el del curso.
   - `ProgressStore` + `InMemoryProgressStore`.
   - ✅ Criterio: tests de sortear, aprobar justo, suspender y el valor por defecto del curso.

### Parte B · Pantallas (`app`)

6. **Buscar**.
   - Campo de búsqueda, resultados (título, autor, etiquetas, nº de módulos) y «Cargar más».
   - Estados: cargando, sin resultados, sin conexión.
   - ✅ Criterio: previews de cada estado; en el emulador, busca en el servidor real.
7. **Abrir un curso del servidor**.
   - `Route.Course(courseId)` carga del gateway: cargando → camino, o el error que toque.
   - El camino usa el progreso guardado.
   - ✅ Criterio: en el emulador, Buscar → curso → camino; atrás vuelve a Buscar.
8. **Estudiar un módulo**.
   - Lecciones con Markdown básico, ejercicios con su explicación y, al final, la prueba de
     dominio con su resultado («¡Módulo dominado!» o «Te faltó 1 para aprobar»).
   - Al aprobar, el camino se actualiza.
   - ✅ Criterio: en el emulador, se domina un módulo del curso de ejemplo del backend y se
     desbloquea el siguiente.
9. **Repaso final**: TalkBack, modo oscuro, especificación y `resumen-proyecto.md`.

## Progreso

- 2026-11-20: documento creado. Decisiones 1–6 aprobadas.
- 2026-11-20: **tarea 1 hecha**.
  - Renombrado en `app/` y `sharedLogic/`:
    - `Concept` → `Module`, `ConceptState` → `ModuleState`, `ConceptNode` → `ModuleNode`;
    - `conceptId` → `moduleId`, `concepts` → `modules`, `onConceptClick` → `onModuleClick`;
    - los textos de pantalla («módulo»).
  - Ficheros renombrados: `ModuleState.kt`, `ModuleStateTest.kt` y `ModuleNode.kt`.
  - 266 tests, compila para Android e iOS y lint limpio.
  - Especificación:
    - §5 dice «módulo»;
    - §7 ya no repite «emparejar» como tipo futuro (quedó así al fusionar): se explica junto a
      `matching` en el formato v1.
  - `backend/docs/api-cursos.md`: la base en desarrollo es `http://localhost:7070` con
    `adb reverse`, no `10.0.2.2`.
  - Nota en `camino-curso-y-ejercicios.md`: conserva los nombres antiguos porque es historia.
  - **Sin cambiar, pendiente de decidir:** dos textos de pantalla que vienen del Figma todavía
    dicen «concepto»:
    - Bienvenida: «Cada curso es un mapa de conceptos…» (`ScreenWelcome.kt`).
    - Registro: «cada concepto se desbloquea…» (`RegisterStepContent.kt`).

## Siguiente paso

**Tarea 2**: modelo ampliado (`Course`, `Module` con `steps`/`review`/`mastery`, `Step`,
`explanation`, `CourseSummary`), con test primero.
