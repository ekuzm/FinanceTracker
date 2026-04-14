# Finance Tracker

**Finance Tracker** — REST API для учета личных финансов: пользователи, счета, бюджеты, теги, транзакции и переводы между счетами.

**Стек:** Java 21, Spring Boot 4.0.3, Spring Web MVC, Spring Data JPA, PostgreSQL, Liquibase, springdoc-openapi.

## Frontend

В проект добавлен отдельный SPA-фронтенд на `Vue 3 + Vite + TypeScript` в директории [frontend](frontend).

Что умеет интерфейс:

- overview-дашборд с net worth, income/expense, savings rate и оперативными карточками
- управление `users`, `accounts`, `budgets`, `tags`, `transactions`
- фильтрация транзакций по диапазону дат и `withEntityGraph`
- переводы между счетами с флагами `transactional` и `failAfterDebit`
- bulk-импорт транзакций и async-импорт с мониторингом задач
- бюджетная аналитика на основе расходов пользователя в периоде бюджета
- поиск пользователей через JPQL/native endpoint'ы
- запуск race-condition demo и просмотр результата
- экспорт текущего workspace snapshot в JSON

### Запуск Frontend

1. Убедитесь, что установлен Node.js 20+.
2. Запустите backend на `http://localhost:8080`.
3. В отдельном терминале перейдите в `frontend`:

```bash
cd frontend
npm install
npm run dev
```

4. Откройте приложение по адресу `http://localhost:5173`.

По умолчанию dev-server использует Vite proxy на `http://localhost:8080`, поэтому поле `Backend base URL` можно оставить пустым.

Если backend доступен по другому адресу, введите его в UI, например `http://localhost:8080`.

## Что умеет сервис

- CRUD для `users`, `accounts`, `budgets`, `tags`, `transactions`
- перевод денег между счетами одного пользователя через `/api/v1/account/transfer`
- bulk-импорт транзакций через `/api/v1/transactions/bulk` с демонстрацией поведения с `@Transactional` и без него
- асинхронный импорт транзакций через `/api/v1/transactions/async` с возвратом `taskId` и проверкой статуса
- демонстрация race condition через `/api/v1/demo/race-condition/run`
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

## Async Операция И Race Condition

Для лабораторной работы отдельно вынесены:

- `POST /api/v1/transactions/async` -> запускает асинхронное создание списка транзакций и сразу возвращает `taskId`
- `GET /api/v1/transactions/async/status/{taskId}` -> возвращает текущий статус задачи
- `GET /api/v1/transactions/async/tasks` -> возвращает все асинхронные задачи в памяти
- `GET /api/v1/demo/race-condition/run` -> запускает демонстрацию race condition, `synchronized` и `AtomicInteger`

Результаты race-condition сценариев пишутся в application logs.

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
- Frontend: `http://localhost:5173`

`docker-compose.yaml` теперь поднимает отдельный сервис `finance-tracker-frontend`, который:

- собирает Vue-приложение
- раздаёт его через `nginx`
- проксирует `/api`, `/v3` и `/swagger-ui` в контейнер `finance-tracker`

Если хотите сменить порт фронта, добавьте в `.env`:

```env
FRONTEND_PORT=5173
```

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
