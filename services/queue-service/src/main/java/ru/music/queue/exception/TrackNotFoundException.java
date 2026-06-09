package ru.music.queue.exception;

import java.util.UUID;

public class TrackNotFoundException extends RuntimeException {
    public TrackNotFoundException(UUID roomId, UUID trackId) {
        super("Track " + trackId + " not found in room " + roomId);
    }

    public TrackNotFoundException(String message) {
        super(message);
    }
}