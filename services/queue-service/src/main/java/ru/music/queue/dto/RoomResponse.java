package ru.music.queue.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record RoomResponse(
        UUID id, String name, String inviteCode, UUID createdBy,
        Set<UserResponse> participants, Instant createdAt
) {}