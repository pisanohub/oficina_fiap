# Rascunho individual — Arquitetura Fase 4 (Pedro)

Rascunho pessoal para a "lição de casa" combinada na reunião de 15/10/2026. Reflete as premissas P1–P7 já validadas pelo grupo.

```mermaid
flowchart TB
    cliente["Cliente / Operador"]

    subgraph gateway["Porta de entrada (Fase 3, sem alteração)"]
        apigw["Amazon API Gateway"]
        lambdaAuth["Lambda CPF<br/>valida CPF, emite JWT"]
        lambdaAuthorizer["Lambda Authorizer<br/>valida JWT"]
    end

    subgraph k8s["EC2 t3.large + K3s"]
        direction LR

        subgraph osservice["OS Service"]
            osApp["Spring Boot<br/>+ Orquestrador da Saga"]
        end

        subgraph billing["Billing Service"]
            billingApp["Spring Boot<br/>+ integração Mercado Pago"]
        end

        subgraph execucao["Execucao e Producao Service"]
            execApp["Spring Boot<br/>fila de execucao, Servico, ItemEstoque"]
        end
    end

    subgraph rds["Amazon RDS - 1 instancia"]
        direction LR
        dbOs[("database: os_service")]
        dbBilling[("database: billing_service")]
    end

    dynamo[("Amazon DynamoDB<br/>execucao_service")]

    subgraph sqs["Amazon SQS"]
        direction LR
        qOsToBilling["fila: gerar-orcamento"]
        qBillingToOs["fila: orcamento-respondido"]
        qOsToBillingPag["fila: processar-pagamento"]
        qBillingToOsPag["fila: pagamento-confirmado"]
        qOsToExec["fila: enviar-execucao"]
        qExecToOs["fila: execucao-concluida"]
    end

    mercadopago["Mercado Pago<br/>sandbox"]

    newrelic["New Relic<br/>traces, logs, metricas<br/>dashboard da Saga"]

    cliente -->|HTTPS + Bearer JWT| apigw
    apigw -->|"/auth/cpf"| lambdaAuth
    apigw -->|rotas protegidas| lambdaAuthorizer
    lambdaAuthorizer -->|proxy autorizado| osApp

    osApp -->|JDBC| dbOs
    billingApp -->|JDBC| dbBilling
    execApp -->|SDK AWS| dynamo

    osApp -->|publica comando| qOsToBilling
    qOsToBilling -->|consome| billingApp
    billingApp -->|publica evento| qBillingToOs
    qBillingToOs -->|consome| osApp

    osApp -->|publica comando| qOsToBillingPag
    qOsToBillingPag -->|consome| billingApp
    billingApp -->|publica evento| qBillingToOsPag
    qBillingToOsPag -->|consome| osApp

    osApp -->|publica comando| qOsToExec
    qOsToExec -->|consome| execApp
    execApp -->|publica evento| qExecToOs
    qExecToOs -->|consome| osApp

    billingApp -->|REST| mercadopago

    osApp -.->|OTLP| newrelic
    billingApp -.->|OTLP| newrelic
    execApp -.->|OTLP| newrelic

    classDef pessoa fill:#f6f8fa,stroke:#57606a,color:#24292f;
    classDef aws fill:#fff0d5,stroke:#d97706,color:#111;
    classDef servico fill:#e8f0fe,stroke:#326ce5,color:#111;
    classDef dado fill:#eee8fa,stroke:#6f42c1,color:#111;
    classDef fila fill:#fff7d6,stroke:#9a6700,color:#111;
    classDef externo fill:#fde2e2,stroke:#c0392b,color:#111;
    classDef obs fill:#e9f7ef,stroke:#1f8f4e,color:#111;

    class cliente pessoa;
    class apigw,lambdaAuth,lambdaAuthorizer aws;
    class osApp,billingApp,execApp servico;
    class dbOs,dbBilling,dynamo dado;
    class qOsToBilling,qBillingToOs,qOsToBillingPag,qBillingToOsPag,qOsToExec,qExecToOs fila;
    class mercadopago externo;
    class newrelic obs;
```

## Fluxo da Saga (texto, pra acompanhar o diagrama)

1. Cliente abre OS → **OS Service** cria o registro e dispara comando `gerar-orcamento` pro Billing (via SQS).
2. **Billing Service** gera o orçamento, publica evento de resposta. OS fica `AGUARDANDO_APROVACAO`.
3. Cliente aprova → **OS Service** dispara comando `processar-pagamento` pro Billing (via SQS) → Billing processa pagamento via Mercado Pago (sandbox) → publica evento de confirmação na fila `pagamento-confirmado`.
4. **OS Service** consome `pagamento-confirmado`, dispara comando `enviar-execucao` pro Execução Service (via SQS).
5. **Execução Service** processa (usa o catálogo `Servico`/`ItemEstoque` que hoje vivem nele) e publica evento de conclusão.
6. **OS Service** recebe o evento, atualiza o status pra `CONCLUIDA` → `ENTREGUE`.

Compensações: pagamento recusado → Billing publica evento de rejeição → OS Service cancela a OS. Falha ao enfileirar execução → OS Service dispara comando de estorno pro Billing.

## Pontos que ainda quero confirmar com o grupo

- Nome exato das filas SQS e formato do payload dos eventos (schema/contrato).
- Se o Billing precisa consultar o catálogo (`Servico`) no Execução Service via REST síncrono na hora de gerar o orçamento, ou se recebe esses dados junto no comando inicial do OS Service.
- Onde fica o estado da máquina de Saga dentro do OS Service (campo de status na própria `OrdemDeServico`, ou uma tabela separada de acompanhamento da Saga).
