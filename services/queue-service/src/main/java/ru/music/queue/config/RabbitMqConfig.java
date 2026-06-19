package ru.music.queue.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@Slf4j
public class RabbitMqConfig {

    public static final String TRACK_CHANGED_EXCHANGE = "track.changed.exchange";
    public static final String TRACK_CHANGED_ROUTING_KEY = "track.changed";
    public static final String QUEUE_FINISHED_QUEUE = "queue.finished.queue";
    public static final String QUEUE_FINISHED_ROUTING_KEY = "queue.finished";

    @Bean
    public TopicExchange trackChangedExchange() {
        return ExchangeBuilder.topicExchange(TRACK_CHANGED_EXCHANGE).durable(true).build();
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }

    @Bean
    public JsonMapper jsonMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
                .build();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         JacksonJsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.debug("Message confirmed by broker: {}", correlationData);
            } else {
                log.error("Message NOT confirmed: {}, cause: {}", correlationData, cause);
            }
        });
        return template;
    }

    @Bean
    public Queue queueFinishedQueue() {
        return QueueBuilder.durable(QUEUE_FINISHED_QUEUE).build();
    }

    @Bean
    public Binding queueFinishedBinding() {
        return BindingBuilder.bind(queueFinishedQueue())
                .to(trackChangedExchange())
                .with(QUEUE_FINISHED_ROUTING_KEY);
    }
}