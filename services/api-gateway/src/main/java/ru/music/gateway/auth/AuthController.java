package ru.music.gateway.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.music.gateway.auth.AuthService;

@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    Mono<ResponseEntity<Object>> login(@RequestBody LoginRequest request) {
        return authService.login(request.username(), request.password())
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body(e.getMessage())));
    }

    @PostMapping("/register")
    Mono<ResponseEntity<Object>> register(@RequestBody RegisterRequest request) {
        return authService.register(request.username(), request.password(), request.email())
                .map(v -> ResponseEntity.ok().build())
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body(e.getMessage())));
    }
}
