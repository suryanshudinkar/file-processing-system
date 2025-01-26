package com.fileprocessingapplication.processor.outputprocessors;

import com.fileprocessingapplication.dto.CsvRecordDto;
import com.fileprocessingapplication.dto.JsonRecordDto;
import com.fileprocessingapplication.dto.XmlRecordDto;
import com.opencsv.CSVWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.nio.file.Path;
import java.util.List;

@Component
public class CsvOutputProcessor implements OutputProcessor {

    @Value("${input.csv.header}")
    private String header;

    @Override
    public void writeOutput(List<Object> records, Path outputPath) throws Exception {
        try (CSVWriter writer = new CSVWriter(new FileWriter(outputPath.toFile()))) {
            // Write header
            writer.writeNext(header.split(","));

            // Write records
            for (Object object : records) {
                CsvRecordDto record;

                if (object instanceof JsonRecordDto) {
                    record = convertJsonDTOToCsvDTO((JsonRecordDto) object);
                } else if (object instanceof XmlRecordDto) {
                    record = convertXmlDTOToCsvDTO((XmlRecordDto) object);
                } else {
                    record = (CsvRecordDto) object;
                }

                writer.writeNext(new String[]{
                        record.getId().toString(),
                        record.getName(),
                        record.getAmount().toString(),
                        record.getCurrency(),
                        record.getDate()
                });
            }
        }
    }

    // Method to convert JsonRecordDto to CsvRecordDto
    public static CsvRecordDto convertJsonDTOToCsvDTO(JsonRecordDto jsonRecordDto) {
        CsvRecordDto csvRecordDto = new CsvRecordDto();
        csvRecordDto.setId(jsonRecordDto.getId());
        csvRecordDto.setName(jsonRecordDto.getName());
        csvRecordDto.setAmount(jsonRecordDto.getAmount());
        csvRecordDto.setCurrency(jsonRecordDto.getCurrency());
        csvRecordDto.setDate(jsonRecordDto.getDate());
        return csvRecordDto;
    }

    // Method to convert XmlRecordDto to CsvRecordDto
    public static CsvRecordDto convertXmlDTOToCsvDTO(XmlRecordDto xmlRecordDto) {
        CsvRecordDto csvRecordDto = new CsvRecordDto();
        csvRecordDto.setId(xmlRecordDto.getId());
        csvRecordDto.setName(xmlRecordDto.getName());
        csvRecordDto.setAmount(xmlRecordDto.getAmount());
        csvRecordDto.setCurrency(xmlRecordDto.getCurrency());
        csvRecordDto.setDate(xmlRecordDto.getDate());
        return csvRecordDto;
    }



}
