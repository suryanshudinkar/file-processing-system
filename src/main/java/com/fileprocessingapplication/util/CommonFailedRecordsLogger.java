package com.fileprocessingapplication.util;

import com.fileprocessingapplication.dto.CsvRecordDto;
import com.fileprocessingapplication.dto.JsonRecordDto;
import com.fileprocessingapplication.dto.XmlRecordDto;
import com.opencsv.CSVWriter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Utility class for logging failed records into different file formats (CSV, JSON, XML).
 * It provides functionality to log failed records in a structured manner for future debugging or analysis.
 */
@Component
public class CommonFailedRecordsLogger {

    private static final Logger logger = org.apache.logging.log4j.LogManager.getLogger(CommonFailedRecordsLogger.class);
    private static final String errorFileHeader = "ID, Name, Amount, Currency, Date, Error Message"; // CSV header

    /**
     * Logs the failed records into a file in the specified format (CSV, JSON, XML).
     *
     * @param failedRecords a list of failed records that need to be logged
     * @param errorLogPath the path where the error log should be written
     * @param format the format in which the records should be logged (CSV, JSON, XML)
     */
    public static void logFailedRecords(List<FailedRecord> failedRecords, Path errorLogPath, String format) {
        try {
            // Determine the file format and log accordingly
            switch (format.toLowerCase()) {
                case "csv":
                    logFailedRecordsCsv(failedRecords, errorLogPath);
                    break;
                case "json":
                    logFailedRecordsJson(failedRecords, errorLogPath);
                    break;
                case "xml":
                    logFailedRecordsXml(failedRecords, errorLogPath);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported file format: " + format);
            }
            logger.info("Failed records logged to: {}", errorLogPath);
        } catch (Exception e) {
            logger.error("Error logging failed records: {}", e.getMessage());
        }
    }

    /**
     * Logs the failed records in CSV format.
     *
     * @param failedRecords a list of failed records to be logged
     * @param errorLogPath the path where the CSV file should be written
     * @throws IOException if an error occurs during writing to the file
     */
    private static void logFailedRecordsCsv(List<FailedRecord> failedRecords, Path errorLogPath) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(errorLogPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
             CSVWriter csvWriter = new CSVWriter(writer)) {

            // Write header if the file is new
            if (Files.size(errorLogPath) == 0) {
                csvWriter.writeNext(errorFileHeader.split(","));
            }

            // Write failed records
            for (FailedRecord failedRecord : failedRecords) {
                    CsvRecordDto record = (CsvRecordDto) failedRecord.getRecord();
                    csvWriter.writeNext(new String[]{
                            record.getId().toString(),
                            record.getName(),
                            String.valueOf(record.getAmount()),
                            record.getCurrency(),
                            record.getDate(),
                            failedRecord.getErrorMessage()
                    });
            }
        }
    }

    /**
     * Logs the failed records in JSON format.
     *
     * @param failedRecords a list of failed records to be logged
     * @param errorLogPath the path where the JSON file should be written
     * @throws IOException if an error occurs during writing to the file
     */
    private static void logFailedRecordsJson(List<FailedRecord> failedRecords, Path errorLogPath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, Object>> errorLogList = new ArrayList<>();

        for (FailedRecord failedRecord : failedRecords) {
            JsonRecordDto record = (JsonRecordDto) failedRecord.getRecord();
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("ID", record.getId());
            errorMap.put("Name", record.getName());
            errorMap.put("Amount", record.getAmount());
            errorMap.put("Currency", record.getCurrency());
            errorMap.put("Date", record.getDate());
            errorMap.put("Error Message", failedRecord.getErrorMessage());
            errorLogList.add(errorMap);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(errorLogPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            // If file exists, no need to add a header for JSON
            objectMapper.writeValue(writer, errorLogList);
        }
    }

    /**
     * Logs the failed records in XML format.
     *
     * @param failedRecords a list of failed records to be logged
     * @param errorLogPath the path where the XML file should be written
     * @throws IOException if an error occurs during writing to the file
     */
    private static void logFailedRecordsXml(List<FailedRecord> failedRecords, Path errorLogPath) throws IOException {
        StringBuilder xmlContent = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<FailedRecords>\n");

        for (FailedRecord failedRecord : failedRecords) {
            if (failedRecord.getRecord() instanceof XmlRecordDto) {
                XmlRecordDto record = (XmlRecordDto) failedRecord.getRecord();
                xmlContent.append("<FailedRecord>\n")
                        .append("<ID>").append(record.getId()).append("</ID>\n")
                        .append("<Name>").append(record.getName()).append("</Name>\n")
                        .append("<Amount>").append(record.getAmount()).append("</Amount>\n")
                        .append("<Currency>").append(record.getCurrency()).append("</Currency>\n")
                        .append("<Date>").append(record.getDate()).append("</Date>\n")
                        .append("<ErrorMessage>").append(failedRecord.getErrorMessage()).append("</ErrorMessage>\n")
                        .append("</FailedRecord>\n");
            } else {
                xmlContent.append("<FailedRecord>\n")
                        .append("<Record>").append(failedRecord.getRecord()).append("</Record>\n")
                        .append("<ErrorMessage>").append(failedRecord.getErrorMessage()).append("</ErrorMessage>\n")
                        .append("</FailedRecord>\n");
            }
        }

        xmlContent.append("</FailedRecords>");

        // Write the constructed XML content to the file
        try (BufferedWriter writer = Files.newBufferedWriter(errorLogPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(xmlContent.toString());
        }
    }
}
