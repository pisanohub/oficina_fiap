# RFC-002 - Exposição da Spring API para o API Gateway

- **Estado:** Aprovada e implementada
- **Data da consolidação:** 8 de setembro de 2026
- **Última revisão:** 12 de setembro de 2026
- **Escopo:** `oficina-fiap-infra-k8s`, `oficina-fiap-lambda-auth` e `oficina_fiap`

## Contexto

O Amazon API Gateway usa uma integração `HTTP_PROXY` e encaminha `/api/{proxy+}` para `${APP_BASE_URL}/api/{proxy}`.

A Spring API é executada em K3s e seu Service Kubernetes é do tipo `ClusterIP`. O acesso externo é fornecido por um manifesto de Ingress que utiliza o Traefik do K3s na porta 80 da instância EC2.

Sem um endpoint acessível pela AWS, o API Gateway não consegue encaminhar as requisições autorizadas para a aplicação.

## Solução adotada para o ambiente educacional

Usar o Traefik incluído no K3s como Ingress Controller:

```text
API Gateway
    -> HTTP na porta 80 da EC2
    -> Traefik / Ingress
    -> Service spring-api:8080
    -> Pods da Spring API
```

O manifesto de Ingress encaminha o prefixo `/api` para o Service `spring-api`. `APP_BASE_URL` recebe a URL base, sem `/api` ao final.

Enquanto o endereço da EC2 permanecer público, a Spring API deve continuar validando o JWT para impedir que alguém contorne o API Gateway.

## Alternativas consideradas

### Traefik Ingress na porta 80

Menor complexidade e aproveita um componente normalmente instalado com K3s. É adequado à demonstração, mas o backend continua publicamente alcançável e o IP pode mudar quando a EC2 for recriada.

### Service `NodePort`

É simples, mas expõe uma porta alta diretamente no nó e exige outra regra no Security Group. Possui pior contrato externo e não é recomendado.

### Load Balancer, ALB ou API Gateway com VPC Link

Oferece uma arquitetura mais robusta e pode manter o backend privado, mas aumenta custo, quantidade de recursos e configuração para o laboratório.

## Consequências da proposta

### Positivas

- usa o Traefik já associado ao K3s;
- aproveita a porta 80 existente no Security Group;
- entrega uma URL utilizável como `APP_BASE_URL`;
- exige poucos recursos adicionais.

### Negativas e riscos

- acesso direto ao backend precisa continuar protegido;
- o IP público da EC2 pode mudar depois de destruir e recriar a infraestrutura;
- `APP_BASE_URL` precisa ser atualizada quando o endereço mudar;
- HTTP sem TLS não é apropriado para produção real.

## Critérios de aceite

No runner ou na máquina com acesso ao cluster:

```bash
kubectl get pods -n kube-system
kubectl get ingress -A
kubectl get service,endpoints -n of-fiap
```

Fora do cluster, a URL pública deve responder. Em uma rota protegida, receber HTTP 401 ou 403 sem token comprova conectividade; timeout ou conexão recusada indica falha de exposição.

O teste ponta a ponta executado foi:

1. configurar `APP_BASE_URL` no repositório da Lambda;
2. implantar API Gateway e Lambdas;
3. obter o JWT com CPF;
4. chamar uma rota protegida pela URL do Gateway;
5. confirmar que a resposta veio da Spring API.

## Resultado da implementação

O Ingress Traefik foi implantado e a Spring API tornou-se acessível ao HTTP Proxy do API Gateway. O fluxo protegido foi validado de ponta a ponta. Como o ambiente do AWS Academy é recriado para controlar custos, o IP público não é tratado como endereço permanente e `APP_BASE_URL` precisa ser atualizado após uma recriação da infraestrutura.
