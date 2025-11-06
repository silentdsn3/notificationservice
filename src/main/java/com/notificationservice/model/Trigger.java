package com.notificationservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Конфигурация триггера для умных уведомлений
 * Определяет условия срабатывания и шаблон сообщения
 */
@Entity
@Table(name = "triggers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Trigger {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String actionType;

    @Column(nullable = false)
    private String patternType;

    @Column(columnDefinition = "TEXT")
    private String conditionConfig;

    @Column(nullable = false, length = 1000)
    private String messageTemplate;

    @Column(nullable = false)
    private int cooldownHours = 24;
}