# **Архитектура и ключевые компоненты**

Контроллеры (REST API):
* ActionController — POST /api/actions, принимает действие { userId, actionType, timestamp } и запускает конвейер обработки 
* NotificationController — GET /api/notifications, возвращает созданные системой уведомления 

Сервисный слой:
* ActionProcessor — транзакционно сохраняет действие, проверяет подходящие триггеры и создает уведомления с учетом валидности конфигурации и cooldown на триггер 
* TriggerEngine — по actionType подбирает список триггеров и делегирует проверку матчинга конкретным паттернам (PatternMatcher) 
* NotificationService — применяет политику shouldSendNotification (cooldown) и сохраняет Notification 

Паттерны активности (service.pattern):
* DefaultPatternMatcher — роутер по типу паттерна (FREQUENCY, TIME_PATTERN, WEEKDAY_PATTERN, GROWTH, SEQUENCE, MULTI_ACTIVITY, SPECIAL_DATE, STABILITY), формирует TriggerPatternContext из JSON-конфигурации 
* FrequencyPattern — "N действий за M минут" 
* TimePattern — активность в заданном окне времени суток (поддержка окон через полночь, например 23:00–04:00) за N разных дней 
* WeekdayPattern — активность в указанные дни недели в пределах последних W недель; опционально проверяет текущий "checkDay" (например, "TUESDAY") 
* GrowthPattern — сравнение активности за «текущий период» и «предыдущий период» (рост в процентах при минимальном базовом объеме) 
* SequencePattern — анализ последовательностей действий (например: "LOGIN -> READ_ARTICLE -> COMMENT" в течение 30 минут)
* MultiActivityPattern — проверка разнообразной активности (разные типы действий в заданном окне)
* SpecialDatePattern — анализ активности в особые даты (праздники, дни рождения)
* StabilityPattern — анализ стабильности и трендов активности (стабильная активность, соотношение выходные/будни, растущая вовлеченность)

Конфигурация триггеров:
* TriggerConfig — единая фабрика предопределенных триггеров (night owl, coffee discount и др.), описанных через JSON в поле conditionConfig 
* ConfigurationValidator — строгая валидация JSON-конфигураций триггеров: типы паттернов, наличие обязательных полей, форматы времени/дней и значения порогов 
* TriggerLoader — загружает предопределенные триггеры в БД при старте (create-drop), TriggerProvider — кэширует триггеры по actionType для быстрых lookup 

Данные и доступ:
* Модели: Action, Trigger, Notification 
* Репозитории: ActionRepository (поиск в окнах времени, агрегации), NotificationRepository (поиск последних уведомлений по пользователю/триггеру), TriggerRepository 

Утилиты:
* TimeUtils — работа только в UTC: парсинг времени "HH:mm", преобразования Instant→LocalDate/LocalTime/DayOfWeek, корректная проверка интервалов через полночь 

Поток обработки (высокоуровнево):

1. Клиент шлет POST /api/actions.
2. ActionProcessor сохраняет Action и синхронно вызывает TriggerEngine
3. TriggerEngine выбирает триггеры по actionType, DefaultPatternMatcher проверяет конфигурацию и делегирует в конкретный паттерн
4. Если совпало: NotificationService проверяет cooldown по этому триггеру и пользователю; при разрешении создает Notification 

# **Эвристика «непредсказуемости»**

* Без расписаний и фоновых джоб: уведомления рождаются не "по времени", а "по событию", в момент, когда поведение пользователя образует распознанный паттерн (например, достигнут порог частоты или выполнены нужные дни недели) — поэтому время уведомления напрямую зависит от динамики действий, а не от таймера 
* Реактивность и контекст: каждое новое действие может немедленно "перевести" пользователя через порог паттерна (последняя чашка кофе, комментарий в ночном окне, "вторник" после пн/ср/пт и т.д.) 
* Временные окна и разнообразие паттернов: сочетание окон времени суток (включая через полночь), окон в минутах/днях/неделях, сравнение периодов (рост) и проверка рабочих/выходных дней приводит к вариативным моментам срабатывания 
* Персональный cooldown на триггер: даже при частых совпадениях для конкретного пользователя, повторное уведомление по тому же триггеру "затухает" на указанные часы, чтобы избежать спама и сделать появление сообщений более "редким" и заметным 
* Расширяемость: добавление новых оригинальных паттернов (например, последовательности действий, мульти‑активность, специальные даты) делает "умные" реакции богаче без перехода к предсказуемым расписаниям 

## Предопределенные «умные» триггеры (из TriggerConfig)

* NIGHT_OWL (TimePattern): активность "COMMENT" внутри 23:00–04:00 (UTC) в N разных дней — показывает, что пользователь "сова" 
* EARLY_BIRD (TimePattern): активность "COMMENT" в 05:00–08:00 несколько дней подряд 
* COMMENT_SPAM (Frequency): ≥10 "COMMENT" за 60 минут 
* WEEKLY_READER (Frequency): ≥20 "READ_ARTICLE" за 7 дней 
* BIG_SPENDER (Frequency): ≥5 "PURCHASE" за сутки 
* COFFEE_ADDICT (Frequency): ≥3 "COFFEE_PURCHASE" за сутки 
* WEEKEND_WARRIOR (Weekday): "WORKOUT" по выходным ("SATURDAY", "SUNDAY") в пределах нескольких недель 
* COFFEE_DISCOUNT (Weekday): покупки кофе по "MONDAY", "WEDNESDAY", "FRIDAY"; предложение скидки, когда наступил "TUESDAY" 
* MONDAY_MOTIVATION (Weekday): активный "LOGIN" по понедельникам 
* ACTIVITY_GROWTH (Growth): рост комментариев на X% при достаточной базе за сравниваемые периоды 
* PURCHASE_GROWTH (Growth): рост покупок на X% при достаточной базе 
* MORNING_RITUAL (Sequence): последовательность "LOGIN → READ_ARTICLE → COMMENT" в утренние часы
* COFFEE_BREAK (Sequence): последовательность "WORKOUT → COFFEE_PURCHASE → READ_ARTICLE" 
* MULTI_ACTIVE_USER (MultiActivity): разнообразная активность (комментарии, чтение, лайки) в течение дня
* UNIVERSAL_DOER (MultiActivity): активность в 4+ разных сферах за неделю
* BIRTHDAY (SpecialDate): повышенная активность в день рождения пользователя
* HOLIDAY_SHOPPING (SpecialDate): увеличение покупок в праздничные дни
* STABLE_ACTIVITY (Stability): стабильная ежедневная активность
* WEEKEND_VS_WEEKDAY (Stability): соотношение активности в выходные/будни
* GROWING_ENGAGEMENT (Stability): растущая вовлеченность в течение недели
* WEEKLY_MARATHON (Stability): последовательный недельный рост активности
* SEASONAL_PATTERN (Stability): сезонные изменения активности
* WEEKLY_BALANCE (Stability): равномерное распределение активности по дням недели

## REST API — примеры запросов и ответов

### * Добавить действие пользователя

    POST /api/actions
    Content-Type: application/json
    Тело: { "userId": "user1", "actionType": "COMMENT", "timestamp": "2024-01-15T23:30:00Z" }
    Ответ: 200 OK
    Action processed successfully

Примечания:
* timestamp — обязательный Instant в UTC (формат ISO-8601), вся логика времени в сервисе — в UTC 
* actionType — произвольная строка, должна совпадать с actionType у триггеров, иначе триггеры не проверяются 

### * Получить все уведомления

    GET /api/notifications
    Ответ: 200 OK
    [
      {
        "id": 1,
        "userId": "user1", 
        "title": "Умное уведомление",
        "message": "Кажется, вы сова! Вы часто комментируете ночью. Не забывайте отдыхать!",
        "timestamp": "2024-01-16T00:00:01Z",
        "status": "PENDING"
      }
    ]

Примечание: уведомления возвращаются по убыванию времени создания; статус по умолчанию PENDING 

### Примеры с test_script.bat (Windows):

* Скрипт генерирует события под разные триггеры и в конце делает GET /notifications, чтобы увидеть созданные записи 
* Для ночного паттерна — несколько COMMENT от user1 в окне 23:00–04:00 на разные дни; для частотного — пачка комментариев за короткое окно; для кофе/выходных — покупки/тренировки в нужные дни 

## Быстрый старт

* Требования: Java 17+, Gradle, PostgreSQL (локально)
* Конфиг по умолчанию (src/main/resources/application.yml):
  * jdbc:postgresql://localhost:5432/postgres
  * username/password: postgres/postgres
  * ddl-auto: create-drop, schema: notification_service
  * порт: 8080 

* Шаги:
1. Запустите PostgreSQL локально и убедитесь, что креды совпадают с application.yml (или измените их) 
2. ./gradlew bootRun (Windows: gradlew.bat bootRun) 
3. Отправляйте события через curl/Postman; для быстрой проверки можно выполнить test_script.bat 

## Важные детали реализации

* **Реактивная проверка триггеров**: никаких фоновых job и cron — сопоставление паттернов идет синхронно при каждом POST /actions, что соответствует ТЗ и обеспечивает «непредсказуемость» момента срабатывания 
* **Валидатор конфигураций**: ConfigurationValidator строго проверяет JSON для каждого типа паттерна — это повышает надежность триггеров и предотвращает ошибочные уведомления 
* **Cooldown на триггер/пользователя**: NotificationService блокирует повторную отправку уведомления по тому же триггеру, если последнее отправлено недавно (по триггерному cooldownHours) 
* **Вся работа со временем — в UTC**: консистентность проверки окон и дней недели вне зависимости от часовых поясов клиентов

## Как добавить новый триггер

1. Добавьте конфигурацию в TriggerConfig: указать id, name, actionType, patternType, JSON conditionConfig, messageTemplate, cooldownHours 
2. Если нужен новый тип паттерна — реализуйте TriggerPattern (в service.pattern), добавьте бин и ветку в DefaultPatternMatcher, расширьте валидатор ConfigurationValidator 
3. Перезапустите приложение — TriggerLoader перезальет триггеры (create-drop), TriggerProvider закэширует их по actionType 

## Тестирование

* Юнит‑тесты покрывают валидацию конфигураций, матчинг паттернов (Frequency, DefaultPatternMatcher), поведение TriggerEngine и утилиты времени (UTC и окна через полночь) 
* Ключевые тесты:
  * ConfigurationValidatorTest — позитивные/негативные кейсы JSON‑схем 
  * FrequencyPatternTest, DefaultPatternMatcherTest — корректная маршрутизация и матчинги 
  * TriggerEngineTest — корректный отбор триггеров и устойчивость к исключениям 
  * TimeUtilsTest — корректная работа с UTC и окнами 
  
## Ограничения и планы

* Время обрабатывается только в зоне UTC
* Некоторые сложные паттерны (Sequence/Multi-Activity/SpecialDate) имеют упрощенную реализацию для демонстрации
* Конфигурацию триггеров вынести в базу данных и добавить контроллер для обновления используемых триггеров из базы на лету, чтоб не перезапускать приложение
* Добавить более сложные алгоритмы анализа трендов для StabilityPattern
* Реализовать интеграцию с реальным календарем праздников для SpecialDatePattern
