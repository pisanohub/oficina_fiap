# ADR-P3 — Mensageria assíncrona com Amazon SQS

- **Estado:** Aceita, implementação pendente
- **Data:** 17/10/2026
- **Repositório principal:** `oficina-fiap-infra-k8s` (local de provisionamento a confirmar quando a infra de mensageria for criada)

## Contexto

O Saga Pattern que coordena a Ordem de Serviço entre os 3 microsserviços precisa de comunicação assíncrona desacoplada (comandos do orquestrador para os participantes, e eventos de resposta de volta), sem que nenhum serviço acesse o banco de outro diretamente.

## Decisão

Usar **Amazon SQS** como mensageria, com:

- Filas de **comando** (orquestrador → participante) e de **resposta/evento** (participante → orquestrador) separadas por par de serviços.
- **Dead Letter Queue (DLQ)** para mensagens que falham repetidamente.
- **Consumidores idempotentes** — o SQS padrão (não FIFO) entrega "pelo menos uma vez", então a mesma mensagem pode chegar duplicada.
- Filas provisionadas via **Terraform**.

## Alternativas consideradas

### RabbitMQ auto-hospedado no cluster

Mais controle e aprendizado prático de um broker "de verdade", mas adiciona um componente com estado pra administrar no mesmo nó EC2 já identificado como frágil na Fase 3 (sessões de 4h, recursos apertados) — descartada.

### Apache Kafka (gerenciado ou auto-hospedado)

Mais poderoso para alta vazão e replay de eventos, mas complexidade operacional desproporcional ao escopo e tempo da Fase 4 — descartada.

### Validação pelo material da disciplina

O conteúdo de aula da Fase 4 (SAGA na prática) cita explicitamente o Amazon SQS como alternativa de mensageria tão válida quanto Kafka/RabbitMQ pra implementar o padrão — reforça que a escolha não compromete a avaliação.

## Consequências

### Positivas

- Serviço gerenciado pela AWS: sobrevive entre sessões do AWS Academy, diferente do nó EC2 (que é recriado a cada sessão).
- Nenhum componente extra com estado pra administrar dentro do cluster.
- Integra nativamente com a `LabRole` já usada pelos outros recursos AWS do projeto.

### Negativas e riscos

- SQS padrão (não FIFO) não garante ordem de entrega — precisa ser considerado no desenho de cada consumidor.
- Entrega "pelo menos uma vez" exige idempotência explícita em cada handler de evento/comando.
- Acoplamento à AWS: menos portável que uma solução baseada em protocolo aberto (ex.: AMQP).

## Plano de verificação

- Confirmar que a `LabRole` permite criar e consumir filas SQS.
- Implementar um teste simulando mensagem duplicada, validando que o consumidor trata isso de forma idempotente, antes de integrar os 3 serviços de verdade.
