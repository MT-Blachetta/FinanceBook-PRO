#!/usr/bin/env bash
#
# Shared settings and helpers for the FinanceBook start/stop scripts.
# This file is sourced, not executed.
#
# Port layout on the host:
#   5432  host's own default PostgreSQL instance  (NOT used by FinanceBook)
#   5500  FinanceBook PostgreSQL Docker container (-> 5432 inside the container)
#   8000  Spring Boot backend
#   5173  Vite frontend dev server
#

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

BACKEND_PORT=8000
FRONTEND_PORT=5173

DB_HOST_PORT=5500
DB_CONTAINER_PORT=5432
DB_BIND_ADDRESS="${DB_BIND_ADDRESS:-127.0.0.1}"
DB_IMAGE="${DB_IMAGE:-financebook-postgres:15}"
DB_CONTAINER="${DB_CONTAINER:-financebook-postgres}"
DB_VOLUME="${DB_VOLUME:-financebook-pgdata}"

info()  { echo "[financebook] $*"; }
error() { echo "[financebook] ERROR: $*" >&2; }

# Returns 0 if something on the host is listening on the given TCP port.
port_in_use() {
    local port=$1
    if command -v ss >/dev/null 2>&1; then
        ss -Hltn "sport = :$port" 2>/dev/null | grep -q .
    elif command -v lsof >/dev/null 2>&1; then
        lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1
    else
        (exec 3<>"/dev/tcp/127.0.0.1/$port") 2>/dev/null
    fi
}

# Prints whatever is listening on the given port (process info may need root).
describe_port() {
    local port=$1
    if command -v ss >/dev/null 2>&1; then
        ss -Hltnp "sport = :$port" 2>/dev/null | sed 's/^/    /'
    elif command -v lsof >/dev/null 2>&1; then
        lsof -nP -iTCP:"$port" -sTCP:LISTEN 2>/dev/null | sed 's/^/    /'
    fi
}

# Aborts if any of the given ports is already in use on the host.
require_ports_free() {
    local port busy=0
    for port in "$@"; do
        if port_in_use "$port"; then
            error "Port $port is already in use on this host:"
            describe_port "$port" >&2
            busy=1
        else
            info "Port $port is free."
        fi
    done
    if [ "$busy" -ne 0 ]; then
        error "Stop the process(es) above or free the port(s), then try again."
        exit 1
    fi
}

# Aborts unless the docker CLI is installed and the daemon is reachable.
require_docker() {
    if ! command -v docker >/dev/null 2>&1; then
        error "Docker is not installed. FinanceBook's PostgreSQL runs in a Docker container."
        error "Install Docker: https://docs.docker.com/engine/install/"
        exit 1
    fi
    local out
    if ! out=$(docker info 2>&1); then
        if echo "$out" | grep -qi "permission denied"; then
            error "No permission to talk to the Docker daemon."
            error "Add your user to the 'docker' group (sudo usermod -aG docker \$USER) and log in again."
        else
            error "The Docker daemon is not running. Start it, e.g.: sudo systemctl start docker"
        fi
        exit 1
    fi
}
