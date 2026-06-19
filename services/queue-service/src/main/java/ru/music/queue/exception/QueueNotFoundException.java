package ru.music.queue.exception;

import java.util.UUID;

public class QueueNotFoundException extends RuntimeException {
    public QueueNotFoundException(UUID roomId) {
        super("Queue not found: " + roomId);
    }

    public QueueNotFoundException(String message) {
        super(message);
    }
}