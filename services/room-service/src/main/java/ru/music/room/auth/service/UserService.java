package ru.music.room.auth.service;

import ru.music.room.auth.model.User;
import ru.music.room.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User getUserByKeycloakId(UUID keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found with keycloakId: " + keycloakId));
    }

    @Transactional(readOnly = true)
    public boolean userExists(UUID keycloakId) {
        return userRepository.existsByKeycloakId(keycloakId);
    }
}