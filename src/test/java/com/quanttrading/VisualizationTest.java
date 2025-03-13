package com.quanttrading;

import com.quanttrading.model.StockData;
import com.quanttrading.visualization.ChartGenerator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VisualizationTest {
    public static void main(String[] args) {
        // 创建测试数据
        List<StockData> stockData = createTestStockData();
        List<Double> predictions = createTestPredictions();
        List<LocalDate> dates = getDatesFromStockData(stockData);

        // 测试预测可视化
        ChartGenerator.createPredictionChart("Test Prediction Chart", stockData, predictions, dates);

        // 测试特征重要性可视化
        Map<String, Double> featureImportance = createTestFeatureImportance();
        ChartGenerator.createFeatureImportanceChart("Test Feature Importance", featureImportance);
    }

    private static List<StockData> createTestStockData() {
        List<StockData> data = new ArrayList<>();
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        double price = 100.0;

        for (int i = 0; i < 30; i++) {
            price += (Math.random() - 0.5) * 2;
            data.add(new StockData("TEST", startDate.plusDays(i), price, price+1, price-1, price, 1000));
        }

        return data;
    }

    private static List<Double> createTestPredictions() {
        List<Double> predictions = new ArrayList<>();
        double price = 100.0;

        for (int i = 0; i < 30; i++) {
            price += (Math.random() - 0.4) * 2; // 稍微偏向上涨
            predictions.add(price);
        }

        return predictions;
    }

    private static List<LocalDate> getDatesFromStockData(List<StockData> stockData) {
        List<LocalDate> dates = new ArrayList<>();
        for (StockData data : stockData) {
            dates.add(data.getDate());
        }
        return dates;
    }

    private static Map<String, Double> createTestFeatureImportance() {
        Map<String, Double> importance = new HashMap<>();
        importance.put("Price", 0.35);
        importance.put("MA5", 0.25);
        importance.put("MA10", 0.15);
        importance.put("MA20", 0.10);
        importance.put("Volume", 0.08);
        importance.put("RSI", 0.05);
        importance.put("MACD", 0.02);
        return importance;
    }
}