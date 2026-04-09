# Onboarding da base `chat-enterprise`

Este documento resume a estrutura do repositório para quem está chegando agora.

## 1) Visão geral

O projeto é um **monorepo Maven multi-módulo** para serviços de chat, com foco em:

- `chat-service`: serviço principal de conversas/mensagens.
- `auth-service`: serviço de autenticação (ainda com implementação mínima).
- `shared-kernel`: tipos e contratos compartilhados entre serviços.

O módulo raiz (`platform-root`) apenas organiza build/dependências dos submódulos.

## 2) Estrutura de módulos

### `platform-root` (raiz)

- Empacotamento `pom` com módulos filhos.
- Centraliza versões e configurações de compilação.
- Usa Java 25 com `--enable-preview` no compilador/testes.

### `shared-kernel`

- Biblioteca (`jar`) com objetos de valor comuns.
- Hoje já possui IDs de domínio como `TenantId` e `UserId`.
- É dependência de `chat-service` e `auth-service`.

### `chat-service`

É o módulo mais completo e segue uma organização em camadas:

- `domain`: entidades, VOs, gateways (interfaces) e regras de negócio.
- `application`: DTOs e casos de uso (`*UseCase`).
- `infra`: persistência JPA, mapeamentos, Kafka, segurança e config.
- `api`: controladores REST e interceptador de autenticação WebSocket.

Principais fluxos:

1. **REST** recebe requisição e extrai identidade do JWT.
2. **UseCase** executa regra de negócio com objetos de domínio.
3. **Gateway** abstrai persistência para manter domínio desacoplado.
4. **Kafka/WebSocket** propagam eventos de mensagem para clientes em tempo real.

### `auth-service`

- Existe como módulo separado, mas ainda está inicial.
- Atualmente tem o entrypoint Spring Boot e propriedades básicas.

## 3) Infra local e dependências externas

O `docker-compose.yml` sobe dependências de desenvolvimento:

- PostgreSQL
- Redis
- Zookeeper
- Kafka

No `chat-service`, as configurações principais estão em `application.properties`:

- Porta `8081`
- PostgreSQL local em `localhost:5432`
- Kafka local em `localhost:9092`
- Flyway desabilitado no momento (`spring.flyway.enabled=false`)

## 4) Pontos importantes para saber cedo

1. **Módulo mais maduro hoje:** `chat-service`.
2. **Arquitetura já preparada para DDD/Clean-ish:** domínio + gateways + use cases + adapters.
3. **JWT provisório:** validação com chave simétrica local no `SecurityConfig`.
4. **Tempo real:** evento sai via Kafka e entra no WebSocket (`/topic/chat.{conversationId}`).
5. **Risco técnico atual:** há sinais de fase de transição (ex.: `flyway` desligado e dependências duplicadas no `chat-service/pom.xml`).

## 5) Sugestão de trilha de aprendizado (ordem prática)

1. Ler o `pom.xml` raiz para entender módulos/versões.
2. Mapear endpoints em `ChatController`.
3. Seguir o fluxo `sendMessage`:
   - Controller → `SendMessageUseCase`
   - Domain (`Conversation`/`Message`)
   - Gateway JPA
   - Kafka Producer/Consumer
   - Push WebSocket
4. Entender segurança (`SecurityConfig` + `WebSocketAuthInterceptor`).
5. Rodar infra com Docker e testar endpoint + assinatura STOMP.

## 6) Dicas práticas para evoluir com segurança

- **Comece por testes de caso de uso** (camada `application`) antes de mexer em infra.
- **Mantenha invariantes no domínio** (não no controller).
- **Evite acoplamento direto do domínio com JPA/Kafka**: continue usando gateways.
- **Padronize erros de API** (hoje há `IllegalArgumentException` em vários fluxos).
- **Planeje o caminho do Auth Service** para substituir segredo simétrico por JWK/RSA.

## 7) Comandos úteis

```bash
# Build completo
./mvnw clean verify

# Rodar só o chat-service
./mvnw -pl chat-service spring-boot:run

# Subir dependências locais
docker compose up -d
```

---

Se você está chegando agora, a melhor estratégia é tratar o `chat-service` como referência de arquitetura e usar o `shared-kernel` como núcleo de contratos estáveis.
