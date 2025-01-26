package com.fileprocessingapplication.processor.inputprocessors;

import com.fileprocessingapplication.service.AuditLogService;
import com.fileprocessingapplication.service.OutputFileService;
import com.fileprocessingapplication.service.ProcessingSummaryService;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractProcessor {
    protected static int CHUNK_SIZE = 100; // The size of each chunk for processing
    protected static int THREAD_POOL_SIZE = 10;
    protected static int MAX_RETRIES = 3;

    protected final AtomicLong totalRecords = new AtomicLong(0);
    protected String fileName;
    protected final String fileType;

    protected final ProcessingSummaryService processingSummaryService;
    protected final AuditLogService auditLogService;
    protected final OutputFileService outputFileService;
    protected final ExecutorService executorService;

    public AbstractProcessor(
            ProcessingSummaryService processingSummaryService,
            AuditLogService auditLogService,
            OutputFileService outputFileService,
            String fileType
    ) {
        this.processingSummaryService = processingSummaryService;
        this.auditLogService = auditLogService;
        this.outputFileService = outputFileService;
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        this.fileType = fileType;
    }

    // Common methods shared by all processors can go here
    protected void logProcessingStart() {
        System.out.println("Starting processing for file type: " + fileType);
    }

    protected void updateTotalRecords(long count) {
        totalRecords.addAndGet(count);
    }

    public abstract void processFile(Path filePath);
}

