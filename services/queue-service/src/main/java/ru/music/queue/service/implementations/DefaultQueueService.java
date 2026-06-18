package ru.music.queue.service.implementations;

import jakarta.transaction.Transactional;
import jdk.jshell.spi.ExecutionControl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
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
import ru.music.queue.feign.RoomClient;
import ru.music.queue.model.Queue;
import ru.music.queue.model.Track;
import ru.music.queue.repository.QueueRepository;
import ru.music.queue.repository.TrackRepository;
import ru.music.queue.service.QueueService;

import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultQueueService implements QueueService {

    private final TrackRepository trackRepository;
    private final QueueRepository queueRepository;

    private final TrackMapper trackMapper;
    private final QueueMapper queueMapper;

    private final RoomClient roomClient;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public TrackDto addTrack(UUID roomId, AddTrackRequest request, UUID userId) {
        ResponseEntity<RoomResponse> roomRe = roomClient.getRoom(roomId);
        if (roomRe.getStatusCode() != HttpStatus.OK) {
            throw new RoomNotFoundException(roomId);
        }

        Queue queue = queueRepository.findByRoomId(roomId)
                .orElseThrow(() -> new QueueNotFoundException(roomId));
        Track track = trackMapper.addTrackRequestToEntity(request, userId);

        int position = request.getPosition() != null ?
                request.getPosition() : queue.getTracks().size();
        queue.getTracks().add(position, track);

        if (position == 0) {
            queue.setCurrentTrackPosition(0);
        }

        Track saved = trackRepository.save(track);
        log.info("Track {} added to room {} at position {}", saved.getId(), roomId, position);

        return trackMapper.entityToDto(saved, position);
    }

    public QueueDto getQueue(UUID roomId) {
        ResponseEntity<RoomResponse> roomRe = roomClient.getRoom(roomId);
        if (roomRe.getStatusCode() != HttpStatus.OK) {
            throw new RoomNotFoundException(roomId);
        }

        Queue queue = queueRepository.findByRoomId(roomId)
                .orElseThrow(() -> new QueueNotFoundException(roomId));

        return queueMapper.entityToDto(queue);
    }

    public TrackDto getCurrentTrack(UUID roomId) {
        ResponseEntity<RoomResponse> roomRe = roomClient.getRoom(roomId);
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

    @Transactional
    public void moveTrack(UUID roomId, UUID trackId, int newPosition) {
        ResponseEntity<RoomResponse> roomRe = roomClient.getRoom(roomId);
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

    @Transactional
    public void removeTrack(UUID roomId, UUID trackId) {
        ResponseEntity<RoomResponse> roomRe = roomClient.getRoom(roomId);
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

    @Transactional
    public void clearQueue(UUID roomId) {
        ResponseEntity<RoomResponse> roomRe = roomClient.getRoom(roomId);
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


    public TrackDto next(UUID roomId) {
        ResponseEntity<RoomResponse> roomRe = roomClient.getRoom(roomId);
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
    public TrackDto previous(UUID roomId) {
        ResponseEntity<RoomResponse> roomRe = roomClient.getRoom(roomId);
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

    private void publishTrackChangedEvent(Queue queue, Track track) {
        TrackDto trackInfo = trackMapper.entityToDto(track, queue.getTracks().indexOf(track));
        TrackChangedEvent event = new TrackChangedEvent(trackInfo);

        String routingKey = "room." + queue.getRoomId() + ".track.changed";

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.TRACK_CHANGED_EXCHANGE,
                routingKey,
                event
        );
        log.info("Published TrackChangedEvent for room {}", queue.getRoomId());
    }
}