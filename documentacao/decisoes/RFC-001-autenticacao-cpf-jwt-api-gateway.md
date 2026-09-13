# RFC-001 - Autenticação por CPF, JWT e API Gateway

- **Estado:** Aprovada e implementada
- **Data da consolidação:** 8 de setembro de 2026
- **Última revisão:** 12 de setembro de 2026
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

Os tokens de cliente e de administrador permanecem em fluxos separados. O token de cliente contém `tipo=CLIENTE` e permite consultar somente os recursos autorizados para o próprio cliente, enquanto as operações administrativas continuam exigindo a autenticação administrativa da Spring API.

## Segredo de assinatura

A Lambda emissora, o Lambda Authorizer e a Spring API precisam usar exatamente a mesma chave de assinatura.

O segredo deixou de ser gerado de forma independente no deploy da Lambda. O mesmo `JWT_SECRET` passou a ser fornecido à Lambda emissora, ao Lambda Authorizer e à Spring API por secrets dos respectivos repositórios, sem registrá-lo no código, nos logs ou neste documento.

Como os secrets do GitHub são isolados por repositório, o mesmo valor precisa ser cadastrado separadamente nos repositórios envolvidos, ou disponibilizado por um gerenciador de segredos comum.

## Compatibilidade implementada

Durante a consolidação inicial, foi identificada uma incompatibilidade: a Lambda emitia `sub=CPF` e as claims `clienteId`, `tipo` e `ativo`, enquanto a aplicação principal reconhecia apenas o contrato administrativo baseado em `username`.

A aplicação foi ajustada para reconhecer explicitamente o contrato de cliente sem tentar carregar o CPF como usuário administrativo. A validação confere assinatura, expiração, tipo do principal, identificador e situação do cliente, mantendo separado o contrato de autenticação administrativa.

Com o `JWT_SECRET` compartilhado, o token emitido pela Lambda passou a ser aceito pelo Authorizer e pela Spring API.

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

## Resultado da implementação

Os critérios de aceite foram validados no ambiente AWS. Um cliente ativo foi autenticado pelo CPF no endpoint público, recebeu um JWT de uma hora e utilizou o token em uma rota protegida. A requisição passou pelo Lambda Authorizer, foi encaminhada pelo API Gateway e chegou à Spring API, que aplicou as permissões do cliente.
