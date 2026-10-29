# FinanceBook (Java Version)

Professional personal finance management application featuring a robust Spring Boot backend, a responsive React web frontend, and a native Android app.

## Overview

FinanceBook is a comprehensive tool for tracking expenses, incomes, and managing financial categories. It supports multi-user environments with strict data isolation, transaction fee calculations, and detailed statistical analysis.

## Architecture

### Backend (`backend/`)
-   **Framework**: Spring Boot 3.2.2
-   **Language**: Java 17
-   **Database**: PostgreSQL 15 in a Docker container (host port 5500, see [Database Setup](#3-database-setup-postgresql-in-docker))
-   **ORM**: Hibernate / Spring Data JPA
-   **Security**: Spring Security with JWT Authentication
-   **Build Tool**: Maven
-   **Tests**: JUnit 5 + Mockito + MockMvc against an in-memory H2 database (`mvn test`, 97 tests)

### Frontend (`frontend/`)
-   **Framework**: React 18
-   **Build Tool**: Vite
-   **Styling**: Styled Components (responsive down to small phones: navigation drawer, wrapping filter rows, fluid cards and forms)
-   **State/Data Fetching**: React Query, Axios
-   **Routing**: React Router

### Android app (`android/`)
-   **Language/UI**: Kotlin + Jetpack Compose (Material 3, same dark finance theme)
-   **Architecture**: MVVM (ViewModel + StateFlow) with a single repository over Retrofit/OkHttp
-   **Feature parity** with the web app: login, payment summary with filters, add/edit payments, categories, category types, statistics
-   See `android/README.md` for build instructions (requires Android Studio / Android SDK)

### REST API highlights
-   `POST /auth/login`, `POST /auth/register`, `GET/PUT /auth/me`
-   `GET/POST/PUT/DELETE /payment-items` with `expenseOnly`, `incomeOnly`, `categoryIds` (descendants included), `dateFrom`, `dateTo`
-   `GET/POST/PUT/DELETE /categories`, `GET /categories/{id}/descendants`
-   `GET/POST/DELETE /category-types`
-   `GET/POST/PUT/DELETE /recipients`
-   `GET /statistics` — totals, monthly buckets, per-category buckets (optional date range)
-   `POST /import-csv` — tolerant CSV import with per-row error reporting
-   Interactive documentation at `http://localhost:8000/docs` (Swagger UI)

---

## Detailed Installation Guide

This guide provides step-by-step instructions to set up the development environment using **SDKMAN!** (for Java/Maven) and **nvm** (for Node.js).

### 1. Java & Maven Setup (via SDKMAN!)

We recommend using **SDKMAN!** to manage your Java and Maven versions easily.

#### Step 1: Install SDKMAN!
Open your terminal and run:
```bash
curl -s "https://get.sdkman.io" | bash
```
Follow the on-screen instructions. Then restart your terminal or run:
```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
```

#### Step 2: Install Java 17
Install OpenJDK 17 (LTS) using SDKMAN:
```bash
sdk install java 17.0.10-tem
```
*Note: You can list available versions with `sdk list java`.*

#### Step 3: Install Maven
Install the latest Maven version:
```bash
sdk install maven
```

#### Step 4: Verify Installation
```bash
java -version
mvn -version
```

### 2. Node.js & npm Setup (via nvm)

We recommend using **nvm** (Node Version Manager) to manage Node.js versions and avoid permission issues.

#### Step 1: Install nvm
Run the install script:
```bash
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash
```
Restart your terminal or run:
```bash
export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"
```

#### Step 2: Install Node.js v20 (LTS)
Install and use the latest LTS version of Node.js (v20):
```bash
nvm install 20
nvm use 20
```

#### Step 3: Verify Installation
```bash
node -v  # Should be v20.x.x
npm -v   # Should be 10.x.x
```

### 3. Database Setup (PostgreSQL in Docker)

The host already runs its own default PostgreSQL instance on port **5432**. FinanceBook does
**not** use it and does **not** need a PostgreSQL installed on the host. Instead, its database runs
in a separate PostgreSQL 15 Docker container, built from the project's `Dockerfile`. The
container's internal port 5432 is published on host port **5500**:

```
host 127.0.0.1:5432  ->  host's default PostgreSQL        (not used by FinanceBook)
host 127.0.0.1:5500  ->  container financebook-postgres:5432  (FinanceBook database)
```

#### Step 1: Install Docker
Install Docker Engine (https://docs.docker.com/engine/install/) and allow your user to use it:
```bash
sudo usermod -aG docker $USER   # log out and back in afterwards
sudo systemctl enable --now docker
```

#### Step 2: Start the database container
```bash
./scripts/start-db.sh
```
The script:
1. checks that Docker is installed and the daemon is running,
2. builds the image `financebook-postgres:15` from the `Dockerfile` (first run only),
3. creates the container `financebook-postgres` with `-p 127.0.0.1:5500:5432` (first run) or starts the existing one,
4. waits until PostgreSQL accepts connections.

Database `financebook`, user `yourself` and password `secretPassword` are created automatically
on first start (set in the `Dockerfile`, matching the backend defaults). Data is stored in the Docker
volume `financebook-pgdata`, so it survives restarts and re-creating the container. The container
uses `--restart unless-stopped`, so it comes back with the Docker daemon after a reboot.

Stop the database with `./scripts/stop-db.sh`. The host's PostgreSQL on 5432 is never touched.

Optional environment overrides for the scripts: `DB_BIND_ADDRESS` (default `127.0.0.1`; set
`0.0.0.0` to reach the database from other machines), `DB_CONTAINER`, `DB_IMAGE`, `DB_VOLUME`.

<details>
<summary>Manual equivalent without the script</summary>

```bash
docker build -t financebook-postgres:15 - < Dockerfile
docker run -d --name financebook-postgres --restart unless-stopped \
    -p 127.0.0.1:5500:5432 -v financebook-pgdata:/var/lib/postgresql/data \
    financebook-postgres:15

# Connect with psql (note: port 5500, not 5432)
psql -h localhost -p 5500 -U yourself financebook
```
</details>

### Ports overview

| Port | Used by |
|------|---------|
| 5432 | Host's own default PostgreSQL (not used by FinanceBook) |
| 5500 | FinanceBook PostgreSQL Docker container (→ 5432 inside the container) |
| 8000 | Spring Boot backend |
| 5173 | Frontend dev server (Vite, `strictPort` — fails instead of switching ports) |
| 8080 | Python admin panel (`backend/adminpanel`) |

---

## Configuration

### Backend Configuration
The backend is configured via `src/main/resources/application.yml`. Key environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `DATABASE_URL` | PostgreSQL Connection URL (the Docker container on host port 5500 — not the host's 5432) | `jdbc:postgresql://localhost:5500/financebook` |
| `DATABASE_USERNAME` | Database User | `yourself` |
| `DATABASE_PASSWORD` | Database Password | `secretPassword` |
| `JWT_SECRET_KEY` | Secret for signing tokens | *(dev default provided)* |
| `ADMIN_DEFAULT_PASSWORD` | Initial password for 'admin' user (only used when the admin user is first created — an existing admin's password is never overwritten on restart) | `admin` |

### Frontend Configuration
The frontend uses a Vite proxy to communicate with the backend.
-   **Proxy Target**: `http://localhost:8000` (Defined in `vite.config.ts`)
-   **Port**: 5173 (Default)

---

## Compiling, Installing, and Running

Follow these steps to compile and run the application locally. Start order:
**database container → backend → frontend**.

### Backend (Spring Boot)

1.  **Navigate to the backend directory:**
    ```bash
    cd backend
    ```

2.  **Clean and Install Dependencies:**
    This compiles the Java code and downloads all Maven dependencies.
    ```bash
    mvn clean install -DskipTests
    ```

    To run the automated test suite (no database required — uses in-memory H2):
    ```bash
    mvn test
    ```

3.  **Run the Application:**
    From the repository root, start the backend with the start script:
    ```bash
    ./scripts/start-backend.sh
    # extra arguments go to Maven, e.g.:
    ./scripts/start-backend.sh -Dspring-boot.run.profiles=dev
    ```
    Before the backend starts, the script
    1. checks that ports **8000** (backend) and **5173** (frontend) are not already in use on the
       host, and aborts with the process holding the port if one is,
    2. ensures Docker is running with the PostgreSQL container on port **5500**
       (`scripts/start-db.sh`, see [Database Setup](#3-database-setup-postgresql-in-docker)),
    3. runs `mvn spring-boot:run` in `backend/`.

    The backend will start on port **8000**.
    *On first run, it will automatically create the database schema and a default admin user.*

    Start the frontend only **after** the backend; otherwise its port 5173 is already taken and
    the backend start script aborts.

    *Running `mvn spring-boot:run` directly also works, but then the port checks are skipped and
    the database container must already be running (`./scripts/start-db.sh`).*

### Frontend (React + Vite)

1.  **Navigate to the frontend directory:**
    Open a new terminal tab/window.
    ```bash
    cd frontend
    ```

2.  **Install Dependencies:**
    Downloads all node modules specified in `package.json`.
    ```bash
    npm install
    ```

3.  **Start Development Server:**
    Starts the Vite development server with hot-reload.
    ```bash
    npm run dev
    ```
    The frontend will be accessible at **http://localhost:5173**.

### Admin Panel (Python, port 8080)

User management (create users, edit profiles, reset passwords, grant admin rights,
activate/deactivate accounts) and the per-user transaction fee configuration live in the admin
panel. Start it after the backend:
```bash
cd backend/adminpanel
./run_admin.sh
```
Open **http://localhost:8080/admin/login** and log in with an admin account (default `admin` / `admin`).

### Restarting after an update

```bash
# 1. Stop running instances with Ctrl+C in their terminals (frontend, admin panel, backend)

# 2. Database container: keeps running; start it if it is stopped (data is kept in the volume)
./scripts/start-db.sh

# 3. Backend: applies pending database migrations (Flyway) automatically on startup
./scripts/start-backend.sh

# 4. Admin panel (new terminal)
cd backend/adminpanel && ./run_admin.sh

# 5. Frontend (new terminal)
cd frontend && npm run dev
```
A full restart of the database container is only needed after changing the `Dockerfile`:
`./scripts/stop-db.sh && docker rm financebook-postgres && ./scripts/start-db.sh`
(data stays in the `financebook-pgdata` volume).

---

## Deployment (Production)

### 1. Build Backend
Package the Spring Boot application into a JAR file.

```bash
cd backend
mvn clean package -DskipTests
```
The executable JAR will be located at `target/financebook-backend-0.1.0.jar`.

### 2. Build Frontend
Build the static assets for the frontend.

```bash
cd frontend
npm run build
```
The static files will be generated in the `dist` directory.

### 3. Running in Production
You can run the JAR file directly. For a complete deployment, you should serve the frontend static files using a web server like Nginx, using it as a reverse proxy to forward API requests to the Java backend.

The database container must be running before the backend starts (`./scripts/start-db.sh`), and
port 8000 must be free.

**Example execution:**
```bash
./scripts/start-db.sh
export DATABASE_URL=jdbc:postgresql://localhost:5500/financebook_prod
export DATABASE_USERNAME=prod_user
export DATABASE_PASSWORD=prod_pass
java -jar financebook-backend-0.1.0.jar
```
The container only creates the database `financebook` and the user `yourself`. Create a different
database or user first, e.g.
`docker exec -it financebook-postgres psql -U yourself -d financebook -c "CREATE DATABASE financebook_prod;"`.

---

## User Manual

### Login
-   **URL**: `http://localhost:5173/login`
-   **Default Admin Credentials**:
    -   Username: `admin`
    -   Password: `admin` (or valid configured password)

### Dashboard (Summary)
The home page displays a list of all payment items sorted by date.
-   **Filter**: Use the filters to view Expenses, Incomes, or fees.
-   **Categories**: Filter by specific categories using the dropdown.

### Adding Payments
Click the **"ADD"** button in the navigation bar.
-   **Description**: Name of the transaction.
-   **Amount**: Value (positive for income, negative for expense).
-   **Date**: Date of transaction.
-   **Category**: Assign a category for classification.
-   **Recipient**: (Optional) Who received/paid the money.

### Managing Categories
Navigate to the "Categories" section (if available in menu) or manage them on the fly when adding payments. Categories are hierarchical.

### Statistics
Click the **"STATISTICS"** link to view visual breakdowns of your finances:
-   **Balance History**: Line chart of running balance.
-   **Income/Expense**: Pie charts by category.

---

## Troubleshooting

**Q: "Login failed" error?**
A: Ensure the backend is running on port 8000. Check the browser console network tab. If using the default admin, ensure the database initialized correctly.

**Q: Categories not showing icons?**
A: Ensure the `icons` directory is correctly pointed to in `application.yml` and contains the SVG files.

**Q: Connection refused to localhost:8000?**
A: The backend is not running or crashed. Check the terminal output where you ran `./scripts/start-backend.sh`.

**Q: "Port 8000 / 5173 is already in use"?**
A: `./scripts/start-backend.sh` refuses to start when another process listens on one of these
ports. It prints the process holding the port (`ss -ltnp 'sport = :8000'` shows it too). Stop that
process, e.g. an old backend or frontend instance, and run the script again.

**Q: "Docker is not installed" / "The Docker daemon is not running"?**
A: The database runs in Docker. Install Docker or start the daemon (`sudo systemctl start docker`).
On "permission denied", add your user to the `docker` group and log in again.

**Q: "Host port 5500 is already in use by a non-Docker process"?**
A: Something other than a Docker container occupies port 5500. Free it; FinanceBook's database
container needs that port.

**Q: Password authentication failed / database "financebook" does not exist?**
A: Make sure you connect to port **5500** (the Docker container), not 5432 (the host's own
PostgreSQL). Check that the container is running: `docker ps --filter name=financebook-postgres`.
