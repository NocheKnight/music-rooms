package ru.music.media.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.music.media.config.FeignConfig;
import ru.music.media.dto.AddTrackRequest;
import ru.music.media.dto.TrackDto;

import java.util.UUID;

@FeignClient(
        name = "queues",
        url = "${services.queue-service.url}",
        configuration = FeignConfig.class
)
public interface QueueServiceClient {
    @PutMapping("/api/queue/{roomId}/tracks/next")
    void nextTrack(@PathVariable(name = "roomId") UUID roomId);

    @PostMapping("/api/queue/{roomId}/tracks")
    TrackDto addTrack(
            @PathVariable(name = "roomId") UUID roomId,
            @RequestBody AddTrackRequest request,
            @RequestHeader("X-User-Id") UUID userId
    );
}
