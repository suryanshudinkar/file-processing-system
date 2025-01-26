package com.fileprocessingapplication.util;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
public class FailedRecord {
    private final Object record;
    private final String errorMessage;

    public FailedRecord(Object record, String errorMessage) {
        this.record = record;
        this.errorMessage = errorMessage;
    }
}
