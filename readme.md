# Firmament Immigration Backend

Backend service for immigration consultation management system.

## Tech Stack
- Java 17
- Spring Boot 3.2.0
- PostgreSQL
- Redis
- JWT Authentication
- Stripe Payment Integration

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.6+
- PostgreSQL 14+
- Redis (optional, for caching)

### Running Locally
```bash
# Clone the repository
git clone [your-repo-url]

# Navigate to project
cd immigration-backend

# Run with H2 (development)
mvn spring-boot:run

# Run with PostgreSQL
mvn spring-boot:run -Dspring.profiles.active=dev