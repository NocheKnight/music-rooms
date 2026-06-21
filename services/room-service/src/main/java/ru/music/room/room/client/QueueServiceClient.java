package ru.music.room.room.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(
        name = "queue-service",
        url = "${services.queue-service.url}"
)
public interface QueueServiceClient {
    @PostMapping("/api/queue/{roomId}")
    void createQueue(
            @PathVariable(name = "roomId") UUID roomId,
            @RequestHeader("X-User-Id") UUID userId
    );
}