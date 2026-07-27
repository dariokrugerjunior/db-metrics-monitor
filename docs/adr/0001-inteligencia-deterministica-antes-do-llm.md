# ADR-0001: A inteligência determinística decide; o LLM explica

- **Status:** Aceito
- **Data:** 2026-07-27

## Contexto

A aplicação entrega um diagnóstico operacional de um banco PostgreSQL em produção. Sobre esse
diagnóstico alguém vai agir — encerrar sessão, matar query, mexer em `max_connections`.

O caminho fácil seria despejar as métricas num prompt e pedir ao modelo que diagnostique. Isso
tem três problemas graves para uso operacional:

1. **O veredito fica não determinístico.** A mesma situação pode gerar "OK" numa chamada e
   "CRÍTICO" na seguinte.
2. **Não há como testar.** Não se escreve teste unitário para "o modelo classificou certo".
3. **A alucinação vira incidente.** Um modelo que infere causa sem evidência produz uma
   recomendação plausível e errada sobre um banco de produção.

## Decisão

O diagnóstico é calculado em Java, antes de qualquer chamada à OpenAI. O LLM recebe um
resultado já apurado e sua função é **redigir**, não decidir.

Concretamente, antes do prompt existir a aplicação já computou:

- **Sinais** (`HealthSignalAnalyzer`) — limiares explícitos: conexões em 70% e 85%, cache hit
  em 97% e 99%, query lenta a partir de 100 ms de tempo médio, incidente recorrente a partir
  de 5 ocorrências em 24h.
- **Health score** (`DatabaseHealthScoreService`) — ver [ADR-0005](0005-health-score-explicavel-por-penalidades.md).
- **Anomalias** contra baseline histórica — ver [ADR-0006](0006-anomalias-por-baseline-historica.md).
- **Alertas e recomendações** por regras versionadas — ver [ADR-0007](0007-regras-como-classes-com-codigos-e-limiares-externos.md).

O prompt (`PromptInstructionBuilder`) então trava o comportamento do modelo:

- *"Use apenas o contexto fornecido."*
- *"Não invente dados, causas, tendências ou impactos sem evidência explícita."*
- *"Não extrapole comportamento futuro sem base no snapshot ou no histórico informado."*
- *"Se algum dado estiver ausente, trate como desconhecido."*
- As regras de severidade são repassadas ao modelo com **os mesmos limiares** já aplicados no
  código, e o formato de saída é fixo: veredito, resumo em até 2 frases, até 5 sinais, até 5
  recomendações.

## Consequências

- O veredito é reprodutível e **testável**: a lógica de classificação está em classes puras,
  cobertas por testes unitários (`DatabaseAlertEngineTest`, `DatabaseHealthScoreServiceTest`,
  `DatabaseAnomalyDetectionServiceTest`, entre outros), sem depender da OpenAI.
- Com a IA desligada, o produto continua diagnosticando — a IA agrega leitura, não é o motor.
- A superfície de alucinação fica restrita à *redação*. O modelo não tem espaço para inventar
  um número: os números já vieram calculados.
- O sistema é auditável de ponta a ponta, porque o caminho entre métrica e veredito é código
  que alguém pode ler.

### Trade-offs aceitos

- **O teto de qualidade é o das regras.** O modelo não vai descobrir uma correlação que as
  regras não preveem, porque não é dele que se espera o diagnóstico. Perde-se o "insight
  inesperado" em troca de previsibilidade.
- **Os limiares estão duplicados.** As mesmas fronteiras (70/85% de conexões, 97/99% de cache)
  vivem como constantes em `HealthSignalAnalyzer` e como texto em
  `PromptInstructionBuilder.buildSeverityRulesBlock()`. Mudar um lado e esquecer o outro faz o
  veredito do modelo divergir do veredito do código, sem nada quebrar. Hoje isso é mantido por
  disciplina; a correção natural é derivar o bloco de severidade das mesmas constantes.
- Manter regra determinística custa mais código do que delegar ao modelo, e esse código precisa
  ser mantido conforme o entendimento operacional evolui.

## Alternativas consideradas

- **Mandar as métricas cruas e deixar o modelo diagnosticar** — descartado: entrega veredito
  não determinístico e não testável sobre banco de produção, que é exatamente o caso de uso em
  que isso não é aceitável.
- **Não usar IA, só as regras** — descartado: as regras produzem sinais corretos porém áridos.
  A tradução para linguagem de plantão, priorizando o que olhar primeiro, é o que o LLM faz bem
  e é justamente onde ele não corre risco de inventar fato.
- **Pedir saída estruturada (JSON) ao modelo e validar por schema** — descartado por ora: o
  problema não é o formato da resposta, é a origem do veredito. Validar schema de um
  diagnóstico alucinado apenas o deixa bem formatado.
