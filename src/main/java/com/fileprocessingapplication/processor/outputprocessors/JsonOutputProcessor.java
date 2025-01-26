package com.fileprocessingapplication.processor.outputprocessors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
public class JsonOutputProcessor implements OutputProcessor {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void writeOutput(List<Object> records, Path outputPath) throws Exception {
        objectMapper.writeValue(outputPath.toFile(), records);
    }
}

