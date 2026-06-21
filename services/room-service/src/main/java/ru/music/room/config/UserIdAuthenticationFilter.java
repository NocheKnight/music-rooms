package ru.music.room.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.music.room.auth.model.User;
import ru.music.room.auth.service.UserService;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Slf4j
public class UserIdAuthenticationFilter extends OncePerRequestFilter {

    private final UserService userService;

    public UserIdAuthenticationFilter(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String userIdHeader = request.getHeader("X-User-Id");

        if (userIdHeader == null || userIdHeader.isEmpty()) {
            log.debug("No X-User-Id header present");
            chain.doFilter(request, response);
            return;
        }

        try {
            UUID keycloakId = UUID.fromString(userIdHeader);
            User user = userService.getUserByKeycloakId(keycloakId);
            Authentication auth = new PreAuthenticatedAuthenticationToken(user, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);
            request.setAttribute("currentUser", user);
            log.debug("Authenticated user: {}", keycloakId);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid X-User-Id format: {}", userIdHeader);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid user ID format");
            return;
        } catch (RuntimeException e) {
            log.warn("User not found for X-User-Id: {}", userIdHeader);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not found");
            return;
        }

        chain.doFilter(request, response);
    }
}