package ru.music.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class UserIdHeaderGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .flatMap(ctx -> {
                    if (ctx.getAuthentication() != null && ctx.getAuthentication().getPrincipal() instanceof Jwt jwt) {
                        String userId = jwt.getClaimAsString("user_id");
                        if (userId == null) userId = jwt.getSubject();
                        final String finalUserId = (userId != null) ? userId : jwt.getSubject();

                        // Извлекаем имя пользователя и email
                        String username = jwt.getClaimAsString("preferred_username");
                        if (username == null) username = jwt.getClaimAsString("name");
                        final String finalUsername = (username != null) ? username : jwt.getSubject();


                        String email = jwt.getClaimAsString("email");

                        System.out.println("=== Gateway: userId=" + userId + ", username=" + username + ", email=" + email);

                        ServerWebExchange mutatedExchange = exchange.mutate()
                                .request(builder -> builder
                                        .header("X-User-Id", finalUserId)
                                        .header("X-User-Name", finalUsername != null ? finalUsername : "")
                                        .header("X-User-Email", email != null ? email : ""))
                                .build();
                        return chain.filter(mutatedExchange);
                    }
                    return chain.filter(exchange);
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}