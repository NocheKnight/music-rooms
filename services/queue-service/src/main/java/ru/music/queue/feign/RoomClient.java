package ru.music.queue.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.music.queue.dto.RoomResponse;

import java.util.UUID;

@FeignClient(
        name = "rooms",
        url = "${services.room-service.url}"
)
public interface RoomClient {
    @GetMapping("api/rooms/{roomId}")
    ResponseEntity<RoomResponse> getRoom(@PathVariable(name = "roomId") UUID roomId);
}