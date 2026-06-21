package ru.music.media.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import ru.music.media.dto.SessionStartedEvent;
import ru.music.media.entity.TrackMeta;
import ru.music.media.feign.QueueServiceClient;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomSessionManager {
    private final AudioStreamService audioStreamService;
    private final ConcurrentHashMap<UUID, BroadcastSession> sessions = new ConcurrentHashMap<>();
    private final QueueServiceClient queueServiceClient;
    private final RabbitTemplate rabbitTemplate;

    public void startSession(UUID roomId, String youtubeUrl) throws Exception {
        stopSession(roomId);
        Runnable onFinished = () -> queueServiceClient.nextTrack(roomId);
        TrackMeta meta = audioStreamService.getTrackMeta(youtubeUrl);
        var audioStream = audioStreamService.getAudioStream(youtubeUrl);
        var session = new BroadcastSession(audioStream.inputStream(), audioStream.ffmpegProcess(), onFinished, meta);
        sessions.put(roomId, session);
        log.info("Session started for room={}", roomId);

        rabbitTemplate.convertAndSend("amq.topic", "room." + roomId + ".session.started", new SessionStartedEvent(roomId));
    }

    public void stopSession(UUID roomId) {
        BroadcastSession session = sessions.remove(roomId);
        if (session != null) {
            log.info("Stopping session for room={}", roomId);
            session.stop();
        }
    }

    public void pauseSession(UUID roomId) {
        BroadcastSession session = sessions.get(roomId);
        if (session == null) {
            throw new IllegalStateException("No active session for room: " + roomId);
        }
        session.pause();
    }

    public void resumeSession(UUID roomId) {
        BroadcastSession session = sessions.get(roomId);
        if (session == null) {
            throw new IllegalStateException("No active session for room: " + roomId);
        }
        session.resume();
    }

    public BroadcastSession getSession(UUID roomId) {
        return sessions.get(roomId);
    }

    public boolean hasSession(UUID roomId) {
        return sessions.containsKey(roomId);
    }
}
