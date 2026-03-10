# API Documentation

REST API сервиса **Finance Tracker**.

- Base URL: `http://localhost:8080`
- Content-Type: `application/json`
- Формат дат:
  - `LocalDate`: `YYYY-MM-DD`
  - `LocalDateTime`: `YYYY-MM-DDTHH:mm:ss`

## Users - `/api/v1/users`

### `GET /api/v1/users`

Получить список пользователей.

### `GET /api/v1/users/search/account-type/jpql`

Найти пользователей через JPQL по вложенным сущностям `accounts` и `budgets`.

Query params:

- `accountType` - обязательный enum: `CHECKING`, `SAVINGS`, `CREDIT`, `INVESTMENT`, `CASH`
- `minBudgetLimit` - обязательный lower bound для `budget.limitAmount`
- `maxBudgetLimit` - обязательный upper bound для `budget.limitAmount`

Пример:

`GET /api/v1/users/search/account-type/jpql?accountType=CHECKING&minBudgetLimit=100&maxBudgetLimit=1000`

Правила:

- пользователь попадает в результат, если у него есть хотя бы один `account` с указанным `type`
- и хотя бы один `budget`, у которого `limitAmount` попадает в диапазон
- один пользователь возвращается один раз, даже если у него несколько счетов одного и того же типа
- ответ возвращается обычным списком пользователей

### `GET /api/v1/users/search/account-type/native`

Тот же поиск, но через native SQL query.

Пример:

`GET /api/v1/users/search/account-type/native?accountType=CHECKING&minBudgetLimit=100&maxBudgetLimit=1000`

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

Правила:

- связанные `accounts` и `budgets` удаляются каскадно
- транзакции удаляемых счетов тоже удаляются каскадно

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

- все связанные транзакции удаляются каскадно

---

## Account Transfer - `/api/v1/account/transfer`

### `POST /api/v1/account/transfer`

Перевести сумму между двумя счетами одного пользователя.

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
  "note": "Card to savings"
}
```

Правила:

- `fromAccountId`, `toAccountId`, `amount` обязательны
- `fromAccountId` и `toAccountId` должны быть разными -> `400`
- оба счета должны принадлежать одному пользователю -> `409`
- `amount >= 0.01`
- на исходном счете должно хватать средств -> `409`
- создаются две обычные транзакции без `transfer_id`:
  - `EXPENSE` для `fromAccountId`
  - `INCOME` для `toAccountId`
- `transactional=true`: при ошибке после списания изменения откатываются
- `transactional=false`: при ошибке после списания сохранится дебет и первая транзакция
- успешное выполнение возвращает `204 No Content` без тела ответа

---

## Budgets - `/api/v1/budgets`

### `GET /api/v1/budgets`

Получить список бюджетов с пагинацией.

Query params:

- `page` - опциональный, default `0`
- `size` - опциональный, default `3`
- `sortBy` - опциональный, default `id`
- `ascending` - опциональный, default `true`

Пример:

`GET /api/v1/budgets?page=0&size=3&sortBy=id&ascending=true`

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
- дата должна быть валидной календарной датой (`2026-09-31` -> `400`)

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

Правила:

- удаляется только сам тег и его связи в `transaction_tag`
- сами `transactions` не удаляются

---

## Transactions - `/api/v1/transactions`

### `GET /api/v1/transactions`

Получить транзакции.

Query params:

- `startDate` (`YYYY-MM-DD`, optional)
- `endDate` (`YYYY-MM-DD`, optional)
- `withEntityGraph` (optional, default `false`)

Поведение:

- если `startDate` и `endDate` не заданы -> возвращаются все транзакции
- если задан хотя бы один из `startDate/endDate`, должны быть заданы оба -> `400`
- если `startDate > endDate` -> `400`

### `GET /api/v1/transactions/{id}`

Получить транзакцию по ID.

### `POST /api/v1/transactions`

Создать транзакцию.

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
  "tagIds": [1, 2]
}
```

### `PATCH /api/v1/transactions/{id}`

Частичное обновление транзакции.

Правила:

- при обновлении баланс сначала откатывается от старого значения, затем применяется новое

### `DELETE /api/v1/transactions/{id}`

Удалить транзакцию.

Правила:

- при удалении баланс счета откатывается с учетом типа транзакции
- другие сущности каскадно не удаляются

---
