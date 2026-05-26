package ru.music.room.room.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinRoomRequest(
        @NotBlank String inviteCode
) {}