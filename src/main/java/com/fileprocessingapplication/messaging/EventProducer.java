package com.fileprocessingapplication.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EventProducer {

    Logger logger = LoggerFactory.getLogger(EventProducer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    ObjectMapper objectMapper;

    public void sendEventToRabbitMQ(String routingKey, Object event) {
        try {
            logger.info("Publishing event to RabbitMQ: ", event);
            rabbitTemplate.convertAndSend(routingKey, objectMapper.writeValueAsString(event));
            logger.info("Successfully published event to RabbitMQ: ", event.toString());
        } catch (Exception e) {
            logger.info("Publishing event failed to RabbitMQ: ", event);
            logger.error(e.getMessage(), e);
        }
    }
}
