# ADR-001 - Executar Kubernetes K3s em uma instância EC2

- **Estado:** Aceita e implementada
- **Data da consolidação:** 8 de setembro de 2026
- **Repositório principal:** `oficina-fiap-infra-k8s`

## Contexto

O Tech Challenge exige uma aplicação em Kubernetes com escalabilidade. O ambiente acadêmico possui restrição de tempo e créditos, tornando importante reduzir custo e complexidade operacional.

## Decisão

Executar um cluster Kubernetes de nó único usando K3s em uma instância Amazon EC2:

- Amazon Linux 2023;
- instância padrão `t3.medium`;
- disco raiz de 30 GB `gp3`;
- VPC e subnet padrão do laboratório;
- IP público associado;
- instalação automatizada do K3s pelo `user_data` do Terraform;
- API Kubernetes na porta 6443 e SSH na porta 22 restritos ao CIDR informado;
- tráfego HTTP na porta 80 permitido para exposição da aplicação;
- estado Terraform remoto em S3.

## Alternativas consideradas

### Amazon EKS

Serviço gerenciado e mais próximo de uma arquitetura corporativa, mas possui custo e complexidade maiores para o laboratório.

### Kubernetes instalado manualmente

Oferece maior controle, porém exige mais configuração e manutenção que o K3s.

### Kubernetes somente local

É barato para desenvolvimento, mas não atende à demonstração de deploy em nuvem.

## Consequências

### Positivas

- implantação simples com Terraform;
- baixo consumo de recursos;
- comandos e manifests Kubernetes permanecem compatíveis com `kubectl`;
- Traefik e componentes essenciais são disponibilizados pelo K3s.

### Negativas e riscos

- o nó único é um ponto único de falha;
- não existe alta disponibilidade do control plane;
- HPA aumenta pods, mas não cria novas máquinas;
- destruir a EC2 pode alterar o IP público e o kubeconfig;
- a equipe precisa administrar atualizações e disponibilidade do nó.

## Evidências de implementação

- recurso Terraform `aws_instance.k3s_node`;
- instalação por `https://get.k3s.io` no `user_data`;
- versão observada no cluster com sufixo `+k3s1`;
- outputs para IP público, SSH e obtenção do kubeconfig.
