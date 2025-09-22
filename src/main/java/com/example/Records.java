package com.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Records {

    @JsonProperty("expiryDates")
    private List<String> expiryDates;

    @JsonProperty("data")
    private List<Data> data;

    @JsonProperty("underlyingValue")
    private double underlyingValue;

    @JsonProperty("strikePrices")
    private List<Double> strikePrices;

    public List<String> getExpiryDates() {
        return expiryDates;
    }

    public void setExpiryDates(List<String> expiryDates) {
        this.expiryDates = expiryDates;
    }

    public List<Data> getData() {
        return data;
    }

    public void setData(List<Data> data) {
        this.data = data;
    }

    public double getUnderlyingValue() {
        return underlyingValue;
    }

    public void setUnderlyingValue(double underlyingValue) {
        this.underlyingValue = underlyingValue;
    }

    public List<Double> getStrikePrices() {
        return strikePrices;
    }

    public void setStrikePrices(List<Double> strikePrices) {
        this.strikePrices = strikePrices;
    }
}
