# 🛒 Marketplace API

> Marketplace de ingressos com bounded contexts e bancos de dados diferentes — DDD, Clean Architecture e persistência poliglota em Spring Boot.

---

## 💡 Sobre o Projeto

Um sistema modular que simula um marketplace de ingressos para eventos, composto por **3 Bounded Contexts** independentes que se comunicam via **eventos de domínio**, cada um com seu próprio banco de dados — aplicando na prática o padrão de **Persistência Poliglota**.

Este projeto foi desenvolvido durante o **Bootcamp AI Java back-end da Santander** pela [DIO](https://www.dio.me/).

---

## 🤔 Por que essa Arquitetura?

Em aplicações reais, nem todo dado tem a mesma natureza. Forçar um único banco de dados para tudo gera compromissos — o que é bom para transações fortes pode ser ruim para buscas flexíveis ou cache de alta performance.

A ideia da **Persistência Poliglota** é simples: **usar o banco certo para o problema certo**.

| Módulo | Banco | Por quê? |
|---|---|---|
| **Registration** | MySQL | Dados estruturados de clientes com relacionamentos (endereço) — banco relacional clássico |
| **Catalog** | MySQL + MongoDB + Redis | Dados do evento em MySQL, metadata flexível (setores, requisitos técnicos) em MongoDB (schema-free), e cache em Redis para alta performance na vitrine |
| **Ticketing** | PostgreSQL + Redis | Transações ACID fortes para reserva de assentos em PostgreSQL, com Redis para locks distribuídos e evitar reservas duplicadas |

### ✅ Vantagens

- **Banco ideal para cada caso de uso** — dados relacionais em SQL, documentos flexíveis em MongoDB, cache/locks em Redis
- **Módulos independentes** — cada bounded context pode escalar, evoluir e ser deployado de forma independente
- **Desacoplamento via eventos** — módulos não se chamam diretamente, reduzindo dependências e pontos de falha
- **Performance** — a vitrine do catálogo é cacheada em Redis, evitando queries pesadas a cada requisição
- **Resiliência** — se o banco do catálogo cair, o registro de clientes e a venda de ingressos continuam funcionando

### ⚠️ Desvantagens

- **Complexidade operacional** — 5 containers de banco para gerenciar, cada um com backup, monitoramento e tuning próprio
- **Consistência eventual** — como módulos se comunicam via eventos assíncronos, há uma janela onde os dados podem estar desatualizados entre módulos
- **Curva de aprendizado** — exige conhecimento de múltiplas tecnologias de banco (SQL, NoSQL, key-value)
- **Configuração verbosa** — cada `DataSource`, `EntityManagerFactory` e `TransactionManager` precisa ser configurado manualmente no Spring
- **Debugging mais difícil** — rastrear um fluxo que passa por 3 bancos diferentes requer mais cuidado

> **Em resumo:** essa arquitetura faz sentido quando os módulos têm **necessidades de dados fundamentalmente diferentes**. Para projetos menores ou com requisitos homogêneos, um único banco relacional é mais pragmático.

---

## 🏗️ Arquitetura

O projeto segue os princípios de **DDD (Domain-Driven Design)** com **Clean Architecture**, organizado em módulos isolados:

```
┌─────────────────────────────────────────────────────────────────┐
│                      MarketplaceApplication                     │
│                   @EnableAsync  @EnableCaching                  │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────────────┐  │
│  │ Registration │  │   Catalog    │  │      Ticketing        │  │
│  │   (MySQL)    │  │(MySQL+Mongo  │  │  (PostgreSQL+Redis)   │  │
│  │              │  │    +Redis)   │  │                       │  │
│  └──────┬───────┘  └──────┬───────┘  └───────────┬───────────┘  │
│         │                 │                      │              │
│         └────── Spring Application Events ───────┘              │
│                 (CustomerCreated, EventUpdated)                 │
└─────────────────────────────────────────────────────────────────┘
```

Cada módulo segue internamente a **Clean Architecture** em 3 camadas:

```
┌──────────────────────────────────────┐
│         Infrastructure               │  ← Controllers, JPA/Mongo Repos, Listeners
│  ┌──────────────────────────────┐    │
│  │        Application           │    │  ← Use Cases, DTOs de entrada/saída
│  │  ┌──────────────────────┐    │    │
│  │  │       Domain         │    │    │  ← Entidades, Value Objects, Interfaces
│  │  └──────────────────────┘    │    │
│  └──────────────────────────────┘    │
└──────────────────────────────────────┘
```

> O domínio de cada módulo **não depende de nenhum framework** — pode ser reutilizado em qualquer contexto.

---

## 📦 Módulos (Bounded Contexts)

### 📋 Registration — Cadastro de Clientes

| | |
|---|---|
| **Banco** | MySQL (porta 3307) |
| **Responsabilidade** | CRUD de clientes via Spring Data REST + HAL Explorer |
| **Domínio** | `Customer`, `CustomerId`, `CustomerRepository` |
| **Infra** | JPA entities (`Customer`, `Address`), projection (`CustomerExcerpt`) |
| **Eventos** | Publica `CustomerCreated` ao registrar um novo cliente |

### 🎭 Catalog — Vitrine de Eventos

| | |
|---|---|
| **Banco** | MySQL (3308) + MongoDB (27018) + Redis (6380) |
| **Responsabilidade** | Exibir eventos com metadados enriquecidos e cache |
| **Domínio** | `Event`, `EventId`, `Seat`, `SeatId`, `Sector`, `SectorId`, `EventMetadata` |
| **Application** | `BrowseShowcaseUseCase`, `EventEnricher` |
| **Infra** | JPA repo (MySQL), MongoDB repo (metadata), Redis (cache), listeners |
| **Eventos** | Consome `EventUpdated` para sincronizar dados dos eventos |

### 🎫 Ticketing — Venda de Ingressos

| | |
|---|---|
| **Banco** | PostgreSQL (5433) + Redis (6381) |
| **Responsabilidade** | Criar eventos/clientes e processar reserva de assentos |
| **Domínio** | `Event`, `Customer`, `Seat`, `Sector` + exceções de negócio |
| **Application** | `CreateCustomerUseCase`, `CreateEventUseCase`, `SelectSeatUseCase` |
| **Infra** | CrudRepository (Postgres), `RedisSeatLockRepository` (locks de assentos) |
| **Eventos** | Consome `CustomerCreated`, publica `EventUpdated` |

---

## 🔄 Comunicação entre Módulos

Os módulos se comunicam de forma **desacoplada** via **Spring Application Events**:

```
Registration ──── CustomerCreated ────► Ticketing
                                          │
Ticketing    ──── EventUpdated ─────► Catalog
```

| Evento | Publicado por | Consumido por | Descrição |
|---|---|---|---|
| `CustomerCreated` | Registration | Ticketing | Sincroniza o cliente no módulo de vendas |
| `EventUpdated` | Ticketing | Catalog | Atualiza a vitrine com setores e assentos |

---

## 🗄️ Persistência Poliglota

Cada módulo usa o banco mais adequado para seu caso de uso:

| Módulo | Banco Relacional | Banco Documento | Cache/Lock |
|---|---|---|---|
| Registration | MySQL | — | — |
| Catalog | MySQL | MongoDB | Redis |
| Ticketing | PostgreSQL | — | Redis |

> Cada módulo possui seu próprio `DataSource`, `EntityManagerFactory` e `TransactionManager` configurados de forma isolada.

---

## 🧩 Design Patterns Utilizados

| Padrão | Onde foi aplicado |
|---|---|
| **Repository** | Interfaces de domínio (`EventRepository`, `CustomerRepository`) + implementações JPA/Mongo/Redis |
| **Use Case / Command** | Cada operação é uma classe isolada (`SelectSeatUseCase`, `BrowseShowcaseUseCase`, etc.) |
| **DTO** | `EventOutput`, `SeatSelectionRequest`, `CustomerCreated`, `EventUpdated` |
| **Value Object** | `EventId`, `CustomerId`, `SeatId`, `SectorId` |
| **Factory Method** | `EventOutput.from(Event)`, `EventMetadataOutput.from(EventMetadata)` |
| **Domain Events** | `CustomerCreated`, `EventUpdated` via Spring `@EventListener` |
| **Unit of Work** | `WorkOfUnitEventRepository` no módulo Ticketing |
| **Polyglot Persistence** | Cada módulo com seu banco de dados independente |

---

## 🛠️ Tecnologias

| Tecnologia | Versão |
|---|---|
| Java | 25 |
| Spring Boot | 4.0.6 |
| Gradle | 9.5 |
| Lombok | ✔ |
| MySQL | 9.6 |
| PostgreSQL | 18.3 |
| MongoDB | 8.2 |
| Redis | 8.6 |
| Docker Compose | ✔ |
| Bean Validation (Jakarta) | ✔ |
| Virtual Threads | ✔ |
| Spring Data REST + HAL Explorer | ✔ |
| Spring Actuator | ✔ |

---

## 🔌 Endpoints

### Registration (Spring Data REST — automático)

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/customers` | Cadastrar novo cliente |
| `GET` | `/customers` | Listar clientes |
| `GET` | `/customers/{id}` | Buscar cliente por ID |
| `PATCH` | `/customers/{id}` | Atualizar cliente (parcial) |
| `DELETE` | `/customers/{id}` | Deletar cliente |
| `GET` | `/explorer` | HAL Explorer (navegação interativa) |

### Catalog

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/showcase` | Listar vitrine de eventos (com metadata e cache) |

### Ticketing

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/ticketing/events/{eventId}/seats/select` | Reservar um assento |

### Exemplos

**Cadastrar cliente:**
```json
POST /customers
{
  "firstName": "Murilo",
  "lastName": "Dias",
  "email": "murilo@email.com",
  "address": {
    "street": "Rua A",
    "city": "São Paulo",
    "state": "SP"
  }
}
```

**Consultar vitrine:**
```json
GET /showcase

// Resposta (200 OK):
[
  {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "title": "Rock in Rio 2026",
    "date": "2026-09-15T18:00:00Z",
    "metadata": {
      "eventDescription": "Festival de música",
      "technicalRequirements": { "stage": "main", "capacity": 80000 },
      "seatsBySector": {
        "VIP": [
          { "id": "A1", "sectorId": "VIP", "price": 1200.00 }
        ]
      }
    }
  }
]
```

**Reservar assento:**
```bash
POST /ticketing/events/{eventId}/seats/select
X-CUSTOMER-ID: 3fa85f64-5717-4562-b3fc-2c963f66afa6

{
  "id": "A1"
}

# Resposta: 201 Created
```

---

## 📂 Estrutura do Projeto

```
src/main/java/com/murilocdias/marketplace/
├── MarketplaceApplication.java
├── common/
│   └── infrastructure/event/dto/
│       ├── CustomerCreated.java          # Evento de domínio
│       └── EventUpdated.java            # Evento de domínio
├── registration/
│   ├── domain/
│   │   ├── Customer.java                # Entidade de domínio
│   │   ├── CustomerId.java              # Value Object
│   │   └── CustomerRepository.java      # Interface do repositório
│   └── infrastructure/
│       ├── RegistrationConfiguration.java
│       ├── event/
│       │   └── CustomerEventHandler.java
│       └── persistence/
│           ├── entity/
│           │   ├── Customer.java         # Entidade JPA
│           │   └── Address.java          # Entidade JPA embeddable
│           └── repository/
│               ├── CustomerEntityRepository.java
│               └── JpaCustomerRepository.java
├── catalog/
│   ├── domain/
│   │   ├── Event.java                   # Aggregate Root
│   │   ├── EventId.java                 # Value Object
│   │   ├── Seat.java / SeatId.java
│   │   ├── Sector.java / SectorId.java
│   │   ├── EventMetadata.java
│   │   ├── EventRepository.java
│   │   └── EventMetadataRepository.java
│   ├── application/
│   │   ├── BrowseShowcaseUseCase.java
│   │   ├── EventEnricher.java
│   │   └── dto/
│   │       └── EventOutput.java         # DTO de saída (Serializable p/ cache)
│   ├── CatalogConfiguration.java
│   └── infrastructure/
│       ├── event/
│       │   ├── EventListener.java
│       │   └── EventMetadataListener.java
│       ├── http/
│       │   └── ShowcaseController.java
│       └── persistence/
│           ├── entity/
│           │   ├── Event.java            # Entidade JPA (MySQL)
│           │   └── EventMetadata.java    # Document (MongoDB)
│           └── repository/
│               ├── EventEntityRepository.java
│               ├── JpaEventRepository.java
│               ├── EventMetadataEntityRepository.java
│               └── MongoEventMetadataRepository.java
└── ticketing/
    ├── domain/
    │   ├── Event.java                   # Aggregate Root
    │   ├── EventId.java / CustomerId.java
    │   ├── Seat.java / SeatId.java
    │   ├── Sector.java / SectorId.java
    │   ├── Customer.java
    │   ├── CustomerRepository.java
    │   ├── EventRepository.java
    │   ├── SeatAlreadyReservedException.java
    │   └── SeatNotFoundException.java
    ├── application/
    │   ├── CreateCustomerUseCase.java
    │   ├── CreateEventUseCase.java
    │   └── SelectSeatUseCase.java
    ├── TicketingConfiguration.java
    └── infrastructure/
        ├── event/
        │   └── TicketingEventListener.java
        ├── http/
        │   ├── SeatSelectionController.java
        │   └── Request/
        │       └── SeatSelectionRequest.java
        └── persistence/
            ├── entity/
            │   ├── Customer.java         # Entidade JPA (Postgres)
            │   ├── Event.java
            │   ├── Seat.java
            │   ├── SeatLock.java         # Entidade Redis (lock)
            │   └── Sector.java
            └── repository/
                ├── CustomerCrudRepository.java
                ├── EventCrudRepository.java
                ├── PostgresCustomerRepository.java
                ├── RedisSeatLockRepository.java
                └── WorkOfUnitEventRepository.java
```

---

## 🐳 Infraestrutura (Docker Compose)

Todos os bancos são provisionados automaticamente via Docker Compose:

| Serviço | Imagem | Porta | Uso |
|---|---|---|---|
| `registration-database` | mysql:9.6 | 3307 | Clientes |
| `catalog-database` | mysql:9.6 | 3308 | Eventos (catálogo) |
| `catalog-metada-database` | mongo:8.2 | 27018 | Metadata dos eventos |
| `catalog-cache` | redis:8.6 | 6380 | Cache da vitrine |
| `ticketing-database` | postgres:18.3 | 5433 | Venda de ingressos |

---

## 🚀 Como Executar

**Pré-requisitos:** Java 25+ e Docker

```bash
# Clonar o repositório
git clone https://github.com/MuriloDias03/marketplace-java-spring-boot-jpa.git

# Entrar no diretório
cd marketplace-java-spring-boot-jpa

# Rodar a aplicação (Docker Compose sobe automaticamente)
./gradlew bootRun

# A API estará disponível em http://localhost:8080
# HAL Explorer em http://localhost:8080/explorer
```

> O Spring Boot Docker Compose (`spring-boot-docker-compose`) inicia os containers automaticamente ao rodar a aplicação.

---

<div align="center">

[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/murilo-cristovao-dias/)

</div>
