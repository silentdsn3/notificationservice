package com.notificationservice.config;

import com.notificationservice.model.Trigger;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конфигурация предопределенных триггеров системы
 * Отвечает только за создание конфигураций триггеров
 * <p>
 * Триггеры реализуют логику "умных" уведомлений на основе паттернов активности пользователей:
 * - TimePattern: активность в определенное время суток
 * - FrequencyPattern: частота действий во временном окне
 * - WeekdayPattern: активность по определенным дням недели
 * - GrowthPattern: рост активности по сравнению с предыдущим периодом
 */
@Component
public class TriggerConfig {

    /**
     * Возвращает список всех предопределенных триггеров системы
     */
    public List<Trigger> getPredefinedTriggers() {
        return List.of(
                createNightOwlTrigger(),
                createEarlyBirdTrigger(),
                createCommentSpamTrigger(),
                createWeeklyReaderTrigger(),
                createBigSpenderTrigger(),
                createCoffeeAddictTrigger(),
                createWeekendWarriorTrigger(),
                createCoffeeDiscountTrigger(),
                createMondayMotivationTrigger(),
                createActivityGrowthTrigger(),
                createPurchaseGrowthTrigger(),
                createMorningRitualTrigger(),
                createCoffeeBreakTrigger(),
                createMultiActiveUserTrigger(),
                createUniversalDoerTrigger(),
                createBirthdayTrigger(),
                createHolidayShoppingTrigger(),
                createStableActivityTrigger(),
                createWeekendVsWeekdayTrigger(),
                createGrowingEngagementTrigger(),
                createWeeklyMarathonTrigger(),
                createSeasonalPatternTrigger(),
                createWeeklyBalanceTrigger()
        );
    }

    /**
     * Триггер "Ночная сова" - пользователь активен ночью 3+ дней
     * Пример из ТЗ: если пользователь пишет комментарии ночью три дня подряд
     */
    private Trigger createNightOwlTrigger() {
        String configJson = """
                {
                    "startTime": "23:00",
                    "endTime": "04:00",
                    "requiredDays": 3,
                    "consecutive": false,
                    "windowDays": 30
                }
                """;

        return new Trigger(
                "NIGHT_OWL",
                "Ночная сова",
                "COMMENT",
                "TIME_PATTERN",
                configJson,
                "Кажется, вы сова! Вы часто комментируете ночью. Не забывайте отдыхать!",
                24
        );
    }

    /**
     * Триггер "Ранняя пташка" - пользователь активен рано утром
     */
    private Trigger createEarlyBirdTrigger() {
        String configJson = """
                {
                    "startTime": "05:00",
                    "endTime": "08:00",
                    "requiredDays": 4,
                    "consecutive": true,
                    "windowDays": 14
                }
                """;

        return new Trigger(
                "EARLY_BIRD",
                "Ранняя пташка",
                "COMMENT",
                "TIME_PATTERN",
                configJson,
                "Вы настоящая ранняя пташка! Активность с 5 до 8 утра - это впечатляет!",
                48
        );
    }

    /**
     * Триггер "Активный комментатор" - 10+ комментариев за час
     */
    private Trigger createCommentSpamTrigger() {
        String configJson = """
                {
                    "actionCount": 10,
                    "timeWindowMinutes": 60
                }
                """;

        return new Trigger(
                "COMMENT_SPAM",
                "Активный комментатор",
                "COMMENT",
                "FREQUENCY",
                configJson,
                "Вы очень активны! 10 комментариев за час - это впечатляет!",
                6
        );
    }

    /**
     * Триггер "Активный читатель" - 20+ статей за неделю
     */
    private Trigger createWeeklyReaderTrigger() {
        String configJson = """
                {
                    "actionCount": 20,
                    "timeWindowMinutes": 10080
                }
                """;

        return new Trigger(
                "WEEKLY_READER",
                "Активный читатель",
                "READ_ARTICLE",
                "FREQUENCY",
                configJson,
                "Вы прочитали 20 статей на этой неделе! Продолжайте в том же духе!",
                24
        );
    }

    /**
     * Триггер "Крупный покупатель" - 5+ покупок за день
     */
    private Trigger createBigSpenderTrigger() {
        String configJson = """
                {
                    "actionCount": 5,
                    "timeWindowMinutes": 1440
                }
                """;

        return new Trigger(
                "BIG_SPENDER",
                "Крупный покупатель",
                "PURCHASE",
                "FREQUENCY",
                configJson,
                "5 покупок за день! Благодарим за активность! Специальное предложение ждет вас.", 12
        );
    }

    /**
     * Триггер "Кофеман" - 3+ покупки кофе за день
     */
    private Trigger createCoffeeAddictTrigger() {
        String configJson = """
                {
                    "actionCount": 3,
                    "timeWindowMinutes": 1440
                }
                """;

        return new Trigger(
                "COFFEE_ADDICT",
                "Кофеман",
                "COFFEE_PURCHASE",
                "FREQUENCY",
                configJson,
                "Заметили вашу любовь к кофе! 3 чашки за день - может, попробуете наш новый сорт?",
                24
        );
    }

    /**
     * Триггер "Спортсмен выходного дня" - активность по выходным
     */
    private Trigger createWeekendWarriorTrigger() {
        String configJson = """
                {
                    "requiredDays": ["SATURDAY", "SUNDAY"],
                    "windowWeeks": 3
                }
                """;

        return new Trigger(
                "WEEKEND_WARRIOR",
                "Спортсмен выходного дня",
                "WORKOUT",
                "WEEKDAY_PATTERN",
                configJson,
                "Замечаем вашу спортивную активность по выходным! Попробуйте короткие тренировки в будни для лучшего результата!", 48
        );
    }

    /**
     * Триггер "Скидка на кофе" - основной пример из ТЗ
     * Пользователь покупает кофе по пн, ср, пт -> предлагаем скидку во вторник
     */
    private Trigger createCoffeeDiscountTrigger() {
        String configJson = """
                {
                    "requiredDays": ["MONDAY", "WEDNESDAY", "FRIDAY"],
                    "checkDay": "TUESDAY",
                    "windowWeeks": 1
                }
                """;

        return new Trigger(
                "COFFEE_DISCOUNT",
                "Скидка на кофе",
                "COFFEE_PURCHASE",
                "WEEKDAY_PATTERN",
                configJson,
                "Заметили, что вы покупаете кофе по пн, ср, пт! Специальная скидка 20% ждет вас сегодня!",
                24
        );
    }

    /**
     * Триггер "Понедельник - день тяжелый" - активность в понедельник утром
     */
    private Trigger createMondayMotivationTrigger() {
        String configJson = """
                {
                    "requiredDays": ["MONDAY"],
                    "windowWeeks": 2
                }
                """;

        return new Trigger(
                "MONDAY_MOTIVATION",
                "Понедельник - день тяжелый",
                "LOGIN",
                "WEEKDAY_PATTERN",
                configJson,
                "Видим, что вы начинаете неделю активно! Небольшая мотивация для продуктивного понедельника!",
                168
        );
    }

    /**
     * Триггер "Рост активности" - увеличение комментариев на 50%+
     */
    private Trigger createActivityGrowthTrigger() {
        String configJson = """
                {
                    "growthPercent": 50,
                    "comparePeriodDays": 7,
                    "minActionCount": 10
                }
                """;

        return new Trigger(
                "ACTIVITY_GROWTH",
                "Рост активности",
                "COMMENT",
                "GROWTH",
                configJson,
                "Ваша активность выросла на 50% за неделю! Отличный прогресс, так держать!",
                72
        );
    }

    /**
     * Триггер "Рост покупок" - увеличение покупок на 30%+
     */
    private Trigger createPurchaseGrowthTrigger() {
        String configJson = """
                {
                    "growthPercent": 30,
                    "comparePeriodDays": 14,
                    "minActionCount": 5
                }
                """;

        return new Trigger(
                "PURCHASE_GROWTH",
                "Рост покупок",
                "PURCHASE",
                "GROWTH",
                configJson,
                "Заметили рост ваших покупок на 30%! Благодарим за лояльность! Специальный бонус для вас.",
                168
        );
    }

    private Trigger createMorningRitualTrigger() {
        String configJson = """
                {
                    "actionSequence": ["LOGIN", "READ_ARTICLE", "COMMENT"],
                    "maxTimeBetweenMinutes": 30,
                    "requiredDays": 3,
                    "timeWindow": "06:00-09:00"
                }
                """;

        return new Trigger(
                "MORNING_RITUAL",
                "Ритуал утра",
                "COMMENT",
                "SEQUENCE",
                configJson,
                "Заметили ваш утренний ритуал: вход → чтение → комментарий. Отличное начало дня!",
                168
        );
    }

    private Trigger createCoffeeBreakTrigger() {
        String configJson = """
                {
                    "actionSequence": ["WORKOUT", "COFFEE_PURCHASE", "READ_ARTICLE"],
                    "maxTimeBetweenMinutes": 15,
                    "minOccurrences": 3
                }
                """;

        return new Trigger(
                "COFFEE_BREAK",
                "Перерыв на кофе",
                "READ_ARTICLE",
                "SEQUENCE",
                configJson,
                "Видим, что после тренировки вы любите выпить кофе и почитать! Отличная привычка!",
                72
        );
    }

    private Trigger createMultiActiveUserTrigger() {
        String configJson = """
                {
                    "requiredActionTypes": ["COMMENT", "READ_ARTICLE", "LIKE"],
                    "timeWindowHours": 24,
                    "minUniqueTypes": 3
                }
                """;

        return new Trigger(
                "MULTI_ACTIVE_USER",
                "Мультиактивный пользователь",
                "LIKE",
                "MULTI_ACTIVITY",
                configJson,
                "Вы сегодня и комментировали, и читали, и ставили лайки! Вы настоящий мультиактивный пользователь!",
                24
        );
    }

    private Trigger createUniversalDoerTrigger() {
        String configJson = """
                {
                    "actionTypes": ["COMMENT", "PURCHASE", "WORKOUT", "LOGIN"],
                    "requiredCount": 10,
                    "windowDays": 7
                }
                """;

        return new Trigger(
                "UNIVERSAL_DOER",
                "Универсальный деятель",
                "LOGIN",
                "MULTI_ACTIVITY",
                configJson,
                "За неделю вы проявили активность в 4 разных сферах! Вы разносторонняя личность!",
                168
        );
    }

    private Trigger createBirthdayTrigger() {
        String configJson = """
                {
                    "specialDate": "USER_BIRTHDAY",
                    "minActions": 5,
                    "actionTypes": ["LOGIN", "COMMENT", "PURCHASE"]
                }
                """;

        return new Trigger(
                "BIRTHDAY",
                "День рождения",
                "LOGIN",
                "SPECIAL_DATE",
                configJson,
                "С Днем рождения! Заметили вашу повышенную активность сегодня! Поздравляем!",
                8760
        );
    }

    private Trigger createHolidayShoppingTrigger() {
        String configJson = """
                {
                    "holidays": ["NEW_YEAR", "CHRISTMAS", "BLACK_FRIDAY"],
                    "purchaseIncreasePercent": 200,
                    "compareToAverage": true
                }
                """;

        return new Trigger(
                "HOLIDAY_SHOPPING",
                "Праздничный шопинг",
                "PURCHASE",
                "SPECIAL_DATE",
                configJson,
                "Заметили, что в праздники вы покупаете больше! Специальная праздничная скидка для вас!",
                168
        );
    }

    private Trigger createStableActivityTrigger() {
        String configJson = """
                {
                    "minDailyAverage": 5,
                    "maxVariancePercent": 30,
                    "analysisPeriodDays": 14
                }
                """;

        return new Trigger(
                "STABLE_ACTIVITY",
                "Стабильная активность",
                "COMMENT",
                "STABILITY",
                configJson,
                "Ваша активность стабильна уже 2 недели! Так держать!",
                336
        );
    }

    private Trigger createWeekendVsWeekdayTrigger() {
        String configJson = """
                {
                    "weekendToWeekdayRatio": 2.0,
                    "minWeekendActions": 10,
                    "periodWeeks": 4
                }
                """;

        return new Trigger(
                "WEEKEND_VS_WEEKDAY",
                "Выходные vs Будни",
                "COMMENT",
                "STABILITY",
                configJson,
                "Заметили, что на выходных вы в 2 раза активнее! Отличный способ проводить свободное время!",
                336
        );
    }

    private Trigger createGrowingEngagementTrigger() {
        String configJson = """
                {
                    "trend": "INCREASING",
                    "minDays": 7,
                    "correlationThreshold": 0.7
                }
                """;

        return new Trigger(
                "GROWING_ENGAGEMENT",
                "Растущая вовлеченность",
                "COMMENT",
                "STABILITY",
                configJson,
                "Ваша активность растет уже неделю! Мы ценим ваше участие!",
                168
        );
    }

    private Trigger createWeeklyMarathonTrigger() {
        String configJson = """
                {
                    "minWeeklyGrowth": 20,
                    "consecutiveWeeks": 3,
                    "minBaseActions": 10
                }
                """;

        return new Trigger(
                "WEEKLY_MARATHON",
                "Недельный марафон",
                "COMMENT",
                "STABILITY",
                configJson,
                "Третью неделю подряд ваша активность растет на 20%! Вы участвуете в недельном марафоне!",
                336
        );
    }

    private Trigger createSeasonalPatternTrigger() {
        String configJson = """
                {
                    "patternType": "SEASONAL",
                    "season": "WINTER",
                    "activityChange": "INCREASE",
                    "minWeeks": 4
                }
                """;

        return new Trigger(
                "SEASONAL_PATTERN",
                "Сезонные паттерны",
                "COMMENT",
                "STABILITY",
                configJson,
                "Зимой вы становитесь активнее! Может быть, это связано с холодными вечерами дома?",
                720
        );
    }

    private Trigger createWeeklyBalanceTrigger() {
        String configJson = """
                {
                    "maxDailyVariance": 50,
                    "minWeeklyTotal": 20,
                    "evaluationWeeks": 2
                }
                """;

        return new Trigger(
                "WEEKLY_BALANCE",
                "Недельный баланс",
                "COMMENT",
                "STABILITY",
                configJson,
                "У вас отличный недельный баланс активности! Вы равномерно распределяете активность по дням!",
                336
        );
    }
}