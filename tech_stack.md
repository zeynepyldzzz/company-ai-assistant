# Tech Stack

## Architecture & General Approach

| Layer | Technology / Detail |
|---|---|
| Architecture | Modular Monolith (Auth, Menu, Shuttle, Directory, Vehicle, Survey, Announcement, Work Schedule, Chatbot) |
| Chatbot L1 | Intent Detection (Embedding-based) & Template Responses (No LLM) |
| Chatbot L2 | RAG Document Assistant (IK Procedures, LLM with Citation) |

## Frontend

### Web & Admin Panel

| Layer | Technology |
|---|---|
| Library | React 18+ |
| Language | TypeScript |
| Build Tool | Vite (vite-plugin-pwa) |
| Styling | Tailwind CSS |
| State Management | TanStack Query |
| Components | TanStack Table + shadcn/ui |
| Experience | PWA (Manifest + Service Worker) |

### Mobile

| Layer | Technology |
|---|---|
| Framework | Expo (React Native) |
| Location Tracking | expo-location + expo-task-manager |
| Push Notifications | expo-notifications |
| Voice Input | expo-speech-recognition |
| Styling | NativeWind |

## Backend

| Layer | Technology |
|---|---|
| Language & API | Java 21 (LTS) + Spring Boot 3 (Spring Web) |
| Build Tool | Maven |
| ORM | Spring Data JPA (Hibernate) |
| DB Migration | Flyway |
| Real-time Comm. | Spring WebSocket (STOMP) |
| Async Processing | Spring `@Async` / `@Scheduled` + Outbox Table |
| Excel Processing | Apache POI |
| Logging | Logback (SLF4J) + File/PostgreSQL Sink |
| API Docs | springdoc-openapi (Swagger UI) |

## Database

| Layer | Technology |
|---|---|
| Engine | PostgreSQL 16+ |
| Vector Search | pgvector |
| Fuzzy Text Search | pg_trgm |
| Backup | pg_dump + cron |

## External Integrations & Shared Tools

| Service / Tool | Purpose |
|---|---|
| Microsoft Entra ID | SSO & 2FA Authentication |
| OAuth 2.0 / OIDC | JWT Token Management |
| Firebase (FCM) | Cross-platform Push Notifications |
| Google Maps API | Route optimization and ETA calculations |
| Geolocation API | User-to-station proximity checks |
| pnpm workspace | Monorepo structure for web, mobile, and shared packages |
| openapi-typescript | Automated TS type generation from Swagger |
| Zod | Shared schema validation for frontend and mobile |

## AI Layer

| Component | Technology / Model |
|---|---|
| Intent Embedding | bge-m3 (or multilingual-e5-large) |
| Local LLM Environment | Ollama (Docker Container) |
| Small Language Model | Phi-4 Mini / Qwen 2.5 (3-7B, GGUF via Ollama) |
| Orchestration | Framework-less custom RAG pipeline |

## Deployment & Infrastructure

| Tool | Purpose |
|---|---|
| Docker / Compose | Single-server container orchestration |
| Nginx | Reverse proxy, TLS termination, and API routing |
| GitHub Actions | CI/CD automation (Build, test, deploy) |
| Redis (Future) | Active vehicle location caching |
| MinIO (Future) | Scalable object storage for HR documentation |