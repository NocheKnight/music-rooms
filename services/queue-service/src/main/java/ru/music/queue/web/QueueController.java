package ru.music.queue.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.music.queue.dto.AddTrackRequest;
import ru.music.queue.dto.MoveTrackRequest;
import ru.music.queue.dto.QueueDto;
import ru.music.queue.dto.TrackDto;
import ru.music.queue.service.QueueService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/queue/{roomId}")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

    @PostMapping("/tracks")
    public ResponseEntity<TrackDto> addTrack(
            @PathVariable(name = "roomId") UUID roomId,
            @Valid @RequestBody AddTrackRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        TrackDto track = queueService.addTrack(roomId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(track);
    }

    @GetMapping("/tracks")
    public ResponseEntity<QueueDto> getQueue(@PathVariable(name = "roomId") UUID roomId) {
        return ResponseEntity.ok(queueService.getQueue(roomId));
    }

    @GetMapping("/tracks/current")
    public ResponseEntity<TrackDto> getCurrentTrack(@PathVariable(name = "roomId") UUID roomId) {
        return ResponseEntity.ok(queueService.getCurrentTrack(roomId));
    }

    @PutMapping("/tracks/{trackId}/move")
    public ResponseEntity<TrackDto> moveTrack(
            @PathVariable(name = "roomId") UUID roomId,
            @PathVariable(name = "trackId") UUID trackId,
            @Valid @RequestBody MoveTrackRequest request) {
        queueService.moveTrack(roomId, trackId, request.getNewPosition());
        return ResponseEntity.ok().body(null);
    }

    @DeleteMapping("/tracks/{trackId}")
    public ResponseEntity<Map<String, String>> removeTrack(
            @PathVariable(name = "roomId") UUID roomId,
            @PathVariable(name = "trackId") UUID trackId) {
        queueService.removeTrack(roomId, trackId);
        return ResponseEntity.ok(Map.of("message", "Track removed successfully"));
    }

    @DeleteMapping("/tracks")
    public ResponseEntity<Map<String, String>> clearQueue(@PathVariable(name = "roomId") UUID roomId) {
        queueService.clearQueue(roomId);
        return ResponseEntity.ok(Map.of("message", "Queue cleared successfully"));
    }

    @PatchMapping("/tracks/next")
    public ResponseEntity<Map<String, String>> next(@PathVariable(name = "roomId") UUID roomId) {
        queueService.next(roomId);
        return ResponseEntity.ok(Map.of("message", "Queue cleared successfully"));
    }

    @PatchMapping("/tracks/previous")
    public ResponseEntity<Map<String, String>> previous(@PathVariable(name = "roomId") UUID roomId) {
        queueService.previous(roomId);
        return ResponseEntity.ok(Map.of("message", "Queue cleared successfully"));
    }
}