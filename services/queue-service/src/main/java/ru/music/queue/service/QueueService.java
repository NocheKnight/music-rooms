package ru.music.queue.service;

import ru.music.queue.dto.AddTrackRequest;
import ru.music.queue.dto.QueueResponse;
import ru.music.queue.dto.TrackResponse;
import java.util.UUID;

public interface QueueService {
    TrackResponse addTrack(UUID roomId, AddTrackRequest request);

    QueueResponse getQueue(UUID roomId);

    TrackResponse getCurrentTrack(UUID roomId);

    TrackResponse moveTrack(UUID roomId, UUID trackId, int newPosition);

    void removeTrack(UUID roomId, UUID trackId);

    void clearQueue(UUID roomId);

    void next(UUID roomId);

    void previous(UUID roomId);
}