package ru.music.queue.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public class FeignConfig {
    @Bean
    public RequestInterceptor requestInterceptor() {
        return (RequestTemplate template) -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UUID userId) {
                template.header("X-User-Id", userId.toString());
            }
        };
    }
}