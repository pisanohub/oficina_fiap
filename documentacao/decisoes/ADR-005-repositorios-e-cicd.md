# ADR-005 - Separar a solução em quatro repositórios e pipelines

- **Estado:** Aceita e implementada
- **Data da consolidação:** 8 de setembro de 2026
- **Última revisão:** 12 de setembro de 2026
- **Escopo:** solução completa

## Contexto

O Tech Challenge exige quatro repositórios com CI/CD. Além do requisito acadêmico, aplicação, banco, cluster e autenticação possuem ciclos de mudança e permissões diferentes.

## Decisão

Manter quatro repositórios independentes:

| Repositório | Responsabilidade | Estratégia de entrega |
|---|---|---|
| `oficina_fiap` | Spring API e manifests | Maven, testes, imagem no GHCR e deploy com `kubectl`/Helm em runner Windows self-hosted |
| `oficina-fiap-infra-k8s` | EC2, rede e K3s | Terraform em runner Ubuntu do GitHub |
| `oficina-fiap-infra-database` | RDS e rede do banco | Terraform em runner Ubuntu do GitHub |
| `oficina-fiap-lambda-auth` | Lambdas e API Gateway | Maven, testes e Terraform em runner Ubuntu do GitHub |

Os estados Terraform são separados por chave no backend S3. Pull Requests executam verificações e o `apply` ocorre na `main` ou por execução manual, conforme cada workflow.

## Alternativas consideradas

### Monorepositório

Facilitaria alterações atômicas entre componentes, mas não atenderia à separação obrigatória e aumentaria o acoplamento dos pipelines.

### Pipeline único para toda a solução

Simplificaria a ordem de execução, mas daria a um único workflow acesso a todas as credenciais e faria mudanças pequenas redeployarem recursos não relacionados.

## Consequências

### Positivas

- responsabilidades e permissões separadas;
- pipelines menores e específicos;
- aplicação e infraestrutura podem evoluir independentemente;
- estados Terraform isolados reduzem o raio de impacto.

### Negativas e riscos

- variáveis como endpoint do RDS, subnets, Security Group e `APP_BASE_URL` precisam ser transferidas entre repositórios;
- secrets do GitHub não são compartilhados automaticamente;
- mudanças de contrato, como JWT, exigem coordenação entre Pull Requests;
- credenciais temporárias do AWS Academy precisam ser renovadas;
- a aplicação depende da disponibilidade do runner Windows self-hosted e do cluster.

## Controles adotados

- alterações por branches e Pull Requests;
- testes antes de empacotar;
- `terraform fmt`, `validate` e `plan` nos repositórios de infraestrutura;
- secrets e variables do GitHub para credenciais e endpoints;
- concorrência controlada no deploy serverless;
- deploy da Lambda condicionado a `DEPLOY_ENABLED=true`.

## Resultado da implementação

Os quatro repositórios e seus pipelines foram utilizados de forma independente. A aplicação principal foi publicada e implantada no K3s, o RDS PostgreSQL foi disponibilizado, e o deploy serverless foi habilitado com as variáveis e secrets necessários. A Lambda de autenticação, o Lambda Authorizer e o API Gateway foram implantados e validados em um fluxo ponta a ponta até a Spring API.
