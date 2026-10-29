#!/usr/bin/env bash
#
# Ensures FinanceBook's PostgreSQL is running in Docker.
#
# The host already runs its own default PostgreSQL on port 5432, so FinanceBook
# uses a separate PostgreSQL 15 container (built from the project's Dockerfile)
# whose port 5432 is published on host port 5500:
#
#     host 127.0.0.1:5500  ->  container financebook-postgres:5432
#
# Steps:
#   1. Check that Docker is installed and the daemon is running.
#   2. Build the image from ./Dockerfile if it does not exist yet.
#   3. Create the container (first run) or start it (later runs).
#   4. Wait until PostgreSQL accepts connections.
#
# Data is kept in the named volume 'financebook-pgdata' and survives
# container restarts and re-creation.
#
# Optional environment overrides:
#   DB_BIND_ADDRESS  host address port 5500 is bound to (default 127.0.0.1;
#                    use 0.0.0.0 to allow connections from other machines)
#   DB_IMAGE, DB_CONTAINER, DB_VOLUME
#
set -euo pipefail

# shellcheck source=common.sh
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

require_docker

if ! docker image inspect "$DB_IMAGE" >/dev/null 2>&1; then
    info "Building Docker image $DB_IMAGE from $ROOT_DIR/Dockerfile ..."
    # The Dockerfile copies no files, so no build context is sent.
    docker build -t "$DB_IMAGE" - < "$ROOT_DIR/Dockerfile"
fi

state=$(docker inspect -f '{{.State.Status}}' "$DB_CONTAINER" 2>/dev/null) || state=missing

if [ "$state" = "running" ]; then
    info "Container $DB_CONTAINER is already running."
elif [ "$state" = "paused" ]; then
    info "Unpausing container $DB_CONTAINER ..."
    docker unpause "$DB_CONTAINER" >/dev/null
elif port_in_use "$DB_HOST_PORT"; then
    # Port 5500 is taken. Accept it only if another container already publishes
    # it (e.g. a FinanceBook database container created by hand under another name).
    other=$(docker ps --filter "publish=$DB_HOST_PORT" --format '{{.Names}}' | head -n 1)
    if [ -z "$other" ]; then
        error "Host port $DB_HOST_PORT is already in use by a non-Docker process:"
        describe_port "$DB_HOST_PORT" >&2
        error "FinanceBook's database container needs port $DB_HOST_PORT. Free it and try again."
        exit 1
    fi
    info "Host port $DB_HOST_PORT is already published by running container '$other' - using it."
    DB_CONTAINER=$other
elif [ "$state" = "missing" ]; then
    info "Creating container $DB_CONTAINER ($DB_BIND_ADDRESS:$DB_HOST_PORT -> $DB_CONTAINER_PORT) ..."
    docker run -d \
        --name "$DB_CONTAINER" \
        --restart unless-stopped \
        -p "$DB_BIND_ADDRESS:$DB_HOST_PORT:$DB_CONTAINER_PORT" \
        -v "$DB_VOLUME:/var/lib/postgresql/data" \
        "$DB_IMAGE" >/dev/null
else
    info "Starting container $DB_CONTAINER (state: $state) ..."
    docker start "$DB_CONTAINER" >/dev/null
fi

if ! docker port "$DB_CONTAINER" "$DB_CONTAINER_PORT/tcp" 2>/dev/null | grep -q ":$DB_HOST_PORT\$"; then
    error "Container $DB_CONTAINER does not publish its port $DB_CONTAINER_PORT on host port $DB_HOST_PORT."
    error "Remove it (docker rm -f $DB_CONTAINER) and run this script again; the data volume is kept."
    exit 1
fi

info "Waiting for PostgreSQL in $DB_CONTAINER to accept connections ..."
# Check over TCP: during first-time initialisation the image runs a temporary
# server that listens on the Unix socket only, which must not count as ready.
for _ in $(seq 1 60); do
    if docker exec "$DB_CONTAINER" pg_isready -q -h 127.0.0.1 -p "$DB_CONTAINER_PORT"; then
        info "PostgreSQL is ready on localhost:$DB_HOST_PORT (container $DB_CONTAINER)."
        exit 0
    fi
    sleep 1
done

error "PostgreSQL in $DB_CONTAINER did not become ready within 60 seconds."
error "Check the container logs: docker logs $DB_CONTAINER"
exit 1
