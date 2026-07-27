# ADR-0003: Persistir toda análise de IA junto com o contexto que a gerou

- **Status:** Aceito
- **Data:** 2026-07-27

## Contexto

Uma análise gerada por LLM é uma recomendação sobre um banco de produção. Se alguém agiu com
base nela e o resultado foi ruim, três perguntas aparecem imediatamente:

- O que exatamente foi dito?
- Com base em quais dados?
- Qual modelo respondeu?

Sem registro, nenhuma dessas perguntas tem resposta: o estado do banco já mudou, o prompt foi
descartado e a saída do modelo não é reproduzível nem repetindo a chamada.

## Decisão

Toda análise é persistida com o conjunto completo do que a produziu
(`AiAnalysisHistoryService.save`):

- o **modelo** utilizado;
- o **prompt do usuário**, quando houver;
- o **contexto integral** enviado — o mesmo texto montado por `DatabaseHealthPromptBuilder`;
- a **resposta** do modelo;
- o **instante** da geração.

O histórico é exposto pela aplicação, não fica só na tabela: `AiAnalysisController` oferece
`GET /history` (escopado ao banco atualmente conectado, via
`AiAnalysisHistoryService.getCurrentDatabaseHistory`) e `DELETE /history` para expurgo.

## Consequências

- Uma recomendação pode ser auditada depois: dá para reler o snapshot que a originou e julgar
  se a conclusão fazia sentido com o que se sabia naquele momento.
- Fica possível avaliar o comportamento do modelo ao longo do tempo — inclusive perceber
  regressão quando a versão do modelo muda sob o mesmo nome.
- O par (contexto, resposta) forma um conjunto de casos reais, útil para ajustar as instruções
  do prompt com evidência em vez de impressão.
- Guardar o `model` deixa explícito que respostas de épocas diferentes podem não ser
  comparáveis entre si.

### Trade-offs aceitos

- **A tabela cresce, e cresce com texto grande.** Cada linha carrega o contexto inteiro. O
  expurgo existe, mas é manual (`DELETE /history`) — não há política de retenção automática,
  então num uso intenso o volume depende de alguém lembrar de limpar.
- **O contexto persistido carrega o que o snapshot carregava**, incluindo trechos truncados de
  query. O histórico herda a mesma consideração de sensibilidade descrita em
  [ADR-0002](0002-contexto-limitado-e-curado-no-prompt.md), agora em repouso e não só em trânsito.
- Persistir antes de responder acrescenta uma escrita ao caminho da requisição — irrelevante
  perto dos segundos gastos na chamada ao modelo, mas é uma escrita a mais.

## Alternativas consideradas

- **Guardar só a resposta** — descartado: sem o contexto, a resposta é inauditável. "O modelo
  disse para matar a sessão 4211" não significa nada sem o estado que ele viu.
- **Só registrar em log** — descartado: log rotaciona, não é consultável pela interface e não
  sustenta a tela de histórico que o produto oferece.
- **Não persistir nada** — descartado: elimina a possibilidade de auditoria, que é justamente o
  que separa "IA em produção" de "IA em demo".
