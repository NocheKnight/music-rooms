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
public class QueueResponse {
    private UUID roomId;
    private int totalTracks;
    private Integer currentTrackPosition;
    private List<TrackResponse> tracks;
}