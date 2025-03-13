package com.quanttrading.evaluation.impl;

import com.quanttrading.evaluation.EvaluationMetric;
import com.quanttrading.model.StockData;
import com.quanttrading.model.TradeSignal;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 均方误差 (Mean Squared Error)
 */
public class MeanSquaredError implements EvaluationMetric {

    @Override
    public double calculate(double[] predictions, double[] actuals) {
        if (predictions.length != actuals.length) {
            throw new IllegalArgumentException("Predictions and actuals must have the same length");
        }

        int n = predictions.length;
        double sumSquaredError = 0.0;

        for (int i = 0; i < n; i++) {
            double error = predictions[i] - actuals[i];
            sumSquaredError += error * error;
        }

        return sumSquaredError / n;
    }

    @Override
    public double calculate(List<StockData> stockData, Map<LocalDate, TradeSignal> signals, Map<String, Object> parameters) {
        // 对于MSE，这个方法不适用于交易信号评估
        throw new UnsupportedOperationException("MSE is not applicable for trade signal evaluation");
    }

    @Override
    public String getName() {
        return "Mean Squared Error";
    }

    @Override
    public String getDescription() {
        return "Average of the squared differences between predicted and actual values";
    }
}