# ADR-004 - Adotar New Relic com OpenTelemetry e nri-bundle

- **Estado:** Aceita; integração aguarda incorporação definitiva à `main`
- **Data da consolidação:** 8 de setembro de 2026
- **Repositório principal:** `oficina_fiap`

## Contexto

O Tech Challenge exige monitoramento de latência, recursos Kubernetes, healthchecks, uptime, falhas de ordens, logs JSON correlacionados, dashboards e alertas.

## Decisão

Usar New Relic como plataforma central de observabilidade, combinando:

- OpenTelemetry na Spring API para exportar traces por OTLP;
- Spring Boot Actuator e Micrometer para métricas da aplicação;
- logs estruturados em JSON com `requestId`, `trace.id` e `span.id`;
- `RequestCorrelationFilter` para propagar ou criar `X-Request-Id`;
- chart Helm `nri-bundle` para métricas, eventos e logs do Kubernetes;
- integração Prometheus para coletar o endpoint de métricas da aplicação;
- dashboards NRQL e condições de alerta no New Relic.

A license key é fornecida ao deploy por secret do GitHub e não deve aparecer nos manifests versionados.

## Alternativas consideradas

### Agente Java proprietário do New Relic

Oferece instrumentação integrada, mas aumenta o acoplamento da aplicação ao fornecedor. OpenTelemetry foi escolhido para os traces por ser um padrão aberto.

### Prometheus, Grafana e Loki autogerenciados

Reduz dependência de SaaS, mas exige provisionamento, armazenamento, atualização e disponibilidade de várias ferramentas.

### Datadog

Atenderia aos requisitos, mas a equipe já validou ingestão e dashboards no New Relic.

## Consequências

### Positivas

- traces, métricas e logs centralizados;
- correlação entre requisição e telemetria;
- visibilidade de CPU, memória, pods e deployments;
- alertas e dashboards sem manter uma pilha completa no cluster;
- OpenTelemetry reduz acoplamento da instrumentação de traces.

### Negativas e riscos

- dependência de conectividade externa com o New Relic;
- uso e retenção sujeitos aos limites da conta;
- instalação do `nri-bundle` adiciona consumo ao nó K3s;
- queries de negócio dependem da emissão correta das métricas customizadas.

## Evidências já obtidas

- traces do serviço `tc-oficina` enviados localmente;
- pods do `nri-bundle` executados no namespace `newrelic`;
- métricas de CPU e memória do Kubernetes no dashboard;
- logs com campos de cluster, namespace, container, pod, serviço e correlação;
- condição e workflow de alerta configurados.

## Evidências de implementação

- profile `application-newrelic.yaml`;
- `logback-spring.xml` para logs JSON;
- `RequestCorrelationFilter`;
- Services de aplicação e métricas;
- `k8s/newrelic-values.yaml`;
- instalação do `nri-bundle` por Helm no workflow de deploy.
