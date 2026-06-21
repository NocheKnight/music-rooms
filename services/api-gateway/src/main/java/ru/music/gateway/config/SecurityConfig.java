package ru.music.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/health", "/ws/**").permitAll()
                        .pathMatchers(
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/webjars/**",
                                "api/auth/**",
                                "/api/ws/**"
                                ).permitAll()
                        .pathMatchers("/api/*/v3/api-docs/**").permitAll()
                        .pathMatchers("/api/media/stream/**").permitAll()
                        .pathMatchers("/realms/**", "/protocol/**", "/resources/**", "/auth/**",
                                "/admin/**",
                                "/js/**",
                                "/css/**",
                                "/img/**",
                                "/auth/**").permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
                .build();
    }

//    @Bean
//    public WebFilter userIdHeaderFilter() {
//        return (exchange, chain) -> exchange.getPrincipal()
//                .filter(principal -> principal instanceof org.springframework.security.oauth2.jwt.Jwt)
//                .cast(org.springframework.security.oauth2.jwt.Jwt.class)
//                .flatMap(jwt -> {
//                    final String userId = jwt.getClaimAsString("user_id");
//                    final String finalUserId = (userId != null) ? userId : jwt.getSubject(); // effectively final
//
//                    System.out.println("=== Gateway: extracted userId = " + userId); // лог
//
//                    ServerWebExchange mutatedExchange = exchange.mutate()
//                            .request(builder -> builder.header("X-User-Id", finalUserId))
//                            .build();
//                    return chain.filter(mutatedExchange);
//                })
//                .switchIfEmpty(chain.filter(exchange));
//    }
}