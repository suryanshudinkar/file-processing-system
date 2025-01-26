package com.fileprocessingapplication.processor.inputprocessor;

import com.fileprocessingapplication.dto.CsvRecordDto;
import com.fileprocessingapplication.processor.inputprocessors.CSVProcessor;
import com.fileprocessingapplication.service.AuditLogService;
import com.fileprocessingapplication.service.OutputFileService;
import com.fileprocessingapplication.service.ProcessingSummaryService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.io.BufferedReader;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestPropertySource(properties = {
        "input.csv.header=id,name,amount,currency,date"
})
@SpringBootTest
class CSVProcessorTest {

    private CSVProcessor csvProcessor;

    @Mock
    private Validator validator;

    @Mock
    private ProcessingSummaryService processingSummaryService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private OutputFileService outputFileService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        csvProcessor = new CSVProcessor(validator, processingSummaryService, auditLogService, outputFileService);
    }

    @Test
    void testTransformData_ValidRecord() {
        // Create a record to transform
        CsvRecordDto record = new CsvRecordDto();
        record.setDate("12/01/2023");
        record.setAmount(100.0);
        record.setCurrency("USD");

        // Call transformData
        csvProcessor.transformData(record);

        // Verify transformation
        assertEquals("2023-12-01", record.getDate());
        assertEquals("EUR", record.getCurrency());
        assertEquals(85.0, record.getAmount()); // USD to EUR conversion
    }

    @Test
    void testParseRecord_ValidRecord() {
        // Mock a CSVRecord
        CSVRecord record = mock(CSVRecord.class);
        when(record.get("id")).thenReturn("1");
        when(record.get("name")).thenReturn("John");
        when(record.get("amount")).thenReturn("100.50");
        when(record.get("currency")).thenReturn("USD");
        when(record.get("date")).thenReturn("12/01/2023");

        // Parse the record
        CsvRecordDto dto = csvProcessor.parseRecord(record);

        // Verify parsing
        assertEquals(1, dto.getId());
        assertEquals("John", dto.getName());
        assertEquals(100.50, dto.getAmount());
        assertEquals("USD", dto.getCurrency());
        assertEquals("12/01/2023", dto.getDate());
    }

    @Test
    void testParseRecord_MissingFields() {
        // Mock a CSVRecord with missing fields
        CSVRecord record = mock(CSVRecord.class);
        when(record.get("id")).thenReturn("");
        when(record.get("name")).thenReturn("John");

        // Expect exception
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            csvProcessor.parseRecord(record);
        });

        // Verify exception message
        assertTrue(exception.getMessage().contains("Missing fields: id amount currency date"));
    }

    @Test
    void testProcessRecordWithRetry_SuccessfulValidation() {
        // Create a record
        CsvRecordDto record = new CsvRecordDto();
        record.setId(1);
        record.setName("John");
        record.setAmount(100.0);
        record.setCurrency("USD");
        record.setDate("12/01/2023");

        // Mock validator
        when(validator.validate(record)).thenReturn(Set.of());

        // Test processing
        boolean result = csvProcessor.processRecordWithRetry(record, List.of());

        // Verify
        assertTrue(result);
    }
}




