package com.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NseResponse {

    @JsonProperty("records")
    private Records records;

    // Getters and setters
    public Records getRecords() {
        return records;
    }

    public void setRecords(Records records) {
        this.records = records;
    }

    @Override
    public String toString() {
        return "NseResponse{" +
                "records=" + records +
                '}';
    }
}
