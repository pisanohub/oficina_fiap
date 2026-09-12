# RFCs e ADRs consolidados - Oficina FIAP - Fase 3

Este índice reúne as propostas técnicas e as decisões arquiteturais que afetam os quatro repositórios da solução.

Data da consolidação: **8 de setembro de 2026**.

## Como interpretar os documentos

- **RFC (Request for Comments):** proposta que ainda precisa de discussão ou confirmação da equipe.
- **ADR (Architecture Decision Record):** decisão arquitetural aceita, acompanhada de contexto, alternativas e consequências.
- Uma RFC aprovada deve originar uma ADR ou atualizar uma ADR existente.
- Uma ADR não deve ser alterada para esconder uma decisão antiga; mudanças relevantes devem gerar uma nova ADR que substitua a anterior.

## Estado consolidado

| Registro | Assunto | Estado |
|---|---|---|
| [RFC-001](RFC-001-autenticacao-cpf-jwt-api-gateway.md) | Autenticação por CPF, contrato JWT e API Gateway | Em discussão - contrato JWT incompatível |
| [RFC-002](RFC-002-exposicao-api-gateway-k3s.md) | Exposição da Spring API para o API Gateway | Em discussão - endpoint não confirmado |
| [ADR-001](ADR-001-k3s-em-ec2.md) | Kubernetes K3s em uma instância EC2 | Aceita e implementada |
| [ADR-002](ADR-002-postgresql-rds.md) | PostgreSQL 16 no Amazon RDS | Aceita e implementada |
| [ADR-003](ADR-003-escalabilidade-hpa.md) | Escalabilidade horizontal com HPA | Aceita e implementada |
| [ADR-004](ADR-004-observabilidade-new-relic.md) | New Relic, OpenTelemetry e `nri-bundle` | Aceita; integração aguarda incorporação definitiva à `main` |
| [ADR-005](ADR-005-repositorios-e-cicd.md) | Quatro repositórios e pipelines independentes | Aceita; deploy serverless aguarda habilitação |

## Bloqueios que precisam de decisão da equipe

### Contrato JWT

A Lambda de autenticação atualmente emite um token com:

- `sub`: CPF;
- `clienteId`;
- `tipo=CLIENTE`;
- `ativo=true`;
- `iat` e `exp`.

A branch `feature/atualizacao-jwt` da aplicação principal espera outro formato:

- `sub`: nome do usuário administrativo;
- claim `cpf`;
- consulta do usuário por `username` durante a autenticação.

Mesmo que os dois projetos usem o mesmo `JWT_SECRET`, esse contrato diferente impede a autenticação de cliente de funcionar ponta a ponta.

### Acesso do API Gateway à aplicação

O API Gateway encaminha as rotas protegidas para `${APP_BASE_URL}/api/{proxy}`. O Kubernetes possui um Service `ClusterIP`, acessível somente dentro do cluster, e não há no repositório um Ingress confirmado para fornecer a URL pública. A porta 80 da EC2 está liberada, mas ainda é necessário configurar e testar o roteamento pelo Traefik ou escolher outra solução.

### Deploy da Lambda

O pipeline da Lambda somente executa o deploy quando `DEPLOY_ENABLED=true`. Antes disso, a equipe precisa fornecer subnets, Security Group, endpoint do RDS, credenciais temporárias do AWS Academy, estado Terraform e `APP_BASE_URL`.

## Relação com os quatro repositórios

| Repositório | Decisões relacionadas |
|---|---|
| [`oficina_fiap`](https://github.com/pisanohub/oficina_fiap) | Spring API, Kubernetes, HPA, JWT, observabilidade e deploy com `kubectl`/Helm |
| [`oficina-fiap-infra-k8s`](https://github.com/pisanohub/oficina-fiap-infra-k8s) | EC2, K3s, rede, Security Group e estado Terraform |
| [`oficina-fiap-infra-database`](https://github.com/pisanohub/oficina-fiap-infra-database) | RDS PostgreSQL, subnets, Security Group e estado Terraform |
| [`oficina-fiap-lambda-auth`](https://github.com/pisanohub/oficina-fiap-lambda-auth) | Autenticação CPF, JWT, Lambda Authorizer, API Gateway e HTTP Proxy |

## Condição para considerar a consolidação concluída

Antes da entrega final, este índice deve ser revisado depois que:

1. a equipe aprovar um único contrato JWT;
2. a aplicação estiver acessível pelo API Gateway;
3. o fluxo CPF, token e API protegida funcionar ponta a ponta;
4. os documentos produzidos pelos responsáveis por autenticação, Kubernetes e banco forem incorporados ou referenciados;
5. os dashboards e alertas do New Relic forem validados.
