package ru.music.media;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MediaController {

    private final RoomSessionManager roomSessionManager;

    // ── Клиентский эндпоинт ──────────────────────────────────────────

    @GetMapping("/stream/{roomId}")
    public ResponseEntity<StreamingResponseBody> stream(@PathVariable String roomId) {

        BroadcastSession session = roomSessionManager.getSession(roomId);

        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        StreamingResponseBody body = outputStream -> {
            session.subscribe(outputStream);
            try {
                session.awaitTermination();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                session.unsubscribe(outputStream);
                log.info("Client disconnected from room={}", roomId);
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "audio/mpeg")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .header(HttpHeaders.TRANSFER_ENCODING, "chunked")
                .header("X-Content-Type-Options", "nosniff")
                .body(body);
    }

    // ── Internal эндпоинты для queue-service ─────────────────────────

    @PostMapping("/internal/play")
    public ResponseEntity<Void> play(@RequestBody PlayRequest request) throws Exception {
        log.info("Play command: room={} url={}", request.roomId(), request.youtubeUrl());
        roomSessionManager.startSession(request.roomId(), request.youtubeUrl());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/internal/pause")
    public ResponseEntity<Void> pause(@RequestBody RoomRequest request) {
        log.info("Pause command: room={}", request.roomId());
        roomSessionManager.pauseSession(request.roomId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/internal/resume")
    public ResponseEntity<Void> resume(@RequestBody RoomRequest request) {
        log.info("Resume command: room={}", request.roomId());
        roomSessionManager.resumeSession(request.roomId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/internal/stop")
    public ResponseEntity<Void> stop(@RequestBody RoomRequest request) {
        log.info("Stop command: room={}", request.roomId());
        roomSessionManager.stopSession(request.roomId());
        return ResponseEntity.ok().build();
    }

    public record PlayRequest(String roomId, String youtubeUrl) {}
    public record RoomRequest(String roomId) {}
}