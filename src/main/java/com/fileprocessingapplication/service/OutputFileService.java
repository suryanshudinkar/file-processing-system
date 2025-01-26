package com.fileprocessingapplication.service;

import com.fileprocessingapplication.processor.outputprocessors.FileNameTemplateProcessor;
import com.fileprocessingapplication.processor.outputprocessors.OutputProcessor;
import com.fileprocessingapplication.processor.outputprocessors.OutputProcessorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
public class OutputFileService {

    @Value("${file.output.path.csv}")
    private String outputPathCsv;

    @Value("${file.output.path.json}")
    private String outputPathJson;

    @Value("${file.output.path.xml}")
    private String outputPathXml;

    @Value("${output.format.csv}")
    private String configuredOutputFormatCsv;

    @Value("${output.format.json}")
    private String configuredOutputFormatJson;

    @Value("${output.format.xml}")
    private String configuredOutputFormatXml;

    @Autowired
    private OutputProcessorFactory outputProcessorFactory;

    public void generateOutputFile(List<Object> records, String fileType) {
        String outputFormat = getConfiguredOutputFormatOfAFileType(fileType);
        String outputPath = getConfiguredOutputPathOfAFileType(outputFormat);

        // Use OutputProcessor to write output based on the format (e.g., CSV, JSON, etc.)
        OutputProcessor processor = outputProcessorFactory.getProcessor(outputFormat);
        Path path = Path.of(outputPath, FileNameTemplateProcessor.generateFileName("records", outputFormat, 1));
        try {
            processor.writeOutput(records, path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getConfiguredOutputPathOfAFileType(String outputFormat) {
        switch (outputFormat) {
            case "json":
                return outputPathJson;
                case "xml":
                    return outputPathXml;
                    case "csv":
                        return outputPathCsv;
                        default:
                            throw new IllegalArgumentException("Unsupported output format: " + outputFormat);
        }
    }

    private String getConfiguredOutputFormatOfAFileType(String fileType) {
        switch (fileType) {
            case "csv":
                return configuredOutputFormatCsv;
                case "json":
                    return configuredOutputFormatJson;
                    case "xml":
                        return configuredOutputFormatXml;
                        default:
                            throw new IllegalArgumentException("Unsupported output format: " + fileType);
        }
    }
}
