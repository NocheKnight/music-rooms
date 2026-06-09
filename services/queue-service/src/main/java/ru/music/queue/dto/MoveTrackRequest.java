package ru.music.queue.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MoveTrackRequest {
    @NotNull(message = "New position is required")
    @Min(value = 0, message = "Position must be non-negative")
    private Integer newPosition;
}