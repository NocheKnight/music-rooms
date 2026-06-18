package ru.music.queue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.music.queue.model.TrackSource;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackDto {
    private UUID id;
    private String name;
    private String artist;
    private Integer durationSec;
    private TrackSource source;
    private String streamUrl;
    private Instant streamUrlExpiresAt;
    private UUID addedBy;
    private Integer position;
}