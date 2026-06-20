package ru.music.webhook.listener;

import ru.music.webhook.config.RabbitMQConfig;
import ru.music.webhook.dto.KeycloakEventDto;
import ru.music.webhook.service.UserSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakEventListener {

    private final UserSyncService userSyncService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void handleEvent(KeycloakEventDto event) {
        log.info("Received Keycloak event: type={}, userId={}", event.eventType(), event.userId());

        try {
            switch (event.eventType()) {
                case "REGISTER":
                    userSyncService.handleRegister(event);
                    break;
                case "UPDATE_PROFILE":
                    userSyncService.handleUpdateProfile(event);
                    break;
                case "DELETE_ACCOUNT":
                    userSyncService.handleDeleteAccount(event);
                    break;
                default:
                    userSyncService.handleOtherEvent(event);
                    break;
            }
        } catch (Exception e) {
            log.error("Error processing event {}: {}", event.id(), e.getMessage(), e);
            // В зависимости от требований можно выбросить исключение для повторной попытки
            throw e;
        }
    }
}