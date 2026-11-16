# Publishing HomeKeep to Google Play

This is the checklist for taking the app from this repo to a production listing
that installs on your Samsung phone. Steps 1-2 must be done before the build is
useful.

## 0. Prerequisites

- Google Play developer account (one-time US$25, identity verification required):
  <https://play.google.com/console/signup>
- A Samsung test device signed into a Google account.
- JDK 17 and Android SDK 36 (Android Studio handles both).

## 1. Backend

Run `supabase/migrations/20261116000000_homekeep.sql` in the Supabase SQL editor
and put `SUPABASE_URL` + `SUPABASE_ANON_KEY` in `local.properties`. See the
README. Test sign-up and sign-in in the debug build before publishing.

## 2. Create the upload keystore

Play App Signing keeps the real signing key; you only need an *upload* key.

```powershell
keytool -genkeypair -v `
  -keystore homekeep-upload.jks `
  -alias homekeep `
  -keyalg RSA -keysize 4096 -validity 10000
```

Store the file and passwords somewhere safe (password manager, not the repo).
Then either:

- For local builds, add to `local.properties`:

  ```properties
  HOMEKEEP_KEYSTORE_PATH=C\:\\path\\to\\homekeep-upload.jks
  HOMEKEEP_KEYSTORE_PASSWORD=...
  HOMEKEEP_KEY_ALIAS=homekeep
  HOMEKEEP_KEY_PASSWORD=...
  ```

- For GitHub Actions, add repository secrets:
  `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `HOMEKEEP_KEYSTORE_BASE64`
  (`[Convert]::ToBase64String([IO.File]::ReadAllBytes("homekeep-upload.jks"))`),
  `HOMEKEEP_KEYSTORE_PASSWORD`, `HOMEKEEP_KEY_ALIAS`, `HOMEKEEP_KEY_PASSWORD`.

Build the bundle:

```powershell
.\gradlew.bat clean bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`. Check it is signed
(`jarsigner -verify -verbose -certs app-release.aab`); an unsigned bundle cannot
be uploaded.

## 3. Create the Play Console app

1. **All apps -> Create app**: name `HomeKeep`, default language English,
   App, Free.
2. **Dashboard -> Set up your app** and fill every section:
   - App access: all functionality is available with an email account created in
     the app. Provide a test account (email + password) in the instructions so
     reviewers can sign in. Do not leave this blank or they will reject it.
   - Ads: no.
   - Content rating questionnaire: Utility/Productivity, no violence, no
     user-generated public content.
   - Target audience: 18+ (or 13+; the app has no child-directed features).
   - News app: no. COVID-19 apps: no. Data safety: see step 5.
   - Government app: no. Financial features: none (no payments, no financial
     info).
3. **Store listing**:
   - App name: `HomeKeep - Home Asset Register` (max 30 chars).
   - Short description (max 80): `Receipts, serials, warranties, repairs and upkeep reminders.`
   - Full description: use the copy in the appendix below.
   - App icon: `docs/store/icon-512.png` (512x512 PNG).
   - Feature graphic: `docs/store/feature-graphic-1024x500.png` (1024x500 PNG).
   - Phone screenshots: at least 2 (up to 8), taken on the Samsung device, at
     least 1080px on the long edge. Suggested: asset list, asset detail with
     warranty badge, add-asset form, reminders section.
   - Support email and, optionally, a website (the GitHub Pages site).

## 4. Testing tracks (required order for new personal accounts)

1. **Internal testing**: create a release, upload the AAB, add your own email as
   a tester. Play generates an opt-in link. Install on the Samsung phone and run
   through: sign up, add asset, upload receipt, add repair, add reminder, sign
   out, sign back in.
2. **Closed testing**: Google requires new *personal* developer accounts to run a
   closed test with **12 testers for 14 continuous days** before production is
   unlocked. Add 12 Google accounts as testers, ship the same build, and have
   them opt in and keep the app installed.
3. **Production**: once the closed test completes and the app is approved, create
   a production release, promote the tested bundle (do not rebuild with a
   different key), and roll out to 100%.

## 5. Data safety answers

Answer truthfully for what the app does:

- Collects **Email address** (account management, app functionality) and
  **User content** such as photos and files (app functionality).
- Data is **encrypted in transit** (HTTPS).
- Users can **request deletion**; also offer in-app deletion. Our
  `hk_delete_account` RPC removes all user rows, storage objects and the auth
  user.
- No data is shared with third parties for advertising; Supabase is a service
  provider/processor.
- No location, contacts, SMS, health or financial data collected.
- Data is not used for tracking.

Provide the account deletion URL:
`https://nexacraftca.github.io/HomeKeep/account-deletion.html` (or your own
domain) and the privacy policy URL:
`https://nexacraftca.github.io/HomeKeep/privacy.html`. Both are generated from
`docs/` by the Pages workflow. Enable **Settings -> Pages -> Build and deployment
-> GitHub Actions** once.

## 6. Release requirements already handled

- Target SDK 36 (required for new apps from August 2026).
- Android App Bundle (.aab), not APK.
- No native libraries, so 16 KB page-size compliance is automatic.
- Package `ca.nexacraft.homekeep`, versionCode 1, versionName 1.0.0. Increment
  `versionCode` for every upload.
- No broad permissions: only internet and notifications.
- Account deletion available in-app (Settings -> Delete my account and data).

## 7. Updating later

1. Bump `versionCode`/`versionName` in `app/build.gradle.kts`.
2. Build a signed AAB (CI or locally).
3. Create a new release in Play Console, roll out to internal testing first.

## Appendix: store description copy

Short:

> Receipts, serials, warranties, repairs and upkeep reminders.

Full:

> HomeKeep is a calm, no-nonsense register of the things in your home.
>
> Keep the details that are painful to find later: serial numbers, receipts,
> warranty end dates, the shop you bought it from, and where it lives in the
> house. Log repairs and services with cost and vendor. Add maintenance
> reminders for filters, servicing and warranty checks, and get a notification
> when they are due.
>
> No budgets. No property management. No ads. Your records are stored in a
> private Supabase database, protected per account, and can be deleted from
> inside the app at any time.
>
> HomeKeep is ideal for appliances, electronics, furniture, HVAC equipment,
> tools and other household purchases you want to keep track of.
