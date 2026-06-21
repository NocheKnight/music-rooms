package ru.music.queue.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal UUID userId) {
        TrackDto track = queueService.addTrack(roomId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(track);
    }

    @GetMapping("/tracks/current")
    public ResponseEntity<TrackDto> getCurrentTrack(
            @PathVariable(name = "roomId") UUID roomId,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(queueService.getCurrentTrack(roomId));
    }

    @PutMapping("/tracks/{trackId}/move")
    public ResponseEntity<TrackDto> moveTrack(
            @PathVariable(name = "roomId") UUID roomId,
            @PathVariable(name = "trackId") UUID trackId,
            @Valid @RequestBody MoveTrackRequest request,
            @AuthenticationPrincipal UUID userId) {
        queueService.moveTrack(roomId, trackId, request.getNewPosition());
        return ResponseEntity.ok().body(null);
    }

    @DeleteMapping("/tracks/{trackId}")
    public ResponseEntity<Map<String, String>> removeTrack(
            @PathVariable(name = "roomId") UUID roomId,
            @PathVariable(name = "trackId") UUID trackId,
            @AuthenticationPrincipal UUID userId) {
        queueService.removeTrack(roomId, trackId);
        return ResponseEntity.ok(Map.of("message", "Track removed successfully"));
    }

    @PatchMapping("/tracks/next")
    public ResponseEntity<Map<String, String>> next(
            @PathVariable(name = "roomId") UUID roomId,
            @AuthenticationPrincipal UUID userId) {
        queueService.next(roomId);
        return ResponseEntity.ok(Map.of("message", "Queue cleared successfully"));
    }

    @PatchMapping("/tracks/previous")
    public ResponseEntity<Map<String, String>> previous(
            @PathVariable(name = "roomId") UUID roomId,
            @AuthenticationPrincipal UUID userId) {
        queueService.previous(roomId);
        return ResponseEntity.ok(Map.of("message", "Queue cleared successfully"));
    }

    @PostMapping
    public ResponseEntity<QueueDto> createQueue(
            @PathVariable(name = "roomId") UUID roomId,
            @AuthenticationPrincipal UUID userId) {
        QueueDto queue = queueService.createQueue(roomId);
        return ResponseEntity.status(HttpStatus.CREATED).body(queue);
    }

    @GetMapping
    public ResponseEntity<QueueDto> getQueue(
            @PathVariable(name = "roomId") UUID roomId,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(queueService.getQueue(roomId));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> clearQueue(
            @PathVariable(name = "roomId") UUID roomId,
            @AuthenticationPrincipal UUID userId) {
        queueService.clearQueue(roomId);
        return ResponseEntity.ok(Map.of("message", "Queue cleared successfully"));
    }
}