package ru.music.media.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
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
    
    public void startSession(UUID roomId, String youtubeUrl) throws Exception {
        stopSession(roomId);

        Runnable onFinished = () -> queueServiceClient.nextTrack(roomId);

        var audioStream = audioStreamService.getAudioStream(youtubeUrl);
        var session = new BroadcastSession(audioStream.inputStream(), audioStream.ffmpegProcess(), onFinished);

        sessions.put(roomId, session);
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
