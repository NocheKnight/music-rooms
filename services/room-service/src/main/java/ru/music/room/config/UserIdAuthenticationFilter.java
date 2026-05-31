package ru.music.room.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class UserIdAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {
        String userId = request.getHeader("X-User-Id");
        System.out.println("=== RoomService: received X-User-Id = " + userId);
        if (userId != null && !userId.isEmpty()) {
            try {
                UUID userIdUuid = UUID.fromString(userId);
                Authentication auth = new PreAuthenticatedAuthenticationToken(userIdUuid, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (IllegalArgumentException e) {
                logger.error("Неверный формат UUID");
            }
        }
        chain.doFilter(request, response);
    }
}