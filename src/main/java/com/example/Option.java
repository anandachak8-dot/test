package com.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Option {

    @JsonProperty("strikePrice")
    private double strikePrice;

    @JsonProperty("openInterest")
    private double openInterest;

    // Getters and setters
    public double getStrikePrice() {
        return strikePrice;
    }

    public void setStrikePrice(double strikePrice) {
        this.strikePrice = strikePrice;
    }

    public double getOpenInterest() {
        return openInterest;
    }

    public void setOpenInterest(double openInterest) {
        this.openInterest = openInterest;
    }

    @Override
    public String toString() {
        return "Option{" +
                "strikePrice=" + strikePrice +
                ", openInterest=" + openInterest +
                '}';
    }
}
