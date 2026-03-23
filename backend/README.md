# Backend

API REST em Java 21 + Spring Boot 3 para observabilidade e intelligence operacional de PostgreSQL.

## Stack

- Java 21
- Spring Boot 3
- Maven
- Spring Web
- Spring JDBC
- Spring Actuator
- Micrometer / Prometheus
- PostgreSQL Driver
- SQLite JDBC
- Bean Validation
- Lombok
- OpenAPI / Swagger

## Modulos principais

```text
src/main/java/br/com/vivovaloriza/dbmetricsmonitor
|-- config
|-- controller
|-- dto
|-- exception
|-- intelligence
|-- model
|-- repository
|-- scheduler
|-- security
`-- service
```

## O que a API entrega

### Monitoramento operacional

- locks ativos, bloqueados e bloqueantes
- running queries e top queries
- resumo de conexoes
- cache hit ratio
- metricas da aplicacao
- dashboard consolidado
- historico de incidentes

### Intelligence operacional

- score de saude do banco com breakdown por categoria
- alertas acionaveis classificados por severidade
- deteccao de anomalias com baseline historica
- recomendacoes automaticas baseadas em correlacao de sinais
- persistencia local de snapshots operacionais para baseline

## Configuracao

### Variaveis principais

```powershell
$env:DB_URL_ADMIN="jdbc:postgresql://localhost:5432/observability"
$env:DB_USER="postgres"
$env:DB_PASSWORD="postgres"
$env:APP_API_KEY="change-me"
$env:APP_PROTECT_READ_ENDPOINTS="true"
```

### Properties de intelligence

Os thresholds ficam em `backend/src/main/resources/application.yml` sob:

- `db.intelligence.score.*`
- `db.intelligence.alerts.*`
- `db.intelligence.anomaly.*`
- `db.intelligence.recommendation.*`

## Subir localmente

```powershell
cd backend
$env:JAVA_HOME="C:\Users\PC Gamer\.jdks\temurin-21.0.10"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn spring-boot:run
```

## Endpoints principais

### Core

- `GET /api/v1/health`
- `GET /api/v1/dashboard/summary`
- `GET /api/v1/system/metrics`
- `GET /api/v1/history/summary`

### Database

- `GET /api/v1/db/locks`
- `GET /api/v1/db/locks/blocking`
- `GET /api/v1/db/locks/blocked`
- `GET /api/v1/db/queries/top?limit=20`
- `GET /api/v1/db/queries/slow?limit=20`
- `GET /api/v1/db/queries/running?minDurationSeconds=30`
- `GET /api/v1/db/connections/summary`
- `GET /api/v1/db/cache/hit-ratio`
- `POST /api/v1/db/sessions/{pid}/terminate`

### Intelligence

- `GET /api/v1/db/intelligence/overview`
- `GET /api/v1/db/intelligence/score`
- `GET /api/v1/db/intelligence/alerts`
- `GET /api/v1/db/intelligence/anomalies`
- `GET /api/v1/db/intelligence/recommendations`

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
