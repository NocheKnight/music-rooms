package ru.music.webhook.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KeycloakEventDto(
        @JsonProperty("id") String id,
        @JsonProperty("realmId") String realmId,
        @JsonProperty("userId") String userId,
        @JsonProperty("eventType") String eventType,
        @JsonProperty("time") Long time,
        @JsonProperty("details") Map<String, String> details
) {
    // Удобные методы для извлечения данных пользователя
    public String getUsername() {
        return details != null ? details.get("username") : null;
    }

    public String getEmail() {
        return details != null ? details.get("email") : null;
    }

    public String getFirstName() {
        return details != null ? details.get("firstName") : null;
    }

    public String getLastName() {
        return details != null ? details.get("lastName") : null;
    }
}