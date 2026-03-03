# 🍕 Pizza Backend — RESTful API v2.1

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7.2-red.svg)](https://redis.io/)
[![Elasticsearch](https://img.shields.io/badge/Elasticsearch-8.11-yellow.svg)](https://www.elastic.co/)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue.svg)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Enterprise-grade Pizza Order Management System backend built with Spring Boot, featuring JWT authentication with refresh token rotation, Redis caching, Elasticsearch full-text search, Iyzico payment gateway integration, and a full Prometheus/Grafana/Loki monitoring stack.

> **Frontend Repository:** [nextjs-pizza](https://github.com/mburakaltiparmak/nextjs-pizza) — Next.js 15 App Router, Redux Toolkit, Tailwind CSS

**📊 Health Check:** `GET /pizza/actuator/health`

---

## 📋 Table of Contents

- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Project Structure](#-project-structure)
- [Getting Started](#-getting-started)
- [API Reference](#-api-reference)
- [Authentication Flow](#-authentication-flow)
- [Caching Strategy](#-caching-strategy)
- [Search Engine](#-search-engine)
- [Payment Integration](#-payment-integration)
- [Rate Limiting](#-rate-limiting)
- [Error Handling](#-error-handling)
- [Monitoring & Observability](#-monitoring--observability)
- [Docker Deployment](#-docker-deployment)
- [Environment Configuration](#-environment-configuration)
- [Contributing](#-contributing)
- [License](#-license)

---

## ✨ Features

### Security & Authentication

- JWT authentication with short-lived access tokens (30 min) and long-lived refresh tokens (7 days)
- Refresh token rotation with automatic reuse detection — if a revoked token is reused, all tokens for that user are invalidated
- OAuth2 integration via Google (Supabase JWKS verification)
- Role-based access control with three roles: `ADMIN`, `PERSONAL`, `CUSTOMER`
- Password encryption with BCrypt
- Email verification flow with token-based activation
- Password reset via email with expiring tokens
- CORS configuration with environment-based allowed origins

### Performance & Scalability

- Redis caching with TTL-based invalidation, reducing response times by ~85% on cached endpoints
- N+1 query elimination using `JOIN FETCH` strategies across all repository methods
- Pessimistic locking for stock management during concurrent orders
- Pagination support on all list endpoints with configurable page size and sort
- HikariCP connection pool tuned for deployment environment
- Response compression enabled in production (gzip for JSON, HTML, JS, CSS)
- Async thread pool configuration (10 core / 20 max in production)

### Search & Discovery

- Elasticsearch 8.11 integration for full-text product, category, order, and user search
- Fuzzy matching with typo tolerance for search queries
- Autocomplete suggestions endpoint for real-time search-as-you-type
- Multi-field search across name, description, and price fields
- Admin reindex endpoints for rebuilding search indices on demand

### Payment Processing

- Iyzico payment gateway integration (Turkish market standard)
- 3D Secure payment flow with callback handling
- Guest checkout support — orders can be placed without registration
- Payment status tracking with order lifecycle integration
- Proportional discount distribution across basket items for promo codes

### Monitoring & Observability

- Prometheus metrics collection with 30-day retention
- Grafana dashboards for JVM, HTTP, database, and business metrics
- Loki + Promtail for centralized Docker container log aggregation (7-day retention)
- node-exporter for host-level CPU, memory, disk, and network metrics
- cAdvisor for per-container resource usage monitoring
- Spring Boot Actuator with health checks and custom health indicators

---

## 🛠 Tech Stack

| Category         | Technology                  | Purpose                                 |
| ---------------- | --------------------------- | --------------------------------------- |
| Framework        | Spring Boot 3.4.2           | Application framework                   |
| Language         | Java 17                     | Backend language                        |
| Build            | Maven                       | Dependency management, build automation |
| Database         | PostgreSQL 16 (Supabase)    | Relational data store                   |
| ORM              | Hibernate / JPA             | Object-relational mapping               |
| Caching          | Redis 7.2                   | In-memory cache with TTL                |
| Search           | Elasticsearch 8.11          | Full-text search engine                 |
| Security         | Spring Security + JWT       | Authentication & authorization          |
| Payment          | Iyzico                      | Turkish payment gateway (3D Secure)     |
| File Storage     | Cloudinary                  | Cloud image upload & management         |
| Email            | SMTP (configurable)         | Transactional emails                    |
| Rate Limiting    | Bucket4j                    | Token bucket rate limiting              |
| Monitoring       | Prometheus + Grafana + Loki | Metrics, dashboards, logs               |
| Containerization | Docker + Docker Compose     | Deployment & orchestration              |

---

## 🏗 Architecture

```
┌──────────────┐     ┌──────────────────────────────────────────────────────┐
│   Frontend   │────▶│                  Spring Boot API                     │
│  (Next.js)   │◀────│                                                      │
└──────────────┘     │  ┌─────────┐  ┌──────────┐  ┌───────────────────┐   │
                     │  │ Security │  │Controllers│  │ Global Exception  │   │
                     │  │ Filters  │─▶│  (REST)   │  │    Handler        │   │
                     │  └─────────┘  └─────┬──────┘  └───────────────────┘   │
                     │                     │                                  │
                     │               ┌─────▼──────┐                          │
                     │               │  Services   │                          │
                     │               │  (Business  │                          │
                     │               │   Logic)    │                          │
                     │               └──┬───┬───┬──┘                          │
                     │                  │   │   │                              │
                     │        ┌─────────┘   │   └──────────┐                  │
                     │        ▼             ▼              ▼                  │
                     │  ┌──────────┐ ┌───────────┐ ┌─────────────┐           │
                     │  │PostgreSQL│ │   Redis    │ │Elasticsearch│           │
                     │  │  (JPA)   │ │  (Cache)   │ │  (Search)   │           │
                     │  └──────────┘ └───────────┘ └─────────────┘           │
                     └──────────────────────────────────────────────────────┘
                                            │
                     ┌──────────────────────┼──────────────────────┐
                     │         Monitoring Stack                    │
                     │  Prometheus ─▶ Grafana ─▶ Loki/Promtail   │
                     │  node-exporter    cAdvisor    Actuator     │
                     └────────────────────────────────────────────┘
```

---

## 📁 Project Structure

```
pizza-backend/
├── src/main/java/com/example/pizza/
│   ├── PizzaApplication.java
│   │
│   ├── config/
│   │   ├── security/          # SecurityConfig, CorsConfig, UnifiedTokenProvider
│   │   ├── auth/              # DotenvConfig, JwksService (Supabase)
│   │   ├── performance/       # RateLimitConfig, WebMvcConfig
│   │   ├── elasticsearch/     # ElasticsearchConfig
│   │   └── logic/             # CloudinaryConfig, IyzicoConfig, RedisConfig
│   │
│   ├── controller/
│   │   ├── AuthController.java
│   │   ├── ProductRestController.java
│   │   ├── CategoryRestController.java
│   │   ├── OrderRestController.java
│   │   ├── UserController.java
│   │   ├── AdminController.java
│   │   ├── PaymentController.java
│   │   └── SearchController.java
│   │
│   ├── service/
│   │   ├── user/              # UserService, SupabaseUserService
│   │   ├── product/           # ProductService, ProductSearchService
│   │   ├── category/          # CategoryService, CategorySearchService
│   │   ├── order/             # OrderService, OrderSearchService
│   │   ├── payment/           # IyzicoPaymentServiceImpl
│   │   └── logic/             # EmailService, RefreshTokenService, FileUploadImpl
│   │
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── ProductRepository.java
│   │   ├── CategoryRepository.java
│   │   ├── OrderRepository.java
│   │   ├── PaymentRepository.java
│   │   ├── RefreshTokenRepository.java
│   │   └── search/            # Elasticsearch repositories
│   │
│   ├── entity/
│   │   ├── user/              # User, UserAddress
│   │   ├── product/           # Product
│   │   ├── category/          # Category, CategoryDocument (ES)
│   │   ├── order/             # Order, OrderItem, OrderDocument (ES)
│   │   ├── token/             # RefreshToken, VerificationToken
│   │   └── logic/             # Payment
│   │
│   ├── dto/
│   │   ├── auth/              # LoginRequest, RegisterRequest, AuthResponse
│   │   ├── user/              # UserDTO, UserResponse
│   │   ├── product/           # ProductResponse, CustomPizzaRequest
│   │   ├── category/          # CategoryResponse
│   │   ├── order/             # OrderCreateRequest, OrderResponse, OrderStatusUpdateRequest
│   │   ├── payment/           # PaymentCardRequest, PaymentResponse
│   │   ├── paginate/          # PagedResponse
│   │   └── exceptions/        # ApiError, ApiResponse
│   │
│   ├── constants/
│   │   ├── user/              # Role, UserStatus
│   │   ├── order/             # OrderStatus, PaymentStatus
│   │   └── logic/             # DatabaseConstants
│   │
│   ├── exceptions/
│   │   ├── ApiGlobalExceptionHandler.java
│   │   ├── common/            # ResourceNotFoundException, FileOperationException
│   │   ├── user/              # UserRegistrationException
│   │   ├── order/             # InsufficientStockException, OrderCreationException
│   │   ├── token/             # RefreshTokenExpiredException, RefreshTokenRevokedException
│   │   └── base/              # ValidationException
│   │
│   └── logic/
│       ├── mapper/            # OrderMapper
│       └── interceptor/       # RateLimitInterceptor
│
├── src/main/resources/
│   ├── application.properties             # Base / common config
│   ├── application-dev.properties         # Development profile
│   ├── application-prod.properties        # Production profile
│   └── logback-spring.xml                 # Logging configuration
│
├── Dockerfile                 # Multi-stage build (Maven → JRE Alpine)
├── docker-compose.yml         # Full stack: app, postgres, redis, ES, monitoring
├── prometheus.yml             # Prometheus scrape configuration
├── loki-config.yml            # Loki storage and retention config
├── promtail-config.yml        # Promtail Docker log collection config
├── .env.example               # Template for environment variables
├── pom.xml                    # Maven dependencies and build config
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL 16 (or use Docker Compose)
- Redis 7.2 (or use Docker Compose)
- Elasticsearch 8.11 (or use Docker Compose)

### Option 1: Docker Compose (Recommended)

This starts the entire stack — application, database, cache, search engine, and monitoring.

```bash
# Clone the repository
git clone https://github.com/mburakaltiparmak/pizza-backend.git
cd pizza-backend

# Create environment file from template
cp .env.example .env
# Edit .env with your actual credentials (see Environment Configuration section)

# Start all services
docker-compose up -d

# Verify services are running
docker-compose ps

# Check application health
curl http://localhost:8080/pizza/actuator/health
```

### Option 2: Local Development

If you want to run the Spring Boot application directly while using Docker for infrastructure services:

```bash
# Start only infrastructure services
docker-compose up -d postgres redis elasticsearch

# Wait for services to be healthy, then run the application
mvn clean install -DskipTests
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The API will be available at `http://localhost:8080/pizza/api`.

---

## 📖 API Reference

**Base URL:** `http://localhost:8080/pizza/api`

### Authentication Endpoints

| Method | Endpoint                           | Access        | Description                                                |
| ------ | ---------------------------------- | ------------- | ---------------------------------------------------------- |
| POST   | `/auth/register`                   | Public        | Register a new user (status: PENDING until email verified) |
| POST   | `/auth/login`                      | Public        | Login and receive access + refresh tokens                  |
| POST   | `/auth/refresh-token`              | Public        | Get new access token using refresh token                   |
| POST   | `/auth/logout`                     | Authenticated | Revoke refresh token                                       |
| GET    | `/auth/verify-email?token={token}` | Public        | Verify email address                                       |
| POST   | `/auth/forgot-password`            | Public        | Request password reset email                               |
| POST   | `/auth/reset-password`             | Public        | Reset password with email token                            |
| POST   | `/auth/google`                     | Public        | OAuth2 login via Google/Supabase                           |

### Category Endpoints

| Method | Endpoint                         | Access | Description                                   |
| ------ | -------------------------------- | ------ | --------------------------------------------- |
| GET    | `/category`                      | Public | Get all categories (legacy, non-paginated)    |
| GET    | `/category/paged?page=0&size=10` | Public | Get categories with pagination                |
| GET    | `/category/simple`               | Public | Get category names and IDs only (lightweight) |
| GET    | `/category/{id}`                 | Public | Get single category by ID                     |
| GET    | `/category/search?query={q}`     | Public | Search categories (Elasticsearch)             |
| POST   | `/category`                      | Admin  | Create new category (multipart: name + image) |
| PUT    | `/category/{id}`                 | Admin  | Update category                               |
| DELETE | `/category/{id}`                 | Admin  | Delete category                               |

### Product Endpoints

| Method | Endpoint                         | Access | Description                              |
| ------ | -------------------------------- | ------ | ---------------------------------------- |
| GET    | `/product/paged?page=0&size=10`  | Public | Get products with pagination             |
| GET    | `/product/{id}`                  | Public | Get single product                       |
| GET    | `/product/category/{categoryId}` | Public | Get products by category                 |
| GET    | `/product/search?query={q}`      | Public | Full-text search (Elasticsearch)         |
| GET    | `/product/suggestions?query={q}` | Public | Autocomplete suggestions                 |
| POST   | `/product`                       | Admin  | Create product (multipart: data + image) |
| PUT    | `/product/{id}`                  | Admin  | Update product                           |
| DELETE | `/product/{id}`                  | Admin  | Delete product                           |

### Order Endpoints

| Method | Endpoint                             | Access                | Description                           |
| ------ | ------------------------------------ | --------------------- | ------------------------------------- |
| POST   | `/orders`                            | Authenticated / Guest | Create new order                      |
| GET    | `/orders/my-orders`                  | Authenticated         | Get current user's orders (paginated) |
| GET    | `/orders/admin/paged?page=0&size=20` | Admin                 | Get all orders (paginated)            |
| GET    | `/orders/admin/{id}`                 | Admin                 | Get order details by ID               |
| PUT    | `/orders/admin/{id}/status`          | Admin                 | Update order status                   |
| GET    | `/orders/admin/search?query={q}`     | Admin                 | Search orders (Elasticsearch)         |

### User Management Endpoints

| Method | Endpoint                | Access        | Description              |
| ------ | ----------------------- | ------------- | ------------------------ |
| GET    | `/user/profile`         | Authenticated | Get current user profile |
| PUT    | `/user/profile`         | Authenticated | Update profile           |
| POST   | `/user/change-password` | Authenticated | Change password          |
| GET    | `/user/addresses`       | Authenticated | Get user addresses       |
| POST   | `/user/addresses`       | Authenticated | Add new address          |
| DELETE | `/user/addresses/{id}`  | Authenticated | Delete address           |

### Admin Endpoints

| Method | Endpoint                            | Access | Description                                   |
| ------ | ----------------------------------- | ------ | --------------------------------------------- |
| GET    | `/admin/dashboard`                  | Admin  | Dashboard statistics (users, orders, revenue) |
| GET    | `/admin/users/paged?page=0&size=20` | Admin  | Get all users (paginated)                     |
| PUT    | `/admin/users/{id}/status`          | Admin  | Update user status (ACTIVE, LOCKED, etc.)     |
| PUT    | `/admin/users/{id}/role`            | Admin  | Update user role                              |
| GET    | `/admin/users/search?query={q}`     | Admin  | Search users (Elasticsearch)                  |
| POST   | `/admin/product/reindex`            | Admin  | Reindex products in Elasticsearch             |
| POST   | `/admin/category/reindex`           | Admin  | Reindex categories in Elasticsearch           |
| POST   | `/admin/users/reindex`              | Admin  | Reindex users in Elasticsearch                |
| POST   | `/orders/admin/reindex`             | Admin  | Reindex orders in Elasticsearch               |

### Payment Endpoints

| Method | Endpoint                    | Access        | Description                        |
| ------ | --------------------------- | ------------- | ---------------------------------- |
| POST   | `/payment/create`           | Authenticated | Create payment intent for an order |
| POST   | `/payment/3ds/callback`     | Internal      | Iyzico 3D Secure callback handler  |
| GET    | `/payment/status/{orderId}` | Authenticated | Check payment status               |

---

## 🔐 Authentication Flow

The application uses a dual-token JWT strategy:

```
┌────────┐                    ┌────────────┐                  ┌──────────┐
│ Client │                    │  Backend   │                  │ Database │
└───┬────┘                    └─────┬──────┘                  └────┬─────┘
    │  POST /auth/login             │                              │
    │  {email, password}            │                              │
    │──────────────────────────────▶│                              │
    │                               │  Validate credentials        │
    │                               │─────────────────────────────▶│
    │                               │◀─────────────────────────────│
    │                               │  Generate access token (30m) │
    │                               │  Generate refresh token (7d) │
    │                               │  Store refresh token ────────▶│
    │  {accessToken, refreshToken}  │                              │
    │◀──────────────────────────────│                              │
    │                               │                              │
    │  GET /api/... (protected)     │                              │
    │  Authorization: Bearer {at}   │                              │
    │──────────────────────────────▶│                              │
    │                               │  Validate JWT                │
    │  200 OK                       │                              │
    │◀──────────────────────────────│                              │
    │                               │                              │
    │  (access token expires)       │                              │
    │                               │                              │
    │  POST /auth/refresh-token     │                              │
    │  {refreshToken}               │                              │
    │──────────────────────────────▶│                              │
    │                               │  Validate + rotate token     │
    │                               │  Revoke old, create new ─────▶│
    │  {new accessToken,            │                              │
    │   new refreshToken}           │                              │
    │◀──────────────────────────────│                              │
```

**Reuse detection:** If a revoked refresh token is used again, the system assumes token theft and revokes all tokens for that user, forcing re-authentication on all devices.

---

## ⚡ Caching Strategy

Redis is used as a centralized cache with the following approach:

| Cache Key                | TTL    | Invalidation Trigger          |
| ------------------------ | ------ | ----------------------------- |
| `categories:all`         | 60 min | Category create/update/delete |
| `categories:{id}`        | 60 min | Category update/delete        |
| `products:page:{params}` | 30 min | Product create/update/delete  |
| `products:{id}`          | 30 min | Product update/delete         |
| `rate-limit:{key}`       | 1 min  | Auto-expire                   |

Cache invalidation uses Spring's `@CacheEvict` annotation with targeted key patterns. The `dev` profile uses the prefix `dev:` and the `prod` profile uses `prod:` to prevent cross-environment cache collisions.

---

## 🔍 Search Engine

Elasticsearch provides full-text search with the following capabilities:

- **Fuzzy matching** — tolerates 1-2 character typos depending on word length
- **Autocomplete** — returns top suggestions as the user types
- **Multi-field** — searches across name, description, and other relevant fields simultaneously
- **Admin reindex** — dedicated endpoints allow rebuilding indices without downtime

All entities are automatically indexed on create/update. The Elasticsearch documents (`CategoryDocument`, `OrderDocument`, etc.) are kept in sync with the PostgreSQL source of truth through service-layer indexing calls.

---

## 💳 Payment Integration

The application integrates with **Iyzico**, the leading payment gateway in Turkey, supporting:

- **Standard payments** — direct card charges
- **3D Secure flow** — redirects to bank for additional authentication, then handles callback
- **Guest checkout** — payment processing works for both authenticated users and guests
- **Promo codes** — discounts are proportionally distributed across basket items to match Iyzico's per-item pricing requirement

Payment lifecycle: `PENDING` → `PROCESSING` → `SUCCESS` / `FAILED`

Order confirmation emails are deferred for online payments until the payment callback confirms success. Cash/card-on-delivery orders trigger emails immediately.

---

## 🚦 Rate Limiting

Bucket4j token-bucket rate limiting is applied at the interceptor level:

| Endpoint Group          | Limit        | Window   |
| ----------------------- | ------------ | -------- |
| `/auth/login`           | 10 requests  | 1 minute |
| `/auth/register`        | 5 requests   | 1 minute |
| `/orders`               | 50 requests  | 1 minute |
| `/product`, `/category` | 200 requests | 1 minute |
| Default (all others)    | 100 requests | 1 minute |

Rate limits are tracked per IP for unauthenticated requests and per user ID for authenticated requests. The `dev` profile applies relaxed limits (1000 req/min) for development convenience.

Response headers include `X-Rate-Limit-Remaining` on success and `X-Rate-Limit-Retry-After-Seconds` on 429 responses.

---

## ❌ Error Handling

All errors are handled by a global `@RestControllerAdvice` exception handler that returns consistent JSON responses:

```json
{
  "message": "Ürün bulunamadı",
  "status": 404,
  "timestamp": "2025-01-15T10:30:00"
}
```

Validation errors include a field-level breakdown:

```json
{
  "timestamp": "2025-01-15T10:30:00",
  "status": 400,
  "errors": {
    "email": "Geçersiz email formatı",
    "password": "Şifre en az 6 karakter olmalıdır"
  }
}
```

Handled exception types include `ResourceNotFoundException`, `InsufficientStockException`, `BadCredentialsException`, `RefreshTokenExpiredException`, `ConstraintViolationException`, `MaxUploadSizeExceededException`, and generic fallback handling for unexpected errors.

Production mode (`application-prod.properties`) disables stack traces, binding errors, and exception details in responses for security.

---

## 📊 Monitoring & Observability

The `docker-compose.yml` includes a full monitoring stack:

| Service           | URL                                    | Purpose                                       |
| ----------------- | -------------------------------------- | --------------------------------------------- |
| **Prometheus**    | `http://localhost:9090`                | Metrics collection (30-day retention)         |
| **Grafana**       | `http://localhost:3001`                | Dashboards and visualization                  |
| **Loki**          | `http://localhost:3100`                | Centralized log aggregation (7-day retention) |
| **Promtail**      | —                                      | Collects Docker container logs → Loki         |
| **node-exporter** | `http://localhost:9100/metrics`        | Host OS metrics (CPU, memory, disk)           |
| **cAdvisor**      | `http://localhost:8081`                | Container resource metrics                    |
| **Actuator**      | `http://localhost:8080/pizza/actuator` | Application health, info, prometheus metrics  |

### Prometheus Scrape Targets

- Spring Boot app → `/pizza/actuator/prometheus` (JVM, HTTP, HikariCP, Redis, custom business metrics)
- node-exporter → system metrics
- cAdvisor → container metrics
- Prometheus self-monitoring

### Grafana Setup

Default credentials: `admin` / `admin` (change on first login).

Pre-configured data sources:

1. **Prometheus** → `http://prometheus:9090`
2. **Loki** → `http://loki:3100`

> **Production note:** Change the Grafana admin password, restrict monitoring endpoint access, and review retention periods for compliance requirements.

---

## 🐳 Docker Deployment

### Multi-Stage Dockerfile

The application uses a two-stage build:

1. **Build stage** — `maven:3.9.9-eclipse-temurin-17-alpine` compiles the application with dependency caching
2. **Runtime stage** — `eclipse-temurin:17-jre-alpine` runs the JAR as a non-root user (`appuser`)

```bash
# Build and run with Docker Compose
docker-compose up -d --build

# View logs
docker-compose logs -f app

# Stop all services
docker-compose down
```

### Services in docker-compose.yml

| Service         | Image                    | Port | Notes                       |
| --------------- | ------------------------ | ---- | --------------------------- |
| `app`           | Custom (Dockerfile)      | 8080 | Spring Boot application     |
| `postgres`      | postgres:16              | 5432 | Primary database            |
| `redis`         | redis:7.2-alpine         | 6379 | Cache layer                 |
| `elasticsearch` | elasticsearch:8.11.x     | 9200 | Search engine (single-node) |
| `prometheus`    | prom/prometheus          | 9090 | Metrics store               |
| `grafana`       | grafana/grafana          | 3001 | Dashboards                  |
| `loki`          | grafana/loki             | 3100 | Log aggregation             |
| `promtail`      | grafana/promtail         | —    | Log collection agent        |
| `node-exporter` | prom/node-exporter       | 9100 | Host metrics                |
| `cadvisor`      | gcr.io/cadvisor/cadvisor | 8081 | Container metrics           |

All services share a `pizza-network` bridge network. Health checks are configured for `postgres`, `redis`, `elasticsearch`, and the application itself.

---

## ⚙ Environment Configuration

Copy `.env.example` to `.env` and fill in your values:

```bash
# Database
POSTGRES_DB=postgres
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_secure_password
SPRING_DATASOURCE_PASSWORD=your_secure_password

# JWT
JWT_SECRET=your-512-bit-secret-key    # Must be 64+ characters
JWT_EXPIRATION=86400000                # Legacy fallback (24h)

# Supabase (OAuth2)
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your_anon_key
SUPABASE_SERVICE_KEY=your_service_key
SUPABASE_JWT_SECRET=your_jwt_secret

# Cloudinary (File Uploads)
CLOUD_NAME=your_cloud_name
API_KEY=your_api_key
API_SECRET=your_api_secret

# Email (SMTP)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
MAIL_FROM_ADDRESS=noreply@yourdomain.com

# Iyzico Payment
IYZICO_API_KEY=your_iyzico_api_key
IYZICO_SECRET_KEY=your_iyzico_secret_key
IYZICO_BASE_URL=https://sandbox-api.iyzipay.com
IYZICO_CALLBACK_URL=http://localhost:8080/pizza/api/payment/3ds/callback

# CORS
ALLOWED_ORIGINS=http://localhost:3000

# Grafana (optional)
GRAFANA_ADMIN_PASSWORD=your_grafana_password
```

### Spring Profiles

| Profile | Activation                    | Use Case                                                      |
| ------- | ----------------------------- | ------------------------------------------------------------- |
| `dev`   | Default                       | Local development, verbose logging, relaxed rate limits       |
| `prod`  | `SPRING_PROFILES_ACTIVE=prod` | Production — no stack traces, strict rate limits, compression |
| `test`  | `SPRING_PROFILES_ACTIVE=test` | Testing with mock configuration                               |

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Commit your changes (`git commit -m 'feat: add your feature'`)
4. Push to the branch (`git push origin feature/your-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

## 📬 Contact

**Burak Altıparmak**  
🌐 [burakaltiparmak.site](https://burakaltiparmak.site)  
📧 info@burakaltiparmak.site  
💼 [GitHub](https://github.com/mburakaltiparmak)
