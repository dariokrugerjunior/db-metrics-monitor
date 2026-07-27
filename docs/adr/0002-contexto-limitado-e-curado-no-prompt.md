# ADR-0002: Enviar ao modelo um snapshot curado e limitado, não o dado bruto

- **Status:** Aceito
- **Data:** 2026-07-27

## Contexto

Um PostgreSQL sob carga produz muito mais dado do que cabe — ou do que convém — num prompt:
centenas de conexões, dezenas de queries em execução, `pg_stat_statements` inteiro, e a lista
completa de settings do servidor.

Mandar tudo tem três custos simultâneos: token (o contexto cresce sem limite superior),
qualidade (o sinal relevante se dilui no ruído) e risco (texto de query pode carregar valor
sensível).

## Decisão

O que vai ao modelo é um `DatabaseHealthSnapshot` montado explicitamente em
`AiAnalysisService.buildSnapshot`, com corte deliberado em cada dimensão:

| Dimensão | Regra de corte |
|----------|----------------|
| Locks bloqueados | os **5** de maior duração |
| Queries em execução | as **5** mais longas |
| Top queries | as **5** de maior tempo médio |
| Settings do PostgreSQL | apenas **8**, por allowlist |
| Texto de query | truncado (`PromptFormattingUtils.truncateQuery`) |
| Números | arredondados a 2 casas; durações já formatadas em texto |

A allowlist de settings é fechada e explícita: `max_connections`, `shared_buffers`, `work_mem`,
`maintenance_work_mem`, `effective_cache_size`, `statement_timeout`, `lock_timeout` e
`idle_in_transaction_session_timeout`.

Além do recorte, o snapshot chega **pré-classificado**: o cache hit ratio, por exemplo, já vai
rotulado como `saudavel` / `atencao` / `critico` pelos limiares de 99% e 97%, em vez de ir como
número solto para o modelo interpretar.

## Consequências

- O tamanho do prompt tem teto conhecido, independente de o banco ter 10 ou 10 mil conexões.
  Custo e latência ficam previsíveis.
- O modelo recebe os itens que realmente importam para o diagnóstico — os mais lentos, os mais
  longos, os que travam — em vez de uma amostra arbitrária.
- Truncar o texto das queries reduz a chance de literal com dado sensível atravessar a
  fronteira do processo rumo a um serviço externo.
- Enviar rótulo em vez de número cru diminui o espaço para o modelo "reinterpretar" um valor
  que o código já classificou ([ADR-0001](0001-inteligencia-deterministica-antes-do-llm.md)).

### Trade-offs aceitos

- **Corte fixo pode esconder o item relevante.** Se 12 locks estão bloqueando, o modelo vê 5.
  A escolha é ordenar por gravidade (duração) e aceitar que a cauda fique de fora — o objetivo
  é diagnóstico, não inventário.
- O `5` e o `2` casas decimais são constantes no código, não configuração. Ajustar exige
  recompilar.
- A allowlist de settings precisa de manutenção manual: um parâmetro relevante que não esteja
  nela é invisível para a análise, e nada avisa.
- Truncar query reduz risco, **não elimina**: um trecho sensível pode caber no prefixo mantido.
  Quem apontar a ferramenta para um banco com dado regulado precisa saber disso.

## Alternativas consideradas

- **Mandar o dump completo do estado** — descartado: contexto sem limite superior, custo
  imprevisível e diluição do sinal.
- **Deixar o modelo pedir mais dados (tool calling / function calling)** — descartado por ora:
  transforma uma chamada previsível num diálogo de várias idas e vindas, com custo e latência
  variáveis, para um caso em que o conjunto de dados relevante já é conhecido de antemão.
- **Resumir o contexto com um segundo modelo antes da análise** — descartado: adiciona uma
  etapa não determinística *antes* da etapa não determinística, exatamente o oposto do que
  [ADR-0001](0001-inteligencia-deterministica-antes-do-llm.md) estabelece.
