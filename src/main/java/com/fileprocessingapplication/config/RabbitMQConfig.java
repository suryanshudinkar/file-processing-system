package com.fileprocessingapplication.config;

import com.fileprocessingapplication.enums.RoutingKey;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    @Bean
    public Queue csvQueue() {
        return new Queue(RoutingKey.CSV_QUEUE.getKey(), true);
    }

    @Bean
    public Queue jsonQueue() {
        return new Queue(RoutingKey.JSON_QUEUE.getKey(), true);
    }

    @Bean
    public Queue xmlQueue() {
        return new Queue(RoutingKey.XML_QUEUE.getKey(), true);
    }
}
