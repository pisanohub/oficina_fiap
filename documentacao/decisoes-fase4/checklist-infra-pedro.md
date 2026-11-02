# Checklist de infra — Pedro (Fase 4)

> Documento de trabalho pessoal, pra retomar rápido. Não é o planejamento oficial do grupo (esse continua em `divisao-tarefas-fase4.md`) — é só o detalhamento técnico da minha parte.
>
> Última atualização: 31/10/2026 (sessão de sábado).

## ✅ Já feito (sessão de 31/10)

- [x] Descoberto e corrigido: o backend do Terraform (`oficina-fiap-tfstate-401510708311`) apontava pra uma conta AWS antiga (provavelmente do Anderson, pré-migração) — bucket criado na conta atual (`381492182695`), `providers.tf` dos dois repositórios atualizado.
- [x] EC2 do cluster k3s subida pra `t3.large` (ADR-P5) — merge feito, apply rodou com sucesso.
- [x] Instância RDS PostgreSQL criada do zero na conta atual (`oficina-fiap-db`, `db.t4g.micro`, banco `oficina` com usuário master `oficina_admin`).
- [x] Branch protection + check de CI obrigatório configurado nos 3 repositórios novos (OS, Billing, Execução).

## ⚠️ Decisões a tomar antes de seguir (rápido, mas importa)

1. **Como vou criar os databases/usuários separados no RDS?** O security group do RDS só libera a porta 5432 pra dentro da própria VPC (`publicly_accessible = false`) — ou seja, o runner do GitHub Actions (hospedado fora da AWS) **não consegue conectar direto**. Duas opções:
   - (a) Liberar temporariamente meu IP atual no security group do RDS, rodar os comandos `psql` da minha própria máquina, depois remover a liberação.
   - (b) Rodar os comandos a partir da própria EC2 do cluster (que já está dentro da VPC), via SSH.
   - Minha sugestão: opção (a), mais simples de fazer uma vez só. Fica a decidir amanhã.
2. **Onde entra o Terraform das filas SQS?** O ADR-P3 ainda não define o repositório — provável que seja um `sqs.tf` novo dentro do `oficina-fiap-infra-k8s` (já que é só configuração, sem servidor próprio). Confirmar antes de criar.
3. **Qual a chave primária da tabela DynamoDB do Execução Service?** Isso é decisão de quem vai consumir a tabela (Guilherme). Vale perguntar antes de criar a tabela, pra não ter que recriar depois.

## 🔲 A fazer, em ordem

### 1. Banco de dados — separar por serviço (ADR-P2)

- [ ] Resolver a decisão de acesso (ver acima).
- [ ] Pegar a senha master do RDS (está no secret `DB_PASSWORD` do repositório `infra-database` — recuperar de onde guardei originalmente, ou gerar outra e atualizar o secret).
- [ ] Criar database `os_service` + usuário próprio com senha própria, privilégios só nesse banco.
- [ ] Criar database `billing_service` + usuário próprio com senha própria, privilégios só nesse banco.
- [ ] Testar isolamento: confirmar que o usuário do `os_service` falha ao tentar logar/ler no `billing_service` (plano de verificação do ADR-P2).
- [ ] Guardar as credenciais de cada serviço como Secrets **nos repositórios de cada serviço** (`oficina-fiap-os-service`, `oficina-fiap-billing-service`), não no `infra-database` — cada serviço só deve ter acesso ao próprio banco.

### 2. Mensageria — SQS (ADR-P3)

- [ ] Confirmar onde entra o Terraform (ver decisão acima).
- [ ] Criar as filas do fluxo de referência (nomes do `event-storming-saga-v1.md`):
  - `gerar-orcamento` (OS → Billing)
  - `orcamento-respondido` (Billing → OS)
  - `processar-pagamento` (OS → Billing) — faltava, pego na revisão do Gabriel Souza (02/11/2026)
  - `pagamento-confirmado` (Billing → OS) — faltava, pego na revisão do Gabriel Souza (02/11/2026)
  - `enviar-execucao` (OS → Execução)
  - `execucao-concluida` (Execução → OS)
- [ ] Criar uma Dead Letter Queue (DLQ) pra cada fila acima.
- [ ] Confirmar que a `LabRole` da EC2 consegue publicar/consumir nas filas (plano de verificação do ADR-P3).
- [ ] Guardar as URLs das filas como Secrets nos repositórios dos serviços correspondentes.

### 3. DynamoDB — Execução Service (ADR-P2)

- [ ] Confirmar com o Guilherme o nome/formato da chave primária antes de criar.
- [ ] Criar a tabela via Terraform.
- [ ] Confirmar que a `LabRole` tem permissão de leitura/escrita (plano de verificação do ADR-P2).
- [ ] Guardar nome da tabela/região como Secret no repositório do Execução Service.

### 4. Pipeline de deploy nos 3 repositórios novos

- [ ] Hoje cada repositório só tem build+test (`ci.yml`). Falta a etapa de deploy: build da imagem Docker, push pro GHCR, aplicar os manifestos `k8s/` no cluster — reaproveitar o padrão do `deploy.yml` do monólito.
- [ ] Cada repositório vai precisar do `KUBECONFIG` como Secret (mesmo esquema já usado no monólito na Fase 3).
- [ ] **Lembrete operacional:** como a EC2 é recriada a cada sessão nova do Academy Lab, o `KUBECONFIG` muda junto — precisa ser atualizado nos Secrets toda vez que o cluster for recriado, igual já fazemos com as credenciais da AWS.

### 5. Fechar os ADRs

- [ ] ADR-P2 e ADR-P3 estão com rascunho pronto em `documentacao/decisoes-fase4/`, faltando só preencher o "Plano de verificação" com o resultado real (depois dos itens 1–3 acima) e mover pro repositório final de cada infra.
- [ ] ADR-P5 já pode ser marcado como "Implementada" (EC2 já está em `t3.large`).

## Lembrete de sessão (toda vez que for trabalhar)

1. Abrir o AWS Academy Learner Lab, clicar "Start Lab".
2. Pegar as 3 credenciais em "AWS Details" → "AWS CLI".
3. Atualizar `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN` nos Secrets de **todos** os repositórios que forem usados na sessão (hoje: `infra-k8s`, `infra-database`; a partir de quando a EC2/CD dos serviços entrar em cena, também nos 3 repositórios de serviço).
4. Exportar as mesmas credenciais como variável de ambiente no PowerShell local, se for rodar comando AWS CLI diretamente (`$env:AWS_ACCESS_KEY_ID = "..."`, etc.).
