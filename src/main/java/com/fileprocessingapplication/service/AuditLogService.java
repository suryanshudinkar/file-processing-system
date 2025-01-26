package com.fileprocessingapplication.service;

import com.fileprocessingapplication.dao.AuditLogRepository;
import com.fileprocessingapplication.model.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service class responsible for handling the logging of audit information related to file processing.
 * It tracks the start and end times of file processing, as well as the status and any error details.
 */
@Service
public class AuditLogService {
    @Autowired
    private AuditLogRepository auditLogRepository;

    /**
     * Logs the start of a file processing operation.
     * This method records the file name, file type, start time, and status as "IN_PROGRESS".
     *
     * @param fileName the name of the file being processed
     * @param fileType the type of the file being processed
     */
    public void logStart(String fileName, String fileType) {
        // Create a new AuditLog object to store the log details
        AuditLog log = new AuditLog();
        log.setFileName(fileName);
        log.setFileType(fileType);
        log.setStartTime(LocalDateTime.now());
        log.setStatus("IN_PROGRESS");

        // Save the log to the repository
        auditLogRepository.save(log);
    }

    /**
     * Logs the end of a file processing operation.
     * This method updates the status of the file processing to either "SUCCESS" or "FAILED",
     * and logs any error details if the status is "FAILED".
     *
     * @param fileName    the name of the file being processed
     * @param status      the status of the file processing ("SUCCESS" or "FAILED")
     * @param errorDetails the error details if the processing failed, null if successful
     */
    public void logEnd(String fileName, String status, String errorDetails) {
        // Retrieve the most recent log for the given file name (assuming one log entry per file)
        AuditLog log = auditLogRepository.findAllByFileName(fileName).get(0);
        log.setEndTime(LocalDateTime.now());
        log.setStatus(status);
        log.setErrorDetails(errorDetails);
        auditLogRepository.save(log);
    }
}
