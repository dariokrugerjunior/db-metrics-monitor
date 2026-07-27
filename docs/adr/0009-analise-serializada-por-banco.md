# ADR-0009: Serializar a análise de IA por banco monitorado

- **Status:** Aceito
- **Data:** 2026-07-27

## Contexto

Uma análise de IA é a operação mais cara da aplicação: coleta dashboard, locks bloqueados,
histórico de incidentes e cache hit ratio, monta o contexto e espera até 60 segundos pela
OpenAI.

Se duas pessoas clicarem em "analisar" ao mesmo tempo — ou se alguém clicar duas vezes — o
resultado é consulta duplicada no banco monitorado (justamente o banco que pode estar sob
estresse, que é o motivo de estarem analisando) e chamada duplicada paga à API, para produzir
duas análises praticamente idênticas.

## Decisão

A análise é serializada **por banco monitorado**. O `AiAnalysisService` mantém um mapa de locks
por URL de conexão:

```java
ConcurrentHashMap<String, ReentrantLock> ANALYSIS_LOCKS;
...
ANALYSIS_LOCKS.computeIfAbsent(dbUrlAdmin, ignored -> new ReentrantLock(true));
```

O lock é **justo** (`fair = true`), e todo o trecho — coleta, montagem do contexto, chamada à
OpenAI e persistência do histórico — roda dentro dele, com liberação garantida em `finally`.

A granularidade é a chave da decisão: o lock é por URL de banco, não global. Duas conexões
diferentes analisam em paralelo; a mesma conexão, não.

## Consequências

- O banco monitorado não recebe rajada de consultas de diagnóstico concorrentes exatamente
  quando está sob pressão.
- O custo de API fica limitado a uma análise em andamento por banco.
- Requisições concorrentes não falham: elas **esperam** e recebem uma análise atual, o que é o
  comportamento esperado por quem clicou.
- O lock justo garante ordem de chegada, evitando que uma requisição fique indefinidamente para
  trás sob concorrência contínua.
- Monitorar vários bancos continua paralelo — a decisão não sacrifica escala entre conexões
  distintas.

### Trade-offs aceitos

- **A espera pode ser longa.** Quem chega durante uma análise em curso pode aguardar perto de
  um minuto sem sinalização específica — não há resposta "já existe uma análise em andamento",
  nem fila visível, nem timeout de aquisição do lock.
- **O mapa de locks só cresce.** Cada URL de banco já vista deixa um `ReentrantLock`
  residente, sem remoção. Irrelevante para dezenas de conexões, é vazamento lento num cenário de
  muitas conexões distintas ao longo do tempo.
- **A garantia é por instância.** Sendo lock em memória, dois processos da aplicação apontando
  para o mesmo banco analisariam em paralelo. Escalar horizontalmente exigiria lock distribuído
  — por exemplo em Redis.
- Uma thread fica bloqueada durante toda a chamada externa, em vez de liberar o worker; num
  volume alto de requisições isso consome thread do pool.

## Alternativas consideradas

- **Nenhum controle de concorrência** — descartado: duplica carga sobre um banco possivelmente
  em estresse e duplica custo de API para produzir a mesma resposta.
- **Lock global (`synchronized` no método)** — descartado: serializaria bancos independentes
  entre si sem necessidade, degradando o uso multi-conexão.
- **Rejeitar a segunda chamada com 409** — considerado e não adotado: exigiria a interface
  tratar o conflito; esperar e devolver a análise é mais simples para quem usa. Continua sendo
  a alternativa natural caso a espera se mostre longa demais na prática.
- **Cache de análise com TTL** — descartado por ora: a análise reflete um instante do banco, e
  servir resultado de 30 segundos atrás como se fosse atual é enganoso numa ferramenta de
  diagnóstico.
