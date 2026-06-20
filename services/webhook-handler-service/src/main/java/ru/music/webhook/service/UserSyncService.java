package ru.music.webhook.service;

import ru.music.webhook.entity.User;
import ru.music.webhook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSyncService {

    private final UserRepository userRepository;

    @Transactional
    public void syncUser(String keycloakId, String username, String email, String firstName, String lastName) {
        if (keycloakId == null) {
            log.warn("Cannot sync user: keycloakId is null");
            return;
        }
        UUID keycloakUuid = UUID.fromString(keycloakId);

        Optional<User> existingUser = userRepository.findByKeycloakId(keycloakUuid);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (username != null) user.setUsername(username);
            if (email != null) user.setEmail(email);
            if (firstName != null) user.setFirstName(firstName);
            if (lastName != null) user.setLastName(lastName);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            log.info("Updated user: {} ({})", username, keycloakId);
        } else {
            User newUser = User.builder()
                    .keycloakId(keycloakUuid)
                    .username(username != null ? username : "user_" + keycloakId.substring(0, 8))
                    .email(email != null ? email : "no-email@example.com")
                    .firstName(firstName)
                    .lastName(lastName)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            userRepository.save(newUser);
            log.info("Created new user: {} ({})", username, keycloakId);
        }
    }

    @Transactional
    public void deleteUser(String keycloakId) {
        if (keycloakId == null) {
            log.warn("Cannot delete user: keycloakId is null");
            return;
        }
        UUID keycloakUuid = UUID.fromString(keycloakId);
        Optional<User> user = userRepository.findByKeycloakId(keycloakUuid);
        if (user.isPresent()) {
            userRepository.softDeleteByKeycloakId(keycloakUuid);
            log.info("Soft-deleted user: {}", keycloakId);
        } else {
            log.warn("User with keycloakId {} not found for deletion", keycloakId);
        }
    }
}