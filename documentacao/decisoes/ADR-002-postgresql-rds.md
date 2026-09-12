# ADR-002 - Utilizar PostgreSQL 16 no Amazon RDS

- **Estado:** Aceita e implementada
- **Data da consolidação:** 8 de setembro de 2026
- **Repositório principal:** `oficina-fiap-infra-database`

## Contexto

O Tech Challenge exige um banco de dados gerenciado. A aplicação já utiliza modelo relacional, transações e integração JDBC/JPA com PostgreSQL.

## Decisão

Usar Amazon RDS para PostgreSQL 16 com a configuração educacional atual:

- classe padrão `db.t4g.micro`;
- 20 GB de armazenamento `gp3`;
- banco inicial `oficina`;
- implantação privada, sem endpoint publicamente acessível;
- DB subnet group com duas subnets da VPC padrão;
- porta 5432 autorizada apenas a partir do CIDR da VPC;
- implantação em uma única zona de disponibilidade;
- estado Terraform remoto em S3.

## Alternativas consideradas

### PostgreSQL dentro do Kubernetes

É simples para desenvolvimento, mas transfere backup, persistência, disponibilidade e manutenção para o cluster. Também acopla os dados ao ciclo de vida da EC2.

### Outro banco relacional gerenciado

MySQL e SQL Server atenderiam ao requisito, mas exigiriam alterações de driver, SQL e testes sem benefício para o domínio atual.

### RDS Multi-AZ

Melhora a disponibilidade, mas aumenta o custo e não foi adotado no ambiente acadêmico.

## Consequências

### Positivas

- banco separado do ciclo de vida dos pods e da EC2;
- serviço gerenciado pela AWS;
- acesso privado dentro da VPC;
- compatibilidade com o código PostgreSQL existente.

### Negativas e riscos

- a configuração atual não possui Multi-AZ;
- `skip_final_snapshot=true` pode causar perda de dados ao destruir o recurso;
- a regra da porta 5432 aceita toda a VPC, sendo mais ampla que um Security Group de origem específico;
- credenciais e endpoint precisam ser distribuídos com segurança entre pipelines.

## Evidências de implementação

- `aws_db_instance.oficina_fiap` com engine PostgreSQL 16;
- `publicly_accessible=false`;
- DB subnet group e Security Group próprios;
- endpoint disponibilizado por output Terraform.
