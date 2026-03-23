# db-metrics-monitor

Monorepo com backend Spring Boot e frontend React/Vite para observabilidade operacional de PostgreSQL.

## Estrutura

```text
.
|-- backend
|   |-- Dockerfile
|   |-- pom.xml
|   |-- postgres-monitoring-queries.sql
|   |-- requests.http
|   `-- src
|-- frontend
|   |-- package.json
|   `-- src
`-- docker-compose.yml
```

## Executar localmente

### Backend

```powershell
cd backend
$env:JAVA_HOME="C:\Users\PC Gamer\.jdks\temurin-21.0.10"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn spring-boot:run
```

### Frontend

```powershell
cd frontend
Copy-Item .env.example .env
npm install
npm run dev
```

### Docker Compose

```powershell
docker compose up --build
```

## Documentacao

- Backend: [backend/README.md](C:/Projetos/Vivo-Valoriza/db-metrics-monitor/backend/README.md)
- Frontend: [frontend/README.md](C:/Projetos/Vivo-Valoriza/db-metrics-monitor/frontend/README.md)
