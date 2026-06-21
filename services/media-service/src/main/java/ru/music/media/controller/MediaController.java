package ru.music.media.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import ru.music.media.dto.PlayRequest;
import ru.music.media.dto.RoomRequest;
import ru.music.media.entity.TrackMeta;
import ru.music.media.feign.QueueServiceClient;
import ru.music.media.service.AudioStreamService;
import ru.music.media.service.BroadcastSession;
import ru.music.media.service.RoomSessionManager;
import ru.music.media.dto.AddTrackRequest;
import ru.music.media.dto.TrackDto;
import ru.music.media.model.TrackSource;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/media")
public class MediaController {

    private final RoomSessionManager roomSessionManager;
    private final AudioStreamService audioStreamService;
    private final QueueServiceClient queueServiceClient;


    @GetMapping("/stream/{roomId}")
    public ResponseEntity<StreamingResponseBody> stream(@PathVariable UUID roomId) {

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

    @GetMapping("/stream/{roomId}/meta")
    public ResponseEntity<Map<String, Object>> getMeta(@PathVariable UUID roomId) {
        BroadcastSession session = roomSessionManager.getSession(roomId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        TrackMeta meta = session.getTrackMeta();
        return ResponseEntity.ok(Map.of(
                "title", meta.title(),
                "durationSeconds", meta.durationSeconds(),
                "positionSeconds", session.getPositionSeconds()
        ));
    }

    @PostMapping("/parse")
    public ResponseEntity<TrackDto> parseAndAdd(
            @RequestParam String youtubeUrl,
            @RequestParam UUID roomId,
            @RequestHeader("X-User-Id") UUID userId
    ) throws Exception {
        TrackMeta meta = audioStreamService.getTrackMeta(youtubeUrl);

        AddTrackRequest request = new AddTrackRequest();
        request.setName(meta.title());
        request.setArtist("Unknown");
        request.setDurationSec((int) meta.durationSeconds());
        request.setSource(TrackSource.YOUTUBE);
        request.setStreamUrl(youtubeUrl);

        TrackDto track = queueServiceClient.addTrack(roomId, request, userId);
        return ResponseEntity.ok(track);
    }


    @PostMapping("/internal/play")
    public ResponseEntity<Void> play(@RequestBody PlayRequest request) throws Exception {
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
}