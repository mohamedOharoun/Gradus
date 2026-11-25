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

Las fechas son `kotlin.time.Instant` (estable, sin `@OptIn`), nunca texto.

*Pendiente (4b y siguientes).*

## Flujo

Inscribirse → estudiar → dominar → sincronizar.

*Pendiente.*

## Invariantes

Reglas que nunca se rompen (p. ej. no hay módulo dominado sin inscripción).

- `studyAccess` solo mira la inscripción; no recibe el estado del módulo (B-6). Combinarlo con
  `ModuleState` es cosa de la interfaz (C9).
- No existe un módulo dominado sin inscripción: lo aplica el almacén en el paso 4b (*pendiente*).

## Contrato ↔ DTO

Cómo pasa cada campo y cada respuesta de `POST /api/progress/sync` al modelo de la app.

*Pendiente (tarea 5).*

## Cómo extenderlo

- **Un tipo de ejercicio nuevo:** *pendiente.*
- **Otra forma de guardar el progreso (otra base de datos, otro servidor):** *pendiente.*

## Qué cuidar al mergear

*Pendiente.*
