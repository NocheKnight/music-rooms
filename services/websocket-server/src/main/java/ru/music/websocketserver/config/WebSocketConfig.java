package ru.music.websocketserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${spring.rabbitmq.host}")
    private String rabbitMqHost;

    @Value("${spring.rabbitmq.stomp-port}")
    private Integer rabbitMqStompPort;

    @Value("${spring.rabbitmq.username}")
    private String rabbitMqUser;

    @Value("${spring.rabbitmq.password}")
    private String rabbitMqPassword;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableStompBrokerRelay("/topic", "/queue", "/exchange", "/amq/queue")
                .setRelayHost(rabbitMqHost)
                .setRelayPort(rabbitMqStompPort)
                .setClientLogin(rabbitMqUser)
                .setClientPasscode(rabbitMqPassword)
                .setSystemLogin(rabbitMqUser)
                .setSystemPasscode(rabbitMqPassword);

        registry.setApplicationDestinationPrefixes("/app");
    }
}