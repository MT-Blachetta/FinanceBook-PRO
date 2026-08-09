# FinanceBook (Java Version)

Professional personal finance management application featuring a robust Spring Boot backend, a responsive React web frontend, and a native Android app.

## Overview

FinanceBook is a comprehensive tool for tracking expenses, incomes, and managing financial categories. It supports multi-user environments with strict data isolation, transaction fee calculations, and detailed statistical analysis.

## Architecture

### Backend (`backend/`)
-   **Framework**: Spring Boot 3.2.2
-   **Language**: Java 17
-   **Database**: PostgreSQL 15+
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

### 3. Database Setup (PostgreSQL)

Ensure PostgreSQL 15+ is installed and running.
```bash
# Ubuntu/Debian
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql

# Create the database
sudo -u postgres psql -c "CREATE DATABASE financebook;"
sudo -u postgres psql -c "CREATE USER yourself WITH PASSWORD 'secretPassword';"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE financebook TO yourself;"
```

---

## Configuration

### Backend Configuration
The backend is configured via `src/main/resources/application.yml`. Key environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `DATABASE_URL` | PostgreSQL Connection URL | `jdbc:postgresql://localhost:5500/financebook` |
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

Follow these steps to compile and run the application locally.

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
    Start the Spring Boot server.
    ```bash
    mvn spring-boot:run
    ```
    The backend will start on port **8000**.
    *On first run, it will automatically create the database schema and a default admin user.*

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

**Example execution:**
```bash
export DATABASE_URL=jdbc:postgresql://localhost:5500/financebook_prod
export DATABASE_USERNAME=prod_user
export DATABASE_PASSWORD=prod_pass
java -jar financebook-backend-0.1.0.jar
```

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
A: The backend is not running or crashed. Check the terminal output where you ran `mvn spring-boot:run`.
