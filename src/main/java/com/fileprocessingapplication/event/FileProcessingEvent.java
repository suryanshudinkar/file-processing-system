package com.fileprocessingapplication.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class FileProcessingEvent {
    private String filePath;
    private String fileType;
}

