// src/main/java/com/quanttrading/model/ProcessedData.java
package com.quanttrading.model;

import java.time.LocalDate;

public class ProcessedData {
    private final double value;
    private final LocalDate date;

    public ProcessedData(double value, LocalDate date) {
        this.value = value;
        this.date = date;
    }

    public double getValue() { return value; }
    public LocalDate getDate() { return date; }

    @Override
    public String toString() {
        return "ProcessedData{" +
                "value=" + value +
                ", date=" + date +
                '}';
    }
}