package ru.music.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Function;

/**
 * Фильтр, который извлекает X-User-Id, находит пользователя через переданный сервис
 * и устанавливает аутентификацию в SecurityContext.
 */
public class UserIdAuthenticationFilter<T> extends OncePerRequestFilter {

    private final Function<UUID, T> userLoader;
    private final Class<T> userType;

    public UserIdAuthenticationFilter(Function<UUID, T> userLoader, Class<T> userType) {
        this.userLoader = userLoader;
        this.userType = userType;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String userIdHeader = request.getHeader("X-User-Id");

        if (userIdHeader == null || userIdHeader.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        try {
            UUID keycloakId = UUID.fromString(userIdHeader);
            T user = userLoader.apply(keycloakId);
            Authentication auth = new PreAuthenticatedAuthenticationToken(user, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);
            request.setAttribute("currentUser", user);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid X-User-Id format");
            return;
        } catch (RuntimeException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not found");
            return;
        }

        chain.doFilter(request, response);
    }
}