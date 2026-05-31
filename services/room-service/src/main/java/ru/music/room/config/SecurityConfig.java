package ru.music.room.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AnonymousConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Отключаем CSRF для API и веб-сокетов
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // --- ВАЖНО: Разрешаем все пути Swagger ---
                        .requestMatchers(
                                "/v3/api-docs",
                                "/v3/api-docs/**",      // JSON-спецификация API
                                "/swagger-ui.html",     // Точка входа для редиректа
                                "/swagger-ui/**",       // Все статические ресурсы UI
                                "/webjars/**"           // Внутренние библиотеки UI
                        ).permitAll()
                        // --- Ваши публичные эндпоинты ---
                        .requestMatchers("/ws/**", "/actuator/health").permitAll()
                        // --- Всё остальное требует аутентификации ---
                        .requestMatchers("/api/auth/dev-token").permitAll()
                        .anyRequest().permitAll()
                )
//                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {})); // Настройка JWT
                .anonymous(AnonymousConfigurer::disable)
                .addFilterBefore(new UserIdAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            var authorities = jwt.getClaimAsStringList("authorities");
            if (authorities != null) {
                return authorities.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
            }
            return List.of();
        });
        return converter;
    }
}
