# Histórico — Progreso de los alumnos (backend, parte A)

Lo terminado de `progreso-backend.md`, para entender una decisión pasada. **No hace falta leerlo para
continuar**: lo vigente está en `progreso-backend.md` y en el contrato `api-progreso.md`.

## Tarea 1 — Dominio y caso de uso `SyncProgress` (hecha el 2026-11-24)

Commits: `add: progress domain records, validation and merge (progress task 1a)` y
`add: SyncProgress use case with its ports (progress task 1b)`. 653 tests en verde al cerrarla.

### Decisiones con los usuarios

- Contrato: UUID inválido → `skipped` `COURSE_NOT_FOUND` (inscripción entera); `moduleId` vacío o
  solo espacios → `skipped` `REQUIRED` (solo ese módulo, `skipped` lleva `moduleId` opcional); falta
  un campo o tipo equivocado → 400; fechas futuras recortadas a la hora del servidor antes de fusionar;
  repetidos en la misma petición se fusionan (reglas 4 y 6–9).
- Orden de `skipped` relajado: primero lo omitido al validar, después los cursos inexistentes, cada
  grupo en orden de petición (así `validate` no necesita saber qué cursos existen).
- Un curso inexistente sale en `skipped` con su UUID canónico (minúsculas) y una sola vez aunque se
  repita; lo omitido al validar conserva el texto tal como llegó.
- Comprobar que un curso existe: puerto propio `CourseExistence` (segregación de interfaces), no un
  método nuevo en `CourseRepository` (`findLatest` carga hasta 2 MiB).

### 1a — Dominio (`domain/progress/`)

- Records `Enrollment`, `MasteredModule` (no admite `moduleId` vacío), `EnrollmentDraft` y
  `MasteredModuleDraft` (entrada en bruto, sin validar), `SkippedEntry(String courseId,
  Optional<String> moduleId, ErrorCode code)` con fábricas `enrollment(...)` y `module(...)`,
  `ValidatedProgress(valid, skipped)`.
- Funciones puras: `ProgressValidation.validate(drafts, now)` y `ProgressMerge.merge(stored, incoming)`
  (unión, gana la fecha más antigua, quita repetidos; orden de primera aparición).
- Fuera del plan, aceptado: solo vale el UUID canónico de 36 caracteres (`UUID.fromString` admite
  `1-1-1-1-1`); mayúsculas válidas. Un `moduleId` válido no se recorta.
- En la revisión se pidieron tests de los invariantes de los *drafts* y de `ValidatedProgress`.

### 1b — Caso de uso (`application/progress/`)

- Puertos `EnrollmentRepository` (`mergeAll`, `findAll`) y `CourseExistence` (`findExisting`);
  `SyncResult(enrollments, skipped)`; `SyncProgress(repository, courses, clock)`.
- `sync`: validar → `merge(List.of(), valid)` → `findExisting` (una vez, solo si queda algo válido) →
  `mergeAll` (solo si queda algo que existe) → `findAll` (siempre). No hace leer → fusionar → guardar.
- Tests con `InMemoryEnrollmentRepository`, `FakeCourseExistence` (anotan las llamadas) y
  `ProgressTestData` (*Object Mother*). `AdjustableClock` pasó a `public` (con permiso) para probar
  que `sync` lee el reloj en cada llamada.
