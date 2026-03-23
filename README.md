# db-metrics-monitor

Monorepo para observabilidade operacional de PostgreSQL com:

- `backend`: API REST em Java 21 + Spring Boot 3
- `frontend`: interface React + Vite para monitoramento e intelligence operacional

## Estrutura

```text
.
|-- backend
|-- frontend
|-- data
|-- docker-compose.yml
`-- README.md
```

## Principais recursos

- monitoramento de conexoes, locks, queries, cache hit ratio, CPU e memoria
- historico de incidentes persistido em SQLite
- camada de intelligence operacional com:
  - score automatico de saude
  - alertas inteligentes
  - deteccao de anomalias por baseline historica
  - recomendacoes automaticas baseadas em correlacao de sinais
- tela web consolidada para `overview`, `locks`, `queries`, `connections`, `history`, `ai-analysis` e `intelligence`

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
npm install
npm run dev
```

### Docker Compose

```powershell
docker compose up --build
```

## Endpoints de destaque

- `GET /api/v1/dashboard/summary`
- `GET /api/v1/db/connections/summary`
- `GET /api/v1/db/locks`
- `GET /api/v1/db/queries/top`
- `GET /api/v1/history/summary`
- `GET /api/v1/db/intelligence/overview`
- `GET /api/v1/db/intelligence/score`
- `GET /api/v1/db/intelligence/alerts`
- `GET /api/v1/db/intelligence/anomalies`
- `GET /api/v1/db/intelligence/recommendations`

## Documentacao

- Backend: [backend/README.md](C:/Projetos/Vivo-Valoriza/db-metrics-monitor/backend/README.md)
- Frontend: [frontend/README.md](C:/Projetos/Vivo-Valoriza/db-metrics-monitor/frontend/README.md)
