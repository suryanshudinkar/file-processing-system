package com.fileprocessingapplication.processor.outputprocessors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class OutputProcessorFactory {

    @Autowired
    private ApplicationContext context;

    public OutputProcessor getProcessor(String format) {
        return switch (format.toLowerCase()) {
            case "csv" -> context.getBean(CsvOutputProcessor.class);
            case "json" -> context.getBean(JsonOutputProcessor.class);
            case "xml" -> context.getBean(XmlOutputProcessor.class);
            default -> throw new IllegalArgumentException("Unsupported format: " + format);
        };
    }
}
