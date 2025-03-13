package com.quanttrading.factory;

import com.quanttrading.ml.MachineLearningAlgorithm;
import com.quanttrading.ml.factory.MLAlgorithmFactory;
import com.quanttrading.strategy.MovingAverageStrategy;
import com.quanttrading.strategy.TradingStrategy;
import com.quanttrading.strategy.impl.MachineLearningStrategy;

import java.util.Map;

public class StrategyFactory {
    public static TradingStrategy createStrategy(String type) {
        return createStrategy(type, null);
    }

    public static TradingStrategy createStrategy(String type, Map<String, Object> config) {
        switch (type.toLowerCase()) {
            case "moving_average":
            case "ma":
                return new MovingAverageStrategy();
            case "ml":
            case "machine_learning":
                if (config != null && config.containsKey("algorithm")) {
                    String algorithmType = (String) config.get("algorithm");
                    MachineLearningAlgorithm algorithm = MLAlgorithmFactory.createAlgorithm(algorithmType);
                    return new MachineLearningStrategy(algorithm);
                } else {
                    // 默认使用随机森林
                    MachineLearningAlgorithm algorithm = MLAlgorithmFactory.createAlgorithm("randomforest");
                    return new MachineLearningStrategy(algorithm);
                }
            default:
                throw new IllegalArgumentException("Unsupported strategy type: " + type);
        }
    }
}