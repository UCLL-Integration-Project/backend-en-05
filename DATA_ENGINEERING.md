# Fire Detection System — Data Engineering

This document covers the data engineering implementation for the Fire Detection System, including database setup, schema design, data pipeline, migrations, seed data, and automated backups. Covers stories **DATA-01** (Table Creation), **DATA-02** (Data Pipeline), and database setup.

### Requirements Covered

| Req ID | Requirement | Points |
|:---|:---|:---|
| **DAI-01** | Design and implement a normalised relational database (3NF minimum) with DDL scripts in version control | 3 |
| **DAI-04** | Build a data pipeline that ingests, cleans and transforms a real dataset (ETL), reproducible via script | 5 |
| **SD-18** | Use an ORM correctly with all schema changes tracked as migrations in version control | 3 |
| **SD-15** | Implement a WebSocket or real-time feature (live updates, notifications) | 8 |
| **MCU-08** | Build an end-to-end IoT pipeline: ESP32 collects data → sends to API → stored in database → visualised in dashboard | 8 |
| **INF-06** | Implement an automated backup strategy for a service, with a verified restore procedure | 3 |

---

## 1. Database Setup

The system uses **PostgreSQL** as its primary database, deployed on the OKD (OpenShift) cluster.

**Connection configuration** (`application.properties`):
```properties
spring.datasource.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
```

All connection credentials are stored as environment variables, never hardcoded. On OKD, these are injected from Kubernetes secrets. For local development, they are set in `application-dev.properties`.

The database uses **Flyway** for schema migrations. Flyway runs automatically on application startup, applying any new migration files in order:

```properties
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
```

Hibernate is set to `validate` mode, meaning it verifies that the JPA entities match the database schema but never modifies the schema itself. All schema changes go through Flyway migrations.

```properties
spring.jpa.hibernate.ddl-auto=validate
```

---

## 2. Database Schema (DATA-01)

The database consists of four tables. The initial schema is defined in `V1__initial_schema.sql`.

### Entity Relationship Diagram

```
┌──────────────────┐       ┌──────────────────────────────────┐
│      users       │       │           fire_events            │
├──────────────────┤       ├──────────────────────────────────┤
│ id          PK   │       │ incident_id       PK  (UUID)    │
│ username         │       │ timestamp                        │
│ first_name       │       │ temperature                      │
│ last_name        │       │ battery_pct                      │
│ password         │       │ duration_s                       │
│ role             │       │ is_extinguished                  │
└──────────────────┘       │ water_level_pct                  │
                           │ event_type                       │
┌──────────────────┐       │ device_id         FK ──────┐    │
│     devices      │       │ latitude                   │    │
├──────────────────┤       │ longitude                  │    │
│ device_id   PK ◄─────────│ location_accuracy_m        │    │
│ device_token     │       └──────────────────────────────────┘
│ name             │
│ is_online        │       ┌──────────────────────────────────┐
│ last_seen        │       │           telemetry              │
└──────────────────┘       ├──────────────────────────────────┤
                           │ id               PK  (BIGSERIAL)│
                           │ time                             │
                           │ battery_voltage                  │
                           │ temperature_c                    │
                           │ flame_left                       │
                           │ flame_center                     │
                           │ flame_right                      │
                           │ pump_active                      │
                           │ motor_left_pwm                   │
                           │ motor_right_pwm                  │
                           │ water_level_pct                  │
                           │ latitude                         │
                           │ longitude                        │
                           │ accuracy_m                       │
                           └──────────────────────────────────┘
```

### Table: `users`

Stores dashboard user accounts. Passwords are BCrypt-hashed.

| Column | Type | Constraints | Description |
|:---|:---|:---|:---|
| `id` | BIGSERIAL | PRIMARY KEY | Auto-incrementing user ID |
| `username` | VARCHAR(255) | UNIQUE, NOT NULL | Login username |
| `first_name` | VARCHAR(255) | | User's first name |
| `last_name` | VARCHAR(255) | | User's last name |
| `password` | VARCHAR(255) | | BCrypt-hashed password |
| `role` | VARCHAR(50) | | User role: ADMIN or VIEWER |

### Table: `devices`

Stores registered ESP32 devices. Each device has a unique ID and authentication token.

| Column | Type | Constraints | Description |
|:---|:---|:---|:---|
| `device_id` | VARCHAR(255) | PRIMARY KEY | Unique device identifier (e.g., ESP32-01) |
| `device_token` | VARCHAR(255) | NOT NULL | Authentication token for API access |
| `name` | VARCHAR(255) | | Human-readable device name |
| `is_online` | BOOLEAN | NOT NULL, DEFAULT FALSE | Whether the device is currently connected |
| `last_seen` | TIMESTAMP | | Last time the device sent a heartbeat |

### Table: `telemetry`

Stores real-time sensor data from the ESP32 devices. Each row is a snapshot of the robot's state at a given moment.

| Column | Type | Constraints | Description |
|:---|:---|:---|:---|
| `id` | BIGSERIAL | PRIMARY KEY | Auto-incrementing telemetry ID |
| `time` | TIMESTAMP | NOT NULL | When the reading was taken |
| `battery_voltage` | REAL | | Battery voltage in volts |
| `temperature_c` | REAL | | Ambient temperature in Celsius |
| `flame_left` | SMALLINT | | Left flame sensor reading |
| `flame_center` | SMALLINT | | Center flame sensor reading |
| `flame_right` | SMALLINT | | Right flame sensor reading |
| `pump_active` | BOOLEAN | NOT NULL, DEFAULT FALSE | Whether the water pump is running |
| `motor_left_pwm` | SMALLINT | | Left motor PWM value |
| `motor_right_pwm` | SMALLINT | | Right motor PWM value |
| `water_level_pct` | SMALLINT | CHECK (0-100) | Water tank level as percentage |
| `latitude` | DOUBLE PRECISION | | GPS latitude from geolocation |
| `longitude` | DOUBLE PRECISION | | GPS longitude from geolocation |
| `accuracy_m` | DOUBLE PRECISION | | Location accuracy in meters |

**Index:** `telemetry_time_desc_idx` on `time DESC` — optimizes queries for the latest telemetry reading, which is the most common query pattern.

### Table: `fire_events`

Stores fire detection events. Each row represents a single fire incident detected by a device.

| Column | Type | Constraints | Description |
|:---|:---|:---|:---|
| `incident_id` | UUID | PRIMARY KEY | Unique event identifier |
| `timestamp` | TIMESTAMP | NOT NULL | When the fire was detected |
| `temperature` | REAL | | Temperature at detection |
| `battery_pct` | SMALLINT | CHECK (0-100) | Battery level at detection |
| `duration_s` | INTEGER | | How long the fire lasted (seconds) |
| `is_extinguished` | BOOLEAN | | Whether the fire was put out |
| `water_level_pct` | SMALLINT | CHECK (0-100) | Water level at detection |
| `event_type` | VARCHAR(50) | | Type: fire_detected, fire_cleared, etc. |
| `device_id` | VARCHAR(255) | FOREIGN KEY → devices | Which device detected it |
| `latitude` | DOUBLE PRECISION | | Location latitude where fire was detected |
| `longitude` | DOUBLE PRECISION | | Location longitude where fire was detected |
| `location_accuracy_m` | INTEGER | | Location accuracy in meters |

**Index:** `fire_events_timestamp_idx` on `timestamp DESC` — optimizes the event log display, which shows the most recent events first.

**Foreign key:** `device_id` references `devices(device_id)` — ensures every fire event is linked to a registered device.

---

## 3. Flyway Migrations

All schema changes are managed through Flyway migration files located in `src/main/resources/db/migration/`. Each file is named with a version prefix (V1, V2, etc.) and is applied exactly once, in order.

| Migration | Description |
|:---|:---|
| `V1__initial_schema.sql` | Creates all four tables (users, devices, telemetry, fire_events) with constraints and indexes |
| `V2__add_device_status.sql` | Adds `is_online` and `last_seen` columns to the devices table for tracking device connectivity |
| `V5__add_location.sql` | Adds `latitude`, `longitude`, and `location_accuary_m` columns to fire_events for storing fire location |
| `V9__drop_token_column.sql` | Removes the `token` column from users table (replaced by Auth0 JWT authentication) |
| `V10__fix_location_accuracy_column_name.sql` | Fixes the typo in column name: `location_accuary_m` → `location_accuracy_m` |

Flyway tracks which migrations have been applied in a `flyway_schema_history` table in the database. If a migration has already been applied, it is skipped on the next startup.

---

## 4. Data Pipeline (DATA-02)

The data pipeline describes how data flows from the ESP32 devices through the backend into the database and out to the frontend.

### Inbound Data Flow (ESP32 → Database)

```
ESP32 Robot
    │
    ├── POST /v1/telemetry ──────────► TelemetryController
    │   (every few seconds)                    │
    │                                          ▼
    │                                  TelemetryRepository.save()
    │                                          │
    │                                          ▼
    │                                  telemetry table
    │
    ├── POST /v1/events ─────────────► EventController
    │   (on fire detection)                    │
    │                                          ▼
    │                                  EventService (deduplication check)
    │                                          │
    │                                          ▼
    │                                  EventRepository.save()
    │                                          │
    │                                          ▼
    │                                  fire_events table
    │
    └── POST /v1/robot/heartbeat ────► StatusController
        (periodic keepalive)                   │
                                               ▼
                                       DeviceRepository
                                       (update is_online, last_seen)
```

### Outbound Data Flow (Database → Frontend)

```
Frontend (React)
    │
    ├── GET /v1/status ──────────────► StatusService
    │                                  └── TelemetryRepository.findFirstByOrderByTimeDesc()
    │                                      → Returns latest telemetry row
    │
    ├── GET /v1/events ──────────────► EventService
    │                                  └── EventRepository.findAll() with filters
    │                                      → Returns all fire events
    │
    └── WebSocket /ws ───────────────► STOMP broker
        (real-time updates)            ├── /topic/telemetry  → live sensor data
                                       ├── /topic/fire-alert → fire notifications
                                       └── /topic/location   → robot position updates
```

### Event Deduplication

The `EventService` implements deduplication to prevent duplicate fire events from being stored. If the same device sends multiple fire detection events within a configurable time window (default: 5 seconds), only the first event is saved.

```properties
app.deduplication.window-seconds=5
```

This is implemented using `EventRepository.findFirstByDeviceIdAndTimestampBetween()`, which checks if an event from the same device already exists within the window.

### WebSocket Real-Time Updates

The backend uses **STOMP over WebSocket** to push real-time data to the frontend without polling.

Configuration (`WebSocketConfig.java`):
- **Endpoint:** `/ws` (with SockJS fallback for browser compatibility)
- **Broker prefix:** `/topic` (clients subscribe to topics under this prefix)
- **Application prefix:** `/app` (clients send messages with this prefix)

Topics:
| Topic | Data | Purpose |
|:---|:---|:---|
| `/topic/telemetry` | Latest sensor readings | Live dashboard updates |
| `/topic/fire-alert` | Fire event details | Real-time fire notifications |
| `/topic/location` | Latitude, longitude, accuracy | Robot position on map |

---

## 5. JPA Entity Mapping

Each database table is mapped to a Java entity class using JPA (Jakarta Persistence API). Spring Data JPA repositories provide database access without writing SQL.

| Table | Entity Class | Repository | Key Query |
|:---|:---|:---|:---|
| `users` | `User.java` | `UserRepository` | `findByUsername(String)` |
| `devices` | `Device.java` | `DeviceRepository` | `findByDeviceIdAndDeviceToken(String, String)` |
| `telemetry` | `Telemetry.java` | `TelemetryRepository` | `findFirstByOrderByTimeDesc()` |
| `fire_events` | `Event.java` | `EventRepository` | `findFirstByOrderByTimestampDesc()` |

The `EventRepository` also extends `JpaSpecificationExecutor`, which allows dynamic filtering of events using Spring Data Specifications (used for the event log filters on the frontend).

---

## 6. Seed Data

On first startup, the `DbInitializer` class populates the database with initial data for development and testing.

### Users

| Username | First Name | Last Name | Password (plain) | Role |
|:---|:---|:---|:---|:---|
| admin | Administrator | System | admin123 | ADMIN |
| jdoe | John | Doe | password123 | VIEWER |
| asmith | Alice | Smith | password123 | VIEWER |

All passwords are BCrypt-hashed before storage. The initializer checks if the database is empty or if any user has a null password before running.

### Devices

| Device ID | Token | Name |
|:---|:---|:---|
| ESP32-01 | token-01 | Front Lobby Robot |
| ESP32-02 | token-02 | Server Room Robot |
| ESP32-03 | token-03 | Warehouse Robot |
| ESP32-04 | token-04 | Kitchen Robot |

### Initial Telemetry

One telemetry record is created with default values (battery: 12.6V, temperature: 22.5°C, pump: off) so the dashboard has something to display on first load.

**Relevant file:** `DbInitializer.java`

---

## 7. Automated Backups

A Kubernetes CronJob runs every 6 hours on the OKD cluster, performing a `pg_dump` of the PostgreSQL database and storing the backup files in a Persistent Volume.

### Components

| File | Resource | Purpose |
|:---|:---|:---|
| `k8s/backup/pvc.yaml` | PersistentVolumeClaim | 8Gi storage for backup files |
| `k8s/backup/configmap.yaml` | ConfigMap | Shell script that performs the backup |
| `k8s/backup/cronjob.yaml` | CronJob | Schedules the backup to run every 6 hours |

### How It Works

1. The CronJob triggers every 6 hours (`0 */6 * * *`)
2. A `postgres:15-alpine` container starts with the backup script mounted
3. The script runs `pg_dump` to export the entire database to a SQL file
4. The file is saved to `/backups/backup-YYYY-MM-DDTHH-MM-SS.sql`
5. The retention policy deletes backup files older than 2 days
6. Database credentials are read from Kubernetes secrets (never hardcoded)

### Backup Script

```bash
TIMESTAMP=$(date +%Y-%m-%dT%H-%M-%S)
FILENAME="backup-${TIMESTAMP}.sql"
PGPASSWORD=$DB_PASSWORD pg_dump -h $DB_HOST -U $DB_USER -d $DB_NAME > /backups/${FILENAME}
# Delete backups older than 2 days
find /backups -name "backup-*.sql" -type f -mmin +2880 -delete
```

### Restore Procedure

To restore a backup:

```bash
cat backup-YYYY-MM-DDTHH-mm-ss.sql | psql -h ${DB_HOST} -U ${DB_USER} -d ${TARGET_DB_NAME}
```

### Verification

Compare row counts between production and a test restore:

```sql
-- On production
SELECT count(*) FROM fire_events;
SELECT count(*) FROM telemetry;

-- On restored test database (should match)
SELECT count(*) FROM fire_events;
SELECT count(*) FROM telemetry;
```

---

## 8. Testing Database

For integration tests, the project uses an **H2 in-memory database** running in PostgreSQL compatibility mode. This avoids needing a real PostgreSQL instance for CI/CD.

Configuration (`application-test.properties`):
```properties
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

The same Flyway migrations run against H2, ensuring the test database schema matches production. The `MODE=PostgreSQL` flag makes H2 accept PostgreSQL-specific SQL syntax.

---

## File Reference

| File | Description |
|:---|:---|
| `V1__initial_schema.sql` | Initial database schema with all four tables |
| `V2__add_device_status.sql` | Adds device online status tracking |
| `V5__add_location.sql` | Adds geolocation columns to fire events |
| `V9__drop_token_column.sql` | Removes deprecated token column from users |
| `V10__fix_location_accuracy_column_name.sql` | Fixes column name typo |
| `User.java` | JPA entity for users table |
| `Device.java` | JPA entity for devices table |
| `Telemetry.java` | JPA entity for telemetry table |
| `Event.java` | JPA entity for fire_events table |
| `UserRepository.java` | Spring Data repository for users |
| `DeviceRepository.java` | Spring Data repository for devices |
| `TelemetryRepository.java` | Spring Data repository for telemetry |
| `EventRepository.java` | Spring Data repository for fire events |
| `DbInitializer.java` | Seed data for initial database population |
| `WebSocketConfig.java` | WebSocket configuration for real-time data |
| `application.properties` | Database and Flyway configuration |
| `application-test.properties` | H2 test database configuration |
| `k8s/backup/pvc.yaml` | Persistent volume for backups |
| `k8s/backup/configmap.yaml` | Backup shell script |
| `k8s/backup/cronjob.yaml` | Scheduled backup job |
