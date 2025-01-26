package com.fileprocessingapplication.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fileprocessingapplication.event.FileProcessingEvent;
import com.fileprocessingapplication.processor.inputprocessors.CSVProcessor;
import com.fileprocessingapplication.processor.inputprocessors.JSONProcessor;
import com.fileprocessingapplication.processor.inputprocessors.XMLProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Path;

@Component
public class EventConsumers {
    private static final Logger logger = LoggerFactory.getLogger(EventConsumers.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    private CSVProcessor csvProcessor;

    @Autowired
    private JSONProcessor jsonProcessor;

    @Autowired
    private XMLProcessor xmlProcessor;

    public EventConsumers() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE
        );
    }

    @RabbitListener(queues = "csv_queue", concurrency = "5")
    public void processCSVFile(String object) {
        try {
            logger.info("Processing CSV File: {}", object);
            FileProcessingEvent fileProcessingEvent = objectMapper.readValue(object, FileProcessingEvent.class);
            csvProcessor.processFile(Path.of(fileProcessingEvent.getFilePath()));
        } catch (Exception e) {
            logger.error("Error while processing csv: {}", object, e);
        }
    }

    @RabbitListener(queues = "json_queue", concurrency = "5")
    public void processJSONFile(String object) {
        try {
            logger.info("Processing JSON File: {}", object);
            FileProcessingEvent fileProcessingEvent = objectMapper.readValue(object, FileProcessingEvent.class);
            jsonProcessor.processFile(Path.of(fileProcessingEvent.getFilePath()));
        } catch (Exception e) {
            logger.error("Error while processing JSON: {}", object, e);
        }
    }

    @RabbitListener(queues = "xml_queue", concurrency = "5")
    public void processXMLFile(String object) {
        try {
            logger.info("Processing XML File: {}", object);
            FileProcessingEvent fileProcessingEvent = objectMapper.readValue(object, FileProcessingEvent.class);
            xmlProcessor.processFile(Path.of(fileProcessingEvent.getFilePath()));
        } catch (Exception e) {
            logger.error("Error while processing XML: {}", object, e);
        }
    }
}
