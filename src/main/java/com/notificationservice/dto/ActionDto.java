package com.notificationservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO для регистрации действий пользователя
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActionDto {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Action type is required")
    private String actionType;

    @NotNull(message = "Timestamp is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant timestamp;
}