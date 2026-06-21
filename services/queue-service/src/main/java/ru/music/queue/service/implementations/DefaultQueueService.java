package ru.music.queue.service.implementations;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.music.queue.config.RabbitMqConfig;
import ru.music.queue.mapper.QueueMapper;
import ru.music.queue.mapper.TrackMapper;
import ru.music.queue.dto.*;
import ru.music.queue.exception.QueueNotFoundException;
import ru.music.queue.exception.RoomNotFoundException;
import ru.music.queue.exception.TrackNotFoundException;
import ru.music.queue.feign.RoomServiceClient;
import ru.music.queue.model.Queue;
import ru.music.queue.model.Track;
import ru.music.queue.repository.QueueRepository;
import ru.music.queue.repository.TrackRepository;
import ru.music.queue.service.QueueService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultQueueService implements QueueService {

    private final TrackRepository trackRepository;
    private final QueueRepository queueRepository;

    private final TrackMapper trackMapper;
    private final QueueMapper queueMapper;

    private final RoomServiceClient roomServiceClient;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public TrackDto addTrack(UUID roomId, AddTrackRequest request, UUID userId) {
        ResponseEntity<RoomResponse> roomRe = roomServiceClient.getRoom(roomId);
        if (roomRe.getStatusCode() != HttpStatus.OK) {
            throw new RoomNotFoundException(roomId);
        }

        Queue queue = queueRepository.findByRoomId(roomId)
                .orElseThrow(() -> new QueueNotFoundException(roomId));
        Track track = trackMapper.addTrackRequestToEntity(request, userId, queue);

        List<Track> queueTrack = queue.getTracks();
        if (queueTrack.isEmpty()) {
            queue.setCurrentTrackPosition(0);
        }

        int position = request.getPosition() == null ?
                queueTrack.size() :
                Math.clamp(request.getPosition(), 0, queueTrack.size());
        queueTrack.add(position, track);

        log.info("Track id before save = {}", track.getId());
        Track savedTrack = trackRepository.save(track);
        Queue savedQueue = queueRepository.save(queue);
        log.info("Track {} added to room {} at position {}", savedTrack.getId(), roomId, position);

        return trackMapper.entityToDto(savedTrack, position);
    }

    @Override
    @Transactional
    public TrackDto getCurrentTrack(UUID roomId) {
        ResponseEntity<RoomResponse> roomRe = roomServiceClient.getRoom(roomId);
        if (roomRe.getStatusCode() != HttpStatus.OK) {
            throw new RoomNotFoundException(roomId);
        }

        Queue queue = queueRepository.findByRoomId(roomId)
                .orElseThrow(() -> new QueueNotFoundException(roomId));

        if (queue.getTracks().isEmpty()) {
            throw new TrackNotFoundException("Queue is empty");
        }
        Track currentTrack = queue.getTracks().get(queue.getCurrentTrackPosition());

        return trackMapper.entityToDto(currentTrack, queue.getTracks().indexOf(currentTrack));
    }

    @Override
    @Transactional
    public void moveTrack(UUID roomId, UUID trackId, int newPosition) {
        ResponseEntity<RoomResponse> roomRe = roomServiceClient.getRoom(roomId);
        if (roomRe.getStatusCode() != HttpStatus.OK) {
            throw new RoomNotFoundException(roomId);
        }

        Queue queue = queueRepository.findByRoomId(roomId)
                .orElseThrow(() -> new QueueNotFoundException(roomId));
        Track trackToMove = trackRepository.findById(trackId)
                .orElseThrow(() -> new TrackNotFoundException(roomId, trackId));

        int oldPosition = queue.getTracks().indexOf(trackToMove);
        if (oldPosition == newPosition) {
            return;
        }

        if (newPosition < 0 || newPosition > queue.getTracks().size()) {
            throw new IllegalArgumentException("Invalid position: " + newPosition +
                    ". Valid range: 0-" + (queue.getTracks().size()));
        }

        queue.getTracks().remove(trackToMove);
        queue.getTracks().add(newPosition, trackToMove);
        queueRepository.save(queue);

        log.info("Track {} moved from position {} to {} in room {}",
                trackId, oldPosition, newPosition, roomId);
    }

    @Override
    @Transactional
    public void removeTrack(UUID roomId, UUID trackId) {
        ResponseEntity<RoomResponse> roomRe = roomServiceClient.getRoom(roomId);
        if (roomRe.getStatusCode() != HttpStatus.OK) {
            throw new RoomNotFoundException(roomId);
        }

        Queue queue = queueRepository.findByRoomId(roomId)
                .orElseThrow(() -> new QueueNotFoundException(roomId));
        Track trackToRemove = trackRepository.findById(trackId)
                .orElseThrow(() -> new TrackNotFoundException(roomId, trackId));

        queue.getTracks().remove(trackToRemove);
        queueRepository.save(queue);

        log.info("Track {} removed from room {}", trackId, roomId);
    }

    @Override
    @Transactional
    public TrackDto next(UUID roomId) {
        ResponseEntity<RoomResponse> roomRe = roomServiceClient.getRoom(roomId);
        if (roomRe.getStatusCode() != HttpStatus.OK) {
            throw new RoomNotFoundException(roomId);
        }

        Queue queue = queueRepository.findByRoomId(roomId)
                .orElseThrow(() -> new QueueNotFoundException(roomId));

        int newTrackPosition = queue.getCurrentTrackPosition() == queue.getTracks().size() - 1 ?
                0 : queue.getCurrentTrackPosition() + 1;
        queue.setCurrentTrackPosition(newTrackPosition);

        Track newTrack = queue.getTracks().get(queue.getCurrentTrackPosition());

        publishTrackChangedEvent(queue, newTrack);
        return trackMapper.entityToDto(newTrack, queue.getTracks().indexOf(newTrack));
    }

    @Override
    @Transactional
    public TrackDto previous(UUID roomId) {
        ResponseEntity<RoomResponse> roomRe = roomServiceClient.getRoom(roomId);
        if (roomRe.getStatusCode() != HttpStatus.OK) {
            throw new RoomNotFoundException(roomId);
        }

        Queue queue = queueRepository.findByRoomId(roomId)
                .orElseThrow(() -> new QueueNotFoundException(roomId));

        int newTrackPosition = queue.getCurrentTrackPosition() == 0 ?
                0 : queue.getCurrentTrackPosition() - 1;
        queue.setCurrentTrackPosition(newTrackPosition);

        Track newTrack = queue.getTracks().get(queue.getCurrentTrackPosition());

        publishTrackChangedEvent(queue, newTrack);
        return trackMapper.entityToDto(newTrack, queue.getTracks().indexOf(newTrack));
    }

    @Override
    @Transactional
    public QueueDto createQueue(UUID roomId) {
        ResponseEntity<RoomResponse> roomRe = roomServiceClient.getRoom(roomId);
        if (roomRe.getStatusCode() != HttpStatus.OK) {
            throw new RoomNotFoundException(roomId);
        }

        Queue queue = Queue.builder()
                .roomId(roomId)
                .build();

        Queue created = queueRepository.save(queue);
        log.info("Queue created in room {}", roomId);

        return queueMapper.entityToDto(created);
    }

    @Override
    @Transactional
    public QueueDto getQueue(UUID roomId) {
        ResponseEntity<RoomResponse> roomRe = roomServiceClient.getRoom(roomId);
        if (roomRe.getStatusCode() != HttpStatus.OK) {
            throw new RoomNotFoundException(roomId);
        }

        Queue queue = queueRepository.findByRoomId(roomId)
                .orElseThrow(() -> new QueueNotFoundException(roomId));

        return queueMapper.entityToDto(queue);
    }

    @Override
    @Transactional
    public void clearQueue(UUID roomId) {
        ResponseEntity<RoomResponse> roomRe = roomServiceClient.getRoom(roomId);
        if (roomRe.getStatusCode() != HttpStatus.OK) {
            throw new RoomNotFoundException(roomId);
        }

        Queue queue = queueRepository.findByRoomId(roomId)
                .orElseThrow(() -> new QueueNotFoundException(roomId));
        int queueSize = queue.getTracks().size();

        queue.getTracks().clear();
        queue.setCurrentTrackPosition(null);
        queueRepository.save(queue);

        log.info("Queue for room {} cleared. Removed {} tracks", roomId, queueSize);
    }

    private void publishTrackChangedEvent(Queue queue, Track track) {
        TrackDto trackInfo = trackMapper.entityToDto(track, queue.getTracks().indexOf(track));
        TrackChangedEvent event = new TrackChangedEvent(trackInfo);

        String routingKey = "room." + queue.getRoomId() + ".track.changed";

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.TRACK_CHANGED_EXCHANGE,
                routingKey,
                event
        );
        log.info("Published TrackChangedEvent for room {} to STOMP topic", queue.getRoomId());
    }
}