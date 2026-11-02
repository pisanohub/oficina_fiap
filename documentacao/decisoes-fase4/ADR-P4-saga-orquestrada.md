# ADR-P4 — Saga orquestrada no OS Service

- **Estado:** Proposta (rascunho inicial — Gabriel Souza valida, ajusta e assume)
- **Data:** 02/11/2026
- **Repositório principal:** OS Service (orquestrador) + Billing Service e Execução Service (participantes)

## Contexto

A Ordem de Serviço passa por um fluxo de vários passos entre 3 serviços (OS, Billing, Execução), coordenado via Saga Pattern orquestrada pelo OS Service, com comandos/eventos assíncronos por SQS. Duas questões em aberto no desenho inicial (event storming, `documentacao/event-storming-saga-v1.md`) precisavam de decisão antes de implementar: (1) como e quando a saga é efetivamente disparada, já que no desenho original o orçamento era pedido assim que a OS abria, sem ainda haver itens definidos; e (2) como o Billing obtém o preço dos itens, já que o catálogo (`Servico`/`ItemEstoque`) vive no Execução Service, em outro banco.

## Decisão

**Fluxo de status (gatilho da saga):**

```
ABERTA → EM_DIAGNOSTICO → [ação explícita: SolicitarOrcamento] → AGUARDANDO_APROVACAO → APROVADA → EM_EXECUCAO → CONCLUIDA → ENTREGUE
```

- `ABERTA` e `EM_DIAGNOSTICO` são transições síncronas (chamada direta à API do OS Service) — não envolvem fila.
- Durante `EM_DIAGNOSTICO`, o Mecânico/Operador adiciona os itens (`ItemOS`) à OS.
- A saga só dispara quando o Mecânico/Operador aciona explicitamente `SolicitarOrcamento` (não é automático ao adicionar o primeiro item) — só nesse momento o comando `GerarOrcamento` é publicado na fila.
- `AGUARDANDO_APROVACAO`, `EM_EXECUCAO` e `CONCLUIDA` são atingidos ao consumir um evento da fila correspondente (ver tabela completa no event storming).
- `CANCELADA` pode entrar por compensação a partir de qualquer status não-terminal.

**Preço dos itens (sem acoplamento síncrono entre Billing e Execução):**

O preço de cada `Servico`/`ItemEstoque` é capturado pelo OS Service no momento em que o item é adicionado à OS, durante `EM_DIAGNOSTICO` (ele já precisa consultar o catálogo do Execução Service pra exibir os preços ao operador). Esse valor fica guardado como snapshot no próprio item, e é esse valor — não uma nova consulta — que vai embutido no comando `GerarOrcamento`. O Billing nunca chama o Execução Service.

**Filas envolvidas no fluxo feliz** (nomes definidos em `event-storming-saga-v1.md`):

| Fila | Direção | Carrega |
|---|---|---|
| `gerar-orcamento` | OS → Billing | comando, com preço já embutido |
| `orcamento-respondido` | Billing → OS | evento `OrcamentoGerado` |
| `processar-pagamento` | OS → Billing | comando `ProcessarPagamento` |
| `pagamento-confirmado` | Billing → OS | evento `PagamentoConfirmado` |
| `enviar-execucao` | OS → Execução | comando `EnviarParaExecucao` |
| `execucao-concluida` | Execução → OS | evento `ExecucaoConcluida` |

## Alternativas consideradas

### Disparar o orçamento automaticamente ao abrir a OS

Era o desenho original — descartada porque a OS ainda não tem itens nesse momento, não dá pra gerar orçamento de nada.

### Disparar o orçamento automaticamente ao adicionar o primeiro item

Mais simples, mas o operador pode querer adicionar vários itens antes de pedir o orçamento — descartada em favor de uma ação explícita.

### REST síncrono do Billing pro Execução na hora do orçamento

Mais "fresco" (preço sempre atualizado), mas cria dependência síncrona entre dois participantes de uma saga assíncrona — descartada.

## Consequências

### Positivas
- Saga só dispara quando a OS está realmente pronta (itens definidos), evitando orçamento vazio.
- Nenhuma dependência síncrona entre Billing e Execução durante a saga.
- Preço do orçamento fica estável/previsível pro cliente a partir do momento em que o item foi adicionado.

### Negativas e riscos
- Precisa de um endpoint/ação explícita extra no OS Service (`SolicitarOrcamento`), além do CRUD de itens.
- Se o catálogo mudar de preço depois que o item já foi adicionado à OS, o orçamento usa o valor antigo (congelado) — aceito como comportamento correto do domínio, não como limitação.

## Plano de verificação

- Teste: tentar disparar `GerarOrcamento` sem ter passado por `SolicitarOrcamento` deve falhar (transição inválida).
- Teste: adicionar item fora do status `EM_DIAGNOSTICO` deve falhar.
- Confirmar que o comando `gerar-orcamento` publicado na fila carrega o preço já embutido, não um id solto.
- Teste: alterar o preço de um `Servico` no catálogo depois de um item já adicionado à OS, confirmar que o orçamento usa o preço antigo (snapshot).
- Confirmar que `processar-pagamento`/`pagamento-confirmado` e as demais filas do fluxo de pagamento estão provisionadas junto com as demais (ver checklist de infra do Pedro).
