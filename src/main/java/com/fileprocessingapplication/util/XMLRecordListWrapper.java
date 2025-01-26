package com.fileprocessingapplication.util;

import com.fileprocessingapplication.dto.XmlRecordDto;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.List;

@XmlRootElement(name = "records")
public class XMLRecordListWrapper {
    private List<XmlRecordDto> records;

    public XMLRecordListWrapper() {
    }

    public XMLRecordListWrapper(List<XmlRecordDto> records) {
        this.records = records;
    }

    @XmlElement(name = "record")
    public List<XmlRecordDto> getRecords() {
        return records;
    }

    public void setRecords(List<XmlRecordDto> records) {
        this.records = records;
    }
}