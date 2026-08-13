# LinkTracker

Telegram-бот для отслеживания обновлений по ссылкам на **GitHub** и **StackOverflow**.
Пользователь подписывается на ссылку — сервис периодически опрашивает внешние API и присылает
уведомление, когда появилось что-то новое (новый PR, Issue, ответ или комментарий).

Проект построен как два независимых микросервиса, взаимодействующих по контракту (REST + Kafka),
с полным набором «продовых» практик: миграции, кэш, паттерны устойчивости, метрики и контейнеризация.

---

## Содержание

- [Архитектура](#архитектура)
- [Технологический стек](#технологический-стек)
- [Возможности](#возможности)
- [Команды бота](#команды-бота)
- [Быстрый старт](#быстрый-старт)
- [Конфигурация](#конфигурация)
- [API](#api)
- [База данных и миграции](#база-данных-и-миграции)
- [Асинхронное взаимодействие и кэш](#асинхронное-взаимодействие-и-кэш)
- [Устойчивость](#устойчивость)
- [Наблюдаемость](#наблюдаемость)
- [Тестирование](#тестирование)
- [CI/CD](#cicd)
- [Структура проекта](#структура-проекта)
- [Архитектурные решения](#архитектурные-решения)

---

## Архитектура

```
                  ┌──────────────┐
   пользователь ──►│   Telegram   │
                  └──────┬───────┘
                         │ long polling
                  ┌──────▼────────────────────┐         ┌───────────────┐
                  │           bot             │────────►│     Redis     │
                  │  :8080 (app) :8090 (mgmt) │  кэш    └───────────────┘
                  └──────┬──────────────▲─────┘
              REST       │              │  уведомления
        (подписки, теги) │              │  HTTP POST /updates  ИЛИ  Kafka topic
                  ┌──────▼──────────────┴─────┐         ┌───────────────┐
                  │         scrapper          │────────►│  PostgreSQL   │
                  │  :8081 (app) :8091 (mgmt) │         └───────────────┘
                  └──────┬────────────────────┘
                         │ periodic scrape
                  ┌──────▼───────────────────┐
                  │  GitHub API / SO API     │
                  └──────────────────────────┘
```

**bot** — вход для пользователя: принимает сообщения Telegram, разбирает команды и диалоги,
обращается к `scrapper` по REST, рассылает уведомления об обновлениях.

**scrapper** — владелец данных: хранит чаты, ссылки, теги и фильтры, по расписанию опрашивает
внешние API и отправляет обновления в `bot`.

Общего модуля намеренно нет — микросервисная изоляция. Контракт описан в OpenAPI-спецификации
**на каждой стороне отдельно**, DTO генерируются per-service.

---

## Технологический стек

| Категория | Технологии |
|---|---|
| Язык / рантайм | Java 21 (виртуальные потоки) |
| Фреймворк | Spring Boot 3.5.16 (Web, Validation, AOP, Data JPA, Data Redis, Cache, Kafka, Actuator) |
| Сборка | Maven (multi-module), Spotless + google-format |
| Telegram | pengrad `java-telegram-bot-api` 10.0.0 |
| БД | PostgreSQL, Liquibase (SQL-миграции) |
| Кэш | Redis |
| Брокер | Apache Kafka (KRaft, single-node) |
| Устойчивость | Resilience4j 2.4.0 |
| Метрики | Micrometer + Prometheus + Grafana |
| Контракты | OpenAPI 3.0 + `openapi-generator-maven-plugin` (генератор `spring`, только модели) |
| Тесты | JUnit 5, Mockito, Testcontainers, WireMock, Spring Boot Test |
| Инфраструктура | Docker Compose, GitHub Actions, GHCR |

---

## Возможности

- Подписка на ссылки GitHub (`https://github.com/{owner}/{repo}`) и StackOverflow
  (`https://stackoverflow.com/questions/{id}`) с валидацией источника на стороне `scrapper`.
- Детализированные уведомления:
  - **GitHub** — новый PR или Issue: название, автор, время создания, превью описания (200 символов).
  - **StackOverflow** — новый ответ, комментарий или правка вопроса: тема, автор, время, превью.
- **Теги и фильтры** для группировки подписок и фильтрации списка.
- **Режимы уведомлений**: мгновенные (`instant`) и дайджест в заданный час (`digest`).
- Батчевый планировщик с keyset-пагинацией и параллельной обработкой на виртуальных потоках.
- Два взаимозаменяемых способа доступа к данным: чистый SQL и ORM.
- Два взаимозаменяемых транспорта уведомлений: HTTP и Kafka с автоматическим fallback.

---

## Команды бота

| Команда | Описание |
|---|---|
| `/start` | Регистрация чата |
| `/help` | Список доступных команд |
| `/track` | Начать отслеживание ссылки (диалог: ссылка → теги → фильтры) |
| `/untrack` | Прекратить отслеживание ссылки |
| `/list [тег]` | Список отслеживаемых ссылок, опционально отфильтрованный по тегу |
| `/tag` | Управление тегами ссылки (диалог: выбор ссылки → действие → имя тега) |
| `/mode instant` \| `/mode digest [час]` | Режим уведомлений; без аргументов — пошаговый диалог |
| `/delete` | Удалить регистрацию и все подписки |

Команды, кроме `/start` и `/help`, требуют регистрации — незарегистрированный чат получает
подсказку отправить `/start`.

---

## Быстрый старт

### Требования

- JDK 21
- Maven 3.9+
- Docker + Docker Compose

### 1. Секреты

```bash
cp .env.example .env
```

Заполните `.env`:

| Переменная | Назначение |
|---|---|
| `TELEGRAM_TOKEN` | Токен бота от @BotFather |
| `GITHUB_TOKEN` | Токен GitHub API (опционально, повышает лимиты) |
| `STACKOVERFLOW_KEY` | Ключ StackOverflow API (опционально) |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Подключение к PostgreSQL (есть дефолты под compose) |

> В IntelliJ IDEA `.env` подключается плагином **EnvFile** в Run Configuration.

### 2. Запуск всего стека в Docker

```bash
docker compose up -d --build
```

Поднимутся: `postgres`, `redis`, `kafka`, `prometheus`, `grafana`, `bot`, `scrapper`.

### 3. Запуск приложений локально (инфраструктура в Docker)

```bash
docker compose up -d postgres redis kafka
mvn clean install
mvn -pl scrapper spring-boot:run
mvn -pl bot spring-boot:run
```

> **Важно:** миграции запускаются собственным `LiquibaseMigrationRunner` на
> `ApplicationReadyEvent`, changelog читается из classpath. При запуске из IDE убедитесь,
> что рабочая директория — корень проекта.

### 4. Проверка

| Что | Где |
|---|---|
| Prometheus targets | http://localhost:9090/targets — `bot` и `scrapper` в статусе `UP` |
| Grafana | http://localhost:3000 (`admin` / `admin`) → папка **LinkTracker** |
| Метрики bot / scrapper | http://localhost:8090/metrics , http://localhost:8091/metrics |

---

## Конфигурация

Типизированные настройки — через `@ConfigurationProperties` (`AppProperties`, record + `@Validated`).

| Свойство | Значения | Описание |
|---|---|---|
| `app.access-type` | `SQL` \| `ORM` | Провайдер доступа к данным (дефолт `SQL`) |
| `app.message-transport` | `HTTP` \| `Kafka` | Основной транспорт уведомлений (дефолт `HTTP`) |
| `app.scheduler.interval` | ISO-8601, напр. `PT1M` | Интервал тика планировщика |
| `app.scheduler.batch-size` | число | Размер батча ссылок за тик |
| `app.scheduler.parallelism` | число | Лимит одновременных проверок (семафор) |
| `app.kafka.topics.updates` | имя топика | Топик обновлений |
| `app.kafka.topics.updates-dlq` | имя топика | Dead Letter Queue |
| `app.cache.enabled` | `true` \| `false` | Кэширование в bot |
| `app.metrics.links-refresh-interval` | ISO-8601 | Период обновления gauge активных ссылок |

> `app.message-transport` задаётся **независимо** в каждом сервисе. Для end-to-end работы через
> Kafka значение `Kafka` должно быть выставлено в **обоих** сервисах: иначе scrapper пишет в топик,
> а bot не поднимает consumer — молча, без ошибок.

---

## API

### scrapper (`:8081`)

| Метод | Путь | Описание |
|---|---|---|
| `POST` | `/tg-chat/{id}` | Зарегистрировать чат |
| `DELETE` | `/tg-chat/{id}` | Удалить чат и его подписки |
| `GET` | `/tg-chat/{id}` | Проверить, зарегистрирован ли чат |
| `GET` | `/links` | Список ссылок чата (опционально `?tag=`) |
| `POST` | `/links` | Добавить ссылку |
| `DELETE` | `/links` | Удалить ссылку |
| `GET` | `/tags` | Все теги чата |
| `POST` | `/links/tags` | Добавить тег ссылке |
| `DELETE` | `/links/tags` | Снять тег со ссылки |

Идентификация чата — заголовком `Tg-Chat-Id`. Ошибки возвращаются единым `ApiErrorResponse`
через `@RestControllerAdvice`.

### bot (`:8080`)

| Метод | Путь | Описание |
|---|---|---|
| `POST` | `/updates` | Приём уведомления об обновлении (HTTP-транспорт) |

---

## База данных и миграции

Схема живёт в каталоге `migrations/` в корне проекта, миграции написаны на SQL, управляются
Liquibase. Автозапуск Liquibase отключён (`spring.liquibase.enabled=false`) — миграции
накатывает собственный `LiquibaseMigrationRunner`.

Таблицы: `chats`, `links`, `chat_links`, `link_tags`, `link_filters` (+ служебные
`databasechangelog`, `databasechangeloglock`).

Hibernate не владеет схемой: `spring.jpa.hibernate.ddl-auto=none`.

### Два провайдера доступа

За интерфейсами `SubscriptionRepository`, `LinkPollingRepository` и `LinkMetricsRepository` стоят
две полные реализации — на `JdbcClient`/чистом SQL и на JPA. Выбор атомарный, через
`@ConditionalOnProperty` на конфигурационных классах:

```properties
app.access-type=SQL   # или ORM
```

Планировщик обходит ссылки **keyset-пагинацией** по составному ключу `(last_checked_at, id)` —
все ссылки не загружаются в память сразу, а ссылки, обновлённые в текущем тике, отсекаются.

---

## Асинхронное взаимодействие и кэш

### Kafka

`NotificationSender` в scrapper — абстракция отправки, за которой стоят `HttpNotificationSender`
и `KafkaNotificationSender`, выбираемые по `app.message-transport`.

В bot consumer — `KafkaUpdateListener` (входной адаптер, симметричный `UpdateController`).
Оба пути сходятся в едином `UpdateProcessor`, поэтому downstream-логика не дублируется.

**DLQ.** Два класса ошибок ведут в один dead-letter-топик:

- ошибки десериализации — `ErrorHandlingDeserializer` (value как `byte[]`);
- семантически невалидные сообщения — `UpdateValidator` (value как `LinkUpdate`).

Оба обрабатываются одним `DeadLetterPublishingRecoverer` с `DelegatingByTypeSerializer`
(мапа по типу value), `FixedBackOff(0, 0)` — без ретраев.

### Redis

| Кэш | Что хранит | TTL |
|---|---|---|
| `linksByChat` | ответ `/list` | 5 минут |
| `chatRegistered` | статус регистрации чата | 7 дней |
| digest-буфер | накопленные обновления (Redis LIST) | до флаша |

`/list <тег>` не кэшируется осознанно — это производная выборка от полного списка.

Регистрация инкапсулирована в `RegistrationService` (`registerIfAbsent` / `unregister`):
вызывающий код видит одно действие, а не «scrapper + кэш» по отдельности.

---

## Устойчивость

Все пять паттернов реализованы на Resilience4j:

| Паттерн | Реализация |
|---|---|
| **Timeout** | `ClientHttpRequestFactorySettings` (connect/read) на всех HTTP-клиентах |
| **Retry** | `RetryRegistry`, ручное оборачивание вызовов; единый список retryable-статусов на все клиенты |
| **Circuit Breaker** | `CircuitBreakerRegistry`, общий метод `decorate(name, call)` — **CB снаружи Retry** |
| **Rate Limiting** | `HandlerInterceptor` + `RateLimiterRegistry`, лимит по IP |
| **Fallback** | `FallbackNotificationSender` — при отказе основного транспорта уведомление уходит вторым (HTTP ⇄ Kafka) |

Retry и Circuit Breaker применяются **не аннотациями**: аннотационные декораторы Resilience4j
несовместимы с прокси-цепочкой `@HttpExchange`. `ScrapperClient` (12 методов) обёрнут через
JDK Dynamic Proxy с `@Primary` над делегатом.

---

## Наблюдаемость

### Actuator

`/metrics` вынесен на выделенный management-порт, отдельный от порта приложения:

| Сервис | Приложение | Метрики |
|---|---|---|
| bot | `8080` | `8090` |
| scrapper | `8081` | `8091` |

Ко всем метрикам добавлен лейбл `application`; для `http.server.requests` включена
percentile-гистограмма.

### Бизнес-метрики

| Метрика | Тип | Модуль | Имя в Prometheus |
|---|---|---|---|
| `bot.user.messages` | Counter | bot | `bot_user_messages_total` |
| `scrapper.links.active` | Gauge (тег `type`) | scrapper | `scrapper_links_active` |
| `scrapper.scrape.duration` | Timer + histogram (тег `type`) | scrapper | `scrapper_scrape_duration_seconds_bucket` |

- Counter инкрементируется на каждое входящее пользовательское сообщение.
- Gauge обновляется по расписанию (`app.metrics.links-refresh-interval`) и считает ссылки по типу
  источника через узкий `LinkMetricsRepository` — логика метрик не смешана с планировщиком.
  Реализация на `ConcurrentHashMap<String, AtomicLong>` + `computeIfAbsent`, чтобы не
  регистрировать gauge повторно.
- Timer оборачивает всю проверку ссылки (`checkOne`) в `try/finally` — длительность фиксируется
  и на пути с ошибкой; тег `type` берётся из `UpdateChecker.type()`.

### Дашборды

Datasource и дашборды Grafana поднимаются автоматически через provisioning, без ручной настройки:

- **RED & Resources** — параметризован по `application`: Rate / Errors / Duration + использование
  памяти JVM с разбивкой по `area`.
- **Business Metrics** — сообщений/сек, активные ссылки по типу, p50/p95/p99 времени одного scrape
  по типу.

<details>
<summary>PromQL-запросы</summary>

```promql
# Rate
sum(rate(http_server_requests_seconds_count{application="$application"}[1m]))

# Errors — доля 5xx
sum(rate(http_server_requests_seconds_count{application="$application", status=~"5.."}[1m]))
/
sum(rate(http_server_requests_seconds_count{application="$application"}[1m]))

# Duration — p50 / p95 / p99
histogram_quantile(0.50, sum by (le) (rate(http_server_requests_seconds_bucket{application="$application"}[5m])))
histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{application="$application"}[5m])))
histogram_quantile(0.99, sum by (le) (rate(http_server_requests_seconds_bucket{application="$application"}[5m])))

# JVM memory по типу
sum by (area) (jvm_memory_used_bytes{application="$application"})

# Бизнес-метрики
rate(bot_user_messages_total[1m])
scrapper_links_active
histogram_quantile(0.95, sum by (le, type) (rate(scrapper_scrape_duration_seconds_bucket[5m])))
```

</details>

---

## Тестирование

```bash
mvn clean verify              # весь проект
mvn -pl scrapper verify       # отдельный модуль
mvn spotless:apply            # форматирование
mvn spotless:check            # проверка формата (как в CI)
```

Что покрыто:

- **Репозитории** — интеграционные тесты на Testcontainers (PostgreSQL), абстрактный базовый класс
  прогоняется для обеих реализаций (SQL и ORM).
- **REST** — end-to-end тесты контроллеров scrapper, включая пути ошибок.
- **Bot** — сквозной прогон `handle(Update)` через диспетчер, диалоги и команды с моком
  `ScrapperClient`; `Update` строится через `BotUtils.parseUpdate(json)`.
- **Внешние API** — WireMock (`wiremock-standalone`) для GitHub/StackOverflow.
- **Kafka** — Testcontainers (`apache/kafka:3.8.0`), топики создаются напрямую через `AdminClient`.
- **Устойчивость** — тесты на каждый из пяти паттернов.
- **Метрики** — `UpdateListenerTest` (инкремент counter), `LinkMetricsGaugeTest` (gauge отражает
  репозиторий, повторный refresh не дублирует регистрацию), `UpdateSchedulerTest` (timer фиксирует
  длительность, включая путь с ошибкой; разные типы — раздельные ряды).

Контейнеры поднимаются по singleton-паттерну (`static { CONTAINER.start(); }`), чтобы кэш
Spring-контекста работал между тестовыми классами.

---

## CI/CD

GitHub Actions запускается на push в `main` и на все PR:

1. `spotless:check` — проверка формата.
2. `mvn verify` — сборка и полный набор тестов (включая Testcontainers).
3. Сборка Docker-образов `bot` и `scrapper` через matrix — после прохождения тестов.
4. Публикация образов в **GHCR** — только на push в `main`; на PR образы лишь собираются
   (проверка Dockerfile).

`bot.Dockerfile` и `scrapper.Dockerfile` — multi-stage: сборка на Maven-образе, рантайм на тонком JRE.

---

## Структура проекта

```
link-tracker-new/
├── bot/                        # Telegram-бот
│   └── src/main/
│       ├── java/by/shaaldy/bot/
│       │   ├── command/        # /start, /help, /track, /untrack, /list, /tag, /mode, /delete
│       │   ├── config/         # клиенты, Kafka, Redis, resilience
│       │   ├── controller/     # UpdateController, KafkaUpdateListener
│       │   ├── dialog/         # FSM: DialogState, DialogContext, DialogHandler
│       │   ├── metrics/
│       │   └── service/        # UpdateProcessor, RegistrationService, DigestBuffer, ...
│       └── resources/openapi/  # контракт бота
├── scrapper/                   # мониторинг ссылок
│   └── src/main/
│       ├── java/by/shaaldy/scrapper/
│       │   ├── client/         # GitHubClient, StackOverflowClient, BotClient
│       │   ├── controller/     # TgChatController, LinkController
│       │   ├── domain/
│       │   ├── notification/   # NotificationSender: HTTP / Kafka / Fallback
│       │   ├── repository/     # sql/ + orm/ реализации
│       │   ├── scheduler/      # UpdateScheduler
│       │   └── service/        # SubscriptionService
│       └── resources/openapi/  # контракт scrapper
├── migrations/                 # Liquibase changelog + SQL changesets
├── grafana/                    # provisioning: datasource + дашборды
├── prometheus/                 # prometheus.yml
├── bot.Dockerfile
├── scrapper.Dockerfile
├── compose.yml
└── pom.xml
```

---

## Архитектурные решения

**Нет общего модуля.** Контракт дублируется как OpenAPI-спецификация на каждой стороне, DTO
генерируются per-service. Это цена микросервисной изоляции: сервисы не связаны общим кодом
и версионируются независимо.

**DTO не проникают глубже контроллера.** Маппинг DTO ↔ домен происходит только на границе;
сервисный и репозиторный слои знают исключительно доменные типы. Ни `ResultSet`, ни JPA-entity
не покидают границ реализации репозитория.

**Репозиторий «глупый».** Не бросает доменных исключений и не валидирует инварианты — возвращает
`boolean`/данные. Вся бизнес-логика и исключения — в сервисе.

**Разделение интерфейсов по клиентам.** `SubscriptionRepository` (CRUD для контроллеров),
`LinkPollingRepository` (батчевый обход для планировщика) и `LinkMetricsRepository` (агрегаты для
метрик) — разные клиенты с разным характером операций, поэтому разные интерфейсы.

**Переключение провайдеров атомарно.** `@ConditionalOnProperty` стоит на `@Configuration`-классах,
а не на отдельных бинах — активируется целиком SQL-набор либо целиком ORM-набор.

**Circuit Breaker снаружи Retry.** Иначе CB видел бы одну «попытку» вместо серии и не успевал
открываться.

**Виртуальные потоки + семафор в планировщике.** `checkOne` — I/O-bound (HTTP к GitHub/SO/bot и
БД), поэтому пул фиксированных потоков избыточен; параллелизм ограничен `Semaphore`.

**Синхронная отправка в Kafka.** `KafkaTemplate.send(...).get(timeout)` — осознанный компромисс:
блокировка на асинхронной отправке нужна, чтобы отказ был виден и сработал fallback.

**Известное поведение.** Инициализация `last_checked_at` эпохой даёт гарантированное уведомление
на первом тике после добавления ссылки — принятый трейд-офф, а не баг.

---

## Этапы разработки

| Этап | Содержание | Статус |
|---|---|---|
| 1 | Скелет, REST-контракты, FSM-диалоги, in-memory хранилище | ✅ |
| 2 | PostgreSQL, Liquibase, SQL/ORM провайдеры, детализация уведомлений (+ теги, многопоточный шедулер) | ✅ |
| 3 | Kafka + DLQ, Redis-кэш (+ дайджест-режим уведомлений) | ✅ |
| 4 | Timeout, Retry, Circuit Breaker, Rate Limiting, Fallback | ✅ |
| 5 | Метрики (Actuator + Micrometer), Prometheus + Grafana, контейнеризация, публикация образов | ✅ |
