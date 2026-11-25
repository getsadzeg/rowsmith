# Lending demo

Spring Boot 4 / Java 21 service for installment loans sold through merchant stores.

- Schema: Flyway migrations in `src/main/resources/db/migration` (PostgreSQL).
- Code: JPA entities and repositories in `com.example.lending.domain`; business logic in
  `com.example.lending.service` (NamedParameterJdbcTemplate).

## Tests

- Integration tests run against a real PostgreSQL started by Testcontainers (Docker is running).
- Pattern: `@SpringBootTest`, `@Import(TestcontainersConfiguration.class)`, `@Transactional` (rolled back after each test).
- Run one class: `./gradlew test --tests 'com.example.lending.service.SomeTest'`
- Run everything: `./gradlew test`
