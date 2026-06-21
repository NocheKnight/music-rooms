package ru.music.websocketserver.components;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
public class WebSocketEventListener {
    @EventListener
    public void handleConnected(SessionConnectedEvent event) {
        System.out.println("Клиент подключился");
    }

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();
        System.out.println("Подписка на: " + destination);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        System.out.println("Клиент отключился");
    }
}