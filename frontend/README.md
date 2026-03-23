# Frontend

Interface React + Vite para visualizacao operacional e analitica do PostgreSQL monitorado pelo backend.

## Stack

- React
- TypeScript
- Vite
- React Router
- Tailwind CSS
- Lucide React
- Motion

## Telas disponiveis

- `Overview`: visao consolidada do ambiente
- `Intelligence`: score, breakdown, alertas, anomalias e recomendacoes
- `Locks`: sessoes bloqueadas, bloqueantes e inspeção operacional
- `Queries`: top queries, slow queries e running queries
- `Connections`: uso atual e distribuicao de conexoes
- `History`: historico de incidentes persistidos
- `Analise IA`: envio do snapshot consolidado para a integracao de IA existente

## Integracao com API

O frontend consome a API em `VITE_API_BASE_URL`, com padrao:

```env
VITE_API_BASE_URL=/api/v1
VITE_API_KEY_HEADER=X-API-KEY
VITE_API_KEY=public-dev-key
```

## Rodar localmente

```powershell
cd frontend
npm install
npm run dev
```

Build de producao:

```powershell
npm run build
```

## Recursos de UI

- polling automatico para atualizar snapshots
- status banners e badges por severidade
- cards de metrica e paines consolidados
- pagina de intelligence com:
  - score de saude
  - score breakdown por categoria
  - penalidades aplicadas
  - alertas ativos
  - anomalias com baseline
  - recomendacoes acionaveis

## Estrutura principal

```text
src/app
|-- components
|-- hooks
|-- lib
|-- pages
`-- routes.tsx
```

## Observacoes

- o frontend nao usa dados mockados para a tela de intelligence; ele consome diretamente `/api/v1/db/intelligence/overview`
- o estado de carregamento da tela de intelligence usa banner informativo, evitando aparencia de erro enquanto o snapshot esta sendo calculado
