package com.fileprocessingapplication.processor.inputprocessors;

import com.fileprocessingapplication.dto.XmlRecordDto;
import com.fileprocessingapplication.service.AuditLogService;
import com.fileprocessingapplication.service.OutputFileService;
import com.fileprocessingapplication.service.ProcessingSummaryService;
import com.fileprocessingapplication.util.CommonFailedRecordsLogger;
import com.fileprocessingapplication.util.FailedRecord;
import com.fileprocessingapplication.util.ProcessingSummary;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
 * XMLProcessor is responsible for processing XML files in chunks, performing validation and transformation
 * of records, and generating output and audit logs.
 */
@Component
public class XMLProcessor extends AbstractProcessor {

    private final Logger logger = LoggerFactory.getLogger(XMLProcessor.class);
    private final Validator validator;

    @Value("${error.file.path.xml}")
    private String errorFilePathXML;

    public XMLProcessor(Validator validator, ProcessingSummaryService processingSummaryService,
                        AuditLogService auditLogService, OutputFileService outputFileService) {
        super(processingSummaryService, auditLogService, outputFileService, "xml");
        this.validator = validator;
    }

    /**
     * Processes the XML file at the given path, logs processing information, and generates output files.
     *
     * @param xmlFilePath Path to the XML file to be processed.
     */
    @Override
    public void processFile(Path xmlFilePath) {
        totalRecords.getAndSet(0);
        fileName = xmlFilePath.getFileName().toString();
        ProcessingSummary summary = new ProcessingSummary(fileName);
        LocalDateTime startTime = LocalDateTime.now();

        try {
            // Log start of file processing
            auditLogService.logStart(fileName, fileType);
            List<XmlRecordDto> successfulRecords = processXmlInChunks(xmlFilePath);

            outputFileService.generateOutputFile(new ArrayList<>(successfulRecords), fileType);
            processingSummaryService.generateProcessingSummary(fileName, totalRecords.get(), successfulRecords.size(), startTime, fileType);

            logger.info("File processed successfully: {}", fileName);
            auditLogService.logEnd(fileName, "SUCCESS", null);
        } catch (Exception e) {
            logger.error("Error processing file: {}", e.getMessage());
            summary.setFailedRecords(totalRecords.get());
            auditLogService.logEnd(fileName, "FAILED", e.getMessage());
        }
    }

    /**
     * Processes the XML file in chunks and returns a list of successfully processed records.
     *
     * @param xmlFilePath Path to the XML file to be processed.
     * @return List of successfully processed XmlRecordDto objects.
     */
    public List<XmlRecordDto> processXmlInChunks(Path xmlFilePath) {
        try {
            File file = xmlFilePath.toFile();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            var document = builder.parse(file);
            var nodeList = document.getElementsByTagName("Record");
            Path errorLogPath = Path.of(errorFilePathXML + "error_" + fileName);
            List<FailedRecord> failedRecords = new ArrayList<>();


            List<XmlRecordDto> allRecords = new ArrayList<>();
            for (int i = 0; i < nodeList.getLength(); i++) {
                var node = nodeList.item(i);
                try {
                    XmlRecordDto dto = parseRecord(node);
                    allRecords.add(dto);
                } catch (Exception e) {
                    failedRecords.add(new FailedRecord("Record#" + i, e.getMessage()));
                }
            }


            totalRecords.getAndSet(nodeList.getLength());
            List<List<XmlRecordDto>> chunks = splitIntoChunks(allRecords);

            List<Future<List<XmlRecordDto>>> futures = new ArrayList<>();

            for (List<XmlRecordDto> chunk : chunks) {
                futures.add(executorService.submit(() -> processChunk(chunk, failedRecords)));
            }

            List<XmlRecordDto> processedRecords = new ArrayList<>();
            for (Future<List<XmlRecordDto>> future : futures) {
                processedRecords.addAll(future.get());
            }

            if (failedRecords.size() > 0) {
                CommonFailedRecordsLogger.logFailedRecords(failedRecords, errorLogPath, "xml");
            }

            return processedRecords;
        } catch (Exception e) {
            throw new RuntimeException("Failed to process XML: " + e.getMessage(), e);
        }
    }

    /**
     * Splits a list of records into chunks for parallel processing.
     *
     * @param records The list of records to split.
     * @return A list of lists, where each inner list is a chunk of records.
     */
    private List<List<XmlRecordDto>> splitIntoChunks(List<XmlRecordDto> records) {
        List<List<XmlRecordDto>> chunks = new ArrayList<>();
        for (int i = 0; i < records.size(); i += CHUNK_SIZE) {
            chunks.add(records.subList(i, Math.min(i + CHUNK_SIZE, records.size())));
        }
        return chunks;
    }

    /**
     * Processes a chunk of records and returns the list of valid records.
     *
     * @param chunkRecords The chunk of records to process.
     * @param failedRecords The list to track failed records.
     * @return A list of valid XmlRecordDto objects.
     */
    public List<XmlRecordDto> processChunk(List<XmlRecordDto> chunkRecords, List<FailedRecord> failedRecords) {
        List<XmlRecordDto> validRecords = new ArrayList<>();

        for (XmlRecordDto record : chunkRecords) {
            boolean success = processRecordWithRetry(record, failedRecords);
            if (success) {
                validRecords.add(record);
            }
        }

        return validRecords;
    }

    /**
     * Processes a single record with retries, validating and transforming data.
     *
     * @param record The record to process.
     * @param failedRecords The list to track failed records.
     * @return true if the record was processed successfully, false otherwise.
     */
    public boolean processRecordWithRetry(XmlRecordDto record, List<FailedRecord> failedRecords) {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                Set<ConstraintViolation<XmlRecordDto>> violations = validator.validate(record);
                if (!violations.isEmpty()) {
                    throw new RuntimeException("Validation failed: " +
                            violations.stream()
                                    .map(ConstraintViolation::getMessage)
                                    .collect(Collectors.joining(", ")));
                }

                transformData(record);

                return true;
            } catch (Exception e) {
                attempt++;
                if (attempt == MAX_RETRIES) {
                    failedRecords.add(new FailedRecord(record, e.getMessage()));
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Transforms the data of a record by formatting dates and adjusting amounts based on the currency.
     *
     * @param record The record to transform.
     */
    public void transformData(XmlRecordDto record) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        LocalDate date = LocalDate.parse(record.getDate(), inputFormatter);
        record.setDate(date.format(outputFormatter));

        record.setAmount(BigDecimal.valueOf(record.getAmount())
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue());

        if ("USD".equalsIgnoreCase(record.getCurrency())) {
            record.setAmount(record.getAmount() * 0.85);
            record.setCurrency("EUR");
        }

        if (record.getName() == null || record.getName().isEmpty()) {
            record.setName("Unknown");
        }
    }

    /**
     * Parses a record from an XML node and converts it into an XmlRecordDto object.
     *
     * @param node The XML node representing the record.
     * @return The parsed XmlRecordDto object.
     * @throws IllegalArgumentException if required fields are missing.
     */
    public XmlRecordDto parseRecord(org.w3c.dom.Node node) throws IllegalArgumentException {
        XmlRecordDto dto = new XmlRecordDto();
        var element = (org.w3c.dom.Element) node;

        // Parse and validate fields
        String idText = getElementTextContent(element, "id");
        String name = getElementTextContent(element, "name");
        String amountText = getElementTextContent(element, "amount");
        String currency = getElementTextContent(element, "currency");
        String date = getElementTextContent(element, "date");

        // Validate required fields
        StringBuilder missingFields = new StringBuilder();

        if (idText == null || idText.isEmpty()) {
            missingFields.append("id,");
        }
        if (name == null || name.isEmpty()) {
            missingFields.append("name,");
        }
        if (amountText == null || amountText.isEmpty()) {
            missingFields.append("amount,");
        }
        if (currency == null || currency.isEmpty()) {
            missingFields.append("currency,");
        }

        // If any fields are missing, throw an exception with the details
        if (missingFields.length() > 0) {
            // Remove trailing comma and space
            missingFields.setLength(missingFields.length() - 1);
            throw new IllegalArgumentException(
                    "Missing fields: " + missingFields
            );
        }

        // Assign values to the DTO
        dto.setId(Integer.parseInt(idText));
        dto.setName(name);
        dto.setAmount(Double.parseDouble(amountText));
        dto.setCurrency(currency);
        dto.setDate(date); // Optional, can be null or empty

        return dto;
    }

    /**
     * Retrieves the text content of an element with the given tag name.
     *
     * @param element The XML element.
     * @param tagName The tag name of the element.
     * @return The text content of the element, or null if the element is missing.
     */
    private String getElementTextContent(org.w3c.dom.Element element, String tagName) {
        var nodeList = element.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null; // Return null if the element is missing
    }
}
