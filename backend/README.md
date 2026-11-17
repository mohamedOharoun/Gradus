# Gradus · Backend

Servidor en Java 25 con Javalin y PostgreSQL en Docker.

## Requisitos

Docker con Compose v2, Maven y Java 25 (el que use Maven: revisa `JAVA_HOME`).

## Arrancar

```bash
cp .env.example .env   # solo la primera vez; pon tu usuario y contraseña
./gradus.sh            # base de datos + migraciones pendientes + servidor en http://localhost:7070
```

| Orden | Qué hace |
|---|---|
| `./gradus.sh` o `./gradus.sh up` | Arranca PostgreSQL, aplica las migraciones pendientes y el servidor (Ctrl+C para pararlo) |
| `./gradus.sh db` | Solo PostgreSQL y migraciones, para arrancar el servidor desde el IDE |
| `./gradus.sh test` | Tests unitarios (`mvn test`) |
| `./gradus.sh reset` | Borra todos los datos locales y crea la base de datos de cero (pide confirmación) |

## Probar

Con el servidor arrancado, envía las carpetas del workspace de Yaak (`yaak/`), o desde la terminal:
`yaak send <id>`. Las peticiones de «Sesiones · Sesión en uso» reutilizan el token de
«Inicio de sesión válido (201)»: envía antes esa.

## Migraciones

- Van en `db/init/` con el nombre `NNN_lo_que_hace.sql` (por ejemplo `004_add_avatar.sql`).
- `gradus.sh` las aplica en orden, cada una en una transacción, y anota las aplicadas en la tabla
  `schema_migrations`: cada una se ejecuta una sola vez.
- **No se modifica una migración ya aplicada**: para cambiar algo, se crea una nueva.
