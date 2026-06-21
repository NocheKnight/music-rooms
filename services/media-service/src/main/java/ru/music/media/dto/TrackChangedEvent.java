package ru.music.media.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrackChangedEvent {
    private UUID roomId;
    private TrackDto currentTrack;
}