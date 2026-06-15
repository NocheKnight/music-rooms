package ru.music.queue.service.implementations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.music.queue.exception.RoomNotFoundException;
import ru.music.queue.service.RoomValidationService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultRoomValidationService implements RoomValidationService {

    private final WebClient.Builder webClientBuilder;

    @Value("${services.room-service.url}")
    private String roomServiceUrl;

    public Mono<Boolean> validateRoomExists(UUID roomId) {
        return webClientBuilder.build()
                .get()
                .uri(roomServiceUrl + "/api/rooms/{roomId}", roomId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(response -> true)
                .onErrorResume(e -> {
                    log.error("Room validation failed for room {}: {}", roomId, e.getMessage());
                    return Mono.error(new RoomNotFoundException(roomId));
                });
    }
}