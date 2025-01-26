package com.fileprocessingapplication.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fileprocessingapplication.util.ProcessingSummary;
import com.opencsv.CSVWriter;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class ProcessingSummaryService {

    private final Logger logger = LoggerFactory.getLogger(ProcessingSummaryService.class);

    @Value("${summary.csv.header}")
    private String summaryCSVHeader;

    @Value("${processing.output.path.csv}")
    private String processingOutputPathCSV;

    @Value("${processing.output.path.json}")
    private String processingOutputPathJson;

    @Value("${processing.output.path.xml}")
    private String processingOutputPathXml;

    public void generateProcessingSummary(String fileName, long totalRecords, long successfulRecords, LocalDateTime startTime, String fileType) {
        // Update summary
        ProcessingSummary summary = new ProcessingSummary(fileName);
        summary.setTotalRecords(totalRecords);
        summary.setSuccessfulRecords(successfulRecords);
        summary.setFailedRecords(totalRecords - successfulRecords);
        // Record processing time
        LocalDateTime endTime = LocalDateTime.now();
        summary.setProcessingTimeInSeconds(Duration.between(startTime, endTime).toSeconds());

        switch (fileType) {
            case "csv":
                saveSummaryToCsv(summary);
                break;
            case "json":
                saveSummaryToJson(summary);
                break;
            case "xml":
                saveSummaryToXml(summary);
                break;
            default:
                throw new IllegalArgumentException("Unsupported file type: " + fileType);
        }

        logger.info("Processing Summary:\n{}", summary);
    }

    public void saveSummaryToCsv(ProcessingSummary summary) {
        Path summaryLogPath = Path.of(processingOutputPathCSV, "summary_" + summary.getFileName());
        try (BufferedWriter writer = Files.newBufferedWriter(summaryLogPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
             CSVWriter csvWriter = new CSVWriter(writer)) {

            // Check if the file is new and write the header
            if (Files.size(summaryLogPath) == 0) {
                csvWriter.writeNext(summaryCSVHeader.split(","));
            }

            // Write summary details
            csvWriter.writeNext(new String[]{
                    summary.getFileName(),
                    String.valueOf(summary.getTotalRecords()),
                    String.valueOf(summary.getSuccessfulRecords()),
                    String.valueOf(summary.getFailedRecords()),
                    String.valueOf(summary.getProcessingTimeInSeconds())
            });

            logger.info("Processing summary logged successfully to: {}", summaryLogPath);
        } catch (IOException e) {
            logger.error("Error while logging processing summary to file: {}", e.getMessage());
        }

    }

    private void saveSummaryToJson(ProcessingSummary summary) {
        try {
            // Create an ObjectMapper instance
            ObjectMapper objectMapper = new ObjectMapper();

            // Create the output file path
            Path outputPath = Path.of(processingOutputPathJson, "summary_" + summary.getFileName());

            // Write the ProcessingSummary object to the JSON file
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputPath.toFile(), summary);

            logger.info("Processing summary saved to JSON: {}", outputPath);
        } catch (IOException e) {
            logger.info("Error saving summary to JSON: {}", e.getMessage());
        }
    }

    private void saveSummaryToXml(ProcessingSummary summary) {
        try {
            // Create the JAXB context for the ProcessingSummary class
            JAXBContext context = JAXBContext.newInstance(ProcessingSummary.class);

            // Create a Marshaller to convert the object to XML
            Marshaller marshaller = context.createMarshaller();

            // Format the XML output
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            // Create the output file path
            Path outputPath = Path.of(processingOutputPathXml, "summary_" + summary.getFileName());

            // Write the object to the XML file
            marshaller.marshal(summary, outputPath.toFile());

            logger.info("Processing summary saved to XML: {}", outputPath);
        } catch (Exception e) {
            logger.info("Error saving summary to XML: {}", e.getMessage());
        }
    }


}
