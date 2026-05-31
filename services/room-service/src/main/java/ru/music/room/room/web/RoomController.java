package ru.music.room.room.web;

import org.springframework.security.core.context.SecurityContextHolder;
import ru.music.room.room.dto.CreateRoomRequest;
import ru.music.room.room.dto.JoinRoomRequest;
import ru.music.room.room.dto.RoomResponse;
import ru.music.room.room.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {
    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(
            @Valid @RequestBody CreateRoomRequest request,
            @AuthenticationPrincipal String userIdStr   // теперь principal - String
    ) {
        UUID userId = UUID.fromString(userIdStr);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roomService.createRoom(request, userId));
    }

    @PostMapping("/join")
    public ResponseEntity<RoomResponse> joinRoom(
            @Valid @RequestBody JoinRoomRequest request,
            @AuthenticationPrincipal String userIdStr   // теперь principal - String
    ) {
        UUID userId = UUID.fromString(userIdStr);
        return ResponseEntity.ok(roomService.joinRoom(request, userId));
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable UUID roomId) {
        return ResponseEntity.ok(roomService.getRoom(roomId));
    }
}