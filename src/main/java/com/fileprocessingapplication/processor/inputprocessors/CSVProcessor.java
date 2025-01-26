package com.fileprocessingapplication.processor.inputprocessors;

import com.fileprocessingapplication.dto.CsvRecordDto;
import com.fileprocessingapplication.service.AuditLogService;
import com.fileprocessingapplication.service.OutputFileService;
import com.fileprocessingapplication.service.ProcessingSummaryService;
import com.fileprocessingapplication.util.CommonFailedRecordsLogger;
import com.fileprocessingapplication.util.FailedRecord;
import com.fileprocessingapplication.util.ProcessingSummary;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * CSVProcessor is responsible for processing CSV files, validating records, applying transformations,
 * and generating output files. This class extends the abstract class AbstractProcessor and implements
 * the logic specific to processing CSV files.
 */
@Component
public class CSVProcessor extends AbstractProcessor {

    private final Logger logger = LoggerFactory.getLogger(CSVProcessor.class);
    private final Validator validator;

    @Value("${error.file.path.csv}")
    private String errorFilePathCSV;

    @Value("${input.csv.header}")
    private String inputCSVHeader;

    public CSVProcessor(Validator validator, ProcessingSummaryService processingSummaryService,
                        AuditLogService auditLogService,
                        OutputFileService outputFileService) {
        super(processingSummaryService, auditLogService, outputFileService, "csv");
        this.validator = validator;
    }

    /**
     * This method is the main entry point for processing a CSV file.
     *
     * @param csvFilePath The path to the CSV file to be processed.
     */
    @Override
    public void processFile(Path csvFilePath) {
        totalRecords.getAndSet(0);
        fileName = csvFilePath.getFileName().toString();
        ProcessingSummary summary = new ProcessingSummary(fileName);
        LocalDateTime startTime = LocalDateTime.now();
        try {
            auditLogService.logStart(fileName, fileType);
            List<CsvRecordDto> records = processCsvInChunks(csvFilePath);
            long successfulRecords = records.size();

            outputFileService.generateOutputFile(new ArrayList<>(records), "csv");
            processingSummaryService.generateProcessingSummary(fileName, totalRecords.get(), successfulRecords, startTime, fileType);

            logger.info("File processed successfully: {}");
            auditLogService.logEnd(fileName, "SUCCESS", null);
        } catch (Exception e) {
            logger.error("Error processing file: {}", e.getMessage());
            summary.setFailedRecords(totalRecords.get());
            auditLogService.logEnd(fileName, "FAILED", e.getMessage());
        }
    }

    /**
     * Process the CSV file in chunks to handle large files efficiently.
     *
     * @param csvFilePath The path to the CSV file to be processed.
     * @return A list of processed CsvRecordDto objects.
     */
    public List<CsvRecordDto> processCsvInChunks(Path csvFilePath) {
        try (BufferedReader reader = Files.newBufferedReader(csvFilePath)) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                    .withHeader(inputCSVHeader.split(","))
                    .withSkipHeaderRecord(true)
                    .parse(reader);

            List<CsvRecordDto> allRecords = new ArrayList<>();
            List<FailedRecord> failedRecords = new ArrayList<>();
            Path errorLogPath = Path.of(errorFilePathCSV + "error_" + fileName);
            List<CsvRecordDto> processedRecords = new ArrayList<>();

            // Collect all records from the CSV
            for (CSVRecord record : records) {
                totalRecords.incrementAndGet();
                try {
                    CsvRecordDto dto = parseRecord(record);
                    allRecords.add(dto);
                } catch (Exception e) {
                    CsvRecordDto dto = new CsvRecordDto();
                    dto.setId(totalRecords.intValue());
                    failedRecords.add(new FailedRecord(dto, e.getMessage()));
                }
            }

            if (allRecords.size() > 0) {
                // Split into chunks
                List<List<CsvRecordDto>> chunks = splitIntoChunks(allRecords);

                // Process each chunk in parallel
                List<Future<List<CsvRecordDto>>> futures = new ArrayList<>();

                for (List<CsvRecordDto> chunk : chunks) {
                    futures.add(executorService.submit(() -> processChunk(chunk, failedRecords)));
                }

                // Wait for all tasks to complete and collect results
                for (Future<List<CsvRecordDto>> future : futures) {
                    processedRecords.addAll(future.get());
                }
            }

            if (failedRecords.size() > 0) {
                // Log failed records to an error log file
                CommonFailedRecordsLogger.logFailedRecords(failedRecords, errorLogPath, "csv");
            }

            return processedRecords;
        } catch (Exception e) {
            throw new RuntimeException("Failed to process CSV: " + e.getMessage(), e);
        }

    }

    /**
     * Attempt to process a record multiple times in case of failure.
     *
     * @param record        The CSV record to be processed.
     * @param failedRecords List to store records that failed after retries.
     * @return true if the record was successfully processed, false otherwise.
     */
    public boolean processRecordWithRetry(CsvRecordDto record, List<FailedRecord> failedRecords) {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                // Validate record
                Set<ConstraintViolation<CsvRecordDto>> violations = validator.validate(record);
                if (!violations.isEmpty()) {
                    throw new RuntimeException("Validation failed: " +
                            violations.stream()
                                    .map(ConstraintViolation::getMessage)
                                    .collect(Collectors.joining(", ")));
                }

                // Transform record
                transformData(record);

                return true; // Successfully processed
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
     * Transform the data in a record. This may include formatting dates, rounding amounts,
     * or performing currency conversion.
     *
     * @param record The CSV record to be transformed.
     */
    public void transformData(CsvRecordDto record) {
        // Convert date to standard format
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate date = LocalDate.parse(record.getDate(), inputFormatter);
        record.setDate(date.format(outputFormatter));

        // Round amount to 2 decimal places
        record.setAmount(BigDecimal.valueOf(record.getAmount())
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue());

        // Example currency conversion (USD to EUR with a fixed rate)
        if ("USD".equalsIgnoreCase(record.getCurrency())) {
            record.setAmount(record.getAmount() * 0.85); // Example conversion rate
            record.setCurrency("EUR");
        }

        // Handle null or missing fields (if applicable)
        if (record.getName() == null || record.getName().isEmpty()) {
            record.setName("Unknown");
        }
    }

    /**
     * Parse a single CSV record and convert it into a CsvRecordDto object.
     *
     * @param record The CSV record to be parsed.
     * @return A CsvRecordDto object representing the parsed record.
     */
    public CsvRecordDto parseRecord(CSVRecord record) {
        CsvRecordDto dto = new CsvRecordDto();

        String idText = record.get("id");
        String name = record.get("name");
        String amountText = record.get("amount");
        String currency = record.get("currency");
        String date = record.get("date");

        // Build an error message for missing fields
        StringBuilder missingFields = new StringBuilder();
        if (idText == null || idText.isEmpty()) {
            missingFields.append("id ");
        }
        if (name == null || name.isEmpty()) {
            missingFields.append("name ");
        }
        if (amountText == null || amountText.isEmpty()) {
            missingFields.append("amount ");
        }
        if (currency == null || currency.isEmpty()) {
            missingFields.append("currency ");
        }
        if (date == null || date.isEmpty()) {
            missingFields.append("date ");
        }

        // If any fields are missing, throw an exception
        if (missingFields.length() > 0) {
            throw new IllegalArgumentException("Missing fields: " + missingFields);
        }

        try {
            dto.setId(Integer.parseInt(idText));
            dto.setName(name);
            dto.setAmount(Double.parseDouble(amountText));
            dto.setCurrency(currency);
            dto.setDate(date);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format in record: " + record, e);
        }

        return dto;
    }

    /**
     * Split a list of records into chunks for parallel processing.
     *
     * @param records The list of records to be split.
     * @return A list of chunks (lists of records).
     */
    private List<List<CsvRecordDto>> splitIntoChunks(List<CsvRecordDto> records) {
        List<List<CsvRecordDto>> chunks = new ArrayList<>();
        for (int i = 0; i < records.size(); i += CHUNK_SIZE) {
            chunks.add(records.subList(i, Math.min(i + CHUNK_SIZE, records.size())));
        }
        return chunks;
    }

    /**
     * Process a chunk of records concurrently.
     *
     * @param chunkRecords  The chunk of records to be processed.
     * @param failedRecords List to store records that failed processing.
     * @return A list of successfully processed records.
     */
    private List<CsvRecordDto> processChunk(List<CsvRecordDto> chunkRecords, List<FailedRecord> failedRecords ) {
        List<CsvRecordDto> validRecords = new ArrayList<>();

        for (CsvRecordDto record : chunkRecords) {
            boolean success = processRecordWithRetry(record, failedRecords);
            if (success) {
                validRecords.add(record);
            }
        }

        return validRecords;
    }
}
