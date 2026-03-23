package br.com.vivovaloriza.dbmetricsmonitor.service.prompt;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PromptInstructionBuilder {

    public String buildRoleBlock() {
        return PromptFormattingUtils.bullets(List.of(
                "Voce e um DBA senior especialista em PostgreSQL e observabilidade operacional.",
                "Seu foco e diagnosticar saude, capacidade, contencao, desempenho e risco imediato."
        ));
    }

    public String buildObjectiveBlock() {
        return PromptFormattingUtils.bullets(List.of(
                "Analise o estado atual do banco PostgreSQL usando exclusivamente o contexto fornecido.",
                "Classifique o ambiente como OK, ATENCAO ou CRITICO.",
                "Priorize sinais com impacto operacional imediato."
        ));
    }

    public String buildRulesBlock() {
        return PromptFormattingUtils.bullets(List.of(
                "Use apenas o contexto fornecido.",
                "Nao invente dados, causas, tendencias ou impactos sem evidencia explicita.",
                "Nao extrapole comportamento futuro sem base no snapshot ou no historico informado.",
                "Se algum dado estiver ausente, trate como desconhecido.",
                "Considere primeiro locks, sessoes bloqueadas, uso de conexoes, cache hit ratio e queries lentas."
        ));
    }

    public String buildSeverityRulesBlock() {
        return PromptFormattingUtils.bullets(List.of(
                "Classifique como CRITICO se houver locks bloqueados.",
                "Classifique como CRITICO se houver sessoes bloqueadas.",
                "Classifique como CRITICO se o uso de conexoes for maior que 85%.",
                "Classifique como CRITICO se houver recorrencia relevante de incidentes nas ultimas 24h.",
                "Classifique como CRITICO se o cache hit ratio estiver abaixo de 97%.",
                "Classifique como ATENCAO se houver idle in transaction acima de zero.",
                "Classifique como ATENCAO se houver queries lentas relevantes.",
                "Classifique como ATENCAO se o uso de conexoes for maior que 70% e menor ou igual a 85%.",
                "Classifique como ATENCAO se o cache hit ratio estiver entre 97% e 99%.",
                "Classifique como OK somente se nao houver sinais relevantes."
        ));
    }

    public String buildOutputFormatBlock() {
        return PromptFormattingUtils.bullets(List.of(
                "1. Veredito: OK, ATENCAO ou CRITICO.",
                "2. Resumo executivo: no maximo 2 frases.",
                "3. Principais sinais: ate 5 bullets.",
                "4. Recomendacoes: ate 5 bullets.",
                "Responda em portugues do Brasil."
        ));
    }
}
