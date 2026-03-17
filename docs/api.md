# API Documentation

REST API сервиса **Finance Tracker**.

## Общие правила

- Base URL: `http://localhost:8080`
- Content-Type: `application/json`
- Аутентификации нет
- Формат дат:
  - `LocalDate`: `YYYY-MM-DD`
  - `LocalDateTime`: `YYYY-MM-DDTHH:mm:ss`
- Enum значения:
  - `AccountType`: `CHECKING`, `SAVINGS`, `CREDIT`, `INVESTMENT`, `CASH`
  - `TransactionType`: `INCOME`, `EXPENSE`

## Swagger / OpenAPI

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Статусы ответов

- `GET` -> `200 OK`
- `POST` -> `201 Created`, кроме `/api/v1/account/transfer`
- `PATCH` -> `200 OK`
- `DELETE` -> `204 No Content`
- `POST /api/v1/account/transfer` -> `204 No Content`

## Общий формат ошибок

Validation error:

```json
{
  "status": 400,
  "message": "Validation failed",
  "timestamp": "2026-03-10T12:00:00",
  "errors": {
    "fieldName": "must not be blank"
  }
}
```

Business/runtime error:

```json
{
  "status": 404,
  "message": "User not found 1",
  "timestamp": "2026-03-10T12:00:00"
}
```

Дополнительные случаи `400 Bad Request`:

- `Invalid value '...' for parameter '...'` для некорректных query/path параметров
- `Invalid value for field '...'` для невалидных enum/date значений в JSON
- `Malformed JSON request` для невалидного JSON

## Важное про PATCH

Сервис обновляет данные частично, но текущие update DTO все еще валидируются как обычные request-body. Из-за этого в `PATCH` нужно передавать обязательное текстовое поле:

- users: `username`
- accounts: `name`
- budgets: `name`
- tags: `name`
- transactions: `description`

Остальные поля действительно можно передавать выборочно, если для них это указано ниже.

## Users - `/api/v1/users`

### `GET /api/v1/users`

Возвращает список пользователей.

Пример ответа:

```json
[
  {
    "id": 1,
    "username": "alex",
    "email": "alex@example.com",
    "accountIds": [1, 2],
    "budgetIds": [10, 11]
  }
]
```

### `GET /api/v1/users/{id}`

Возвращает пользователя по ID.

### `GET /api/v1/users/search/account-type/jpql`

Поиск пользователей через JPQL по `accounts.type` и диапазону `budgets.limitAmount`.

Query params:

- `accountType` - обязательный enum
- `minBudgetLimit` - обязательный нижний предел
- `maxBudgetLimit` - обязательный верхний предел

Пример:

`GET /api/v1/users/search/account-type/jpql?accountType=CHECKING&minBudgetLimit=100&maxBudgetLimit=1000`

Правила:

- пользователь попадает в результат, если у него есть хотя бы один счет указанного типа
- и хотя бы один бюджет с `limitAmount` в заданном диапазоне
- каждый пользователь возвращается один раз

### `GET /api/v1/users/search/account-type/native`

Тот же поиск, но через native SQL.

Пример:

`GET /api/v1/users/search/account-type/native?accountType=CHECKING&minBudgetLimit=100&maxBudgetLimit=1000`

### `POST /api/v1/users`

Создает пользователя.

```json
{
  "username": "alex",
  "email": "alex@example.com",
  "accountIds": [1, 2],
  "budgetIds": [10, 11]
}
```

Правила:

- `username` обязателен, длина `3..50`
- `email` опционален, но если передан, должен быть валидным email
- `accountIds` и `budgetIds` опциональны
- если часть `accountIds` или `budgetIds` не найдена -> `404`
- если счет или бюджет уже принадлежит другому пользователю -> `409`

### `PATCH /api/v1/users/{id}`

Частично обновляет пользователя.

Пример:

```json
{
  "username": "alex-updated",
  "email": "alex.new@example.com",
  "accountIds": [1],
  "budgetIds": []
}
```

Правила:

- `username` должен присутствовать и быть непустым
- `email` обновляется только если поле передано и не равно `null`
- `accountIds`/`budgetIds`:
  - `null` или отсутствие поля -> связи не меняются
  - `[]` -> коллекция заменяется на пустую
- если часть `accountIds` или `budgetIds` не найдена -> `404`
- если новый счет или бюджет уже принадлежит другому пользователю -> `409`
- текущий JPA mapping использует `orphanRemoval`, поэтому удаление счетов/бюджетов из коллекции пользователя может физически удалить эти записи из БД

### `DELETE /api/v1/users/{id}`

Удаляет пользователя.

Правила:

- связанные `accounts` и `budgets` удаляются каскадно
- транзакции удаляемых счетов тоже удаляются каскадно

## Accounts - `/api/v1/accounts`

### `GET /api/v1/accounts`

Возвращает список счетов.

Пример ответа:

```json
[
  {
    "id": 1,
    "name": "Main Card",
    "type": "CHECKING",
    "balance": 1000.00,
    "userId": 1
  }
]
```

### `GET /api/v1/accounts/{id}`

Возвращает счет по ID.

### `POST /api/v1/accounts`

Создает счет.

```json
{
  "name": "Main Card",
  "type": "CHECKING",
  "balance": 1000.00,
  "userId": 1
}
```

Правила:

- `name` обязателен, длина `3..50`
- `type` обязателен
- `balance >= 0.00`
- `userId >= 1`, пользователь должен существовать

### `PATCH /api/v1/accounts/{id}`

Частично обновляет счет.

Пример:

```json
{
  "name": "Savings Account",
  "type": "SAVINGS",
  "balance": 2500.00,
  "userId": 2
}
```

Правила:

- `name` должен присутствовать и быть непустым
- `type`, `balance`, `userId` опциональны
- `balance` задает новое абсолютное значение, а не дельту
- если передан `userId`, владелец счета может быть изменен
- смена владельца запрещена, если у счета уже есть транзакции -> `409`

### `DELETE /api/v1/accounts/{id}`

Удаляет счет.

Правила:

- все связанные транзакции удаляются каскадно

## Account Transfer - `/api/v1/account/transfer`

### `POST /api/v1/account/transfer`

Переводит сумму между двумя счетами одного пользователя.

Query params:

- `transactional` - optional, default `true`
- `failAfterDebit` - optional, default `false`

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
- `occurredAt` опционален; если не передан, используется текущее время сервера
- `note` опционален; пустое/blank значение заменяется на `Transfer`, длина `<= 255`
- создаются две обычные транзакции без отдельной сущности transfer:
  - `EXPENSE` для `fromAccountId` с описанием вида `"<note> to account {id}"`
  - `INCOME` для `toAccountId` с описанием вида `"<note> from account {id}"`
- новые транзакции создаются без тегов
- `transactional=true`: при ошибке после списания изменения откатываются
- `transactional=false`: при ошибке после списания дебет и первая транзакция сохраняются
- успешный ответ: `204 No Content`

## Budgets - `/api/v1/budgets`

### `GET /api/v1/budgets`

Возвращает бюджеты с пагинацией и сортировкой.

Query params:

- `page` - optional, default `0`
- `size` - optional, default `3`
- `sortBy` - optional, default `id`
- `ascending` - optional, default `true`

Пример:

`GET /api/v1/budgets?page=0&size=3&sortBy=id&ascending=true`

Ответом является Spring `Page<BudgetResponse>`, например:

```json
{
  "content": [
    {
      "id": 10,
      "name": "Food",
      "limitAmount": 500.00,
      "startDate": "2026-03-01",
      "endDate": "2026-03-31",
      "userId": 1
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 3,
  "number": 0
}
```

### `GET /api/v1/budgets/{id}`

Возвращает бюджет по ID.

### `POST /api/v1/budgets`

Создает бюджет.

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

- `name` обязателен, длина `3..50`
- `limitAmount > 0`
- `startDate` и `endDate` обязательны
- `startDate <= endDate`
- `userId >= 1`, пользователь должен существовать
- невалидная календарная дата в JSON -> `400`

### `PATCH /api/v1/budgets/{id}`

Частично обновляет бюджет.

Пример:

```json
{
  "name": "Food Revised",
  "limitAmount": 650.00,
  "endDate": "2026-04-05"
}
```

Правила:

- `name` должен присутствовать и быть непустым
- остальные поля опциональны
- после merge с текущими значениями диапазон дат снова валидируется
- если передан `userId`, бюджет перепривязывается к другому пользователю

### `DELETE /api/v1/budgets/{id}`

Удаляет бюджет.

## Tags - `/api/v1/tags`

### `GET /api/v1/tags`

Возвращает список тегов.

### `GET /api/v1/tags/{id}`

Возвращает тег по ID.

### `POST /api/v1/tags`

Создает тег.

```json
{
  "name": "groceries"
}
```

Правила:

- `name` обязателен, длина `1..50`
- имя нормализуется как `trim().toLowerCase(Locale.ROOT)`
- имя уникально глобально, дубликат -> `409`

### `PATCH /api/v1/tags/{id}`

Частично обновляет тег.

```json
{
  "name": "Travel"
}
```

Правила:

- `name` должен присутствовать и быть непустым
- при обновлении применяется та же нормализация и проверка уникальности

### `DELETE /api/v1/tags/{id}`

Удаляет тег.

Правила:

- удаляется сам тег и связи в `transaction_tag`
- сами `transactions` не удаляются

## Transactions - `/api/v1/transactions`

### `GET /api/v1/transactions`

Возвращает транзакции.

Query params:

- `startDate` - optional, формат `YYYY-MM-DD`
- `endDate` - optional, формат `YYYY-MM-DD`
- `withEntityGraph` - optional, default `false`

Поведение:

- если `startDate` и `endDate` не заданы -> возвращаются все транзакции
- `withEntityGraph=true` влияет только на получение всех транзакций без date-фильтра
- если задан хотя бы один из `startDate`/`endDate`, должны быть заданы оба -> `400`
- если `startDate > endDate` -> `400`
- диапазон включает весь день `endDate` до `23:59:59.999999999`

### `GET /api/v1/transactions/{id}`

Возвращает транзакцию по ID.

### `POST /api/v1/transactions`

Создает транзакцию.

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
- `description`, длина `3..255`
- `accountId` должен существовать, иначе `404`
- если часть тегов не найдена -> `404`
- `tagIds` опционален; `null` и `[]` означают создание транзакции без тегов
- баланс счета пересчитывается автоматически:
  - `INCOME` увеличивает баланс
  - `EXPENSE` уменьшает баланс
- отдельной проверки на достаточность средств нет

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

### `POST /api/v1/transactions/bulk`

Массово импортирует список транзакций.

Query params:

- `transactional` - optional, default `true`

Request:

```json
[
  {
    "occurredAt": "2026-03-08T08:00:00",
    "amount": 1200.00,
    "description": "Salary",
    "type": "INCOME",
    "accountId": 1,
    "tagIds": [1]
  },
  {
    "occurredAt": "2026-03-08T12:30:00",
    "amount": 35.00,
    "description": "Lunch",
    "type": "EXPENSE",
    "accountId": 1,
    "tagIds": [2, 3]
  }
]
```

Правила:

- request body должен содержать хотя бы один объект, пустой список -> `400`
- каждый элемент валидируется по тем же правилам, что и обычный `POST /api/v1/transactions`
- баланс счета пересчитывается после каждой транзакции в порядке элементов списка
- `transactional=true`: весь bulk-импорт атомарный, при ошибке откатываются все уже обработанные элементы
- `transactional=false`: каждый успешно обработанный элемент успевает сохраниться, даже если позже один из элементов завершится ошибкой

Пример ответа:

```json
[
  {
    "id": 201,
    "occurredAt": "2026-03-08T08:00:00",
    "amount": 1200.00,
    "description": "Salary",
    "type": "INCOME",
    "accountId": 1,
    "tagIds": [1]
  },
  {
    "id": 202,
    "occurredAt": "2026-03-08T12:30:00",
    "amount": 35.00,
    "description": "Lunch",
    "type": "EXPENSE",
    "accountId": 1,
    "tagIds": [2, 3]
  }
]
```

Демонстрация разницы в состоянии БД:

Пусть у счета `id=1` начальный баланс `1000.00`, а в таблице `transactions` для него пока нет новых записей из примера ниже.

Запрос:

```json
[
  {
    "occurredAt": "2026-03-17T09:00:00",
    "amount": 100.00,
    "description": "Groceries",
    "type": "EXPENSE",
    "accountId": 1,
    "tagIds": []
  },
  {
    "occurredAt": "2026-03-17T10:00:00",
    "amount": 50.00,
    "description": "Invalid account demo",
    "type": "EXPENSE",
    "accountId": 999,
    "tagIds": []
  }
]
```

- `POST /api/v1/transactions/bulk?transactional=true`
  - второй элемент завершится `404 Account not found: 999`
  - баланс счета `1` останется `1000.00`
  - в БД не появится ни одной новой транзакции из этого bulk-запроса
- `POST /api/v1/transactions/bulk?transactional=false`
  - второй элемент завершится той же ошибкой `404`
  - первая транзакция `"Groceries"` сохранится
  - баланс счета `1` станет `900.00`
  - в БД появится одна новая транзакция из bulk-запроса

### `PATCH /api/v1/transactions/{id}`

Частично обновляет транзакцию.

Пример:

```json
{
  "description": "Lunch with team",
  "amount": 65.00,
  "type": "EXPENSE",
  "tagIds": []
}
```

Правила:

- `description` должен присутствовать и быть непустым
- `occurredAt`, `amount`, `type`, `accountId`, `tagIds` опциональны
- `tagIds`:
  - `null` или отсутствие поля -> теги не меняются
  - `[]` -> теги очищаются
- при обновлении баланс сначала откатывается от старого значения, затем применяется новое
- если меняется `accountId`, транзакция переносится на другой счет, а баланс пересчитывается для обоих счетов

### `DELETE /api/v1/transactions/{id}`

Удаляет транзакцию.

Правила:

- при удалении баланс счета откатывается с учетом типа транзакции
- другие сущности каскадно не удаляются
