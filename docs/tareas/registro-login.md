# Registro e inicio de sesión

## Objetivo

Que una persona sin cuenta pueda ver la bienvenida, registrarse en tres pasos o iniciar sesión,
y llegar a una pantalla de inicio provisional. Solo interfaz y validación: todavía sin servidor.

## Por qué

Es la puerta de entrada de la app y la primera funcionalidad completa del anillo 1. Nos sirve
para aprender Compose, el estado, la navegación y los tests con algo pequeño y visible.

## Diseño

Figma, pantallas «1 · Bienvenida» y «1a, 1b, 1c · Registro». El inicio de sesión no está en Figma:
sigue el mismo estilo que el registro (barra superior, título serif, campos, botón principal).

## Alcance

- Pantalla de bienvenida con el grafo dibujado.
- Registro en tres pasos: nombre, correo, contraseña (con repetición y requisitos).
- Inicio de sesión con correo y contraseña.
- Navegación entre las pantallas y una pantalla de inicio provisional con un saludo.
- Validación de los campos en Kotlin puro, con tests.

## Fuera de alcance

- Crear cuentas de verdad o comprobar contraseñas en un servidor.
- Recordar la sesión al cerrar la app.
- Recuperar la contraseña.
- Traducción a otros idiomas.

## Tareas

Cada tarea se hace en una rama propia y se fusiona cuando cumple sus criterios.

- [x] **T1 · Tema del Figma.** Colores de las variables de Figma en `Color.kt` y conectados al
  `colorScheme`; fuentes Source Serif 4 y Source Sans 3 en `res/font` y `Type.kt`; tema renombrado
  a `GradusTheme`.
  *Criterios:* una preview de prueba se ve con fondo lavanda, botón morado y tarjeta blanca; en
  oscuro cambia sin tocar la preview; los títulos salen en serif.
- [x] **T2 · Navegación.** Navigation Compose con cuatro pantallas vacías (solo un título):
  Bienvenida, Registro, Inicio de sesión e Inicio.
  *Criterios:* se puede ir y volver entre ellas; tras «entrar», el botón atrás no vuelve al registro.
- [x] **T3 · Bienvenida (sin grafo).** Logotipo, título, texto y los tres botones. Sigue el modo
  claro u oscuro del móvil (decisión nuestra, distinta del Figma); la ilustración tiene siempre fondo noche.
  *Criterios:* coincide con el Figma en oscuro y usa los colores del tema en claro; los iconos de la
  barra de estado se leen en los dos modos; los botones navegan a su pantalla.
- [x] **T4 · Ilustración de la bienvenida.** Escalera de conceptos dibujada con `Canvas`
  (`WelcomeIllustration`), con fondo noche en los dos modos. En el Figma no hay leyenda de estados.
  *Criterios:* se ve como en el Figma y se encoge en móviles pequeños sin cortarse.
- [x] **T5 · Componentes de formulario.** `FormField` (etiqueta encima, error debajo),
  `PasswordField` (con ojo para mostrar u ocultar) y `PrimaryButton`. Iconos de Material Symbols en `res/drawable`.
  *Criterios:* previews de cada estado: vacío, con texto, con error y contraseña oculta y visible.
- [x] **T6 · Validación con tests.** `model/Validation.kt` (con `FieldError` y `PasswordRequirement`): nombre, correo, requisitos de
  contraseña (8 caracteres, letra y número, carácter especial) y que coincidan. Test primero.
  *Criterios:* todos los tests de `ValidationTest` pasan; no importa nada de Android.
- [ ] **T7 · Inicio de sesión.** Pantalla con correo, contraseña y «Entrar».
  *Criterios:* los errores salen solo tras pulsar «Entrar»; con datos válidos llega a Inicio.
- [ ] **T8 · Registro, pasos 1 y 2.** Estado del paso, campos de nombre y correo, botón
  «Siguiente», ilustración y chips de datos añadidos.
  *Criterios:* no avanza con datos no válidos; al girar el móvil no se pierde nada; atrás del
  sistema retrocede un paso.
- [ ] **T9 · Indicador de pasos.** Puntos unidos por líneas, con ✓ en los pasos hechos.
  *Criterios:* se ve como en el Figma en los tres pasos.
- [ ] **T10 · Registro, paso 3.** Contraseña, repetición, requisitos que se marcan al escribir y
  casilla de términos.
  *Criterios:* cada requisito se marca en cuanto se cumple; avisa si no coinciden.
- [ ] **T11 · Deslizador.** «Desliza para registrarte», bloqueado hasta que todo sea válido.
  *Criterios:* si se suelta antes del final vuelve al principio; con TalkBack se activa con doble
  toque; al completarlo llega a Inicio.

## Progreso

- T1 implementada: colores de Figma en `Color.kt` y conectados al `colorScheme` (Figma no tiene
  modo oscuro; se construye con noche, lila y sobre-oscuro), fuentes en `res/font` y `Type.kt`,
  tema renombrado a `GradusTheme` y muestrario en `ui/theme/ThemePreview.kt`.
  Revisada y hecha.
- T2 implementada: Navigation Compose 2.10.2 con rutas `@Serializable` (`ui/navigation/Routes.kt`),
  `GradusNavHost` y cuatro pantallas provisionales sobre `PlaceholderScreen`. `MainActivity`
  arranca en Bienvenida. Probada y hecha.

- T3 y T4 hechas: `ScreenWelcome` con `BrandLogo` y `WelcomeIllustration`; sigue el modo del móvil.

- T5 implementada: `FormField`, `PasswordField`, `PrimaryButton` e iconos. Revisada y hecha.

- T6 hecha: 26 tests de `ValidationTest` en verde; `model/` sin imports de Android.

## Siguiente paso

T7 · Inicio de sesión.
