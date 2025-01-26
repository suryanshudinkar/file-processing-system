package com.fileprocessingapplication.service;

import com.fileprocessingapplication.enums.RoutingKey;
import com.fileprocessingapplication.event.FileProcessingEvent;
import com.fileprocessingapplication.messaging.EventProducer;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


/**
 * Service class responsible for processing files in a file processing application.
 * This service handles saving files to storage and publishing corresponding events to a message queue.
 */
@Service
public class FileProcessingService {

    private final RabbitTemplate rabbitTemplate;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private EventProducer eventProducer;

    /**
     * Constructor for initializing the FileProcessingService with the RabbitTemplate.
     *
     * @param rabbitTemplate the RabbitTemplate used for sending events to RabbitMQ
     */
    public FileProcessingService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Saves a file and publishes an event to RabbitMQ after the file is saved.
     * This method is called when a file is uploaded, and it initiates the file processing
     * by first saving the file and then notifying other components by sending an event.
     *
     * @param file the file to be processed and saved
     */
    public void saveFileAndPublishEvent(MultipartFile file) {
        // Save the file and obtain the event associated with the file
        FileProcessingEvent fileProcessingEvent = fileStorageService.saveFile(file);

        // Publish the event to the appropriate RabbitMQ queue based on the file type
        eventProducer.sendEventToRabbitMQ(getQueueName(fileProcessingEvent.getFileType()), fileProcessingEvent);
    }

    /**
     * Determines the appropriate RabbitMQ queue name based on the file type.
     * The method maps the file type to a routing key that corresponds to a specific queue.
     *
     * @param fileType the type of the file (e.g., "csv", "json", "xml")
     * @return the queue name that corresponds to the file type
     * @throws RuntimeException if the file type is unsupported
     */
    private static String getQueueName(String fileType) {
        switch (fileType.toLowerCase()) {
            case "csv":
                return RoutingKey.CSV_QUEUE.getKey();
            case "json":
                return RoutingKey.JSON_QUEUE.getKey();
            case "xml":
                return RoutingKey.XML_QUEUE.getKey();
            default:
                throw new RuntimeException("Unsupported file type: " + fileType);
        }
    }
}
