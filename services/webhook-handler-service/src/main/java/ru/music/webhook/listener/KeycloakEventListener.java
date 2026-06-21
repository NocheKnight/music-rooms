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

            // Определяем тип события по наличию ключей
            String userId = null;
            Map<String, String> details = null;
            String eventType = null;
            String operationType = null;

            // Проверяем на admin-событие (EventAdminNotificationMqMsg)
            if (eventData.containsKey("operationType")) {
                operationType = (String) eventData.get("operationType");
                String resourcePath = (String) eventData.get("resourcePath");
                String resourceType = (String) eventData.get("resourceTypeAsString");
                if (resourceType == null) {
                    resourceType = (String) eventData.get("resourceType");
                }
                // Извлекаем userId из resourcePath, если это USER
                if ("USER".equals(resourceType) && resourcePath != null) {
                    userId = extractUserIdFromPath(resourcePath);
                }
                eventType = operationType; // для логики используем operationType
                log.info("Parsed admin event: operationType={}, userId={}", operationType, userId);
            }
            // Проверяем на client-событие (EventClientNotificationMqMsg)
            else if (eventData.containsKey("type")) {
                eventType = (String) eventData.get("type");
                userId = (String) eventData.get("userId");
                details = (Map<String, String>) eventData.get("details");
                log.info("Parsed client event: type={}, userId={}", eventType, userId);
            }
            else {
                log.warn("Unknown event format, skipping: {}", eventData);
                return;
            }

            if (userId == null) {
                log.warn("Missing userId in event, skipping");
                return;
            }

            // Обработка по типу события
            if ("REGISTER".equals(eventType) || "UPDATE_PROFILE".equals(eventType) || "CREATE".equals(eventType) || "UPDATE".equals(eventType)) {
                // Для client-событий details уже извлечены, для admin-событий details может не быть
                String username = null;
                String email = null;
                String firstName = null;
                String lastName = null;
                if (details != null) {
                    username = details.get("username");
                    email = details.get("email");
                    firstName = details.get("firstName");
                    lastName = details.get("lastName");
                }
                userSyncService.syncUser(userId, username, email, firstName, lastName);
                log.info("Processed {} event for user {}", eventType, userId);
            }
            else if ("DELETE".equals(eventType) || "DELETE_ACCOUNT".equals(eventType)) {
                userSyncService.deleteUser(userId);
                log.info("Processed DELETE event for user {}", userId);
            }
            else {
                log.debug("Unhandled event type: {}", eventType);
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