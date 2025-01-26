package com.fileprocessingapplication.processor.outputprocessors;

import com.fileprocessingapplication.dto.CsvRecordDto;
import com.fileprocessingapplication.dto.JsonRecordDto;
import com.fileprocessingapplication.dto.XmlRecordDto;
import com.fileprocessingapplication.util.XMLRecordListWrapper;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class XmlOutputProcessor implements OutputProcessor {


    @Override
    public void writeOutput(List<Object> records, Path outputPath) throws Exception {
        JAXBContext context = JAXBContext.newInstance(XMLRecordListWrapper.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        List<XmlRecordDto> xmlRecords = new ArrayList<>();

        for (Object record : records) {
            if (record instanceof CsvRecordDto) {
                XmlRecordDto xmlRecordDto = convertCsvDTOToXmlDTO((CsvRecordDto) record);
                xmlRecords.add(xmlRecordDto);
            } else if (record instanceof XmlRecordDto) {
                xmlRecords.add((XmlRecordDto) record);
            } else if (record instanceof JsonRecordDto) {
                XmlRecordDto xmlRecordDto = convertJsonDTOToXmlDTO((JsonRecordDto) record);
                xmlRecords.add(xmlRecordDto);
            }
        }

//      Wrap list in a container for XML
        XMLRecordListWrapper wrapper = new XMLRecordListWrapper(xmlRecords);
        marshaller.marshal(wrapper, outputPath.toFile());
    }

    // Method to convert CsvRecordDto to XmlRecordDto
    public static XmlRecordDto convertCsvDTOToXmlDTO(CsvRecordDto csvRecordDto) {
        XmlRecordDto xmlRecordDto = new XmlRecordDto();
        xmlRecordDto.setId(csvRecordDto.getId());
        xmlRecordDto.setName(csvRecordDto.getName());
        xmlRecordDto.setAmount(csvRecordDto.getAmount());
        xmlRecordDto.setCurrency(csvRecordDto.getCurrency());
        xmlRecordDto.setDate(csvRecordDto.getDate());
        return xmlRecordDto;
    }

    // Method to convert CsvRecordDto to XmlRecordDto
    public static XmlRecordDto convertJsonDTOToXmlDTO(JsonRecordDto dto) {
        XmlRecordDto xmlRecordDto = new XmlRecordDto();
        xmlRecordDto.setId(dto.getId());
        xmlRecordDto.setName(dto.getName());
        xmlRecordDto.setAmount(dto.getAmount());
        xmlRecordDto.setCurrency(dto.getCurrency());
        xmlRecordDto.setDate(dto.getDate());
        return xmlRecordDto;
    }
}
