# ADR-003 - Escalar a Spring API horizontalmente com HPA

- **Estado:** Aceita e implementada
- **Data da consolidação:** 8 de setembro de 2026
- **Repositório principal:** `oficina_fiap`

## Contexto

A aplicação precisa suportar variações de carga e demonstrar escalabilidade no Kubernetes. Os pods já possuem requests e limits de CPU e memória, possibilitando o cálculo de utilização pelo HPA.

## Decisão

Usar `HorizontalPodAutoscaler` com API `autoscaling/v2` para o Deployment `spring-api`:

- mínimo de 1 réplica;
- máximo de 3 réplicas;
- alvo médio de CPU em 70%;
- alvo médio de memória em 80%;
- requests por pod de 200 millicores e 256 MiB;
- limits por pod de 500 millicores e 512 MiB;
- readiness e liveness probes pelos endpoints do Actuator.

## Alternativas consideradas

### Quantidade fixa de réplicas

É previsível, mas não reage automaticamente à variação de consumo.

### Escalabilidade vertical

Altera recursos do pod, mas pode exigir reinicialização e não demonstra distribuição horizontal das requisições.

### Escalabilidade baseada somente em CPU

É mais simples, porém pode ignorar pressão de memória relevante para uma aplicação Java.

## Consequências

### Positivas

- reação automática a CPU e memória;
- possibilidade de reduzir recursos quando a carga cai;
- compatibilidade com o requisito de escalabilidade;
- probes evitam direcionar tráfego a pods ainda indisponíveis.

### Negativas e riscos

- depende de métricas de recursos disponíveis no cluster;
- em nó único, a quantidade máxima de pods continua limitada à capacidade da EC2;
- `minReplicas=1` não oferece redundância quando o HPA reduz ao mínimo;
- valores precisam ser validados com carga real.

## Evidências de implementação

- manifesto `k8s/hpa.yaml`;
- requests e limits em `k8s/app-deployment.yaml`;
- endpoints de liveness e readiness do Spring Boot Actuator.
