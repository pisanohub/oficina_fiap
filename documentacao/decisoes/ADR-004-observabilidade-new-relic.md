# ADR-004 - Adotar New Relic com OpenTelemetry e nri-bundle

- **Estado:** Aceita, implementada e validada na `main`
- **Data da consolidação:** 8 de setembro de 2026
- **Última revisão:** 12 de setembro de 2026
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
- coleta Prometheus validada nos dois pods da Spring API;
- métricas de volume de ordens, transições e duração por status validadas com fluxos reais;
- dashboard com volume diário, tempo médio por status, erros HTTP, latência e recursos do Kubernetes;
- condição de alerta configurada, habilitada e disparada em teste controlado.

## Evidências de implementação

- profile `application-newrelic.yaml`;
- `logback-spring.xml` para logs JSON;
- `RequestCorrelationFilter`;
- Services de aplicação e métricas;
- `k8s/newrelic-values.yaml`;
- instalação do `nri-bundle` por Helm no workflow de deploy.

## Resultado da implementação

A integração foi incorporada à branch `main` e implantada pelo pipeline da aplicação. O `nri-prometheus` foi configurado com `scrape_endpoints: true` e `scrape_services: false`, permitindo coletar separadamente as duas réplicas da Spring API. O fluxo de criação e avanço de uma ordem de serviço foi executado para validar as métricas de negócio, e um erro HTTP controlado foi usado para validar a abertura de alerta no New Relic.
