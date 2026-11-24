# Especificación — Gradus

Borrador v0.1 · noviembre de 2026.

Este documento dice **qué** hace la app. El **cómo** trabajamos está en `../AGENTS.md` y cada
funcionalidad grande tiene su documento en `docs/tareas/`. Si algo cambia, se actualiza aquí.

## 1. Qué es Gradus

Una app Android para aprender por dominio. Los instructores publican cursos estructurados como
grafos de módulos; los estudiantes los descargan y los estudian sin conexión. Un módulo se
desbloquea cuando se dominan sus prerrequisitos, y los repasos espaciados evitan olvidar lo dominado.

## 2. Usuarios

- **Estudiante:** busca cursos, se inscribe, estudia respondiendo preguntas y repasa.
- **Instructor:** crea cursos (normalmente con ayuda de un LLM) y los sube.
- Una misma cuenta puede hacer las dos cosas.

## 3. Glosario

- **Curso:** conjunto de módulos, guardado como JSON (formato en §6).
- **Módulo:** unidad de aprendizaje; es un nodo del grafo. Tiene una parte formativa (lecciones y
  ejercicios intercalados) y una reserva de ejercicios de repaso.
- **Prerrequisito:** una arista A → B significa «hay que dominar A antes de B».
- **Prueba de dominio:** ejercicios sorteados de la reserva de un módulo; superarla lo da por dominado.
- **Estados de un módulo:**
  - **dominado:** el estudiante ya lo sabe.
  - **frontera:** desbloqueado pero sin dominar; es lo que toca estudiar.
  - **bloqueado:** le faltan prerrequisitos.
- **Repaso:** ejercicio de la reserva de un módulo ya dominado, programado para no olvidarlo.
- **Etiqueta:** palabra clave de un curso (por ejemplo, `#grafos`) para buscar y filtrar.

## 4. Reglas del motor de aprendizaje

1. El grafo de un curso no puede tener ciclos. Un curso con ciclos no se importa.
2. Un módulo se desbloquea cuando **todos** sus prerrequisitos están dominados.
3. Un módulo está dominado cuando se supera su **prueba de dominio**: se sortean `questions`
   ejercicios de su reserva y hay que acertar al menos `minCorrect`. Los valores los fija el curso
   (con un valor por defecto y, si hace falta, uno propio por módulo); ver §2.6 y §5 de
   `backend/docs/especificacion-cursos-v1.md`. El modelo va detrás de una interfaz para poder
   cambiarlo más adelante (por ejemplo, por BKT).
4. Cada respuesta se guarda como una fila nueva (pregunta, acierto, fecha) y nunca se modifica.
   El estado de dominio se calcula a partir de ese registro.
   *(Hoy, mientras no exista Room: el progreso se guarda en memoria, solo qué módulos se han
   dominado, y se pierde al cerrar la app. Ver `tareas/cursos-backend-app.md`.)*
5. Si se falla el repaso de un módulo dominado, se marca como «necesita repaso», pero **no** se
   vuelven a bloquear los módulos que dependen de él. *(Decisión provisional.)*
6. Las tarjetas autoevaluadas (cuando existan, ver §7) cuentan para los repasos, pero no para
   desbloquear módulos.
7. Los repasos se programan con SM-2.

## 5. Pantallas

El diseño de referencia es el Figma «Dominio · Pantallas principales».

**Sin sesión:**

- **Bienvenida:** grafo de ejemplo, «Crear cuenta», «Iniciar sesión» y «Explorar cursos sin cuenta».
- **Registro en tres pasos:**
  - **Nombre:** al menos 2 caracteres.
  - **Correo:** con formato válido.
  - **Contraseña:** mínimo 8 caracteres, con letra y número y con un carácter especial. Hay que
    escribirla dos veces y aceptar los términos.
  - Indicador de pasos con puntos y líneas.
  - Los datos ya añadidos aparecen como chips editables.
  - «Siguiente» en los pasos 1 y 2; deslizador «Desliza para registrarte» en el 3.
- **Inicio de sesión:** correo y contraseña, «¿Has olvidado tu contraseña?» y enlace a crear cuenta.

**Con sesión** (barra inferior: Inicio, Buscar, Mis cursos, Ajustes):

- **Inicio:** saludo, repasos de hoy, curso en progreso y cursos inscritos con su porcentaje.
- **Buscar:**
  - Búsqueda por título o descripción mientras se escribe, con el número de resultados.
  - Filtros en una hoja inferior: etiquetas (varias), creador y fecha de publicación
    (cualquiera, último mes, último año).
  - Orden fijo: los más recientes primero. «Mejor valorados» llegará cuando haya valoraciones;
    no hay orden por relevancia.
  - Los filtros activos se ven como chips que se pueden quitar.
- **Mis cursos:**
  - Cursos **creados** por el usuario, cada uno con su estado: publicado, con avisos o borrador.
  - «Subir curso» es la acción principal; la guía para crear cursos con IA es un enlace secundario.
- **Ajustes:**
  - Perfil (nombre y correo) y cambio de contraseña.
  - Modo oscuro e idioma.
  - Etiquetas preferidas: ordenan antes los cursos con esas etiquetas.
  - Cerrar sesión y eliminar cuenta.

**Dentro de un curso** (sin barra inferior; diseñadas en Compose, el Figma no las tiene; ver
`tareas/camino-curso-y-ejercicios.md` y `tareas/cursos-backend-app.md`):

- **Camino del curso:**
  - El curso se descarga del servidor al abrirlo. Mientras, una rueda; si falla, el motivo
    (no existe, sin conexión, formato más nuevo que la app o fallo del servidor) y, si sirve,
    «Reintentar».
  - Los módulos, de arriba abajo, unidos por líneas a sus prerrequisitos. Si una fila solo
    tiene un módulo, el camino hace zigzag; como mucho caben 3 por fila.
  - Cada módulo tiene color e icono según su estado: dominado (morado, ✓), frontera (naranja, ▶)
    y bloqueado (gris, 🔒). Las líneas hacia un módulo desbloqueado son llenas y moradas; hacia
    uno bloqueado, discontinuas y grises.
  - Cabecera con el título y el progreso («3 de 10 dominados»).
  - Tocar un módulo bloqueado dice qué falta para desbloquearlo. Tocar uno de la frontera o
    dominado abre su **hoja**: su título, su estado, «Estudiar» (o «Repasar» si ya está dominado) y
    «Mis notas · N». Al dominar un módulo o guardar una nota, el camino se actualiza solo.
- **Estudiar un módulo:**
  - **Parte formativa:** sus pasos en orden, con una barra de progreso («Paso 2 de 5»).
    - Lección: texto en Markdown básico (negrita, cursiva, código, párrafos y listas) y «Continuar».
    - Ejercicio: «Comprobar»; después, el resultado (con la solución si se falla), la
      **explicación** del autor y «Continuar». Fallar aquí no impide seguir.
  - **Prueba de dominio:** una pantalla dice cuántas preguntas hay y cuántas hay que acertar
    («Empezar la prueba» o «Ahora no»). Las preguntas se sortean de la reserva y se responden igual
    que los ejercicios.
  - **Resultado:** «¡Módulo dominado!» (y se desbloquea lo que dependía de él) o «Te faltó 1 para
    aprobar», con «Repetir la prueba» (otro sorteo, sin repetir las lecciones) y «Volver al camino».
  - Un módulo sin reserva termina tras sus pasos, sin prueba.
  - Tipos de ejercicio: opción única, selección múltiple y emparejar (§7).

- **Notas con cámara** (`tareas/notas-camara.md`): fotos de los apuntes de papel, guardadas junto al
  módulo al que pertenecen.
  - «Mis notas» de un módulo: las fotos en cuadrícula, la más reciente primero, con su fecha
    («24 nov»). «Hacer foto» y «De la galería» (el selector de fotos de Android, sin permisos).
  - Hacer foto: la cámara dentro de la app (CameraX). El permiso se pide al pulsar; si se deniega,
    se explica para qué hace falta y, si ya no se puede volver a pedir, se ofrece abrir los ajustes.
    Después se revisa la foto: «Repetir» o «Guardar», con un pie opcional de hasta 100 caracteres.
  - Ver una nota: a pantalla completa, con zoom (pellizcar o doble toque), su fecha y su pie.
    «Eliminar» pide confirmación; no hay papelera.
  - Las fotos se quedan en el móvil (no se suben al servidor) y son del móvil, no de la cuenta.
    TalkBack lee cada foto con su fecha y su pie.

**Pendiente de diseñar:** repasos.

## 6. Formato de los cursos

- La definición completa está en `backend/docs/especificacion-cursos-v1.md` (semántica) y en
  `backend/src/main/resources/schemas/course-v1.schema.json` (estructura).
- JSON con un campo `formatVersion` desde la primera versión (hoy, `1`).
- Contiene módulos (id, título, prerrequisitos `requires` y prueba de dominio opcional). Cada módulo
  tiene una parte formativa (`steps`: lecciones y ejercicios) y una reserva (`review`: solo
  ejercicios). Los textos largos admiten Markdown.
- El **validador** es la única fuente de verdad del formato. Comprueba, en este orden:
  1. Sintaxis: UTF-8 y JSON válidos, sin claves repetidas.
  2. Versión del formato y estructura (el schema).
  3. Identidad y referencias: ids únicos, prerrequisitos y opciones que existen.
  4. Grafo: sin ciclos.
  5. Dominio y pedagogía: pruebas de dominio realizables y difíciles de aprobar adivinando.
- **Errores** (bloquean la publicación): por ejemplo, un ciclo o un prerrequisito inexistente.
- **Avisos** (se publica igualmente): por ejemplo, aristas redundantes o una reserva demasiado
  pequeña para reintentar la prueba sin repetir preguntas.
- Los mensajes del validador deben ser claros, para poder pegarlos en un LLM y que corrija el curso.
- Una guía en Markdown, con un curso de ejemplo, explica el formato a los instructores y a sus LLM.

## 7. Tipos de pregunta (primera versión)

Formato v1:

- **Opción única** (`singleChoice`), **selección múltiple** (`multipleChoice`) y **unir parejas**
  (`matching`). En la app, unir parejas se hace tocando uno de cada lado (aparece una flecha entre
  los dos), sin arrastrar; solo es correcta si están bien todas las parejas. Ver
  `tareas/camino-curso-y-ejercicios.md`.

Previstos para versiones posteriores del formato:

- **Respuesta corta:** se normaliza quitando espacios, ignorando mayúsculas y unificando Unicode,
  y se compara con una lista de respuestas aceptadas.
- **Numérica:** con tolerancia; acepta tanto «0,5» como «0.5».
- **Ordenar**, incluido Parsons: las líneas se ordenan tocándolas en orden, sin arrastrar.
- **Tarjeta autoevaluada:** para los repasos.

## 8. Requisitos de la asignatura

| Requisito | Cómo lo cumple Gradus |
|---|---|
| Kotlin nativo | App en Kotlin con Jetpack Compose; la lógica, en Kotlin puro |
| API del dispositivo | Cámara (CameraX, dentro de la app) para fotografiar apuntes y asociarlos a módulos |
| SQLite | Room (`gradus.db`): hoy, las notas con cámara (las fotos como ficheros; en la base de datos, su ruta, curso, módulo, fecha y pie). Previsto: cursos descargados y registro de respuestas |
| Backend | Anillo 2: servidor Javalin (Java) con PostgreSQL: cuentas, catálogo, subida validada, descarga y valoraciones. La app lo llama con Ktor Client desde `sharedLogic` |

## 9. Plan por anillos

1. **Núcleo sin conexión** (compromiso):
   - Registro e inicio de sesión (solo interfaz).
   - Motor de aprendizaje y vista del grafo.
   - Práctica de preguntas.
   - Notas con cámara.
   - Recordatorios.
   - Importar cursos desde un fichero.
2. **Backend mínimo** (objetivo): cuentas reales, catálogo y búsqueda, subida, descarga y valoraciones.
3. **Chat:** fuera de alcance.

## 10. Decisiones abiertas

- ¿Los cursos llevan imágenes (paquete `.zip`) o solo JSON?
- ¿El fallo en un repaso se queda en «necesita repaso» o vuelve a bloquear lo que depende de él?
- ~~¿Cuentas reales con nuestro servidor o con Firebase Authentication?~~ Resuelto: con nuestro
  servidor Javalin (ver `backend/docs/tareas/registro-usuario.md` e `inicio-sesion.md`).
- ~~¿Las etiquetas son una lista cerrada o libres, normalizadas a minúsculas?~~ Resuelto: **libres**.
  Cada curso lleva de 1 a 10, de 1 a 32 caracteres, y se guardan y se muestran tal como las escribió
  el autor. Solo para comparar (repetidas y búsqueda) se normalizan: Unicode NFC, espacios recortados
  y agrupados, y minúsculas. Dos etiquetas que coinciden así dan el aviso `DUPLICATE_TAG`, y filtrar
  por «Grafos» encuentra los cursos con «grafos» (ver `backend/docs/especificacion-cursos-v1.md`
  §2.2 y §3.4).

## 11. Fuera de alcance

- Chat entre estudiantes e instructores.
- Ejecutar código enviado por instructores dentro de la app.
- Versión para iOS, de momento. Es una posibilidad futura: la lógica vive en el módulo
  multiplataforma `sharedLogic` (Kotlin Multiplatform) para poder llevarla a iOS si decidimos hacerla.
