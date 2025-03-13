package com.quanttrading.strategy;

import com.quanttrading.model.ProcessedData;
import com.quanttrading.model.TradeSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class MovingAverageStrategy implements TradingStrategy {
    private static final Logger logger = LoggerFactory.getLogger(MovingAverageStrategy.class);
    private int shortWindow = 5;
    private int longWindow = 20;

    @Override
    public void setParameters(Map<String, Double> params) {
        if (params.containsKey("shortWindow")) {
            this.shortWindow = params.get("shortWindow").intValue();
        }
        if (params.containsKey("longWindow")) {
            this.longWindow = params.get("longWindow").intValue();
        }
        logger.info("Strategy configured with shortWindow={}, longWindow={}", shortWindow, longWindow);
    }

    @Override
    public TradeSignal generateSignal(ProcessedData currentData, List<ProcessedData> historicalData) {
        if (historicalData.size() < longWindow) {
            logger.warn("Not enough historical data for MA calculation. Need at least {} data points.", longWindow);
            return TradeSignal.HOLD;
        }

        double shortMA = calculateMA(historicalData, shortWindow);
        double longMA = calculateMA(historicalData, longWindow);

        if (shortMA > longMA) {
            return TradeSignal.BUY;
        } else if (shortMA < longMA) {
            return TradeSignal.SELL;
        } else {
            return TradeSignal.HOLD;
        }
    }

    private double calculateMA(List<ProcessedData> data, int window) {
        if (data.size() < window) {
            return 0;
        }

        return data.stream()
                .limit(window)
                .mapToDouble(ProcessedData::getValue)
                .average()
                .orElse(0);
    }
}