package ru.music.common.security;

import java.util.UUID;

public interface UserService<T> {
    T getUserByKeycloakId(UUID keycloakId);
    boolean userExists(UUID keycloakId);
}