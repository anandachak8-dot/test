package com.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NseResponse {

    @JsonProperty("records")
    private Records records;

    private long timestamp;

    public Records getRecords() {
        return records;
    }

    public void setRecords(Records records) {
        this.records = records;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
