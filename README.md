# <img src="https://raw.githubusercontent.com/Alovoa/alovoa/master/src/main/resources/static/img/android-chrome-192x192.png" width="60"> AURA

### Advanced User Relationship Architecture

[![GitHub](https://img.shields.io/badge/GitHub-tashiscool%2Falovoa-181717?logo=github)](https://github.com/tashiscool/alovoa)
[![CI/CD](https://github.com/tashiscool/alovoa/actions/workflows/ci.yml/badge.svg)](https://github.com/tashiscool/alovoa/actions/workflows/ci.yml)
[![CodeQL](https://github.com/tashiscool/alovoa/actions/workflows/codeql.yml/badge.svg)](https://github.com/tashiscool/alovoa/actions/workflows/codeql.yml)
[![License](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)

---

**AURA** is a comprehensive, safety-focused dating platform built on [Alovoa](https://github.com/Alovoa/alovoa). It features AI-powered compatibility matching, video verification, public accountability, and values-based community gating.

<table>
<tr>
<td width="50%">

### Why AURA?

- **AI-Powered Matching** - Multi-dimensional compatibility scoring
- **Video Verification** - Liveness detection & deepfake protection
- **Public Accountability** - Transparent community feedback
- **Values Alignment** - Political/economic assessment gating
- **Anti-Ghosting** - 24-hour match decision windows
- **Privacy First** - No GPS tracking, user-declared areas only
- **100% Free** - No paywalls, donation-supported

</td>
<td width="50%">

### Tech Stack

```
Backend:     Spring Boot 3.x + Java 17
Database:    MariaDB/MySQL + Flyway
Storage:     S3/MinIO
Auth:        Session + OAuth2
Real-time:   WebSocket
AI:          Video analysis, compatibility
Vectors:     Pinecone/pgvector
```

</td>
</tr>
</table>

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              AURA Platform                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐  │
│  │   Web UI     │   │  Mobile App  │   │   REST API   │   │  WebSocket   │  │
│  │  (Thymeleaf) │   │   (Expo)     │   │  (JSON)      │   │  (Chat)      │  │
│  └──────┬───────┘   └──────┬───────┘   └──────┬───────┘   └──────┬───────┘  │
│         │                  │                  │                  │          │
│         └──────────────────┴──────────────────┴──────────────────┘          │
│                                    │                                         │
│  ┌─────────────────────────────────┼─────────────────────────────────────┐  │
│  │                        Spring Boot Application                         │  │
│  ├───────────────────────────────────────────────────────────────────────┤  │
│  │  ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌──────────┐ │  │
│  │  │  Profile  │ │  Matching │ │Assessment │ │   Video   │ │Reputation│ │  │
│  │  │  Service  │ │  Service  │ │  Service  │ │  Service  │ │ Service  │ │  │
│  │  └───────────┘ └───────────┘ └───────────┘ └───────────┘ └──────────┘ │  │
│  │  ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌──────────┐ │  │
│  │  │   Essay   │ │Relationship│ │  Message  │ │ Calendar  │ │ Donation │ │  │
│  │  │  Service  │ │  Service  │ │  Service  │ │  Service  │ │ Service  │ │  │
│  │  └───────────┘ └───────────┘ └───────────┘ └───────────┘ └──────────┘ │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                    │                                         │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐  │
│  │   MariaDB    │   │    MinIO     │   │   AI/ML      │   │   Stripe     │  │
│  │  (Primary)   │   │   (Media)    │   │  Services    │   │  (Payments)  │  │
│  └──────────────┘   └──────────────┘   └──────────────┘   └──────────────┘  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Feature Matrix

### User Profile & Identity

| Feature | Description | Status |
|---------|-------------|:------:|
| **Extended Profile Details** | Height, body type, ethnicity, diet, pets, education, occupation, languages, zodiac, income (9 brackets) | ✅ |
| **10 Essay Prompts** | OKCupid-style fixed prompts (2000 char each) | ✅ |
| **Profile Media** | Multiple images, audio intro, video intro with AI analysis | ✅ |
| **Linked Relationships** | "In a relationship with..." Facebook-style linking | ✅ |
| **Profile Completeness** | Track and encourage profile completion | ✅ |

### Matching & Discovery

| Feature | Description | Status |
|---------|-------------|:------:|
| **AI Compatibility Scoring** | 6 dimensions + enemy score | ✅ |
| **24-Hour Match Windows** | Timed decisions prevent ghosting | ✅ |
| **Daily Match Limits** | Quality over quantity (default: 5/day) | ✅ |
| **Smart Location Matching** | Privacy-safe with travel time prefs | ✅ |
| **Travel Mode** | Temporary city visibility | ✅ |
| **Extended Search Filters** | Height, body type, diet, income, zodiac, etc. | ✅ |

### Assessments

| Feature | Description | Status |
|---------|-------------|:------:|
| **Big Five (OCEAN)** | Personality assessment | ✅ |
| **Attachment Style** | Secure/Anxious/Avoidant/Disorganized | ✅ |
| **Values Assessment** | Political, economic, lifestyle | ✅ |
| **Dealbreaker Detection** | Critical incompatibility flags | ✅ |
| **Communication Style** | Directness & emotional tone | ✅ |

### Safety & Trust

| Feature | Description | Status |
|---------|-------------|:------:|
| **Video Verification** | Face match + liveness + deepfake detection | ✅ |
| **Reputation Scoring** | 4-dimensional (Response, Respect, Authenticity, Investment) | ✅ |
| **Trust Levels** | NEW → VERIFIED → TRUSTED → HIGHLY_TRUSTED | ✅ |
| **Behavior Tracking** | 18 event types | ✅ |
| **Public Accountability** | Community-visible feedback | ✅ |

### Communication

| Feature | Description | Status |
|---------|-------------|:------:|
| **Real-Time Chat** | WebSocket with typing indicators | ✅ |
| **Read Receipts** | Delivered & read timestamps | ✅ |
| **Message Reactions** | Emoji reactions | ✅ |
| **Video Dating** | Scheduled video calls with feedback | ✅ |
| **Calendar Integration** | Google, Apple, Outlook sync | ✅ |

---

## Deployment Strategy

This repo supports both low-floor deployment tracks:

| Strategy | Best for | Cost profile | Docs |
|---|---|---|---|
| **Edge + Serverless Postgres** (Cloudflare Workers + Neon) | Multi-app platform with bursty traffic and near-zero idle cost | Scales with requests; very low baseline | `deploy/edge/platform-worker/README.md` |
| **Single VM + Docker** (Caddy + app + MariaDB) | Always-on backend with simple Linux operations | Small fixed monthly VM bill | `deploy/vm/README.md` |

Shared entitlement schema for both tracks:

- `deploy/shared/sql/001_platform_core.sql`

Unified deployment overview:

- `deploy/README.md`

---

## Matching Reranker

This repo includes a modular, feature-flagged reciprocal reranker layer on top of existing candidate generation.

- Design + formulas: `docs/matching-reranker-design.md`
- Dashboard SQL: `docs/sql/reranker_dashboard_queries.sql`
- Backfill script: `scripts/backfill-reranker.sh`
- Migration: `src/main/resources/db/migration/V16__matching_reranker_core.sql`
- Collaborative prior migration: `src/main/resources/db/migration/V18__collaborative_priors.sql`
- Offline CF trainer: `scripts/recommender/train_cf_priors.py` (`implicit` / `lightfm`)

Media-model ports (backend-only):

- Hidden attractiveness scoring endpoint: `POST /attractiveness/score`
- Image moderation endpoint: `POST /moderation/image`
- Text moderation endpoint: `POST /moderation/text`
- Face quality endpoint: `POST /quality/face`
- Optional OSS adapters:
  - `services/media-service/scripts/silent_face_antispoof_adapter.py`
  - `services/media-service/scripts/nsfw_opennsfw2_adapter.py`
  - `services/media-service/scripts/nsfw_nudenet_adapter.py`
  - `services/media-service/scripts/nsfw_clip_adapter.py`
  - `services/media-service/scripts/text_detoxify_adapter.py`
  - `services/media-service/scripts/faceqan_adapter.py`
- Face-quality event persistence:
  - `src/main/resources/db/migration/V21__face_quality_events.sql`
  - `src/main/java/com/nonononoki/alovoa/matching/rerank/service/FaceQualityScoringService.java`

Admin endpoints:

- `GET /api/v1/admin/matching-analytics/summary`
- `GET /api/v1/admin/matching-analytics/distribution-shift`
- `POST /api/v1/admin/matching-analytics/rebuild-stats`

---

## ML Backend (Java + Python Workers)

Backend orchestration is now Java-first, with Python worker scripts for model execution/calibration.

- Java ML integration endpoints:
  - `GET /api/v1/ml/integrations/status`
  - `GET /api/v1/ml/integrations/qdrant/candidate-enrichment`
- Java runtime integration paths:
  - attractiveness sync policy gates/hints in `VisualAttractivenessSyncService`
  - Qdrant/Unleash/OpenFGA clients in `src/main/java/com/nonononoki/alovoa/service/ml/`
- Java job runner for offline ML jobs:
  - Main class: `com.nonononoki.alovoa.jobs.ml.MlJobsRunner`
  - Jobs: `train-cf-priors`, `build-scut-calibration`, `build-nsfw-calibration`,
    `build-deepfake-calibration`, `evaluate-reranker-offline`, `manage-reranker-rollout`
- Python worker/adapters remain under:
  - `services/media-service/scripts/`
  - `scripts/recommender/`

---

## Quick Start

### Prerequisites

- JDK 17+
- Maven 3.8+
- MariaDB 10.6+ or MySQL 8+
- Docker (optional)

### Option 1: Docker Compose (Recommended)

```bash
# Clone repository
git clone https://github.com/tashiscool/alovoa.git
cd alovoa

# Start Java-first backend stack (default)
docker-compose -f docker-compose.aura.yml up -d

# Optional: include legacy Python ML microservices
docker-compose -f docker-compose.aura.yml --profile python-ml up -d

# View logs
docker-compose -f docker-compose.aura.yml logs -f

# Access at http://localhost:8080
```

### Option 2: Local Development

```bash
# Clone repository
git clone https://github.com/tashiscool/alovoa.git
cd alovoa

# Start MariaDB (Docker)
docker run -d --name aura-db \
  -e MARIADB_ROOT_PASSWORD=root \
  -e MARIADB_DATABASE=alovoa \
  -e MARIADB_USER=alovoa \
  -e MARIADB_PASSWORD=alovoa \
  -p 3306:3306 mariadb:10.11

# Configure
# Edit src/main/resources/application.properties with your local settings

# Build & Run
mvn clean install -DskipTests
mvn spring-boot:run

# Access at http://localhost:8080
```

### Option 3: IDE (IntelliJ IDEA)

1. Import as Maven project
2. Configure database in `application.properties`
3. Run `AlovoaApplication.java`

---

## Configuration

### Essential Properties

```properties
# Database
spring.datasource.url=jdbc:mariadb://localhost:3306/alovoa
spring.datasource.username=alovoa
spring.datasource.password=alovoa

# Email (required for registration)
spring.mail.host=smtp.example.com
spring.mail.port=587
spring.mail.username=your_email
spring.mail.password=your_password

# Security
app.text.key=your-32-char-encryption-key-here

# S3/MinIO (media storage)
app.storage.s3.enabled=true
app.storage.s3.endpoint=http://localhost:9000
app.storage.s3.access-key=minio_access_key
app.storage.s3.secret-key=minio_secret_key
app.storage.s3.bucket.media=aura-media
app.storage.s3.bucket.video=aura-video
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | Database host | `localhost` |
| `DB_PORT` | Database port | `3306` |
| `DB_NAME` | Database name | `alovoa` |
| `DB_USER` | Database user | `alovoa` |
| `DB_PASSWORD` | Database password | - |
| `MAIL_HOST` | SMTP server | - |
| `MINIO_ENDPOINT` | MinIO S3 endpoint | `http://localhost:9000` |

---

## API Reference

### Core Endpoints

#### Profile & User

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/user/profile` | Get current user profile |
| `POST` | `/api/v1/user/profile` | Update profile |
| `GET` | `/api/v1/user/details` | Get extended profile details |
| `POST` | `/api/v1/user/details` | Update extended details |

#### Essays

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/essays` | Get all user essays |
| `GET` | `/api/v1/essays/templates` | Get all 10 essay templates |
| `POST` | `/api/v1/essays/{promptId}` | Save/update essay |
| `DELETE` | `/api/v1/essays/{promptId}` | Delete essay |

#### Relationships (Linked Profiles)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/relationship` | Get active relationship |
| `POST` | `/api/v1/relationship/request` | Send relationship request |
| `POST` | `/api/v1/relationship/{uuid}/accept` | Accept request |
| `POST` | `/api/v1/relationship/{uuid}/decline` | Decline request |
| `DELETE` | `/api/v1/relationship/{uuid}/cancel` | Cancel sent request |
| `POST` | `/api/v1/relationship/{uuid}/end` | End relationship |
| `PUT` | `/api/v1/relationship/{uuid}/type` | Update type |
| `POST` | `/api/v1/relationship/{uuid}/toggle-visibility` | Toggle public/private |

#### Matching

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/search` | Search users with filters |
| `POST` | `/api/v1/like/{uuid}` | Like a user |
| `GET` | `/api/v1/compatibility/{uuid}` | Get compatibility score |
| `GET` | `/api/v1/match-window` | Get active match windows |
| `POST` | `/api/v1/match-window/{uuid}/confirm` | Confirm match |

#### Assessments

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/assessment/personality` | Get personality profile |
| `POST` | `/api/v1/assessment/personality` | Submit assessment answers |
| `GET` | `/api/v1/assessment/political` | Get political assessment |
| `POST` | `/api/v1/assessment/political` | Submit political assessment |

---

## Database Migrations

AURA uses Flyway for version-controlled migrations:

| Version | Description |
|---------|-------------|
| V1 | Original Alovoa schema |
| V2 | AURA base extensions |
| V3 | OKCupid essay prompts, income levels |
| V4 | User relationships (linked profiles) |
| V5+ | Additional features |

### Running Migrations

```bash
# Automatically on startup (default)
# Or manually:
mvn flyway:migrate
```

---

## Testing

### Run All Tests

```bash
mvn test
```

### Run Specific Test Class

```bash
mvn test -Dtest=RelationshipServiceTest
```

### Run with Coverage

```bash
mvn verify
# Coverage report: target/site/jacoco/index.html
```

### Test Categories

| Category | Description | Count |
|----------|-------------|-------|
| Service Tests | Business logic | 20+ |
| Controller Tests | REST API endpoints | 15+ |
| Integration Tests | Full stack | 10+ |
| Repository Tests | Database queries | 5+ |

---

## CI/CD Pipeline

### GitHub Actions Workflows

| Workflow | Trigger | Description |
|----------|---------|-------------|
| `ci.yml` | Push/PR | Build, test, security scan |
| `codeql.yml` | Push/PR/Weekly | CodeQL security analysis |
| `docker-publish.yml` | Push/Tag | Build & publish Docker image |

### Pipeline Stages

```
Build → Unit Tests → Integration Tests → Security Scan → Docker Build → Release
```

---

## Project Structure

```
alovoa/
├── .github/
│   ├── workflows/           # CI/CD pipelines
│   ├── CONTRIBUTING.md      # Contribution guidelines
│   └── ISSUE_TEMPLATE/      # Issue templates
├── src/
│   ├── main/
│   │   ├── java/.../alovoa/
│   │   │   ├── config/      # Spring configurations
│   │   │   ├── entity/      # JPA entities (42 classes)
│   │   │   ├── html/        # Page controllers (35 classes)
│   │   │   ├── model/       # DTOs and models
│   │   │   ├── repo/        # Repositories
│   │   │   ├── rest/        # REST controllers (34 classes)
│   │   │   └── service/     # Business logic (44 classes)
│   │   └── resources/
│   │       ├── db/migration/  # Flyway migrations
│   │       ├── static/        # CSS, JS, images
│   │       └── templates/     # Thymeleaf templates
│   └── test/
│       └── java/.../alovoa/   # Test classes
├── docker-compose.yml         # Production compose
├── docker-compose.aura.yml    # Development compose
├── Dockerfile                 # Container build
└── pom.xml                    # Maven config
```

---

## Codebase Statistics

| Component | Count |
|-----------|------:|
| Entity Classes | 42 |
| REST Controllers | 34 |
| Service Classes | 44 |
| Page Controllers | 35 |
| API Endpoints | 237+ |
| Database Tables | 50+ |
| Test Classes | 37+ |

---

## Contributing

We welcome contributions! Please see our [Contributing Guide](.github/CONTRIBUTING.md).

### Quick Start for Contributors

1. Fork the repository
2. Create feature branch: `git checkout -b feature/amazing-feature`
3. Make changes with tests
4. Commit: `git commit -m 'feat: add amazing feature'`
5. Push: `git push origin feature/amazing-feature`
6. Open Pull Request

### Code Style

- Follow Google Java Style Guide
- Write tests for new features
- Update documentation

---

## Upstream & Credits

AURA is built on [Alovoa](https://github.com/Alovoa/alovoa), the first widespread free and open-source dating platform.

| Resource | Link |
|----------|------|
| Original Alovoa | [github.com/Alovoa/alovoa](https://github.com/Alovoa/alovoa) |
| Mobile App | [F-Droid](https://f-droid.org/en/packages/com.alovoa.expo/) • [Google Play](https://play.google.com/store/apps/details?id=com.alovoa.expo) |
| Translations | [Weblate](https://hosted.weblate.org/projects/alovoa/alovoa/) |

---

## Support

| Channel | Link |
|---------|------|
| GitHub Sponsors | [github.com/sponsors/tashiscool](https://github.com/sponsors/tashiscool) |
| Issues | [github.com/tashiscool/alovoa/issues](https://github.com/tashiscool/alovoa/issues) |
| Discussions | [github.com/tashiscool/alovoa/discussions](https://github.com/tashiscool/alovoa/discussions) |

---

## License

- **Code**: [AGPL-3.0](LICENSE)
- **Images**: Proprietary (unless stated)
- **Third-party**: See `pom.xml` and `resources/*/lib/`

---

<p align="center">
  Made with care for safer, more meaningful connections.
</p>
