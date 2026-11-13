# AGENTS.md — Gradus

Instrucciones para el agente de IA (Claude Code). Se leen al principio de cada petición.
Si una petición contradice este fichero, pregunta antes de actuar.

## 1. El proyecto

Gradus es una app Android nativa para aprender por dominio (*mastery learning*). Cada curso es un
grafo de conceptos: un concepto se desbloquea cuando se dominan sus prerrequisitos. Incluye
repetición espaciada, notas con la cámara y, más adelante, un catálogo de cursos en un servidor.

Es el proyecto de la asignatura PAMN (ULPGC). Lo hacemos dos estudiantes que **estamos aprendiendo
Kotlin y Android**. Requisitos de la asignatura: Kotlin nativo, al menos una API del dispositivo
(cámara), base de datos local SQLite y algún tipo de backend.

## 2. Cómo trabajamos

Usamos programación en pareja con IA (AIXP) y documentos de tarea ligeros (ODD):

- **Nosotros dirigimos y decidimos; tú implementas.** Nada se aplica sin que lo entendamos.
- Ciclo de cada petición: **entender → proponer → esperar nuestro OK → implementar → comprobar → explicar**.
- **Pasos pequeños.** Una cosa cada vez. Si un cambio toca más de dos ficheros o más de unas
  100 líneas, propón primero un plan dividido en pasos y espera a que elijamos.
- **Si algo es ambiguo, haz una sola pregunta concreta** en vez de suponer.
- **Tareas pequeñas** (un componente, un arreglo): sin documentos, directamente.
- **Funcionalidades grandes** (varias sesiones, varias pantallas): crea o actualiza
  `docs/tareas/<funcionalidad>.md` con: objetivo, alcance, fuera de alcance, lista de tareas con
  criterios de aceptación, progreso y siguiente paso. Léelo antes de continuar una funcionalidad.

## 3. Modo aprendizaje

- Explica en español sencillo **qué** haces y **por qué**.
- La primera vez que aparezca un concepto nuevo de Kotlin o Compose (por ejemplo `remember`,
  `Modifier`, `data class`, corrutinas), explícalo en dos o tres frases con un ejemplo.
- Muestra solo el código que cambia, salvo que pidamos el fichero completo.
- Si decimos «lo hago yo» o «dame una pista», da pistas y no la solución.
- Menciona una alternativa solo cuando cambie de verdad la decisión.

## 4. Stack

- Kotlin 2.2, Android Gradle Plugin 9.4, Java 11. `minSdk` 24, `targetSdk` 37.
- Interfaz: Jetpack Compose + Material 3 (Compose BOM 2026.02.01). **Nada de vistas XML.**
- Previstas, cuando toque: Navigation Compose, ViewModel, Room (SQLite), WorkManager,
  CameraX, y Ktor para el servidor.
- **No añadas dependencias sin preguntar.** Si se aprueban, van en `gradle/libs.versions.toml`.
- Si no estás seguro de que una API o versión exista, dilo en vez de inventarla.

## 5. Estructura

Paquete raíz: `es.ulpgc.pamn.dominio` (no se renombra aunque la app se llame Gradus).

```
app/src/main/java/es/ulpgc/pamn/dominio/
├── MainActivity.kt
├── model/          Datos y reglas en Kotlin puro (sin imports de Android)
├── data/           Datos de ejemplo; más adelante, base de datos y red
└── ui/
    ├── theme/      Color.kt, Type.kt, Theme.kt
    ├── components/ Piezas reutilizables
    └── screens/    Una pantalla por fichero
app/src/test/       Tests de la lógica (JUnit)
docs/               Especificación y documentos de tareas
```

## 6. Convenciones de código

- **Nombres en inglés** (clases, funciones, variables), salvo términos técnicos estándar.
- Clases pequeñas: ningún archivo debería ser largo, la separación de responsabilidades es prioritaria, así como la reutilización del código.
- Patrones de diseño: es importante el uso de patrones de diseño, e informar de su uso. Asimismo, patrones de diseño como el builder, factory methods, así como tener un enfoque de programación funcional.
- Pantallas: `ScreenX` en `ui/screens/`. Componentes reutilizables en `ui/components/`.
- Todo composable acepta `modifier: Modifier = Modifier` y recibe datos y eventos por parámetro
  (*state hoisting*): las pantallas no deciden la navegación, avisan con lambdas como `onVolver`.
- Cada pantalla y componente tiene `@Preview` en claro y en oscuro.
- **Colores y textos de estilo siempre desde el tema:** `MaterialTheme.colorScheme` y
  `MaterialTheme.typography`. Nunca `Color(0x...)` dentro de una pantalla. Un color nuevo va en
  `Color.kt` con el nombre de su variable en Figma.
- **La lógica no va en la interfaz.** Validaciones, motor de dominio, grafo y repasos van en
  `model/` en Kotlin puro, para poder probarlos y reutilizarlos en el servidor.
- Accesibilidad: `contentDescription` en iconos con significado y zonas táctiles de 48 dp como mínimo.
- Textos visibles: de momento en el código. Cuando añadamos idiomas, pasarán a `strings.xml`.

## 7. Tests

- **Lógica en Kotlin puro: test primero.** Escribe el test que falla, después el código mínimo
  que lo hace pasar y luego limpia (rojo → verde → refactor). Van en `app/src/test/`.
- **Interfaz:** preview y prueba en el emulador. Indica siempre qué pasos seguir para comprobarlo.

## 8. Fuentes de verdad

- **Diseño:** Figma «Dominio · Pantallas principales»
  (https://www.figma.com/design/knk7ucKW8xHfpa4LcqacUO). Cuando trabajemos una pantalla,
  adjuntaremos una captura. Si el código y el diseño no coinciden, pregunta.
- **Especificación:** `docs/especificacion.md`. Si una petición la contradice, avísanos.

## 9. Lo que no debes hacer

- Hacer commits, push o cambiar de rama: eso lo hacemos nosotros.
- Borrar, mover o renombrar ficheros o paquetes sin permiso.
- Modificar ficheros de Gradle, `AndroidManifest.xml` o el tema sin que lo pidamos.
- Tocar `local.properties`, `.idea/` o carpetas `build/`.
- Escribir claves, contraseñas o tokens en el código.
- Cambiar código que no está relacionado con la petición, aunque se pueda mejorar: menciónalo y ya.

## 10. Cuándo algo está terminado

- Compila sin errores ni avisos nuevos.
- Las previews se ven bien en claro y en oscuro.
- Los tests pasan.
- Si es interfaz, lo hemos probado en el emulador.
- Está explicado y, si tiene documento de tarea, la tarea está marcada como hecha.

## 11. Formato de tus respuestas

1. **Qué voy a hacer**, en una a tres frases.
2. **El cambio**, solo el código que cambia, indicando el fichero.
3. **Por qué**, con los conceptos nuevos explicados.
4. **Cómo comprobarlo**, con pasos concretos.
5. **Siguiente paso sugerido**, sin empezarlo.