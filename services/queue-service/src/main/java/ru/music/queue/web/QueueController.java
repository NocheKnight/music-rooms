package ru.music.queue.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.music.queue.dto.AddTrackRequest;
import ru.music.queue.dto.MoveTrackRequest;
import ru.music.queue.dto.QueueResponse;
import ru.music.queue.dto.TrackResponse;
import ru.music.queue.service.QueueService;
import ru.music.queue.service.implementations.DefaultQueueService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/queue/{roomId}")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

    @PostMapping("/tracks")
    public ResponseEntity<TrackResponse> addTrack(
            @PathVariable(name = "roomId") UUID roomId,
            @Valid @RequestBody AddTrackRequest request) {
        TrackResponse track = queueService.addTrack(roomId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(track);
    }

    @GetMapping("/tracks")
    public ResponseEntity<QueueResponse> getQueue(@PathVariable(name = "roomId") UUID roomId) {
        return ResponseEntity.ok(queueService.getQueue(roomId));
    }

    @GetMapping("/tracks/current")
    public ResponseEntity<TrackResponse> getCurrentTrack(@PathVariable(name = "roomId") UUID roomId) {
        return ResponseEntity.ok(queueService.getCurrentTrack(roomId));
    }

    @PutMapping("/tracks/{trackId}/move")
    public ResponseEntity<TrackResponse> moveTrack(
            @PathVariable(name = "roomId") UUID roomId,
            @PathVariable(name = "trackId") UUID trackId,
            @Valid @RequestBody MoveTrackRequest request) {
        return ResponseEntity.ok(queueService.moveTrack(roomId, trackId, request.getNewPosition()));
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
}