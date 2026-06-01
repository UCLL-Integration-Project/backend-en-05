# Security & Data Engineering - GUIDE

## Security - The Big Picture

- *Dashboard users:* log in through **Auth0** (like a bouncer), get a JWT token (like a wristband), show it with every request.
- *ESP32 robots:* show their device ID and secret token. Backend checks if they match.
- *All traffic* is encrypted with HTTPS.

---

## How Login Works

We use **Auth0** — we don't handle passwords ourselves.

1. You click "Sign In" → get sent to Auth0's page.
2. Type your email and password.
3. Auth0 asks for a code from your phone (MFA).
4. Auth0 gives you a **JWT token** — a digital ID card with your name and role.
5. Your browser shows this token to the backend with every request.

Our backend **never sees your password**.

---

## MFA (Multi-Factor Authentication)

You need **two things** to log in: your password + a code from your phone.

First login: Auth0 shows a **QR code**. You scan it with an authenticator app (Google Authenticator, Microsoft Authenticator, etc.). This links your account.

Every login after that: after your password, Auth0 asks for a **6-digit code** from the app. It changes every 30 seconds. You only scan the QR code once.

Even if someone steals your password, they can't log in without your phone.

---

## RBAC (Who Can Do What)

- **ADMIN** — can see everything AND control the robot
- **VIEWER** — can see everything but CANNOT control the robot

The frontend hides control buttons for VIEWERs, but **hiding a button is NOT security**. The backend also checks your role — if a VIEWER tries to send a command directly, the backend returns `403 Forbidden`.

---

## Security Headers

Rules the backend sends to the browser:

- **CSP:** whitelist of trusted domains. Browser blocks scripts/fonts/API calls from anywhere else. Stops XSS attacks.
- **HSTS:** forces HTTPS, never HTTP.
- **X-Frame-Options: DENY:** blocks our site from being loaded in an iframe. Stops clickjacking.
- **CORS:** only our frontend can talk to our backend.

---

## HTTPS / TLS

Like sending a letter in a sealed envelope instead of a postcard. Without it, anyone on the same WiFi could read your data. OKD handles it automatically.

---

## Password Hashing

We store a **hash**, not the actual password. It's one-way — you can't reverse it. We use **BCrypt**, which is slow on purpose so hackers can't brute-force it.

---

# Data Engineering

## Database & Tables

**PostgreSQL** on OKD. Credentials stored in Kubernetes Secrets, never hardcoded.

Four tables:
- **users** — dashboard accounts (username, hashed password, role)
- **devices** — ESP32 robots (ID, token, name, online/offline, last seen)
- **telemetry** — sensor snapshots every few seconds (battery, temperature, flame sensors, pump, motors, water level, GPS)
- **fire_events** — fire incidents (when, where, temperature, battery, water level, which device)

---

## Migrations (Flyway)

We never manually create tables. **Flyway** applies SQL files from `db/migration/` automatically on startup.

- **V1** — creates all four tables
- **V2** — adds online status to devices
- **V5** — adds GPS to fire events
- **V9** — removes old user token column (replaced by Auth0)
- **V10** — fixes a column name typo

---

## Data Pipeline

**Robot → Backend → Database:**
- `POST /v1/telemetry` — sensor readings → saved to telemetry table
- `POST /v1/events` — fire events → checked for duplicates → saved to fire_events table
- `POST /v1/robot/heartbeat` — updates device online status

**Database → Frontend:**
- **REST API:** frontend asks for data (`GET /v1/status`, `GET /v1/events`)
- **WebSocket:** backend pushes updates automatically to `/topic/telemetry`, `/topic/fire-alert`, `/topic/location` — dashboard updates in real-time without refreshing

---

## Event Deduplication

If the flame sensor flickers, the robot might send "FIRE!" three times in one second. The backend checks: "Did this robot already report fire in the last 5 seconds?" If yes, the duplicate is dropped.

---

## Automated Backups

Every 6 hours, a CronJob exports the entire database to a SQL file. Files older than 2 days are deleted. Config lives in `k8s/backup/`.
