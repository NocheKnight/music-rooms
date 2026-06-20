package ru.music.room.room.service;

import lombok.extern.slf4j.Slf4j;
import ru.music.room.auth.model.User;
import ru.music.room.auth.service.UserService;
import ru.music.room.room.dto.CreateRoomRequest;
import ru.music.room.room.dto.JoinRoomRequest;
import ru.music.room.room.dto.RoomResponse;
import ru.music.room.room.mapper.RoomMapper;
import ru.music.room.room.model.Room;
import ru.music.room.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;
    private final UserService userService;
    private final RoomMapper roomMapper;

    private final ReentrantLock inviteCodeLock = new ReentrantLock();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String INVITE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int INVITE_CODE_LENGTH = 8;

    @Transactional
    public RoomResponse createRoom(CreateRoomRequest request, User creator) {
        log.debug("Creating room '{}' for user {}", request.name(), creator.getId());

        Room room = new Room();
        room.setName(request.name());
        room.setInviteCode(generateUniqueInviteCode());
        room.setCreatedBy(creator.getKeycloakId());
        room.getParticipants().add(creator);
        room.setActive(true);

        Room savedRoom = roomRepository.save(room);
        log.info("Room created: id={}, inviteCode={}", savedRoom.getId(), savedRoom.getInviteCode());

        return roomMapper.toResponse(savedRoom);
    }

    @Transactional
    public RoomResponse joinRoom(JoinRoomRequest request, UUID userId) {
        log.debug("User {} joining room with invite code {}", userId, request.inviteCode());

        User user = userService.getUserByKeycloakId(userId);  // теперь через сервис

        Room room = roomRepository.findByInviteCode(request.inviteCode())
                .orElseThrow(() -> new RuntimeException("Room not found with invite code: " + request.inviteCode()));

        if (!room.isActive()) {
            throw new RuntimeException("Room is no longer active");
        }

        if (!room.getParticipants().contains(user)) {
            room.getParticipants().add(user);
            roomRepository.save(room);
            log.info("User {} joined room {}", userId, room.getId());
        }

        return roomMapper.toResponse(room);
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoom(UUID roomId) {
        log.debug("Fetching room {}", roomId);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + roomId));
        return roomMapper.toResponse(room);
    }

    private String generateUniqueInviteCode() {
        inviteCodeLock.lock();
        try {
            String inviteCode;
            boolean exists;
            do {
                inviteCode = generateRandomInviteCode();
                exists = roomRepository.findByInviteCode(inviteCode).isPresent();
            } while (exists);
            return inviteCode;
        } finally {
            inviteCodeLock.unlock();
        }
    }

    private String generateRandomInviteCode() {
        StringBuilder sb = new StringBuilder(INVITE_CODE_LENGTH);
        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            int index = SECURE_RANDOM.nextInt(INVITE_CODE_CHARS.length());
            sb.append(INVITE_CODE_CHARS.charAt(index));
        }
        return sb.toString();
    }
}