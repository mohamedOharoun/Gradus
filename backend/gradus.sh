#!/usr/bin/env bash
# Arranca el backend de Gradus con un solo comando: base de datos, migraciones y servidor.
# Uso: ./gradus.sh [up|db|test|reset|help]   (sin argumentos = up)
set -euo pipefail

readonly BACKEND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly ENV_FILE="$BACKEND_DIR/.env"
readonly MIGRATIONS_DIR="$BACKEND_DIR/db/init"
readonly CONTAINER="gradus-postgres"
readonly REQUIRED_JAVA=25
# Nombre de las migraciones: 001_lo_que_hace.sql (tres cifras para que se ordenen bien).
readonly MIGRATION_NAME='^[0-9]{3}_[a-z0-9_]+\.sql$'

cd "$BACKEND_DIR"

info() { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31mError:\033[0m %s\n' "$*" >&2; exit 1; }

usage() {
    cat <<'EOF'
Uso: ./gradus.sh [orden]

  up      (por defecto) Base de datos + migraciones pendientes + servidor en primer plano.
  db      Solo base de datos + migraciones (para arrancar el servidor desde el IDE).
  test    Tests unitarios (mvn test). No necesita la base de datos.
  reset   Borra todos los datos locales y crea la base de datos de cero. Pide confirmación.
  help    Muestra esta ayuda.

La configuración sale de backend/.env (ver .env.example). Para probar con Yaak, deja el
servidor arrancado y envía las carpetas del workspace «Gradus · Backend».
EOF
}

require_command() {
    command -v "$1" >/dev/null 2>&1 || fail "No se encuentra '$1'. $2"
}

# Exporta las variables de .env sin mostrarlas: las usan docker compose y el servidor.
load_env() {
    [[ -f "$ENV_FILE" ]] || fail "Falta backend/.env. Créalo con 'cp .env.example .env' y pon tu usuario y contraseña."
    set -a
    # shellcheck source=/dev/null
    source "$ENV_FILE"
    set +a
}

# Maven compila con su propio Java (JAVA_HOME), que puede no ser el del PATH: se comprueba ese.
check_maven_and_java() {
    require_command mvn "Instala Maven."
    local version
    version=$(mvn -v | awk '/^Java version/ {print $3}' | tr -d ,)
    [[ "${version%%.*}" -ge $REQUIRED_JAVA ]] 2>/dev/null \
        || fail "Maven usa Java ${version:-desconocido}; hace falta Java $REQUIRED_JAVA o superior (revisa JAVA_HOME)."
}

start_database() {
    require_command docker "Instala Docker y arranca su servicio."
    docker compose version >/dev/null 2>&1 || fail "Falta el plugin 'docker compose' (Compose v2)."
    info "Arrancando PostgreSQL ($CONTAINER)…"
    # --wait espera a que el healthcheck diga que acepta conexiones.
    docker compose up -d --wait postgres
}

# psql dentro del contenedor, con las credenciales del propio contenedor: nunca se imprimen.
# client_min_messages=warning oculta avisos como «already exists, skipping»; los errores sí salen.
psql_in_container() {
    docker exec -i -e PGOPTIONS='-c client_min_messages=warning' "$CONTAINER" \
        sh -c 'psql -X -q -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" "$@"' psql "$@"
}

# Aplica, en orden, las migraciones que no figuran en schema_migrations. Cada una va en una
# transacción junto con su anotación: si falla, no queda ni a medias ni anotada.
migrate() {
    info "Buscando migraciones pendientes…"
    psql_in_container -c "CREATE TABLE IF NOT EXISTS schema_migrations (
        filename   TEXT        PRIMARY KEY,
        applied_at TIMESTAMPTZ NOT NULL DEFAULT now())"

    local applied file name pending=0
    applied=$(psql_in_container -At -c "SELECT filename FROM schema_migrations")
    for file in "$MIGRATIONS_DIR"/*.sql; do
        name=$(basename "$file")
        [[ "$name" =~ $MIGRATION_NAME ]] || fail "Nombre de migración no válido: $name (formato: 001_lo_que_hace.sql)."
        grep -qxF "$name" <<<"$applied" && continue

        info "  Aplicando $name"
        { cat "$file"; printf "\nINSERT INTO schema_migrations (filename) VALUES ('%s');\n" "$name"; } \
            | psql_in_container --single-transaction -f -
        pending=$((pending + 1))
    done

    if ((pending == 0)); then
        info "No hay migraciones pendientes."
    else
        info "$pending migración(es) aplicada(s)."
    fi
}

# Conecta el localhost:PUERTO del emulador o del móvil (USB) con el del ordenador, que es la URL
# que usa la app (AppContainer). Opcional: sin adb o sin dispositivo solo se avisa.
connect_device() {
    local port="${GRADUS_HTTP_PORT:-7070}"
    if command -v adb >/dev/null 2>&1 && adb reverse "tcp:$port" "tcp:$port" >/dev/null 2>&1; then
        info "App conectada: localhost:$port del dispositivo → este ordenador (adb reverse)."
    else
        info "Sin adb o sin dispositivo: para la app, ejecuta 'adb reverse tcp:$port tcp:$port' al abrir el emulador."
    fi
}

run_server() {
    check_maven_and_java
    info "Servidor en http://localhost:${GRADUS_HTTP_PORT:-7070} (Ctrl+C para pararlo)."
    exec mvn -q compile exec:java
}

reset_database() {
    printf 'Esto borra TODOS los datos de la base de datos local (volumen gradus-pgdata).\n'
    local answer
    read -r -p "Escribe 'borrar' para continuar: " answer
    [[ "$answer" == "borrar" ]] || fail "Cancelado: no se ha borrado nada."
    docker compose down -v
    start_database
    migrate
}

main() {
    case "${1:-up}" in
        up)             load_env; start_database; migrate; connect_device; run_server ;;
        db)             load_env; start_database; migrate; connect_device ;;
        test)           check_maven_and_java; mvn test ;;
        reset)          load_env; reset_database ;;
        help | -h | --help) usage ;;
        *)              usage; exit 1 ;;
    esac
}

main "$@"
