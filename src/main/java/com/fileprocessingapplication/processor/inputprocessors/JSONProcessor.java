package com.fileprocessingapplication.processor.inputprocessors;

import com.fileprocessingapplication.dto.JsonRecordDto;
import com.fileprocessingapplication.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fileprocessingapplication.service.OutputFileService;
import com.fileprocessingapplication.service.ProcessingSummaryService;
import com.fileprocessingapplication.util.CommonFailedRecordsLogger;
import com.fileprocessingapplication.util.FailedRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.*;

/**
 * This class processes JSON files, validates, transforms the data, logs errors, and writes valid records to output files.
 * It also handles retries for failed records and logs the failed records to an error file.
 */
@Component
public class JSONProcessor extends AbstractProcessor {
    private static final Logger logger = LoggerFactory.getLogger(JSONProcessor.class);

    @Autowired
    private OutputFileService outputFileService;

    @Value("${error.file.path.json}")
    private String errorFilePathJSON;

    @Autowired
    public JSONProcessor(
            ProcessingSummaryService processingSummaryService,
            AuditLogService auditLogService,
            OutputFileService outputFileService
    ){
        super(processingSummaryService, auditLogService, outputFileService, "json");
    }

    /**
     * Processes the given JSON file, validates the records, applies transformations, and generates the output file.
     * Logs errors and failed records to an error log.
     *
     * @param jsonFilePath Path to the JSON file to process
     */
    @Override
    public void processFile(Path jsonFilePath) {
        totalRecords.getAndSet(0);
        fileName = jsonFilePath.getFileName().toString();
        LocalDateTime startTime = LocalDateTime.now();
        try {
            // Log start of processing
            auditLogService.logStart(fileName, fileType);

            // Read JSON content
            List<JsonRecordDto> records = processJsonInChunks(jsonFilePath);
            long successfulRecords = records.size();

            outputFileService.generateOutputFile(new ArrayList<>(records), fileType);
            processingSummaryService.generateProcessingSummary(fileName, totalRecords.get(), successfulRecords, startTime, fileType);

            logger.info("File processed successfully");
            auditLogService.logEnd(fileName, "SUCCESS", null);
        } catch (Exception e) {
            logger.error("Error processing file: {}", e.getMessage());
            auditLogService.logEnd(fileName, "FAILED", e.getMessage());
        }
    }

    /**
     * Processes the JSON file in chunks to ensure efficient processing for large files.
     *
     * @param jsonFilePath Path to the JSON file
     * @return A list of valid records
     * @throws IOException If there is an error reading the JSON file
     */
    private List<JsonRecordDto> processJsonInChunks(Path jsonFilePath) throws IOException {
        List<JsonRecordDto> validRecords = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        List<JsonRecordDto> records = objectMapper.readValue(jsonFilePath.toFile(), objectMapper.getTypeFactory().constructCollectionType(List.class, JsonRecordDto.class));

        List<JsonRecordDto> chunkRecords = new ArrayList<>();
        List<Future<List<JsonRecordDto>>> futures = new ArrayList<>();
        List<FailedRecord> failedRecords = new ArrayList<>();
        Path errorLogPath = Path.of(errorFilePathJSON + "error_" + fileName);

        for (JsonRecordDto record : records) {
            chunkRecords.add(record);
            totalRecords.incrementAndGet();
            if (chunkRecords.size() >= CHUNK_SIZE) {
                // Submit chunk for parallel processing
                futures.add(executorService.submit(() -> processChunk(chunkRecords, failedRecords)));
                chunkRecords.clear(); // Clear chunk after submission
            }
        }

        // Process remaining chunk if any
        if (!chunkRecords.isEmpty()) {
            futures.add(executorService.submit(() -> processChunk(chunkRecords, failedRecords)));
        }

        // Wait for all chunks to be processed
        for (Future<List<JsonRecordDto>> future : futures) {
            try {
                validRecords.addAll(future.get());
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Error processing chunk: {}", e.getMessage());
            }
        }

        // Log failed records to an error log file
        if (failedRecords.size() > 0) {
            CommonFailedRecordsLogger.logFailedRecords(failedRecords, errorLogPath, "json");
        }

        return validRecords;
    }

    /**
     * Processes a chunk of records, retrying failed records up to a maximum number of retries.
     *
     * @param chunkRecords The chunk of records to process
     * @param failedRecords The list to store failed records
     * @return A list of valid records from the chunk
     */
    private List<JsonRecordDto> processChunk(List<JsonRecordDto> chunkRecords, List<FailedRecord> failedRecords) {
        List<JsonRecordDto> validRecords = new ArrayList<>();

        for (JsonRecordDto record : chunkRecords) {
            boolean success = processRecordWithRetry(record, failedRecords);
            if (success) {
                validRecords.add(record);
            }
        }

        return validRecords;
    }

    /**
     * Attempts to process a record with retries in case of failure.
     *
     * @param record The record to process
     * @param failedRecords The list to store failed records
     * @return True if the record was processed successfully, false otherwise
     */
    public boolean processRecordWithRetry(JsonRecordDto record, List<FailedRecord> failedRecords) {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                if (isValidRecord(record)) {
                    // Apply transformations
                    transformData(record);
                    return true;
                } else {
                    // Log failed record
                    logger.error("Validation failed for record: {}", record);
                    throw new RuntimeException("Validation failed for record: {}" + record);
                }
            } catch (Exception e) {
                attempt++;
                if (attempt == MAX_RETRIES) {
                    failedRecords.add(new FailedRecord(record, e.getMessage()));
                    return false; // Failed after max retries
                }
            }
        }
        return false;
    }

    /**
     * Validates the record to ensure all required fields are present and correctly formatted.
     *
     * @param record The record to validate
     * @return True if the record is valid, false otherwise
     */
    public boolean isValidRecord(JsonRecordDto record) {
        // Check if the essential fields are not null
        if (record.getId() == null) {
            logger.error("Validation failed: ID is missing for record: {}", record);
            return false;
        }
        if (record.getName() == null || record.getName().isEmpty()) {
            logger.error("Validation failed: Name is missing or empty for record: {}", record);
            return false;
        }
        if (record.getAmount() == null || record.getAmount() <= 0) {
            logger.error("Validation failed: Invalid or missing amount for record: {}", record);
            return false;
        }
        if (record.getCurrency() == null || record.getCurrency().isEmpty()) {
            logger.error("Validation failed: Currency is missing or empty for record: {}", record);
            return false;
        }

        // Custom date validation (example: ensure the date is not in the future)
        if (record.getDate() != null) {
            try {
                LocalDate date = LocalDate.parse(record.getDate(), DateTimeFormatter.ofPattern("MM/dd/yyyy"));
                if (date.isAfter(LocalDate.now())) {
                    logger.error("Validation failed: Date is in the future for record: {}", record);
                    return false;
                }
            } catch (DateTimeParseException e) {
                logger.error("Validation failed: Invalid date format for record: {}", record);
                return false;
            }
        }

        return true;
    }

    /**
     * Applies transformations to the data in the record, such as date formatting and currency conversion.
     *
     * @param record The record to transform
     */
    private void transformData(JsonRecordDto record) {
        // Example transformation: Convert date format from "MM/dd/yyyy" to "yyyy-MM-dd"
        if (record.getDate() != null) {
            try {
                // Assuming input date format is "MM/dd/yyyy"
                LocalDate date = LocalDate.parse(record.getDate(), DateTimeFormatter.ofPattern("MM/dd/yyyy"));
                record.setDate(date.format(DateTimeFormatter.ISO_LOCAL_DATE)); // Output format "yyyy-MM-dd"
            } catch (DateTimeParseException e) {
                logger.error("Error transforming date format for record: {}", record);
            }
        }

        // Example transformation: Currency conversion (dummy example, can be replaced with actual logic)
        if (record.getCurrency() != null && record.getCurrency().equals("USD")) {
            // Convert USD amount to another currency (e.g., EUR)
            double conversionRate = 0.85; // Example conversion rate from USD to EUR
            if (record.getAmount() != null) {
                double newAmount = record.getAmount() * conversionRate;
                record.setAmount(newAmount);
                record.setCurrency("EUR"); // Update currency to EUR after conversion
            }
        }
    }
}
