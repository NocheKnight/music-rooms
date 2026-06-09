package ru.music.queue.exception;

import java.util.UUID;

public class RoomNotFoundException extends RuntimeException {
    public RoomNotFoundException(UUID roomId) {
        super("Room not found: " + roomId);
    }

    public RoomNotFoundException(String message) {
        super(message);
    }
}