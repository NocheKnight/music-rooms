package ru.music.queue.service;

import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RoomValidationService {
    Mono<Boolean> validateRoomExists(UUID roomId);
}