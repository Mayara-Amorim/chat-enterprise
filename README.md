# Chat Enterprise

Projeto multi-módulo em Spring Boot para uma plataforma de chat corporativo, com separação entre serviços de autenticação e chat.

## Estrutura atual

- `auth-service`: serviço de autenticação (ainda mínimo).
- `chat-service`: APIs de conversa/mensagens, integração com PostgreSQL, Kafka e WebSocket.
- `shared-kernel`: value objects e contratos compartilhados.

## Melhorias sugeridas (prioridade)

### 1) Qualidade e segurança (alta)
- **Adicionar suíte de testes automatizados** (unitários + integração): hoje o repositório não contém testes, o que aumenta o risco de regressão.
- **Remover segredos hardcoded e logs de debug em produção**: o `chat-service` está com usuário/senha de banco e logs verbosos no `application.properties`.
- **Ajustar CORS e autenticação WebSocket**: atualmente há configuração permissiva (`*`) e JWT com chave default de desenvolvimento.

### 2) Confiabilidade do ambiente local (alta)
- **Corrigir e padronizar o `docker-compose`**:
  - revisar o volume do PostgreSQL;
  - incluir healthchecks para Kafka/Postgres/Redis;
  - considerar profiles para subir apenas dependências necessárias.
- **Criar bootstrap de ambiente** (ex.: `make up`, `make down`, `make test`).

### 3) Observabilidade e operação (média)
- **Adicionar Actuator + métricas** (Prometheus) e logs estruturados.
- **Adicionar tracing distribuído** para fluxo assíncrono (Kafka) e requisições HTTP/WebSocket.
- **Documentar runbook básico** (start, troubleshooting, dependências).

### 4) Evolução de domínio e produto (média)
- **Padronizar contratos de eventos Kafka** com versionamento.
- **Definir estratégias de idempotência e retry** para consumo de mensagens.
- **Criar endpoints de saúde e readiness/liveness** para orquestração.

## O que falta para o projeto ficar “produção-ready”

Checklist mínimo:

- [ ] Testes automatizados em CI.
- [ ] Pipeline CI/CD (build, testes, análise estática e publicação).
- [ ] Gestão de segredos/variáveis por ambiente (sem segredos em arquivo).
- [ ] Estratégia de migração de banco com Flyway/Liquibase habilitado por ambiente.
- [ ] Política de segurança (CORS, autenticação, autorização e rotação de chaves JWT).
- [ ] Observabilidade (métricas, tracing, alertas).
- [ ] Documentação de arquitetura + guia de execução local.

## Próximo passo recomendado (1 sprint)

1. Criar pipeline CI com execução de testes e validação de estilo.
2. Escrever testes de regressão para casos críticos do `chat-service`.
3. Externalizar configurações sensíveis para variáveis de ambiente.
4. Endurecer segurança (CORS/JWT/WebSocket) e revisar defaults de dev.
