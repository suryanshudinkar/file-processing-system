package com.fileprocessingapplication.service;

import com.fileprocessingapplication.event.FileProcessingEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

@Slf4j
@Service
public class FileStorageService {

    @Value("${file.upload.path.csv}")
    private String uploadPathCSV;

    @Value("${file.upload.path.json}")
    private String uploadPathJSON;

    @Value("${file.upload.path.xml}")
    private String uploadPathXML;

    public FileProcessingEvent saveFile(MultipartFile file){
        try {
            String uploadPath = getUploadPath(file);
            String fileType = StringUtils.getFilenameExtension(Objects.requireNonNull(file.getOriginalFilename()));


            // Ensure storage directory exists
            Path storageDir = Paths.get(uploadPath);
            if (!storageDir.toFile().exists()) {
                storageDir.toFile().mkdirs();
            }

            // Copy file to the desired location
            Path copyLocation = Path.of(uploadPath, file.getOriginalFilename());
            Files.copy(file.getInputStream(), copyLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("File saved to {}", copyLocation);
            return new FileProcessingEvent(copyLocation.toString(),fileType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getUploadPath(MultipartFile file) {
        String fileType = StringUtils.getFilenameExtension(Objects.requireNonNull(file.getOriginalFilename()));
        switch (fileType) {
            case "csv":
                return uploadPathCSV;
            case "json":
                return uploadPathJSON;
            case "xml":
                 return uploadPathXML;
            default:
                 throw new IllegalArgumentException("Unsupported file type: " + fileType);
        }
    }
}