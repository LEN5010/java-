package com.quanttrading.preprocessing;

import com.quanttrading.model.ProcessedData;
import com.quanttrading.model.StockData;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StandardizationProcessor implements DataPreprocessor {
    private double mean;
    private double stdDev;

    @Override
    public void setParameters(Map<String, Double> params) {
        // No parameters needed for standardization
    }

    @Override
    public List<ProcessedData> process(List<StockData> rawData) {
        double[] closes = rawData.stream().mapToDouble(StockData::getClose).toArray();
        this.mean = new Mean().evaluate(closes);
        this.stdDev = new StandardDeviation().evaluate(closes);

        return rawData.stream()
                .map(d -> new ProcessedData(
                        (d.getClose() - mean) / stdDev,
                        d.getDate()
                ))
                .collect(Collectors.toList());
    }

    public double getMean() {
        return mean;
    }

    public double getStdDev() {
        return stdDev;
    }
}