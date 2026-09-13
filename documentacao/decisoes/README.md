# RFCs e ADRs consolidados - Oficina FIAP - Fase 3

Este índice reúne as propostas técnicas e as decisões arquiteturais que afetam os quatro repositórios da solução.

Data da consolidação: **12 de setembro de 2026**.

## Como interpretar os documentos

- **RFC (Request for Comments):** proposta que ainda precisa de discussão ou confirmação da equipe.
- **ADR (Architecture Decision Record):** decisão arquitetural aceita, acompanhada de contexto, alternativas e consequências.
- Uma RFC aprovada deve originar uma ADR ou atualizar uma ADR existente.
- Uma ADR não deve ser alterada para esconder uma decisão antiga; mudanças relevantes devem gerar uma nova ADR que substitua a anterior.

## Estado consolidado

| Registro | Assunto | Estado |
|---|---|---|
| [RFC-001](RFC-001-autenticacao-cpf-jwt-api-gateway.md) | Autenticação por CPF, contrato JWT e API Gateway | Aprovada, implementada e validada |
| [RFC-002](RFC-002-exposicao-api-gateway-k3s.md) | Exposição da Spring API para o API Gateway | Aprovada, implementada e validada |
| [ADR-001](ADR-001-k3s-em-ec2.md) | Kubernetes K3s em uma instância EC2 | Aceita e implementada |
| [ADR-002](ADR-002-postgresql-rds.md) | PostgreSQL 16 no Amazon RDS | Aceita e implementada |
| [ADR-003](ADR-003-escalabilidade-hpa.md) | Escalabilidade horizontal com HPA | Aceita e implementada |
| [ADR-004](ADR-004-observabilidade-new-relic.md) | New Relic, OpenTelemetry e `nri-bundle` | Aceita, implementada e validada na `main` |
| [ADR-005](ADR-005-repositorios-e-cicd.md) | Quatro repositórios e pipelines independentes | Aceita e implementada |

## Decisões implementadas e validadas

### Contrato JWT

A Lambda de autenticação atualmente emite um token com:

- `sub`: CPF;
- `clienteId`;
- `tipo=CLIENTE`;
- `ativo=true`;
- `iat` e `exp`.

Durante a implementação, a aplicação principal foi ajustada para reconhecer esse contrato de cliente separadamente do token administrativo. O `JWT_SECRET` foi configurado com o mesmo valor nos componentes envolvidos.

O fluxo CPF, emissão do token, Lambda Authorizer e acesso à rota protegida foi validado de ponta a ponta.

### Acesso do API Gateway à aplicação

O API Gateway encaminha as rotas protegidas para `${APP_BASE_URL}/api/{proxy}`. O Ingress Traefik expõe a Spring API pela porta 80 da EC2 e encaminha as chamadas ao Service `spring-api`. O IP pode mudar após a recriação do ambiente e, nesse caso, `APP_BASE_URL` deve ser atualizada.

### Deploy da Lambda

O deploy foi habilitado depois da configuração de subnets, Security Group, endpoint do RDS, credenciais temporárias do AWS Academy, backend Terraform, credenciais do banco, `JWT_SECRET` e `APP_BASE_URL`. A Lambda, o Authorizer e o API Gateway foram implantados e testados.

### Observabilidade

A integração do New Relic está na branch `main`. Foram validados traces, logs estruturados, métricas da aplicação e do Kubernetes, coleta Prometheus das duas réplicas, dashboards de negócio e uma condição de alerta disparada por erro HTTP controlado.

## Relação com os quatro repositórios

| Repositório | Decisões relacionadas |
|---|---|
| [`oficina_fiap`](https://github.com/pisanohub/oficina_fiap) | Spring API, Kubernetes, HPA, JWT, observabilidade e deploy com `kubectl`/Helm |
| [`oficina-fiap-infra-k8s`](https://github.com/pisanohub/oficina-fiap-infra-k8s) | EC2, K3s, rede, Security Group e estado Terraform |
| [`oficina-fiap-infra-database`](https://github.com/pisanohub/oficina-fiap-infra-database) | RDS PostgreSQL, subnets, Security Group e estado Terraform |
| [`oficina-fiap-lambda-auth`](https://github.com/pisanohub/oficina-fiap-lambda-auth) | Autenticação CPF, JWT, Lambda Authorizer, API Gateway e HTTP Proxy |

## Condição para considerar a entrega documental concluída

As decisões técnicas, a autenticação ponta a ponta e a observabilidade já foram validadas. Para concluir a entrega documental, ainda é necessário incorporar ou referenciar os documentos finais produzidos pelos responsáveis por autenticação, Kubernetes e banco, reunir as evidências visuais e gerar o PDF único da fase.
