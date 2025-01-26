package com.fileprocessingapplication.processor.outputprocessors;

import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
public interface OutputProcessor {
    void writeOutput(List<Object> records, Path outputPath) throws Exception;
}
