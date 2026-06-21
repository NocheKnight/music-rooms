package ru.music.webhook.listener;

import com.fasterxml.jackson.core.type.TypeReference;
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

            // Пытаемся определить тип события
            String eventType = (String) eventData.get("type"); // для EventClientNotificationMqMsg
            String operationType = (String) eventData.get("operationType"); // для EventAdminNotificationMqMsg

            // Извлекаем userId (может быть в поле userId или из resourcePath)
            String userId = (String) eventData.get("userId");
            String resourcePath = (String) eventData.get("resourcePath");
            if (userId == null && resourcePath != null) {
                userId = extractUserIdFromPath(resourcePath);
            }

            if (userId == null) {
                log.warn("Missing userId in event, skipping");
                return;
            }

            // Извлекаем данные пользователя
            String username = null;
            String email = null;
            String firstName = null;
            String lastName = null;

            // Пробуем получить данные из representation (для EventAdminNotificationMqMsg)
            String representationJson = (String) eventData.get("representation");
            if (representationJson != null) {
                try {
                    Map<String, String> userData = objectMapper.readValue(representationJson, new TypeReference<>() {});
                    username = userData.get("username");
                    email = userData.get("email");
                    firstName = userData.get("firstName");
                    lastName = userData.get("lastName");
                } catch (Exception e) {
                    log.warn("Failed to parse representation: {}", e.getMessage());
                }
            }

            // Если не нашли в representation, пробуем из details (для EventClientNotificationMqMsg)
            if (username == null) {
                Map<String, String> details = (Map<String, String>) eventData.get("details");
                if (details != null) {
                    username = details.get("username");
                    email = details.get("email");
                    firstName = details.get("firstName");
                    lastName = details.get("lastName");
                }
            }

            // Определяем тип операции
            String opType = operationType != null ? operationType : eventType;
            // Для событий типа "REGISTER", "CREATE", "UPDATE" – синхронизация
            if ("CREATE".equals(opType) || "REGISTER".equals(opType) || "UPDATE".equals(opType)) {
                userSyncService.syncUser(userId, username, email, firstName, lastName);
                log.info("Processed {} event for user {}", opType, userId);
            } else if ("DELETE".equals(opType) || "DELETE_ACCOUNT".equals(opType)) {
                userSyncService.deleteUser(userId);
                log.info("Processed DELETE event for user {}", userId);
            } else {
                log.debug("Unhandled event type: {}", opType);
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