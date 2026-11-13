# Ticket: Post-Workshop Firebase & GCP Security Hardening (`sounny-com`)

- **Date filed:** 2026-11-13
- **Status:** open (deferred until after 2026-11-13 afternoon SpaceCamp workshop)
- **Project / Scope:** `sounny-com` Firebase Realtime Database & Browser API Key (`src/features/spacecamp/SpaceCampFirebase.js`, `database.rules.json`)

## Verified Baseline (2026-11-12 Audit)

- `generativelanguage.googleapis.com` returns `403 PERMISSION_DENIED` on the browser API key (`AIzaSyDOvv...`), so Gemini quota cannot be billed through it.
- Realtime Database root `/.json`, `/spacecamp/secrets.json`, and `/spacecamp/proofs.json` return `401 Unauthorized`, so existing player callsigns/scores cannot be hijacked.
- Default Firestore (`404`), Cloud Storage object listing (`404`), and Identity Toolkit signup (`CONFIGURATION_NOT_FOUND`) are closed.

## Post-Workshop Action Checklist

- [ ] **1. Lock Website (HTTP Referrer) Restrictions on Browser API Key**
  In Google Cloud Console (`sounny-com` → APIs & Services → Credentials → Browser key `AIzaSyDOvv...`), set Application restrictions to **Websites** allowing:
  - `https://jsmars.sounny.com/*`
  - `https://jsmars-sounny.web.app/*`
  - `https://sounny-com.firebaseapp.com/*`
  - `http://localhost:*` and `http://127.0.0.1:*`

- [ ] **2. Restrict API Key Scope**
  Under **API restrictions** on the same key, allow only the required Firebase client APIs (Firebase Installations API / Token Service API / Firebase Realtime Database) so future enabled GCP APIs on `sounny-com` are never automatically reachable with this public key.

- [ ] **3. Verify Billing Plan & Budget Alert**
  Confirm whether `sounny-com` is on Firebase **Spark** (free cap, `$0` overrun risk) or **Blaze** (pay-as-you-go); if on Blaze, configure a low `$5` budget alert in GCP Billing to guard against scripted RTDB bandwidth/spam.

- [ ] **4. Optional — Enable Firebase App Check (reCAPTCHA v3)**
  Enforce App Check on Realtime Database (`sounny-com-default-rtdb`) and initialize `firebase/app-check` in `SpaceCampFirebase.js` so direct `curl`/Python scripts cannot register fake callsigns or post synthetic `600` scores to `/spacecamp/leaderboard` or `/spacecamp/feed`. (Wait until after live workshops and test on both `jsmars.sounny.com` and `jsmars-sounny.web.app` before enforcing.)
