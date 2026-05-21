# Fire Detection System - Backend

This is the Spring Boot backend for the Fire Detection System. It manages device authentication and stores fire event data in a PostgreSQL database.

## Prerequisites

To run this project locally, you need the following:

1.  **Java JDK 21**: [Download and install JDK 21](https://adoptium.net/temurin/releases/?version=21).
2.  **Apache Maven**: [Download and install Maven](https://maven.apache.org/download.cgi).
3.  **PostgreSQL**: A running PostgreSQL instance (Local installation or Docker).

## Local Database Setup

The project is configured to use a native PostgreSQL database by default for local development.

1.  Create a database named: `it-integration-project-local-test`
2.  Configure your credentials in `src/main/resources/application-dev.properties`: \* Set `spring.datasource.password` to your local PostgreSQL password.
    TEST

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

## API Endpoints

- **POST /events**: Stores a fire event. Requires `Authorization` (Bearer token) and `X-Device-ID` headers.
- **GET /events**: Retrieves all stored events for the frontend.

## Security

Devices are validated against the `devices` table. Initial test devices are seeded automatically by the `DbInitializer` class upon startup.
| Device ID | Token |
| :--- | :--- |
| ESP32-01 | token-01 |
| ESP32-02 | token-02 |
| ESP32-03 | token-03 |
| ESP32-04 | token-04 |


Small change

