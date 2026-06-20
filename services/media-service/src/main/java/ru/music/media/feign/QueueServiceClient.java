package ru.music.media.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "queues",
        url = "${services.queue-service.url}"
)
public interface QueueServiceClient {
    @PatchMapping("/tracks/next")
    void nextTrack(@PathVariable(name = "roomId") UUID roomId);
}
