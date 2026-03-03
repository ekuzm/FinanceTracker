# Finance Tracker

**Finance Tracker** — REST API для управления личными финансами: пользователи, счета, бюджеты, теги, транзакции и переводы между счетами.

**Стек:** Java 21 · Spring Boot 4 · Spring Data JPA · PostgreSQL · Liquibase

---

## Актуальные сущности

- `User`:
  - базовая сущность владельца данных;
  - связи `1:N` с `Account`, `Budget`, `Transaction`, `Tag`.
- `Account`:
  - поля: `name`, `type`, `balance`, `user`;
  - используется для обычных транзакций и transfer-операций.
- `Budget`:
  - поля: `name`, `limitAmount`, `startDate`, `endDate`, `user`;
  - поле `spent` не хранится в БД и вычисляется из транзакций.
- `Tag`:
  - заменяет старую `Category`;
  - принадлежит пользователю (`user`);
  - связь с `Transaction` — `Many-to-Many`;
  - уникальность имени обеспечивается через `normalizedName` в рамках пользователя.
- `Transaction`:
  - поля: `occurredAt` (`LocalDateTime`), `amount` (`BigDecimal`), `description`, `type` (`INCOME`/`EXPENSE`);
  - обязательные связи: `user`, `account`;
  - `budget` обязателен для обычных операций и `null` для transfer;
  - `transferId` (`UUID`) связывает пару транзакций перевода;
  - теги (`tagIds`) доступны только для обычных операций.

---

## API

Полная документация: [docs/api.md](docs/api.md)

Основные группы endpoint'ов:
- `/api/v1/users`
- `/api/v1/accounts`
- `/api/v1/budgets`
- `/api/v1/tags`
- `/api/v1/transactions`

Перевод между счетами:
- `POST /api/v1/accounts/transfer?transactional=true|false&failAfterDebit=true|false`

Особенности списка транзакций:
- по умолчанию `GET /api/v1/transactions` скрывает transfer-записи;
- для включения переводов используйте `includeTransfers=true`;
- для демонстрации подгрузки связей доступен `withEntityGraph=true`.

---

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

## Проверка API через Makefile

В проекте есть smoke/happy-path проверка API через `curl` + `jq`:

```bash
make api-check
```

Доступные команды:

```bash
make help
make api-up
make api-wait
make api-check
make api-check-keep
make api-down
```

---

## SonarQube Cloud

[Sonar Analysis](https://sonarcloud.io/summary/new_code?id=ekuzm_FinanceTracker)
