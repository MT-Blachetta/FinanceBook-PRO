#!/usr/bin/env bash
#
# Starts the FinanceBook Spring Boot backend.
#
#   1. Aborts if the app ports are already in use on the host:
#        8000 (backend) and 5173 (frontend dev server).
#   2. Makes sure Docker is running with the FinanceBook PostgreSQL container
#      on host port 5500 (scripts/start-db.sh).
#   3. Runs the backend with Maven. Extra arguments are passed to mvn, e.g.
#        ./scripts/start-backend.sh -Dspring-boot.run.profiles=dev
#
# Start the frontend (npm run dev) only after this script, otherwise its
# port 5173 is reported as in use.
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"

info "Checking that the backend and frontend ports are free ..."
require_ports_free "$BACKEND_PORT" "$FRONTEND_PORT"

"$SCRIPT_DIR/start-db.sh"

info "Starting backend on port $BACKEND_PORT ..."
cd "$ROOT_DIR/backend"
exec mvn spring-boot:run "$@"
