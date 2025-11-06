package com.notificationservice.config;

import com.notificationservice.model.Trigger;
import com.notificationservice.repository.TriggerRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Загрузчик триггеров в базу данных при старте приложения
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TriggerLoader {

    private final TriggerRepository triggerRepository;
    private final TriggerConfig triggerConfig;

    @PostConstruct
    public void initializeTriggers() {
        try {
            triggerRepository.deleteAll();
            List<Trigger> predefinedTriggers = triggerConfig.getPredefinedTriggers();
            triggerRepository.saveAll(predefinedTriggers);

            log.info("Загружено {} триггеров в базу данных", predefinedTriggers.size());
        } catch (Exception e) {
            log.error("Ошибка при инициализации триггеров", e);
            throw new RuntimeException("Не удалось инициализировать триггеры", e);
        }
    }
}