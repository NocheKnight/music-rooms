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
public class TrackResponse {
    private UUID id;
    private UUID roomId;
    private String name;
    private String artist;
    private Integer durationSec;
    private TrackSource source;
    private String externalId;
    private String streamUrl;
    private Instant streamUrlExpiresAt;
    private Integer position;
    private UUID addedBy;
    private Instant createdAt;
    private Boolean isCurrent;
}