package com.quanttrading.factory;

import com.quanttrading.strategy.MovingAverageStrategy;
import com.quanttrading.strategy.TradingStrategy;

public class StrategyFactory {
    public static TradingStrategy createStrategy(String type) {
        switch (type.toLowerCase()) {
            case "moving_average":
            case "ma":
                return new MovingAverageStrategy();
            default:
                throw new IllegalArgumentException("Unsupported strategy type: " + type);
        }
    }
}