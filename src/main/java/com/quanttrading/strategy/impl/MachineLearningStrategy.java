package com.quanttrading.strategy.impl;

import com.quanttrading.ml.MachineLearningAlgorithm;
import com.quanttrading.model.ProcessedData;
import com.quanttrading.model.StockData;
import com.quanttrading.model.TradeSignal;
import com.quanttrading.strategy.TradingStrategy;
import com.quanttrading.visualization.ChartGenerator;
import com.quanttrading.ml.impl.SimpleRandomForestAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于机器学习的交易策略
 */
public class MachineLearningStrategy implements TradingStrategy {
    private static final Logger logger = LoggerFactory.getLogger(MachineLearningStrategy.class);

    private MachineLearningAlgorithm algorithm;
    private int lookbackWindow;
    private double buyThreshold;
    private double sellThreshold;
    private Map<LocalDate, double[]> featureCache;
    private List<Double> predictions;
    private List<LocalDate> predictionDates;
    private String[] featureNames;

    public MachineLearningStrategy(MachineLearningAlgorithm algorithm) {
        this.algorithm = algorithm;
        this.lookbackWindow = 10;
        this.buyThreshold = 0.01;  // 预测收益率 > 1%
        this.sellThreshold = -0.01; // 预测收益率 < -1%
        this.featureCache = new HashMap<>();
        this.predictions = new ArrayList<>();
        this.predictionDates = new ArrayList<>();
    }

    public void setFeatureNames(String[] featureNames) {
        this.featureNames = featureNames;

        // 如果算法支持，设置特征名称
        if (algorithm instanceof SimpleRandomForestAlgorithm) {
            ((SimpleRandomForestAlgorithm) algorithm).setFeatureNames(featureNames);
        }
    }

    @Override
    public TradeSignal generateSignal(ProcessedData currentData, List<ProcessedData> historicalData) {
        if (historicalData.size() < lookbackWindow) {
            logger.warn("Not enough historical data for ML prediction. Need at least {} data points.", lookbackWindow);
            return TradeSignal.HOLD;
        }

        // 获取当前日期的特征
        LocalDate date = currentData.getDate();
        double[] features;

        if (featureCache.containsKey(date)) {
            features = featureCache.get(date);
        } else {
            features = extractFeatures(currentData, historicalData);
            featureCache.put(date, features);
        }

        // 使用模型预测
        double prediction = algorithm.predict(features);

        // 收集预测结果
        predictions.add(prediction);
        predictionDates.add(date);

        // 根据预测结果生成交易信号
        if (prediction > buyThreshold) {
            return TradeSignal.BUY;
        } else if (prediction < sellThreshold) {
            return TradeSignal.SELL;
        } else {
            return TradeSignal.HOLD;
        }
    }

    /**
     * 从数据中提取特征
     */
    private double[] extractFeatures(ProcessedData currentData, List<ProcessedData> historicalData) {
        List<Double> features = new ArrayList<>();

        // 添加当前价格
        features.add(currentData.getValue());

        // 添加历史价格
        for (int i = 0; i < Math.min(lookbackWindow, historicalData.size()); i++) {
            features.add(historicalData.get(i).getValue());
        }

        // 计算技术指标作为特征
        if (historicalData.size() >= 5) {
            // 5日移动平均
            double ma5 = historicalData.stream().limit(5).mapToDouble(ProcessedData::getValue).average().orElse(0);
            features.add(ma5);
        }

        if (historicalData.size() >= 10) {
            // 10日移动平均
            double ma10 = historicalData.stream().limit(10).mapToDouble(ProcessedData::getValue).average().orElse(0);
            features.add(ma10);
        }

        if (historicalData.size() >= 20) {
            // 20日移动平均
            double ma20 = historicalData.stream().limit(20).mapToDouble(ProcessedData::getValue).average().orElse(0);
            features.add(ma20);
        }

        // 转换为数组
        double[] featureArray = new double[features.size()];
        for (int i = 0; i < features.size(); i++) {
            featureArray[i] = features.get(i);
        }

        return featureArray;
    }

    @Override
    public void setParameters(Map<String, Double> params) {
        if (params.containsKey("lookbackWindow")) {
            this.lookbackWindow = params.get("lookbackWindow").intValue();
        }
        if (params.containsKey("buyThreshold")) {
            this.buyThreshold = params.get("buyThreshold");
        }
        if (params.containsKey("sellThreshold")) {
            this.sellThreshold = params.get("sellThreshold");
        }
    }

    /**
     * 训练模型
     * @param stockData 历史股票数据
     */
    public void trainModel(List<StockData> stockData) {
        if (stockData.size() < lookbackWindow + 1) {
            logger.error("Not enough data to train model");
            return;
        }

        int numSamples = stockData.size() - lookbackWindow;
        double[][] features = new double[numSamples][];
        double[] labels = new double[numSamples];

        // 准备训练数据
        for (int i = 0; i < numSamples; i++) {
            // 提取特征
            List<ProcessedData> history = new ArrayList<>();
            for (int j = i + 1; j <= i + lookbackWindow; j++) {
                StockData sd = stockData.get(j);
                history.add(new ProcessedData(sd.getClose(), sd.getDate()));
            }

            StockData current = stockData.get(i);
            ProcessedData currentProcessed = new ProcessedData(current.getClose(), current.getDate());

            features[i] = extractFeatures(currentProcessed, history);

            // 计算标签（下一天的收益率）
            double currentPrice = current.getClose();
            double nextPrice = stockData.get(i > 0 ? i - 1 : 0).getClose();
            labels[i] = (nextPrice - currentPrice) / currentPrice;
        }

        // 训练模型
        algorithm.train(features, labels);
        logger.info("Model training completed with {} samples", numSamples);
    }

    /**
     * 可视化预测结果
     */
    public void visualizePredictions(List<StockData> stockData) {
        if (predictions.isEmpty() || predictionDates.isEmpty()) {
            logger.warn("No predictions available for visualization");
            return;
        }

        String symbol = stockData.size() > 0 ? stockData.get(0).getSymbol() : "Unknown";
        String title = symbol + " - ML Prediction vs Actual Price";

        ChartGenerator.createPredictionChart(title, stockData, predictions, predictionDates);
    }

    /**
     * 可视化特征重要性
     */
    public void visualizeFeatureImportance() {
        Map<String, Double> importance = algorithm.getFeatureImportance();
        if (importance.isEmpty()) {
            logger.warn("No feature importance data available");
            return;
        }

        String title = "Feature Importance";
        ChartGenerator.createFeatureImportanceChart(title, importance);
    }
}