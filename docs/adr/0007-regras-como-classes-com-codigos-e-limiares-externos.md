# ADR-0007: Regras como classes, com códigos em enum e limiares em properties

- **Status:** Aceito
- **Data:** 2026-07-27

## Contexto

A camada de inteligência é, no fundo, um conjunto grande de regras: quando alertar, quanto
penalizar, o que recomendar. Escrito da forma mais direta, isso vira um método de trezentas
linhas com `if` encadeado, mensagens em string literal e números mágicos no meio do código.

Esse formato falha em três frentes ao mesmo tempo: não dá para testar uma regra isolada, o
frontend precisa comparar texto para reagir a um achado, e ajustar um limiar exige recompilar.

## Decisão

Três padrões aplicados de forma consistente em toda a camada `intelligence`:

**1. Uma regra, uma classe.** Cada regra implementa uma interface e recebe um contexto de
avaliação:

| Interface | Contexto | Implementações |
|-----------|----------|----------------|
| `AlertRule` | `AlertEvaluationContext` | anomalia, cache, conexão, incidente, lock, query, recurso |
| `RecommendationRule` | `RecommendationEvaluationContext` | cache/queries pesadas, pooling, CPU/queries caras, lock/idle, incidente recorrente |
| `ScoreCategoryEvaluator` | `DatabaseHealthSnapshot` | conexão, cache, lock, query, recurso, incidente |

Os orquestradores (`DatabaseAlertEngine`, `DatabaseRecommendationService`,
`DatabaseHealthScoreService`) recebem a lista por injeção e apenas percorrem — não conhecem
nenhuma regra específica.

**2. Todo achado tem código em enum.** `AlertCode`, `AnomalyCode`, `RecommendationCode`,
`PenaltyCode`, e as severidades `AlertSeverity`, `AnomalySeverity`, `RecommendationPriority`,
`HealthClassification`. Nada de identificar achado por texto.

**3. Limiar é configuração.** `DatabaseIntelligenceProperties` agrupa `AlertProperties`,
`AnomalyProperties` e `ScoreProperties`. Os números que definem o comportamento ficam fora do
código compilado.

## Consequências

- Acrescentar uma regra é criar uma classe; nenhum arquivo existente muda. O risco de regressão
  na adição fica próximo de zero.
- Cada regra é testável em isolamento, com um contexto montado à mão — é o que permite a
  cobertura da camada de inteligência sem subir contexto Spring.
- O frontend reage a **código**, não a frase. Traduzir, reordenar ou reescrever a mensagem não
  quebra a interface.
- Calibrar a ferramenta para um ambiente é editar configuração, não fazer release.
- Os códigos em enum são o vocabulário estável entre backend, frontend e histórico —
  comparações ao longo do tempo continuam válidas mesmo com o texto reescrito.

### Trade-offs aceitos

- **Mais arquivos.** Dezenas de classes pequenas em vez de alguns métodos grandes. A lógica
  fica espalhada, e entender o conjunto exige navegar pelo pacote em vez de ler um arquivo.
- **A ordem de avaliação passa a ser implícita**, definida pela ordem de injeção da lista. Onde
  isso importa — como no resumo do score, que usa as duas primeiras penalidades — a dependência
  é frágil e não está declarada.
- Enum de código é contrato: remover ou renomear constante quebra frontend e invalida o
  histórico. Códigos precisam ser tratados como API pública.
- Limiar em properties significa que o comportamento do sistema não está inteiramente no
  código — reproduzir um diagnóstico exige saber também a configuração vigente.

## Alternativas consideradas

- **Cadeia de `if` num serviço único** — descartado: intestável em unidade, e cada nova regra
  edita o mesmo arquivo que todas as outras.
- **Motor de regras externo (Drools ou similar)** — descartado: dependência pesada e uma segunda
  linguagem para expressar regras que, em Java simples, já são legíveis e testáveis.
- **Mensagens como identificador, sem enum** — descartado: acopla frontend e histórico à
  redação, e qualquer ajuste de texto vira mudança de contrato.
- **Limiares como constantes no código** — descartado para a camada de inteligência: calibração
  por ambiente é caso de uso real. (Notar a inconsistência assumida em
  [ADR-0002](0002-contexto-limitado-e-curado-no-prompt.md): os cortes do prompt continuam
  constantes no código.)
