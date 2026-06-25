# Banking Service

A production-grade Spring Boot (Java 25) microservice skeleton providing a health endpoint, Flyway-managed MySQL schema, and HTTPS-only transport.

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java JDK | 25 | `java -version` must show 25.x |
| Maven | 3.9+ | `mvn -version` |
| Docker + Compose | Latest | `docker compose version` |
| OpenSSL | Any recent | `openssl version` |

---

## Quickstart

### 1. Generate TLS Credentials

```bash
bash scripts/generate-certs.sh
```

Expected output: certificates written to `certs/` and test copies placed in `src/test/resources/ssl/`.  
The script is idempotent — rerunning it when the certificate is still valid prints:
```
Certificates still valid — skipping regeneration.
```

### 2. Start the Local Database

```bash
docker compose up -d
```

Verify the container is healthy:
```bash
docker compose ps
```

The `banking-db` service should show `healthy` status.

### 3. Export Required Environment Variables

All 8 variables are mandatory. The application refuses to start if any are missing.

| Variable | Example Value | Description |
|----------|---------------|-------------|
| `DB_HOST` | `localhost` | MySQL hostname |
| `DB_PORT` | `3306` | MySQL port (optional, defaults to 3306) |
| `DB_NAME` | `banking_service` | Database name |
| `DB_USERNAME` | `banking_user` | Database user |
| `DB_PASSWORD` | *(from docker-compose.yml)* | Database password |
| `SSL_KEYSTORE_PATH` | `$(pwd)/certs/keystore.jks` | JKS keystore path |
| `SSL_KEYSTORE_PASSWORD` | `changeit` | Keystore password |
| `SSL_TRUSTSTORE_PATH` | `$(pwd)/certs/truststore.jks` | JKS truststore path |
| `SSL_TRUSTSTORE_PASSWORD` | `changeit` | Truststore password |

```bash
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=banking_service
export DB_USERNAME=banking_user
export DB_PASSWORD=<your-db-password>

export SSL_KEYSTORE_PATH=$(pwd)/certs/keystore.jks
export SSL_KEYSTORE_PASSWORD=changeit
export SSL_TRUSTSTORE_PATH=$(pwd)/certs/truststore.jks
export SSL_TRUSTSTORE_PASSWORD=changeit
```

**Fail-fast check**: Unset any variable (e.g., `unset DB_HOST`) and start the application — it will refuse with a clear error naming the missing variable. Restore before proceeding.

### 4. Build and Start

```bash
mvn clean package -DskipTests
java -jar target/banking-service-*.jar
```

Expected: application starts within 30 seconds, Flyway applies `V1__baseline.sql`, and the server binds only to port **8443** (no port 8080).

### 5. Verify the Health Endpoint

```bash
curl --cacert certs/ca.crt https://localhost:8443/health
```

Expected response (HTTP 200):
```json
{
  "status": "UP",
  "version": "0.0.1-SNAPSHOT",
  "timestamp": "2026-06-25T12:00:00Z"
}
```

Plain HTTP must be refused:
```bash
curl http://localhost:8080/health   # Connection refused — no HTTP port
curl http://localhost:8443/health   # Error — HTTPS port rejects plain HTTP
```

### 6. Verify Actuator Health

```bash
curl --cacert certs/ca.crt https://localhost:8443/actuator/health
```

Expected: `{"status":"UP","components":{"db":{"status":"UP",...}}}`

### 7. Run Integration Tests

Testcontainers starts an ephemeral MySQL container automatically — no external database required.

```bash
mvn verify
```

All three integration tests must pass:
- `HealthEndpointIT` — health endpoint over HTTP (TLS disabled for this test)
- `DatabaseConnectivityIT` — Flyway migration applied; `SELECT 1` succeeds
- `TlsConnectivityIT` — HTTPS on 8443 accepted; plain HTTP refused (**requires `generate-certs.sh` to have been run first**)

---

## Architecture

```
src/main/java/com/banking/service/
├── BankingServiceApplication.java   # Entry point
├── config/
│   ├── BankingProperties.java       # @ConfigurationProperties — typed env var binding
│   ├── RequiredEnvVarsPostProcessor.java  # Fail-fast env var validation at startup
│   └── DataSourceStartupListener.java    # DB connectivity retry with exponential backoff
├── controller/
│   └── HealthController.java        # GET /health → HealthService
├── dto/
│   └── HealthResponse.java          # Immutable Java Record
├── exception/
│   └── GlobalExceptionHandler.java  # @RestControllerAdvice — RFC 7807 ProblemDetail
└── service/
    └── HealthService.java           # Business logic — injects BuildProperties
```

Layering constraint: **Controller → Service** (no controller may access a repository directly).

---

## Credential Rotation

No rebuild is required to rotate credentials. Update environment variables and restart:

```bash
export DB_PASSWORD=<new-password>
java -jar target/banking-service-*.jar
```

---

## Architectural Decisions

See [docs/decisions.md](docs/decisions.md) for full ADRs covering:
- Spring Boot 4.1.0 + Java 25 (D-001)
- SSL Bundles for TLS configuration (D-002)
- `sslMode=VERIFY_CA` for MySQL connections (D-003)
- HikariCP + spring-retry for DB startup resilience (D-004)
- Fail-fast environment variable validation (D-005)
- `openssl` + `keytool` for certificate generation (D-006)
