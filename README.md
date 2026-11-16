# HomeKeep

A quiet home asset register for Android: what you own, what it cost, its serial
number, when the warranty ends, what you had repaired, and what maintenance is
coming up. No budgets, no property management, no ceremony.

Built with Kotlin + Jetpack Compose, backed by Supabase (Postgres, Auth and
Storage) and shipped to Google Play as a native Android app.

## Features

- Asset register with category, brand, model, serial number, location, purchase
  date, price, retailer, warranty expiry and notes.
- Receipts, warranty PDFs, manuals and photos stored in a private Supabase
  Storage bucket, one folder per user.
- Repair history with vendor, cost, date and notes.
- Maintenance reminders with optional repeat interval, plus a daily WorkManager
  check that raises local notifications up to a week ahead.
- Email/password accounts, per-user Row Level Security on every table and every
  stored file, and in-app account deletion.
- Warm, single-purpose Material 3 UI sized for one-handed phone use.

Not included on purpose: budgets, valuation tracking, tenant/landlord features,
bank syncing, sharing or multi-user households.

## Stack

| Layer | Choice |
| --- | --- |
| App | Kotlin 2.2, Jetpack Compose (Material 3), minSdk 26, targetSdk 36 |
| HTTP | Ktor client (Android engine) + kotlinx.serialization |
| Backend | Supabase: Postgres, GoTrue auth, Storage |
| Background | WorkManager (daily reminder check) + local notifications |
| Build | Gradle 8.14, AGP 8.13, JDK 17 |
| CI | GitHub Actions (APK + AAB artifacts) |

## Layout

```
app/src/main/java/ca/nexacraft/homekeep/
  data/        Supabase REST/auth/storage client, models, repository
  notify/      WorkManager worker, scheduler and notification channel
  ui/          ViewModel, navigation root, screens, theme, components
  util/        date and money helpers
supabase/migrations/   full schema, RLS policies and storage bucket
docs/                  privacy policy, account deletion page (GitHub Pages)
.github/workflows/     Android build and Pages deploy
```

## 1. Create the Supabase backend

1. Create a project at <https://supabase.com/dashboard> (or reuse an existing
   one; HomeKeep only adds `hk_*` tables and a `homekeep` bucket).
2. Open **SQL Editor** and run the contents of
   [`supabase/migrations/20261116000000_homekeep.sql`](supabase/migrations/20261116000000_homekeep.sql).
   This creates the tables, indexes, triggers, RLS policies, the private
   `homekeep` storage bucket and the `hk_delete_account()` function.
3. Under **Authentication -> Providers -> Email**, enable email sign-ups. If you
   reuse a project that has sign-ups disabled (the vending app does), either turn
   them on for this project or create your user in **Authentication -> Users**.
4. Copy **Project URL** and the **anon/publishable key** from
   **Project Settings -> API**.

The anon key is designed to be public. Never put the `service_role` key in the
app.

## 2. Configure the app

Create `local.properties` in the repo root (it is git-ignored):

```properties
sdk.dir=C\:\\Users\\you\\AppData\\Local\\Android\\Sdk
SUPABASE_URL=https://YOURPROJECT.supabase.co
SUPABASE_ANON_KEY=YOUR_ANON_KEY
```

For CI, set the same values as GitHub Actions secrets. Signing keys are covered
in [PLAY_STORE.md](PLAY_STORE.md).

## 3. Build and run

Requirements: JDK 17, Android SDK platform 36 and build tools. Android Studio
will install these automatically.

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug        # with a phone or emulator attached
```

The APK lands in `app/build/outputs/apk/debug/`.

## 4. Publish to Google Play

Follow [PLAY_STORE.md](PLAY_STORE.md) end to end. Short version:

1. Run the SQL migration and configure the app.
2. Create an upload keystore, build a signed AAB with `bundleRelease`.
3. Create the Play Console listing, complete Data safety and content rating,
   publish to internal testing, install from the Play link on your Samsung
   phone, then promote to production.

## Data model

| Table | Purpose |
| --- | --- |
| `hk_assets` | One row per owned item |
| `hk_attachments` | Receipts, warranties, manuals, photos (Storage paths) |
| `hk_repairs` | Repair/service history |
| `hk_reminders` | Maintenance reminders, optional repeat |

All rows carry `user_id` defaulting to `auth.uid()`, and every policy is
`to authenticated using (user_id = auth.uid())`. Storage objects must live under
`<auth.uid()>/...`.

## CI

- `.github/workflows/android.yml` builds the debug APK and release AAB on every
  push and uploads them as artifacts. If keystore secrets are present the release
  bundle is signed; otherwise it is unsigned.
- `.github/workflows/pages.yml` publishes `docs/` (privacy policy and account
  deletion page) to GitHub Pages.

## Privacy

The policy lives in [docs/privacy.html](docs/privacy.html). It assumes the Pages
URL `https://nexacraftca.github.io/HomeKeep/privacy.html`; update it if you
deploy elsewhere.

## Known limits

- One account per person; no household sharing yet.
- Files are capped at 10 MB each.
- Editing requires a connection; data is fetched on demand (no offline cache).
- Notifications depend on Android's battery optimisation; the daily check can be
  delayed.
