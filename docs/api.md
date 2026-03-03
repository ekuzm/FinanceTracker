# API Documentation

REST API сервиса **Finance Tracker**.

- Base URL: `http://localhost:8080`
- Prefix для endpoint'ов: `/api/v1`
- Формат: `application/json`
- Даты:
  - `LocalDate` -> `YYYY-MM-DD`
  - `LocalDateTime` -> `YYYY-MM-DDTHH:mm:ss` (ISO-8601)

## Статусы ответов

- `200 OK` - успешный `GET`/`PATCH`
- `201 Created` - успешный `POST`
- `204 No Content` - успешный `DELETE` и `POST /accounts/transfer`
- `400 Bad Request` - валидация/некорректные параметры
- `404 Not Found` - сущность не найдена
- `409 Conflict` - бизнес-конфликт (владение, ограничения удаления/изменения, дубликаты)
- `500 Internal Server Error` - искусственно вызванная ошибка в transfer-сценарии

## Enum значения

- `AccountType`: `CHECKING`, `SAVINGS`, `CREDIT`, `INVESTMENT`, `CASH`
- `TransactionType`: `INCOME`, `EXPENSE`

---

## Users - `/api/v1/users`

### `GET /api/v1/users`
Список пользователей.

### `GET /api/v1/users/{id}`
Пользователь по ID.

### `POST /api/v1/users`
Создать пользователя.

Request:

```json
{
  "username": "alex",
  "email": "alex@example.com",
  "accountIds": [1, 2],
  "transactionIds": [10, 11]
}
```

Notes:

- `username` обязателен (`3..50`)
- `email` валидируется как email
- `accountIds`/`transactionIds` опциональны
- если переданные `accountIds`/`transactionIds` не существуют -> `404`
- если счет/транзакция уже принадлежит другому пользователю -> `409`

### `PATCH /api/v1/users/{id}`
Частичное обновление пользователя.

### `DELETE /api/v1/users/{id}`
Удалить пользователя.

---

## Accounts - `/api/v1/accounts`

### `GET /api/v1/accounts`
Список счетов.

### `GET /api/v1/accounts/{id}`
Счет по ID.

### `POST /api/v1/accounts`
Создать счет.

Request:

```json
{
  "name": "Main Card",
  "type": "CHECKING",
  "balance": 1000.00,
  "userId": 1
}
```

Notes:

- `name` обязателен (`3..50`)
- `type` обязателен (см. `AccountType`)
- `balance >= 0.00`
- `userId >= 1`

### `PATCH /api/v1/accounts/{id}`
Частичное обновление счета.

Ограничение: смена владельца запрещена, если у счета уже есть транзакции (`409`).

### `DELETE /api/v1/accounts/{id}`
Удалить счет.

Ограничение: нельзя удалить счет, если у него есть транзакции (`409`).

### `POST /api/v1/accounts/transfer`
Перевод между счетами одного пользователя.

Query params:

- `transactional` (optional, default `true`)
- `failAfterDebit` (optional, default `false`)

Request:

```json
{
  "userId": 1,
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 150.00,
  "occurredAt": "2026-03-03T10:30:00",
  "description": "Transfer to savings"
}
```

Поведение:

- `fromAccountId` и `toAccountId` должны отличаться (`400`)
- оба счета должны принадлежать `userId` (`409`)
- на счете-источнике должны быть достаточные средства (`409`)
- создается пара транзакций с одним `transferId`:
  - `EXPENSE` на source account
  - `INCOME` на target account
- если `transactional=false` и `failAfterDebit=true`, возможен частичный эффект (дебет пройдет, кредит нет)

---

## Budgets - `/api/v1/budgets`

### `GET /api/v1/budgets`
Список бюджетов.

### `GET /api/v1/budgets/{id}`
Бюджет по ID.

### `POST /api/v1/budgets`
Создать бюджет.

Request:

```json
{
  "name": "Food",
  "limitAmount": 500.00,
  "startDate": "2026-03-01",
  "endDate": "2026-03-31",
  "userId": 1
}
```

Notes:

- `name` обязателен (`3..50`)
- `limitAmount > 0`
- `startDate` и `endDate` обязательны
- `startDate <= endDate` (иначе `400`)

### `PATCH /api/v1/budgets/{id}`
Частичное обновление бюджета.

Ограничения:

- нельзя сменить владельца, если у бюджета есть транзакции (`409`)
- нельзя изменить период так, чтобы существующие транзакции вышли за новый диапазон (`409`)

### `DELETE /api/v1/budgets/{id}`
Удалить бюджет.

Ограничение: нельзя удалить бюджет, если у него есть транзакции (`409`).

---

## Tags - `/api/v1/tags`

### `GET /api/v1/tags`
Список тегов.

### `GET /api/v1/tags/{id}`
Тег по ID.

### `POST /api/v1/tags`
Создать тег.

Request:

```json
{
  "name": "groceries",
  "userId": 1
}
```

Notes:

- `name` обязателен (`1..50`)
- `userId >= 1`
- уникальность имени проверяется в нормализованном виде (trim + lower) в рамках пользователя

### `PATCH /api/v1/tags/{id}`
Частичное обновление тега.

Ограничения:

- смена владельца запрещена (`409`)
- дубликат имени (с учетом нормализации) для пользователя -> `409`

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

- если `startDate` и `endDate` не заданы -> возвращаются все транзакции по флагам
- если задан хотя бы один из `startDate`/`endDate`, должны быть заданы оба (`400`)
- при фильтре по диапазону учитывается `includeTransfers`
- при фильтре по диапазону `withEntityGraph` не используется

### `GET /api/v1/transactions/{id}`
Транзакция по ID.

### `POST /api/v1/transactions`
Создать обычную транзакцию (не transfer-пару).

Request:

```json
{
  "occurredAt": "2026-03-03T12:00:00",
  "amount": 50.00,
  "description": "Lunch",
  "type": "EXPENSE",
  "userId": 1,
  "accountId": 1,
  "budgetId": 1,
  "tagIds": [1, 2]
}
```

Notes:

- `amount >= 0.01`
- `description` обязателен (`3..255`)
- `userId`, `accountId`, `budgetId` обязательны и `> 0`
- `account` и `budget` должны принадлежать `userId`, иначе `409`
- дата транзакции должна попадать в период бюджета, иначе `409`
- если часть тегов не найдена -> `404`
- если теги не принадлежат пользователю -> `409`
- баланс счета пересчитывается автоматически:
  - `INCOME` увеличивает баланс
  - `EXPENSE` уменьшает баланс

### `PATCH /api/v1/transactions/{id}`
Частичное обновление транзакции.

Баланс сначала откатывается по старому значению транзакции, затем применяется заново по новым данным.

### `DELETE /api/v1/transactions/{id}`
Удалить транзакцию.

Баланс счета откатывается на сумму удаляемой транзакции.
