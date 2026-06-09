package ru.music.queue.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.music.queue.dto.AddTrackRequest;
import ru.music.queue.dto.QueueResponse;
import ru.music.queue.dto.TrackResponse;
import ru.music.queue.exception.TrackNotFoundException;
import ru.music.queue.model.QueueItem;
import ru.music.queue.repository.QueueItemRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueService {

    private final QueueItemRepository queueItemRepository;
    private final RoomValidationService roomValidationService;

    @Transactional
    public TrackResponse addTrack(UUID roomId, AddTrackRequest request, UUID userId) {
        // Валидация комнаты
        roomValidationService.validateRoomExists(roomId).block();

        int position;
        if (request.getPosition() != null) {
            position = request.getPosition();
            // Сдвигаем треки, освобождая позицию
            queueItemRepository.shiftPositionsForward(roomId, position);
        } else {
            // Добавляем в конец очереди
            Integer maxPosition = queueItemRepository.findMaxPositionByRoomId(roomId);
            position = maxPosition != null ? maxPosition + 1 : 0;
        }

        QueueItem queueItem = QueueItem.builder()
                .roomId(roomId)
                .name(request.getName())
                .artist(request.getArtist())
                .durationSec(request.getDurationSec())
                .source(request.getSource())
                .externalId(request.getExternalId())
                .streamUrl(request.getStreamUrl())
                .position(position)
                .addedBy(userId)
                .build();

        QueueItem saved = queueItemRepository.save(queueItem);
        log.info("Track {} added to room {} at position {}", saved.getId(), roomId, position);

        return mapToTrackResponse(saved);
    }

    public QueueResponse getQueue(UUID roomId) {
        roomValidationService.validateRoomExists(roomId).block();

        List<QueueItem> tracks = queueItemRepository.findByRoomIdOrderByPositionAsc(roomId);

        List<TrackResponse> trackResponses = tracks.stream()
                .map(this::mapToTrackResponse)
                .collect(Collectors.toList());

        return QueueResponse.builder()
                .roomId(roomId)
                .totalTracks(trackResponses.size())
                .currentTrackPosition(trackResponses.isEmpty() ? null : 0)
                .tracks(trackResponses)
                .build();
    }

    public TrackResponse getCurrentTrack(UUID roomId) {
        roomValidationService.validateRoomExists(roomId).block();

        return queueItemRepository.findFirstByRoomIdOrderByPositionAsc(roomId)
                .map(this::mapToTrackResponse)
                .orElseThrow(() -> new TrackNotFoundException("No tracks in queue for room " + roomId));
    }

    @Transactional
    public TrackResponse moveTrack(UUID roomId, UUID trackId, int newPosition) {
        roomValidationService.validateRoomExists(roomId).block();

        QueueItem trackToMove = queueItemRepository.findByRoomIdAndId(roomId, trackId)
                .orElseThrow(() -> new TrackNotFoundException(roomId, trackId));

        int oldPosition = trackToMove.getPosition();

        if (oldPosition == newPosition) {
            return mapToTrackResponse(trackToMove);
        }

        // Валидация новой позиции
        Integer maxPosition = queueItemRepository.findMaxPositionByRoomId(roomId);
        if (newPosition < 0 || (maxPosition != null && newPosition > maxPosition)) {
            throw new IllegalArgumentException("Invalid position: " + newPosition +
                    ". Valid range: 0-" + (maxPosition != null ? maxPosition : 0));
        }

        if (newPosition < oldPosition) {
            // Перемещение вверх - сдвигаем треки между новой и старой позицией вниз
            queueItemRepository.shiftPositionsBetweenForward(roomId, newPosition, oldPosition);
        } else {
            // Перемещение вниз - сдвигаем треки между старой и новой позицией вверх
            queueItemRepository.shiftPositionsBetweenBackward(roomId, oldPosition, newPosition);
        }

        // Обновляем позицию перемещаемого трека
        trackToMove.setPosition(newPosition);
        QueueItem updated = queueItemRepository.save(trackToMove);

        log.info("Track {} moved from position {} to {} in room {}",
                trackId, oldPosition, newPosition, roomId);

        return mapToTrackResponse(updated);
    }

    @Transactional
    public void removeTrack(UUID roomId, UUID trackId) {
        roomValidationService.validateRoomExists(roomId).block();

        QueueItem trackToRemove = queueItemRepository.findByRoomIdAndId(roomId, trackId)
                .orElseThrow(() -> new TrackNotFoundException(roomId, trackId));

        int removedPosition = trackToRemove.getPosition();
        queueItemRepository.delete(trackToRemove);

        // Сдвигаем оставшиеся треки вверх
        queueItemRepository.shiftPositionsBackward(roomId, removedPosition);

        log.info("Track {} removed from room {} at position {}", trackId, roomId, removedPosition);
    }

    @Transactional
    public void clearQueue(UUID roomId) {
        roomValidationService.validateRoomExists(roomId).block();

        List<QueueItem> tracks = queueItemRepository.findByRoomIdOrderByPositionAsc(roomId);
        queueItemRepository.deleteAll(tracks);

        log.info("Queue for room {} cleared. Removed {} tracks", roomId, tracks.size());
    }

    private TrackResponse mapToTrackResponse(QueueItem item) {
        return TrackResponse.builder()
                .id(item.getId())
                .roomId(item.getRoomId())
                .name(item.getName())
                .artist(item.getArtist())
                .durationSec(item.getDurationSec())
                .source(item.getSource())
                .externalId(item.getExternalId())
                .streamUrl(item.getStreamUrl())
                .streamUrlExpiresAt(item.getStreamUrlExpiresAt())
                .position(item.getPosition())
                .addedBy(item.getAddedBy())
                .build();
    }
}