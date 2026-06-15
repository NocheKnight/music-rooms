package ru.music.media;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomSessionManager {
    private final AudioStreamService audioStreamService;
    private final ConcurrentHashMap<String, BroadcastSession> sessions = new ConcurrentHashMap<>();
    private final QueueServiceClient queueServiceClient;
    
    public void startSession(String roomId, String youtubeUrl) throws Exception {
        stopSession(roomId);

        Runnable onFinished = () -> queueServiceClient.notifyTrackFinished(roomId);

        var audioStream = audioStreamService.getAudioStream(youtubeUrl);
        var session = new BroadcastSession(audioStream.inputStream(), audioStream.ffmpegProcess(), onFinished);

        sessions.put(roomId, session);
    }

    public void stopSession(String roomId) {
        BroadcastSession session = sessions.remove(roomId);
        if (session != null) {
            log.info("Stopping session for room={}", roomId);
            session.stop();
        }
    }

    public void pauseSession(String roomId) {
        BroadcastSession session = sessions.get(roomId);
        if (session == null) {
            throw new IllegalStateException("No active session for room: " + roomId);
        }
        session.pause();
    }

    public void resumeSession(String roomId) {
        BroadcastSession session = sessions.get(roomId);
        if (session == null) {
            throw new IllegalStateException("No active session for room: " + roomId);
        }
        session.resume();
    }

    public BroadcastSession getSession(String roomId) {
        return sessions.get(roomId);
    }

    public boolean hasSession(String roomId) {
        return sessions.containsKey(roomId);
    }
}
