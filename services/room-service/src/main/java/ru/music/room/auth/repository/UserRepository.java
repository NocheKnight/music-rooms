package ru.music.room.auth.repository;

import ru.music.room.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByKeycloakId(UUID keycloakId);
    boolean existsByKeycloakId(UUID keycloakId);
}