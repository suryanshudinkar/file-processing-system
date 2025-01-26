package com.fileprocessingapplication.processor.outputprocessors;

import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Component
public class FileNameTemplateProcessor {
    public static String processTemplate(String template, Map<String, String> values) {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            template = template.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return template;
    }

    public static String generateFileName(String recordType, String format, int batchId) {
        String template = "${record_type}_${date}_${batch_id}.${format}";
        Map<String, String> values = Map.of(
                "record_type", recordType,
                "date", new SimpleDateFormat("yyyyMMdd").format(new Date()),
                "batch_id", String.valueOf(batchId),
                "format", format
        );
        return processTemplate(template, values);
    }
}
