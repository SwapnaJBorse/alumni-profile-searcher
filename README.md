# Alumni LinkedIn Profile Searcher

Spring Boot REST API that finds LinkedIn profiles of a university's alumni through the **PhantomBuster API**
and stores them in **PostgreSQL**. No UI - test with Swagger or Postman.

## Endpoints

| Method | URL | Purpose |
|--------|-----|---------|
| POST | `/api/alumni/search` | Scrape + save + return alumni for `university`, `designation`, optional `passoutYear` |
| GET | `/api/alumni/all` | Return every saved profile |
| GET | `/swagger-ui.html` | Swagger UI (OpenAPI JSON at `/v3/api-docs`) |
| GET | `/actuator/health` | Health check |

```bash
curl -X POST http://localhost:8080/api/alumni/search \
  -H "Content-Type: application/json" \
  -d '{"university":"Sandip University","designation":"Software Engineer","passoutYear":2025}'
```

Success: `{"status":"success","data":[{...}]}`
Error: `{"status":"error","code":400,"message":"Validation failed","errors":["university: university is required"],"timestamp":"..."}`

| Status | When |
|--------|------|
| 400 | Blank university/designation, year outside 1950-2100, malformed JSON |
| 502 | PhantomBuster returned an error / unusable result |
| 503 | PhantomBuster API key or agent id missing |
| 504 | Agent did not finish within `phantombuster.timeout-ms` |
| 500 | Anything unexpected (details are logged, never returned) |

## Run locally

```bash
# Option A - everything in Docker (mock scraper, no PhantomBuster account needed)
docker compose up --build

# Option B - Postgres in Docker, app from Maven
docker compose up -d db
PHANTOMBUSTER_MOCK=true mvn spring-boot:run

# Tests (H2 in memory, no Docker needed)
mvn test
```

### Configuration (environment variables)

| Variable | Meaning |
|----------|---------|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | JDBC URL (`jdbc:postgresql://host:5432/db`) and credentials |
| `PHANTOMBUSTER_API_KEY` | PhantomBuster API key |
| `PHANTOMBUSTER_AGENT_ID` | Id of your LinkedIn search phantom (agent) |
| `LINKEDIN_SESSION_COOKIE` | LinkedIn `li_at` cookie used by the phantom |
| `PHANTOMBUSTER_MOCK` | `true` = use built-in deterministic sample data instead of calling PhantomBuster |

### Using the real PhantomBuster

1. Create a **LinkedIn Search Export** phantom and note its agent id.
2. Open the agent's *Launch via API* section and compare its JSON argument with `buildArgument(...)` in
   `PhantomBusterClient`; rename keys there if your phantom differs.
3. Set the three env vars above and `PHANTOMBUSTER_MOCK=false`.

## Design

```
controller  ->  service (AlumniService)  ->  ProfileScraperClient (port)  ->  PhantomBusterClient | MockProfileScraperClient
                     |                                                          \-> PhantomBusterResultMapper (anti-corruption)
                     \-> AlumniRepository (Spring Data JPA)
```

* **Adapter / Strategy** - `ProfileScraperClient` hides the provider; the real or mock client is chosen by config.
* **Anti-corruption mapper** - converts PhantomBuster's loose JSON into `ScrapedProfile`.
* **Upsert by profile URL** - re-running a search updates rows instead of duplicating them (unique key + in-batch de-duplication).
* **No DB transaction during the scrape** - the service is intentionally not `@Transactional`.
* **One error envelope** - `GlobalExceptionHandler` covers validation, bad JSON, 404/405/415, provider failures and unknown errors.
* **TDD-style test pyramid** - unit (service, mapper, URL builder), HTTP client (`MockRestServiceServer`),
  web layer (`@WebMvcTest`), JPA (`@DataJpaTest`) and one end-to-end `@SpringBootTest`.

## Assumptions

* The search runs synchronously and may take up to `timeout-ms` (default 3 min); the API answers when the phantom finishes.
* Search exports rarely include graduation year. The year is taken from the result when present
  (`Class of 2020` in the headline, or an end-year field); otherwise the requested year is stored.
* `university` is stored as the value the caller searched for.
* Schema is generated with `ddl-auto: update`; use Flyway/Liquibase for a production system.
* Scraping LinkedIn is subject to LinkedIn's terms and PhantomBuster's usage limits; this is for the assignment demo.

## Deploy on Render (Docker)

1. Push this repo to GitHub.
2. Render -> **New PostgreSQL** (free). Copy host, database, user, password.
3. Render -> **New Web Service** from the repo (Runtime: Docker). Add env vars:
   `DB_URL=jdbc:postgresql://<host>:5432/<database>` (Render's `postgres://` string must be rewritten to this `jdbc:` form),
   `DB_USERNAME`, `DB_PASSWORD`, `PHANTOMBUSTER_MOCK=true` (or the real keys).
4. Health check path: `/actuator/health`. Swagger: `https://<service>.onrender.com/swagger-ui.html`.
