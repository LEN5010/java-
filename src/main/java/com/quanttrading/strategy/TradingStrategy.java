// src/main/java/com/quanttrading/strategy/TradingStrategy.java
package com.quanttrading.strategy;

import com.quanttrading.model.ProcessedData;
import com.quanttrading.model.TradeSignal;
import java.util.List;
import java.util.Map;

public interface TradingStrategy {
    TradeSignal generateSignal(ProcessedData currentData, List<ProcessedData> historicalData);
    void setParameters(Map<String, Double> params);
}