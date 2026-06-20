package ru.music.webhook.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ru.music.webhook.config.RabbitMQConfig;
import ru.music.webhook.service.UserSyncService;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakEventListener {

    private final UserSyncService userSyncService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void handleEvent(String message) {
        log.info("Received raw message: {}", message);

        try {
            Map<String, Object> eventData = objectMapper.readValue(message, Map.class);

            String operationType = (String) eventData.get("operationType");
            String resourcePath = (String) eventData.get("resourcePath");
            String resourceType = (String) eventData.get("resourceTypeAsString");
            if (resourceType == null) {
                resourceType = (String) eventData.get("resourceType");
            }

            log.info("Parsed event: operationType={}, resourceType={}, resourcePath={}",
                    operationType, resourceType, resourcePath);

            if (!"USER".equals(resourceType)) {
                log.debug("Skipping non-user event: {}", resourceType);
                return;
            }

            String userId = extractUserIdFromPath(resourcePath);
            if (userId == null) {
                log.warn("Missing userId in event, skipping");
                return;
            }

            // Попытка извлечь данные пользователя из representation (если есть)
            String username = null;
            String email = null;
            String firstName = null;
            String lastName = null;

            String representationJson = (String) eventData.get("representation");
            if (representationJson != null && !representationJson.isEmpty()) {
                try {
                    Map<String, Object> userData = objectMapper.readValue(representationJson, Map.class);
                    username = (String) userData.get("username");
                    email = (String) userData.get("email");
                    firstName = (String) userData.get("firstName");
                    lastName = (String) userData.get("lastName");
                } catch (Exception e) {
                    log.warn("Failed to parse representation: {}", representationJson, e);
                }
            }

            // Если не удалось извлечь из representation, пробуем взять из details
            if (username == null) {
                Map<String, String> details = (Map<String, String>) eventData.get("details");
                if (details != null) {
                    username = details.get("username");
                    email = details.get("email");
                    firstName = details.get("firstName");
                    lastName = details.get("lastName");
                }
            }

            switch (operationType) {
                case "CREATE", "UPDATE" -> {
                    userSyncService.syncUser(userId, username, email, firstName, lastName);
                    log.info("Processed {} event for user {}", operationType, userId);
                }
                case "DELETE" -> {
                    userSyncService.deleteUser(userId);
                    log.info("Processed DELETE event for user {}", userId);
                }
                default -> log.debug("Unhandled operation type: {}", operationType);
            }

        } catch (Exception e) {
            log.error("Failed to process message: {}", message, e);
            throw new RuntimeException("Error processing Keycloak event", e);
        }
    }

    private String extractUserIdFromPath(String resourcePath) {
        if (resourcePath == null) return null;
        String[] parts = resourcePath.split("/");
        for (String part : parts) {
            if (part.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
                return part;
            }
        }
        return null;
    }
}