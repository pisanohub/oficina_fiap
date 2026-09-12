# RFC-001 - Autenticação por CPF, JWT e API Gateway

- **Estado:** Em discussão
- **Data da consolidação:** 8 de setembro de 2026
- **Escopo:** `oficina-fiap-lambda-auth` e `oficina_fiap`

## Contexto

O Tech Challenge exige autenticação de clientes por CPF, Function Serverless, emissão de JWT, API Gateway e proteção das APIs sensíveis.

O repositório `oficina-fiap-lambda-auth` já contém:

- `POST /auth/cpf` público no Amazon API Gateway;
- Lambda Java que valida o CPF, consulta o cliente no RDS e emite JWT;
- Lambda Authorizer que valida o Bearer token;
- proxy protegido `/api/{proxy+}` para a aplicação principal.

A Spring API também possui validação JWT. Essa segunda validação é importante enquanto o backend puder ser chamado diretamente, sem passar pelo API Gateway.

## Proposta

Adotar o seguinte fluxo:

1. o cliente envia o CPF para `POST /auth/cpf`;
2. a Lambda de autenticação, configurada nas subnets da VPC, consulta o RDS privado;
3. para um cliente existente e ativo, a Lambda emite um JWT de curta duração;
4. o cliente envia esse token em `Authorization: Bearer <JWT>`;
5. o Lambda Authorizer valida assinatura, expiração e claims;
6. o API Gateway encaminha a chamada autorizada para a Spring API;
7. a Spring API valida novamente o token e aplica as permissões do cliente.

## Contrato JWT proposto para clientes

| Campo | Conteúdo |
|---|---|
| `sub` | CPF normalizado do cliente |
| `clienteId` | Identificador numérico do cliente |
| `tipo` | `CLIENTE` |
| `ativo` | `true` |
| `iat` | Instante de emissão |
| `exp` | Instante de expiração |

Validade inicial proposta: **3.600 segundos**.

Tokens administrativos existentes devem ser diferenciados por uma claim explícita, por exemplo `tipo=ADMIN`, ou permanecer em um fluxo de autenticação separado. A equipe deve decidir quais rotas aceitam `CLIENTE`, `ADMIN` ou ambos.

## Segredo de assinatura

A Lambda emissora, o Lambda Authorizer e a Spring API precisam usar exatamente a mesma chave de assinatura.

O Terraform da Lambda atualmente cria `random_password.jwt`, enquanto a aplicação recebe `JWT_SECRET` pelo GitHub Actions. Esses valores independentes não funcionam juntos. A proposta é transformar o segredo em entrada sensível do Terraform e fornecer o mesmo valor nos dois pipelines, sem registrá-lo no código, nos logs ou neste documento.

Como os secrets do GitHub são isolados por repositório, o mesmo valor precisa ser cadastrado separadamente nos repositórios envolvidos, ou disponibilizado por um gerenciador de segredos comum.

## Incompatibilidade encontrada

A implementação atual da Lambda usa `sub=CPF` e as claims `clienteId`, `tipo` e `ativo`.

A branch `feature/atualizacao-jwt` da aplicação usa `sub=username`, adiciona `cpf` e carrega `UsuarioDetails` pelo nome do usuário. Portanto, apenas realizar o merge dessa branch e igualar o `JWT_SECRET` não torna os tokens compatíveis.

Antes do teste ponta a ponta, a aplicação precisa reconhecer o contrato de token de cliente definido nesta RFC, sem exigir que o CPF seja um usuário administrativo.

## Alternativas consideradas

### Confiar somente no Lambda Authorizer

Simplifica a Spring API, mas permite contornar a autorização se o backend possuir uma URL pública acessível diretamente. Só é aceitável com restrição de rede que force todo o tráfego a passar pelo Gateway.

### Validar no Authorizer e na Spring API

Adiciona uma validação duplicada, porém preserva a segurança quando o backend é público. É a alternativa recomendada para a arquitetura atual.

### Manter tokens de cliente e administrador com contratos implícitos

Evita alterações imediatas, mas aumenta ambiguidades e falhas de autorização. Não é recomendado; o tipo do principal deve ser explícito.

## Consequências

### Positivas

- autenticação serverless conforme o requisito;
- rejeição antecipada de tokens inválidos no API Gateway;
- proteção adicional na aplicação;
- token curto evita uma consulta ao banco em cada requisição protegida.

### Negativas e riscos

- compartilhamento seguro do segredo entre dois repositórios;
- necessidade de compatibilizar a autenticação administrativa existente;
- alteração do status do cliente não revoga imediatamente um token já emitido;
- cache de 300 segundos do Authorizer pode manter uma decisão até expirar.

## Critérios de aceite

- um CPF válido e ativo recebe HTTP 200 e um JWT;
- CPF inválido, inexistente e inativo recebem os códigos definidos no contrato;
- o Authorizer devolve `Deny` para token inválido ou expirado;
- a Spring API aceita o token emitido pela Lambda;
- uma rota protegida rejeita chamadas sem token;
- o fluxo completo é testado usando a URL do API Gateway.
