package com.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Data {

    @JsonProperty("strikePrice")
    private double strikePrice;

    @JsonProperty("CE")
    private Option callOption;

    @JsonProperty("PE")
    private Option putOption;

    @JsonProperty("expiryDate")
    private String expiryDate;

    // Getters and setters
    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public double getStrikePrice() {
        return strikePrice;
    }

    public void setStrikePrice(double strikePrice) {
        this.strikePrice = strikePrice;
    }

    public Option getCallOption() {
        return callOption;
    }

    public void setCallOption(Option callOption) {
        this.callOption = callOption;
    }

    public Option getPutOption() {
        return putOption;
    }

    public void setPutOption(Option putOption) {
        this.putOption = putOption;
    }

    @Override
    public String toString() {
        return "Data{" +
                "strikePrice=" + strikePrice +
                ", callOption=" + callOption +
                ", putOption=" + putOption +
                '}';
    }
}
