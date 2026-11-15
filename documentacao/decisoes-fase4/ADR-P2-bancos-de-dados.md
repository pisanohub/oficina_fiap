# ADR-P2 — Banco de dados próprio por microsserviço (RDS compartilhado + DynamoDB)

- **Estado:** Aceita e implementada
- **Data:** 17/10/2026 (plano de verificação concluído em 15/11/2026)
- **Repositórios principais:** `oficina-fiap-infra-database` (RDS); repositório do Execução e Produção Service (DynamoDB)

## Contexto

A Fase 4 exige que cada microsserviço tenha banco de dados próprio, sendo obrigatório o uso de pelo menos um banco relacional (SQL) e um não relacional (NoSQL) no conjunto da solução. Nenhum serviço pode acessar diretamente o banco de outro.

## Decisão

- **OS Service** e **Billing Service** usam **PostgreSQL** numa **única instância Amazon RDS**, com **databases e usuários separados por serviço** — cada serviço só recebe a credencial do próprio database, garantindo isolamento lógico (não físico).
- **Execução e Produção Service** usa **Amazon DynamoDB** (NoSQL), banco próprio e isolado por natureza (tabela dedicada).

## Alternativas consideradas

### Duas instâncias RDS separadas (isolamento físico total)

Isolamento mais forte entre OS e Billing, mas custo e consumo de recursos maiores no ambiente acadêmico — descartada.

### MongoDB auto-hospedado no cluster Kubernetes

Atenderia ao requisito NoSQL, mas adiciona um componente com estado pra administrar no mesmo nó EC2 já identificado como frágil na Fase 3 (sessões de 4h, recursos apertados) — descartada.

### Um único banco PostgreSQL pra tudo

Mais simples, mas não atende ao requisito obrigatório de ter pelo menos um banco não relacional — descartada.

## Consequências

### Positivas

- Menor custo e consumo de recursos que duas instâncias RDS separadas.
- Atende ao requisito obrigatório de pelo menos 1 SQL + 1 NoSQL.
- DynamoDB gerenciado pela AWS reduz carga operacional no cluster (sem precisar administrar réplicas/backup de um banco NoSQL auto-hospedado).

### Negativas e riscos

- Isolamento **lógico**, não físico, entre OS e Billing — um erro de configuração de credencial poderia, em tese, permitir acesso cruzado entre os databases se mal configurado.
- A instância RDS única se torna ponto de contenção de recursos (CPU/IOPS) compartilhado entre os dois serviços.
- Acesso do Execução Service ao DynamoDB depende da `LabRole` da EC2 (não é possível criar roles IAM customizadas no AWS Academy) — precisa ser validado antes de depender dele no cronograma.

## Plano de verificação

- [x] **Isolamento entre databases confirmado (02/11/2026):** usuário do `os_service` recebeu erro de permissão negada ao tentar acessar `billing_service` — acesso cruzado realmente falha.
- [x] **Permissão da `LabRole` no DynamoDB confirmada (09/11/2026, reconfirmada em 15/11/2026 após recriação completa do cluster):** a consulta da outbox do Execução Service ao DynamoDB roda a cada 5 s, sem try/catch ao redor da chamada — qualquer `AccessDenied` apareceria como erro não tratado nos logs, e não apareceu em nenhuma das duas verificações (antes e depois de recriar a EC2 do zero via Terraform).
