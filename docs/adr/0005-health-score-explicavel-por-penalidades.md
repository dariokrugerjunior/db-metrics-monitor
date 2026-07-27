# ADR-0005: Health score é 100 menos penalidades explicáveis

- **Status:** Aceito
- **Data:** 2026-07-27

## Contexto

O produto precisa de um número único que responda "o banco está bem agora?". Um número único é
ótimo para dashboard e péssimo para ação: sozinho, "72" não diz o que fazer.

Pior ainda se ele vier de uma fórmula opaca — média ponderada calibrada no olho, ou pontuação
sugerida por um modelo. Ninguém consegue contestar um score que não sabe compor, e um score
incontestável é ignorado no primeiro desacordo.

## Decisão

O score parte de **100** e desconta penalidades:

```
score = max(0, 100 − Σ penalidades.pontos)
```

Cada penalidade (`ScorePenaltyResponse`) carrega **código, pontos e mensagem**, e é produzida
por um avaliador de categoria (`ScoreCategoryEvaluator`) — conexões, cache, locks, queries,
recursos e incidentes têm cada um o seu, sobre uma base comum
(`AbstractScoreCategoryEvaluator`).

A resposta (`DatabaseHealthScoreResponse`) devolve, além do número:

- a **classificação** (`HEALTHY` / `WARNING` / `CRITICAL`), por faixas configuráveis
  (`ScoreProperties.healthyMinScore`, `warningMinScore`);
- a **lista completa de penalidades** aplicadas;
- o **detalhamento por categoria** (`breakdown`);
- um **resumo textual** derivado das duas penalidades mais relevantes.

Os avaliadores são injetados como `List<ScoreCategoryEvaluator>`: acrescentar uma categoria é
criar uma classe, sem tocar no serviço que soma.

## Consequências

- O número é sempre **decomponível**: para todo score existe a lista exata do que o derrubou e
  em quantos pontos.
- Um DBA pode discordar de uma penalidade específica em vez de descartar o indicador inteiro —
  a discussão vira "essa regra vale 15 pontos?" em vez de "esse score não significa nada".
- As faixas de classificação são configuração, então a régua se ajusta por ambiente sem mexer
  em código.
- A soma é trivialmente testável, e cada avaliador é testável isoladamente.
- O `max(0, ...)` garante que o score nunca fica negativo, mesmo com penalidades acumuladas.

### Trade-offs aceitos

- **A escala satura.** Passando de 100 pontos de penalidade, tudo vira 0: um banco ruim e um
  banco catastrófico exibem o mesmo número. A lista de penalidades ainda diferencia os dois, o
  score não.
- **Penalidades são independentes por construção.** Duas penalidades que apontam para a mesma
  causa raiz (por exemplo, lock gerando query lenta) somam duas vezes, e o score cai mais do
  que a gravidade real justificaria.
- Os pontos de cada regra são um julgamento de engenharia, não uma medida — calibrados para
  ordenar situações, não para significar percentual de saúde.
- O resumo textual usa as duas primeiras penalidades da lista, o que assume que a ordem de
  chegada é relevante; não há ordenação explícita por gravidade nesse ponto.

## Alternativas consideradas

- **Média ponderada de métricas normalizadas** — descartado: força normalizar grandezas
  incomparáveis (percentual de cache, contagem de locks, milissegundos) e o resultado deixa de
  ter explicação direta — não dá para dizer *o que* derrubou o número.
- **Pedir o score ao LLM** — descartado pelos motivos de
  [ADR-0001](0001-inteligencia-deterministica-antes-do-llm.md): número não determinístico e
  não testável, exibido como métrica.
- **Semáforo puro, sem número** — descartado: perde a granularidade que permite comparar dois
  momentos do mesmo banco, que é o uso principal do indicador ao longo do tempo.
