# 🔧 Sistema de Gestão de Oficina Mecânica

API back-end desenvolvida em **Java 21 + Spring Boot 4** para gestão de ordens de serviço, clientes, veículos e estoque de peças. Projeto do **Tech Challenge — Fase 1** da pós-graduação em Arquitetura de Software (FIAP / SOAT15).

---

## 🎯 Objetivo

Digitalizar e organizar o fluxo operacional de uma oficina mecânica, eliminando controles manuais e proporcionando:

- Rastreabilidade completa das ordens de serviço
- Gestão eficiente de clientes, veículos e estoque
- Acompanhamento em tempo real pelo cliente (sem necessidade de login)
- Segurança via JWT para operações administrativas

---

## 🧩 Funcionalidades

### Ordem de Serviço
- Criação de OS vinculada a cliente (CPF/CNPJ) e veículo
- Adição de serviços e peças/insumos do estoque
- Orçamento calculado automaticamente
- Fluxo completo de status com transições controladas

### Workflow de Status
```
ABERTA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO → APROVADA → EM_EXECUCAO → CONCLUIDA → ENTREGUE
                                                         ↘ CANCELADA (de qualquer etapa)
```

- Consulta pública da OS pelo cliente (sem JWT): `GET /api/v1/ordens/{id}` e `GET /api/v1/ordens/cliente/{clienteId}`
- Cálculo automático do `tempoMedioExecucaoMinutos` de cada serviço ao concluir a OS

### Gestão Administrativa (requer JWT)
- CRUD de Clientes (validação de CPF/CNPJ)
- CRUD de Veículos (validação de placa — Mercosul e padrão antigo)
- CRUD de Serviços (catálogo com tempo estimado)
- CRUD de Peças e Insumos (controle de estoque)

---

## 🏗️ Arquitetura

A arquitetura da Fase 3 está documentada em duas visões complementares:

- [Diagrama de Componentes UML](documentacao/diagrama-componentes-uml.md): componentes, interfaces oferecidas e dependências;
- [Visão arquitetural, implantação e CI/CD](documentacao/arquitetura-implantacao-cicd-fase3.md): AWS, Kubernetes, RDS, pipelines e New Relic.

As decisões técnicas e arquiteturais estão reunidas no [índice consolidado de RFCs e ADRs](documentacao/decisoes/README.md).

Monolito em camadas seguindo princípios de **Domain-Driven Design (DDD)**:

```
src/main/java/br/com/fiap/soat15/tc_oficina/
├── application/         # Controllers REST
├── domain/
│   ├── impl/            # Implementações dos Services (regras de negócio)
│   ├── model/           # DTOs
│   ├── service/         # Interfaces dos Services
│   └── validator/       # Validadores de CPF/CNPJ e placa
└── infrastructure/
    ├── config/          # SecurityConfig, OpenAPI
    ├── entity/          # Entidades JPA
    ├── exception/       # Tratamento global de erros
    ├── repository/      # Repositórios Spring Data JPA
    └── security/        # JWT (JwtService, JwtAuthFilter, UsuarioDetailsService)
```

---

## 🛠️ Tecnologias

| Tecnologia | Versão | Justificativa |
|---|---|---|
| Java | 21 | LTS atual, suporte a records e pattern matching |
| Spring Boot | 4.0.5 | Framework principal, versão mais recente |
| Spring Security + JWT (JJWT) | 0.12.6 | Autenticação stateless |
| Spring Data JPA + Hibernate | — | Mapeamento ORM |
| PostgreSQL | — | Banco relacional principal |
| H2 | — | Banco em memória exclusivo para testes |
| Springdoc OpenAPI (Swagger) | 2.8.8 | Documentação da API |
| JaCoCo | 0.8.12 | Cobertura de testes (mínimo 80%) |
| SonarQube | 26.x (community) | Análise estática de qualidade e segurança do código |
| Lombok | 1.18.42 | Redução de boilerplate |

> **Por que PostgreSQL?**
>
> O domínio de uma oficina mecânica tem operações que naturalmente precisam ser atômicas: ao concluir uma Ordem de Serviço, o sistema recalcula o tempo médio de execução de cada serviço e registra a data de fechamento tudo isso em uma única transação. Se qualquer parte falhar, nada deve ser salvo pela metade.
>
> Além disso, o modelo de dados tem relacionamentos importantes que precisam ser protegidos por integridade referencial: um veículo só pode existir vinculado a um cliente, uma OS só pode ter itens que referenciam serviços e peças cadastrados, e a placa do veículo precisa ser única no sistema. Tentar garantir essas regras na aplicação seria frágil o banco precisa ser o guardião final.
>
> O PostgreSQL atende tudo isso com suporte robusto a transações ACID, chaves estrangeiras e constraints. É gratuito, open source, tem integração nativa com Spring Data JPA.

---

## 🔐 Segurança

- **JWT** para autenticação de rotas administrativas
- Endpoints públicos (sem token): `/auth/**`, `/swagger-ui/**`, `GET /api/v1/ordens/{id}`, `GET /api/v1/ordens/cliente/{clienteId}`
- Validação de CPF/CNPJ (incluindo alfanumérico) e placa de veículo (Mercosul e padrão antigo)

---

## 🧪 Testes

- **212 testes** (unitários + integração) — 0 falhas
- **Cobertura de 92%** nos domínios críticos (mínimo exigido: 80%)
- Execução com relatório automático:

```bash
./mvnw test
# Relatório HTML gerado em: target/site/jacoco/index.html
```

---

## 📊 Qualidade de Código — SonarQube

A análise estática é feita com SonarQube Community via Docker.

### Subir o SonarQube

```bash
docker run -d --name sonarqube \
  -p 9000:9000 \
  sonarqube:community
```

Acesse **http://localhost:9000** (login: `admin` / `admin`).

### Gerar token

1. Avatar → **My Account → Security**
2. **Generate Tokens** → tipo `User Token` → **Generate**
3. Copie o token gerado

### Rodar a análise

```bash
./mvnw verify sonar:sonar -Dsonar.token=SEU_TOKEN_AQUI
```

O dashboard com Bugs, Vulnerabilities, Code Smells e cobertura fica em:
**http://localhost:9000/dashboard?id=oficina-fiap**

---

## 🚀 Como executar localmente

### Pré-requisitos
- Java 21+
- Maven 3.9+
- PostgreSQL rodando (ou via Docker — ver seção abaixo)

### Variável de ambiente obrigatória

```bash
export JWT_SECRET=sua-chave-secreta-com-minimo-32-caracteres
```

> No IntelliJ: **Edit Configurations → Environment Variables** → adicionar `JWT_SECRET=...`

### Rodando a aplicação

```bash
# Clonar o repositório
git clone <url-do-repositorio>
cd oficina_fiap

# Executar
./mvnw spring-boot:run
```

A aplicação sobe em: **http://localhost:8080**

Swagger disponível em: **http://localhost:8080/swagger-ui.html**

---

## 🐳 Docker

> Dentro da raíz do projeto, executar:

```bash
docker-compose up --build
```

---

## 📋 Principais Endpoints

### Autenticação
| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/auth/registro` | Criar usuário administrativo |
| POST | `/auth/login` | Login e obtenção do token JWT |

### Clientes
| Método | Endpoint | Auth |
|---|---|---|
| GET | `/api/v1/clientes` | 🔒 JWT |
| POST | `/api/v1/clientes` | 🔒 JWT |
| PUT | `/api/v1/clientes/{id}` | 🔒 JWT |
| DELETE | `/api/v1/clientes/{id}` | 🔒 JWT |

### Ordens de Serviço
| Método | Endpoint | Auth |
|---|---|---|
| POST | `/api/v1/ordens` | 🔒 JWT |
| GET | `/api/v1/ordens` | 🔒 JWT |
| GET | `/api/v1/ordens/{id}` | 🌐 Público |
| GET | `/api/v1/ordens/cliente/{clienteId}` | 🌐 Público |
| PATCH | `/api/v1/ordens/{id}/status` | 🔒 JWT |
| POST | `/api/v1/ordens/{id}/itens` | 🔒 JWT |
| DELETE | `/api/v1/ordens/{id}/itens/{itemId}` | 🔒 JWT |

> Lista completa no Swagger: **http://localhost:8080/swagger-ui.html**

---

## 👥 Grupo

SOAT15 — Pós-graduação em Arquitetura de Software · FIAP · Fase 1

