package ru.music.gateway.auth;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final WebClient webClient;

    @Value("${keycloak.url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.registration.client-id}")
    private String registrationClientId;

    @Value("${keycloak.registration.client-secret}")
    private String registrationClientSecret;

    public Mono<Object> login(String username, String password) {
        return webClient.post()
                .uri(keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "password")
                        .with("client_id", clientId)
                        .with("username", username)
                        .with("password", password))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException("Invalid credentials"))))
                .bodyToMono(Object.class);
    }

    public Mono<Void> register(String username, String password, String email) {
        return getServiceAccountToken()
                .flatMap(token -> createUser(token, username, password, email));
    }

    private Mono<String> getServiceAccountToken() {
        return webClient.post()
                .uri(keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "client_credentials")
                        .with("client_id", registrationClientId)
                        .with("client_secret", registrationClientSecret))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(map -> (String) map.get("access_token"));
    }

    private Mono<Void> createUser(String token, String username,
                                  String password, String email) {
        Map<String, Object> body = Map.of(
                "username", username,
                "email", email,
                "enabled", true,
                "firstName", username,
                "lastName", "",
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", password,
                        "temporary", false
                ))
        );

        return webClient.post()
                .uri(keycloakUrl + "/admin/realms/" + realm + "/users")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> status.value() == 409,
                        response -> Mono.error(new RuntimeException("User already exists")))
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(body2 -> Mono.error(new RuntimeException("Registration failed: " + body2))))
                .toBodilessEntity()
                .then();
    }
}