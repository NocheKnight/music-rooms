package ru.music.queue.service;

import ru.music.queue.dto.AddTrackRequest;
import ru.music.queue.dto.QueueDto;
import ru.music.queue.dto.TrackDto;
import ru.music.queue.model.Track;

import java.util.UUID;

public interface QueueService {
    TrackDto addTrack(UUID roomId, AddTrackRequest request, UUID userId);

    QueueDto getQueue(UUID roomId);

    TrackDto getCurrentTrack(UUID roomId);

    void moveTrack(UUID roomId, UUID trackId, int newPosition);

    void removeTrack(UUID roomId, UUID trackId);

    void clearQueue(UUID roomId);

    TrackDto next(UUID roomId);

    TrackDto previous(UUID roomId);
}