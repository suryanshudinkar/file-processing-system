package com.fileprocessingapplication.enums;

public enum RoutingKey {
    CSV_QUEUE("csv_queue"),
    JSON_QUEUE("json_queue"),
    XML_QUEUE("xml_queue");

    final String routingKey;
    RoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public String getKey() {
        return routingKey;
    }
}
