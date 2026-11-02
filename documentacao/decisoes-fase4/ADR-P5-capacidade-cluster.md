# ADR-P5 — Upgrade de capacidade do cluster Kubernetes (t3.medium → t3.large)

- **Estado:** Aceita, implementação pendente
- **Data:** 17/10/2026
- **Repositório principal:** `oficina-fiap-infra-k8s`

## Contexto

Na Fase 3, o cluster K3s rodava uma única aplicação (2 réplicas) mais os agentes do New Relic, num nó EC2 `t3.medium` (4 GB de RAM). Na Fase 4, o mesmo nó único vai rodar 3 serviços Spring Boot independentes simultaneamente — cada um com seu próprio processo JVM, potencialmente múltiplas réplicas — além dos mesmos agentes de observabilidade. Isso aumenta de forma real a carga de memória e CPU sobre o mesmo nó.

## Decisão

Subir o tipo da instância EC2 de `t3.medium` (4 GB) para **`t3.large` (8 GB)** no Terraform do `oficina-fiap-infra-k8s`, e definir `requests`/`limits` de memória explícitos em cada Deployment dos 3 serviços — para evitar que um serviço sozinho esgote a memória do nó e derrube os demais (`OOMKilled`).

## Alternativas consideradas

### Manter `t3.medium` e reduzir réplicas/recursos ao mínimo

Evita custo extra, mas aumenta bastante o risco de instabilidade sob qualquer carga real de teste ou demonstração em vídeo — descartada.

### Cluster multi-nó (vários EC2)

Mais próximo de um ambiente de produção real, mas aumenta a complexidade de rede e gerenciamento, fora do escopo de tempo da Fase 4 — descartada.

### Serviço gerenciado tipo Amazon EKS

Mesmo motivo já registrado no ADR-001 da Fase 3: custo e complexidade desproporcionais ao ambiente acadêmico — descartada.

## Consequências

### Positivas

- Margem de memória maior reduz o risco de `OOMKilled` com 3 serviços + observabilidade rodando ao mesmo tempo.
- Continua sendo um nó único, sem aumento de complexidade operacional em relação à Fase 3.

### Negativas e riscos

- Continua sendo **ponto único de falha** — sem alta disponibilidade do control plane, mesma limitação já registrada no ADR-001 da Fase 3.
- É preciso confirmar que o AWS Academy Learner Lab da conta do grupo **permite** esse tipo de instância antes de contar com ela no planejamento — nem toda conta do Academy libera todos os tipos.

## Plano de verificação

- Tentar provisionar uma instância `t3.large` via Terraform logo na Semana 0, antes de depender dela pro restante do cronograma.
- Se não for permitida pela conta do Academy, este ADR precisa ser revisado com uma alternativa (ex.: reduzir réplicas por serviço, ou tentar outro tipo de instância intermediário).
