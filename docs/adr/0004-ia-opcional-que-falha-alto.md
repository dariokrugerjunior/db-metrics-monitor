# ADR-0004: A IA é um recurso opcional que falha alto, nunca em silêncio

- **Status:** Aceito
- **Data:** 2026-07-27

## Contexto

A análise por IA depende de coisas que podem faltar: a chave da OpenAI pode não estar
configurada, o recurso pode estar desligado no ambiente, a API externa pode estar fora,
devolver erro HTTP ou responder sem conteúdo aproveitável.

Numa ferramenta de observabilidade, a pior reação a essa falha é continuar respondendo alguma
coisa. Um texto genérico exibido como se fosse diagnóstico é pior que erro nenhum — quem está
de plantão trata como informação.

## Decisão

A IA é opcional e explicitamente separada do resto do produto:

- **Desligável por configuração.** Se `appProperties.ai.enabled` for falso, a análise recusa a
  requisição com mensagem clara.
- **Sem chave, sem análise.** Se a chave efetiva estiver ausente ou em branco, a resposta é
  erro de requisição informando exatamente isso — não há degradação para um texto padrão.
- **Erro externo vira erro.** Status HTTP fora da faixa 2xx, falha de I/O, interrupção ou
  resposta sem texto analisável levantam `ExternalIntegrationException`.
- **Nada de resposta vazia disfarçada.** Se a OpenAI responder sem conteúdo, a leitura tenta o
  campo `output_text` e depois percorre `output[].content[].text`; se ainda assim não houver
  texto, é erro — não uma análise em branco.

O restante da aplicação — coleta, dashboard, health score, anomalias, alertas, recomendações,
histórico de incidentes — funciona integralmente com a IA desligada.

## Consequências

- É possível rodar a ferramenta inteira sem chave de OpenAI e sem qualquer chamada externa.
  Para quem não pode mandar dado de banco para fora, o produto continua útil.
- A separação entre "o que o sistema calculou" e "o que o modelo escreveu" fica visível também
  no comportamento de falha, reforçando [ADR-0001](0001-inteligencia-deterministica-antes-do-llm.md).
- Toda falha da integração chega ao usuário como falha, com mensagem específica, através do
  `GlobalExceptionHandler`.
- O tratamento correto de `InterruptedException` (restaurar o flag de interrupção antes de
  propagar) evita engolir sinal de cancelamento de thread.

### Trade-offs aceitos

- **Não há fallback nem retry.** Uma indisponibilidade momentânea da OpenAI resulta em erro
  imediato para o usuário, que precisa repetir a ação. É preferível a fingir sucesso, mas é
  menos resiliente do que uma política de repetição com backoff.
- Timeouts são fixos no código (20s para conectar, 60s para a requisição), não configuráveis
  por ambiente.
- Sem circuit breaker: se a API externa estiver degradada, cada tentativa paga o timeout
  inteiro antes de falhar.

## Alternativas consideradas

- **Cair para uma análise textual gerada localmente quando a IA falha** — descartado: produz
  algo com cara de diagnóstico de IA que não é, e confunde quem lê. Os sinais determinísticos
  já estão disponíveis em outras telas, identificados como o que são.
- **Deixar a IA obrigatória** — descartado: tornaria a ferramenta inutilizável em ambiente sem
  saída para internet ou sem verba de API, para um recurso que é complementar por design.
- **Retornar 200 com corpo vazio quando não há chave** — descartado: erro silencioso é a falha
  mais cara de diagnosticar justamente numa ferramenta de diagnóstico.
