package ru.music.webhook.service;

import ru.music.webhook.dto.KeycloakEventDto;
import ru.music.webhook.entity.User;
import ru.music.webhook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSyncService {

    private final UserRepository userRepository;

    @Transactional
    public void handleRegister(KeycloakEventDto event) {
        String keycloakId = event.userId();
        if (userRepository.existsByKeycloakId(keycloakId)) {
            log.info("User with keycloakId {} already exists, skipping registration", keycloakId);
            return;
        }

        User user = User.builder()
                .keycloakId(keycloakId)
                .username(event.getUsername() != null ? event.getUsername() : "user_" + keycloakId.substring(0, 8))
                .email(event.getEmail() != null ? event.getEmail() : "no-email@example.com")
                .firstName(event.getFirstName())
                .lastName(event.getLastName())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
        log.info("User registered: {} ({})", user.getUsername(), keycloakId);
    }

    @Transactional
    public void handleUpdateProfile(KeycloakEventDto event) {
        String keycloakId = event.userId();
        Optional<User> optionalUser = userRepository.findByKeycloakId(keycloakId);
        if (optionalUser.isEmpty()) {
            log.warn("User with keycloakId {} not found for update, creating new record", keycloakId);
            // Можно создать или просто пропустить
            handleRegister(event);
            return;
        }

        User user = optionalUser.get();
        // Обновляем только те поля, которые пришли
        if (event.getUsername() != null) {
            user.setUsername(event.getUsername());
        }
        if (event.getEmail() != null) {
            user.setEmail(event.getEmail());
        }
        if (event.getFirstName() != null) {
            user.setFirstName(event.getFirstName());
        }
        if (event.getLastName() != null) {
            user.setLastName(event.getLastName());
        }
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
        log.info("User profile updated: {} ({})", user.getUsername(), keycloakId);
    }

    @Transactional
    public void handleDeleteAccount(KeycloakEventDto event) {
        String keycloakId = event.userId();
        if (!userRepository.existsByKeycloakId(keycloakId)) {
            log.warn("User with keycloakId {} not found for deletion", keycloakId);
            return;
        }
        userRepository.softDeleteByKeycloakId(keycloakId);
        log.info("User soft-deleted: {}", keycloakId);
    }

    @Transactional
    public void handleOtherEvent(KeycloakEventDto event) {
        log.debug("Unhandled event type: {} for user {}", event.eventType(), event.userId());
    }
}