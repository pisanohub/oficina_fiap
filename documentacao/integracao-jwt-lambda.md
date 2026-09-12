# Integração JWT da Lambda com a Spring API

Esta branch incorpora `feature/atualizacao-jwt` (87db07e) sobre a main (080b582) e ajusta somente a integração de autenticação/autorização. Pipelines e infraestrutura não foram alterados.

## Dois tipos de identidade

- Usuário administrativo: continua usando login com username, senha e CPF da branch original; a Spring resolve o usuário por username.
- Cliente autenticado pela Lambda: utiliza `sub=CPF`, `clienteId`, `tipo=CLIENTE`, `ativo=true`, `iat` e `exp`. Não precisa de registro em `TB_USUARIOS` nem recebe papel ADMIN. A Spring verifica assinatura/expiração e confirma ID, CPF e status em `TB_CLIENTES`, atribuindo `ROLE_CLIENTE`.

Tokens de cliente são identificados pelas claims `tipo` ou `clienteId`; tokens marcados como cliente e inválidos não são reaproveitados no fluxo administrativo. As chaves devem ser iguais e interpretadas como bytes UTF-8 nas duas Lambdas e na Spring. Não é necessário adicionar username ao token emitido pela Lambda.

## Permissões introduzidas para clientes

| Operação | Cliente | Administrador |
|---|---|---|
| GET /api/v1/ordens/cliente/{clienteId} | Somente seu próprio ID | Permitido |
| GET /api/v1/ordens/{id} | Somente ordem de seu veículo | Permitido |
| GET /api/v1/ordens/{id}/status | Somente ordem de seu veículo | Permitido |
| Criação/alteração de OS e demais operações protegidas | Negado | Mantido |

As consultas de ordens deixam de ser públicas. A política de cliente é conservadora e deve ser revisada com o grupo; ela não adiciona aprovação, abertura ou alteração de OS pelo cliente. O controle ocorre na camada de segurança, sem mudar as regras de negócio dos serviços.

O filtro retorna 401 para tokens de cliente inválidos, expirados ou sem cadastro ativo compatível. A autorização retorna 403 quando uma identidade válida solicita um recurso não permitido. A aplicação ainda pode retornar erros de negócio após a autorização.

## Verificação local

Com JDK 21 e Maven:

```powershell
mvn clean verify
```

`ClienteJwtIntegrationTest` usa tokens reais assinados no contrato da Lambda e a cadeia real do Spring Security. Endpoints e persistência são simulados para isolar autenticação; não é um teste na AWS nem um teste do banco gerenciado. Cobre token válido, assinatura incorreta, expiração, ausência de expiração, cliente inativo/removido, divergência de CPF, acesso a outra ordem/cliente e tentativa de operação administrativa. Também cobre o login administrativo por token da Spring.

Os testes de integração já desabilitados no projeto permanecem desabilitados; a nova suíte de segurança está habilitada.

Verificação local em 10/09/2026: `mvn clean verify` com Java 21 finalizou em BUILD SUCCESS. Foram reportados 228 testes, sendo 184 executados sem falhas/erros e 44 previamente desabilitados. O JAR foi gerado. Os sete testes de integração JWT e dois novos testes de ausência de CPF estão incluídos nesse resultado.

## Pontos obrigatórios antes do merge/deploy

1. Revisar as permissões da tabela acima com o líder.
2. A branch original adiciona `cpf` único e obrigatório a `TB_USUARIOS`. Se houver usuários antigos, o responsável pelo banco deve planejar preenchimento/migração antes de impor NOT NULL; não inventar CPFs nem recriar o banco. Usuário administrativo sem CPF é rejeitado com credenciais inválidas. Não foi aplicada migração ao RDS por esta mudança.
3. Configurar `JWT_SECRET` idêntico no ambiente efetivamente implantado.
4. Confirmar rede e acesso da Lambda ao RDS e cadastro normalizado do cliente de teste.
5. Revisar o cache do Authorizer na Lambda: a configuração de 300 segundos com política por ARN pode bloquear outra rota com o mesmo token. Esta branch da Spring não altera o Gateway.
6. No Postman, obter um token em POST /dev/auth/cpf e consultar GET /dev/api/v1/ordens/cliente/ID. Repetir com outro ID e token inválido. A criação de OS usa credencial administrativa no fluxo da aplicação; o Authorizer de clientes não aceita automaticamente tokens administrativos da Spring.

O merge na main pode acionar o deploy já existente no repositório. Portanto, este ajuste deve passar por revisão antes de integrar. Os diagramas/RFC do repositório da Lambda devem ser atualizados como implementação aprovada após a decisão do grupo.
