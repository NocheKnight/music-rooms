package ru.music.media.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RabbitMqConfig {
    public static final String TRACK_CHANGED_EXCHANGE = "amq.topic";
    public static final String MEDIA_QUEUE = "media-service.track.changed";

    @Bean
    public Queue mediaQueue() {
        return new Queue(MEDIA_QUEUE, true);
    }

    @Bean
    public Binding mediaQueueBinding(Queue mediaQueue) {
        return BindingBuilder
                .bind(mediaQueue)
                .to(new TopicExchange(TRACK_CHANGED_EXCHANGE))
                .with("room.*.track.changed");
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         JacksonJsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}