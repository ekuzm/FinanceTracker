# API Documentation

REST API сервиса **Finance Tracker**.

- Base URL: `http://localhost:8080`
- Prefix: `/api/v1`
- Content-Type: `application/json`
- Формат дат:
  - `LocalDate`: `YYYY-MM-DD`
  - `LocalDateTime`: `YYYY-MM-DDTHH:mm:ss`

## Коды ответов

- `200 OK` - успешный `GET`/`PATCH`
- `201 Created` - успешный `POST`
- `204 No Content` - успешный `DELETE`
- `400 Bad Request` - валидация, некорректные параметры
- `404 Not Found` - сущность не найдена
- `409 Conflict` - бизнес-конфликт
- `500 Internal Server Error` - ожидаем только в тестовом сценарии `transfer` (`transactional=false&failAfterDebit=true`)

## Enum

- `AccountType`: `CHECKING`, `SAVINGS`, `CREDIT`, `INVESTMENT`, `CASH`
- `TransactionType`: `INCOME`, `EXPENSE`

## Связи

- `User -> Account` (`OneToMany`)
- `User -> Budget` (`OneToMany`)
- `Account -> Transaction` (`OneToMany`)
- `Transaction <-> Tag` (`ManyToMany`)
- `Transfer -> Transaction` (`OneToMany`, пара `EXPENSE` + `INCOME`)
- `Transfer -> Account` напрямую не хранится: source/target определяются через `Transaction.accountId` (`EXPENSE`/`INCOME`)

---

## Users - `/api/v1/users`

### `GET /api/v1/users`
Получить список пользователей.

### `GET /api/v1/users/{id}`
Получить пользователя по ID.

### `POST /api/v1/users`
Создать пользователя.

```json
{
  "username": "alex",
  "email": "alex@example.com",
  "accountIds": [1, 2],
  "budgetIds": [10, 11]
}
```

Правила:

- `username` обязателен (`3..50`)
- `email` опционален, но если задан - должен быть валиден
- `accountIds`/`budgetIds` опциональны
- если часть `accountIds`/`budgetIds` не найдена -> `404`
- если счет/бюджет уже принадлежит другому пользователю -> `409`

### `PATCH /api/v1/users/{id}`
Частичное обновление пользователя.

Notes:

- `null` в `accountIds`/`budgetIds` -> оставить связи без изменений
- пустой массив `[]` в `accountIds`/`budgetIds` -> очистить связи

### `DELETE /api/v1/users/{id}`
Удалить пользователя.

---

## Accounts - `/api/v1/accounts`

### `GET /api/v1/accounts`
Получить список счетов.

### `GET /api/v1/accounts/{id}`
Получить счет по ID.

### `POST /api/v1/accounts`
Создать счет.

```json
{
  "name": "Main Card",
  "type": "CHECKING",
  "balance": 1000.00,
  "userId": 1
}
```

Правила:

- `name` обязателен (`3..50`)
- `type` обязателен
- `balance >= 0.00`
- `userId >= 1`, пользователь должен существовать

### `PATCH /api/v1/accounts/{id}`
Частичное обновление счета.

Правила:

- смена владельца запрещена, если у счета уже есть транзакции -> `409`

### `DELETE /api/v1/accounts/{id}`
Удалить счет.

Правила:

- нельзя удалить счет, если у него есть транзакции -> `409`

---

## Budgets - `/api/v1/budgets`

### `GET /api/v1/budgets`
Получить список бюджетов.

### `GET /api/v1/budgets/{id}`
Получить бюджет по ID.

### `POST /api/v1/budgets`
Создать бюджет.

```json
{
  "name": "Food",
  "limitAmount": 500.00,
  "startDate": "2026-03-01",
  "endDate": "2026-03-31",
  "userId": 1
}
```

Правила:

- `name` обязателен (`3..50`)
- `limitAmount > 0`
- `startDate` и `endDate` обязательны
- `startDate <= endDate`
- `userId >= 1`, пользователь должен существовать

### `PATCH /api/v1/budgets/{id}`
Частичное обновление бюджета.

Правила:

- после merge с текущими данными диапазон дат снова валидируется (`startDate <= endDate`)

### `DELETE /api/v1/budgets/{id}`
Удалить бюджет.

---

## Tags - `/api/v1/tags`

### `GET /api/v1/tags`
Получить список тегов.

### `GET /api/v1/tags/{id}`
Получить тег по ID.

### `POST /api/v1/tags`
Создать тег.

```json
{
  "name": "groceries"
}
```

Правила:

- `name` обязателен (`1..50`)
- имя нормализуется: `trim + lower`
- имя уникально глобально, дубликат -> `409`

### `PATCH /api/v1/tags/{id}`
Частичное обновление тега.

Правила:

- при обновлении имени применяется та же нормализация и проверка уникальности

### `DELETE /api/v1/tags/{id}`
Удалить тег.

---

## Transactions - `/api/v1/transactions`

### `GET /api/v1/transactions`
Получить транзакции.

Query params:

- `startDate` (`YYYY-MM-DD`, optional)
- `endDate` (`YYYY-MM-DD`, optional)
- `withEntityGraph` (optional, default `false`)
- `includeTransfers` (optional, default `false`)

Поведение:

- если `startDate` и `endDate` не заданы -> возвращаются все транзакции по `withEntityGraph/includeTransfers`
- если задан хотя бы один из `startDate/endDate`, должны быть заданы оба -> `400`
- если `startDate > endDate` -> `400`

### `GET /api/v1/transactions/{id}`
Получить транзакцию по ID.

### `POST /api/v1/transactions`
Создать обычную транзакцию (без `Transfer`).

```json
{
  "occurredAt": "2026-03-08T12:00:00",
  "amount": 50.00,
  "description": "Lunch",
  "type": "EXPENSE",
  "accountId": 1,
  "tagIds": [1, 2]
}
```

Правила:

- `occurredAt`, `amount`, `description`, `type`, `accountId` обязательны
- `amount >= 0.01`
- `description` (`3..255`)
- если часть тегов не найдена -> `404`
- баланс счета пересчитывается автоматически:
  - `INCOME` увеличивает баланс
  - `EXPENSE` уменьшает баланс

Пример ответа:

```json
{
  "id": 101,
  "occurredAt": "2026-03-08T12:00:00",
  "amount": 50.00,
  "description": "Lunch",
  "type": "EXPENSE",
  "accountId": 1,
  "accountName": "Main Card",
  "tagIds": [1, 2],
  "transferId": null
}
```

### `PATCH /api/v1/transactions/{id}`
Частичное обновление транзакции.

Правила:

- transfer-транзакции нельзя обновлять напрямую -> `409`
- при обновлении баланс сначала откатывается от старого значения, затем применяется новое

### `DELETE /api/v1/transactions/{id}`
Удалить транзакцию.

Правила:

- transfer-транзакции нельзя удалять напрямую -> `409`

---

## Transfers - `/api/v1/transfers`

### `GET /api/v1/transfers`
Получить список переводов.

### `GET /api/v1/transfers/{id}`
Получить перевод по UUID.

### `POST /api/v1/transfers`
Создать перевод.

Query params:

- `transactional` (optional, default `true`)
- `failAfterDebit` (optional, default `false`)

Request:

```json
{
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 150.00,
  "occurredAt": "2026-03-08T10:30:00",
  "note": "Transfer to savings"
}
```

Правила:

- `fromAccountId`, `toAccountId`, `amount` обязательны
- `fromAccountId != toAccountId` -> иначе `400`
- оба счета должны принадлежать одному пользователю -> иначе `409`
- на счете-источнике должны быть достаточные средства -> иначе `409`
- `amount > 0`
- `note` опционален, при пустом значении используется `"Transfer"`
- создается `Transfer` и 2 транзакции: `EXPENSE` + `INCOME`
- `fromAccountId`/`toAccountId` в `TransferResponse` вычисляются из этих транзакций (через их `accountId`)

Пример ответа:

```json
{
  "id": "4bd91d20-91f3-4eb7-a219-487db4063dbf",
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 150.00,
  "occurredAt": "2026-03-08T10:30:00",
  "note": "Transfer to savings"
}
```

Отдельный тестовый сценарий:

- `transactional=false&failAfterDebit=true` намеренно выбрасывает `500` после дебета, чтобы показать поведение без транзакции.

### `PATCH /api/v1/transfers/{id}`
Частичное обновление перевода.

Правила:

- можно обновлять счета/сумму/дату/заметку
- при обновлении сначала откатывается старое влияние перевода на балансы, затем применяется новое

### `DELETE /api/v1/transfers/{id}`
Удалить перевод.

Правила:

- при удалении влияние перевода на балансы откатывается

---

## Postman

Актуальная коллекция: `finance-tracker.postman_collection.json`.

Рекомендуемый порядок smoke-прогона:

1. `users -> create user`
2. `accounts -> create source account`
3. `accounts -> create target account`
4. `tags -> create tag`
5. `transactions -> create transaction`
6. `transfers -> create transfer (transactional)`

В коллекции после `POST` запросов ID автоматически сохраняются в collection variables.
