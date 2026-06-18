package ru.music.queue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueueDto {
    private UUID id;
    private UUID roomId;
    private List<TrackDto> tracks;
    private Integer currentTrackPosition;
}