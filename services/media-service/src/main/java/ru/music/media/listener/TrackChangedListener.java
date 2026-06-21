package ru.music.media.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ru.music.media.dto.TrackChangedEvent;
import ru.music.media.dto.TrackDto;
import ru.music.media.service.RoomSessionManager;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrackChangedListener {
    private final RoomSessionManager roomSessionManager;

    @RabbitListener(queues = "media-service.track.changed")
    public void onTrackChanged(TrackChangedEvent event) {
        if (event.getCurrentTrack() == null || event.getCurrentTrack().getStreamUrl() == null) return;

        UUID roomId = event.getRoomId();
        String streamUrl = event.getCurrentTrack().getStreamUrl();
        log.info("Received TrackChangedEvent for room={}, starting session", roomId);

        try {
            roomSessionManager.startSession(roomId, streamUrl);
        } catch (Exception e) {
            log.error("Failed to start session for room={}", roomId, e);
        }
    }
}