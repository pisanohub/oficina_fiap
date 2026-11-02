# Checklist de infra — Pedro (Fase 4)

> Documento de trabalho pessoal, pra retomar rápido. Não é o planejamento oficial do grupo (esse continua em `divisao-tarefas-fase4.md`) — é só o detalhamento técnico da minha parte.
>
> Última atualização: 02/11/2026.

## ✅ Já feito

**Sessão de 31/10:**
- [x] Descoberto e corrigido: o backend do Terraform (`oficina-fiap-tfstate-401510708311`) apontava pra uma conta AWS antiga (provavelmente do Anderson, pré-migração) — bucket criado na conta atual (`381492182695`), `providers.tf` dos dois repositórios atualizado.
- [x] EC2 do cluster k3s subida pra `t3.large` (ADR-P5) — merge feito, apply rodou com sucesso.
- [x] Instância RDS PostgreSQL criada do zero na conta atual (`oficina-fiap-db`, `db.t4g.micro`, banco `oficina` com usuário master `oficina_admin`).
- [x] Branch protection + check de CI obrigatório configurado nos 3 repositórios novos (OS, Billing, Execução).

**Sessão de 02/11 — item 1 completo (ADR-P2, banco de dados):**
- [x] Decisão de acesso resolvida: opção (a) — liberei meu IP temporariamente no security group do RDS, rodei os comandos via minha máquina, removi a liberação depois.
- [x] Databases `os_service` e `billing_service` criados, cada um com usuário próprio e senha própria, privilégios restritos ao próprio banco.
- [x] Isolamento testado: usuário do `os_service` recebeu erro de permissão negada ao tentar acessar `billing_service` — confirma o plano de verificação do ADR-P2.
- [x] Security group do RDS limpo de volta ao estado original (IP removido).
- [x] Credenciais de cada serviço salvas como Secrets nos repositórios de cada serviço (`oficina-fiap-os-service`, `oficina-fiap-billing-service`).

## ⚠️ Decisões a tomar antes de seguir (rápido, mas importa)

1. **Onde entra o Terraform das filas SQS?** O ADR-P3 ainda não define o repositório — provável que seja um `sqs.tf` novo dentro do `oficina-fiap-infra-k8s` (já que é só configuração, sem servidor próprio). Confirmar antes de criar.
2. **Qual a chave primária da tabela DynamoDB do Execução Service?** Isso é decisão de quem vai consumir a tabela (Guilherme). Vale perguntar antes de criar a tabela, pra não ter que recriar depois.

## 🔲 A fazer, em ordem

### 1. ~~Banco de dados — separar por serviço (ADR-P2)~~ ✅ concluído (02/11/2026)

### 2. Mensageria — SQS (ADR-P3)

- [x] Confirmado onde entra o Terraform: `sqs.tf` dentro do `oficina-fiap-infra-k8s` (ver decisão acima).
- [x] Criadas as 6 filas do fluxo de referência + 6 DLQs (12 recursos, via `for_each`) — PR #5, merge e apply em 02/11/2026:
  - `gerar-orcamento` (OS → Billing)
  - `orcamento-respondido` (Billing → OS)
  - `processar-pagamento` (OS → Billing)
  - `pagamento-confirmado` (Billing → OS)
  - `enviar-execucao` (OS → Execução)
  - `execucao-concluida` (Execução → OS)
  - URLs completas no output `sqs_queue_urls` do Terraform (formato `https://sqs.us-east-1.amazonaws.com/381492182695/<nome-da-fila>`).
- [ ] Confirmar que a `LabRole` da EC2 consegue publicar/consumir nas filas (plano de verificação do ADR-P3) — só dá pra testar de verdade quando tiver algum serviço rodando no cluster.
- [ ] Guardar as URLs das filas como Secrets nos repositórios dos serviços correspondentes — depende dos repositórios dos serviços terem código pra consumir (ainda são só esqueletos).

### 3. DynamoDB — Execução Service (ADR-P2)

- [ ] Confirmar com o Guilherme o nome/formato da chave primária antes de criar.
- [ ] Criar a tabela via Terraform.
- [ ] Confirmar que a `LabRole` tem permissão de leitura/escrita (plano de verificação do ADR-P2).
- [ ] Guardar nome da tabela/região como Secret no repositório do Execução Service.

### 4. ~~Pipeline de deploy nos 3 repositórios novos~~ ✅ concluído (02/11/2026)

- [x] `deploy.yml` criado nos 3 repositórios (OS, Billing, Execução) — diferente do monólito, roda em `ubuntu-latest` (não self-hosted), pra não gargalar 4 pipelines de deploy na mesma máquina.
- [x] Corrigido gap que existia no monólito: `ghcr-secret` (pull da imagem) agora é criado explicitamente no pipeline (`kubectl create secret docker-registry`), não depende de passo manual nem de o pacote ser público.
- [x] Corrigido `envFrom` faltando nos manifestos do OS Service e do Execução Service (só o Billing já tinha) — sem isso o ConfigMap/Secret eram criados mas nunca chegavam no container.
- [x] New Relic (Helm chart) deliberadamente **não** replicado nos 3 pipelines novos — é instalação de cluster inteiro, não por serviço; fica com o Guilherme.
- [x] Os 3 deployaram com sucesso, rollout confirmado.

**Problemas reais encontrados e corrigidos no caminho** (deixando registrado pra não repetir):
- PAT do GHCR sem escopo certo → gerado de novo com `write:packages` + `repo`.
- Porta 6443 (API do Kubernetes) só liberava o IP do `ssh_allowed_cidr` — os runners do GitHub Actions usam IP dinâmico. Aberta pra `0.0.0.0/0` (autenticação continua sendo via certificado cliente do kubeconfig).
- IP público da EC2 mudava a cada sessão, quebrando o `KUBECONFIG` **e** o certificado TLS do k3s (que só é gerado uma vez, no primeiro boot, com `--tls-san` fixado no IP daquele momento) → resolvido de vez com um **Elastic IP** (ver `eip.tf`, PR #8). O IP agora é fixo: `54.159.42.66`, não muda mais nem se a EC2 for recriada do zero (o Elastic IP é um recurso Terraform separado, sobrevive à recriação da instância).
- `terraform plan` local no Windows acusava "recriar a instância" por causa de `core.autocrlf` convertendo quebra de linha — corrigido com `.gitattributes` (`eol=lf`) no `infra-k8s`.

### 5. Fechar os ADRs

- [ ] ADR-P2 e ADR-P3 estão com rascunho pronto em `documentacao/decisoes-fase4/`, faltando só preencher o "Plano de verificação" com o resultado real (depois dos itens 1–3 acima) e mover pro repositório final de cada infra.
- [ ] ADR-P5 já pode ser marcado como "Implementada" (EC2 já está em `t3.large`).

## Lembrete de sessão (toda vez que for trabalhar)

1. Abrir o AWS Academy Learner Lab, clicar "Start Lab".
2. Pegar as 3 credenciais em "AWS Details" → "AWS CLI".
3. Atualizar `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN` nos Secrets de **todos** os repositórios que forem usados na sessão (hoje: `infra-k8s`, `infra-database`; a partir de quando a EC2/CD dos serviços entrar em cena, também nos 3 repositórios de serviço).
4. Exportar as mesmas credenciais como variável de ambiente no PowerShell local, se for rodar comando AWS CLI diretamente (`$env:AWS_ACCESS_KEY_ID = "..."`, etc.).

## Pegadinha do KUBECONFIG (descoberta em 02/11/2026, resolvida no mesmo dia)

O `scp` do `k3s.yaml` (comando no output `kubeconfig_fetch_command`) traz o arquivo com `server: https://127.0.0.1:6443` — funciona só de dentro da própria EC2, nunca de fora (GitHub Actions, sua máquina). **Sempre** depois do `scp`, trocar `127.0.0.1` pelo IP público:

```powershell
cd "...\oficina-fiap-infra-k8s"
scp -i ~/.ssh/oficina-fiap-k3s ec2-user@54.159.42.66:/etc/rancher/k3s/k3s.yaml ./kubeconfig-oficina-fiap.yaml
(Get-Content '.\kubeconfig-oficina-fiap.yaml') -replace '127\.0\.0\.1', '54.159.42.66' | Set-Content '.\kubeconfig-oficina-fiap.yaml'
```

**Desde a adição do Elastic IP (PR #8), o IP é sempre `54.159.42.66`** — não muda mais entre sessões, nem se a EC2 for recriada do zero pelo Terraform. Então esse passo continua necessário toda vez que a EC2 for recriada (o `127.0.0.1` sempre aparece de novo no `k3s.yaml` fresco), mas o valor pra substituir é sempre o mesmo, sem precisar consultar o IP atual antes.

Depois de corrigir o arquivo, atualizar o Secret `KUBECONFIG` nos repositórios que forem usados na sessão:

```powershell
gh secret set KUBECONFIG --repo pisanohub/oficina-fiap-os-service --body "$(Get-Content -Raw '.\kubeconfig-oficina-fiap.yaml')"
gh secret set KUBECONFIG --repo pisanohub/oficina-fiap-billing-service --body "$(Get-Content -Raw '.\kubeconfig-oficina-fiap.yaml')"
gh secret set KUBECONFIG --repo pisanohub/oficina-fiap-execucao-service --body "$(Get-Content -Raw '.\kubeconfig-oficina-fiap.yaml')"
```
