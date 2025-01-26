package com.fileprocessingapplication.processor.inputprocessor;

import com.fileprocessingapplication.dto.XmlRecordDto;
import com.fileprocessingapplication.processor.inputprocessors.XMLProcessor;
import com.fileprocessingapplication.service.AuditLogService;
import com.fileprocessingapplication.service.OutputFileService;
import com.fileprocessingapplication.service.ProcessingSummaryService;
import com.fileprocessingapplication.util.FailedRecord;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class XMLProcessorTest {

    private XMLProcessor xmlProcessor;

    @Mock
    private ProcessingSummaryService processingSummaryService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private OutputFileService outputFileService;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        this.xmlProcessor = new XMLProcessor(mock(Validator.class), processingSummaryService, auditLogService, outputFileService);
    }

    @Test
    void testProcessRecordWithRetry_validationFailure() {
        // Create a record with missing required fields to simulate a validation failure
        XmlRecordDto invalidRecord = new XmlRecordDto();
        invalidRecord.setId(0); // Invalid record without proper ID
        invalidRecord.setName(""); // Invalid name
        invalidRecord.setAmount((double) 0);
        invalidRecord.setCurrency(""); // Invalid currency

        List<FailedRecord> failedRecords = new ArrayList<>();
        boolean result = xmlProcessor.processRecordWithRetry(invalidRecord, failedRecords);

        assertFalse(result, "Expected the record to fail validation");
        assertEquals(1, failedRecords.size(), "Expected one failed record due to validation error");
    }

    @Test
    void testProcessRecordWithRetry_successfulProcessing() {
        // Create a valid record
        XmlRecordDto validRecord = new XmlRecordDto();
        validRecord.setId(1);
        validRecord.setName("Test Name");
        validRecord.setAmount(100.0);
        validRecord.setCurrency("USD");
        validRecord.setDate("2022-12-31");

        List<FailedRecord> failedRecords = new ArrayList<>();
        boolean result = xmlProcessor.processRecordWithRetry(validRecord, failedRecords);

        assertTrue(result, "Expected the record to be processed successfully");
        assertTrue(failedRecords.isEmpty(), "Expected no failed records");
    }
}