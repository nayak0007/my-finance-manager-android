# My Finance Manager (Android)

Native Android app for the My Finance Manager PRD, backed by the Spring Boot service at
`https://my-finance-manager-backend-ah25.onrender.com`.

Every feature is backed by the account: income, expenses, investments, the SMS review queue,
imported statements, category budgets, capture and notification settings, and AI insights all
live on the backend and are mirrored into Room for offline reading. Room is a cache, not a
second copy — nothing here is device-only.

## What is included

Phase 1 (MVP)

- Email/password sign-up and login against Neon Auth, with a short-lived JWT refreshed transparently on 401 (see [Authentication](#authentication))
- Two-way sync of income, expenses, investments, budgets, the SMS review queue and statement imports (see [Sync model](#sync-model))
- "Continue with Google" is a placeholder: it reports that web sign-in is not wired up rather than creating a local-only session. Google is enabled on the Neon Auth instance, so enabling it means a browser round trip plus a redirect back into the app.
- Dashboard: monthly income, expenses, investments, net savings, pie and trend charts, recent feed
- Dedicated Income, Expense, and Investment screens with add/edit/delete, filters, and search
- Profile, currency, notifications, and account deletion; CSV export is built from the account's own export endpoint

Phase 2 / 3 client features (parsing stays on-device; the files and queue are mirrored)

- Smart Import for CSV/TXT/PDF-text statements with review, duplicate detection, and commit; the statement itself is uploaded to the backend for storage and parsing
- SMS auto-capture with runtime permission rationale, inbox scan, review queue, allow/block senders; queue items are mirrored and reviews are applied by the backend
- Email capture toggle (stored with the capture settings in the account)
- Insights tab: the backend generates suggestions from the account's records, and saved/dismissed state is stored there too
- Optional category budgets with progress bars

### Authentication

Credentials belong to **Neon Auth** (Managed Better Auth), not to this app and not to the
backend. Sign-up and sign-in are REST calls to the instance's `/<database>/auth` base URL, and
the session lives in a cookie carried by a dedicated `NeonAuthCookieJar`. The app then calls
`GET /token` for a short-lived **Ed25519**-signed JWT and sends that as the bearer token to the
finance API, which verifies it against the instance's JWKS.

The token from sign-up/sign-in is an opaque session token, *not* the JWT — so the JWT always
comes from `/token`. `SessionStore` keeps it alongside the backend's own user id, and
`TokenAuthenticator` mints a new one on a 401. The instance URL is `BuildConfig.NEON_AUTH_URL`,
set in `app/build.gradle.kts` next to `API_BASE_URL`, and sign-up sends
`Origin: http://localhost` because Better Auth rejects a sign-up without one and the instance's
trusted-origin list allows localhost. Turn `allow_localhost` off in the Neon Console and that
constant has to point at a domain you own.

Two consequences worth knowing:

- Sign-up and sign-in need a connection and cannot fall back to a local session.
- Account deletion in the app removes the finance data but **cannot** remove the Neon Auth
  identity: the managed service exposes no delete-user route on its public API, so the UI says
  so and points at the Neon Console.

## Architecture

- Kotlin, Jetpack Compose, Material 3, Navigation Compose
- MVVM (`AppViewModel`) with a simple `AppContainer` instead of Hilt
- Room caches the account's data so the UI works offline; `SyncEngine` reconciles it with the backend
- Retrofit client pointed at the deployed API via `BuildConfig.API_BASE_URL`
- On-device SMS parsing; parsed items stay in a confirmation queue

## Project layout

```
app/src/main/java/com/myfinancemanager/app/
  data/local        Room entities, DAOs, database, private statement store
  data/parser       SMS and statement parsing
  data/remote       Retrofit API definition, DTOs and error parsing
  data/sync         SyncEngine: two-way reconciliation with the backend
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

## Sync model

`data/sync/SyncEngine.kt` runs one reconciliation pass per signed-in user, under a single
mutex so overlapping triggers collapse into one run. Each pass is ordered:

1. **Tombstones** — push deletes first. A record removed locally while offline has to be
   removed server-side before the pull, or the pull would resurrect it.
2. **Push** — send every row flagged `dirty`. Rows with no `remoteId` are created, rows with
   one are updated. The backend does **not** de-duplicate on create, so the stored `remoteId`
   is what keeps a record from being pushed twice.
3. **Push queue and imports** — mirror pending SMS captures, replay review decisions, and
   upload imported statements (see below).
4. **Pull** — page through the server collections and merge by `remoteId`, so a record that was
   just pushed is updated in place rather than duplicated. Record pulls run after the queue push
   so a capture confirmed in this pass has its transaction pulled in the same pass.

Conflicts resolve last-write-wins with **local priority**: a row still marked `dirty` after
the push means the server rejected it, so the pull leaves the local edit alone instead of
discarding it. Rows the server rejects with a 4xx are parked locally and stop retrying.

Sync triggers are: app start / sign-in, a debounced 1.5s after any edit, and the
**Sync now** button in Settings.

### Review queue

Captured SMS items are mirrored to `/api/v1/auto-capture` as **pending** items, so the queue
survives a reinstall and can be reviewed from anywhere. The **review outcome is owned by the
backend**: confirming calls `POST /api/v1/auto-capture/{id}/confirm`, the server writes the
income/expense/investment record, and that record arrives through the ordinary record pull.
The app deliberately does not create the transaction locally as well, because the backend does
not de-duplicate on write and doing both would count the same payment twice.

That has two visible consequences:

- Confirming needs no network at the moment you tap it — the decision is stored on the item and
  replayed on the next sync — but the transaction appears in the ledger only after that sync.
- A capture with no detected amount cannot be confirmed, since the backend rejects a
  transaction without a positive amount; the app says so instead of queueing a doomed request.

Rejects are replayed too (`POST /{id}/reject`), which is what keeps the server's history
complete. Status changes flow back down on the next pull.

### Statement imports

The local import still parses and commits on the device, then uploads the statement file to
`POST /api/v1/imports` (multipart) so the source document is stored in your account. The server
re-parses the upload into its own staged transactions; those are **never committed from here**,
because the app has already pushed the records it created and committing again would duplicate
every one of them. The batch list is pulled for the server-side history.

The picked file is copied into private storage (`files/statements/<userId>`) first, so an upload
that fails while offline is retried on a later sync instead of being lost when the content Uri
goes away.

### Budgets

One limit per category, pushed with `PUT /api/v1/budgets/{category}` and pulled with
`GET /api/v1/budgets`. The category is in the URL because it is the account's key, so the local
row id is derived from (user, category) and a push and a pull land on the same row.

### Settings

The capture switches and the sender allow/block lists are one document on the account
(`/api/v1/auto-capture/settings`), and notifications, insight frequency and currency ride on the
profile (`PUT /api/v1/users/me`). Each travels in **one direction per pass** — pushed while the
local copy is flagged dirty, pulled when it is not — so two devices cannot overwrite each
other's unsent edits. The DataStore copy is a cache that makes the UI instant; the account is
the authority on every pull. `preferences` is replaced wholesale server-side, so this app sends
the map complete and owns the single `insightFrequencyDays` key in it.

### Insights

The backend generates insights (`POST /api/v1/insights/generate`) from the records it holds, so
"Refresh insights" needs a connection; if the server has no OpenRouter key configured it answers
`503` and that message is shown verbatim. Saving or dismissing an insight is pushed as a status
change (`PATCH /api/v1/insights/{id}/status`), and the list is pulled so the same suggestions
appear on any device. A refresh also deletes the superseded suggestions it replaced from the
account — anything still unsaved — so the tab does not become an archive of stale advice.

### What is device-only

Nothing in a user's account is device-only. Two things intentionally stay local because they are
about this phone rather than the data: the private statement copies used to retry uploads, and
the "invite seen" onboarding flag. Reading SMS is inherently device-side — the parsed result is
mirrored immediately.

### Required API contract

The client depends on these endpoints. The Neon Auth rows are served by the auth instance; every other row is served by the deployed backend.

| Method | Path | Used for |
| --- | --- | --- |
| POST | `<auth>/sign-up/email`, `/sign-in/email`, `/sign-out` | Neon Auth session |
| GET | `<auth>/token` | the bearer JWT sent to the backend |
| DELETE | `/api/v1/users/me` | delete the finance data (not the identity) |
| GET / POST / PUT / DELETE | `/api/v1/incomes`, `/expenses`, `/investments` | record sync |
| GET / POST | `/api/v1/auto-capture` | mirror captures, review them |
| POST | `/api/v1/auto-capture/{id}/confirm`, `/{id}/reject` | the server writes and closes the transaction |
| GET / POST (multipart) | `/api/v1/imports` | upload and list statement batches |
| GET / PUT / DELETE | `/api/v1/budgets`, `/api/v1/budgets/{category}` | budget sync |
| GET / PUT | `/api/v1/auto-capture/settings` | capture switches and sender lists |
| GET / POST / PATCH / DELETE | `/api/v1/insights`, `/generate`, `/{id}/status` | insights and their saved/dismissed state |
| GET | `/api/v1/users/me/export` | CSV export source |

Two validation rules the UI mirrors: sign-up requires a password of **at least 8
characters** (Neon Auth enforces it, and answers `PASSWORD_TOO_SHORT`), and the backend rejects
any request whose bearer token is missing, expired or signed by an unknown key.

## Permissions

- `READ_SMS` / `RECEIVE_SMS`: optional, requested with in-app rationale, used only to parse transaction alerts into the review queue
- `POST_NOTIFICATIONS`: optional insight alerts
- `INTERNET`: Neon Auth, the backend API and their sign-in round trips

## Tests

```
./gradlew test
```

Parser and fingerprint unit tests live in `app/src/test`, alongside `RemoteApiTest`, which pins
the JSON contract with the backend (field names, date formats, omitted nulls) for every synced
resource, and `NeonAuthApiTest`, which pins the Neon Auth wire contract against payloads
captured from the live service — request bodies, the error shape, and that the JWT comes only
from `/token`. The backend's own suite covers JWKS verification, user provisioning and the
budgets API end to end.
