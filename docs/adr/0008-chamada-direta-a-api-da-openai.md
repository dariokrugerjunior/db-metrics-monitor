# ADR-0008: Chamar a API da OpenAI direto por HttpClient, sem SDK

- **Status:** Aceito
- **Data:** 2026-07-27

## Contexto

A aplicação faz exatamente **uma** operação contra a OpenAI: enviar um texto de contexto já
montado e receber um texto de volta. Sem streaming, sem tool calling, sem embeddings, sem
threads, sem upload de arquivo.

Para isso existe um leque de opções: SDK oficial, um framework de orquestração de LLM
(Spring AI, LangChain4j), ou HTTP puro.

## Decisão

Chamada direta com o `HttpClient` do próprio JDK contra o endpoint `/responses`, com o corpo
montado via Jackson e a resposta lida também com Jackson.

Detalhes que a decisão implica assumir explicitamente:

- **Timeouts próprios** — 20s para conectar, 60s para a requisição.
- **Autenticação** por header `Authorization: Bearer`, com a chave vinda de
  `RuntimeConfigurationService.getEffectiveOpenAiApiKey()` (configurável em runtime, não só por
  variável de ambiente).
- **Leitura tolerante da resposta** — tenta `output_text` e, se vier vazio, percorre
  `output[].content[].text`, porque o formato admite as duas formas.
- **`max_output_tokens`** enviado a partir da configuração efetiva, então o custo por chamada
  tem teto conhecido.

## Consequências

- Zero dependência nova no `pom.xml` para a integração — menos superfície de CVE, menos
  conflito de versão transitiva, build mais leve.
- O contrato com o provedor externo fica **visível**: dá para ler num arquivo exatamente o que
  é enviado e o que é lido, sem atravessar camadas de abstração de framework.
- Erro de integração é tratado onde acontece, com exceção própria do domínio da aplicação
  ([ADR-0004](0004-ia-opcional-que-falha-alto.md)), em vez de exceção genérica de biblioteca.
- Trocar `max_output_tokens`, timeout ou modelo é edição local, sem depender de o framework
  expor a opção.

### Trade-offs aceitos

- **O acoplamento ao formato da OpenAI é nosso.** Se o formato de resposta mudar, o parsing
  quebra e a correção é manual — um SDK absorveria isso numa atualização de versão.
- **Trocar de provedor exigirá trabalho.** Não há camada de abstração sobre "modelo": apontar
  para Anthropic, Gemini ou um modelo local significa reescrever `callOpenAi`. Aceitável
  enquanto houver um provedor só; é o primeiro ponto a revisitar se isso mudar.
- Recursos avançados (streaming da resposta, tool calling, retentativa com backoff) teriam de
  ser implementados à mão se um dia forem necessários.
- Não há métrica nem tracing da chamada externa além do log — um SDK ou framework em geral traz
  instrumentação pronta.

## Alternativas consideradas

- **SDK oficial da OpenAI** — descartado: dependência e ciclo de atualização para cobrir uma
  única chamada HTTP, cujo formato já é simples e estável.
- **Spring AI / LangChain4j** — descartado: são feitos para orquestração (cadeias, memória,
  RAG, ferramentas). Aqui não há orquestração alguma — a montagem do contexto é determinística e
  já é responsabilidade do pacote `service.prompt`. Seria abstração sem problema correspondente.
- **Serviço intermediário próprio para IA** — descartado: acrescentaria um deploy e um salto de
  rede para uma aplicação que hoje é um backend só.
