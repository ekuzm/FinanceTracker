# Finance Tracker

**Finance Tracker** — REST API для учета личных финансов: пользователи, счета, бюджеты, теги, транзакции и переводы между счетами.

**Стек:** Java 21, Spring Boot 4.0.3, Spring Web MVC, Spring Data JPA, PostgreSQL, Liquibase, springdoc-openapi.

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

Остановка:

```bash
docker compose down
```

## Конфигурация и логи

- лог-файл приложения: `logs/application.log`
- лог-файл ошибок: `logs/error.log`
- в `docker-compose.yaml` логи пробрасываются в локальную директорию `./logs`

## JMeter

В проекте теперь лежат три JMeter-плана:

- `docs/jmeter/all-endpoints-no-race.jmx` - полный прогон CRUD, search, transfer и async endpoint-ов без `/api/v1/demo/race-condition/run`
- `docs/jmeter/race-condition-only.jmx` - отдельная многопоточная нагрузка только на `/api/v1/demo/race-condition/run`
- `docs/jmeter/transactions-async-load-test.jmx` - точечный async-нагрузочный сценарий для `POST /api/v1/transactions/async`

Что важно перед запуском:

- приложение должно быть поднято на `localhost:8080`, если не переопределяете `host`, `port`, `protocol`
- для `all-endpoints-no-race.jmx` предварительные данные не нужны: план сам создаёт `user`, `accounts`, `budget`, `tag`, `transaction`, прогоняет сценарии и удаляет созданные сущности в конце
- для `transactions-async-load-test.jmx` по-прежнему нужен существующий `accountId`
- результаты удобно складывать в `docs/jmeter/results/`

Основные properties:

- для всех планов: `host`, `port`, `protocol`, `basePath`
- для `all-endpoints-no-race.jmx`: при необходимости можно переопределить даты через `budgetStartDate`, `budgetEndDate`, `budgetUpdateEndDate`, `txDateStart`, `txDateEnd`, `txOccurredAt`, `txUpdatedAt`, `transferOccurredAt`, `asyncOccurredAt`, а также паузу перед cleanup через `asyncWaitMillis`
- для `race-condition-only.jmx`: `raceUsers`, `raceRampUpSeconds`, `raceLoops`
- для `transactions-async-load-test.jmx`: `accountId`, `transactional`

Примеры запуска:

Для реального нагрузочного прогона используйте non-GUI режим JMeter через `-n`.
Команда `jmeter -t docs/jmeter/race-condition-only.jmx` только открывает план в GUI и обычно работает заметно хуже под нагрузкой.

```bash
jmeter -n \
  -t docs/jmeter/all-endpoints-no-race.jmx \
  -l docs/jmeter/results/all-endpoints-no-race.jtl
```

```bash
jmeter -n \
  -t docs/jmeter/race-condition-only.jmx \
  -l docs/jmeter/results/race-condition-only.jtl \
  -JraceUsers=10 \
  -JraceLoops=5
```

```bash
jmeter -n \
  -t docs/jmeter/transactions-async-load-test.jmx \
  -Jhost=localhost \
  -Jport=8080 \
  -JaccountId=1 \
  -l docs/jmeter/results/transactions-async-load-test.jtl
```

Для `race-condition-only.jmx` выбран умеренный дефолтный профиль нагрузки, потому что каждый запрос к `/api/v1/demo/race-condition/run` внутри приложения сам поднимает несколько потоков и пишет результат в application logs.

## SonarQube Cloud

[Sonar Analysis](https://sonarcloud.io/summary/new_code?id=ekuzm_FinanceTracker)

Для передачи покрытия тестов в SonarQube Cloud проект настроен на генерацию JaCoCo XML-отчета в стандартный путь `target/site/jacoco/jacoco.xml`.

GitHub Actions workflow находится в `.github/workflows/ci.yml` и выполняет:

- сборку
- линтинг через `checkstyle`
- unit-тесты
- генерацию JaCoCo coverage report
- отправку анализа в SonarQube Cloud

Для работы workflow в GitHub repository settings нужно задать:

- secret `SONAR_TOKEN`
- variable `SONAR_ORGANIZATION`, если organization key в SonarQube Cloud отличается от owner репозитория
- variable `SONAR_PROJECT_KEY`, если нужно переопределить текущее значение по умолчанию `ekuzm_FinanceTracker`

Локальный запуск с покрытием:

```bash
./mvnw -Pcoverage verify
```

Отправка анализа вместе с покрытием в SonarQube Cloud:

```bash
./mvnw -Pcoverage verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
  -Dsonar.token=$SONAR_TOKEN
```
