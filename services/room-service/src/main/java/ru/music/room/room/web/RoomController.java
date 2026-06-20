package ru.music.room.room.web;

import ru.music.room.auth.model.User;
import ru.music.room.room.dto.CreateRoomRequest;
import ru.music.room.room.dto.JoinRoomRequest;
import ru.music.room.room.dto.RoomResponse;
import ru.music.room.room.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Slf4j
public class RoomController {
    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(
            @Valid @RequestBody CreateRoomRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        log.info("Creating room for user: id={}, name={}", currentUser.getId(), currentUser.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roomService.createRoom(request, currentUser));
    }

    @PostMapping("/join")
    public ResponseEntity<RoomResponse> joinRoom(
            @Valid @RequestBody JoinRoomRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        log.info("User {} joining room with code {}", currentUser.getId(), request.inviteCode());
        return ResponseEntity.ok(roomService.joinRoom(request, currentUser.getKeycloakId()));
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> getRoom(
            @PathVariable(name = "roomId") UUID roomId,
            @AuthenticationPrincipal User currentUser
    ) {
        log.debug("User {} fetching room {}", currentUser.getId(), roomId);
        return ResponseEntity.ok(roomService.getRoom(roomId));
    }
}