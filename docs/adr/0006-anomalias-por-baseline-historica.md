# ADR-0006: Detectar anomalias por baseline histórica, não por limiar fixo

- **Status:** Aceito
- **Data:** 2026-07-27

## Contexto

Limiar fixo não sabe o que é normal para *aquele* banco. "Alertar acima de 200 conexões" é
ruído num sistema que vive com 400 e é tarde demais num que vive com 20.

O que interessa operacionalmente quase nunca é o valor absoluto: é a **mudança de
comportamento**. Um banco que sempre teve zero lock bloqueado e agora tem dois mudou de
estado, mesmo que dois seja um número pequeno.

## Decisão

As anomalias são calculadas comparando o snapshot atual com a **baseline histórica recente**
do próprio banco (`DatabaseAnomalyDetectionService`).

Um agendador (`OperationalSnapshotScheduler`) grava snapshots operacionais periodicamente
(`DatabaseMetricsHistoryRecorder`), e a baseline é lida do histórico persistido
(`PersistedDatabaseMetricsHistoryProvider`).

Para cada métrica a baseline traz média, mediana, mínimo, máximo e o desvio percentual do valor
atual. Dispara anomalia quando o desvio ultrapassa o limiar configurado da métrica — e a
severidade sobe de `WARNING` para `CRITICAL` quando o desvio passa do **dobro** do limiar.

Métricas cobertas: total de conexões, conexões ativas, `idle in transaction`, locks bloqueados,
queries em execução, queries longas, cache hit ratio, CPU e memória.

Dois comportamentos merecem destaque, porque são onde limiar percentual quebra:

- **Baseline zero.** Se a média histórica é `0` e o valor atual é maior que zero, o desvio
  percentual seria indefinido ou infinito. O caso é tratado explicitamente: vira anomalia
  direta ("valor fora do padrão histórico nulo"). Para locks bloqueados isso é `CRITICAL` — um
  banco que nunca travou e travou agora é a definição de mudança de comportamento.
- **Cache hit ratio cai, não sobe.** Percentual de desvio não serve para uma métrica em que só
  a queda importa e a escala é comprimida perto de 100%. Por isso o cache usa **queda
  absoluta** contra a média (`CacheHitDropThreshold`), com `CRITICAL` ao dobrar o limiar.

### Guarda de confiança

Se o histórico tiver menos amostras que `anomaly.minimumSamples`, o serviço **não detecta
nada** e responde explicitamente "baseline insuficiente para detectar anomalias com
confiança", registrando log estruturado. Não há palpite com amostra pequena.

## Consequências

- O sistema se adapta ao banco em vez de exigir calibração manual por ambiente.
- Anomalias vêm com a baseline anexada (`MetricBaselineResponse`), então o alerta já mostra
  contra o quê o valor foi comparado — média, mediana, mínimo, máximo e desvio.
- Nos primeiros ciclos, a ferramenta declara que ainda não sabe, em vez de gerar falso
  positivo — comportamento honesto e visível na interface.
- Os limiares por métrica ficam em `AnomalyProperties`, ajustáveis sem recompilar.

### Trade-offs aceitos

- **A baseline aprende o problema.** Um banco cronicamente saturado estabelece a saturação como
  normal, e o desvio para de disparar. Baseline detecta *mudança*, não *inadequação* — por isso
  ela convive com as regras de limiar absoluto de
  [ADR-0001](0001-inteligencia-deterministica-antes-do-llm.md) e
  [ADR-0007](0007-regras-como-classes-com-codigos-e-limiares-externos.md), que continuam
  necessárias.
- **Média é sensível a outlier.** A mediana é calculada e devolvida, mas quem decide o desvio é
  a média — um pico isolado no histórico eleva a régua por um tempo.
- **Não há sazonalidade.** A janela recente não distingue segunda de manhã de domingo de
  madrugada; um pico previsível de carga aparece como anomalia.
- Sem desvio padrão ou z-score: o critério é percentual sobre a média, mais simples de explicar
  e mais grosseiro que um modelo estatístico.
- A qualidade da baseline depende do agendador ter rodado — histórico interrompido significa
  baseline enviesada, e nada além do contador de amostras sinaliza isso.

## Alternativas consideradas

- **Só limiar fixo** — descartado: não distingue banco de banco, e é a causa clássica de
  alerta ignorado por excesso de ruído.
- **Modelo estatístico (z-score, EWMA) ou de ML** — descartado por ora: exige mais histórico e
  mais explicação para ganho pequeno num diagnóstico que precisa ser justificável em uma frase.
  Percentual sobre média é defensável na tela.
- **Detectar anomalia com LLM** — descartado: mesma objeção de
  [ADR-0001](0001-inteligencia-deterministica-antes-do-llm.md), agravada por ser cálculo
  numérico, que é onde modelo de linguagem tem menos garantia.
