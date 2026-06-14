# Adoption Eligibility — Testing Starter

Homework project for **T1 — Unit Test Quality**.

## Domain

An animal shelter evaluates whether an adopter is eligible to adopt a specific animal.
The service applies multiple validation gates (early returns) and calculates a priority score.

## Architecture

| Layer | Class | Role |
|-------|-------|------|
| Model | `Animal`, `Adopter`, `AdoptionResult`, enums | Domain objects |
| Repository | `AdopterRepository`, `AnimalRepository` | Interfaces — mock in tests |
| Client | `NotificationClient` | Void method — verify in tests |
| Audit | `AuditLogger` | Void methods — verify in tests |
| **Service** | **`AdoptionEligibilityService`** | **Class under test** |

## Your task

Write unit tests in `AdoptionEligibilityServiceTest.java` to cover as much behavior as possible while still writing meaningful tests.

## Commands

```bash
# Run tests
mvn test
```

## Project structure

```
src/
├── main/java/lv/bootcamp/shelter/
│   ├── audit/
│   │   └── AuditLogger.java
│   ├── client/
│   │   └── NotificationClient.java
│   ├── model/
│   │   ├── Adopter.java
│   │   ├── AdoptionResult.java
│   │   ├── Animal.java
│   │   ├── AnimalStatus.java
│   │   └── AnimalType.java
│   ├── repository/
│   │   ├── AdopterRepository.java
│   │   └── AnimalRepository.java
│   └── service/
│       └── AdoptionEligibilityService.java
└── test/java/lv/bootcamp/shelter/service/
    └── AdoptionEligibilityServiceTest.java   ← your work here
```

## Prerequisites

- Java 21
- Maven 3.9+
