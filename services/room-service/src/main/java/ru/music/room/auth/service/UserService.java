package ru.music.room.auth.service;

import ru.music.room.auth.model.User;
import ru.music.room.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    // Новый метод: получить или создать пользователя
    @Transactional
    public User getOrCreateUser(UUID userId, String username, String email) {
        // Сначала пытаемся найти
        Optional<User> existing = userRepository.findById(userId);
        if (existing.isPresent()) {
            return existing.get();
        }
        // Если не нашли, создаём нового
        User newUser = new User();
        newUser.setId(userId);
        newUser.setUsername(username != null && !username.isBlank() ? username : userId.toString());
        newUser.setEmail(email != null && !email.isBlank() ? email : userId.toString() + "@temp.local");

        try {
            return userRepository.save(newUser);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // В параллельном потоке уже вставили такого пользователя – просто находим и возвращаем
            return userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Failed to retrieve user after duplicate exception", e));
        }
    }

    /**
     * Найти пользователя по ID.
     */
    @Transactional(readOnly = true)
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }

    /**
     * Найти пользователя по username.
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Найти пользователя по email.
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Создать или обновить пользователя (синхронизация с Auth Server).
     * Обычно вызывается после успешной аутентификации.
     */
    @Transactional
    public User createOrUpdateUser(UUID id, String username, String email) {
        User user = userRepository.findById(id).orElse(new User());
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        log.debug("Saving user: id={}, username={}, email={}", id, username, email);
        return userRepository.save(user);
    }

    /**
     * Проверить, существует ли пользователь.
     */
    @Transactional(readOnly = true)
    public boolean userExists(UUID userId) {
        return userRepository.existsById(userId);
    }
}