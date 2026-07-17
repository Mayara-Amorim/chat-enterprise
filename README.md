<div align="center">
  <br />
  <img src="https://capsule-render.vercel.app/api?type=waving&color=0:8e2de2,100:4a00e0&height=220&section=header&text=Chat%20Microservice&fontSize=70&animation=fadeIn&fontAlignY=38&desc=Java%2025%20•%20Spring%20Boot%204%20•%20Kafka%20•%20WebSocket&descAlignY=55&descAlign=50" alt="header" />
  
  <br />
  
  <img src="https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring_Boot-4.0-6DB33F?style=for-the-badge&logo=spring&logoColor=white" />
  <img src="https://img.shields.io/badge/Apache_Kafka-Event_Driven-231F20?style=for-the-badge&logo=apachekafka&logoColor=white" />
  <img src="https://img.shields.io/badge/PostgreSQL-Persistence-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" />
  <img src="https://img.shields.io/badge/WebSocket-Real_Time-000000?style=for-the-badge&logo=socket.io&logoColor=white" />
  <img src="https://img.shields.io/badge/Docker-Container-2496ED?style=for-the-badge&logo=docker&logoColor=white" />

  <br /><br />

  <table>
    <tr>
      <td align="center" width="600px">
        <h3>🚧 PROJETO EM DESENVOLVIMENTO ATIVO 🚧</h3>
        <sub>Arquitetura Core funcional. Integração de Segurança e Gateway em progresso.</sub>
      </td>
    </tr>
  </table>
</div>

<br />

<div align="center">
  <h2>🌟 Sobre o Projeto</h2>
</div>

<p align="justify">
  Este não é apenas um sistema de chat. É uma <strong>implementação de referência</strong> para sistemas distribuídos de alta performance. O objetivo é resolver o desafio da <em>latência de escrita</em> e <em>entrega em tempo real</em> utilizando uma arquitetura orientada a eventos.
</p>

<p align="justify">
  O projeto segue rigorosamente os princípios de <strong>Clean Architecture</strong> e <strong>Domain-Driven Design (DDD)</strong>, garantindo que o núcleo da aplicação permaneça agnóstico a frameworks, protegendo as regras de negócio.
</p>

<br />

<table align="center">
  <tr>
    <td align="center" width="300">
      <img src="https://cdn-icons-png.flaticon.com/512/2620/2620582.png" width="50" />
      <h3>⚡ Alta Performance</h3>
      <p>Escrita assíncrona via Kafka.<br>O usuário nunca espera o banco de dados.</p>
    </td>
    <td align="center" width="300">
      <img src="https://cdn-icons-png.flaticon.com/512/1067/1067357.png" width="50" />
      <h3>🏗️ Clean Arch & DDD</h3>
      <p>Domínio isolado.<br>Camadas de Application e Infraestrutura desacopladas.</p>
    </td>
    <td align="center" width="300">
      <img src="https://cdn-icons-png.flaticon.com/512/9790/9790352.png" width="50" />
      <h3>📡 Tempo Real</h3>
      <p>WebSocket com protocolo STOMP.<br>Entrega instantânea (Push).</p>
    </td>
  </tr>
</table>

<br />

<h2 align="center">🗺️ Roadmap de Engenharia</h2>

<div align="center">
  <table>
    <thead>
      <tr>
        <th>Fase</th>
        <th>Status</th>
        <th>Progresso</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td><strong>Fase 1: Core Domain</strong></td>
        <td>✅ Concluído</td>
        <td>
          <img src="https://geps.dev/progress/100" alt="Progress" />
        </td>
      </tr>
      <tr>
        <td><strong>Fase 2: Mensageria (Kafka)</strong></td>
        <td>✅ Concluído</td>
        <td>
          <img src="https://geps.dev/progress/100" alt="Progress" />
        </td>
      </tr>
      <tr>
        <td><strong>Fase 3: WebSocket & Inbox</strong></td>
        <td>✅ Concluído</td>
        <td>
          <img src="https://geps.dev/progress/100" alt="Progress" />
        </td>
      </tr>
      <tr>
        <td><strong>Fase 4: Segurança (OAuth2)</strong></td>
        <td>🔄 Em Andamento</td>
        <td>
          <img src="https://geps.dev/progress/15" alt="Progress" />
        </td>
      </tr>
      <tr>
        <td><strong>Fase 5: API Gateway</strong></td>
        <td>⏳ Planejado</td>
        <td>
          <img src="https://geps.dev/progress/0" alt="Progress" />
        </td>
      </tr>
    </tbody>
  </table>
</div>
<h2 align="center">📐 Arquitetura do Sistema</h2>

```mermaid
graph TD
    User((User Client))
    
    subgraph "Edge Layer"
        LB[Load Balancer]
        WS[WebSocket Broker]
    end
    
    subgraph "Core Services"
        API[Chat Controller]
        Domain[Domain Layer]
        Repo[Postgres Repo]
    end
    
    subgraph "Infrastructure"
        DB[(PostgreSQL)]
        Kafka{Apache Kafka}
        KConsumer[Kafka Consumer]
    end

    User -->|HTTP POST| LB
    LB --> API
    API --> Domain
    Domain -->|Save Async| Kafka
    
    Kafka -->|Consume| KConsumer
    KConsumer -->|Persist| Repo
    Repo --> DB
    
    KConsumer -->|Push Event| WS
    WS -.->|WebSocket| User
    
    style Kafka fill:#231F20,stroke:#fff,color:#fff
    style DB fill:#336791,stroke:#fff,color:#fff
    style Domain fill:#f9f,stroke:#333,stroke-width:2px
```
  <div align="center">
  <hr />
  <p>
    <sub>Desenvolvido por <strong>Mayara Amorim</strong></sub>
    <br />
  </p>
</div>
