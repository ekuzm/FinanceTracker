# Finance Tracker

**Finance Tracker** — REST API для управления личными финансами: пользователи, счета, бюджеты, теги, транзакции и переводы между счетами.

**Стек:** Java 21 · Spring Boot 4 · Spring Data JPA · PostgreSQL · Liquibase

---

## Связи таблиц

<img width="758" height="603" alt=" ER-diagram" src="https://github.com/user-attachments/assets/aac5c160-384e-4b68-84e5-a70feaea4e2b" />

---

## API

Полная документация: [/docs/api.md](docs/api.md)

Основные группы endpoint'ов:

- `/api/v1/users`
- `/api/v1/accounts`
- `/api/v1/budgets`
- `/api/v1/tags`
- `/api/v1/transactions`
- `/api/v1/transfers`

## Запуск приложения

### 1. Подготовьте `.env`

Пример актуальных значений:

```env
POSTGRES_USER=finance-tracker-user
POSTGRES_PASSWORD=finance-tracker-password
POSTGRES_DB=finance-tracker
POSTGRES_PORT=5432
POSTGRES_HOST=pg

FINANCE_TRACKER_PORT=8080
```

### 2. Поднимите стек

```bash
docker compose up -d --build
```

Остановка:

```bash
docker compose down
```

---

## SonarQube Cloud

[Sonar Analysis](https://sonarcloud.io/summary/new_code?id=ekuzm_FinanceTracker)
