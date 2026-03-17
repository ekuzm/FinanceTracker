# Finance Tracker

**Finance Tracker** — REST API для учета личных финансов: пользователи, счета, бюджеты, теги, транзакции и переводы между счетами.

**Стек:** Java 21, Spring Boot 4.0.3, Spring Web MVC, Spring Data JPA, PostgreSQL, Liquibase, springdoc-openapi.

## Что умеет сервис

- CRUD для `users`, `accounts`, `budgets`, `tags`, `transactions`
- перевод денег между счетами одного пользователя через `/api/v1/account/transfer`
- bulk-импорт транзакций через `/api/v1/transactions/bulk` с демонстрацией поведения с `@Transactional` и без него
- поиск пользователей по типу счета и диапазону бюджета через JPQL и native SQL
- фильтрация транзакций по диапазону дат
- пагинация и сортировка бюджетов
- Swagger UI и OpenAPI JSON
- единый формат ошибок, аспектное логирование сервисов и простой in-memory cache для чтения пользователей и поисковых запросов

## Связи таблиц

<img width="618" height="744" alt="image" src="https://github.com/user-attachments/assets/ae27d4f3-41e1-41b0-9cea-f96f756a61c5" />

## API

Полная документация: [docs/api.md](docs/api.md)

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Base URL: `http://localhost:8080`

Основные группы endpoint'ов:

- `/api/v1/users`
- `/api/v1/accounts`
- `/api/v1/account/transfer`
- `/api/v1/budgets`
- `/api/v1/tags`
- `/api/v1/transactions`

## Bulk Импорт И Транзакционность

Для лабораторной работы добавлен endpoint `POST /api/v1/transactions/bulk?transactional=true|false`, который принимает JSON-массив `TransactionRequest` и массово импортирует транзакции.

Сценарий демонстрации:

- если первый элемент списка валиден, а второй ссылается на несуществующий `accountId`, то при `transactional=true` весь bulk откатывается
- тот же запрос при `transactional=false` сохранит первый элемент и изменит баланс счета только по нему
- подробный пример входных данных и ожидаемого состояния БД описан в [docs/api.md](docs/api.md)

## Формат ошибок

Все ошибки API возвращаются в одном из двух JSON-форматов.

Validation error:

```json
{
  "status": 400,
  "message": "Validation failed",
  "timestamp": "2026-03-10T12:00:00",
  "errors": {
    "username": "must not be blank"
  }
}
```

Обычная бизнес-ошибка:

```json
{
  "status": 404,
  "message": "User not found 1",
  "timestamp": "2026-03-10T12:00:00"
}
```

Дополнительно сервис возвращает:

- `Invalid value '...' for parameter '...'` для некорректных query/path параметров
- `Invalid value for field '...'` для невалидных enum/date значений в JSON
- `Malformed JSON request` для поврежденного JSON

## Запуск

### Docker Compose

1. Подготовьте `.env`:

```env
POSTGRES_USER=finance-tracker-user
POSTGRES_PASSWORD=finance-tracker-password
POSTGRES_DB=finance-tracker
POSTGRES_PORT=5432
POSTGRES_HOST=pg

FINANCE_TRACKER_PORT=8080
```

2. Поднимите стек:

```bash
docker compose up -d --build
```

3. Откройте:

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

Остановка:

```bash
docker compose down
```

## Конфигурация и логи

- лог-файл приложения: `logs/application.log`
- лог-файл ошибок: `logs/error.log`
- в `docker-compose.yaml` логи пробрасываются в локальную директорию `./logs`

## SonarQube Cloud

[Sonar Analysis](https://sonarcloud.io/summary/new_code?id=ekuzm_FinanceTracker)
