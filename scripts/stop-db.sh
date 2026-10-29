#!/usr/bin/env bash
#
# Stops FinanceBook's PostgreSQL Docker container. The data stays in the
# 'financebook-pgdata' volume. The host's own PostgreSQL on port 5432 is not touched.
#
set -euo pipefail

# shellcheck source=common.sh
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

require_docker

if [ "$(docker inspect -f '{{.State.Running}}' "$DB_CONTAINER" 2>/dev/null)" = "true" ]; then
    docker stop "$DB_CONTAINER" >/dev/null
    info "Stopped container $DB_CONTAINER."
else
    info "Container $DB_CONTAINER is not running."
fi
