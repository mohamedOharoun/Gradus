# Progreso del estudiante — cómo está hecho y cómo se comporta

Documento de **referencia**: describe el estado **actual** (no la historia; esa está en
`docs/tareas/progreso-logica.md` y `progreso-app.md`). Léelo antes de mergear una rama que toque el
progreso o de añadir algo que lo use (p. ej. un tipo de ejercicio nuevo).

Cada paso que cambie una pieza actualiza su sección; el orquestador la revisa junto con el código.
Contrato con el servidor: `backend/docs/api-progreso.md`.

## Mapa de piezas

Puertos, implementaciones, casos de uso y el patrón de diseño de cada uno.

Paquete `course/progress/` (Kotlin puro, sin textos):

| Pieza | Qué es | Patrón |
|---|---|---|
| `StudyAccess` (`Allowed \| NotEnrolled`) | Si se puede estudiar un curso | Jerarquía sellada (tipo suma): el `when` es exhaustivo |
| `studyAccess(enrolled)` | Decide el acceso solo por la inscripción | Función pura |
| `Enrollment(courseId, enrolledAt)` | Una inscripción y su fecha | *Value object* (`data class`) |
| `MasteredModule(moduleId, masteredAt)` | Un módulo dominado y su fecha | *Value object* (`data class`) |
| `EnrollmentStore` | Puerto: `enrolled(courseId): Flow<Boolean>` y `suspend enroll(courseId)` | *Strategy* (puerto con implementaciones intercambiables) |
| `ProgressStore` | Puerto: `mastered(courseId): Flow<Set<String>>` y `suspend markMastered(courseId, moduleId)` | *Strategy* |
| `InMemoryProgressStore(sessions, clock)` | Implementa **los dos puertos** con un solo `MutableStateFlow`; la parte C pondrá Room en su lugar | *Interface Segregation* (un objeto, dos puertos pequeños); inyección de dependencias (`SessionStore`, `Clock`) |
| `UserProgress` → `CourseProgress` (`internal`) | El estado de un usuario: `courses: courseId → CourseProgress(enrollment, mastered)` | Valor inmutable con transformaciones puras (enfoque funcional) |

Las fechas son `kotlin.time.Instant` (estable, sin `@OptIn`), nunca texto. El `Clock` se inyecta
(`Clock.System` por defecto); los tests usan un `FakeClock`.

Cómo está hecho `InMemoryProgressStore`:

- **Estado:** un `MutableStateFlow<Map<String, UserProgress>>`; la clave es `session.account.id`.
- **Lo dominado está anidado en su inscripción** (`CourseProgress.mastered`), como en el contrato.
  El estado «dominados sin inscripción» no se puede representar.
- **Escribir** (`enroll`, `markMastered`): primero se lee la sesión y la hora (`current()` es
  `suspend`, y la lambda de `update` puede repetirse, así que debe ser pura); después, un
  `update` atómico aplica la transformación. Si la lambda lanza, el valor no cambia: no queda nada.
- **Leer** (`mastered`, `enrolled`): un `flow { … }` que lee la sesión **al empezar a escuchar**.
  Sin sesión emite el vacío (`emptySet()` / `false`) y termina. Con `distinctUntilChanged` no avisa
  de cambios en otros cursos.
- `progressOf(userId)` es `internal` y de solo lectura: existe para que los tests vean las fechas,
  que los puertos no exponen. En 4c se decidirá si lo sustituye el puerto de pendientes.

*Pendiente (4c y siguientes).*

## Flujo

Inscribirse → estudiar → dominar → sincronizar.

1. **Inscribirse:** `EnrollmentStore.enroll(courseId)`. Sin error si ya estaba inscrito (se queda la
   primera fecha). Sin sesión lanza `IllegalStateException`.
2. **Estudiar:** la interfaz lee `enrolled(courseId)` y decide con `studyAccess(enrolled)`.
   `NotEnrolled` → no se ofrece estudiar ni practicar. Funciona sin red: todo sale del almacén local.
3. **Dominar:** `ProgressStore.markMastered(courseId, moduleId)` guarda el módulo con la fecha del
   `Clock`. Dominar dos veces conserva la primera fecha. Sin inscripción o sin sesión lanza
   `IllegalStateException` y no escribe nada.
4. **Sincronizar:** *pendiente (tareas 4c, 5 y 6).*

## Invariantes

Reglas que nunca se rompen (p. ej. no hay módulo dominado sin inscripción).

- `studyAccess` solo mira la inscripción; no recibe el estado del módulo (B-6). Combinarlo con
  `ModuleState` es cosa de la interfaz (C9).
- **No existe un módulo dominado sin inscripción.** Lo impone la estructura (`CourseProgress`
  contiene lo dominado) y `UserProgress.masteredIn`, que lanza `IllegalStateException` si el curso
  no está inscrito. El mensaje lleva el `courseId`, nunca el token.
- **Un fallo no deja nada escrito:** la comprobación va dentro de la lambda de `update`.
- **El progreso es por usuario** (clave: `account.id`). Cambiar de sesión no mezcla ni pierde datos.
- **Sin sesión:** leer da vacío/`false` y el `Flow` termina; escribir lanza `IllegalStateException`
  (fallo de programación, no un caso de usuario).
- **La primera fecha se conserva:** inscribirse o dominar otra vez no cambia ninguna fecha.
- **No hay baja** (decisión 7): por eso comprobar la inscripción y escribir después es seguro.
- **Quien escucha conserva su usuario:** `mastered` y `enrolled` leen la sesión al empezar; si luego
  entra otro usuario, quien ya escuchaba no se entera de lo suyo. La interfaz debe volver a escuchar
  al cambiar de sesión.
- **Cualquier otra implementación de los puertos** (Room, en la parte C) tiene que cumplir todo lo
  anterior; los comentarios de `ProgressStore` y `EnrollmentStore` lo dicen.

## Contrato ↔ DTO

Cómo pasa cada campo y cada respuesta de `POST /api/progress/sync` al modelo de la app.

*Pendiente (tarea 5).*

## Cómo extenderlo

- **Un tipo de ejercicio nuevo:**
  1. Antes de ofrecerlo, lee `enrolled(courseId)` de `EnrollmentStore` y decide con
     `studyAccess(enrolled)`. Si da `NotEnrolled`, no lo abras (la interfaz pone el mensaje).
  2. Al dominar el módulo, llama a `ProgressStore.markMastered(courseId, moduleId)`.
  3. **No captures** la `IllegalStateException` de `markMastered`: es el invariante y avisa de un fallo
     de programación (se llegó a dominar sin comprobar `studyAccess`). Taparla escondería el fallo.
  4. Nunca escribas lo dominado por otro camino que no sea `markMastered`.
- **Otra forma de guardar el progreso (otra base de datos, otro servidor):** implementa
  `ProgressStore` y `EnrollmentStore` cumpliendo los invariantes de arriba (por usuario, nada escrito
  si falla, primera fecha, sin sesión). *Pendiente: detalle de Room en la parte C.*

## Qué cuidar al mergear

- **`app/.../data/AppContainer.kt`, línea de `progressStore`:** debe ser
  `InMemoryProgressStore(sessionStore)`. Sin el parámetro no compila. Si `dev/joel` también toca
  `AppContainer`, conserva el `sessionStore` al resolver el conflicto (`sessionStore` está declarado
  después, pero es `lazy`: no importa el orden).
- **`ExerciseViewModel.markMastered` lanzará la excepción** al dominar un módulo hasta que la parte C
  añada el botón de inscribirse (C9/C10). Está aceptado y no se arregla capturándola. Hasta entonces,
  esta rama no sirve para enseñar la app.
- **`CourseViewModel` (`mastered`):** sin sesión ve el progreso vacío; la sesión se lee al empezar a
  escuchar.
