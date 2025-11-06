package com.notificationservice.controller;

import com.notificationservice.dto.ActionDto;
import com.notificationservice.service.ActionProcessor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер для регистрации действий пользователей
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ActionController {

    private final ActionProcessor actionProcessor;

    @PostMapping("/actions")
    public ResponseEntity<String> addAction(@Valid @RequestBody ActionDto actionDto,
                                            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body("Validation error");
        }

        actionProcessor.processAction(actionDto);
        return ResponseEntity.ok("Action processed successfully\n");
    }
}