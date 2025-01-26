package com.fileprocessingapplication.controller;

import com.fileprocessingapplication.service.FileProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/api/v1/process")
public class FileProcessingController {

    @Autowired
    private FileProcessingService fileProcessingService;

    @PostMapping("/file")
    public ResponseEntity<?> processFile(@RequestParam("file") MultipartFile file) {
        try {
            // Determine file type
            String fileType = StringUtils.getFilenameExtension(Objects.requireNonNull(file.getOriginalFilename()));
            if (fileType == null || !(fileType.equalsIgnoreCase("csv") || fileType.equalsIgnoreCase("json") || fileType.equalsIgnoreCase("xml"))) {
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body("Unsupported file type");
            }

            if (file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is empty");
            }

            // Handle potential file size limits
            long maxSizeInBytes = 1024L * 1024L * 1024L; // 1GB in bytes
            if (file.getSize() > maxSizeInBytes) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body("File size exceeds limit (1GB)");
            }

            fileProcessingService.saveFileAndPublishEvent(file);
            return ResponseEntity.status(HttpStatus.OK).body("File processing initiated.");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
