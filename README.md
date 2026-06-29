# Banking Service

A Spring Boot 4.1.0 REST API for managing bank accounts and financial transactions. Built on Java 25 with HTTPS-only transport (TLS 1.2/1.3), Flyway-managed MySQL, and OpenAPI/Swagger UI.

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java JDK | 25 (Temurin) | `java -version` must show 25.x; install via [Adoptium](https://adoptium.net/) |
| Maven | 3.9+ | `mvn -version` |
| Docker + Compose | Latest | `docker compose version` |

---

## Dev Mode Setup

All environment variables have built-in defaults — **no exports required** for local development.

### 1. Start the database

```bash
docker compose up -d
```

Verify it is healthy before proceeding:
```bash
docker compose ps
```

The `banking-db` service should show `healthy` (MySQL 8.4 on port 3306).

### 2. Start the application

```bash
mvn spring-boot:run
```

Alternatively, build and run the JAR:
```bash
mvn clean package -DskipTests
java -jar target/banking-service-*.jar
```

Expected: the application starts within ~15 seconds, Flyway applies migrations, and the server binds to port **8443** (HTTPS only). Look for:
```
Tomcat started on port 8443 (https)
```

### 3. Verify the application is running

```bash
curl -k https://localhost:8443/actuator/health
```

Expected: `{"status":"UP"}`

> The `-k` flag skips certificate verification for the bundled self-signed cert. For CA-verified requests see [TLS Credentials](#tls-credentials).

---

## Swagger UI

The application exposes Swagger UI at all times. Because the server uses a self-signed certificate, the browser must trust it before "Try it out" requests work.

### Step 1 — Trust the self-signed certificate

Open the following URL in your browser and accept the security warning:

```
https://localhost:8443/actuator/health
```

Click **Advanced → Proceed to localhost (unsafe)** (Chrome) or the equivalent in your browser. This is a one-time step per browser session.

### Step 2 — Open Swagger UI

```
https://localhost:8443/swagger-ui
```

You will see two groups: **Accounts** and **Users**.

### Step 3 — Execute a request

1. Expand any endpoint, e.g. `GET /api/users/{userId}/accounts`.
2. Click **Try it out**.
3. Fill in the required path parameters.
4. Click **Execute** — the response body and HTTP status code appear below.

### Finding seeded IDs

`DbInitializer` seeds sample data into a fresh database on first startup. Query the running container for real UUIDs to use in Swagger:

```bash
# List users
docker exec -it $(docker compose ps -q banking-db) \
  mysql -u banking_app -pchangeme banking_service \
  -e "SELECT id, name FROM users;"

# List accounts
docker exec -it $(docker compose ps -q banking-db) \
  mysql -u banking_app -pchangeme banking_service \
  -e "SELECT id, user_id, currency_code, balance FROM accounts;"
```

Copy a UUID from the output and paste it into the Swagger path parameter field.

---

## Running Tests

### Unit tests

```bash
mvn test
```

### Integration tests

Testcontainers spins up an ephemeral MySQL container automatically — the running `banking-db` container is not used.

```bash
mvn verify
```

---

## TLS Credentials

### Dev default (bundled)

The application defaults to `classpath:ssl/test-keystore.jks` and `classpath:ssl/test-truststore.jks`, which are pre-generated self-signed JKS files bundled in `src/main/resources/ssl/`. No setup is needed.

---

## Environment Variables

All variables have defaults suitable for local development. Override them to connect to a different database or use custom TLS credentials.

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | MySQL hostname |
| `DB_PORT` | `3306` | MySQL port |
| `DB_NAME` | `banking_service` | Database name |
| `DB_USERNAME` | `banking_app` | Database user |
| `DB_PASSWORD` | `changeme` | Database password |
| `DB_ROOT_PASSWORD` | `root_password` | MySQL root password (Docker only) |
| `SSL_KEYSTORE_PATH` | `classpath:ssl/test-keystore.jks` | JKS keystore location |
| `SSL_KEYSTORE_PASSWORD` | `changeit` | Keystore password |
| `SSL_KEY_ALIAS` | `banking-service` | Key alias in the keystore |
| `SSL_TRUSTSTORE_PATH` | `classpath:ssl/test-truststore.jks` | JKS truststore location |
| `SSL_TRUSTSTORE_PASSWORD` | `changeit` | Truststore password |
| `DEBIT_CHECK_URL` | *(Postman mock)* | External debit-check service URL |

---

## Architecture

```
src/main/java/com/banking/service/
├── BankingServiceApplication.java        # Entry point; registers HC5 client, ObjectMapper, OpenAPI beans
├── config/
│   ├── CorsConfig.java                   # CORS configuration
│   └── DbInitializer.java                # Seeds currencies, exchange rates, and sample data on startup
├── constant/
│   └── TransactionType.java              # DEPOSIT, WITHDRAWAL, CURRENCY_EXCHANGE
├── controller/
│   ├── AccountController.java            # /api/accounts/{accountId} — balance, transactions, deposit, withdraw, exchange
│   ├── UserController.java               # /api/users/{userId}/accounts — list accounts for a user
│   └── dto/                              # Request/response records (AccountResponse, DepositRequest, …)
├── dao/
│   ├── CurrencyDao.java                  # Interface
│   ├── ExchangeRateDao.java              # Interface
│   └── jpa/                              # JPA implementations
├── entity/                               # JPA entities: Account, User, Transaction, Currency, ExchangeRate
├── exception/
│   └── GlobalExceptionHandler.java       # @RestControllerAdvice — RFC 7807 ProblemDetail responses
├── mapper/                               # MapStruct mappers: AccountMapper, TransactionMapper
├── repository/                           # Spring Data JPA repositories
└── service/
    ├── AccountService.java               # Core business logic with Spring Retry
    ├── UserService.java                  # User account queries
    ├── ExternalLoggingService.java       # Apache HttpClient 5 client for debit-check notifications
    └── dto/                              # Internal service transfer objects
```

Layering constraint: **Controller → Service → DAO / Repository** (controllers may not access repositories directly).

---
