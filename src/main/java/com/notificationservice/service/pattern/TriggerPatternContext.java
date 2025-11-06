package com.notificationservice.service.pattern;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

@Getter
public class TriggerPatternContext {
    private final JsonNode config;

    public TriggerPatternContext(JsonNode config) {
        this.config = config;
    }

}