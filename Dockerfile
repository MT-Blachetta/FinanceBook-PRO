# PostgreSQL database for FinanceBook.
#
# The host already runs its own default PostgreSQL on port 5432, so FinanceBook
# uses this separate container. Inside the container PostgreSQL listens on its
# default port 5432, which is published on host port 5500:
#
#     host 127.0.0.1:5500  ->  container :5432
#
# Normally started via ./scripts/start-db.sh (or ./scripts/start-backend.sh).
# Manual equivalent:
#     docker build -t financebook-postgres:15 - < Dockerfile
#     docker run -d --name financebook-postgres --restart unless-stopped \
#         -p 127.0.0.1:5500:5432 -v financebook-pgdata:/var/lib/postgresql/data \
#         financebook-postgres:15

# Use the official PostgreSQL 15 image as the base
FROM postgres:15

# Credentials must match the backend defaults in backend/src/main/resources/application.yml.
# They are only applied when the data volume is initialised for the first time.
ENV POSTGRES_USER=yourself
ENV POSTGRES_PASSWORD=secretPassword
ENV POSTGRES_DB=financebook

# Persistent data directory (mount a named volume here)
VOLUME /var/lib/postgresql/data

# Container-internal PostgreSQL port; published on host port 5500
EXPOSE 5432

HEALTHCHECK --interval=10s --timeout=5s --retries=5 \
    CMD pg_isready -h 127.0.0.1 -p 5432 -U "$POSTGRES_USER" -d "$POSTGRES_DB" || exit 1
