package com.fileprocessingapplication.util;

import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@XmlRootElement // Required for JAXB to marshal/unmarshal this class
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingSummary {
    private String fileName;
    private long totalRecords;
    private long successfulRecords;
    private long failedRecords;
    private long processingTimeInSeconds;

    public ProcessingSummary(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String toString() {
        return String.format(
                "Processing Summary for File: %s%n" +
                        "Total Records: %d%n" +
                        "Successful Records: %d%n" +
                        "Failed Records: %d%n" +
                        "Processing Time: %ss%n",
                fileName, totalRecords, successfulRecords, failedRecords,
                processingTimeInSeconds
        );
    }
}
