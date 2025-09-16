package com.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NseResponse {

    @JsonProperty("filtered")
    private Records filtered;

    // Getters and setters
    public Records getFiltered() {
        return filtered;
    }

    public void setFiltered(Records filtered) {
        this.filtered = filtered;
    }

    private long timestamp;

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "NseResponse{" +
                "filtered=" + filtered +
                ", timestamp=" + timestamp +
                '}';
    }
}
