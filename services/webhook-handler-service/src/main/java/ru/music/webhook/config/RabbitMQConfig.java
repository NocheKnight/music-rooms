package ru.music.webhook.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    // Название обменника (должно совпадать с тем, что настроено в Keycloak)
    public static final String EXCHANGE_NAME = "keycloak_events";
    // Очередь для этого сервиса
    public static final String QUEUE_NAME = "webhook-handler-queue";

    @Bean
    public TopicExchange keycloakExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue webhookQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public Binding binding(Queue webhookQueue, TopicExchange keycloakExchange) {
        // Привязываем очередь к обменнику с routing key "#" – получаем все события
        return BindingBuilder.bind(webhookQueue).to(keycloakExchange).with("#");
    }
}