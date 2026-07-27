# Registros de Decisão de Arquitetura (ADR)

Registros curtos das decisões que moldaram este sistema: o que foi decidido, por quê, o que
custa e o que foi descartado. Cada registro é imutável — se uma decisão mudar, um novo ADR
substitui o antigo em vez de reescrever a história.

Formato: [MADR](https://adr.github.io/madr/) reduzido — Contexto, Decisão, Consequências,
Alternativas consideradas.

| # | Decisão | Status |
|---|---------|--------|
| [0001](0001-inteligencia-deterministica-antes-do-llm.md) | A inteligência determinística decide; o LLM explica | Aceito |
| [0002](0002-contexto-limitado-e-curado-no-prompt.md) | Enviar ao modelo um snapshot curado e limitado, não o dado bruto | Aceito |
| [0003](0003-persistir-toda-analise-com-o-contexto.md) | Persistir toda análise de IA junto com o contexto que a gerou | Aceito |
| [0004](0004-ia-opcional-que-falha-alto.md) | A IA é um recurso opcional que falha alto, nunca em silêncio | Aceito |
| [0005](0005-health-score-explicavel-por-penalidades.md) | Health score é 100 menos penalidades explicáveis | Aceito |
| [0006](0006-anomalias-por-baseline-historica.md) | Detectar anomalias por baseline histórica, não por limiar fixo | Aceito |
| [0007](0007-regras-como-classes-com-codigos-e-limiares-externos.md) | Regras como classes, com códigos em enum e limiares em properties | Aceito |
| [0008](0008-chamada-direta-a-api-da-openai.md) | Chamar a API da OpenAI direto por HttpClient, sem SDK | Aceito |
| [0009](0009-analise-serializada-por-banco.md) | Serializar a análise de IA por banco monitorado | Aceito |
