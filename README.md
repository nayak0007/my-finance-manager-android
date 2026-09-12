# My Finance Manager (Android)

Native Android app for the My Finance Manager PRD. Backend is out of scope for this repo; the client stores data locally and is ready to call `/api/v1` once the Spring Boot service exists.

## What is included

Phase 1 (MVP)

- Email/password sign-up and login, plus a Google Sign-In path (local session until OAuth client IDs are wired)
- Dashboard: monthly income, expenses, investments, net savings, pie and trend charts, recent feed
- Dedicated Income, Expense, and Investment screens with add/edit/delete, filters, and search
- Profile, currency, notifications, CSV export, and account deletion

Phase 2 / 3 client features (local, backend-optional)

- Smart Import for CSV/TXT/PDF-text statements with review, duplicate detection, and commit
- SMS auto-capture with runtime permission rationale, inbox scan, review queue, allow/block senders
- Email capture toggle (OAuth/Gmail API will attach to the future backend)
- Insights tab with on-device observations and an advice disclaimer
- Optional category budgets with progress bars

## Architecture

- Kotlin, Jetpack Compose, Material 3, Navigation Compose
- MVVM (`AppViewModel`) with a simple `AppContainer` instead of Hilt
- Room for local persistence matching the PRD data model
- Retrofit client pointed at `https://api.myfinancemanager.local/` (`ApiConfig.BASE_URL`) for later backend work
- On-device SMS parsing; parsed items stay in a confirmation queue

## Project layout

```
app/src/main/java/com/myfinancemanager/app/
  data/local        Room entities, DAOs, database
  data/parser       SMS and statement parsing
  data/insights     On-device insight engine
  data/remote       Retrofit API stubs
  data/repository   Auth + finance repositories
  ui/screens        Compose screens
  sms               BroadcastReceiver + inbox scanner
```

## Build

Open the project in Android Studio Ladybug or newer (AGP 8.7, Kotlin 2.0, compileSdk 35, minSdk 26).

```
./gradlew assembleDebug
```

If `gradle-wrapper.jar` is missing, generate it from Android Studio (`File > Settings > Build > Gradle`) or:

```
gradle wrapper --gradle-version 8.9
```

## Connecting the future backend

1. Set `ApiConfig.BASE_URL` to the Spring Boot host.
2. Auth already tries remote login/signup and falls back to local accounts if the API is unreachable.
3. Replace local Room writes with API sync when endpoints are ready. Entities already match the PRD tables (`IncomeRecord`, `ExpenseRecord`, `InvestmentRecord`, `ImportBatch`, `AutoCaptureQueueItem`, `AIInsight`).
4. Plug Google Sign-In and Gmail OAuth client IDs in `AuthRepository.loginWithGoogle`.
5. Swap `InsightEngine` for the LLM-backed `/api/v1/insights` route when it exists; the Insights tab already degrades if generation is empty.

## Permissions

- `READ_SMS` / `RECEIVE_SMS`: optional, requested with in-app rationale, used only to parse transaction alerts into the review queue
- `POST_NOTIFICATIONS`: optional insight alerts
- `INTERNET`: future API calls

## Tests

```
./gradlew test
```

Parser, fingerprint, and insight-engine unit tests live in `app/src/test`.
