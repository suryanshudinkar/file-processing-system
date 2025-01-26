package com.fileprocessingapplication.processor.inputprocessor;

import com.fileprocessingapplication.dto.JsonRecordDto;
import com.fileprocessingapplication.processor.inputprocessors.JSONProcessor;
import com.fileprocessingapplication.service.AuditLogService;
import com.fileprocessingapplication.service.OutputFileService;
import com.fileprocessingapplication.service.ProcessingSummaryService;
import com.fileprocessingapplication.util.FailedRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JSONProcessorTest {

    @Mock
    private ProcessingSummaryService processingSummaryService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private OutputFileService outputFileService;

    @InjectMocks
    private JSONProcessor jsonProcessor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testIsValidRecord_ValidRecord() {
        JsonRecordDto record = new JsonRecordDto();
        record.setId(1);
        record.setName("Test Record");
        record.setAmount(100.0);
        record.setCurrency("USD");
        record.setDate("01/01/2023");

        boolean isValid = jsonProcessor.isValidRecord(record);

        assertTrue(isValid);
    }

    @Test
    void testIsValidRecord_InvalidRecord() {
        JsonRecordDto record = new JsonRecordDto();
        record.setId(null); // Missing ID

        boolean isValid = jsonProcessor.isValidRecord(record);

        assertFalse(isValid);
    }

    @Test
    void testProcessRecordWithRetry_Failure() {
        JsonRecordDto record = new JsonRecordDto(); // Invalid record
        record.setId(null);

        List<FailedRecord> failedRecords = new ArrayList<>();
        boolean result = jsonProcessor.processRecordWithRetry(record, failedRecords);

        assertFalse(result);
        assertEquals(1, failedRecords.size());
    }
}
