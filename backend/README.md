# Backend

Backend REST em Java 21 + Spring Boot 3 para observabilidade operacional de PostgreSQL e da propria aplicacao.

## Stack

- Java 21
- Spring Boot 3
- Maven
- Spring Web
- Spring JDBC
- Spring Actuator
- Micrometer
- PostgreSQL Driver
- Bean Validation
- Lombok
- OpenAPI / Swagger

## Estrutura

```text
src/main/java/br/com/vivovaloriza/dbmetricsmonitor
|-- config
|-- controller
|-- dto
|-- exception
|-- model
|-- repository
|-- scheduler
|-- security
`-- service
```

## Como subir localmente

### Variaveis de ambiente

```powershell
$env:DB_URL_ADMIN="jdbc:postgresql://localhost:5432/observability"
$env:DB_USER="postgres"
$env:DB_PASSWORD="postgres"
$env:APP_API_KEY="change-me"
$env:APP_PROTECT_READ_ENDPOINTS="true"
```

### Subir com Maven

```powershell
cd backend
$env:JAVA_HOME="C:\Users\PC Gamer\.jdks\temurin-21.0.10"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn spring-boot:run
```

### Subir com Docker Compose

```powershell
docker compose up --build
```

## Endpoints principais

- `GET /api/v1/health`
- `GET /api/v1/db/locks`
- `GET /api/v1/db/queries/top?limit=20`
- `GET /api/v1/db/connections/summary`
- `GET /api/v1/system/metrics`
- `GET /api/v1/dashboard/summary`
- `GET /api/v1/db/cache/hit-ratio`

## Swagger

- UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Testes

```powershell
cd backend
$env:JAVA_HOME="C:\Users\PC Gamer\.jdks\temurin-21.0.10"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn test
```

## Arquivos uteis

- Queries SQL: [backend/postgres-monitoring-queries.sql](C:/Projetos/Vivo-Valoriza/db-metrics-monitor/backend/postgres-monitoring-queries.sql)
- Requests HTTP: [backend/requests.http](C:/Projetos/Vivo-Valoriza/db-metrics-monitor/backend/requests.http)
