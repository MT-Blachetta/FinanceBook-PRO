# FinanceBook Android App

Native Android client for FinanceBook, mirroring the React web frontend
against the same Spring Boot REST API.

## Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose (Material 3, dark finance theme matching the web app)
- **Architecture**: MVVM — `ViewModel` + `StateFlow` per screen, one repository
- **Networking**: Retrofit + OkHttp + kotlinx.serialization (snake_case JSON,
  same contract as the web app)
- **Auth**: JWT stored in Preferences DataStore; attached by an OkHttp
  interceptor; automatic logout on 401
- **Navigation**: Navigation Compose

## Screens (feature parity with the web frontend)

| Screen | Mirrors web page | Features |
|---|---|---|
| Login | `/login` | Username/password, error handling |
| Summary | `/` | Filter chips All/Expenses/Incomes/Fees, category filter chips (descendants included server-side), payment cards with recipient/description/periodic/fee, SUM row, delete with confirmation |
| Add/Edit Payment | `/add`, `/payment/:id/edit` | Amount with expense/income toggle, date picker, description, periodic flag, recipient picker with inline creation, one-category-per-type selection |
| Categories | `/categories` | List with type/parent, create (type + optional parent), delete |
| Category Types | `/category-types` | List, create, delete (server-guarded) |
| Statistics | `/statistics` | Totals (income/expenses/balance/fees), monthly bar chart, per-category breakdown — powered by the `GET /statistics` endpoint |

## Building

Requires Android Studio (Koala or newer) or an Android SDK installation
(compileSdk 35). This repository's CI/development container has no Android
SDK, so the project is built on a developer machine:

```bash
cd android
./gradlew assembleDebug        # or open in Android Studio
./gradlew test                 # JVM unit tests (JSON contract tests)
```

## Backend URL

By default the app talks to `http://10.0.2.2:8000/` — that is the host
machine's `localhost:8000` as seen from the Android emulator, where the
Spring Boot backend runs.

For a physical device on the same network, point the app at your machine's
LAN address:

```bash
./gradlew assembleDebug -PapiBaseUrl="http://192.168.1.10:8000/"
```

Cleartext HTTP is only permitted for `10.0.2.2`/`localhost` (see
`app/src/main/res/xml/network_security_config.xml`); production deployments
should use HTTPS.
